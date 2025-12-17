package com.ccbsa.wms.tenant.domain.core.event;

import com.ccbsa.common.domain.EventMetadata;
import com.ccbsa.common.domain.valueobject.TenantId;

/**
 * Domain Event: TenantActivatedEvent
 * <p>
 * Published when a tenant is activated.
 */
public final class TenantActivatedEvent
        extends TenantEvent<TenantId> {
    public TenantActivatedEvent(TenantId tenantId) {
        super(tenantId);
    }

    public TenantActivatedEvent(TenantId tenantId, EventMetadata metadata) {
        super(tenantId, metadata);
    }
}

