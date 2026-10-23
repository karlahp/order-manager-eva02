# Fase 4 — API REST

**Objetivo:** exponer el servicio por HTTP bajo `/api/orders`.

- `OrderController` con los endpoints de lectura, creación, modificación, transiciones y borrado (ver tabla en el README).
- DTOs con Jakarta Validation: `CreateOrderRequest` y `AddItemRequest`.
- `GlobalExceptionHandler` (`@RestControllerAdvice`) con formato de error uniforme:
  404 (no existe), 409 (estado inválido), 400 (datos inválidos), 500 (error interno sin detalles).
