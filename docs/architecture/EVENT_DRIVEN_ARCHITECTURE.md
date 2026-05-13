# Event-Driven Architecture

<!--
  Ownership: Platform Architecture / Streaming Guild
-->

## 1. Rationale

**Why Kafka:** Decouple ingress velocity from analytic processing and AI latency spikes; reuse single spine for replay & fan-out patterns familiar to observability engineers.

Events are categorized:

- **Telemetry events** — high volume, immutable facts.
- **Domain events** — lower volume, lifecycle (IncidentOpened, IncidentUpdated).
- **Commands** *(discouraged on Kafka unless idempotent patterns)* — prefer REST for synchronous user actions unless proven otherwise.

---

## 2. Event Taxonomy

| Name | Topic | Producer | Consumers | Ordering |
|------|-------|----------|-----------|----------|
| `GatewayAccessEmitted` | `gateway-access-v1` | api-gateway-service | ingestion | By `routeId` *(best-effort partitions)* |
| `TelemetryEnriched` | `telemetry-enriched-v1` | log-ingestion-service | detection (optional shortcut) | `incident_shard_key` |
| `IncidentSignaled` | `incidents-v1` | incident-detection-service | ai-analysis (optional trigger), auditing | By `incidentId` |

---

## 3. Packaging & Versioning Strategy

Envelope pattern (recommended):

```json
{
  "schema_version": 1,
  "event_type": "...",
  "occurred_at": "ISO8601",
  "trace_context": {"traceparent": "...", "tracestate": "..."},
  "correlation_id": "uuid-or-string",
  "producer": {"service": "api-gateway-service", "instance": "uuid"},
  "payload": { "...": "type-specific canonical fields" }
}
```

**Backward compatibility:**

- Consumers accept `schema_version <= N`.
- Breaking changes → new topic suffix (`-v2`) or new event type topic with rolling dual-write window.

Formal schema artefacts: `/contracts/async`.

---

## 4. Saga / Consistency Model

**No distributed sagas across telemetry & incident initially** — eventual consistency acceptable.

**Outbox consideration:** Postgres outbox pattern for bridging DB commit → Kafka emission on critical paths (*incident persistence*). Evaluate in Phase 4+ (see TECH_DEBT).

---

## 5. Idempotency Rules

| Consumer | Idempotency key |
|----------|-----------------|
| Ingestion | `event_id` from producer (UUIDv7 or ULID recommended) |
| Detection | `(incident_candidate_fingerprint)` with window bucket |
| AI trigger | `(incident_id, rca_generation_version)` |

---

## 6. Replay Strategy

Operational replay:

1. Stop consumer groups or reset offsets (**controlled** ops runbook later).
2. Ensure downstream writers tolerate re-processing (UPSERT semantics).
3. Guard AI regeneration with explicit versioning to avoid overwriting human-approved RCA (future flag).

---

## 7. Event vs REST Boundaries

| Operation | Preferred transport |
|-----------|---------------------|
| User requests RCA refresh | REST → AI emits async job event |
| Burst telemetry | Kafka |
| Dashboard initial page load aggregates | REST read API (cached) |

---

## 8. Related Documents

- [../streaming/KAFKA_TOPICS.md](../streaming/KAFKA_TOPICS.md)
- [../streaming/DLQ_STRATEGY.md](../streaming/DLQ_STRATEGY.md)
- [../streaming/RETRY_STRATEGY.md](../streaming/RETRY_STRATEGY.md)
- [SEQUENCE_DIAGRAMS.md](./SEQUENCE_DIAGRAMS.md)
