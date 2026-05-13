/*
 * Fallback handler for reactive circuit breaker paths configured on gateway routes.
 */
package com.gatewayobs.gateway.web;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
class CircuitBreakerFallbackController {

    @GetMapping(path = "/fallback/mock-circuit-breaker", produces = MediaType.APPLICATION_JSON_VALUE)
    Mono<String> upstreamMockBreaker() {
        return Mono.just("{\"circuitBreaker\":\"half-open-open\",\"upstream\":\"upstream-mock\"}");
    }
}
