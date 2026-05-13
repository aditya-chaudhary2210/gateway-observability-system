# Low-Level Design (LLD)

<!--
  Ownership: Platform Architecture + Service Leads
  Purpose: Translate HLD containers into modules, synchronous APIs, persistence patterns, and scaling knobs.
-->

## 1. LLD Principles

**Why:** HLD avoids implementation; LLD avoids *code* but pins **interfaces** so implementation stays modular (SOLID, ports/adapters).

- **Ports & adapters** in Java services (`api`, `application`, `domain`, `infrastructure`).
- **Explicit versioning** on external APIs (`/api/v1`) and Kafka payload `schema_version`.
- **Back-pressure** everywhere we cross process boundaries (WebFlux/Reactor Kafka, bounded queues).

---

## 2. Module Map per Service

### 2.1 API Gateway Service (`spring-cloud-gateway`)

| Package / Layer | Responsibility |
|-----------------|----------------|
| `bootstrap` | Route definitions, Security filter chain, GatewayGlobalFilters |
| `api` *(gateway filters)* | JWT validation filter, correlation ID injection, metrics filter |
| `application` | Rate limit policy facade, simulation toggles; use-case orchestration reading config |
| `infrastructure` | Kafka producer bean, Redis rate limit repo, resilience registries |

**Key technical choices:**

- **JWT:** Resource-server style validation (issuer, audience); optional JWKS caching.
- **Rate limiting:** Redis token bucket via Spring Data Redis reactive or Bucket4j variant (exact lib TBD — log in DECISION_LOG).
- **Failure simulation:** Feature-flagged routes/filters injecting latency/5xx (**non-prod default**).

**Outbound telemetry:**

- Structured log line per phase (received, routed, upstream response) **or** consolidated single span-like record (preference: single structured record + trace span to minimise log volume).

### 2.2 Log Ingestion Service

| Component | Behaviour |
|-----------|-----------|
| `KafkaConsumerGatewayAccess` | Subscribe `gateway-access-v1` consumer group `ingestion.v1` |
| `NormalizationPipeline` | JSON → canonical DTO (`EnrichedGatewayEvent`) |
| `EnrichmentService` | IP/geo (**mockable**), user-agent parse, severity from status/latency |
| `ElasticsearchWriter` | Bulk index async with retry policy ([RETRY](../streaming/RETRY_STRATEGY.md)) |
| `PostgresProjectionWriter` | Idempotent upsert keyed by `(event_id)` |
| `Forwarder` *(optional)* | Publish to `telemetry-enriched-v1` |

**Checkpointing:**

- Combine Kafka **consumer offsets** with **processed_event** dedupe table in Postgres for at-least-once → effectively-once writes (see postgres doc).

### 2.3 Incident Detection Service

| Stage | Inputs | Outputs |
|-------|--------|---------|
| **Rule Engine** | Enriched stream / rolling windows in Redis | `threshold_breaches` internal |
| **Anomaly Detector** | Latency percentiles (approx), error spike | anomaly scores |
| **Clustering** | Recent failure fingerprints | Incident candidate groups |
| **Incident Aggregator** | Candidates | Persisted Incident + emits `incidents-v1` |

**State:**

- Rolling windows — Redis (`ZSET`/time buckets) with TTL aligned to detection horizon (e.g. 15 min / 1 h tiers).

### 2.4 AI Analysis Service (FastAPI)

| Route Group | Responsibility |
|-------------|----------------|
| `/api/v1/rca/incidents/{id}` | Generate/regenerate RCA for incident |
| `/api/v1/incidents/similar` | Embedding + kNN hybrid with metadata filters |
| `/api/v1/prioritize` | Scoring augmentation for alerting |

Internals:

- LangChain **LCEL-style** pipelines (conceptually) orchestrating prompt templates + tool calls to Postgres/ES for evidence bundle assembly.

---

## 3. API Surface (Draft Contract Summary)

Formal OpenAPI stubs land under `contracts/rest/` later; **truth for naming** stays here until generated.

### 3.1 Gateway-Obs Dashboard BFF *(could live on gateway or dedicated read service)*

**Decision stub:** MVP may expose reads via **ingestion/read API** façade to avoid gateway bloat — record final choice in DECISION_LOG.

| Method | Path | Notes |
|--------|------|-------|
| GET | `/api/v1/incidents` | Paginated, filters (severity, time, route) |
| GET | `/api/v1/incidents/{id}` | Detail + RCA summary pointer |
| GET | `/api/v1/traces/{traceId}` | Aggregated view (*may proxy OTel/Jaeger backend in Phase 7*) |
| GET | `/api/v1/metrics/summary` | Pre-aggregates or Prometheus metric proxies |

### 3.2 AI Service

| Method | Path |
|--------|------|
| POST | `/api/v1/rca/incidents/{id}/generate` |
| GET | `/api/v1/rca/incidents/{id}` |
| POST | `/api/v1/incidents/similarity` |

---

## 4. Persistence Interaction Matrix

| Writer | Postgres | Redis | Elasticsearch |
|--------|----------|-------|---------------|
| Ingestion | Event projection, checkpoints | Dedup cache optional | Raw + searchable docs |
| Detection | Incident rows, incident_events | Rolling windows | Optional signal index |
| AI | RCA report, embeddings metadata | Prompt cache | Similarity lexical fallback |

Detailed schemas: `../database/*`.

---

## 5. Concurrency & Threading Models

| Service | Model |
|---------|-------|
| Gateway | Netty reactive; blocking calls isolated |
| Ingestion | Reactor Kafka / listener container with bounded concurrency |
| Detection | Partition-aware consumers (order per route key desirable) |
| AI | Async HTTP workers; bounded threadpool for blocking LLM I/O |

---

## 6. Configuration Management

-hierarchical YAML + env overrides (`SPRING_*`, `APP_*`).
- Feature flags prefix: `obs.failuresimulation.enabled`, `obs.ai.enabled`.

---

## 7. Local vs Cluster Differences

| Aspect | Docker Compose Local | Kubernetes |
|--------|----------------------|------------|
| Kafka brokers | Single-broker tolerated | RF=3 prod target |
| ES/OS | Single node | Dedicated masters + data tiers (future) |
| Secrets | `.env` (gitignored) | SealedSecrets/ExternalSecrets (future doc) |

---

## 8. Testing Strategy Hooks

- Contract tests against `contracts/async` schemas.
- Testcontainers suites: Kafka + Postgres + Redis + ES.
- Chaos: failure simulator in gateway toggled only in `test` profile.

---

## 9. Related Documents

- [SERVICE_BOUNDARIES.md](./SERVICE_BOUNDARIES.md)
- [FAILURE_HANDLING.md](./FAILURE_HANDLING.md)
- [../streaming/KAFKA_TOPICS.md](../streaming/KAFKA_TOPICS.md)
- [../infra/OBSERVABILITY_STACK.md](../infra/OBSERVABILITY_STACK.md)
