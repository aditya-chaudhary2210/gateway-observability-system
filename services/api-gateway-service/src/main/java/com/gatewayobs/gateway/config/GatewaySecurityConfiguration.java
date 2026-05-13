/*
 * OAuth2 JWT can be layered later via issuer-uri. Docker/local default is permit-all for operator UX.
 */
package com.gatewayobs.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
class GatewaySecurityConfiguration {

    @Bean
    SecurityWebFilterChain gatewaySecurity(ServerHttpSecurity http) {
        return http.csrf(ServerHttpSecurity.CsrfSpec::disable).authorizeExchange(
                        auth -> auth.pathMatchers("/actuator/**").permitAll().anyExchange().permitAll())
                .build();
    }
}
