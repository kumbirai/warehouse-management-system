package com.ccbsa.common.keycloak.util;

import java.util.List;
import java.util.Optional;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;

import com.ccbsa.common.keycloak.config.KeycloakConfig;
import com.ccbsa.common.keycloak.port.KeycloakClientPort;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Utility class for retrieving Keycloak client secrets.
 * <p>
 * This utility can be used to programmatically retrieve client secrets from Keycloak when they are not configured in the application properties.
 */
@Slf4j
@RequiredArgsConstructor
@SuppressFBWarnings(value = {"CT_CONSTRUCTOR_THROW", "UUF_UNUSED_FIELD"}, justification =
        "CT_CONSTRUCTOR_THROW: False positive - Lombok's @RequiredArgsConstructor doesn't throw exceptions. "
                + "UUF_UNUSED_FIELD: False positive - keycloakClientPort is used in retrieveClientSecret method.")
public class KeycloakClientSecretRetriever {
    private final KeycloakClientPort keycloakClientPort;
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "KeycloakConfig is a Spring-managed @ConfigurationProperties bean that is not modified after initialization. "
            + "The reference is safe to store.")
    private final KeycloakConfig keycloakConfig;

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
            log.warn("Client ID is null or empty, cannot retrieve secret");
            return Optional.empty();
        }

        String targetRealm = realm != null ? realm : keycloakConfig.getDefaultRealm();

        try {
            Keycloak keycloak = keycloakClientPort.getAdminClient();
            ClientsResource clientsResource = keycloak.realm(targetRealm).clients();

            // Find the client by client ID
            List<ClientRepresentation> clients = clientsResource.findByClientId(clientId);

            if (clients.isEmpty()) {
                log.warn("Client '{}' not found in realm '{}'", clientId, targetRealm);
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
                    log.info("Successfully retrieved client secret for client '{}' in realm '{}'", clientId, targetRealm);
                    return Optional.of(secret.getValue());
                } else {
                    log.warn("Client '{}' is confidential but secret is not set in realm '{}'", clientId, targetRealm);
                    return Optional.empty();
                }
            } else {
                log.info("Client '{}' is public or bearer-only, no secret required in realm '{}'", clientId, targetRealm);
                return Optional.empty();
            }
        } catch (Exception e) {
            log.error("Failed to retrieve client secret for client '{}' in realm '{}': {}", clientId, targetRealm, e.getMessage(), e);
            return Optional.empty();
        }
    }
}

