from fastapi import APIRouter

from app.settings import settings

router = APIRouter(tags=["health"])


@router.get("/health")
def health_probe() -> dict[str, str]:
    cfg = settings.llm_http_config()
    return {
        "status": "READY",
        "llm_backend": settings.llm_backend,
        "tier": settings.llm_backend if cfg else "heuristic",
        "groq_configured": bool((settings.groq_api_key or "").strip()),
    }
