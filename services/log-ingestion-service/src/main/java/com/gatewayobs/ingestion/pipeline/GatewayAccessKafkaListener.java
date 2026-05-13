package com.gatewayobs.ingestion.pipeline;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
class GatewayAccessKafkaListener {

    private final TelemetryIngestionService ingestionService;

    GatewayAccessKafkaListener(TelemetryIngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @KafkaListener(topics = "${ingestion.gateway-topic}", groupId = "${spring.kafka.consumer.group-id}")
    void consume(String message) {
        ingestionService.ingest(message);
    }
}
