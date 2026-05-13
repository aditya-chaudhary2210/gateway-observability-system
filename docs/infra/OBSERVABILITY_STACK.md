# Observability Stack (Platform Self-Observability)

<!--
  Ownership: Platform SRE
-->

## 1. Why

The system **eats its own telemetry dogfood partially** — we must observe pipeline health independently of business logs inside ES.

Layers:

1. **Metrics:** Prometheus scraping Spring Boot `/actuator/prometheus`.
2. **Dashboards:** Grafana as code in `deploy/grafana/`.
3. **Tracing:** OpenTelemetry exporters → Collector → Tempo/Jaeger **(integration Phase 7)**.
4. **Logs:** Structured JSON to stdout captured by cluster agent (Loki optional future).

---

## 2. RED / USE Highlights

### Gateway

| Metric | Type | Description |
|--------|------|-------------|
| `http_server_requests_seconds` | Histogram | Latency |
| `gateway_kafka_produce_total` | Counter | Success/fail |
| `resilience4j_circuitbreaker_state` | Gauge | CB state |
| `gateway_rate_limit_rejections_total` | Counter | RL hits |

### Ingestion

| Metric | Type | Description |
|--------|------|-------------|
| `kafka_consumer_lag` | Gauge | Per partition |
| `ingestion_es_bulk_failures_total` | Counter | Index errors |
| `ingestion_processing_latency_seconds` | Histogram | End-to-end internal |

### Detection

| Metric | Type | Description |
|--------|------|-------------|
| `incidents_open_total` | Counter | Created |
| `detection_rule_eval_seconds` | Histogram | Rule latency |

### AI

| Metric | Type | Description |
|--------|------|-------------|
| `ai_llm_requests_total` | Counter | Calls |
| `ai_llm_latency_seconds` | Histogram | Provider RTT |
| `ai_token_usage_total` | Counter | Approximate |

---

## 3. SLO Sketch (Portfolio Level)

| SLI | Target (demo posture) |
|-----|------------------------|
| Gateway success (non-simulated) | 99% over 24h |
| Telemetry produce error rate | < 0.1% |
| Ingestion lag p95 | < 30s under nominal load |

Formal error budget policy future work.

---

## 4. Alerting Principles

Avoid noisy LLM alerts. Start with infrastructure:

- Kafka consumer lag sustained
- ES cluster health red
- AI provider 5xx spike (token spend guard)

---

## 5. Correlation & Trace Context

Propagate W3C `traceparent` end-to-end; span names:

- `gateway.route`
- `ingestion.processBatch`
- `detection.evaluateWindow`
- `ai.generateRca`

---

## 6. Related

- [../architecture/HLD.md](../architecture/HLD.md)
- [../architecture/FAILURE_HANDLING.md](../architecture/FAILURE_HANDLING.md)
