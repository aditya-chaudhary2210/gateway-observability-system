# Prompt Strategy

<!--
  Ownership: AI Engineering
-->

## 1. Design Principles

| Principle | Rationale |
|-----------|-----------|
| **Structured-first** | JSON schema outputs parse reliably downstream |
| **Evidence-bound** | Prompt explicitly states facts separate from hypotheses |
| **Uncertainty explicit** | Model must emit `unknown` rather than invent services |
| **Short context windows** | Minimize latency & bill shock |

---

## 2. Prompt Blocks (Conceptual Composition)

Stable **system block** (`SYSTEM_RCA_V1`):

- Role definition: assisting SRE—not executing changes.
- Prohibitions: no credential leakage, fabricating metrics absent in evidence JSON.
- Output schema reference.

Injected blocks:

| Block Key | Purpose |
|-----------|---------|
| `EVIDENCE_JSON` | Serialised deterministic evidence pack |
| `CLASSIFICATION_TAGS` | From incident classifier |
| `USER_LOCALE` | Optional tone only — not factual override |

Never embed raw JWTs.

---

## 3. Incident Summarisation Template (Outline)

Sections required in structured output:

1. `situation_summary` (≤ N tokens)
2. `likely_primary_causes` (ranked, each paired with cited evidence IDs)
3. `ruled_out_hypotheses` (optional — improves trust)
4. `recommended_checks` concrete commands or dashboards (generic not env-specific secrets)
5. `confidence_score` [0–1]
6. `follow_up_human_questions`

---

## 4. Embedding / Similarity Prompts

For similarity we **don't** freestyle against LLM retrieval; embeddings feed vector search hybridized with lexical filters in OpenSearch (**future** TECH_DEBT if pure pgvector adopted).

Prompt only used if **ambiguous** clustering requires narrative dedupe — gated.

---

## 5. Regression Control

Maintain golden **`EvidencePack` fixtures** in tests ensuring:

- Valid JSON survives parser
- Sensitive patterns stripped

---

## 6. Related

- [AI_PIPELINE.md](./AI_PIPELINE.md)
- [RCA_GENERATION_FLOW.md](./RCA_GENERATION_FLOW.md)
