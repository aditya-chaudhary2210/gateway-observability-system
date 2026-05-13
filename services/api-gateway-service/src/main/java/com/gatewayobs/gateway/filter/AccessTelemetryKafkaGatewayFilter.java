package com.gatewayobs.gateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gatewayobs.telemetry.model.TelemetryKafkaEnvelope;
import com.gatewayobs.telemetry.model.TelemetryKafkaEnvelope.GatewayAccessPayload;
import com.gatewayobs.telemetry.model.TelemetryKafkaEnvelope.ProducerRef;
import com.gatewayobs.telemetry.model.TelemetryKafkaEnvelope.TraceContextPayload;
import com.gatewayobs.telemetry.model.Topics;
import com.gatewayobs.gateway.trace.TracePropagation;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Component
/**
 * Publishes structured gateway envelopes to Kafka once the downstream exchange completes — fire-and-forget on
 * {@link Schedulers#boundedElastic()} to avoid blocking the Netty loop.
 */
@ConditionalOnProperty(name = "gateway.telemetry.kafka.enabled", havingValue = "true", matchIfMissing = true)
class AccessTelemetryKafkaGatewayFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(AccessTelemetryKafkaGatewayFilter.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String instanceId;
    private final Counter success;
    private final Counter failures;

    AccessTelemetryKafkaGatewayFilter(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            MeterRegistry registry,
            @Value("${gateway.instance-id:${HOSTNAME:gateway-unknown}}") String instanceId) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.instanceId = instanceId;
        this.success = registry.counter("gateway_telemetry_publish_total", "result", "success");
        this.failures = registry.counter("gateway_telemetry_publish_total", "result", "failure");
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        var startNanos = System.nanoTime();
        // Completes routing + response on reactor-netty, then offload emit() only onto boundedElastic. Using
        // defer + subscribeOn on the runnable avoids shifting chain.filter upstream (breaks lifecycle) AND avoids
        // publishOn quirks where the post-commit worker never runs reliably for some Gateway paths.
        return chain.filter(exchange)
                .then(Mono.defer(
                        () -> Mono.<Void>fromRunnable(() -> emit(exchange, startNanos))
                                .subscribeOn(Schedulers.boundedElastic())));
    }

    private void emit(ServerWebExchange exchange, long startNanos) {
        try {
            var req = exchange.getRequest();
            var resp = exchange.getResponse();
            var route = resolveRouteOptional(exchange);
            var latencyMs = Math.max(0L, (System.nanoTime() - startNanos) / 1_000_000L);
            var trace = TracePropagation.traceFrom(req);
            String correlation =
                    Optional.ofNullable(req.getHeaders().getFirst(CorrelationIdGatewayFilter.HEADER_CORRELATION))
                            .filter(s -> !s.isBlank())
                            .orElse(UUID.randomUUID().toString());

            UUID eventId = UUID.randomUUID();
            String path = Optional.ofNullable(req.getPath()).map(ob -> ob.value()).orElse("");
            if (req.getURI().getRawQuery() != null) {
                path = path + "?q=stripped";
            }
            var uaShort = truncate(Optional.ofNullable(req.getHeaders().getFirst(HttpHeaders.USER_AGENT)).orElse(""), 140);

            String routeId = route.map(Route::getId).orElse("unmapped");
            String upstreamId =
                    route.map(Route::getUri).map(uri -> uri.getAuthority() != null ? uri.getAuthority() : uri.getHost()).orElse("unknown");

            var payload =
                    new GatewayAccessPayload(
                            eventId.toString(),
                            routeId,
                            req.getMethod().name(),
                            anonymizeTemplates(path),
                            resp.getStatusCode() != null ? resp.getStatusCode().value() : 0,
                            latencyMs,
                            upstreamId,
                            summarizeAuthorization(req.getHeaders().getFirst(HttpHeaders.AUTHORIZATION)),
                            snippetClientIp(req.getHeaders().getFirst("X-Forwarded-For"), req),
                            uaShort);

            var envelope =
                    new TelemetryKafkaEnvelope<>(
                            1,
                            "GatewayAccessEmitted",
                            OffsetDateTime.now(ZoneOffset.UTC).toString(),
                            new TraceContextPayload(trace.traceId(), trace.spanId(), trace.traceparent()),
                            correlation,
                            new ProducerRef("api-gateway-service", instanceId),
                            payload);

            String jsonPayload = objectMapper.writeValueAsString(envelope);
            kafkaTemplate.send(Topics.GATEWAY_ACCESS_V1, payload.routeId(), jsonPayload).get(5, java.util.concurrent.TimeUnit.SECONDS);
            success.increment();
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            failures.increment();
            log.warn("Kafka publish interrupted", interrupted);
        } catch (JsonProcessingException e) {
            failures.increment();
            log.warn("Failed serializing telemetry envelope", e);
        } catch (java.util.concurrent.ExecutionException | java.util.concurrent.TimeoutException e) {
            failures.increment();
            log.warn("Kafka publish timed out/failed waiting for acknowledgement", e);
        } catch (Exception e) {
            failures.increment();
            log.warn("Failed publishing telemetry", e);
        }
    }

    private static Optional<Route> resolveRouteOptional(ServerWebExchange exchange) {
        return Optional.ofNullable(exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR));
    }

    private static String anonymizeTemplates(String path) {
        if (path.isEmpty()) {
            return path;
        }
        return path.replaceAll("/[0-9a-fA-F]{8}-[0-9a-fA-F-]{36}", "/{uuid}").replaceAll("/\\d{5,}", "/{num}");
    }

    private static String truncate(String raw, int max) {
        if (raw.length() <= max) {
            return raw;
        }
        return raw.substring(0, max);
    }

    /** Never propagate raw bearer secrets — summarise only for grouping. */
    private static String summarizeAuthorization(String authorization) {
        if (authorization == null || authorization.isBlank()) {
            return "anonymous";
        }
        String lower = authorization.toLowerCase().trim();
        if (lower.startsWith("bearer ")) {
            var token = authorization.substring(authorization.indexOf(' ') + 1).trim();
            int hash = token.hashCode();
            return "jwt-digest-" + Integer.toHexString(hash);
        }
        return "opaque-authorization";
    }

    private static String snippetClientIp(String forwardedFor, org.springframework.http.server.reactive.ServerHttpRequest req) {
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            var hop = forwardedFor.split(",")[0].trim();
            return hop.length() > 24 ? hop.substring(0, 24) + "…" : hop;
        }
        var remote = req.getRemoteAddress();
        return remote != null ? remote.getAddress().getHostAddress() : "unknown";
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE - 50;
    }
}
