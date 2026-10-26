# Fase 1 — Fundación del proyecto

**Objetivo:** crear la base del microservicio Spring Boot.

- Proyecto Maven con Spring Boot 3.5.16 y Java 17 (`pom.xml`).
- Dependencias: Web, Data JPA, Validation, Actuator, H2 (runtime), PostgreSQL (runtime), Test.
- Punto de entrada: `OrderManagerApplication`.
- Configuración base en `application.properties` (H2 en memoria, puerto **8098**, Actuator; consola H2 deshabilitada por defecto y activable solo en desarrollo con `H2_CONSOLE_ENABLED=true`).
- `.gitignore` (excluye `target/`, IDE, logs, `.env`).

**Entregable:** la aplicación arranca con `mvn spring-boot:run`.
