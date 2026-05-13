package com.gatewayobs.detection.engine;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gatewayobs.detection.config.DetectionKafkaTopics;
import com.gatewayobs.detection.persistence.IncidentRepository;
import com.gatewayobs.detection.persistence.IncidentRepository.OpenedIncident;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
class IncidentSignalProcessor {

    private static final Logger log = LoggerFactory.getLogger(IncidentSignalProcessor.class);

    private final ObjectMapper mapper;
    private final KafkaTemplate<String, String> kafka;
    private final DetectionKafkaTopics topics;
    private final IncidentRepository incidentRepository;

    IncidentSignalProcessor(
            ObjectMapper mapper,
            KafkaTemplate<String, String> kafka,
            DetectionKafkaTopics topics,
            IncidentRepository incidentRepository) {
        this.mapper = mapper;
        this.kafka = kafka;
        this.topics = topics;
        this.incidentRepository = incidentRepository;
    }

    @KafkaListener(topics = "${detection.enriched-topic}", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(String payload) {
        try {
            JsonNode root = mapper.readTree(payload);
            JsonNode base = root.path("payload").path("base");
            if (base.isMissingNode()) {
                return;
            }

            String routeId = base.path("routeId").asText("unmapped");
            String upstream = base.path("upstreamId").asText("unknown");
            int status = base.path("statusCode").asInt();
            long latency = base.path("latencyMs").asLong();
            String eventId = base.path("eventId").asText(UUID.randomUUID().toString());

            boolean errorBurst = status >= 500;
            boolean latencyRegression = latency >= topics.latencyThresholdMs();

            if (!errorBurst && !latencyRegression) {
                return;
            }

            String classification = errorBurst ? "AVAILABILITY_UPSTREAM" : "LATENCY_REGRESSION";
            String severity = errorBurst ? "SEV3" : "SEV4";
            String fingerprint = fingerprint(routeId, upstream, classification);

            OpenedIncident incident =
                    incidentRepository.open(
                            routeId, upstream, fingerprint, severity, classification, latency, status, eventId);

            publishIncidentKafka(root, incident, classification, fingerprint, latency, status);

        } catch (Exception ex) {
            log.warn("Failed processing enrichment payload", ex);
        }
    }

    private static String fingerprint(String route, String upstream, String classification) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update((route + "|" + upstream + "|" + classification).getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(digest.digest());
    }

    private void publishIncidentKafka(
            JsonNode root, OpenedIncident incident, String classification, String fingerprint, long latency, int status)
            throws Exception {
        ObjectNode envelope = mapper.createObjectNode();
        envelope.put("schemaVersion", 1);
        envelope.put("eventType", "IncidentOpened");
        envelope.put("occurredAt", OffsetDateTime.now(ZoneOffset.UTC).toString());
        envelope.put("correlationId", root.path("correlationId").asText(""));
        ObjectNode payload = mapper.createObjectNode();
        payload.put("incidentId", incident.incidentId().toString());
        payload.put("severity", incident.severity());
        payload.put("classification", classification);
        payload.put("fingerprint", fingerprint);
        payload.put("latencyMs", latency);
        payload.put("statusCode", status);
        envelope.set("payload", payload);

        kafka.send(topics.incidentsTopic(), incident.incidentId().toString(), mapper.writeValueAsString(envelope));
    }
}
