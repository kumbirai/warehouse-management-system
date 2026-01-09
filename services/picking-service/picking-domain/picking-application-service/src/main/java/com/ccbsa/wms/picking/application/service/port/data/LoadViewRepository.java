package com.ccbsa.wms.picking.application.service.port.data;

import java.util.List;
import java.util.Optional;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.picking.application.service.query.dto.LoadView;
import com.ccbsa.wms.picking.domain.core.valueobject.LoadId;

/**
 * Data Port: LoadViewRepository
 * <p>
 * Defines the contract for Load read model queries. Implemented by data access adapters.
 * <p>
 * This port is used by query handlers to access denormalized read models (projections).
 */
public interface LoadViewRepository {
    /**
     * Finds a Load view by ID and tenant ID.
     *
     * @param id       Load identifier
     * @param tenantId Tenant identifier
     * @return Optional LoadView if found
     */
    Optional<LoadView> findByIdAndTenantId(LoadId id, TenantId tenantId);

    /**
     * Finds all load views for a tenant with pagination.
     *
     * @param tenantId Tenant identifier
     * @param page     Page number (0-based)
     * @param size     Page size
     * @return List of LoadView
     */
    List<LoadView> findByTenantId(TenantId tenantId, int page, int size);
}
