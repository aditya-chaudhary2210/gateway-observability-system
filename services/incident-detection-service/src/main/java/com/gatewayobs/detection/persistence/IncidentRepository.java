package com.gatewayobs.detection.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class IncidentRepository {

    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;

    IncidentRepository(JdbcTemplate jdbc, ObjectMapper mapper) {
        this.jdbc = jdbc;
        this.mapper = mapper;
    }

    @Transactional
    public OpenedIncident open(
            String routeId,
            String upstream,
            String fingerprint,
            String severity,
            String classification,
            long latency,
            int status,
            String sourceEventId) {
        UUID incidentId = UUID.randomUUID();
        ObjectNode blast = mapper.createObjectNode();
        blast.put("route", routeId);
        blast.put("upstream", upstream);
        blast.put("latencyMs", latency);
        blast.put("statusCode", status);

        jdbc.update(
                """
                INSERT INTO incident (incident_id, opened_at, closed_at, severity, status, primary_classification, fingerprint, blast_radius_hint, detection_ruleset_version)
                VALUES (?, NOW(), NULL, ?, 'OPEN', ?, ?, CAST(? AS JSONB), 1)
                """,
                incidentId,
                severity,
                classification,
                fingerprint,
                blast.toString());

        ObjectNode eventPayload = mapper.createObjectNode();
        eventPayload.put("sourceEventId", sourceEventId);
        eventPayload.put("routeId", routeId);
        eventPayload.put("upstreamId", upstream);

        jdbc.update(
                """
                INSERT INTO incident_event (incident_id, type, payload)
                VALUES (?, 'OPENED', CAST(? AS JSONB))
                """,
                incidentId,
                eventPayload.toString());

        return new OpenedIncident(incidentId, severity, classification, fingerprint);
    }

    public record OpenedIncident(UUID incidentId, String severity, String classification, String fingerprint) {}
}
