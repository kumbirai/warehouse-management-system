package com.ccbsa.wms.tenant.application.service.query.dto;

import java.time.LocalDateTime;
import java.util.Optional;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.tenant.domain.core.valueobject.TenantName;
import com.ccbsa.wms.tenant.domain.core.valueobject.TenantStatus;

/**
 * View: TenantView
 * <p>
 * Read-optimized view of a tenant for query results.
 */
public final class TenantView {
    private final TenantId tenantId;
    private final TenantName name;
    private final TenantStatus status;
    private final String email;
    private final String phone;
    private final String address;
    private final String keycloakRealmName;
    private final boolean usePerTenantRealm;
    private final LocalDateTime createdAt;
    private final LocalDateTime activatedAt;
    private final LocalDateTime deactivatedAt;

    public TenantView(TenantId tenantId, TenantName name, TenantStatus status, String email, String phone, String address, String keycloakRealmName, boolean usePerTenantRealm,
                      LocalDateTime createdAt, LocalDateTime activatedAt,
                      LocalDateTime deactivatedAt) {
        this.tenantId = tenantId;
        this.name = name;
        this.status = status;
        this.email = email;
        this.phone = phone;
        this.address = address;
        this.keycloakRealmName = keycloakRealmName;
        this.usePerTenantRealm = usePerTenantRealm;
        this.createdAt = createdAt;
        this.activatedAt = activatedAt;
        this.deactivatedAt = deactivatedAt;
    }

    public TenantId getTenantId() {
        return tenantId;
    }

    public TenantName getName() {
        return name;
    }

    public TenantStatus getStatus() {
        return status;
    }

    public Optional<String> getEmail() {
        return Optional.ofNullable(email);
    }

    public Optional<String> getPhone() {
        return Optional.ofNullable(phone);
    }

    public Optional<String> getAddress() {
        return Optional.ofNullable(address);
    }

    public Optional<String> getKeycloakRealmName() {
        return Optional.ofNullable(keycloakRealmName);
    }

    public boolean isUsePerTenantRealm() {
        return usePerTenantRealm;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public Optional<LocalDateTime> getActivatedAt() {
        return Optional.ofNullable(activatedAt);
    }

    public Optional<LocalDateTime> getDeactivatedAt() {
        return Optional.ofNullable(deactivatedAt);
    }
}

