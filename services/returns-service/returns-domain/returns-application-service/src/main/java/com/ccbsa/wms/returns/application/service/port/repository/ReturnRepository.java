package com.ccbsa.wms.returns.application.service.port.repository;

import java.util.List;
import java.util.Optional;

import com.ccbsa.common.domain.valueobject.OrderNumber;
import com.ccbsa.common.domain.valueobject.ReturnStatus;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.returns.domain.core.entity.Return;
import com.ccbsa.common.domain.valueobject.ReturnId;

/**
 * Repository Port: ReturnRepository
 * <p>
 * Defines the contract for Return aggregate persistence. Implemented by data access adapters.
 * <p>
 * This port is defined in the application service layer to maintain proper dependency direction in hexagonal architecture.
 */
public interface ReturnRepository {
    /**
     * Saves a Return aggregate.
     * <p>
     * Creates a new return if it doesn't exist, or updates an existing one.
     *
     * @param returnAggregate Return aggregate to save
     * @return Saved return aggregate
     */
    Return save(Return returnAggregate);

    /**
     * Finds a Return by ID and tenant ID.
     *
     * @param id       Return identifier
     * @param tenantId Tenant identifier
     * @return Optional Return if found
     */
    Optional<Return> findByIdAndTenantId(ReturnId id, TenantId tenantId);

    /**
     * Finds returns by status and tenant ID.
     *
     * @param status   Return status
     * @param tenantId Tenant identifier
     * @return List of returns
     */
    List<Return> findByStatusAndTenantId(ReturnStatus status, TenantId tenantId);

    /**
     * Finds returns by order number and tenant ID.
     *
     * @param orderNumber Order number
     * @param tenantId    Tenant identifier
     * @return List of returns
     */
    List<Return> findByOrderNumberAndTenantId(OrderNumber orderNumber, TenantId tenantId);
}
