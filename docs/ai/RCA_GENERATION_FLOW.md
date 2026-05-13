# RCA Generation Flow

<!--
  Ownership: AI Engineering + Incident Squad
-->

## 1. Triggers

| Trigger | Source | Behaviour |
|---------|--------|-----------|
| `IncidentOpened` | Kafka / REST | enqueue RCA job version 1 |
| `IncidentSeverityRaised` | Detection | regenerate if delta significant |
| Manual refresh | Dashboard REST POST | increments `generation_version` |
| Periodic none | ❌ avoided | wastes tokens |

---

## 2. State Machine (`incident.rca_status`)

States:

- `PENDING`
- `IN_PROGRESS`
- `READY`
- `FAILED`
- `STALE` *(underlying incident changed materially)*

Transitions logged with timestamps for audit UX.

---

## 3. Step-by-step Flow

1. **Authorize** caller (JWT scope `incidents:rca`).
2. **Load incident** baseline from Postgres (+ join top signals).
3. **Assemble EvidencePack** (deterministic ordering).
4. **Short-circuit** if identical hash of evidence exists for `generation_version` — return cached.
5. **LLM structured call** with schema `RcaStructuredV1`.
6. **Validate** JSON; if fail → repair pass with trimmed evidence.
7. **Persist** row in `incident_rca` with unique `(incident_id, generation_version)`.
8. **Emit** domain event `RcaGenerated` *(optional future topic for audit bus)*.

---

## 4. Schema Sketch `RcaStructuredV1`

```json
{
  "schema_version": 1,
  "incident_id": "uuid",
  "generation_version": 2,
  "sections": {
    "situation_summary": "string",
    "primary_causes": [{"cause": "string", "evidence_refs": ["e1"]}],
    "ruled_out": [],
    "checks": [],
    "confidence": 0.0
  },
  "model_metadata": {
    "provider": "openai|ollama",
    "model_name": "string",
    "prompt_hash": "sha256"
  }
}
```

Final field list synchronized with `contracts/` when generated.

---

## 5. Failure UX

| Condition | User sees |
|-----------|-----------|
| LLM timeout | Banner + last known good RCA if any |
| Validation fail | `FAILED` with correlation id for support |
| AI disabled platform-wide | Static message + deterministic bullet list from rules only |

---

## 6. Related

- [AI_PIPELINE.md](./AI_PIPELINE.md)
- [PROMPT_STRATEGY.md](./PROMPT_STRATEGY.md)
- [../database/POSTGRES_SCHEMA.md](../database/POSTGRES_SCHEMA.md)
