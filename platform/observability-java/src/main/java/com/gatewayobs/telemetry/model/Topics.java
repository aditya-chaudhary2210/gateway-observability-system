package com.gatewayobs.telemetry.model;

/** Utility constants for deterministic topic naming. */
public final class Topics {
    /** Raw gateway emits. */
    public static final String GATEWAY_ACCESS_V1 = "gateway-access-v1";

    /** Post-ingestion fan-out feeding detection/advanced analytics. */
    public static final String TELEMETRY_ENRICHED_V1 = "telemetry-enriched-v1";

    /** Incident lifecycle / detection outputs. */
    public static final String INCIDENTS_V1 = "incidents-v1";

    private Topics() {}
}
