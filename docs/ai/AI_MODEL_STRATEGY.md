# AI Model Strategy

<!--
  Ownership: AI Engineering
-->

## 1. Principles

| Principle | Rationale |
|-----------|-----------|
| **Right model for tier** | Fast classification & summarization differs from embedding suitability |
| **Provider abstraction** | Swap OpenAI vs local Ollama without route-layer churn |
| **Deterministic scaffolding** | JSON schema repair loops bounded |
| **Cost transparency** | Token estimates logged per invocation |

Avoid constant premium-model usage for exploratory dev—use cheapest adequate model tiers.

---

## 2. Model Roles

| Role | Suggested archetypes _(exact ids versioned externally)_ |
|------|-------------------------------------------------------|
| **RCA summarization** | Higher capability chat model (GPT-4o-class or Llama3 70B local if hardware permits) |
| **Short incident titles** _(optional)_ | Cheaper/fast smaller model |
| **Embeddings (similarity)** | Dedicated embedding model separate dimension space |
| **Classifier explanation** _(rare textual)** | Lightweight chat model AFTER numeric labels fixed |

Formal pinning: `.env`:

```
AI_RCA_MODEL=...
AI_EMBEDDING_MODEL=...
```

---

## 3. Provider Matrix

| Provider | Strength | Weakness |
|----------|----------|----------|
| **OpenAI** | Quality + embeddings ecosystem | Spend + egress policy |
| **Ollama (Llama 3)** | Local privacy iterative | Hardware bound ops burden |

Dual implementation behind `LLMGateway` ensures parity tests comparing JSON validation success—not subjective prose scoring only.

---

## 4. Evaluation Harness

Maintain benchmark JSON fixtures:

| Case | Measures |
|------|----------|
| Latency spike incident | RCA lists latency evidence correctly |
| Upstream outage | RCA references dependency failures not imaginary DB |
| Benign churn | Confidence low flagged |

Golden metrics:

- Structured parse success %
- Evidence citation recall heuristic (presence of fingerprint/route tokens)

Manual qualitative review sparingly acceptable early.

---

## 5. Drift Mitigation

- Record `prompt_hash` + provider model id persisted with each RCA artefact (`incident_rca.model_metadata`).
- Alert if parse failure rate spikes after upgrade.

---

## 6. Ethical / Safety Bounds

Operational assistant framing only—prompt system block instructs refusal for destructive infra actions lacking human approval.

---

## Related

- [AI_PIPELINE.md](./AI_PIPELINE.md)
- [PROMPT_STRATEGY.md](./PROMPT_STRATEGY.md)
- [RCA_GENERATION_FLOW.md](./RCA_GENERATION_FLOW.md)
