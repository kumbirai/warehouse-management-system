package com.ccbsa.wms.stock.application.service.port.repository;

import java.util.List;
import java.util.Optional;

import com.ccbsa.common.domain.valueobject.ProductId;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;
import com.ccbsa.wms.stock.domain.core.entity.RestockRequest;
import com.ccbsa.wms.stock.domain.core.valueobject.RestockRequestId;
import com.ccbsa.wms.stock.domain.core.valueobject.RestockRequestStatus;

/**
 * Repository Port: RestockRequestRepository
 * <p>
 * Defines the contract for RestockRequest aggregate persistence.
 */
public interface RestockRequestRepository {

    /**
     * Saves a restock request aggregate.
     *
     * @param restockRequest Restock request to save
     */
    void save(RestockRequest restockRequest);

    /**
     * Finds a restock request by ID and tenant.
     *
     * @param restockRequestId Restock request ID
     * @param tenantId         Tenant ID
     * @return Optional restock request
     */
    Optional<RestockRequest> findById(RestockRequestId restockRequestId, TenantId tenantId);

    /**
     * Finds active restock request for a product and location.
     * <p>
     * Active means status is PENDING or SENT_TO_D365.
     *
     * @param tenantId   Tenant ID
     * @param productId  Product ID
     * @param locationId Location ID (optional)
     * @return Optional active restock request
     */
    Optional<RestockRequest> findActiveByProductIdAndLocationId(TenantId tenantId, ProductId productId, LocationId locationId);

    /**
     * Finds all restock requests for a tenant with optional status filter.
     *
     * @param tenantId Tenant ID
     * @param status   Optional status filter
     * @return List of restock requests
     */
    List<RestockRequest> findByTenantId(TenantId tenantId, RestockRequestStatus status);
}
