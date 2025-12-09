package com.ccbsa.wms.tenant.application.service.port.service;

import com.ccbsa.common.keycloak.port.KeycloakRealmPort;

/**
 * Port interface for Keycloak realm operations.
 * <p>
 * This wraps the common KeycloakRealmPort for tenant-service specific use.
 * The implementation is provided by the messaging/integration layer.
 */
public interface KeycloakRealmServicePort extends KeycloakRealmPort {
    // Tenant-service specific methods can be added here if needed
}

