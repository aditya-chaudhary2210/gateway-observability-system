/*
 * Ownership: api-gateway-service
 * Bounded context: Edge telemetry emission + resilience policies forwarding traffic.
 */
package com.gatewayobs.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class GatewayObservabilityApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayObservabilityApplication.class, args);
    }
}
