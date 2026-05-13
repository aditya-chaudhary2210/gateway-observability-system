# Retry Strategy

<!--
  Ownership: Streaming + Application Resilience
-->

## 1. Goals

Retries must be:

- **Bounded** — max attempts + timeouts.
- **Jittered** — avoid thundering herds.
- **Measurable** — metrics on attempts & exhaustion.
- **Safe** — idempotent sinks (Postgres UPSERT, ES bulk item idempotent).

---

## 2. Layered Policies

### 2.1 Kafka Producer (Gateway → Kafka)

Kafka client built-in retries for transient broker errors.

Additional application-level guard: circuit break if prolonged failures (metric-based).

### 2.2 Kafka Consumer (Ingestion / Detection)

Spring Kafka / Reactor defaults tuned:

| Setting | Guidance |
|---------|----------|
| `max.poll.interval.ms` | Accommodate long ES bulk (raise cautiously) |
| `Backoff` handler | exponential `initial=500ms multiplier=2 max=30s jitter=20%` |
| Retry topic pattern | OPTIONAL alternative to in-process backoff — TECH_DEBT evaluation |

After exhaustion → DLQ.

### 2.3 HTTP Clients (Gateway upstream, AI → providers)

Resilience4j or native timeouts:

| Call | Timeout | Retries |
|------|---------|---------|
| Upstream routing | Dynamic per route baseline + slack | Usually 0–1 idempotent reads only |
| LLM completions | Moderate RTT SLA | Single retry different region optional enterprise |

---

## 3. Idempotency Keys

Kafka consumers use:

- Dedupe table (`processed_event`) before side effects crossing external systems combining unique business id.

---

## 4. Non-Retryable Errors

Examples:

- Permanent 401 from LLM (bad key).
- Serialization bug (code defect) — circuit & fix forward.

Immediately surface **high severity** ops alert metric `nonretryable_fatal_total`.

---

## 5. Testing

Integration tests injecting:

- flaky ES response first N attempts
- assert single visible side effect externally

---

## Related

- [DLQ_STRATEGY.md](./DLQ_STRATEGY.md)
- [../architecture/FAILURE_HANDLING.md](../architecture/FAILURE_HANDLING.md)
