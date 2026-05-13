/*
 * Redis-backed RequestRateLimiter key resolver — stable per-route key for demo tenancy.
 */
package com.gatewayobs.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

@Configuration
class RedisAndRateLimiterConfiguration {

    /** Uses first X-Forwarded-For entry when present otherwise remote address token. */
    @Bean(name = "ipKeyResolver")
    KeyResolver ipKeyResolver() {
        return exchange -> {
            var forwarded = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                var hop = forwarded.split(",")[0].trim();
                return Mono.just("rl:" + hop);
            }
            var addr =
                    exchange.getRequest().getRemoteAddress() != null
                            ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                            : "unknown";
            return Mono.just("rl:" + addr);
        };
    }
}
