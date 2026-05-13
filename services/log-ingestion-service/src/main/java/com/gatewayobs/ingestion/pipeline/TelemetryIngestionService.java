package com.gatewayobs.ingestion.pipeline;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.json.JsonData;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gatewayobs.ingestion.config.IngestionProperties;
import com.gatewayobs.telemetry.model.TelemetryKafkaEnvelope;
import com.gatewayobs.telemetry.model.TelemetryKafkaEnvelope.GatewayAccessPayload;
import com.gatewayobs.telemetry.model.TelemetryKafkaEnvelope.ProducerRef;
import com.gatewayobs.telemetry.model.TelemetryKafkaEnvelope.TelemetryEnrichedPayload;
import com.gatewayobs.telemetry.model.TelemetryKafkaEnvelope.TraceContextPayload;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
class TelemetryIngestionService {

    private static final Logger log = LoggerFactory.getLogger(TelemetryIngestionService.class);

    private final ObjectMapper objectMapper;
    private final RelationalTelemetryWriter relationalTelemetryWriter;
    private final ElasticsearchClient elasticsearchClient;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final IngestionProperties properties;
    private final Counter processed;
    private final Counter duplicates;
    private final Counter failures;

    TelemetryIngestionService(
            ObjectMapper objectMapper,
            RelationalTelemetryWriter relationalTelemetryWriter,
            ElasticsearchClient elasticsearchClient,
            KafkaTemplate<String, String> kafkaTemplate,
            IngestionProperties properties,
            MeterRegistry registry) {
        this.objectMapper = objectMapper;
        this.relationalTelemetryWriter = relationalTelemetryWriter;
        this.elasticsearchClient = elasticsearchClient;
        this.kafkaTemplate = kafkaTemplate;
        this.properties = properties;
        this.processed = registry.counter("ingestion_events_processed_total", "result", "success");
        this.duplicates = registry.counter("ingestion_events_processed_total", "result", "duplicate");
        this.failures = registry.counter("ingestion_events_processed_total", "result", "failure");
    }

    public void ingest(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode payloadNode = root.path("payload");
            UUID eventId = UUID.fromString(payloadNode.path("eventId").asText());
            Instant occurredAt = Instant.now();
            String traceId = root.path("traceContext").path("traceId").asText("");
            String correlationId = root.path("correlationId").asText("");
            String routeId = payloadNode.path("routeId").asText("unmapped");
            String method = payloadNode.path("httpMethod").asText("");
            String pathTemplate = payloadNode.path("pathTemplate").asText("");
            int status = payloadNode.path("statusCode").asInt();
            long latency = payloadNode.path("latencyMs").asLong();
            String upstream = payloadNode.path("upstreamId").asText("");
            String clientId = payloadNode.path("clientIdHash").asText("");

            var relationalRequest =
                    new RelationalTelemetryWriter.PersistRequest(
                            occurredAt,
                            traceId,
                            correlationId,
                            routeId,
                            method,
                            pathTemplate,
                            status,
                            latency,
                            upstream,
                            clientId,
                            "{}",
                            null);

            Optional<UUID> inserted = relationalTelemetryWriter.persistIfAbsent(eventId, relationalRequest);
            if (inserted.isEmpty()) {
                duplicates.increment();
                return;
            }

            String esDocId = indexElasticsearch(eventId, occurredAt, traceId, correlationId, routeId, method, pathTemplate, status, latency, upstream, clientId);
            relationalTelemetryWriter.updateElasticsearchPointer(eventId, esDocId);

            publishEnriched(root, payloadNode, esDocId);
            processed.increment();
        } catch (Exception ex) {
            failures.increment();
            log.warn("Failed ingesting gateway telemetry payload", ex);
        }
    }

    private String indexElasticsearch(
            UUID eventId,
            Instant occurredAt,
            String traceId,
            String correlationId,
            String routeId,
            String method,
            String pathTemplate,
            int status,
            long latency,
            String upstream,
            String clientId) {
        Map<String, Object> document = new HashMap<>();
        document.put("event_id", eventId.toString());
        document.put("occurred_at", occurredAt.toString());
        document.put("trace_id", traceId);
        document.put("correlation_id", correlationId);
        document.put("route_id", routeId);
        document.put("http_method", method);
        document.put("path_template", pathTemplate);
        document.put("status_code", status);
        document.put("latency_ms", latency);
        document.put("upstream_id", upstream);
        document.put("client_id_hash", clientId);
        try {
            elasticsearchClient.index(
                    IndexRequest.of(
                            idx ->
                                    idx.index(properties.esIndex())
                                            .id(eventId.toString())
                                            .document(JsonData.of(document))));
            return eventId.toString();
        } catch (Exception ex) {
            log.warn("Elasticsearch indexing failed for event {}", eventId, ex);
            return "unindexed";
        }
    }

    private void publishEnriched(JsonNode root, JsonNode payloadNode, String esDocId) throws Exception {
        GatewayAccessPayload base = objectMapper.treeToValue(payloadNode, GatewayAccessPayload.class);
        TraceContextPayload trace = readTrace(root);
        ProducerRef producer = readProducer(root);
        String correlation = root.path("correlationId").asText("");
        TelemetryEnrichedPayload enrichedPayload = new TelemetryEnrichedPayload(base, true, esDocId);
        var envelope =
                new TelemetryKafkaEnvelope<>(
                        root.path("schemaVersion").asInt(1),
                        "TelemetryEnriched",
                        OffsetDateTime.now(ZoneOffset.UTC).toString(),
                        trace,
                        correlation,
                        producer,
                        enrichedPayload);
        kafkaTemplate.send(properties.enrichedTopic(), base.routeId(), objectMapper.writeValueAsString(envelope));
    }

    private TraceContextPayload readTrace(JsonNode root) {
        JsonNode trace = root.path("traceContext");
        if (trace.isMissingNode() || trace.isNull()) {
            return new TraceContextPayload("", "", "");
        }
        return new TraceContextPayload(
                trace.path("traceId").asText(""),
                trace.path("spanId").asText(""),
                trace.path("traceparentHeader").asText(""));
    }

    private ProducerRef readProducer(JsonNode root) {
        JsonNode producer = root.path("producer");
        if (producer.isMissingNode() || producer.isNull()) {
            return new ProducerRef("unknown-producer", "unknown-instance");
        }
        return new ProducerRef(
                producer.path("service").asText("unknown-producer"),
                producer.path("instanceId").asText("unknown-instance"));
    }
}
