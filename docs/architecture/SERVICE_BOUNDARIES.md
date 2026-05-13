# Service Boundaries and Ownership

<!--
  Ownership: Platform Architecture
  Purpose: Prevent “god services” and clarify allowed dependencies for SDE-2 resume narrative.
-->

## 1. Why Boundaries Matter

Microservices without explicit dependency rules regress into **distributed monoliths**. This document defines **ownership**, **allowed dependencies**, **scaling unit**, **failure containment**, and **observability minimums**.

---

## 2. Catalog of Deployables

| Service | Owner | Primary Responsibility |
|---------|-------|-------------------------|
| `api-gateway-service` | Edge Team *(logical)* | AuthN/Z at edge, traffic policy, telemetry emission, resilience patterns |
| `log-ingestion-service` | Telemetry Team | Consume raw gateway events → enrich → index → relational projections |
| `incident-detection-service` | Reliability Analytics | Incident lifecycle creation/updates from signals |
| `ai-analysis-service` | AI Platform *(logical)* | RCA + similarity + prioritization **only after gating** |
| `dashboard-web` | Frontend | Visualization; consumes public/read APIs |

**Optional later:** `notification-edge-service`, `bff-read-service`.

---

## 3. Allowed Dependency Graph

Allowed direction (**→** reads as “may call / depend on”):

```
dashboard-web → api-gateway-service (routes to BFF/read façade)
api-gateway-service → Redis, KafkaProducer, JWKS/Issuer
log-ingestion-service → KafkaConsumer, Postgres, ES/OS, KafkaProducer(for derived topics)
incident-detection-service → KafkaConsumer/Producer, Postgres, Redis
ai-analysis-service → KafkaConsumer *(optional batch)* REST triggered, Postgres, Redis, ES/OS, LLM Endpoint
```

**Forbidden (initial phases):**

- `log-ingestion-service` → **`ai-analysis-service` HTTP** *(creates brittle sync coupling)* — use events instead.
- `incident-detection-service` ← **dashboard direct DB access** *(bypass)*.
- Circular synchronous call chains Gateway → Ingestion → Gateway.

---

## 4. Ownership of Data

| Dataset | Owning Writer | Consumers |
|---------|---------------|-----------|
| Raw gateway telemetry index | Ingestion | Dashboard (read-only), Detection (signals) |
| Relational telemetry projection | Ingestion | Detection, Dashboard |
| Incident aggregate | Detection | Dashboard, AI (read + append RCA artefacts) |
| RCA artefacts | AI | Dashboard |
| Rate limit counters | Gateway | Gateway |
| Rolling detection windows | Detection | Detection |

Cross-service **CQRS**: other services subscribe via DB read replicas or Kafka events — prefer Kafka for loosely coupled freshness.

---

## 5. Scaling Considerations

| Service | Stateless? | Horizontal scale trigger | Bottleneck hints |
|---------|------------|--------------------------|------------------|
| Gateway | Mostly | CPU, conn count, producer lag backlog | Kafka buffer, JWT crypto |
| Ingestion | Consumer group | Partition count, ES bulk latency | Bulk indexing |
| Detection | Consumer + Redis memory | Spike evaluation CPU | Redis hot keys mitigation via sharded keys |
| AI | Stateless API pods | Queue depth / LLM rate limits | Token budget enforcement |

---

## 6. Failure Modes & Degradation

| Service Failure | Expected behaviour |
|-----------------|---------------------|
| AI down | Incidents continue; RCA panel shows **degraded — rules only** badge |
| ES down | Search degraded; Postgres still drives incident table |
| Kafka slow | Gateway buffers with back-pressure; timeouts → circuit break & shed load |
| Redis down | Rate limit fail-open *(configurable)* or fail-closed (document per env policy) |

Policy choice recorded in DECISION_LOG when implemented.

---

## 7. Observability Hooks (Per Service Minimum)

Each service MUST expose:

- `/actuator/health` + `/actuator/readiness`
- Prometheus scrape (`/actuator/prometheus` or OTel Metrics bridge)
- Structured logs JSON with **`service.name`**, **`correlation_id`**, **`trace_id`**

---

## 8. Related Documents

- [HLD.md](./HLD.md)
- [LLD.md](./LLD.md)
- [../infra/OBSERVABILITY_STACK.md](../infra/OBSERVABILITY_STACK.md)
