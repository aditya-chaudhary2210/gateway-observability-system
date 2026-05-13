package com.gatewayobs.gateway.filter;

import java.util.Locale;
import java.util.UUID;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
final class CorrelationIdGatewayFilter implements GlobalFilter, Ordered {

    static final String HEADER_CORRELATION = "X-Correlation-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String incoming = exchange.getRequest().getHeaders().getFirst(HEADER_CORRELATION);
        final String canonical =
                incoming == null || incoming.isBlank()
                        ? UUID.randomUUID().toString()
                        : sanitizeIncomingCorrelation(incoming);
        var httpRequest = exchange.getRequest().mutate().headers(h -> h.set(HEADER_CORRELATION, canonical)).build();
        ServerWebExchange outgoing = exchange.mutate().request(httpRequest).build();

        // Must not set response headers in chain.filter(...).then(...) — by then the response is often
        // committed and headers are read-only (ReadOnlyHttpHeaders), which breaks chunked responses.
        outgoing.getResponse()
                .beforeCommit(() -> {
                    if (outgoing.getResponse().getHeaders().getFirst(HEADER_CORRELATION) == null) {
                        outgoing.getResponse().getHeaders().set(HEADER_CORRELATION, canonical);
                    }
                    return Mono.empty();
                });

        return chain.filter(outgoing);
    }

    private static String sanitizeIncomingCorrelation(String inbound) {
        String firstLine = inbound.lines().findFirst().map(String::trim).orElse("");
        if (firstLine.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return firstLine.length() <= 128 ? firstLine.toLowerCase(Locale.ROOT) : firstLine.substring(0, 128).toLowerCase(Locale.ROOT);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
