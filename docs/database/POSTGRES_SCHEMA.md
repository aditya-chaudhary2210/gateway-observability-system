# PostgreSQL Schema Documentation

<!--
  Ownership: Data Platform
  Status: Contract-first — migrations implement exactly this in Phase 4+
-->

## 1. Design Tenets

- **OLTP truth** for incidents & RCA lifecycle.
- **Idempotent ingestion** via natural or surrogate keys with unique constraints.
- **Timestamps** stored `timestamptz` UTC.
- **Soft delete** generally avoided for audit trail — use status fields.

---

## 2. Entity-Relationship Overview

```
gateway_event_projection (1) ----< (N) incident_signal_link (optional)
incident (1) ----< (N) incident_event
incident (1) ----< (0..N) incident_rca  (versioned generations)
ingestion_checkpoint (consumer_group, topic, partition)
```

---

## 3. Tables

### 3.1 `gateway_event_projection`

| Column | Type | Notes |
|--------|------|-------|
| `event_id` | `UUID` PK | Producer-assigned ULID/UUIDv7 |
| `occurred_at` | `timestamptz` | End of request |
| `trace_id` | `TEXT` | Extracted trace |
| `correlation_id` | `TEXT` | Propagation |
| `route_id` | `TEXT` | Gateway route identifier |
| `http_method` | `TEXT` | Verb |
| `path_template` | `TEXT` | Parameterized path pattern |
| `status_code` | `INT` | Final status |
| `latency_ms` | `INT` | Integer ms for simple analytics |
| `upstream_id` | `TEXT` | Logical upstream name |
| `client_id` | `TEXT` | From JWT `sub` or anonymous marker |
| `classification_snapshot` | `JSONB` | Optional quick tags |
| `raw_ref` | `TEXT` | Pointer to ES document id `_id` |

**Indexes:**

- `(occurred_at DESC)`
- `(route_id, occurred_at DESC)`
- `(trace_id)`

### 3.2 `incident`

| Column | Type | Notes |
|--------|------|-------|
| `incident_id` | `UUID` PK |
| `opened_at` | `timestamptz` |
| `closed_at` | `timestamptz` NULL |
| `severity` | `TEXT` ENUM via CHECK | `SEV1`..`SEV4`, `INFO` |
| `status` | `TEXT` CHECK | OPEN, MITIGATING, RESOLVED |
| `primary_classification` | `TEXT` |
| `fingerprint` | `TEXT` | Stable clustering key hash |
| `blast_radius_hint` | `JSONB` | routes upstreams approximate counts |
| `detection_ruleset_version` | `INT` |

Indexes: `(status, severity)`, `(fingerprint)`

### 3.3 `incident_event`

Audit trail rows for dashboard timeline.

| Column | Type |
|--------|------|
| `id` BIGSERIAL PK |
| `incident_id` FK |
| `type` TEXT |
| `payload` JSONB |
| `occurred_at` timestamptz |

### 3.4 `incident_rca`

| Column | Type | Notes |
|--------|------|-------|
| `rca_id` UUID PK |
| `incident_id` FK |
| `generation_version` INT | Unique per incident (`UNIQUE(incident_id, generation_version)`) |
| `status` TEXT | READY/FAILED/etc |
| `structured` JSONB | `RcaStructuredV1` |
| `prompt_hash` TEXT |
| `model_metadata` JSONB |
| `created_at` timestamptz |

### 3.5 `processed_event`

| Column | Type | Notes |
|--------|------|-------|
| `event_id` UUID PK | Dedup |

### 3.6 `ingestion_checkpoint`

| Column | Type |
|--------|------|
| `consumer_group` TEXT |
| `topic` TEXT |
| `partition` INT |
| `offset` BIGINT |
| PRIMARY KEY `(consumer_group, topic, partition)` |

**Note:** Combination with Kafka offset commit strategy documented in ingestion LLD.

---

## 4. Migration Strategy

Flyway repeatable scripts numbering `V1__...` onward; forbid manual prod DDL drift.

---

## 5. Data Retention (Demo vs Prod Aspiration)

| Table | Demo | Prod idea |
|-------|------|-----------|
| `gateway_event_projection` | 14–30 days | Partition by month |

---

## 6. Related

- [REDIS_USAGE.md](./REDIS_USAGE.md)
- [ELASTICSEARCH_INDEXES.md](./ELASTICSEARCH_INDEXES.md)
- [../streaming/KAFKA_TOPICS.md](../streaming/KAFKA_TOPICS.md)
