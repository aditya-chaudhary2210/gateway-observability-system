package com.gatewayobs.ingestion.read;

import java.time.Instant;
import java.util.UUID;

public record IncidentDetail(
        UUID incidentId,
        Instant openedAt,
        Instant closedAt,
        String severity,
        String status,
        String primaryClassification,
        String fingerprint,
        String blastRadiusJson) {}
