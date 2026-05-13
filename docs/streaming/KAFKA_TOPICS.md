# Kafka Topics Strategy

<!--
  Ownership: Streaming Guild
-->

## 1. Topic Inventory

| Topic | Partitions Guidance | Payload | Consumers |
|-------|---------------------|---------|-----------|
| `gateway-access-v1` | Start 6 *(tune)* | Telemetry envelope `GatewayAccessEmitted` | Ingestion |
| `telemetry-enriched-v1` | Same or 2× raw | Enriched telemetry | Detection (optional shortcut) |
| `incidents-v1` | 3–6 domain throughput | Incident lifecycle JSON | AI (async), Notifications future |
| `dlq.gateway-access-v1` | 3 | Dead letters | Operational replay tooling |

Compaction: **disabled** for high-volume telemetry; **consider** compaction for incidents if reduced to keyed changelog (alternative design — defer).

Retention:

| Topic | Retention demo | Prod aspiration |
|-------|----------------|----------------|
| gateway-access-v1 | 24–72h | 7d (cost tradeoff) |
| telemetry-enriched-v1 | Same or shorter derivable replay | Derived optional skip |
| incidents-v1 | 7d min | Longer immutable audit |

---

## 2. Keying & Ordering

Recommended message key:

- Telemetry: **`route_id` + optional shard salt** balancing skew vs locality.
- Incidents: **`incident_id`**.

Preserves relative ordering per incident lineage.

---

## 3. Schema Governance

`/contracts/async` holds JSON Schema; CI validates producers & consumers with compatibility rules **BACKWARD**.

---

## 4. ACL Sketch (Production)

Producer accounts per service; consumer read-only group ACLs; admin separate.

---

## 5. Observability

Exporter metrics: broker disk, ISR, under-replicated partitions. Consumer lag alerts per group.

---

## Related

- [DLQ_STRATEGY.md](./DLQ_STRATEGY.md)
- [RETRY_STRATEGY.md](./RETRY_STRATEGY.md)
- [../architecture/EVENT_DRIVEN_ARCHITECTURE.md](../architecture/EVENT_DRIVEN_ARCHITECTURE.md)
