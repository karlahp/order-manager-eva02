#!/usr/bin/env bash
# ============================================================
# scripts/smoke-test.sh - Verificación post-despliegue (IE4)
#
# Uso:  scripts/smoke-test.sh [URL_BASE] [SHA_ESPERADO]
#   URL_BASE      por defecto http://localhost:8098
#   SHA_ESPERADO  si se indica, verifica que /actuator/info reporte ESE commit
#                 (prueba de trazabilidad: lo desplegado == lo commiteado)
#
# Requiere: curl y jq. Sale con código != 0 si algo falla (bloquea el pipeline).
# ============================================================
set -euo pipefail

BASE_URL="${1:-http://localhost:8098}"
EXPECTED_SHA="${2:-}"
API="$BASE_URL/api/orders"

ok()   { echo "  ✔ $*"; }
fail() { echo "  ✘ $*" >&2; exit 1; }

# Ejecuta una petición y valida el código HTTP. Deja el cuerpo en $BODY.
# uso: call METODO URL CODIGO_ESPERADO [JSON]
call() {
  local method="$1" url="$2" expected="$3" data="${4:-}" status
  local args=(-s -o /tmp/smoke-body.json -w '%{http_code}' -X "$method" "$url")
  [[ -n "$data" ]] && args+=(-H 'Content-Type: application/json' -d "$data")
  status="$(curl "${args[@]}")"
  BODY="$(cat /tmp/smoke-body.json)"
  [[ "$status" == "$expected" ]] || fail "$method $url -> HTTP $status (esperado $expected). Cuerpo: $BODY"
}

echo "▶ Smoke test contra $BASE_URL"

# 1) La aplicación debe quedar saludable (reintenta hasta ~2 min)
echo "1) Health check"
for i in $(seq 1 30); do
  if curl -sf "$BASE_URL/actuator/health" | jq -e '.status == "UP"' >/dev/null 2>&1; then
    ok "/actuator/health = UP"; break
  fi
  [[ $i -eq 30 ]] && fail "la aplicación no llegó a UP"
  sleep 4
done
curl -sf "$BASE_URL/actuator/health/readiness" | jq -e '.status == "UP"' >/dev/null \
  && ok "/actuator/health/readiness = UP" || fail "readiness no está UP"

# 2) Trazabilidad: commit desplegado
echo "2) Trazabilidad (/actuator/info)"
INFO="$(curl -sf "$BASE_URL/actuator/info")"
echo "   $INFO"
if [[ -n "$EXPECTED_SHA" ]]; then
  ACTUAL_SHA="$(echo "$INFO" | jq -r '.app.commit')"
  [[ "$ACTUAL_SHA" == "$EXPECTED_SHA" ]] \
    && ok "commit desplegado = $ACTUAL_SHA" \
    || fail "commit desplegado ($ACTUAL_SHA) != esperado ($EXPECTED_SHA)"
fi

# 3) Flujo de negocio completo
echo "3) Flujo de negocio"
call POST "$API" 201 '{"customerName":"Smoke Test","items":[{"productName":"Teclado","quantity":2,"unitPrice":15000.50}]}'
ID="$(echo "$BODY" | jq -r '.id')"
[[ "$(echo "$BODY" | jq -r '.status')" == "PENDING" ]] || fail "el pedido nuevo debe quedar PENDING"
ok "pedido $ID creado (PENDING)"

call POST "$API/$ID/items" 200 '{"productName":"Mouse","quantity":1,"unitPrice":8000}'
[[ "$(echo "$BODY" | jq -r '.items | length')" == "2" ]] || fail "debería tener 2 ítems"
ok "ítem agregado (2 ítems)"

call PUT "$API/$ID/confirm" 200; ok "CONFIRMED"
call POST "$API/$ID/items" 409 '{"productName":"X","quantity":1,"unitPrice":1}'
ok "no se puede modificar un pedido CONFIRMED (409)"
call PUT "$API/$ID/ship" 200;    ok "SHIPPED"
call PUT "$API/$ID/deliver" 200; ok "DELIVERED"
[[ "$(echo "$BODY" | jq -r '.status')" == "DELIVERED" ]] || fail "estado final debe ser DELIVERED"
call PUT "$API/$ID/cancel" 409;  ok "un pedido DELIVERED no se puede cancelar (409)"

# 4) Manejo de errores
echo "4) Manejo de errores"
call GET "$API/999999" 404;                                ok "pedido inexistente -> 404"
call POST "$API" 400 '{"customerName":"","items":[]}';     ok "validación de entrada -> 400"

# 5) Limpieza
call DELETE "$API/$ID" 204; ok "pedido de prueba eliminado"

echo "✅ Smoke test OK"
