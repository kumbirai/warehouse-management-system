package com.ccbsa.wms.user.config;

import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.ccbsa.common.keycloak.config.KeycloakConfig;
import com.ccbsa.common.keycloak.port.KeycloakClientPort;
import com.ccbsa.common.keycloak.util.KeycloakClientSecretRetriever;

/**
 * Application startup validator.
 * Validates critical configuration on application startup.
 * Optionally attempts to retrieve client secret from Keycloak if not configured.
 */
@Component
public class ApplicationStartupValidator implements CommandLineRunner {
    private static final Logger logger = LoggerFactory.getLogger(ApplicationStartupValidator.class);
    private final KeycloakConfig keycloakConfig;
    private final KeycloakClientPort keycloakClientPort;

    public ApplicationStartupValidator(KeycloakConfig keycloakConfig,
                                       KeycloakClientPort keycloakClientPort) {
        this.keycloakConfig = Objects.requireNonNull(keycloakConfig, "KeycloakConfig must not be null");
        this.keycloakClientPort = Objects.requireNonNull(keycloakClientPort, "KeycloakClientPort must not be null");
    }

    @Override
    public void run(String... args) {
        logger.info("Validating application configuration...");

        boolean isValid = true;

        // Validate Keycloak configuration
        if (keycloakConfig.getServerUrl() == null || keycloakConfig.getServerUrl()
                .isEmpty()) {
            logger.error("❌ Keycloak server URL is not configured");
            isValid = false;
        } else {
            logger.info("✓ Keycloak server URL: {}",
                    keycloakConfig.getServerUrl());
        }

        if (keycloakConfig.getDefaultRealm() == null || keycloakConfig.getDefaultRealm()
                .isEmpty()) {
            logger.error("❌ Keycloak default realm is not configured");
            isValid = false;
        } else {
            logger.info("✓ Keycloak default realm: {}",
                    keycloakConfig.getDefaultRealm());
        }

        // Check client secret configuration
        if (keycloakConfig.getClientSecret() == null || keycloakConfig.getClientSecret()
                .isEmpty()) {
            logger.warn("⚠ Keycloak client secret is not configured - attempting to retrieve from Keycloak...");

            try {
                KeycloakClientSecretRetriever retriever = new KeycloakClientSecretRetriever(keycloakClientPort,
                        keycloakConfig);
                Optional<String> secret = retriever.retrieveWmsApiClientSecret();

                if (secret.isPresent()) {
                    keycloakConfig.setClientSecret(secret.get());
                    logger.info("✓ Keycloak client secret retrieved from Keycloak and configured");
                } else {
                    logger.warn("⚠ Could not retrieve client secret from Keycloak - BFF may not work with confidential clients");
                    logger.warn("  To configure manually, run: ./scripts/get-keycloak-client-secret.sh");
                    logger.warn("  Or set environment variable: KEYCLOAK_CLIENT_SECRET=<secret>");
                }
            } catch (Exception e) {
                logger.warn("⚠ Failed to retrieve client secret from Keycloak: {}",
                        e.getMessage());
                logger.warn("  To configure manually, run: ./scripts/get-keycloak-client-secret.sh");
                logger.warn("  Or set environment variable: KEYCLOAK_CLIENT_SECRET=<secret>");
            }
        } else {
            logger.info("✓ Keycloak client secret: [CONFIGURED]");
        }

        if (isValid) {
            logger.info("✅ Application configuration validation passed");
        } else {
            logger.error("❌ Application configuration validation failed - please check configuration");
            throw new IllegalStateException("Application configuration validation failed");
        }
    }
}

