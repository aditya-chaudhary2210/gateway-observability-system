# Local Development Strategy

<!--
  Ownership: Platform / DX
-->

## 1. Objectives

**Fast iteration** without compromising **parity** with production patterns (Kafka consumer groups, multi-container deps, actuator metrics).

Profiles balance:

- Minimal laptop (`cpu`) vs **full fidelity** demo stack.

---

## 2. Prerequisites

| Tool | Version hint |
|------|----------------|
| Docker Desktop / Colima | Current stable |
| JDK | Temurin 21 |
| Maven | 3.9+ |
| Node | 20 LTS *(dashboard)* |
| Python | 3.12 *(AI)* |
| Optional | `kafka-topics`, `jq`, `grpcurl` |

---

## 3. Profiles (Compose)

Recommended `Makefile`/`justfile` wrappers later:

| Profile | Components | Purpose |
|---------|--------------|---------|
| `infra` | Kafka, Postgres, Redis, OpenSearch, Prometheus, Grafana | Shared foundation |
| `apps` | Java services + AI + dashboard | Integrated manual test |
| `obs` *(optional overlap)* | Extra Jaeger/collector Phase 7 | Trace validation |

Cold start sequencing: infra healthy checks before apps (depends_on health hooks).

---

## 4. Service Run Modes

| Service | Typical dev workflow |
|---------|-----------------------|
| Gateway | `./mvnw -pl services/api-gateway-service spring-boot:run` binding host debug port 5005 |
| Ingestion / Detection | same pattern scoped modules |
| AI | `uvicorn app.main:app --reload` inside `ai-analysis-service` |
| Dashboard | `pnpm dev` or `npm run dev` Vite |

Environment variables wired via `.env` consumed by Compose **and** optional `docker-compose.override.yml` excluded from git.

---

## 5. Seeds & Fixtures

Provide:

- JWKS stub or symmetric dev key **explicitly insecure** flagged.
- Scripted topic creation aligning [KAFKA_TOPICS.md](../streaming/KAFKA_TOPICS.md).

---

## 6. Debugging Correlation Flow

Mandatory manual smoke:

1. `curl` gateway `GET /health` (**not always auth-guarded**) or dedicated sample route with JWT developer token.
2. Extract `traceparent`/`X-Correlation-Id` response headers.
3. Locate matching row in Postgres `gateway_event_projection` + ES `_id`.

Document failure triage snippets in KNOWN_ISSUES as blockers emerge.

---

## 7. Performance on Laptops

If OpenSearch dominates RAM:

- Start with reduced heap flags per [DOCKER_STRATEGY.md](../infra/DOCKER_STRATEGY.md).
- Alternatively profile `infra-lite` delaying ES bring-up for pure gateway kafka tests.

---

## 8. Pre-Commit Expectations

Local mirror of CI (**when wired** Phase 8): formatters + unit tests selective module scope.

Minimal interim: `./mvnw -pl <module> test`.

---

## Related

- [../infra/DOCKER_STRATEGY.md](../infra/DOCKER_STRATEGY.md)
- [../tracking/IMPLEMENTATION_PROGRESS.md](../tracking/IMPLEMENTATION_PROGRESS.md)
