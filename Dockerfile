# ============================================================
# Dockerfile - Order Manager API  (IE1)
# Build multi-etapa + imagen final mínima, no-root y con healthcheck.
# ============================================================

# ------------------------------------------------------------
# ETAPA 1: BUILD  (no forma parte de la imagen final)
# ------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# 1) Solo el pom.xml: si el código cambia pero las dependencias no,
#    Docker reutiliza la capa de dependencias (caché de capas).
COPY pom.xml .
RUN mvn -B -ntp dependency:go-offline

# 2) Código fuente y empaquetado. Los tests NO se ejecutan aquí porque
#    ya son una etapa bloqueante del pipeline (job "test").
COPY src ./src
RUN mvn -B -ntp clean package -DskipTests

# 3) Extrae el JAR en capas (dependencias / loader / aplicación).
#    Así un cambio de código solo invalida la capa pequeña "application".
RUN java -Djarmode=layertools -jar target/*.jar extract --destination target/extracted

# ------------------------------------------------------------
# ETAPA 2: RUNTIME  (imagen final)
# JRE (no JDK) sobre Alpine: pequeña y con menor superficie de ataque.
# ------------------------------------------------------------
FROM eclipse-temurin:17-jre-alpine AS runtime
WORKDIR /app

# Usuario NO root con UID numérico fijo (Kubernetes exige UID numérico
# para verificar runAsNonRoot).
RUN addgroup -S -g 10001 spring && adduser -S -u 10001 -G spring spring

# Copia de capas, de la que menos cambia a la que más cambia.
COPY --from=build --chown=spring:spring /app/target/extracted/dependencies/ ./
COPY --from=build --chown=spring:spring /app/target/extracted/spring-boot-loader/ ./
COPY --from=build --chown=spring:spring /app/target/extracted/snapshot-dependencies/ ./
COPY --from=build --chown=spring:spring /app/target/extracted/application/ ./

USER 10001:10001

# Trazabilidad: el pipeline pasa el SHA del commit; queda en la imagen,
# en sus labels OCI y en /actuator/info.
ARG APP_COMMIT_SHA=local
ENV APP_COMMIT_SHA=${APP_COMMIT_SHA}
LABEL org.opencontainers.image.title="order-manager" \
      org.opencontainers.image.description="Order Manager API (DOY0101)" \
      org.opencontainers.image.revision="${APP_COMMIT_SHA}"

# La JVM respeta los límites de memoria del contenedor (cgroups):
# usa hasta 75% de la RAM asignada y se cae limpio ante un OOM
# (para que Docker/Kubernetes lo reinicien en vez de quedar colgado).
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

# Debe coincidir con server.port de las propiedades.
EXPOSE 8098

HEALTHCHECK --interval=10s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8098/actuator/health || exit 1

# Formato exec (sin shell): la JVM recibe SIGTERM directamente (apagado ordenado).
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
