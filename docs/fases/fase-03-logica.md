# Fase 3 — Lógica de negocio

**Objetivo:** implementar los casos de uso en `OrderService`.

- Consultas: todos, por id, por estado.
- Creación de pedidos con ítems opcionales.
- Agregar ítems (solo en `PENDING`).
- Transiciones: `confirm` (PENDING→CONFIRMED), `ship` (CONFIRMED→SHIPPED), `deliver` (SHIPPED→DELIVERED).
- `cancel`: permitido hasta `SHIPPED`; un pedido `DELIVERED` o ya `CANCELLED` no se cancela.
- Eliminación con verificación de existencia.
- Excepciones propias: `OrderNotFoundException` y `InvalidOrderStateException`.
