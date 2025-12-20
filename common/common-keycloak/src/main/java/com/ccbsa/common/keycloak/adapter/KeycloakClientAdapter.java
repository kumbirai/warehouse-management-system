package com.ccbsa.common.keycloak.adapter;

import java.util.Objects;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.ccbsa.common.keycloak.config.KeycloakConfig;
import com.ccbsa.common.keycloak.port.KeycloakClientPort;

import jakarta.annotation.PreDestroy;

/**
 * Adapter implementation for Keycloak client operations. Provides the base Keycloak Admin Client instance used by all service-specific adapters.
 * <p>
 * This adapter manages the Keycloak client lifecycle and connection pooling.
 */
@Component
public class KeycloakClientAdapter implements KeycloakClientPort {
    private static final Logger logger = LoggerFactory.getLogger(KeycloakClientAdapter.class);
    private final KeycloakConfig config;
    private Keycloak keycloak;

    public KeycloakClientAdapter(KeycloakConfig config) {
        this.config = Objects.requireNonNull(config, "KeycloakConfig must not be null");
        this.keycloak = createKeycloakClient();
        logger.info("Keycloak Admin Client initialized for server: {}", config.getServerUrl());
    }

    private Keycloak createKeycloakClient() {
        return KeycloakBuilder.builder().serverUrl(config.getServerUrl()).realm(config.getAdminRealm()).username(config.getAdminUsername()).password(config.getAdminPassword())
                .clientId(config.getAdminClientId()).build();
    }

    @Override
    public boolean isAccessible() {
        try {
            getAdminClient().realms().findAll();
            return true;
        } catch (Exception e) {
            logger.warn("Keycloak is not accessible: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public Keycloak getAdminClient() {
        if (keycloak == null || isClosed()) {
            logger.debug("Recreating Keycloak client connection");
            keycloak = createKeycloakClient();
        }
        return keycloak;
    }

    private boolean isClosed() {
        // Keycloak client doesn't expose a direct "isClosed" method
        // We check accessibility by attempting a simple operation
        try {
            keycloak.realms().findAll();
            return false;
        } catch (Exception e) {
            return true;
        }
    }

    @Override
    @PreDestroy
    public void close() {
        if (keycloak != null) {
            try {
                keycloak.close();
                logger.info("Keycloak Admin Client closed");
            } catch (Exception e) {
                logger.warn("Error closing Keycloak client: {}", e.getMessage());
            } finally {
                keycloak = null;
            }
        }
    }
}

