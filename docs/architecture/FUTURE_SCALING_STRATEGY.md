# Future Scaling Strategy

<!--
  Ownership: Platform Architecture
-->

## 1. Purpose

Demonstrate intentional **beyond-MVP scaling** reasoning for SDE-2/staff narratives without overbuilding prematurely.

Axes: **Throughput**, **State**, **AI cost**, **Multi-tenant isolation**, **Global footprint**.

---

## 2. Ingest Throughput Expansion

Current design target **≥50k gateway events/day** easily fits modest brokers; uplift path:

| Lever | Benefit |
|-------|---------|
| Increase topic partitions | Consumer horizontal parallelism |
| Batching tuning `linger.ms` | Broker efficiency trade latency |
| Separate hot vs cold telemetry topics | QoS segregation |
| Optional tiering to **object storage** parquet lake | Analystics decoupled from OLTP |

---

## 3. Storage Scaling

### PostgreSQL

- Time-based **partition pruning** heavy projection table.
- Read replicas feeding analytics if synchronous API load grows.

### Elasticsearch / OpenSearch

- Dedicated masters + hot/warm architectural split.
- Index lifecycle rollover automation + snapshot S3-compliant repo.

### Redis

- Hash-tag sharding mitigation for RL hot tenants.
- Redis Cluster adoption if memory ceiling hit.

---

## 4. AI Cost & Capacity

| Strategy | Description |
|---------|-------------|
| Regional caching of embeddings keyed by fingerprint | Avoid repeated LLM preprocessing |
| KEDA scaler on RCA queue depth | Autoscale AI replicas |
| Smaller distilled models for summaries | Complexity vs fidelity trade documented before switch |
| Hard global token budget alerting | Observability dashboards |

---

## 5. Multi-Tenancy (If Extended)

Isolation layers (choose future):

| Level | Complexity | Suitability Demo |
|-------|-------------|------------------|
| Logical tenant-id column filtering | Low | MVP extension |
| Schema-per-tenant | Medium | Regulatory niches |
| Cluster-per-major-tenant | High | Enterprise hyperscale |

Add topic prefix per tenant ONLY if cardinality controlled.

---

## 6. Global / DR Footprint

Aspirational:

- Kafka MirrorMaker / MSK multi-region standby.
- Postgres logical replication standby read region.
- RPO/RTO tabletop exercises documented—not implemented early.

---

## 7. De-risking Unknown Hotspots

Establish load test harness milestones:

| Milestone | Tooling |
|-----------|---------|
| Gateway saturated | `k6` scripts simulating bursts + RL stress |
| Ingest backlog | Controlled producer flood verifying lag alerting |
| RCA storm | Burst incident opens measuring AI concurrency |

Artifacts: Grafana snapshots archived per release candidate.

---

## Related

- [HLD.md](./HLD.md)
- [../tracking/TECH_DEBT.md](../tracking/TECH_DEBT.md)
