package com.gatewayobs.ingestion.read;

import java.time.Instant;
import java.util.UUID;

public record IncidentSummary(
        UUID incidentId,
        Instant openedAt,
        String severity,
        String status,
        String primaryClassification,
        String fingerprint) {}
