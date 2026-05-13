"""OpenTelemetry OTLP traces + logging correlation for FastAPI."""

from __future__ import annotations

import logging
import os
from urllib.parse import urljoin

from opentelemetry import trace
from opentelemetry.exporter.otlp.proto.http.trace_exporter import OTLPSpanExporter
from opentelemetry.instrumentation.logging import LoggingInstrumentor
from opentelemetry.sdk.resources import Resource
from opentelemetry.sdk.trace import TracerProvider
from opentelemetry.sdk.trace.export import BatchSpanProcessor

_LOGGER = logging.getLogger(__name__)


def _normalize_traces_endpoint(raw: str) -> str:
    base = raw.strip().rstrip("/")
    if base.endswith("/v1/traces"):
        return base
    return urljoin(base + "/", "v1/traces")


_CONFIGURED = False


def configure_observability(service_name: str | None = None) -> None:
    """
    Honour OTEL_SDK_DISABLED=true or OTEL_TRACES_EXPORTER=none to skip exporting.
    Defaults follow https://opentelemetry.io/docs/specs/otel/configuration/sdk-environment-variables/
    """

    global _CONFIGURED

    if _CONFIGURED:
        return

    if os.getenv("OTEL_SDK_DISABLED", "").strip().lower() in ("true", "1"):
        _CONFIGURED = True
        return

    exporter_mode = os.getenv("OTEL_TRACES_EXPORTER", "otlp").strip().lower()
    if exporter_mode in ("none", "noop"):
        _CONFIGURED = True
        return

    name = (service_name or os.getenv("OTEL_SERVICE_NAME") or "ai-analysis-service").strip()
    traces_ep = (
        os.getenv("OTEL_EXPORTER_OTLP_TRACES_ENDPOINT")
        or os.getenv("OTEL_EXPORTER_OTLP_ENDPOINT")
        or "http://127.0.0.1:4318/v1/traces"
    )
    endpoint = _normalize_traces_endpoint(traces_ep)

    headers = {}
    hdr = os.getenv("OTEL_EXPORTER_OTLP_HEADERS", "").strip()
    if hdr:
        for pair in hdr.split(","):
            if "=" in pair:
                k, v = pair.split("=", 1)
                headers[k.strip()] = v.strip()

    provider = TracerProvider(resource=Resource.create({"service.name": name}))
    trace.set_tracer_provider(provider)
    exporter = OTLPSpanExporter(endpoint=endpoint, headers=headers or None)
    provider.add_span_processor(BatchSpanProcessor(exporter))

    LoggingInstrumentor().instrument(set_logging_format=True)

    _LOGGER.info("OpenTelemetry OTLP exporter enabled endpoint=%s service=%s", endpoint, name)
    _CONFIGURED = True
