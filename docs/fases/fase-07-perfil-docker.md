# Fase 7 — Perfil `docker` (PostgreSQL)

**Entregable:** `application-docker.properties`, activo con `SPRING_PROFILES_ACTIVE=docker`.

- Conexión a PostgreSQL con `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`. **Usuario y clave no tienen valor por defecto**: si faltan, la app no arranca (no hay credenciales escondidas en el código).
- Pool Hikari acotado (`DB_POOL_SIZE`).
- `ddl-auto=update` (en producción real: `validate` + Flyway/Liquibase).
- `show-sql=false`, consola H2 deshabilitada.
- Actuator con `show-details=never` y probes de liveness/readiness.
- Apagado ordenado (`server.shutdown=graceful`).
