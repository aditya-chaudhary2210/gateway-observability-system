# API Contracts Overview

<!--
  Ownership: Platform Architecture
  Purpose: Stable naming and versioning before OpenAPI artefacts land under contracts/rest/
-->

## 1. Why Contract-First

Client generation, mock servers, CI breaking-change detection, and cross-language alignment (React + JVM + Python) all depend on **published contracts**. This document names operations and payloads; **`contracts/rest/*.yaml`** will mirror it in Phase 2+.

---

## 2. Global Conventions

| Convention | Rule |
|------------|------|
| Base path prefix | `/api/v1` for public read/write surfaces |
| Correlation | Request header `X-Correlation-Id` optional; always echoed |
| Errors | JSON `{ "error_code", "message", "correlation_id" }` |
| Pagination | Cursor style `next_cursor`, `limit` (max 100 default) unless offset justified |
| Dates | RFC 3339 `timestamptz` strings |

---

## 3. Read API (Incident & Telemetry façade)

_Placement TBD_: dedicated BFF or gateway-hosted routes — see [DECISION_LOG.md](../tracking/DECISION_LOG.md).

### 3.1 Incidents

| Operation | Method | Path | Description |
|-----------|--------|------|-------------|
| List | GET | `/api/v1/incidents` | Filters: `from`, `to`, `severity`, `status`, `classification`, `cursor` |
| Detail | GET | `/api/v1/incidents/{incident_id}` | Includes latest RCA summary pointer |
| Timeline | GET | `/api/v1/incidents/{incident_id}/events` | Audit stream |

**Response snippets (conceptual fields):**

- `IncidentSummary`: `id`, `opened_at`, `severity`, `status`, `primary_classification`, `confidence`, `fingerprint_short`
- `IncidentDetail`: extends summary + `blast_radius_hint`, `links.trace_id_samples[]`

### 3.2 Traces / exploration

| Operation | Method | Path | Notes |
|-----------|--------|------|-------|
| Trace summary | GET | `/api/v1/traces/{trace_id}` | Aggregated span-like view or proxy OTel backend Phase 7 |

### 3.3 Metrics façade

| Operation | Method | Path | Notes |
|-----------|--------|------|-------|
| Summary | GET | `/api/v1/metrics/summary` | Pre-computed KPIs or thin Prometheus adapter |

---

## 4. AI Analysis Service REST

_Base URL_: internal cluster DNS `ai-analysis-service:8082` _(port illustrative)_.

| Operation | Method | Path | Body / Notes |
|-----------|--------|------|----------------|
| Generate RCA | POST | `/api/v1/rca/incidents/{incident_id}/generate` | Optional `{ force: bool, locale: string }` |
| Get RCA | GET | `/api/v1/rca/incidents/{incident_id}?version=` | Latest if version omitted |
| Similar incidents | POST | `/api/v1/incidents/similarity` | `{ incident_id \| fingerprint, limit }` |
| Prioritize batch | POST | `/api/v1/prioritize` | `{ incident_ids: [] }` assistive ranking |

Structured RCA JSON: see [RCA_GENERATION_FLOW.md](../ai/RCA_GENERATION_FLOW.md).

---

## 5. Gateway Health (Operational)

Spring Boot actuator standard:

- `/actuator/health`, `/actuator/prometheus` _(if exposed via network policy)_.

---

## 6. Authorization Model (Draft)

| Audience | Grants |
|---------|--------|
| Dashboard user | JWT scope `obs:read` |
| RCA generation | JWT scope `incidents:rca` |
| Service-to-service | mTLS **future** Phase 8; MVP network isolation |

---

## 7. Async Contract Counterpart

HTTP complements Kafka envelopes documented in:

- [EVENT_DRIVEN_ARCHITECTURE.md](../architecture/EVENT_DRIVEN_ARCHITECTURE.md)
- [KAFKA_TOPICS.md](../streaming/KAFKA_TOPICS.md)

---

## 8. Evolution

Breaking changes ⇒ bump `/api/v2` **or** introduce parallel resource with deprecation header `Sunset`.
