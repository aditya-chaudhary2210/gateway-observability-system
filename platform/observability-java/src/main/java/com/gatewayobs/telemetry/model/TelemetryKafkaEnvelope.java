/*
 * Ownership: Platform / shared library
 *
 * Telemetry envelope aligns with docs/architecture/EVENT_DRIVEN_ARCHITECTURE.md
 */
package com.gatewayobs.telemetry.model;

/**
 * Canonical Kafka envelope wrapping versioned payloads and trace context propagation fields.
 *
 * @param schemaVersion Logical JSON schema iteration.
 * @param eventType Stable event name (GatewayAccessEmitted, TelemetryEnriched, ...
 */
public record TelemetryKafkaEnvelope<T>(
        int schemaVersion,
        String eventType,
        String occurredAtIso,
        TraceContextPayload traceContext,
        String correlationId,
        ProducerRef producer,
        T payload)
{
    /** HTTP-friendly trace propagation (W3C traceparent excerpt). */
    public record TraceContextPayload(String traceId, String spanId, String traceparentHeader) {}

    public record ProducerRef(String service, String instanceId) {}

    /** Gateway access telemetry payload persisted + indexed downstream. */
    public record GatewayAccessPayload(
            String eventId,
            String routeId,
            String httpMethod,
            String pathTemplate,
            int statusCode,
            long latencyMs,
            String upstreamId,
            String clientIdHash,
            String clientIp,
            String userAgentSnippet) {}

    /** Enriched telemetry emitted after ingestion normalisation/persistence hints. */
    public record TelemetryEnrichedPayload(GatewayAccessPayload base, boolean persistedPostgres, String esDocId)
    {}
}
