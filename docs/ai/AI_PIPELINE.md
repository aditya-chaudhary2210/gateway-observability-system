# AI Pipeline (Non-Gimmicky)

<!--
  Ownership: AI / Reliability Engineering
  Reasoning: LLMs excel at condensation & hypothesis framing, not brute-force log ingestion.
-->

## 1. Mission

Deliver **meaningful incident intelligence**:

- RCA narrative grounded in aggregated evidence — not hallucinated infra details.
- Similar historical incidents leveraging hybrid retrieval.
- Alert prioritization as **assistive ranking**, not authoritative paging policy (human override preserved).

Explicit **non-goals:**

- Passing every gateway log line to an LLM.
- “Magic anomaly” claims without deterministic baselines layered underneath.

Pipeline order: **Rules → Signals → Retrieval → LLM**.

---

## 2. Tiered Pipeline

```
[ Gateway Telemetry Facts ]
           |
           v
+---------------------+
| Tier 0: Filtering |  <-- drop health checks, sampled success-only traffic optionally
+---------------------+
           |
           v
+-------------------------------+
| Tier 1: Deterministic Ops    | <-- thresholds spikes dependency flags
+-------------------------------+
           |
           v
+-----------------------------------------------+
| Tier 2: Statistical / Heuristic anomaly      |
+-----------------------------------------------+
           |
           v
+------------------------------+
| Tier 3: Embedding similarity | <-- only for incidents OPEN + stable fingerprint
+------------------------------+
           |
           v
+-------------------------------+
| Tier 4: LLM summarization RCA | <-- bounded tokens strict JSON schema validation
+-------------------------------+
```

---

## 3. Evidence Bundle Construction

Prior to LLM invocation, assemble **`EvidencePack`** containing:

| Section | Contents |
|---------|----------|
| `incident.summary` | Time window severity blast radius heuristic |
| `top_routes` | Aggregated latency error counts |
| `dependency_signals` | Upstream timeouts CB open flags if present |
| `recent_changes` *(optional stub)* | Placeholder hook for CI deploy correlation |
| `exemplar_events` | 3–10 **redacted** JSON snippets max |

Truncate fields deterministically (`max_chars`, lexical priority ordering).

---

## 4. Model Provider Abstraction

`LLMGateway` interface in Python (`generate_structured(prompt, schema)`, `embed(text)`).

Implementations:

- **OpenAI** (chat + embeddings API)
- **Ollama** (local Llama3) with compatible HTTP interface

Swap via configuration — no vendor logic in routes.

---

## 5. Output Contract

Return validated JSON adhering to **`RcaStructuredV1`** schema (documented fully in RCA doc). On validation failure → single repair attempt using smaller excerpt; still failing → degrade status.

---

## 6. Cost & Safety Guards

| Guard | Mechanism |
|-------|-----------|
| Token ceiling | Hard cap input + truncation |
| Call budget | Redis counter per incident |
| Sensitive data | Redaction map for headers claims bodies |
| Hallucination risk | Confidence score separate from textual fluency disclaimers |

---

## 7. Related Documents

- [PROMPT_STRATEGY.md](./PROMPT_STRATEGY.md)
- [RCA_GENERATION_FLOW.md](./RCA_GENERATION_FLOW.md)
- [INCIDENT_CLASSIFICATION.md](./INCIDENT_CLASSIFICATION.md)
