# Fase 8 — Orquestación (IE5)

**Entregables:** `docker-compose.yml`, `.env.example` y carpeta `k8s/`.

**Docker Compose:** servicios `app` + `db`, red privada, volumen persistente, `depends_on` con `service_healthy`, límites de CPU/memoria, `read_only`, `cap_drop: ALL`, `no-new-privileges`. Las credenciales se leen de `.env` (ignorado por Git).

**Kubernetes (Kustomize):** namespace, ConfigMap, StatefulSet de PostgreSQL con PVC, Deployment (2 réplicas, RollingUpdate sin caída, probes, `initContainer`, `securityContext` endurecido), Service, HPA (2–5 réplicas al 70 % de CPU) y PodDisruptionBudget. El Secret se crea en el clúster; `secret.example.yaml` solo documenta el formato.
