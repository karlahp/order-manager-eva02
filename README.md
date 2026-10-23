# 📦 Order Manager API

[![CI/CD Pipeline](https://github.com/USUARIO/REPOSITORIO/actions/workflows/ci-cd.yml/badge.svg)](https://github.com/USUARIO/REPOSITORIO/actions/workflows/ci-cd.yml)
[![Java](https://img.shields.io/badge/Java-17-blue.svg)](https://openjdk.org/projects/jdk/17/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.4-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Docker](https://img.shields.io/badge/Docker-multi--stage-blue.svg)](https://www.docker.com/)

Microservicio REST para la gestión de pedidos de un e-commerce, desarrollado para la asignatura **Ingeniería DevOps (DOY0101)** de Duoc UC — **Evaluación Parcial N°2: "Añadiéndole complejidad a nuestro pipeline"**.

El proyecto automatiza todo el ciclo de vida del microservicio: **pruebas → análisis de seguridad → contenedor → despliegue → orquestación**, con trazabilidad desde el commit hasta el contenedor en ejecución.

> ⚠️ **Antes de publicar:** reemplazar `USUARIO/REPOSITORIO` en los badges, completar la sección [Autores](#-autores) y seguir la [Configuración inicial](#-configuración-inicial-en-github).

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
| **IE2** Pruebas automatizadas | Pruebas unitarias en CI según políticas de calidad | 60+ pruebas JUnit 5/Mockito + integración (`src/test`), cobertura mínima **70 %** forzada por JaCoCo, job `test` |
| **IE3** Escalabilidad y seguridad | Dependabot + SonarQube/Snyk + alertas/bloqueos | [`dependabot.yml`](.github/dependabot.yml), job `sonar` (Quality Gate bloqueante), job `alert`, contenedor endurecido, límites de recursos, HPA |
| **IE4** Despliegue y trazabilidad | Deploy automático en entorno simulado + README | job `deploy` (Docker Compose), imagen etiquetada con el SHA, `/actuator/info`, registro de despliegue |
| **IE5** Orquestación | Kubernetes o Docker Compose | [`docker-compose.yml`](docker-compose.yml) **y** manifiestos en [`k8s/`](k8s/) (réplicas, probes, HPA, PVC) |

---

## 🛠 Stack tecnológico

| Capa | Tecnología |
|---|---|
| Lenguaje / framework | Java 17, Spring Boot 3.3.4 (Web, Data JPA, Validation, Actuator) |
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

Consola H2: `http://localhost:8098/h2-console` · JDBC URL `jdbc:h2:mem:orderdb` · usuario `sa` · sin contraseña.

### Opción 2 — Docker Compose (app + PostgreSQL)

```bash
cp .env.example .env                 # definir credenciales (cambiar la clave)
docker compose up -d --build         # construir imagen y levantar
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
   │ • JaCoCo ≥ 70 %  ⛔ │             │   bloqueante     ⛔  │
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
                  │   (+ SBOM y procedencia)
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
| `build` | Construir, verificar y publicar la imagen | Docker Buildx, GHCR | IE1 |
| `deploy` | Desplegar y verificar en entorno simulado | Docker Compose, `smoke-test.sh` | IE4 |
| `alert` | Notificar fallos con un Issue | `actions/github-script` | IE3 |

### Bloqueos y alertas (IE3)

| Si falla… | Consecuencia |
|---|---|
| Un test, o la cobertura baja del 70 % | `test` falla → no se construye ni se despliega |
| El Quality Gate de SonarCloud | `sonar` falla → no se construye ni se despliega |
| Falta el `SONAR_TOKEN` en un push | el pipeline **falla** (la seguridad nunca se omite en silencio) |
| La imagen corre como root o su smoke test falla | `build` falla → no se publica ni se despliega |
| El health check o el smoke test del despliegue | `deploy` falla → se limpia el entorno y salta la alerta |
| Cualquier etapa en un `push` | job `alert` abre un **Issue** automático (y GitHub envía correo) |
| Dependabot detecta una vulnerabilidad | abre un PR; ese PR debe pasar todo el pipeline para poder integrarse |

---

## 🔧 Configuración inicial en GitHub

Estos pasos se hacen **una sola vez** en el repositorio (no se pueden versionar en el código):

1. **SonarCloud** ([sonarcloud.io](https://sonarcloud.io)) → iniciar sesión con GitHub → *Analyze new project* → seleccionar el repositorio.
   En *Administration › Analysis Method* desactivar **Automatic Analysis** (el análisis lo hace el pipeline).
2. En GitHub: **Settings › Secrets and variables › Actions**
   - **Secret** `SONAR_TOKEN` → token generado en SonarCloud (*My Account › Security*).
   - **Variables** `SONAR_ORGANIZATION` y `SONAR_PROJECT_KEY` → valores que muestra SonarCloud.
3. **Settings › Code security**: activar *Dependency graph*, *Dependabot alerts* y *Dependabot security updates*.
4. **Settings › Actions › General › Workflow permissions**: *Read and write permissions* (necesario para publicar en GHCR y abrir Issues).
5. **Settings › Branches**: regla de protección para `main` exigiendo que los checks `Pruebas y cobertura`, `SonarCloud (Quality Gate)` e `Imagen Docker` pasen antes de hacer merge. *(Es el bloqueo "duro" a nivel de repositorio.)*
6. *(Opcional)* **Settings › Environments › simulated-cloud**: agregar *Required reviewers* para exigir aprobación manual antes de desplegar.

> Los PR creados por Dependabot o desde forks no reciben secrets; en ellos el análisis de Sonar se omite con una advertencia y se ejecuta al integrar en `main`.

---

## 🛡 Seguridad y escalabilidad (parámetros ajustados)

### Seguridad

| Parámetro | Dónde | Valor | Justificación |
|---|---|---|---|
| Permisos del token | `ci-cd.yml` | `contents: read` global; cada job amplía lo mínimo | Mínimo privilegio |
| Quality Gate | job `sonar` | `sonar.qualitygate.wait=true` | Bloquea ante bugs/vulnerabilidades |
| Cobertura mínima | `pom.xml` | `jacoco.line.minimum = 0.70` | Política de calidad verificable |
| Usuario del contenedor | `Dockerfile` | UID 10001 (no root), verificado en CI | Limita el daño ante una intrusión |
| Imagen base | `Dockerfile` | `eclipse-temurin:17-jre-alpine` | Menor superficie de ataque (sin JDK ni Maven) |
| Sistema de archivos | Compose / K8s | `read_only` / `readOnlyRootFilesystem` + `/tmp` en memoria | Evita persistencia de malware |
| Capacidades Linux | Compose / K8s | `cap_drop: ALL`, `no-new-privileges` | Sin privilegios extra |
| Credenciales | `.env`, Secret, GitHub Secrets | Nunca en el repositorio; en deploy se generan efímeras | Gobernanza de secretos |
| Salud sin detalles | `application-docker.properties` | `show-details=never` | No filtra información interna |
| Dependencias | `dependabot.yml` | Maven, Docker, Compose y Actions | Parches de seguridad continuos |
| Cadena de suministro | job `build` | SBOM + procedencia en la imagen | Auditoría de componentes |

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
| Caché de build | `Dockerfile` + CI | capas de Maven y JAR en capas + `cache-to: gha` | Builds rápidos |

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
3. **Imagen (GHCR):** etiquetada con el **SHA completo del commit** y con labels OCI (`org.opencontainers.image.revision`). Incluye SBOM y procedencia.
4. **Despliegue:** el SHA se inyecta en la imagen (`APP_COMMIT_SHA`) y la aplicación lo expone en `/actuator/info`. El **smoke test compara** ese valor con el commit del pipeline: si no coinciden, el despliegue falla. Así se demuestra que *lo que corre es exactamente lo que se commiteó*.
5. **Historial:** GitHub Environments (`simulated-cloud`) registra cada despliegue; el artefacto `deployment-record.json` guarda commit, autor, rama, run, imagen, digest y fecha (90 días).

```bash
docker inspect order-manager-app --format '{{.Config.Image}}'
curl -s localhost:8098/actuator/info | jq .app.commit
```

### Calidad (capas)

| Capa | Herramienta | Criterio |
|---|---|---|
| Pruebas unitarias y de integración | JUnit 5 + Mockito + MockMvc | todas deben pasar |
| Cobertura | JaCoCo | ≥ 70 % de líneas (bloqueante) |
| Análisis estático | SonarCloud | Quality Gate aprobado (bloqueante) |
| Dependencias | Dependabot | PRs automáticos + alertas |
| Validación de entrada | Jakarta Validation | `@NotBlank`, `@Min`, `@Max`, `@DecimalMin`… |
| Errores | `@RestControllerAdvice` | formato JSON uniforme |
| Verificación del contenedor | pipeline | no-root + smoke test funcional |

### Reportes disponibles
- **Surefire + JaCoCo:** artefacto `reportes-calidad` en cada ejecución (y resumen en la pestaña *Summary* del run).
- **Dashboard de SonarCloud:** calidad, cobertura, deuda técnica.
- **Historial de despliegues:** pestaña *Actions* y *Environments* de GitHub.

---

## 🤖 Uso de Inteligencia Artificial

> ✏️ **Revisar y ajustar esta sección antes de entregar** para que refleje fielmente lo que hizo el equipo.

### Herramientas utilizadas

| Herramienta | Uso | Aplicación |
|---|---|---|
| **Claude (Anthropic)** | Generación de borradores de la configuración DevOps (Dockerfile, workflow de GitHub Actions, Docker Compose, manifiestos de Kubernetes, `smoke-test.sh`, pruebas de integración) y de la estructura de este README | Los borradores fueron revisados, probados y adaptados por el equipo |

### Qué NO se delegó a la IA
- ❌ Las **reflexiones individuales** (sección siguiente) fueron redactadas por cada integrante sin IA.
- ❌ Las **conclusiones** y la valoración personal del aprendizaje.
- ❌ El código de negocio del microservicio (desarrollado en la evaluación anterior).

### Validación
Todo contenido asistido por IA fue **revisado** por el equipo, **ejecutado y verificado** (pipeline en verde en GitHub Actions) y **ajustado** a los requisitos de la rúbrica.

### Referencias
- Política institucional de uso de IA: <https://bibliotecas.duoc.cl/ia>

---

## 💭 Conclusiones y reflexiones individuales

> ⚠️ **Obligatorio:** cada integrante debe redactar su reflexión **sin apoyo de IA** (aprendizaje y contribución personal). Extensión sugerida: 150–250 palabras.

### Reflexión — [Nombre del Integrante 1]

*(Escribir aquí.)*

Preguntas guía: ¿Qué aprendiste que no sabías antes? ¿Cuál fue el mayor desafío técnico y cómo lo resolviste? ¿Qué harías distinto? ¿Cómo contribuiste al proyecto?

---

### Reflexión — [Nombre del Integrante 2]

*(Escribir aquí.)*

Preguntas guía: ¿Qué concepto de DevOps comprendiste mejor al aplicar el pipeline? ¿Qué fue lo más complejo? ¿Cuál fue tu aporte concreto al equipo?

---

## 👥 Autores

| Nombre | Rol | GitHub |
|---|---|---|
| [Nombre 1] | [ej. DevOps / Pipeline] | [@usuario1](https://github.com/usuario1) |
| [Nombre 2] | [ej. Testing / Documentación] | [@usuario2](https://github.com/usuario2) |

**Institución:** Duoc UC — Ingeniería en Informática
**Asignatura:** DOY0101 — Ingeniería DevOps
