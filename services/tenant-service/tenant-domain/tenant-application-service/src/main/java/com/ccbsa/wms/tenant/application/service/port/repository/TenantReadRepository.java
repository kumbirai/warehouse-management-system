package com.ccbsa.wms.tenant.application.service.port.repository;

import com.ccbsa.wms.tenant.application.service.query.dto.TenantListQuery;
import com.ccbsa.wms.tenant.application.service.query.dto.TenantListResult;

/**
 * Read-only repository port dedicated to query-side operations for tenants.
 * <p>
 * Keeps CQRS separation by exposing read models without leaking JPA concerns.
 */
public interface TenantReadRepository {
    /**
     * Lists tenants using pagination, filtering, and search criteria.
     *
     * @param query Query parameters (page, size, filters)
     * @return Paginated tenant list result
     */
    TenantListResult listTenants(TenantListQuery query);
}

