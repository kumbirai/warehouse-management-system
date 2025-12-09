package com.ccbsa.wms.tenant.application.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO: CreateTenantRequest
 * <p>
 * Request DTO for creating a tenant.
 */
public final class CreateTenantRequest {
    @NotBlank(message = "Tenant ID is required")
    @Size(max = 50,
            message = "Tenant ID cannot exceed 50 characters")
    private String tenantId;
    @NotBlank(message = "Tenant name is required")
    @Size(max = 200,
            message = "Tenant name cannot exceed 200 characters")
    private String name;
    @Email(message = "EmailAddress must be valid")
    @JsonProperty("emailAddress")
    private String emailAddress;
    @Size(max = 50,
            message = "Phone cannot exceed 50 characters")
    private String phone;
    @Size(max = 500,
            message = "Address cannot exceed 500 characters")
    private String address;
    private String keycloakRealmName;
    private Boolean usePerTenantRealm;

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

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

