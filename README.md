# AI-Powered API Gateway Observability Platform

Portfolio-grade **distributed observability + incident intelligence** stack: gateway telemetry → Kafka → ingestion → detection → RCA service → dashboard, with Prometheus/Grafana and a Compose-first local path to Kubernetes.

Implementation now lives alongside the documentation set:
- **Spring Cloud Gateway** emits versioned Kafka envelopes, enforces Redis-backed rate limits (demo profile), optional chaos headers, and proxies read traffic to ingestion + AI.
- **Log ingestion** persists to PostgreSQL + Elasticsearch, replays enriched telemetry for detection.
- **Incident detection** applies threshold rules, opens incidents, emits `incidents-v1` events.
- **AI service (FastAPI)** ships deterministic RCA scaffolding ready for LangChain providers.
- **React dashboard** polls `/api/v1/incidents/**` via nginx → gateway.

## Run the stack locally

Prerequisites: Docker Compose v2 + ~8GB RAM.

```bash
docker compose -f deploy/docker-compose/docker-compose.yml up --build
```

Service map (host ports):
- API gateway `8080`
- Dashboard (nginx) `4173`
- Grafana `3000` _(admin/admin demo creds)_
- Prometheus `9090`
- Postgres `5432`, Redis `6379`, Elasticsearch `9200`, Kafka `9092`

Smoke traffic:

```bash
curl -i http://localhost:8080/mock/demo
curl -i -H "X-Chaos-Status:503" http://localhost:8080/mock/error
```

## Repo map

| Path | Purpose |
|------|---------|
| `services/*` | JVM microservices (Spring Boot 3.4 / Java 21) |
| `ai-analysis-service/` | FastAPI RCA façade |
| `dashboard-web/` | Vite + React + Tailwind operator UI |
| `deploy/docker-compose/` | Local full stack + Prometheus scrape config |
| `contracts/` | Async/REST contract seeds (JSON Schema + OpenAPI stubs) |
| `docs/` | Persistent architecture + operational context |

## Documentation hub


Open **[`docs/README.md`](docs/README.md)** for the full index.

Quick links:

| Area | Doc |
|------|-----|
| High-level architecture | [`docs/architecture/HLD.md`](docs/architecture/HLD.md) |
| Target repo layout | [`docs/REPOSITORY_FOLDER_STRUCTURE.md`](docs/REPOSITORY_FOLDER_STRUCTURE.md) |
| Kafka / DLQ / retry | [`docs/streaming/KAFKA_TOPICS.md`](docs/streaming/KAFKA_TOPICS.md), [`DLQ`](docs/streaming/DLQ_STRATEGY.md), [`Retry`](docs/streaming/RETRY_STRATEGY.md) |
| Session handoff | [`docs/tracking/IMPLEMENTATION_PROGRESS.md`](docs/tracking/IMPLEMENTATION_PROGRESS.md), [`DECISION_LOG`](docs/tracking/DECISION_LOG.md) |

## License

See [`LICENSE`](LICENSE).
