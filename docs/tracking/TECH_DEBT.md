# Technical Debt Register

<!--
  Ownership: Engineering + Architecture
-->

| ID | Area | Shortcut / Debt | Planned Resolution | Severity |
|----|------|-----------------|---------------------|----------|
| TD-001 | Ingestion | Dual-write Postgres+ES without transactional guarantee | Evaluate outbox pattern + saga compensation metrics | Medium |
| TD-002 | Scaling | Single-node ES/OS in local dev only | Snapshot + multi-node staging | Medium |
| TD-003 | Observability | Local trace stacks single-node Compose (Jaeger OTLP for Java services; Tempo OTLP for AI) — HA/persistence SLA absent | Grafana Cloud traces or clustered Tempo + durable Jaeger collector for demos | Medium |
| TD-004 | Classification | Rules-only baseline before ML uplift | Incremental heuristic improvements + dataset capture | Medium |
| TD-005 | Replay | Manual DLQ replay CLI absent | Scripted tool + rate-limit guard Phase 8 | Medium |
| TD-006 | Frontend | SPA may start with polling vs WebSocket SSE | SSE for incident stream later | Low |
| TD-007 | Tooling | Frontend lockfile not committed (npm install per machine) | Commit `package-lock.json` or adopt pnpm reproducible store | Low |
| TD-008 | Incidents | Detector opens a new row per breach (no dedupe window) | Sliding window + unique partial index on fingerprint | Medium |
| TD-009 | AI | RCA quality hinges on prompts/models; outbound httpx/OpenAI spans not instrumented yet | Add OpenTelemetry instrumentation for httpx + eval fixtures | Medium |

Adding debt: bump table; link related PR/issue + DECISION_LOG if architectural.

## Resolution SLA (Portfolio Guidance)

Severity High: tackle before proclaiming prod-like readiness demo. Others schedule opportunistically.

---

## Retirement Process

Upon resolution: strike row referencing PR + date in commit message footer `Closes TD-xxx`.
