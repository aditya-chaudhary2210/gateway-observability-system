/*
 * Redis-backed rate limit + StripPrefix upstream to WireMock, then verifies the gateway emits
 * GatewayAccessEmitted on gateway-access-v1 (Kafka). Requires Docker (Testcontainers).
 */
package com.gatewayobs.it.gatewayhttp;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gatewayobs.gateway.GatewayObservabilityApplication;
import com.gatewayobs.telemetry.model.Topics;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.KafkaAdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.TopicExistsException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;
import com.github.tomakehurst.wiremock.client.WireMock;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(
        classes = GatewayObservabilityApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "management.tracing.enabled=false",
            "management.otlp.tracing.export.enabled=false",
            /* MicrometerKafkaClientMetricsSender + observation can deadlock or stall first async send on reactive threads under load. */
            "spring.kafka.template.observation-enabled=false",
            "gateway.rate-limit.replenish=600",
            "gateway.rate-limit.burst=900"
        })
class GatewayHttpToKafkaIntegrationTest {

    /** Official Apache Kafka image (KRaft); matches Testcontainers KafkaContainer wait/entrypoint. */
    private static final DockerImageName KAFKA_IMAGE = DockerImageName.parse("apache/kafka:3.8.1");

    private static final DockerImageName WIREMOCK_IMAGE =
            DockerImageName.parse("docker.io/wiremock/wiremock:3.13.1");

    private static final Duration HTTP_CONNECT = Duration.ofSeconds(10);
    private static final Duration HTTP_REQUEST = Duration.ofSeconds(45);

    @Container
    static final KafkaContainer KAFKA = new KafkaContainer(KAFKA_IMAGE);

