package com.ccbsa.common.keycloak.util;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ccbsa.common.keycloak.config.KeycloakConfig;
import com.ccbsa.common.keycloak.port.KeycloakClientPort;

/**
 * Utility class for retrieving Keycloak client secrets.
 * <p>
 * This utility can be used to programmatically retrieve client secrets from Keycloak when they are not configured in the application properties.
 */
public class KeycloakClientSecretRetriever {
    private static final Logger logger = LoggerFactory.getLogger(KeycloakClientSecretRetriever.class);
    private final KeycloakClientPort keycloakClientPort;
    private final KeycloakConfig keycloakConfig;

    public KeycloakClientSecretRetriever(KeycloakClientPort keycloakClientPort, KeycloakConfig keycloakConfig) {
        this.keycloakClientPort = Objects.requireNonNull(keycloakClientPort, "KeycloakClientPort must not be null");
        this.keycloakConfig = Objects.requireNonNull(keycloakConfig, "KeycloakConfig must not be null");
    }

    /**
     * Retrieves the client secret for the wms-api client using the default realm.
     *
     * @return Optional containing the client secret, or empty if not found or client is public
     */
    public Optional<String> retrieveWmsApiClientSecret() {
        return retrieveClientSecret("wms-api", null);
    }

    /**
     * Retrieves the client secret for the specified client ID from Keycloak.
     *
     * @param clientId The client ID to retrieve the secret for
     * @param realm    The realm name (defaults to defaultRealm from config if null)
     * @return Optional containing the client secret, or empty if not found or client is public
     */
    public Optional<String> retrieveClientSecret(String clientId, String realm) {
        if (clientId == null || clientId.isEmpty()) {
            logger.warn("Client ID is null or empty, cannot retrieve secret");
            return Optional.empty();
        }

        String targetRealm = realm != null ? realm : keycloakConfig.getDefaultRealm();

        try {
            Keycloak keycloak = keycloakClientPort.getAdminClient();
            ClientsResource clientsResource = keycloak.realm(targetRealm).clients();

            // Find the client by client ID
            List<ClientRepresentation> clients = clientsResource.findByClientId(clientId);

            if (clients.isEmpty()) {
                logger.warn("Client '{}' not found in realm '{}'", clientId, targetRealm);
                return Optional.empty();
            }

            ClientRepresentation client = clients.get(0);
            String clientUuid = client.getId();

            // Get client resource
            ClientResource clientResource = clientsResource.get(clientUuid);

            // Check if client is confidential (has secret)
            if (!Boolean.TRUE.equals(client.isPublicClient()) && !Boolean.TRUE.equals(client.isBearerOnly())) {
                // Get client secret
                CredentialRepresentation secret = clientResource.getSecret();

                if (secret != null && secret.getValue() != null && !secret.getValue().isEmpty()) {
                    logger.info("Successfully retrieved client secret for client '{}' in realm '{}'", clientId, targetRealm);
                    return Optional.of(secret.getValue());
                } else {
                    logger.warn("Client '{}' is confidential but secret is not set in realm '{}'", clientId, targetRealm);
                    return Optional.empty();
                }
            } else {
                logger.info("Client '{}' is public or bearer-only, no secret required in realm '{}'", clientId, targetRealm);
                return Optional.empty();
            }
        } catch (Exception e) {
            logger.error("Failed to retrieve client secret for client '{}' in realm '{}': {}", clientId, targetRealm, e.getMessage(), e);
            return Optional.empty();
        }
    }
}

