# Redis Usage Strategy

<!--
  Ownership: Platform Data
-->

## 1. Use Cases Matrix

| Feature | Redis Structure | TTL | Owning Service |
|---------|-----------------|-----|----------------|
| Rate limiting | Token bucket keyed `rl:{route}:{subject}` | route-specific | Gateway |
| Detection rolling windows | `ZSET` timestamp members per metric key | horizon + buffer | Detection |
| Idempotency fast path | `SET key NX EX` optional | minutes | Ingestion/AI |
| RCA rate budget | `INCRBY` capped counter | sliding window hour | AI |
| Embedding cache | `STRING` gzipped embedding vector bytes | configurable | AI |

---

## 2. Key Naming Conventions

`{subsystem}:{tenant|global}:{logical_key}` — lower snake segments.

Avoid unbounded cardinality `user_specific` exploded keys unless sharded TTL enforced.

---

## 3. Consistency Expectations

Redis is **not** transactional with Kafka offsets — reconcile via:

- TTL expiry acceptable loss for heuristic windows vs strict financial correctness.
- For rate limits: approximation acceptable by design.

---

## 4. Failure Policy

Configurable:

- **`fail_closed`** paging risk if RL can't confirm — safer security stance.
- **`fail_open`** availability bias — ONLY non-prod default if chosen.

Decision recorded in DECISION_LOG at implementation lock.

---

## 5. Security

ACLs separating gateway vs detection DB index **optional** elevated posture; MVP single Redis logical DB with prefixed keys acceptable if secrets isolated.

---

## 6. Memory Planning

Estimated memory light for MVP; monitor `evicted_keys` metric.

---

## Related

- [POSTGRES_SCHEMA.md](./POSTGRES_SCHEMA.md)
- [../architecture/FAILURE_HANDLING.md](../architecture/FAILURE_HANDLING.md)
