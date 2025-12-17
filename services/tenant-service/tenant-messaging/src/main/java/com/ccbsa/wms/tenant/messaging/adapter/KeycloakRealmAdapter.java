package com.ccbsa.wms.tenant.messaging.adapter;

import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RealmsResource;
import org.keycloak.representations.idm.RealmRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.common.keycloak.port.KeycloakClientPort;
import com.ccbsa.wms.tenant.application.service.port.service.KeycloakRealmServicePort;

import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.core.Response;

/**
 * Adapter: KeycloakRealmAdapter
 * <p>
 * Implements KeycloakRealmServicePort for tenant-service. Provides Keycloak realm management operations.
 */
@Component
public class KeycloakRealmAdapter
        implements KeycloakRealmServicePort {
    private static final Logger logger = LoggerFactory.getLogger(KeycloakRealmAdapter.class);
    private final KeycloakClientPort keycloakClient;

    public KeycloakRealmAdapter(KeycloakClientPort keycloakClient) {
        this.keycloakClient = keycloakClient;
    }

    @Override
    public RealmRepresentation createRealm(TenantId tenantId, String realmName) {
        logger.info("Creating Keycloak realm: {} for tenant: {}", realmName, tenantId);

        // Check if realm already exists first
        RealmRepresentation existingRealm = getRealm(realmName);
        if (existingRealm != null) {
            logger.info("Keycloak realm already exists: {}. Enabling if disabled.", realmName);
            // Ensure realm is enabled
            if (!existingRealm.isEnabled()) {
                enableRealm(realmName);
                return getRealm(realmName);
            }
            return existingRealm;
        }

        // Try to create the realm
        RealmRepresentation realm = new RealmRepresentation();
        realm.setRealm(realmName);
        realm.setEnabled(true);
        realm.setDisplayName(String.format("Tenant: %s", tenantId.getValue()));

        RealmsResource realmsResource = keycloakClient.getAdminClient()
                .realms();

        try {
            realmsResource.create(realm);
            logger.info("Keycloak realm created: {}", realmName);
        } catch (ClientErrorException e) {
            // Handle 409 Conflict - realm already exists (race condition)
            if (e.getResponse() != null && e.getResponse()
                    .getStatus() == Response.Status.CONFLICT.getStatusCode()) {
                logger.warn("Keycloak realm already exists (409 Conflict): {}. This may be due to a race condition. Enabling realm.", realmName);
                // Realm exists but might be disabled, ensure it's enabled
                enableRealm(realmName);
            } else {
                // Re-throw other client errors
                logger.error("Failed to create Keycloak realm: {}", realmName, e);
                throw e;
            }
        }

        return getRealm(realmName);
    }

    @Override
    public RealmRepresentation getRealm(String realmName) {
        try {
            RealmResource realmResource = keycloakClient.getAdminClient()
                    .realms()
                    .realm(realmName);
            return realmResource.toRepresentation();
        } catch (ClientErrorException e) {
            // Handle 404 Not Found specifically
            if (e.getResponse() != null && e.getResponse()
                    .getStatus() == Response.Status.NOT_FOUND.getStatusCode()) {
                logger.debug("Realm not found: {}", realmName);
                return null;
            }
            // Log other client errors but still return null
            logger.debug("Error checking realm existence: {}", realmName, e);
            return null;
        } catch (Exception e) {
            logger.debug("Realm not found: {}", realmName, e);
            return null;
        }
    }

    @Override
    public void enableRealm(String realmName) {
        logger.info("Enabling Keycloak realm: {}", realmName);

        RealmRepresentation realm = getRealm(realmName);
        if (realm == null) {
            throw new IllegalArgumentException(String.format("Realm not found: %s", realmName));
        }

        realm.setEnabled(true);
        updateRealm(realmName, realm);

        logger.info("Keycloak realm enabled: {}", realmName);
    }

    @Override
    public void updateRealm(String realmName, RealmRepresentation realm) {
        logger.info("Updating Keycloak realm: {}", realmName);

        RealmResource realmResource = keycloakClient.getAdminClient()
                .realms()
                .realm(realmName);
        realmResource.update(realm);

        logger.info("Keycloak realm updated: {}", realmName);
    }

    @Override
    public void disableRealm(String realmName) {
        logger.info("Disabling Keycloak realm: {}", realmName);

        RealmRepresentation realm = getRealm(realmName);
        if (realm == null) {
            throw new IllegalArgumentException(String.format("Realm not found: %s", realmName));
        }

        realm.setEnabled(false);
        updateRealm(realmName, realm);

        logger.info("Keycloak realm disabled: {}", realmName);
    }

    @Override
    public boolean realmExists(String realmName) {
        return getRealm(realmName) != null;
    }
}

