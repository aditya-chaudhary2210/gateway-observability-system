# Failure Handling and Resilience

<!--
  Ownership: Platform SRE-aligned architecture
-->

## 1. Philosophy

Failures in observability pipelines are **guaranteed**. Design goals:

1. **Contain** blast radius (no cascading synchronous pile-ups).
2. **Measure** degradation (explicit metrics vs silent loss).
3. **Recover** via retries with jitter and idempotent sinks.
4. **Prefer** shedding load over risking OOM.

---

## 2. Resilience Patterns by Layer

### 2.1 API Gateway Service

| Pattern | Library / Mechanism | Config knobs |
|---------|---------------------|---------------|
| **Timeouts** | Resilience4j TimeLimiter | per-route overrides |
| **Circuit breaker** | Resilience4j | failureRateThreshold slowCallThreshold |
| **Bulkhead** *(optional)* | Resilience4j | limit concurrent upstream |
| **Rate limit** | Redis token bucket | per-subject/route |
| **Kafka producer resilience** | `retries`, `linger.ms`, bounded buffer monitor | alerting on record-error-rate |

Gateway **must never block** waiting for telemetry persistence success.

### 2.2 Ingestion Pipeline

| Failure | Response |
|---------|----------|
| Transient ES bulk failure | Bounded exponential backoff retry ([RETRY](../streaming/RETRY_STRATEGY.md)) |
| Postgres unavailable | Pause consumption / backoff (consumer stalled metric) |
| Poison message | Route to DLQ after N attempts |

### 2.3 Detection Service

| Failure | Response |
|---------|----------|
| Redis flake | degrade rule tier (fewer histograms); alert |
| Model evaluation error | swallow sub-score; never block ingestion |

### 2.4 AI Service

| Failure | Response |
|---------|----------|
| LLM timeout | Partial summary or mark `rca_status=FAILED` |
| Token budget exceeded | Evidence truncation policy + log surfaced reason |

---

## 3. Error Taxonomy

| Category | Codes (example naming) | Client visibility |
|----------|-------------------------|-------------------|
| Auth | `AUTH_DENIED`, `JWT_EXPIRED` | 401/403 |
| Validation | `VALIDATION_FAILED` | 400 |
| Upstream | `UPSTREAM_TIMEOUT` | 502/504 gateway mapping |
| Policy | `RATE_LIMITED` | 429 |
| Internal | `INTERNAL_ERROR` | 500 sanitized message |

Correlation ID returned in **`X-Correlation-Id`** consistently.

---

## 4. Compensating Actions

No automatic saga compensation across telemetry duplication in MVP. Manual ops:

- Replay Kafka topic segments after fixing consumer bug.
- Rebuild ES index via batch job (TECH_DEBT: reindex playbook).

---

## 5. Disaster & Local Dev Degradation

| Mode | Behaviour |
|------|-----------|
| **Local compose partial** | Service refuses readiness if mandatory deps absent (explicit profile `minimal`) |
| **AI disabled** | Feature flag skips LLM endpoints |

---

## 6. Testing Failures

- Gateway failure simulation toggles (**non-default prod**): latency injection, weighted error responses upstream.
- Contract tests asserting retry headers not required on Kafka paths but **metrics** assertions in integration tests.

---

## 7. Related Documents

- [../streaming/DLQ_STRATEGY.md](../streaming/DLQ_STRATEGY.md)
- [../streaming/RETRY_STRATEGY.md](../streaming/RETRY_STRATEGY.md)
- [../engineering/ERROR_HANDLING_GUIDELINES.md](../engineering/ERROR_HANDLING_GUIDELINES.md)
