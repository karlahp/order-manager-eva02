# Fase 9 — Pipeline CI/CD (IE1–IE4)

**Entregables:** `.github/workflows/ci-cd.yml`, `.github/dependabot.yml`, `sonar-project.properties`, `scripts/smoke-test.sh`.

| Job | Hace | Bloquea si… |
|---|---|---|
| `test` | `mvn clean verify`, sube reportes Surefire y JaCoCo | falla un test o la cobertura de líneas < 75 % |
| `sonar` | Análisis SonarCloud y espera del Quality Gate; se omite en PR de forks y Dependabot por falta de secrets | no se aprueba el gate o falta `SONAR_TOKEN` en un push o PR interno |
| `build` | Construye la imagen, verifica no-root, ejecuta smoke test, publica en GHCR (`sha-<commit>`) | falla cualquier verificación |
| `deploy` | Entorno `simulated-cloud`: Compose espera servicios saludables, smoke test, verifica el commit y registra el despliegue | health/smoke fallan |
| `alert` | Crea o comenta un Issue de alerta por rama | — |

**Mejoras respecto a un pipeline básico:** permisos mínimos, `concurrency`, `timeout-minutes`, caché Maven de `setup-java`, credenciales aleatorias de PostgreSQL enmascaradas y limitadas al job, `secrets` pasados vía `env`, y acciones mantenidas por Dependabot. El Dockerfile ordena las capas para reutilizar dependencias dentro de una construcción; el workflow no configura caché persistente de capas Docker entre ejecuciones.
