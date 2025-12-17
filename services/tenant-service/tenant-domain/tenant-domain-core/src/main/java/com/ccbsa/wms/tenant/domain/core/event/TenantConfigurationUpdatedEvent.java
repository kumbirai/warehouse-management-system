package com.ccbsa.wms.tenant.domain.core.event;

import com.ccbsa.common.domain.EventMetadata;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.tenant.domain.core.valueobject.TenantConfiguration;

/**
 * Domain Event: TenantConfigurationUpdatedEvent
 * <p>
 * Published when tenant configuration is updated.
 */
public final class TenantConfigurationUpdatedEvent
        extends TenantEvent<TenantId> {
    private final TenantConfiguration configuration;

    public TenantConfigurationUpdatedEvent(TenantId tenantId, TenantConfiguration configuration) {
        super(tenantId);
        if (configuration == null) {
            throw new IllegalArgumentException("Tenant configuration cannot be null");
        }
        this.configuration = configuration;
    }

    public TenantConfigurationUpdatedEvent(TenantId tenantId, TenantConfiguration configuration, EventMetadata metadata) {
        super(tenantId, metadata);
        if (configuration == null) {
            throw new IllegalArgumentException("Tenant configuration cannot be null");
        }
        this.configuration = configuration;
    }

    public TenantConfiguration getConfiguration() {
        return configuration;
    }
}

