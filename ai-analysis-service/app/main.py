"""FastAPI entrypoint orchestrating RCA JSON contracts."""

from fastapi import FastAPI

from app import telemetry
from app.routes import health, rca


def create_app() -> FastAPI:
    telemetry.configure_observability()

    app = FastAPI(title="AI Analysis Service", version="0.1.2")
    app.include_router(health.router, prefix="/api/ai/rca")
    app.include_router(rca.router, prefix="/api/ai/rca")

    from opentelemetry.instrumentation.fastapi import FastAPIInstrumentor

    FastAPIInstrumentor.instrument_app(
        app, excluded_urls="/api/ai/rca/health,/openapi.json,/docs,/docs/oauth2-redirect,/redoc"
    )
    return app


app = create_app()
