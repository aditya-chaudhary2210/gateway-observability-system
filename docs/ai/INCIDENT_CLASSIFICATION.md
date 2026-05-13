# Incident Classification

<!--
  Ownership: Incident Detection + AI Assist
-->

## 1. Objectives

Provide **stable labels** that:

- Power dashboard filters & analytics.
- Feed AI summarization for context (not as unverified truth).
- Align with on-call mental models: availability vs latency vs security vs dependency.

---

## 2. Taxonomy (`incident_classification`)

Primary categories (`primary` ENUM):

| Code | Description | Typical signals |
|------|-------------|----------------|
| `AVAILABILITY_UPSTREAM` | Upstream failures / 502 storms | Elevated 5xx, CB open counts |
| `LATENCY_REGRESSION` | Tail latency outliers | P95/P99 spike vs sliding baseline |
| `CLIENT_ERROR_BURST` | 4xx spikes | Possibly attack or client bug split via heuristics |
| `SECURITY_ABUSE_RATE_LIMIT` | Auth / RL saturation | JWT failures + RL hits |
| `DATA_DEPENDENCY_DEGRADED` | Cached DB/third-party anomalies | Increased timeouts fingerprints |
| `UNKNOWN` | Insufficient discriminative signals | Requires human triage badge |

Secondary free-form **`tags`** (max 16) curated via allowlist initially.

---

## 3. Classification Pipeline

Stages:

1. **Feature extraction:** rolling counts per route/upstream/cluster.
2. **Rule matchers:** weighted deterministic rule set (explainable paths).
3. **Score merging:** softmax-like normalization into primary candidate.
4. **Confidence assignment:** HIGH / MEDIUM / LOW.
5. **AI explanation layer (optional textual)** — AFTER classification numeric choices locked (prevents circular reasoning leakage).

Low confidence ⇒ UI chip “unverified classification”.

---

## 4. Evolution Strategy

Adding new classifier rules:

- Must ship with unit tests + sampled replay fixture.
- Version field `classification_ruleset_version`.

---

## 5. Related

- [../architecture/HLD.md](../architecture/HLD.md)
- [AI_PIPELINE.md](./AI_PIPELINE.md)
