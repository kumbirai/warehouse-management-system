package com.ccbsa.wms.user.config;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.ccbsa.common.keycloak.config.KeycloakConfig;

/**
 * Health indicator for Keycloak connectivity. Checks if Keycloak server is reachable and responsive.
 */
@Component
public class KeycloakHealthIndicator
        implements HealthIndicator {
    private static final Logger logger = LoggerFactory.getLogger(KeycloakHealthIndicator.class);
    private final KeycloakConfig keycloakConfig;
    private final RestTemplate restTemplate;

    public KeycloakHealthIndicator(KeycloakConfig keycloakConfig, RestTemplate restTemplate) {
        this.keycloakConfig = Objects.requireNonNull(keycloakConfig, "KeycloakConfig must not be null");
        this.restTemplate = Objects.requireNonNull(restTemplate, "RestTemplate must not be null");
    }

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

            logger.debug("Keycloak health check passed");

            return Health.up()
                    .withDetails(details)
                    .build();
        } catch (Exception e) {
            logger.warn("Keycloak health check failed: {}", e.getMessage());

            Map<String, Object> details = new HashMap<>();
            details.put("serverUrl", keycloakConfig.getServerUrl());
            details.put("defaultRealm", keycloakConfig.getDefaultRealm());
            details.put("error", e.getMessage());
            details.put("status", "DOWN");

            return Health.down()
                    .withDetails(details)
                    .build();
        }
    }
}

