package com.ccbsa.wms.tenant.application.service.port.data;

import java.util.Optional;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.tenant.application.service.query.dto.TenantListQuery;
import com.ccbsa.wms.tenant.application.service.query.dto.TenantListResult;
import com.ccbsa.wms.tenant.application.service.query.dto.TenantView;

/**
 * Data Port: TenantViewRepository
 * <p>
 * Read model repository for tenant queries. Provides optimized read access to tenant data.
 * <p>
 * This is a data port (read model) used by query handlers, not a repository port (write model).
 * <p>
 * Note: Tenant Service is NOT tenant-aware (it manages tenants), so all queries operate on the public schema.
 */
public interface TenantViewRepository {

    /**
     * Finds a tenant view by tenant ID.
     *
     * @param tenantId Tenant ID
     * @return Optional TenantView
     */
    Optional<TenantView> findById(TenantId tenantId);

    /**
     * Lists tenants using pagination, filtering, and search criteria.
     *
     * @param query Query parameters (page, size, filters)
     * @return Paginated tenant list result
     */
    TenantListResult listTenants(TenantListQuery query);
}

