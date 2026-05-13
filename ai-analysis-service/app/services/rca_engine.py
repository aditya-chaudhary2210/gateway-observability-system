"""Evidence bundle + summarisation (LLM optional — interview-friendly fallbacks)."""

from __future__ import annotations

import json
import uuid
from typing import Any

import httpx
from openai import AsyncOpenAI

from app.settings import AiSettings


async def fetch_incident(client: httpx.AsyncClient, settings: AiSettings, incident_id: uuid.UUID) -> dict[str, Any] | None:
    url = f"{settings.ingestion_base_url.rstrip('/')}/api/v1/incidents/{incident_id}"
    response = await client.get(url, timeout=10.0)
    if response.status_code == 404:
        return None
    response.raise_for_status()
    return response.json()


async def fetch_recent_events(
    client: httpx.AsyncClient, settings: AiSettings, route_hint: str | None, status_min: int | None
) -> list[dict[str, Any]]:
    query: dict[str, Any] = {"size": 8, "sort": [{"occurred_at": {"order": "desc"}}], "query": {"bool": {"must": []}}}
    if route_hint:
        query["query"]["bool"]["must"].append({"match": {"route_id": route_hint}})
    if status_min is not None:
        query["query"]["bool"]["must"].append({"range": {"status_code": {"gte": status_min}}})
    if not query["query"]["bool"]["must"]:
        query["query"] = {"match_all": {}}

    search_url = f"{settings.elasticsearch_url.rstrip('/')}/{settings.elasticsearch_index}/_search"
    response = await client.post(search_url, content=json.dumps(query), headers={"Content-Type": "application/json"}, timeout=15.0)
    response.raise_for_status()
    hits = response.json().get("hits", {}).get("hits", [])
    return [h.get("_source", {}) for h in hits]


def build_prompt(incident_payload: dict[str, Any], evidence_rows: list[dict[str, Any]]) -> str:
    return (
        "You are an experienced SRE tasked with summarising an API gateway incident backed by concrete telemetry.\n"
        "Do not invent downstream service names that are absent from the evidence.\n\n"
        "INCIDENT_JSON:\n"
        f"{json.dumps(incident_payload, indent=2)}\n\n"
        "RECENT_TELEMETRY_SAMPLES (from Elasticsearch; higher index = more recent window):\n"
        f"{json.dumps(evidence_rows[:6], indent=2)}\n\n"
        "Return STRICT JSON with keys summary, probable_root_cause, confidence (0-1 float), recommended_action.\n"
    )


async def llm_json_response(prompt: str, *, base_url: str, api_key: str, model: str) -> dict[str, Any]:
    """Call any OpenAI-compatible chat completions endpoint (Ollama, Groq, OpenAI)."""
    client = AsyncOpenAI(base_url=base_url, api_key=api_key)
    completion = await client.chat.completions.create(
        model=model,
        temperature=0.2,
        messages=[
            {"role": "system", "content": "Answer only with compact JSON. No markdown fences."},
            {"role": "user", "content": prompt},
        ],
    )
    text = completion.choices[0].message.content or "{}"
    try:
        return json.loads(text)
    except json.JSONDecodeError:
        return {"summary": text.strip(), "probable_root_cause": "unable-to-parse-llm-json", "confidence": 0.35, "recommended_action": ""}


def heuristic_summary(incident_payload: dict[str, Any], evidence_rows: list[dict[str, Any]]) -> dict[str, Any]:
    status_codes = [row.get("status_code") for row in evidence_rows if isinstance(row.get("status_code"), int)]
    latency_ms = [row.get("latency_ms") for row in evidence_rows if isinstance(row.get("latency_ms"), (int, float))]
    blast = incident_payload.get("blastRadiusJson")
    route = None
    try:
        blast_obj = json.loads(blast) if isinstance(blast, str) else blast
        route = blast_obj.get("route") if isinstance(blast_obj, dict) else None
    except json.JSONDecodeError:
        route = None
    status_focus = max(status_codes) if status_codes else None
    latency_focus = max(latency_ms) if latency_ms else None
    summary_parts = [
        f"Incident {incident_payload.get('incidentId')} flagged as "
        f"{incident_payload.get('primaryClassification')} ({incident_payload.get('severity')})."
    ]
    if route:
        summary_parts.append(f" Blast radius references route `{route}`.")
    if status_focus and status_focus >= 500:
        summary_parts.append(f" Latest gateway telemetry shows HTTP {status_focus} responses.")
    elif status_focus:
        summary_parts.append(f" Latest HTTP status observed: {status_focus}.")
    if latency_focus:
        summary_parts.append(f" Peak sampled latency ~{int(latency_focus)} ms in evidence window.")
    summary = "".join(summary_parts)

    if status_focus and status_focus >= 500:
        probable = "Upstream dependency instability or timeout surfaced as 5xx responses at the gateway edge."
        action = "Inspect upstream health dashboards, connection pools, and circuit breaker open events for the implicated route."
        confidence = 0.78
    elif latency_focus and latency_focus > 1200:
        probable = "Latency regression consistent with slow downstream calls or thread pool saturation."
        action = "Compare P95/P99 latency for the upstream backing the route; validate autoscaling and pool sizing."
        confidence = 0.68
    else:
        probable = "Limited signal in captured evidence — treat as ambiguous until more correlated spans arrive."
        action = "Gather distributed traces for the incident window and widen the Elasticsearch query window."
        confidence = 0.45

    return {
        "summary": summary.strip(),
        "probable_root_cause": probable,
        "confidence": confidence,
        "recommended_action": action,
    }
