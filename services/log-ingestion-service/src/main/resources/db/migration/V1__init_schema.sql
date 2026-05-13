CREATE TABLE gateway_event_projection (
    event_id UUID PRIMARY KEY,
    occurred_at TIMESTAMPTZ NOT NULL,
    trace_id TEXT,
    correlation_id TEXT,
    route_id TEXT,
    http_method TEXT,
    path_template TEXT,
    status_code INT,
    latency_ms BIGINT,
    upstream_id TEXT,
    client_id TEXT,
    classification_snapshot JSONB,
    raw_ref TEXT
);

CREATE INDEX IF NOT EXISTS idx_gep_occurred ON gateway_event_projection (occurred_at DESC);
CREATE INDEX IF NOT EXISTS idx_gep_route ON gateway_event_projection (route_id, occurred_at DESC);
CREATE INDEX IF NOT EXISTS idx_gep_corr ON gateway_event_projection (correlation_id);

CREATE TABLE processed_event (
    event_id UUID PRIMARY KEY,
    consumed_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE ingestion_checkpoint (
    consumer_group TEXT NOT NULL,
    topic TEXT NOT NULL,
    partition INT NOT NULL,
    committed_offset BIGINT NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (consumer_group, topic, partition)
);

CREATE TABLE incident (
    incident_id UUID PRIMARY KEY,
    opened_at TIMESTAMPTZ NOT NULL,
    closed_at TIMESTAMPTZ,
    severity TEXT NOT NULL,
    status TEXT NOT NULL,
    primary_classification TEXT,
    fingerprint TEXT NOT NULL,
    blast_radius_hint JSONB,
    detection_ruleset_version INT NOT NULL DEFAULT 1
);

CREATE INDEX IF NOT EXISTS idx_incident_status ON incident (status, severity);
CREATE INDEX IF NOT EXISTS idx_incident_fingerprint ON incident (fingerprint);

CREATE TABLE incident_event (
    id BIGSERIAL PRIMARY KEY,
    incident_id UUID NOT NULL REFERENCES incident (incident_id),
    type TEXT NOT NULL,
    payload JSONB,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE incident_rca (
    rca_id UUID PRIMARY KEY,
    incident_id UUID NOT NULL REFERENCES incident (incident_id),
    generation_version INT NOT NULL,
    status TEXT NOT NULL,
    structured JSONB,
    prompt_hash TEXT,
    model_metadata JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (incident_id, generation_version)
);
