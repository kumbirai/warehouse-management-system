package com.ccbsa.wms.tenant.application.api.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO: TenantResponse
 * <p>
 * Response DTO for tenant queries.
 */
public final class TenantResponse {
    private final String tenantId;
    private final String name;
    private final String status;
    @JsonProperty("emailAddress")
    private final String emailAddress;
    private final String phone;
    private final String address;
    private final String keycloakRealmName;
    private final boolean usePerTenantRealm;
    private final LocalDateTime createdAt;
    private final LocalDateTime activatedAt;
    private final LocalDateTime deactivatedAt;

    public TenantResponse(String tenantId, String name, String status, String emailAddress, String phone, String address, String keycloakRealmName, boolean usePerTenantRealm,
                          LocalDateTime createdAt, LocalDateTime activatedAt, LocalDateTime deactivatedAt) {
        this.tenantId = tenantId;
        this.name = name;
        this.status = status;
        this.emailAddress = emailAddress;
        this.phone = phone;
        this.address = address;
        this.keycloakRealmName = keycloakRealmName;
        this.usePerTenantRealm = usePerTenantRealm;
        this.createdAt = createdAt;
        this.activatedAt = activatedAt;
        this.deactivatedAt = deactivatedAt;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getName() {
        return name;
    }

    public String getStatus() {
        return status;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public String getPhone() {
        return phone;
    }

    public String getAddress() {
        return address;
    }

    public String getKeycloakRealmName() {
        return keycloakRealmName;
    }

    public boolean isUsePerTenantRealm() {
        return usePerTenantRealm;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getActivatedAt() {
        return activatedAt;
    }

    public LocalDateTime getDeactivatedAt() {
        return deactivatedAt;
    }
}

