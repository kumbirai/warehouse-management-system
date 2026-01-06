package com.ccbsa.common.keycloak.adapter;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.stereotype.Component;

import com.ccbsa.common.keycloak.config.KeycloakConfig;
import com.ccbsa.common.keycloak.port.KeycloakClientPort;

import jakarta.annotation.PreDestroy;
import jakarta.ws.rs.client.ClientBuilder;
import lombok.extern.slf4j.Slf4j;

/**
 * Adapter implementation for Keycloak client operations. Provides the base Keycloak Admin Client instance used by all service-specific adapters.
 * <p>
 * This adapter manages the Keycloak client lifecycle and connection pooling with production-grade timeout configuration.
 */
@Slf4j
@Component
public class KeycloakClientAdapter implements KeycloakClientPort {
    private final KeycloakConfig config;
    private Keycloak keycloak;

    public KeycloakClientAdapter(KeycloakConfig config) {
        this.config = Objects.requireNonNull(config, "KeycloakConfig must not be null");
        this.keycloak = createKeycloakClient();
        log.info("Keycloak Admin Client initialized for server: {} with connectionTimeout={}ms, socketTimeout={}ms", config.getServerUrl(), config.getConnectionTimeout(),
                config.getSocketTimeout());
    }

    private Keycloak createKeycloakClient() {
        // Configure Resteasy client with timeouts
        ResteasyClientBuilder resteasyClientBuilder = (ResteasyClientBuilder) ClientBuilder.newBuilder();
        resteasyClientBuilder.connectionPoolSize(10);
        resteasyClientBuilder.connectionCheckoutTimeout(config.getConnectionTimeout(), TimeUnit.MILLISECONDS);
        resteasyClientBuilder.readTimeout(config.getSocketTimeout(), TimeUnit.MILLISECONDS);
        resteasyClientBuilder.connectTimeout(config.getConnectionTimeout(), TimeUnit.MILLISECONDS);

        return KeycloakBuilder.builder().serverUrl(config.getServerUrl()).realm(config.getAdminRealm()).username(config.getAdminUsername()).password(config.getAdminPassword())
                .clientId(config.getAdminClientId()).resteasyClient(resteasyClientBuilder.build()).build();
    }

    @Override
    public boolean isAccessible() {
        try {
            getAdminClient().realms().findAll();
            return true;
        } catch (Exception e) {
            log.warn("Keycloak is not accessible: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public Keycloak getAdminClient() {
        // Always return the client - don't check if closed as it requires a blocking call
        // The client will handle connection errors when used, and timeouts are configured
        if (keycloak == null) {
            log.debug("Creating new Keycloak client connection");
            keycloak = createKeycloakClient();
        }
        return keycloak;
    }

    @Override
    @PreDestroy
    public void close() {
        if (keycloak != null) {
            try {
                keycloak.close();
                log.info("Keycloak Admin Client closed");
            } catch (Exception e) {
                log.warn("Error closing Keycloak client: {}", e.getMessage());
            } finally {
                keycloak = null;
            }
        }
    }
}

