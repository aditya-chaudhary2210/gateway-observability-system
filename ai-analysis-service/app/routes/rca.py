"""RCA HTTP surface — evidence bundle from Postgres (via ingestion BFF) + Elasticsearch."""

from __future__ import annotations

import uuid

import httpx
from fastapi import APIRouter, HTTPException

from app.services import rca_engine
from app.settings import settings

router = APIRouter(tags=["rca"])


@router.post("/incidents/{incident_id}/summarize")
async def summarize_incident(incident_id: uuid.UUID) -> dict[str, object]:
    async with httpx.AsyncClient() as client:
        incident = await rca_engine.fetch_incident(client, settings, incident_id)
        if incident is None:
            raise HTTPException(status_code=404, detail="Incident not found in ingestion read API.")

        blast = incident.get("blastRadiusJson") or "{}"
        route_hint = None
        try:
            import json as _json

            parsed = _json.loads(blast) if isinstance(blast, str) else blast
            if isinstance(parsed, dict):
                route_hint = parsed.get("route")
        except (ValueError, TypeError):
            route_hint = None

        severity = incident.get("severity")
        classification = incident.get("primaryClassification")

        evidence = await rca_engine.fetch_recent_events(
            client, settings, route_hint, status_min=400 if severity in {"SEV1", "SEV2"} else None
        )

        prompt = rca_engine.build_prompt(incident, evidence)

        cfg = settings.llm_http_config()
        if cfg:
            base_url, api_key, model = cfg
            llm_payload = await rca_engine.llm_json_response(prompt, base_url=base_url, api_key=api_key, model=model)
            provider = settings.llm_backend
        else:
            llm_payload = rca_engine.heuristic_summary(incident, evidence)
            provider = (
                "heuristic"
                if settings.llm_backend == "heuristic"
                else f"heuristic_fallback_{settings.llm_backend}_unconfigured"
            )

        return {
            "incident_id": str(incident_id),
            "summary": llm_payload.get("summary"),
            "probable_root_cause": llm_payload.get("probable_root_cause"),
            "confidence": float(llm_payload.get("confidence") or 0.0),
            "recommended_action": llm_payload.get("recommended_action"),
            "provider": provider,
            "evidence_preview": evidence[:5],
            "classification": classification,
            "severity": severity,
            "route_focus": route_hint,
        }


@router.post("/prioritize")
async def prioritize_stub(incidents: dict) -> dict:
    """Alert prioritisation assist hook — deterministic ordering before heavier LLM steps."""
    items = incidents.get("incident_ids", [])
    ranked = [{"incident_id": item, "priority_score": idx} for idx, item in enumerate(items)]
    return {"ranked": ranked}
