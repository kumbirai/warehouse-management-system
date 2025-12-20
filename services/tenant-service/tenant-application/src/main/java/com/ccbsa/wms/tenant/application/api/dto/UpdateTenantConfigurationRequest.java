package com.ccbsa.wms.tenant.application.api.dto;

import jakarta.validation.constraints.Size;

/**
 * DTO: UpdateTenantConfigurationRequest
 * <p>
 * Request DTO for updating tenant configuration.
 */
public final class UpdateTenantConfigurationRequest {
    @Size(max = 100, message = "Keycloak realm name cannot exceed 100 characters")
    private String keycloakRealmName;
    private Boolean usePerTenantRealm;

    public String getKeycloakRealmName() {
        return keycloakRealmName;
    }

    public void setKeycloakRealmName(String keycloakRealmName) {
        this.keycloakRealmName = keycloakRealmName;
    }

    public Boolean getUsePerTenantRealm() {
        return usePerTenantRealm;
    }

    public void setUsePerTenantRealm(Boolean usePerTenantRealm) {
        this.usePerTenantRealm = usePerTenantRealm;
    }
}

