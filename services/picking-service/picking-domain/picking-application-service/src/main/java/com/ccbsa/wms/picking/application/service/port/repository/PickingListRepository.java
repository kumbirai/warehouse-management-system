package com.ccbsa.wms.picking.application.service.port.repository;

import java.util.List;
import java.util.Optional;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.picking.domain.core.entity.PickingList;
import com.ccbsa.wms.picking.domain.core.valueobject.PickingListId;
import com.ccbsa.wms.picking.domain.core.valueobject.PickingListStatus;

/**
 * Repository Port: PickingListRepository
 * <p>
 * Defines the contract for PickingList aggregate persistence. Implemented by data access adapters.
 * <p>
 * This port is defined in the application service layer (not domain core) to maintain proper dependency direction in hexagonal architecture.
 */
public interface PickingListRepository {
    /**
     * Saves a PickingList aggregate.
     * <p>
     * Creates a new picking list if it doesn't exist, or updates an existing one.
     *
     * @param pickingList PickingList aggregate to save
     */
    void save(PickingList pickingList);

    /**
     * Finds a PickingList by ID and tenant ID.
     *
     * @param id       Picking list identifier
     * @param tenantId Tenant identifier
     * @return Optional PickingList if found
     */
    Optional<PickingList> findByIdAndTenantId(PickingListId id, TenantId tenantId);

    /**
     * Finds all picking lists for a tenant with filtering and pagination.
     *
     * @param tenantId Tenant identifier
     * @param status   Optional status filter
     * @param page     Page number (0-based)
     * @param size     Page size
     * @return List of PickingList aggregates
     */
    List<PickingList> findByTenantId(TenantId tenantId, PickingListStatus status, int page, int size);

    /**
     * Counts all picking lists for a tenant with optional status filter.
     *
     * @param tenantId Tenant identifier
     * @param status   Optional status filter
     * @return Total count of picking lists
     */
    long countByTenantId(TenantId tenantId, PickingListStatus status);
}
