package com.ccbsa.wms.tenant.dataaccess.mapper;

import org.springframework.stereotype.Component;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.tenant.dataaccess.entity.TenantEntity;
import com.ccbsa.wms.tenant.domain.core.entity.Tenant;
import com.ccbsa.wms.tenant.domain.core.valueobject.ContactInformation;
import com.ccbsa.wms.tenant.domain.core.valueobject.TenantConfiguration;
import com.ccbsa.wms.tenant.domain.core.valueobject.TenantName;

/**
 * Mapper: TenantEntityMapper
 * <p>
 * Maps between Tenant domain aggregate and TenantEntity JPA entity.
 */
@Component
public class TenantEntityMapper {
    /**
     * Converts Tenant domain entity to TenantEntity JPA entity.
     * <p>
     * For new entities (version == 0), version is not set to let Hibernate manage it. For existing entities (version > 0), version is set to enable optimistic locking.
     *
     * @param tenant Tenant domain entity
     * @return TenantEntity JPA entity
     * @throws IllegalArgumentException if tenant is null
     */
    public TenantEntity toEntity(Tenant tenant) {
        if (tenant == null) {
            throw new IllegalArgumentException("Tenant cannot be null");
        }

        TenantEntity entity = new TenantEntity();
        entity.setTenantId(tenant.getId().getValue());
        entity.setName(tenant.getName().getValue());
        entity.setStatus(tenant.getStatus());

        // For new entities, version will be set by Hibernate when persisting
        // For existing entities loaded from DB, version is already set
        // We only set version when mapping from domain if it's > 0 (existing entity)
        int domainVersion = tenant.getVersion();
        if (domainVersion > 0) {
            entity.setVersion(domainVersion);
        }
        // For new entities (version == 0), don't set version - let Hibernate manage it

        // Contact information - always set fields explicitly (even if null) to ensure proper persistence
        if (tenant.getContactInformation() != null) {
            entity.setEmailAddress(tenant.getContactInformation().getEmailValue().orElse(null));
            entity.setPhone(tenant.getContactInformation().getPhone().orElse(null));
            entity.setAddress(tenant.getContactInformation().getAddress().orElse(null));
        } else {
            // Explicitly set to null to ensure proper persistence
            entity.setEmailAddress(null);
            entity.setPhone(null);
            entity.setAddress(null);
        }

        // Configuration
        if (tenant.getConfiguration() != null) {
            entity.setKeycloakRealmName(tenant.getConfiguration().getKeycloakRealmName().orElse(null));
            entity.setUsePerTenantRealm(tenant.getConfiguration().isUsePerTenantRealm());
        }

        // Timestamps
        entity.setCreatedAt(tenant.getCreatedAt());
        entity.setActivatedAt(tenant.getActivatedAt());
        entity.setDeactivatedAt(tenant.getDeactivatedAt());

        return entity;
    }

    public Tenant toDomain(TenantEntity entity) {
        TenantId tenantId = TenantId.of(entity.getTenantId());
        TenantName name = TenantName.of(entity.getName());

        ContactInformation contactInfo = null;
        if (entity.getEmailAddress() != null || entity.getPhone() != null || entity.getAddress() != null) {
            contactInfo = ContactInformation.of(entity.getEmailAddress(), entity.getPhone(), entity.getAddress());
        }

        TenantConfiguration configuration = TenantConfiguration.builder().keycloakRealmName(entity.getKeycloakRealmName()).usePerTenantRealm(entity.isUsePerTenantRealm()).build();

        // Use buildWithoutEvents() to avoid publishing creation event when loading from database
        // Set version and timestamps using builder methods
        Tenant tenant = Tenant.builder().tenantId(tenantId).name(name).contactInformation(contactInfo).configuration(configuration).status(entity.getStatus())
                .createdAt(entity.getCreatedAt()).activatedAt(entity.getActivatedAt()).deactivatedAt(entity.getDeactivatedAt()).version(entity.getVersion()).buildWithoutEvents();

        return tenant;
    }
}

