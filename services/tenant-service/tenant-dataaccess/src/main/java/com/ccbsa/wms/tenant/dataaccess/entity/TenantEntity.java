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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA Entity: TenantEntity
 * <p>
 * JPA representation of Tenant aggregate. Note: This service is NOT tenant-aware (it manages tenants), so we use a single schema, not schema-per-tenant.
 */
@Entity
@Table(name = "tenants", schema = "public")
@Getter
@Setter
@NoArgsConstructor
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
}

