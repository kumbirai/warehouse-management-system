package com.ccbsa.wms.user.config;

import java.util.Optional;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.ccbsa.common.keycloak.config.KeycloakConfig;
import com.ccbsa.common.keycloak.port.KeycloakClientPort;
import com.ccbsa.common.keycloak.util.KeycloakClientSecretRetriever;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Application startup validator. Validates critical configuration on application startup. Optionally attempts to retrieve client secret from Keycloak if not configured.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApplicationStartupValidator implements CommandLineRunner {
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "KeycloakConfig is a Spring-managed @ConfigurationProperties bean that is not modified after initialization. "
            + "The reference is safe to store.")
    private final KeycloakConfig keycloakConfig;
    private final KeycloakClientPort keycloakClientPort;

    @Override
    public void run(String... args) {
        log.info("Validating application configuration...");

        boolean isValid = true;

        // Validate Keycloak configuration
        if (keycloakConfig.getServerUrl() == null || keycloakConfig.getServerUrl().isEmpty()) {
            log.error("❌ Keycloak server URL is not configured");
            isValid = false;
        } else {
            log.info("✓ Keycloak server URL: {}", keycloakConfig.getServerUrl());
        }

        if (keycloakConfig.getDefaultRealm() == null || keycloakConfig.getDefaultRealm().isEmpty()) {
            log.error("❌ Keycloak default realm is not configured");
            isValid = false;
        } else {
            log.info("✓ Keycloak default realm: {}", keycloakConfig.getDefaultRealm());
        }

        // Check client secret configuration
        if (keycloakConfig.getClientSecret() == null || keycloakConfig.getClientSecret().isEmpty()) {
            log.warn("⚠ Keycloak client secret is not configured - attempting to retrieve from Keycloak...");

            try {
                KeycloakClientSecretRetriever retriever = new KeycloakClientSecretRetriever(keycloakClientPort, keycloakConfig);
                Optional<String> secret = retriever.retrieveWmsApiClientSecret();

                if (secret.isPresent()) {
                    keycloakConfig.setClientSecret(secret.get());
                    log.info("✓ Keycloak client secret retrieved from Keycloak and configured");
                } else {
                    log.warn("⚠ Could not retrieve client secret from Keycloak - BFF may not work with confidential clients");
                    log.warn("  To configure manually, run: ./scripts/get-keycloak-client-secret.sh");
                    log.warn("  Or set environment variable: KEYCLOAK_CLIENT_SECRET=<secret>");
                }
            } catch (Exception e) {
                log.warn("⚠ Failed to retrieve client secret from Keycloak: {}", e.getMessage());
                log.warn("  To configure manually, run: ./scripts/get-keycloak-client-secret.sh");
                log.warn("  Or set environment variable: KEYCLOAK_CLIENT_SECRET=<secret>");
            }
        } else {
            log.info("✓ Keycloak client secret: [CONFIGURED]");
        }

        if (isValid) {
            log.info("✅ Application configuration validation passed");
        } else {
            log.error("❌ Application configuration validation failed - please check configuration");
            throw new IllegalStateException("Application configuration validation failed");
        }
    }
}