    @SuppressWarnings("resource")
    @Container
    static final GenericContainer<?> REDIS =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);

    @SuppressWarnings("resource")
    @Container
    static final GenericContainer<?> WIREMOCK_DOCKER =
            new GenericContainer<>(WIREMOCK_IMAGE).withExposedPorts(8080);

    @Autowired ObjectMapper objectMapper;

    @LocalServerPort int serverPort;

    private KafkaConsumer<String, String> consumer;

    @DynamicPropertySource
    static void infra(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);

        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));

        registry.add("UPSTREAM_MOCK_HOST", WIREMOCK_DOCKER::getHost);
        registry.add("UPSTREAM_MOCK_PORT", () -> WIREMOCK_DOCKER.getMappedPort(8080));
    }

    @BeforeEach
    void stubsAndKafkaSeekEnd() {
        ensureGatewayAccessTopicPresent();
        WireMock.configureFor(WIREMOCK_DOCKER.getHost(), WIREMOCK_DOCKER.getMappedPort(8080));
        WireMock.reset();
        WireMock.stubFor(
                get(urlEqualTo("/integration-ok")).willReturn(aResponse().withStatus(200).withBody("upstream-ok")));

        consumer = createConsumer(UUID.randomUUID().toString());
        consumer.subscribe(List.of(Topics.GATEWAY_ACCESS_V1));
        Awaitility.await().atMost(15, TimeUnit.SECONDS).untilAsserted(() -> {
            consumer.poll(Duration.ofMillis(250));
            assertThat((Collection<?>) consumer.assignment()).isNotEmpty();
        });
        consumer.seekToEnd(consumer.assignment());
        consumer.poll(Duration.ofMillis(200));
        drainOutstanding(consumer, Duration.ofSeconds(5));
    }

    @AfterEach
    void stopConsumer() {
        if (consumer != null) {
            consumer.close(Duration.ofSeconds(5));
        }
        consumer = null;
    }

    @Test
    void http_via_gateway_emits_kafka_gateway_access_envelope() {
        awaitGatewayHealthy();
        assertIngressViaMockRoute();

        String upstreamAuthority =
                "%s:%d".formatted(WIREMOCK_DOCKER.getHost(), WIREMOCK_DOCKER.getMappedPort(8080));

        Awaitility.await().atMost(60, TimeUnit.SECONDS).untilAsserted(() -> {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(400));
            assertThat(records.isEmpty()).isFalse();
            for (ConsumerRecord<String, String> r : records) {
                if (!Topics.GATEWAY_ACCESS_V1.equals(r.topic())) {
                    continue;
                }
                JsonNode root = objectMapper.readTree(r.value());
                JsonNode payload = root.path("payload");
                if (!payload.path("pathTemplate").asText("").startsWith("/mock/integration-ok")) {
                    continue;
                }
                validateEnvelope(root, payload, upstreamAuthority);
                return;
            }
            throw new AssertionError("expected gateway-access envelope for /mock/integration-ok");
        });
    }

    /** Avoid Reactor Netty WebTestClient flakes (PrematureCloseException) against a cold gateway on CI. */
    private void awaitGatewayHealthy() {
        HttpClient client = HttpClient.newBuilder().connectTimeout(HTTP_CONNECT).build();
        Awaitility.await().atMost(90, TimeUnit.SECONDS).pollInterval(250, TimeUnit.MILLISECONDS).untilAsserted(() -> {
            try {
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create("http://127.0.0.1:" + serverPort + "/actuator/health"))
                        .timeout(Duration.ofSeconds(10))
                        .GET()
                        .build();
                HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
                assertThat(resp.statusCode()).isEqualTo(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AssertionError(e);
            } catch (IOException e) {
                throw new AssertionError("health check failed: " + e.getMessage(), e);
            }
        });
    }

    private void assertIngressViaMockRoute() {
        HttpClient client = HttpClient.newBuilder().connectTimeout(HTTP_CONNECT).build();
        Awaitility.await().atMost(60, TimeUnit.SECONDS).pollInterval(400, TimeUnit.MILLISECONDS).untilAsserted(() -> {
            try {
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create("http://127.0.0.1:" + serverPort + "/mock/integration-ok"))
                        .timeout(HTTP_REQUEST)
                        .GET()
                        .build();
                HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
                assertThat(resp.statusCode()).isEqualTo(200);
                assertThat(resp.body()).isEqualTo("upstream-ok");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AssertionError(e);
            } catch (IOException e) {
                throw new AssertionError("ingress GET failed: " + e.getMessage(), e);
            }
        });
    }

    private void validateEnvelope(JsonNode root, JsonNode payload, String expectedUpstreamAuthority) {
        assertThat(root.path("eventType").asText()).isEqualTo("GatewayAccessEmitted");
        assertThat(root.path("schemaVersion").asInt()).isEqualTo(1);
        assertThat(root.path("correlationId").asText()).isNotBlank();

        assertThat(payload.path("routeId").asText()).isEqualTo("upstream-mock");
        assertThat(payload.path("httpMethod").asText()).isEqualTo("GET");
        assertThat(payload.path("statusCode").asInt()).isEqualTo(200);
        assertThat(payload.path("latencyMs").asLong()).isGreaterThanOrEqualTo(0L);

        assertThat(payload.path("upstreamId").asText()).isEqualTo(expectedUpstreamAuthority);
        assertThat(payload.path("eventId").asText()).isNotBlank();
    }

    private static KafkaConsumer<String, String> createConsumer(String groupId) {
        Map<String, Object> cfg = new HashMap<>();
        cfg.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers());
        cfg.put(ConsumerConfig.GROUP_ID_CONFIG, "it-gateway-http-" + groupId);
        cfg.put(
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class.getName());
        cfg.put(
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class.getName());
        cfg.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        // seekToEnd + auto-commit races can rewind / skip envelopes on CI; deterministic manual position only.
        cfg.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");

        return new KafkaConsumer<>(cfg);
    }

    private static void drainOutstanding(KafkaConsumer<String, String> c, Duration maxWall) {
        long deadlineNanos = System.nanoTime() + maxWall.toNanos();
        while (System.nanoTime() < deadlineNanos && !c.poll(Duration.ofMillis(200)).isEmpty()) {}
    }

    /** KRaft Testcontainers Kafka may disallow auto-topic create; ingestion IT uses its own broker so this topic is never pre-created across modules. */
    private static void ensureGatewayAccessTopicPresent() {
        Map<String, Object> adminCfg =
                Map.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers());
        try (AdminClient admin = KafkaAdminClient.create(adminCfg)) {
            admin.createTopics(List.of(new NewTopic(Topics.GATEWAY_ACCESS_V1, 1, (short) 1)))
                    .all()
                    .get(60, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof TopicExistsException) {
                return;
            }
            throw new AssertionError("create topic " + Topics.GATEWAY_ACCESS_V1, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError(e);
        } catch (java.util.concurrent.TimeoutException e) {
            throw new AssertionError("create topic " + Topics.GATEWAY_ACCESS_V1, e);
        }
    }
}
