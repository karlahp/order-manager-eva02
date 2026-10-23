# Fase 1 — Fundación del proyecto

**Objetivo:** crear la base del microservicio Spring Boot.

- Proyecto Maven con Spring Boot 3.3.4 y Java 17 (`pom.xml`).
- Dependencias: Web, Data JPA, Validation, Actuator, H2 (runtime), PostgreSQL (runtime), Test.
- Punto de entrada: `OrderManagerApplication`.
- Configuración base en `application.properties` (H2 en memoria, puerto **8098**, consola H2, Actuator).
- `.gitignore` (excluye `target/`, IDE, logs, `.env`).

**Entregable:** la aplicación arranca con `mvn spring-boot:run`.
