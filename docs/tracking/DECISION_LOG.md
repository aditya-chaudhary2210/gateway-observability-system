# Architecture & Engineering Decision Log (ADR-lite)

<!--
  Ownership: Architecture Guild
-->

Record **dated** choices with rationale. Full ADR adoption optional later.

---

## Template (copy for new rows)

```
### YYYY-MM-DD — SHORT TITLE

**Problem:** …

**Chosen solution:** …

**Alternatives considered:** …

**Tradeoffs:** …

**Impacted docs / code:** paths…
```

---

## 2026-05-13 — Monorepo vs Multi-repo

**Problem:** Maintain coherent cross-service evolution for résumé-scale project vs independent lifecycles.

**Chosen solution:** Single **monorepo** with Maven aggregator plus Python/FastAPI and Vite workspaces unified under Compose.

**Alternatives:** Multi-repo with git submodules.

**Tradeoffs:** Heavier CI matrix vs simpler refactor propagation.

**Impacted docs / code:** `docs/REPOSITORY_FOLDER_STRUCTURE.md`, compose files, `.github/workflows`.

---

## 2026-05-13 — Kafka as Unified Event Spine

**Problem:** Need replayable buffer between ingest & analytics velocities.

**Chosen solution:** Kafka topics per major event family with `schema_version` envelopes.

**Alternatives:** HTTP fan-out streaming; Redis Streams-only bus.

**Tradeoffs:** Ops complexity vs elasticity & replay story.

---

## 2026-05-13 — Dual Storage (Postgres + Elasticsearch 8.x)

**Problem:** Incident OLTP correctness vs high-cardinality search.

**Chosen solution:** Postgres canonical incident/event tables + Elasticsearch raw telemetry index; ingestion coordinates both before emitting enriched topic.

**Alternatives:** ClickHouse-only analytics plane.

**Tradeoffs:** Dual-write consistency relies on deterministic `event_id` + processed markers.

---

## 2026-05-13 — AI Gating Before LLM

**Problem:** Avoid gimmicky LLM cost/latency.

**Chosen solution:** Tiered pipeline: rules/anomaly/embeddings-first, LLM last for RCA only.

**Alternatives:** LLM-first triage.

**Tradeoffs:** More deterministic engineering for trustworthy demos.

---

## 2026-05-13 — Implementation Bootstrap (Compose-first)

**Problem:** Portfolio needs runnable spine before deep ML/LLM fidelity.

**Chosen solution:**
- Deliver Spring Cloud Gateway (telemetry + RL + chaos + circuit breaker façade) + ingestion + heuristic detection + FastAPI stubs + SPA.
- Compose orchestrates infra (Kafka/ZK/PG/Redis/Elasticsearch) mirroring docs.

**Alternatives:** K8s-only local dev (heavy for recruiters cloning repo).

**Tradeoffs:** Compose operational drift vs Helm manifests deferred to Phase 8.

**Impacted docs / code:** `deploy/docker-compose/*`, Spring services `Dockerfile`s, dashboards.

---

## Open Decisions (to close during next iterations)

| Topic | Options | Owner |
|-------|---------|-------|
| Read API placement | Dedicated BFF vs gateway reverse routes | Platform |
| Rate limit failure mode | fail-open vs fail-closed default | Security + SRE |
| Trace backend | Tempo vs Jaeger exporter wiring | Platform |

---

Update this file **before** merging architectural shifts.
