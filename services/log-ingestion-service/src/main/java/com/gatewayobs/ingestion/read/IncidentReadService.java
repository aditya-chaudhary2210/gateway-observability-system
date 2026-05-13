package com.gatewayobs.ingestion.read;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

@Service
public class IncidentReadService implements RowMapper<IncidentSummary> {

    private final JdbcTemplate jdbcTemplate;

    public IncidentReadService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<IncidentSummary> recent(int limit) {
        int safe = Math.min(Math.max(limit, 1), 200);
        return jdbcTemplate.query(
                """
                SELECT incident_id, opened_at, severity, status, primary_classification, fingerprint
                FROM incident
                ORDER BY opened_at DESC
                LIMIT ?
                """,
                this,
                safe);
    }

    public Optional<IncidentDetail> detail(UUID incidentId) {
        List<IncidentDetail> hits =
                jdbcTemplate.query(
                        """
                        SELECT incident_id, opened_at, closed_at, severity, status,
                               primary_classification, fingerprint,
                               CAST(blast_radius_hint AS TEXT) AS blast
                        FROM incident
                        WHERE incident_id = ?
                        """,
                        IncidentReadService::mapDetail,
                        incidentId);
        if (hits.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(hits.getFirst());
    }

    private static IncidentDetail mapDetail(ResultSet rs, int rowNum) throws SQLException {
        Timestamp opened = rs.getTimestamp("opened_at");
        Timestamp closed = rs.getTimestamp("closed_at");
        return new IncidentDetail(
                rs.getObject("incident_id", UUID.class),
                opened.toInstant(),
                closed == null ? null : closed.toInstant(),
                rs.getString("severity"),
                rs.getString("status"),
                rs.getString("primary_classification"),
                rs.getString("fingerprint"),
                rs.getString("blast"));
    }

    @Override
    public IncidentSummary mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new IncidentSummary(
                rs.getObject("incident_id", java.util.UUID.class),
                rs.getTimestamp("opened_at").toInstant(),
                rs.getString("severity"),
                rs.getString("status"),
                rs.getString("primary_classification"),
                rs.getString("fingerprint"));
    }
}
