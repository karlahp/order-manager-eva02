# Fase 6 — Contenedores (IE1)

**Entregables:** `Dockerfile` y `.dockerignore`.

Decisiones del `Dockerfile`:

| Decisión | Motivo |
|---|---|
| Build **multi-etapa** (Maven → JRE) | La imagen final no lleva Maven ni JDK |
| Base `eclipse-temurin:17-jre-alpine` | Imagen pequeña y menor superficie de ataque |
| `pom.xml` antes que `src` | Aprovecha la caché de capas de dependencias |
| JAR en **capas** (`layertools`) | Un cambio de código solo reconstruye la capa de la aplicación |
| Usuario **no root** UID 10001 | Seguridad; UID numérico exigido por Kubernetes |
| `HEALTHCHECK` sobre `/actuator/health` | Docker y orquestadores conocen el estado real |
| `JAVA_TOOL_OPTIONS` (`MaxRAMPercentage=75`) | La JVM respeta el límite de memoria del contenedor |
| `ARG APP_COMMIT_SHA` + label OCI | Trazabilidad commit → imagen |
| `.dockerignore` (sin `.env`, `.git`, `target`) | Contexto liviano y sin secretos |
