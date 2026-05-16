package com.telemetry.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Security configuration.
 *
 * JWT validation is delegated to Spring Security OAuth2 resource server using
 * the JWKS URI from the configured OIDC provider (local Keycloak in dev).
 *
 * SSE stream and feed status endpoints are protected.
 * Actuator health check is public for Docker/K8s probes.
 *
 * For local development, start Keycloak in dev mode:
 *   docker run -p 8180:8080 -e KEYCLOAK_ADMIN=admin -e KEYCLOAK_ADMIN_PASSWORD=admin \
 *     quay.io/keycloak/keycloak:latest start-dev
 * Then import the realm configuration from docs/keycloak-realm.json
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
    private String jwkSetUri;

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        // Public endpoints
                        .pathMatchers("/actuator/health").permitAll()
                        .pathMatchers("/api/feeds/status").permitAll()
                        // Protected API and SSE stream
                        .anyExchange().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwkSetUri(jwkSetUri))
                )
                .build();
    }

    /**
     * CORS configuration for local development (Angular dev server on :4200).
     * In production, restrict origins to the actual deployment domain.
     */
    @Bean
    public CorsWebFilter corsWebFilter(
            @Value("${telemetry.cors.allowed-origins:http://localhost:4200}") String allowedOrigins) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(allowedOrigins.split(",")));
        config.setAllowedMethods(List.of("GET", "POST", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsWebFilter(source);
    }
}
