package com.ccbsa.wms.tenant.application.api.dto;

/**
 * DTO: TenantRealmResponse
 * <p>
 * Response DTO for tenant realm queries. Used by user-service to determine which Keycloak realm to use.
 */
public final class TenantRealmResponse {
    private final String tenantId;
    private final String realmName;

    public TenantRealmResponse(String tenantId, String realmName) {
        this.tenantId = tenantId;
        this.realmName = realmName;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getRealmName() {
        return realmName;
    }
}

