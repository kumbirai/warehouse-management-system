package com.ccbsa.wms.tenant.application.service.query.dto;

import com.ccbsa.common.application.query.Query;
import com.ccbsa.common.domain.valueobject.TenantId;

/**
 * Query: GetTenantQuery
 * <p>
 * Represents a query to get a tenant by ID.
 */
public final class GetTenantQuery implements Query<TenantView> {
    private final TenantId tenantId;

    public GetTenantQuery(TenantId tenantId) {
        if (tenantId == null) {
            throw new IllegalArgumentException("TenantId cannot be null");
        }
        this.tenantId = tenantId;
    }

    public TenantId getTenantId() {
        return tenantId;
    }
}

