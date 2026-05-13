# Docker Strategy

<!--
  Ownership: Platform
-->

## 1. Goals

Unified **local fidelity** vs **minimal laptop burn**, using multi-stage Dockerfiles and Compose profiles:

- `infra` profile: kafka, zk optional (KRaft mode preferred), postgres, redis, opensearch.
- `apps` profile: java services + AI + nginx-served SPA (or dev server split).
- `obs` profile: prometheus, grafana, optional jaeger/collector.

---

## 2. Image Build Patterns

### Java Services

- **Stage 1:** `maven:3-eclipse-temurin-21` build multi-module with layer caching (`--mount=cache`).
- **Stage 2:** `eclipse-temurin:21-jre-alpine` (or distroless aspiration) slim runtime.

Non-root user `appuser`; read-only root FS where feasible.

### AI Service

- `python:3.12-slim` + `uv` or `pip` freeze; non-root; healthcheck `curl /health`.

### Dashboard

- `node:20` builder + `nginx:alpine` static OR `node` dev for hot reload local only.

---

## 3. Compose Networking

Single bridge `obs_net`; internal DNS hostnames: `kafka`, `postgres`, `redis`, `opensearch`.

---

## 4. Volume Strategy

| Component | Volume |
|-----------|--------|
| Postgres | Named volume persistent dev |
| Kafka | Ephemeral acceptable quick dev; named for replay demos |
| ES | Named for index survival across restarts |

---

## 5. Resource Baselines (Guidance)

Initial laptop-friendly caps (tune):

- ES: `-Xms512m -Xmx512m` demo only.
- Kafka: single broker partition count conservative.

---

## 6. Security

- No default weak passwords in committed compose — use `.env` from template.
- Scan images in CI (Trivy) Phase 8.

---

## 7. Related

- [DEPLOYMENT_ARCHITECTURE.md](./DEPLOYMENT_ARCHITECTURE.md)
- [K8S_STRATEGY.md](./K8S_STRATEGY.md)
