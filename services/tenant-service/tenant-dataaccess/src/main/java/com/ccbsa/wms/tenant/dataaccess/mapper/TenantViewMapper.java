package com.ccbsa.wms.tenant.dataaccess.mapper;

import org.springframework.stereotype.Component;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.tenant.application.service.query.dto.TenantView;
import com.ccbsa.wms.tenant.dataaccess.entity.TenantEntity;
import com.ccbsa.wms.tenant.domain.core.valueobject.TenantName;

/**
 * Mapper converting {@link TenantEntity} instances to read-optimized {@link TenantView}s.
 */
@Component
public class TenantViewMapper {
    public TenantView toView(TenantEntity entity) {
        return new TenantView(TenantId.of(entity.getTenantId()), TenantName.of(entity.getName()), entity.getStatus(), entity.getEmailAddress(), entity.getPhone(),
                entity.getAddress(), entity.getKeycloakRealmName(),
                entity.isUsePerTenantRealm(), entity.getCreatedAt(), entity.getActivatedAt(), entity.getDeactivatedAt());
    }
}

