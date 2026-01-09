package com.ccbsa.wms.picking.application.service.port.repository;

import java.util.List;
import java.util.Optional;

import com.ccbsa.common.domain.valueobject.LoadNumber;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.picking.domain.core.entity.Load;
import com.ccbsa.wms.picking.domain.core.valueobject.LoadId;
import com.ccbsa.wms.picking.domain.core.valueobject.LoadStatus;
import com.ccbsa.wms.picking.domain.core.valueobject.PickingListId;

/**
 * Repository Port: LoadRepository
 * <p>
 * Defines the contract for Load aggregate persistence. Implemented by data access adapters.
 */
public interface LoadRepository {
    /**
     * Saves a Load aggregate.
     *
     * @param load Load aggregate to save
     */
    void save(Load load);

    /**
     * Finds a Load by ID and tenant ID.
     *
     * @param id       Load identifier
     * @param tenantId Tenant identifier
     * @return Optional Load if found
     */
    Optional<Load> findByIdAndTenantId(LoadId id, TenantId tenantId);

    /**
     * Finds a Load by load number and tenant ID.
     *
     * @param loadNumber Load number
     * @param tenantId   Tenant identifier
     * @return Optional Load if found
     */
    Optional<Load> findByLoadNumberAndTenantId(LoadNumber loadNumber, TenantId tenantId);

    /**
     * Finds all loads for a tenant with filtering and pagination.
     *
     * @param tenantId Tenant identifier
     * @param status   Optional status filter
     * @param page     Page number (0-based)
     * @param size     Page size
     * @return List of Load aggregates
     */
    List<Load> findByTenantId(TenantId tenantId, LoadStatus status, int page, int size);

    /**
     * Finds the PickingListId for a given LoadId.
     *
     * @param loadId   Load identifier
     * @param tenantId Tenant identifier
     * @return Optional PickingListId if found
     */
    Optional<PickingListId> findPickingListIdByLoadId(LoadId loadId, TenantId tenantId);
}
