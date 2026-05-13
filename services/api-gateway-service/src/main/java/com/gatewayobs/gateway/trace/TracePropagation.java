package com.gatewayobs.gateway.trace;

import java.util.Optional;
import org.springframework.http.server.reactive.ServerHttpRequest;

/** Lightweight W3C traceparent parsing without coupling the JVM agent. */
public final class TracePropagation {

    private TracePropagation() {}

    public static ParsedTrace traceFrom(ServerHttpRequest request) {
        var tp = Optional.ofNullable(request.getHeaders().getFirst("traceparent")).orElse("");
        if (tp.length() >= 37 && tp.charAt(3) == '-') {
            try {
                var traceId = tp.substring(4, 36);
                var spanPart = tp.substring(37).split("-");
                var parentSpan = spanPart.length > 0 ? spanPart[0] : "";
                return new ParsedTrace(traceId, parentSpan, tp);
            } catch (Exception ignored) {
                return ParsedTrace.none();
            }
        }
        return ParsedTrace.none();
    }

    public record ParsedTrace(String traceId, String spanId, String traceparent) {
        static ParsedTrace none() {
            return new ParsedTrace("", "", "");
        }
    }
}
