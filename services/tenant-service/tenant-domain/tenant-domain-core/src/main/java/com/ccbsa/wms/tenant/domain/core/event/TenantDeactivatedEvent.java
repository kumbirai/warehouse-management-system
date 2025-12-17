package com.ccbsa.wms.tenant.domain.core.event;

import com.ccbsa.common.domain.EventMetadata;
import com.ccbsa.common.domain.valueobject.TenantId;

/**
 * Domain Event: TenantDeactivatedEvent
 * <p>
 * Published when a tenant is deactivated.
 */
public final class TenantDeactivatedEvent
        extends TenantEvent<TenantId> {
    public TenantDeactivatedEvent(TenantId tenantId) {
        super(tenantId);
    }

    public TenantDeactivatedEvent(TenantId tenantId, EventMetadata metadata) {
        super(tenantId, metadata);
    }
}

