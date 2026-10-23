# Fase 9 — Pipeline CI/CD (IE1–IE4)

**Entregables:** `.github/workflows/ci-cd.yml`, `.github/dependabot.yml`, `sonar-project.properties`, `scripts/smoke-test.sh`.

| Job | Hace | Bloquea si… |
|---|---|---|
| `test` | `mvn clean verify`, resumen de pruebas/cobertura, sube reportes | falla un test o la cobertura < 70 % |
| `sonar` | Análisis SonarCloud y espera del Quality Gate | no se aprueba el gate o falta `SONAR_TOKEN` (en push) |
| `build` | Construye la imagen, verifica no-root, smoke test, publica en GHCR (`sha-<commit>`, SBOM) | falla cualquier verificación |
| `deploy` | Entorno `simulated-cloud`: Compose con la imagen publicada, health, smoke test, verifica el commit, registra el despliegue | health/smoke fallan |
| `alert` | Abre un Issue indicando la etapa fallida | — |

**Mejoras respecto a un pipeline básico:** permisos mínimos, `concurrency`, `timeout-minutes`, caché de Maven y de capas Docker, credenciales efímeras en el deploy, `secrets` pasados vía `env` (no se pueden usar directo en un `if`), y acciones oficiales mantenidas al día por Dependabot.
