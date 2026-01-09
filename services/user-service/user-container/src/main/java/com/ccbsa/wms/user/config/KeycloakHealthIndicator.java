package com.ccbsa.wms.user.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.ccbsa.common.keycloak.config.KeycloakConfig;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Health indicator for Keycloak connectivity. Checks if Keycloak server is reachable and responsive.
 * <p>
 * Uses externalRestTemplate (non-load-balanced) since Keycloak is an external service
 * not registered with Eureka.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KeycloakHealthIndicator implements HealthIndicator {
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "KeycloakConfig is a Spring-managed @ConfigurationProperties bean that is not modified after initialization. "
            + "The reference is safe to store.")
    private final KeycloakConfig keycloakConfig;
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "RestTemplate is a Spring-managed bean that is thread-safe and not modified after initialization. The reference"
            + " is safe to store.")
    @Qualifier("externalRestTemplate")
    private final RestTemplate restTemplate;

    @Override
    public Health health() {
        try {
            // Check Keycloak server by attempting to reach the realm endpoint
            // This is more reliable than /health which may not be available
            String realmUrl = String.format("%s/realms/%s", keycloakConfig.getServerUrl(), keycloakConfig.getDefaultRealm());
            restTemplate.getForEntity(realmUrl, String.class);

            Map<String, Object> details = new HashMap<>();
            details.put("serverUrl", keycloakConfig.getServerUrl());
            details.put("defaultRealm", keycloakConfig.getDefaultRealm());
            details.put("status", "UP");

            log.debug("Keycloak health check passed");

            return Health.up().withDetails(details).build();
        } catch (Exception e) {
            log.warn("Keycloak health check failed: {}", e.getMessage());

            Map<String, Object> details = new HashMap<>();
            details.put("serverUrl", keycloakConfig.getServerUrl());
            details.put("defaultRealm", keycloakConfig.getDefaultRealm());
            details.put("error", e.getMessage());
            details.put("status", "DOWN");

            return Health.down().withDetails(details).build();
        }
    }
}

