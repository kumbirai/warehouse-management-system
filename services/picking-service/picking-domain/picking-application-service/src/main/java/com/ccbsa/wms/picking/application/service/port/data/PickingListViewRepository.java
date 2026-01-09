package com.ccbsa.wms.picking.application.service.port.data;

import java.util.List;
import java.util.Optional;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.picking.application.service.query.dto.PickingListView;
import com.ccbsa.wms.picking.domain.core.valueobject.PickingListId;
import com.ccbsa.wms.picking.domain.core.valueobject.PickingListStatus;

/**
 * Data Port: PickingListViewRepository
 * <p>
 * Defines the contract for PickingList read model queries. Implemented by data access adapters.
 * <p>
 * This port is used by query handlers to access denormalized read models (projections).
 */
public interface PickingListViewRepository {
    /**
     * Finds a PickingList view by ID and tenant ID.
     *
     * @param id       Picking list identifier
     * @param tenantId Tenant identifier
     * @return Optional PickingListView if found
     */
    Optional<PickingListView> findByIdAndTenantId(PickingListId id, TenantId tenantId);

    /**
     * Finds all picking list views for a tenant with filtering and pagination.
     *
     * @param tenantId Tenant identifier
     * @param status   Optional status filter
     * @param page     Page number (0-based)
     * @param size     Page size
     * @return List of PickingListView
     */
    List<PickingListView> findByTenantId(TenantId tenantId, PickingListStatus status, int page, int size);

    /**
     * Counts all picking list views for a tenant with optional status filter.
     *
     * @param tenantId Tenant identifier
     * @param status   Optional status filter
     * @return Total count of picking lists
     */
    long countByTenantId(TenantId tenantId, PickingListStatus status);
}
