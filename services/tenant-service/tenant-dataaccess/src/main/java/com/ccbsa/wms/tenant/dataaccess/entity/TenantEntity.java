package com.ccbsa.wms.tenant.dataaccess.entity;

import java.time.LocalDateTime;

import com.ccbsa.wms.tenant.dataaccess.converter.TenantStatusConverter;
import com.ccbsa.wms.tenant.domain.core.valueobject.TenantStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * JPA Entity: TenantEntity
 * <p>
 * JPA representation of Tenant aggregate. Note: This service is NOT tenant-aware (it manages tenants), so we use a single schema, not schema-per-tenant.
 */
@Entity
@Table(name = "tenants", schema = "public")
public class TenantEntity {
    @Id
    @Column(name = "tenant_id", length = 50, nullable = false)
    private String tenantId;
    @Column(name = "name", length = 200, nullable = false)
    private String name;
    @Convert(converter = TenantStatusConverter.class)
    @Column(name = "status", length = 20, nullable = false)
    private TenantStatus status;
    @Column(name = "email_address", length = 255)
    private String emailAddress;
    @Column(name = "phone", length = 50)
    private String phone;
    @Column(name = "address", length = 500)
    private String address;
    @Column(name = "keycloak_realm_name", length = 100)
    private String keycloakRealmName;
    @Column(name = "use_per_tenant_realm", nullable = false)
    private boolean usePerTenantRealm;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @Column(name = "activated_at")
    private LocalDateTime activatedAt;
    @Column(name = "deactivated_at")
    private LocalDateTime deactivatedAt;
    @Version
    @Column(name = "version", nullable = false)
    private int version;

    // JPA requires no-arg constructor
    public TenantEntity() {
    }

    // Getters and setters
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

    public TenantStatus getStatus() {
        return status;
    }

    public void setStatus(TenantStatus status) {
        this.status = status;
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

    public boolean isUsePerTenantRealm() {
        return usePerTenantRealm;
    }

    public void setUsePerTenantRealm(boolean usePerTenantRealm) {
        this.usePerTenantRealm = usePerTenantRealm;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getActivatedAt() {
        return activatedAt;
    }

    public void setActivatedAt(LocalDateTime activatedAt) {
        this.activatedAt = activatedAt;
    }

    public LocalDateTime getDeactivatedAt() {
        return deactivatedAt;
    }

    public void setDeactivatedAt(LocalDateTime deactivatedAt) {
        this.deactivatedAt = deactivatedAt;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }
}

