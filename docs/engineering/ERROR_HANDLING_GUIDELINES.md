# Error Handling Guidelines

<!--
  Ownership: Engineering
-->

## 1. Philosophy

Predictable failures > silent degradation. Operators read **signals**, not anecdotes.

---

## 2. Exception Taxonomy

| Type | Typical Use | Mapped HTTP |
|------|-------------|-------------|
| `DomainValidationException` | Semantic invalid Ops data | 400 |
| `AuthException` | JWT issues | 401/403 |
| `TransientDependencyException` | Upstream flaky | Retry eligible |
| `FatalConfigurationException` | Misconfig startup | Crash loop fast |

Avoid generic `RuntimeException`.

---

## 3. Gateway HTTP Mapping Table

Maintain explicit `ExceptionHandler`:

| Condition | Body code | Retry hint header |
|-----------|-----------|-------------------|
| RL breach | `RATE_LIMITED` | `Retry-After` |
| CB open | `UPSTREAM_UNAVAILABLE` | None |
| Unknown | `INTERNAL_ERROR` | None |

Never leak stack traces to clients.

---

## 4. Kafka Error Policy

Consumers: log structured with `partition` `offset` `error_code`. Never log full payload if PII risk—use hashed id.

---

## 5. AI Service Errors

LLM failures produce:

```json
{"error_code":"AI_LLM_TIMEOUT","correlation_id":"...","detail":"provider timeout"}
```

Detail strings must be operator-safe.

---

## 6. Client Correlation

Always echo `X-Correlation-Id` if provided; else generate at edge.

---

## Related

- [../architecture/FAILURE_HANDLING.md](../architecture/FAILURE_HANDLING.md)
- [../streaming/DLQ_STRATEGY.md](../streaming/DLQ_STRATEGY.md)
