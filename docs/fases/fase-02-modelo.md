# Fase 2 — Modelo de dominio

**Objetivo:** representar un pedido de e-commerce.

| Clase | Tipo | Responsabilidad |
|---|---|---|
| `Order` | `@Entity` (tabla `orders`) | Cliente, estado, total, fecha de creación, ítems |
| `OrderItem` | `@Embeddable` (tabla `order_items`) | Producto, cantidad, precio; calcula su subtotal |
| `OrderStatus` | `enum` | `PENDING`, `CONFIRMED`, `SHIPPED`, `DELIVERED`, `CANCELLED` |

Reglas en el modelo: el nombre del cliente y del producto son obligatorios, la cantidad debe ser > 0, el precio no puede ser negativo; `Order.addItem` recalcula el total; solo un pedido `PENDING` puede modificarse (`canBeModified`).
