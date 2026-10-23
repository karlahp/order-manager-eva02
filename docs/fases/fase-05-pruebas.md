# Fase 5 — Pruebas automatizadas (IE2)

| Nivel | Clases | Enfoque |
|---|---|---|
| Unitarias de modelo | `OrderTest`, `OrderItemTest` | Reglas del dominio y cálculo de totales |
| Unitarias de servicio | `OrderServiceTest` | Mockito sobre el repositorio; transiciones y errores |
| Web (slice) | `OrderControllerTest` | `@WebMvcTest` + `MockMvc`: códigos HTTP y validaciones |
| Manejo de errores | `GlobalExceptionHandlerTest` | Formato de respuesta de cada excepción |
| **Integración** | `OrderFlowIntegrationTest` | `@SpringBootTest` con H2: ciclo de vida completo, reglas 409, 400/404, health e info |

**Política de calidad:** JaCoCo genera el informe en `mvn test` y **falla `mvn verify` si la cobertura de líneas es < 70 %** (`jacoco.line.minimum` en el `pom.xml`).
