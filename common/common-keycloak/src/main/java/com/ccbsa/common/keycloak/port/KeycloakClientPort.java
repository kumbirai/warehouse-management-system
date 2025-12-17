package com.ccbsa.common.keycloak.port;

import org.keycloak.admin.client.Keycloak;

/**
 * Port interface for Keycloak client operations. Provides base Keycloak client access for administrative operations.
 * <p>
 * This port is implemented by the KeycloakClientAdapter in the common-keycloak module and used by service-specific adapters (KeycloakUserPort, KeycloakRealmPort, etc.).
 */
public interface KeycloakClientPort {
    /**
     * Gets the Keycloak admin client instance. The client is configured with admin credentials and can be used to perform administrative operations on Keycloak.
     *
     * @return Keycloak admin client instance
     */
    Keycloak getAdminClient();

    /**
     * Checks if Keycloak is accessible.
     *
     * @return true if Keycloak is accessible, false otherwise
     */
    boolean isAccessible();

    /**
     * Closes the Keycloak client connection. Should be called when the client is no longer needed to free resources.
     */
    void close();
}

