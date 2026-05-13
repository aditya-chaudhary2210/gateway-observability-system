# Implementation Progress

<!--
  Ownership: All contributors — update at end of every meaningful session
  Last updated: 2026-05-13 (phase-1 backlog: gateway IT, OTEL FastAPI, Tempo/Grafana CI)
-->

## Phase Overview

| Phase | Status | Notes |
|-------|--------|-------|
| 1 — Documentation & architecture | **Complete** | `/docs/**` authoritative |
| 2 — Docker / local infra | **Expanded** | Compose: Jaeger + **Tempo** (AI OTLP), Grafana datasource provisioning |
| 3 — API Gateway + producer | **MVP** | Gateway emits `gateway-access-v1` (Kafka); HTTP→Kafka Testcontainers IT removed (flakey CI) |
| 4 — Ingestion + persistence | **MVP + IT** | `integration/gateway-pipeline-it` — Kafka→Postgres/ES→incident |
| 5 — Detection + AI | **Improved** | Evidence RCA (ingestion API + ES) + Groq/Ollama; **OpenTelemetry OTLP→Tempo** |
| 6 — Dashboard | **MVP SPA** | Vite/React/Tailwind + nginx `/api/*` gateway proxy |
| 7 — Observability deep integration | **Improved** | Java OTLP→Jaeger; Python OTLP→Tempo; Grafana Jaeger + Tempo + Prometheus datasources |
| 8 — K8s + CI/CD hardening | **Improved** | CI `mvn verify` exercises Testcontainers modules on GitHub (Docker socket) |

## Completed Modules

- [x] Monorepo parent POM (`pom.xml`) + `platform/observability-java`
- [x] `services/api-gateway-service`
- [x] `services/log-ingestion-service` (+ Flyway `V1__init_schema.sql`)
- [x] `services/incident-detection-service`
- [x] `ai-analysis-service` RCA + OTLP instrumentation
- [x] `integration/gateway-pipeline-it`
- [x] `dashboard-web`
- [x] Compose stack + Prometheus + **Grafana provisioning** (`deploy/docker-compose/grafana/provisioning`)
- [x] Contracts seed (`contracts/async/*.schema.json`)
- [x] CI workflow (`mvn verify` with integration tests)

## In Progress / Next Tasks

1. Tune Tempo HA / retention for non-local deployments; validate `tempo.yaml` against upgrades.
2. Optional: OTEL Collector sidecar fan-out vs dual backends (Jaeger + Tempo) for pure OTLP neutrality.
3. Implement JWT-backed security profile toggled off `local`/`docker` defaults.
4. Introduce reproducible frontend lockfile (`package-lock.json`) or switch to PNPM pinned store.

## Blockers

- Local validation still requires Docker for Testcontainers (gateway + pipeline IT modules).

## Operational Smoke (developer laptop)

```bash
# Terminal A — infra + apps (from repo root)
docker compose -f deploy/docker-compose/docker-compose.yml up --build

# Terminal B — generate gateway traffic → Kafka → ingestion → detection
curl -i http://localhost:8080/mock/ping
curl -i -H "X-Chaos-Status:503" http://localhost:8080/mock/chaos-demo
```

Then open Grafana `http://localhost:3000` (admin/admin): **Prometheus**, **Jaeger** (Java traces), **Tempo** (FastAPI AI traces), and SPA `http://localhost:4173`.

---

**Handoff discipline:** replicate these bullets inside PR summaries; update TECH_DEBT + DECISION_LOG when modifying contracts or dual-write semantics.
