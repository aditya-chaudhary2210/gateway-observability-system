package com.gatewayobs.ingestion.pipeline;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class RelationalTelemetryWriter {

    private final JdbcTemplate jdbcTemplate;

    RelationalTelemetryWriter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** Atomically stakes claim on duplicates via processed_event sentinel row. */
    @Transactional
    public Optional<UUID> persistIfAbsent(UUID eventId, PersistRequest request) {
        int inserted =
                jdbcTemplate.update(
                        """
                        INSERT INTO processed_event (event_id) VALUES (?)
                        ON CONFLICT (event_id) DO NOTHING
                        """,
                        eventId);
        if (inserted != 1) {
            return Optional.empty();
        }

        jdbcTemplate.update(
                """
                INSERT INTO gateway_event_projection (
                    event_id, occurred_at, trace_id, correlation_id, route_id, http_method, path_template,
                    status_code, latency_ms, upstream_id, client_id, classification_snapshot, raw_ref)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS JSONB), ?)
                """,
                eventId,
                Timestamp.from(request.occurredAt()),
                emptyToNull(request.traceId()),
                emptyToNull(request.correlationId()),
                request.routeId(),
                request.httpMethod(),
                request.pathTemplate(),
                request.statusCode(),
                request.latencyMs(),
                request.upstreamId(),
                request.clientId(),
                request.classificationJson(),
                request.rawRef());

        return Optional.of(eventId);
    }

    void updateElasticsearchPointer(UUID eventId, String esPointer) {
        jdbcTemplate.update(
                """
                UPDATE gateway_event_projection SET raw_ref = ?
                WHERE event_id = ?
                """,
                esPointer,
                eventId);
    }

    private static String emptyToNull(String raw) {
        return raw == null || raw.isBlank() ? null : raw;
    }

    record PersistRequest(
            Instant occurredAt,
            String traceId,
            String correlationId,
            String routeId,
            String httpMethod,
            String pathTemplate,
            int statusCode,
            long latencyMs,
            String upstreamId,
            String clientId,
            String classificationJson,
            String rawRef) {}
}
