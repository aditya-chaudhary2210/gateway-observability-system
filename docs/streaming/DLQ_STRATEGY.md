# Dead Letter Queue (DLQ) Strategy

<!--
  Ownership: Streaming Reliability
-->

## 1. Philosophy

**Why DLQ:** Poison messages should not infinite-loop consumers or stall partitions. Isolate, inspect, **replay deliberately**.

---

## 2. When to DLQ vs Retry

| Condition | Path |
|-----------|------|
| Deserialization/schema mismatch AFTER version negotiation window | DLQ immediately + alert |
| Transient datastore timeout | Bounded retry first |
| Malformed-but-repairable (single bad optional field) | Quarantine transformer attempt then DLQ if still fatal |

Configurable `max.failures` per pipeline stage.

---

## 3. DLQ Message Envelope Extension

Adds metadata:

```json
{
  "original_topic": "...",
  "original_partition": 3,
  "original_offset": 102948,
  "failure_reason_code": "...",
  "failure_stack_truncated": "...",
  "first_seen_at": "...",
  "retry_count": 5
}
```

---

## 4. Operational Playbook (Summary)

1. Identify spike via Grafana DLQ rate panel.
2. Sample messages (PII safe viewer).
3. Fix consumer or deploy schema bridge.
4. Replay tool: seek consumer or dedicated republisher with **rate limit**.

Full automation Phase 8+.

---

## 5. Security

DLQ may hold sensitive headers — restrict topic ACLs; optional field redactor pre-DLQ for demo compliance.

---

## Related

- [RETRY_STRATEGY.md](./RETRY_STRATEGY.md)
- [../architecture/FAILURE_HANDLING.md](../architecture/FAILURE_HANDLING.md)
