# Elasticsearch / OpenSearch Indexes

<!--
  Ownership: Search & Analytics
-->

## 1. Purpose

Provide **interactive search & aggregations** on high-volume telemetry with richer full-text queries than Postgres row scans.

Assume OpenSearch-compatible mappings (Elasticsearch 8+ similar).

---

## 2. Index Naming & Lifecycle

Primary index alias: **`gateway-events-active`** pointing to concrete `gateway-events-YYYY.MM.dd`.

ILM-ish policy demo-simplified:

- Hot: recent 7d
- Delete after 14d demo (TECH_DEBT: warm/cold tiers production)

---

## 3. Document Model (`gateway-event-v1`)

| Field | Type | Notes |
|-------|------|-------|
| `event_id` | keyword | Join key |
| `occurred_at` | date |
| `trace_id` | keyword |
| `correlation_id` | keyword |
| `route_id` | keyword |
| `http_method` | keyword |
| `path_template` | text + keyword multi-field |
| `status_code` | integer |
| `latency_ms` | integer |
| `upstream_id` | keyword |
| `client_id_hash` | keyword | hashed obscured identifier |
| `error_class` | keyword optional |
| `sampled_payload_digest` | keyword optional |
| `tags` | keyword array |

---

## 4. Key Queries

Dashboard use-cases mapped:

| Use case | Query pattern |
|---------|----------------|
| Route traffic timeline | `date_histogram` agg on `route_id` filter |
| Error spike explorer | filters `status_code:[500 TO 599]` |
| Trace lookup | term `trace_id` |
| Incident context slice | bool filter time + route + fingerprint terms |

---

## 5. Ingestion Bulk Settings

Bulk size target 500–2000 docs or 5MB compressed — tune under load testing.

Replicas: `0` demo single node; production `≥1`.

---

## 6. Security

Index per tenant future; demo single logical tenant.

Apply index templates via init container or REST bootstrap job.

---

## 7. Related

- [../streaming/KAFKA_TOPICS.md](../streaming/KAFKA_TOPICS.md)
- [POSTGRES_SCHEMA.md](./POSTGRES_SCHEMA.md)
