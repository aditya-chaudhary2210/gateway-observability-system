/*
 * Validates: gateway-access-v1 consume → Postgres + Elasticsearch → telemetry-enriched-v1 → detection → incident row.
 * Requires Docker (Testcontainers).
 */
package com.gatewayobs.it.pipeline;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(
        classes = PipelineIntegrationHarnessApplication.class,
        properties = {
            "management.tracing.enabled=false",
            "management.otlp.tracing.export.enabled=false"
        })
class PipelineKafkaIngestDetectionTest {

    /** Official Apache Kafka image (KRaft); Confluent cp-kafka 7.6+ no longer matches Testcontainers' boot path in CI. */
    private static final DockerImageName KAFKA_IMAGE = DockerImageName.parse("apache/kafka:3.8.1");

    private static final DockerImageName ES_IMAGE =
            DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch:8.15.3");

    @Container
    static final KafkaContainer KAFKA = new KafkaContainer(KAFKA_IMAGE);

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    static final ElasticsearchContainer ELASTICSEARCH = new ElasticsearchContainer(ES_IMAGE);

    @DynamicPropertySource
    static void infra(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);

        registry.add(
                "spring.elasticsearch.uris",
                () -> "http://" + ELASTICSEARCH.getHost() + ":" + ELASTICSEARCH.getMappedPort(9200));

        registry.add("ingestion.gateway-topic", () -> "gateway-access-v1");
        registry.add("ingestion.enriched-topic", () -> "telemetry-enriched-v1");
        registry.add("ingestion.es-index", () -> "gateway-events-active");
        registry.add("detection.enriched-topic", () -> "telemetry-enriched-v1");
        registry.add("detection.incidents-topic", () -> "incidents-v1");
        registry.add("detection.latency-threshold-ms", () -> 50L);
    }

    @Autowired KafkaTemplate<String, String> kafkaTemplate;

    @Autowired JdbcTemplate jdbcTemplate;

    @Test
    void ingestion_emits_enriched_detection_opens_incident() {
        UUID eventId = UUID.randomUUID();
        String correlation = UUID.randomUUID().toString();
        String envelope =
                """
                {"schemaVersion":1,"eventType":"GatewayAccessEmitted","occurredAt":"2026-05-01T12:34:56Z","traceContext":{"traceId":"","spanId":"","traceparentHeader":""},"correlationId":"%s","producer":{"service":"test","instanceId":"it"},"payload":{"eventId":"%s","routeId":"upstream-mock","httpMethod":"GET","pathTemplate":"/mock/error","statusCode":503,"latencyMs":5,"upstreamId":"localhost","clientIdHash":"anonymous","clientIp":"","userAgentSnippet":""}}
                """
                        .formatted(correlation, eventId);

        kafkaTemplate.send("gateway-access-v1", "upstream-mock", envelope);

        Awaitility.await()
                .atMost(55, TimeUnit.SECONDS)
                .pollInterval(200, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> assertThat(rows("SELECT COUNT(*) FROM incident")).isGreaterThanOrEqualTo(1));

        assertThat(rows(String.format(
                        "SELECT COUNT(*) FROM gateway_event_projection WHERE event_id = '%s'", eventId)))
                .isEqualTo(1);
    }

    private int rows(String sql) {
        List<Integer> list = jdbcTemplate.query(sql, (rs, i) -> rs.getInt(1));
        assertThat(list).isNotEmpty();
        return list.getFirst();
    }
}
