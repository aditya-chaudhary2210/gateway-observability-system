# Sequence Diagrams

<!--
  Ownership: Platform Architecture
  Note: Diagrams use Mermaid; render in GitHub or compatible viewers.
-->

## 1. Happy Path — Request to Searchable Telemetry

```mermaid
sequenceDiagram
  autonumber
  participant Client
  participant GW as API Gateway Service
  participant US as Upstream Backend
  participant K as Kafka
  participant ING as Log Ingestion Service
  participant ES as OpenSearch Elasticsearch
  participant PG as PostgreSQL

  Client->>GW: HTTP + optional traceparent JWT
  GW->>GW: Validate JWT correlate metrics
  GW->>US: Forward request
  US-->>GW: Response status latency
  GW->>K: Produce gateway-access-v1
  ING->>K: Consume batch
  ING->>ING: Normalize enrich
  par Dual persistence
    ING->>ES: Bulk index searchable doc
    ING->>PG: Upsert projection + dedupe
  end
  GW-->>Client: Final response trace headers
```

---

## 2. Incident Open + RCA (Gated AI)

```mermaid
sequenceDiagram
  participant K as Kafka
  participant DET as Incident Detection Service
  participant PG as PostgreSQL
  participant AI as AI Analysis Service
  participant LLM as LLM Provider

  K->>DET: telemetry-enriched batch
  DET->>DET: Rule + anomaly scoring
  DET->>PG: Persist Incident OPENED
  DET->>K: incidents-v1 IncidentOpened
  Note over DET,AI: Optional async trigger webhook or consumer
  AI->>PG: Load incident bundle + correlated events excerpt
  AI->>AI: Compose evidence pack non-LLM filters
  AI->>LLM: Summarize propose RCA
  LLM-->>AI: Structured RCA JSON
  AI->>PG: Store INCIDENT_RCA artefact idempotent version
```

---

## 3. Failure — Kafka Temporarily Unavailable

```mermaid
sequenceDiagram
  participant GW as API Gateway Service
  participant K as Kafka
  participant BUF as Local Buffer optional

  GW->>K: Produce
  K--x GW: Failure timeout
  alt Buffer enabled non-prod demo
    GW->>BUF: Spill to disk queue
  else Drop with metric
    GW->>GW: Increment drop counter log error
  end
  GW-->>GW: Still return client response policy TBD
```

Policy (drop vs buffer) must be explicit per environment — see [FAILURE_HANDLING.md](./FAILURE_HANDLING.md).

---

## 4. Rate Limit Breach

```mermaid
sequenceDiagram
  participant Client
  participant GW as API Gateway Service
  participant RD as Redis

  Client->>GW: Request JWT
  GW->>RD: INCR bucket key ttl
  alt Allowed
    RD-->>GW: ok
    GW->>GW: Proceed upstream
  else Denied
    RD-->>GW: exceeds
    GW-->>Client: 429 Retry-After
    GW->>GW: Structured log RL violation
  end
```

---

## Related

- [HLD.md](./HLD.md)
- [FAILURE_HANDLING.md](./FAILURE_HANDLING.md)
