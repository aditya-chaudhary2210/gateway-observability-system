package com.gatewayobs.gateway.filter;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
/** Optional chaos engineering hook guarded by explicit configuration flags. */
@ConditionalOnProperty(name = "gateway.chaos.allow-header", havingValue = "true")
class FailureSimulationGatewayFilter implements GlobalFilter, Ordered {

    static final String CHAOS_STATUS_HEADER = "X-Chaos-Status";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        var requested = exchange.getRequest().getHeaders().getFirst(CHAOS_STATUS_HEADER);
        if (requested != null && !requested.isBlank()) {
            HttpStatus resolved;
            try {
                resolved = HttpStatus.resolve(Integer.parseInt(requested.trim().split(";")[0].trim()));
            } catch (NumberFormatException ex) {
                resolved = HttpStatus.BAD_REQUEST;
            }
            HttpStatus chosen = resolved != null ? resolved : HttpStatus.BAD_REQUEST;
            exchange.getResponse().setStatusCode(chosen);
            exchange.getResponse().getHeaders().setContentType(MediaType.TEXT_PLAIN);
            DataBuffer buffer =
                    exchange.getResponse().bufferFactory().wrap(("chaos-response:" + chosen.value()).getBytes());
            return exchange.getResponse().writeWith(Mono.just(buffer));
        }
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 5;
    }
}
