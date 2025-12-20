package com.ccbsa.wms.tenant.domain.core.event;

import com.ccbsa.common.domain.EventMetadata;
import com.ccbsa.common.domain.valueobject.TenantId;

/**
 * Domain Event: TenantSuspendedEvent
 * <p>
 * Published when a tenant is suspended.
 */
public final class TenantSuspendedEvent extends TenantEvent<TenantId> {
    public TenantSuspendedEvent(TenantId tenantId) {
        super(tenantId);
    }

    public TenantSuspendedEvent(TenantId tenantId, EventMetadata metadata) {
        super(tenantId, metadata);
    }
}

