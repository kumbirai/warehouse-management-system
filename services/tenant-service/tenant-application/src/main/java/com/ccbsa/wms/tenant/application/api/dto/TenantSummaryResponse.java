package com.ccbsa.wms.tenant.application.api.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Lightweight DTO for tenant list responses.
 */
public final class TenantSummaryResponse {
    private final String tenantId;
    private final String name;
    private final String status;
    @JsonProperty("emailAddress")
    private final String emailAddress;
    private final String phone;
    private final LocalDateTime createdAt;
    private final LocalDateTime activatedAt;
    private final boolean usePerTenantRealm;

    public TenantSummaryResponse(String tenantId, String name, String status, String emailAddress, String phone, LocalDateTime createdAt, LocalDateTime activatedAt,
                                 boolean usePerTenantRealm) {
        this.tenantId = tenantId;
        this.name = name;
        this.status = status;
        this.emailAddress = emailAddress;
        this.phone = phone;
        this.createdAt = createdAt;
        this.activatedAt = activatedAt;
        this.usePerTenantRealm = usePerTenantRealm;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getActivatedAt() {
        return activatedAt;
    }

    public boolean isUsePerTenantRealm() {
        return usePerTenantRealm;
    }
}

