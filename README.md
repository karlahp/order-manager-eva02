# 📦 Order Manager API

[![CI/CD Pipeline](https://github.com/karlahp/order-manager-eva02/actions/workflows/ci-cd.yml/badge.svg)](https://github.com/karlahp/order-manager-eva02/actions/workflows/ci-cd.yml)
[![Java](https://img.shields.io/badge/Java-17-blue.svg)](https://openjdk.org/projects/jdk/17/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.2-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Docker](https://img.shields.io/badge/Docker-multi--stage-blue.svg)](https://www.docker.com/)

Microservicio REST para la gestión de pedidos de un e-commerce, desarrollado para la asignatura **Ingeniería DevOps (DOY0101)** de Duoc UC — **Evaluación Parcial N°2: "Añadiéndole complejidad a nuestro pipeline"**.

El proyecto automatiza el ciclo de vida del microservicio: **pruebas → análisis estático y revisión de dependencias → contenedor → despliegue → orquestación**, con trazabilidad desde el commit hasta el contenedor en ejecución.

---

## 📑 Tabla de contenidos

1. [Descripción y mapeo con la rúbrica](#-descripción-y-mapeo-con-la-rúbrica)
2. [Stack tecnológico](#-stack-tecnológico)
3. [Arquitectura](#-arquitectura)
4. [Requisitos previos](#-requisitos-previos)
5. [Ejecución local](#-ejecución-local)
6. [Endpoints de la API](#-endpoints-de-la-api)
7. [Pipeline CI/CD](#-pipeline-cicd)
8. [Configuración inicial en GitHub](#-configuración-inicial-en-github)
9. [Seguridad y escalabilidad (parámetros ajustados)](#-seguridad-y-escalabilidad-parámetros-ajustados)
10. [Orquestación de contenedores](#-orquestación-de-contenedores)
11. [Trazabilidad y calidad](#-trazabilidad-y-calidad)
12. [Uso de Inteligencia Artificial](#-uso-de-inteligencia-artificial)
13. [Conclusiones y reflexiones individuales](#-conclusiones-y-reflexiones-individuales)
14. [Autores](#-autores)

---

## 🎯 Descripción y mapeo con la rúbrica

**Order Manager** permite crear pedidos con ítems, modificarlos mientras están pendientes y hacerlos avanzar por un ciclo de vida con reglas de negocio:

```
PENDING ──► CONFIRMED ──► SHIPPED ──► DELIVERED
   │            │             │
   └────────────┴─────────────┴──► CANCELLED      (DELIVERED es terminal: no se cancela)
```

### ¿Dónde se cumple cada indicador?

| Indicador | Qué exige | Dónde se evidencia |
|---|---|---|
| **IE1** Contenedores | Dockerfile + construcción de imagen en el pipeline | [`Dockerfile`](Dockerfile) multi-etapa, [`.dockerignore`](.dockerignore), job `build` de [`ci-cd.yml`](.github/workflows/ci-cd.yml) |
| **IE2** Pruebas automatizadas | Pruebas unitarias en CI según políticas de calidad | 69 pruebas JUnit 5/Mockito + integración (`src/test`), cobertura mínima **75 %** forzada por JaCoCo, job `test` |
| **IE3** Escalabilidad y seguridad | Análisis estático, actualizaciones de dependencias, alertas y controles del contenedor | SonarCloud en el job `sonar`, actualizaciones de Dependabot, alerta del job `alert` ante fallos en `push`, controles del contenedor y límites de recursos. Los manifiestos incluyen HPA, pero no se ha demostrado su ejecución en un clúster. |
| **IE4** Despliegue y trazabilidad | Deploy automático en entorno simulado + README | job `deploy` (Docker Compose), imagen etiquetada con el SHA, `/actuator/info`, registro de despliegue |
| **IE5** Orquestación | Kubernetes o Docker Compose | [`docker-compose.yml`](docker-compose.yml) se ejecutó en el pipeline con la API y PostgreSQL. También se incluyen manifiestos en [`k8s/`](k8s/), sin evidencia de aplicación en un clúster. |

---

## 🛠 Stack tecnológico

| Capa | Tecnología |
|---|---|
| Lenguaje / framework | Java 17, Spring Boot 4.1.2 (Web, Data JPA, Validation, Actuator) |
| Persistencia | H2 en memoria (desarrollo y tests) · PostgreSQL 16 (Docker/Kubernetes) |
| Testing | JUnit 5, Mockito, Spring Boot Test (`MockMvc`), JaCoCo |
| Contenedores | Docker (multi-stage, JAR en capas), Docker Compose, Kubernetes (Kustomize) |
| CI/CD | GitHub Actions, GitHub Container Registry (GHCR), GitHub Environments |
| Seguridad / calidad | Dependabot, SonarCloud (Quality Gate), JaCoCo |

---

## 🏗 Arquitectura

Arquitectura en capas clásica de Spring Boot:

```
┌──────────────────────────────────────────┐
│  Cliente HTTP (curl, Postman, frontend)  │
└────────────────────┬─────────────────────┘
                     │ JSON / HTTP :8098
┌────────────────────▼─────────────────────┐
│  OrderController      (@RestController)  │  ← Capa REST
│  GlobalExceptionHandler                  │  ← Errores uniformes
├──────────────────────────────────────────┤
│  OrderService         (@Service)         │  ← Reglas de negocio / transiciones
├──────────────────────────────────────────┤
│  OrderRepository      (JpaRepository)    │  ← Acceso a datos
├──────────────────────────────────────────┤
│  Order, OrderItem     (@Entity)          │  ← Modelo de dominio
└────────────────────┬─────────────────────┘
                     │ JDBC
┌────────────────────▼─────────────────────┐
│  H2 (dev/test)  |  PostgreSQL 16 (docker)│
└──────────────────────────────────────────┘
```

**Estructura del repositorio:**

```
order-manager/
├── .github/
│   ├── workflows/ci-cd.yml        # Pipeline CI/CD
│   └── dependabot.yml             # Escaneo de dependencias
├── k8s/                           # Manifiestos Kubernetes (Kustomize)
├── scripts/smoke-test.sh          # Verificación post-despliegue
├── src/
│   ├── main/java/com/example/ordermanager/
│   │   ├── controller/ dto/ exception/ model/ repository/ service/
│   │   └── OrderManagerApplication.java
│   ├── main/resources/
│   │   ├── application.properties          # Perfil base (H2)
│   │   └── application-docker.properties   # Perfil docker (PostgreSQL)
│   └── test/java/.../                      # Unitarias + integración
├── docs/fases/                    # Bitácora por fase del proyecto
├── Dockerfile                     # Imagen multi-etapa
├── .gitignore                     # Excluye secretos y artefactos locales
├── docker-compose.yml             # Orquestación app + BD
├── .env.example                   # Plantilla de credenciales (NO subir .env)
├── sonar-project.properties       # Configuración SonarCloud
└── pom.xml
```

---

## ⚙️ Requisitos previos

| Herramienta | Versión mínima | Verificación |
|---|---|---|
| Java JDK | 17 | `java -version` |
| Maven | 3.8+ | `mvn -version` |
| Docker + Compose v2 | 20+ | `docker compose version` |
| `curl` y `jq` | — | solo para `scripts/smoke-test.sh` |
| kubectl + clúster local (opcional) | 1.27+ | kind, minikube o Docker Desktop |

---

## 🖥 Ejecución local

### Opción 1 — Maven (desarrollo rápido, usa H2)

```bash
mvn clean verify          # pruebas + cobertura mínima
mvn spring-boot:run       # http://localhost:8098
```

La consola H2 está deshabilitada por defecto y también en Docker. Si la necesitan en desarrollo local, actívenla solo para esa ejecución con `H2_CONSOLE_ENABLED=true`; el usuario es `sa` y la base no tiene contraseña. El endpoint de salud no revela detalles internos.

### Opción 2 — Docker Compose (app + PostgreSQL)

```bash
cp .env.example .env                 # definir credenciales (cambiar la clave)
docker compose up -d --build --wait --wait-timeout 90  # construir y esperar estado saludable
docker compose ps                    # app y db deben estar "healthy"
scripts/smoke-test.sh                # prueba de punta a punta (opcional)
docker compose logs -f app
docker compose down                  # detener (conserva datos)
docker compose down -v               # detener y borrar datos
```

| Servicio | Contenedor | Puerto | Notas |
|---|---|---|---|
| API | `order-manager-app` | 8098 | usuario no-root, FS de solo lectura |
| PostgreSQL | `order-manager-db` | 127.0.0.1:5432 | volumen persistente `db-data` |

### Opción 3 — Kubernetes (kind / minikube / Docker Desktop)

```bash
docker build -t order-manager:local --build-arg APP_COMMIT_SHA=local .
kind load docker-image order-manager:local        # (minikube: minikube image load order-manager:local)

kubectl create namespace order-manager
kubectl -n order-manager create secret generic order-manager-db \
  --from-literal=DB_USER=order_user \
  --from-literal=DB_PASSWORD="$(openssl rand -hex 16)"

kubectl apply -k k8s/
kubectl -n order-manager get pods,svc,hpa
kubectl -n order-manager port-forward svc/order-manager 8098:8098
```

> Para usar la imagen publicada en GHCR, editar `images:` en `k8s/kustomization.yaml` (`newName: ghcr.io/<usuario>/<repo>`, `newTag: sha-<commit>`). Si el paquete es privado, crear un `imagePullSecret`. El HPA requiere `metrics-server` en el clúster.

---

## 📡 Endpoints de la API

| Método | Endpoint | Descripción | Éxito | Errores |
|---|---|---|---|---|
| `GET` | `/api/orders` | Lista todos los pedidos | 200 | |
| `GET` | `/api/orders/{id}` | Obtiene un pedido | 200 | 404 |
| `GET` | `/api/orders/status/{status}` | Filtra por estado | 200 | 400 |
| `POST` | `/api/orders` | Crea un pedido (con ítems opcionales) | 201 | 400 |
| `POST` | `/api/orders/{id}/items` | Agrega un ítem (solo `PENDING`) | 200 | 400, 404, 409 |
| `PUT` | `/api/orders/{id}/confirm` | `PENDING → CONFIRMED` | 200 | 404, 409 |
| `PUT` | `/api/orders/{id}/ship` | `CONFIRMED → SHIPPED` | 200 | 404, 409 |
| `PUT` | `/api/orders/{id}/deliver` | `SHIPPED → DELIVERED` | 200 | 404, 409 |
| `PUT` | `/api/orders/{id}/cancel` | Cancela (no si ya fue entregado) | 200 | 404, 409 |
| `DELETE` | `/api/orders/{id}` | Elimina un pedido | 204 | 404 |
| `GET` | `/actuator/health` | Estado de salud | 200 | |
| `GET` | `/actuator/health/liveness` · `/readiness` | Probes para Kubernetes | 200 | |
| `GET` | `/actuator/info` | Versión y **commit desplegado** | 200 | |

**Ejemplo:**

```bash
curl -X POST http://localhost:8098/api/orders \
  -H "Content-Type: application/json" \
  -d '{"customerName":"Ana","items":[{"productName":"Teclado","quantity":2,"unitPrice":15000.50}]}'

curl -X PUT http://localhost:8098/api/orders/1/confirm
curl http://localhost:8098/actuator/info
# {"app":{"name":"order-manager","version":"1.0.0","commit":"a1b2c3..."}}
```

**Formato de error uniforme:**

```json
{ "timestamp": "2026-10-20T10:30:00", "status": 409, "error": "Conflict",
  "message": "Un pedido entregado no puede cancelarse" }
```

---

## 🔄 Pipeline CI/CD

Definido en [`.github/workflows/ci-cd.yml`](.github/workflows/ci-cd.yml). Se ejecuta en cada `push` a `main`/`develop`, en cada Pull Request hacia `main` y manualmente (`workflow_dispatch`).

```
                      git push / pull request
                                │
              ┌─────────────────┴─────────────────┐
              ▼                                   ▼
   ┌─────────────────────┐             ┌─────────────────────┐
   │ JOB test       (IE2)│             │ JOB sonar      (IE3)│
   │ • mvn clean verify  │             │ • Análisis SonarCloud│
   │ • JUnit + integración│            │ • Quality Gate       │
   │ • JaCoCo ≥ 75 %  ⛔ │             │   bloqueante     ⛔  │
   │ • Reportes (artifact)│            └──────────┬──────────┘
   └──────────┬──────────┘                        │
              └─────────────────┬─────────────────┘
                                ▼
                  ┌───────────────────────────┐
                  │ JOB build            (IE1)│
                  │ • docker build (multi-stage)
                  │ • Verifica usuario no-root ⛔
                  │ • Smoke test del contenedor ⛔
                  │ • Publica en GHCR :sha-<commit>
                  │ • Etiqueta asociada al SHA :sha-<commit>
                  └─────────────┬─────────────┘
                                ▼   solo push a main
                  ┌───────────────────────────┐
                  │ JOB deploy           (IE4)│
                  │ • Environment "simulated-cloud"
                  │ • docker compose pull/up
                  │ • Espera estado healthy ⛔
                  │ • Smoke test + verifica commit ⛔
                  │ • Registro de despliegue (artifact)
                  └─────────────┬─────────────┘
                                ▼   si algo falló en un push
                  ┌───────────────────────────┐
                  │ JOB alert            (IE3)│
                  │ • Abre un Issue con la etapa fallida
                  └───────────────────────────┘
                                          ⛔ = bloquea el pipeline si falla
```

### Etapas

| Job | Propósito | Herramientas | Indicador |
|---|---|---|---|
| `test` | Ejecutar pruebas y exigir cobertura mínima | JUnit 5, Mockito, MockMvc, JaCoCo | IE2 |
| `sonar` | Análisis estático y Quality Gate | SonarCloud + `sonar-maven-plugin` | IE3 |
| `build` | Construir, verificar y publicar la imagen | Docker, GHCR | IE1 |
| `deploy` | Desplegar y verificar en entorno simulado | Docker Compose, `smoke-test.sh` | IE4 |
| `alert` | Notificar fallos con un Issue | `actions/github-script` | IE3 |

### Bloqueos y alertas (IE3)

| Si falla… | Consecuencia |
|---|---|
| Un test, o la cobertura baja del 75 % | `test` falla → no se construye ni se despliega |
| El Quality Gate de SonarCloud | `sonar` falla → no se construye ni se despliega |
| Falta el `SONAR_TOKEN` en un push | el pipeline **falla** (la seguridad nunca se omite en silencio) |
| La imagen corre como root o su smoke test falla | `build` falla → no se publica ni se despliega |
| El health check o el smoke test del despliegue | `deploy` falla → se limpia el entorno y salta la alerta |
| Cualquier etapa en un `push` | job `alert` abre un **Issue** automático (y GitHub envía correo) |
| Dependabot encuentra una actualización de versión | abre un PR automático; el PR debe revisarse y pasar los checks antes de integrarse. Las alertas de vulnerabilidades de GitHub están activadas; al verificarlas había cero alertas abiertas. Los PR automáticos de seguridad permanecen desactivados. |

---

## 🔧 Configuración inicial en GitHub

Estos pasos se hacen **una sola vez** en el repositorio (no se pueden versionar en el código):

1. **SonarCloud** ([sonarcloud.io](https://sonarcloud.io)) → iniciar sesión con GitHub → *Analyze new project* → seleccionar el repositorio.
   En *Administration › Analysis Method* desactivar **Automatic Analysis** (el análisis lo hace el pipeline).
2. En GitHub: **Settings › Secrets and variables › Actions**
   - **Secret** `SONAR_TOKEN` → token generado en SonarCloud (*My Account › Security*).
   - **Variables** `SONAR_ORGANIZATION` y `SONAR_PROJECT_KEY` → valores que muestra SonarCloud.
3. **Settings › Code security**: *Dependency graph* y *Dependabot alerts* están activados. Las actualizaciones de versiones están configuradas en `.github/dependabot.yml`. *Dependabot security updates* permanece desactivado; si se habilita, GitHub abrirá PRs automáticos para vulnerabilidades con parches disponibles.
4. **Settings › Actions › General › Workflow permissions**: *Read and write permissions* (necesario para publicar en GHCR y abrir Issues).
5. **Settings › Rules › Rulesets** (o **Settings › Branches**, según la interfaz): configurar y comprobar una regla para `main` que exija los checks `Pruebas y cobertura`, `SonarCloud (Quality Gate)` e `Imagen Docker` antes de permitir el merge. Esta regla es una configuración manual de GitHub; el workflow por sí solo no impide integrar un PR con checks fallidos.
6. *(Opcional)* **Settings › Environments › simulated-cloud**: agregar *Required reviewers* para exigir aprobación manual antes de desplegar.

> Los PR de forks y los PR creados por Dependabot no reciben secrets de GitHub. Por eso el job `sonar` se omite en esos PR; las pruebas y la construcción de imagen siguen ejecutándose. SonarCloud vuelve a ejecutarse en el `push` a `main`. En los PR internos creados por integrantes, el análisis sí es obligatorio.

---

## 🛡 Seguridad y escalabilidad (parámetros ajustados)

### Seguridad

| Parámetro | Dónde | Valor | Justificación |
|---|---|---|---|
| Permisos del token | `ci-cd.yml` | `contents: read` global; cada job amplía lo mínimo | Mínimo privilegio |
| Quality Gate | job `sonar` | `sonar.qualitygate.wait=true` | El job falla si el Quality Gate configurado no pasa |
| Cobertura mínima | `pom.xml` | `jacoco.line.minimum = 0.75` | Política de calidad verificable |
| Usuario del contenedor | `Dockerfile` | UID 10001 (no root), verificado en CI | Limita el daño ante una intrusión |
| Imagen base | `Dockerfile` | `eclipse-temurin:17-jre-alpine` | Menor superficie de ataque (sin JDK ni Maven) |
| Sistema de archivos | Compose / K8s | `read_only` / `readOnlyRootFilesystem` + `/tmp` en memoria | Evita persistencia de malware |
| Capacidades Linux | Compose / K8s | `cap_drop: ALL`, `no-new-privileges` | Sin privilegios extra |
| Credenciales | `.env`, Secret, GitHub Secrets | Nunca en el repositorio; la contraseña de PostgreSQL se genera aleatoriamente, se enmascara en los logs y dura solo el job | Reduce exposición y reutilización |
| Salud sin detalles | `application-docker.properties` | `show-details=never` | No filtra información interna |
| Actualizaciones de versiones | `dependabot.yml` | Maven, Docker y GitHub Actions | PRs periódicos de actualización; revisar y validar antes de integrar |
| Cadena de suministro | Dependabot + etiqueta SHA | PRs de actualización y referencia al commit desplegado | Revisión de dependencias y trazabilidad |

### Escalabilidad y desempeño

| Parámetro | Dónde | Valor | Justificación |
|---|---|---|---|
| Límites de recursos | Compose | app: 1 CPU / 512 MB | Evita que un contenedor monopolice el host |
| JVM consciente del contenedor | `Dockerfile` | `MaxRAMPercentage=75`, `ExitOnOutOfMemoryError` | Usa la memoria asignada y reinicia limpio ante OOM |
| Pool de conexiones | `application-docker.properties` | `DB_POOL_SIZE=10` | Evita saturar PostgreSQL al escalar réplicas |
| Réplicas | `k8s/deployment.yaml` | 2 | Alta disponibilidad |
| Autoescalado | `k8s/hpa.yaml` | 2–5 réplicas al 70 % de CPU | Responde a la carga |
| Actualización sin caída | `k8s/deployment.yaml` | `maxUnavailable: 0`, `maxSurge: 1` | Despliegues sin interrupción |
| Disponibilidad en mantenimiento | `k8s/hpa.yaml` | `PodDisruptionBudget minAvailable: 1` | Siempre queda un pod vivo |
| Apagado ordenado | propiedades | `server.shutdown=graceful` | No corta peticiones en curso |
| Caché de build | `Dockerfile` | dependencias Maven en capa separada y JAR en capas | Builds más eficientes |

---

## 🧩 Orquestación de contenedores

**Docker Compose** (`docker-compose.yml`) — usado por el pipeline para el despliegue:
- `app` espera a que `db` esté realmente `healthy` (`depends_on: condition: service_healthy`).
- Red privada `backend`; el puerto de PostgreSQL solo se publica en `127.0.0.1`.
- Volumen nombrado `db-data` para persistencia; `restart: unless-stopped`.

**Kubernetes** (`k8s/`, aplicable con `kubectl apply -k k8s/`):

| Recurso | Archivo | Función |
|---|---|---|
| Namespace, ConfigMap | `namespace.yaml`, `configmap.yaml` | Aislamiento y configuración no sensible |
| Secret (ejemplo) | `secret.example.yaml` | Credenciales (se crea en el clúster, no se versiona) |
| StatefulSet + PVC | `postgres.yaml` | Base de datos con almacenamiento persistente |
| Deployment | `deployment.yaml` | 2 réplicas, probes startup/readiness/liveness, `initContainer` que espera la BD |
| Service | `service.yaml` | Balanceo interno (puerto 8098) |
| HPA + PDB | `hpa.yaml` | Autoescalado y disponibilidad mínima |

---

## 🎯 Trazabilidad y calidad

### Trazabilidad (de extremo a extremo)

```
commit (SHA) ─► run del pipeline (Run ID) ─► imagen ghcr.io/…:sha-<SHA> ─► contenedor ─► /actuator/info
```

1. **Código (Git):** cada cambio tiene commit con SHA, autor, fecha y mensaje.
2. **Pipeline (GitHub Actions):** cada ejecución tiene `run_id`, logs y artefactos (reportes y `deployment-record`).
3. **Imagen (GHCR):** etiquetada con el **SHA completo del commit** y con label OCI (`org.opencontainers.image.revision`).
4. **Despliegue:** el SHA se inyecta en la imagen (`APP_COMMIT_SHA`) y la aplicación lo expone en `/actuator/info`. El **smoke test compara** ese valor con el commit del pipeline: si no coinciden, el despliegue falla. Así se demuestra que *lo que corre es exactamente lo que se commiteó*.
5. **Historial:** GitHub Environments (`simulated-cloud`) registra cada despliegue; el artefacto `deployment-record.json` guarda commit, autor, rama, run e imagen (90 días).

```bash
docker inspect order-manager-app --format '{{.Config.Image}}'
curl -s localhost:8098/actuator/info | jq .app.commit
```

### Calidad (capas)

| Capa | Herramienta | Criterio |
|---|---|---|
| Pruebas unitarias y de integración | JUnit 5 + Mockito + MockMvc | todas deben pasar |
| Cobertura | JaCoCo | ≥ 75 % de líneas (bloqueante) |
| Análisis estático | SonarCloud | Quality Gate aprobado; el job `build` depende del resultado (bloqueante en el pipeline) |
| Actualizaciones de dependencias | Dependabot | PRs automáticos configurados; verificar por separado que las alertas de seguridad estén activas en GitHub |
| Validación de entrada | Jakarta Validation | `@NotBlank`, `@Min`, `@Max`, `@DecimalMin`… |
| Errores | `@RestControllerAdvice` | formato JSON uniforme |
| Verificación del contenedor | pipeline | no-root + smoke test funcional |

### Reportes disponibles
- **Surefire + JaCoCo:** artefacto `reportes-calidad` en cada ejecución; ahí se consulta el reporte de cobertura validado por el umbral del 75 %.
- **Cobertura verificada con JaCoCo:** en la ejecución de `main` [#40427007011](https://github.com/karlahp/order-manager-eva02/actions/runs/40427007011) (commit `08e1fab`) el reporte global registró **96,72 % de líneas (177/183)**, **98,15 % de ramas (53/54)** y **69 pruebas ejecutadas, 0 fallos y 0 errores**. El `pom.xml` mantiene el umbral bloqueante de 75 % de líneas en `mvn clean verify`; el reporte HTML/XML está disponible en el artefacto `reportes-calidad`. Este registro corresponde al commit indicado, no garantiza por sí solo el resultado de commits posteriores.
- **Historial de despliegues:** pestaña *Actions* y *Environments* de GitHub.

---

## 🤖 Uso de Inteligencia Artificial

### Herramientas utilizadas

| Herramienta | Uso | Aplicación |
|---|---|---|
| **Claude (Anthropic)** | Apoyo en borradores de configuración DevOps y documentación del proyecto. | El equipo revisó y adaptó los borradores al microservicio Order Manager. |
| **ChatGPT/Codex (OpenAI)** | Apoyo para depurar el workflow CI/CD, interpretar resultados de pruebas y seguridad, revisar dependencias, mejorar la documentación técnica y contrastar evidencias con la rúbrica. | El equipo debe revisar y comprender los cambios antes de entregar; las reflexiones personales no se redactan con IA. La evidencia de CI se enlaza en la sección de reportes. |

### Trabajo personal del equipo
- Las **reflexiones individuales** y las conclusiones personales deben ser redactadas por cada integrante a partir de su propia experiencia, sin asistencia de IA.
- La declaración identifica las herramientas de IA utilizadas y las tareas en que apoyaron al equipo.

### Validación
La ejecución [#40427007011 de `main`](https://github.com/karlahp/order-manager-eva02/actions/runs/40427007011), del 27/10/2026 y commit `08e1fab`, terminó con resultado `success`: pasaron pruebas/cobertura, SonarCloud, renderizado de manifiestos Kubernetes, construcción Docker, escaneo Trivy, smoke test con Docker Compose y despliegue simulado. Se publicaron los artefactos `reportes-calidad`, `manifiestos-kubernetes` y `deployment-record`. En esa ejecución JaCoCo registró 96,72 % de líneas (177/183), por encima del mínimo bloqueante de 75 %. La imagen del commit `08e1fab` fue reconstruida y pasó el escaneo Trivy con bloqueo de severidad `HIGH,CRITICAL`; el paquete `brotli-libs` quedó en `1.2.1-r0` para corregir `CVE-2026-111497`. El registro `deployment-record` confirma que el despliegue simulado terminó correctamente. Esta evidencia corresponde al commit enlazado.

### Referencias
- Política institucional de uso de IA: <https://bibliotecas.duoc.cl/ia>

---

## 💭 Conclusiones y reflexiones individuales

Cada integrante debe completar su reflexión personal a partir de su propia experiencia, sin apoyo de IA. Extensión sugerida: 150–250 palabras.

### Reflexión — Karla Hoch

*(Escribir aquí.)*

Preguntas guía: ¿Qué aprendiste que no sabías antes? ¿Cuál fue el mayor desafío técnico y cómo lo resolviste? ¿Qué harías distinto? ¿Cómo contribuiste al proyecto?

---

### Reflexión — Sebastián Huaiquimilla

*(Escribir aquí.)*

Preguntas guía: ¿Qué concepto de DevOps comprendiste mejor al aplicar el pipeline? ¿Qué fue lo más complejo? ¿Cuál fue tu aporte concreto al equipo?

---

## 👥 Autores

| Nombre | Rol | GitHub |
|---|---|---|
| Karla Hoch | Integrante del equipo | [@karlahp](https://github.com/karlahp) |
| Sebastián Huaiquimilla | Integrante del equipo | [@rojasebastian](https://github.com/rojasebastian) |

**Nota sobre la autoría en Git:** Karla y Sebastián trabajaron juntos en un mismo computador. Los commits se registraron desde la cuenta de GitHub `karlahp`; por eso el historial muestra esa cuenta como autora de los cambios.

**Institución:** Duoc UC — Ingeniería en Informática
**Asignatura:** DOY0101 — Ingeniería DevOps
