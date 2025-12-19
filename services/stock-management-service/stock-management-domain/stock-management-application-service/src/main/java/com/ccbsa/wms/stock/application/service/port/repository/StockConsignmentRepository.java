package com.ccbsa.wms.stock.application.service.port.repository;

import java.util.Optional;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.stock.domain.core.entity.StockConsignment;
import com.ccbsa.wms.stock.domain.core.valueobject.ConsignmentId;
import com.ccbsa.wms.stock.domain.core.valueobject.ConsignmentReference;

/**
 * Repository Port: StockConsignmentRepository
 * <p>
 * Defines the contract for StockConsignment aggregate persistence. Implemented by data access adapters.
 * <p>
 * This port is defined in the application service layer (not domain core) to maintain proper dependency direction in hexagonal architecture.
 */
public interface StockConsignmentRepository {
    /**
     * Saves a StockConsignment aggregate.
     * <p>
     * Creates a new consignment if it doesn't exist, or updates an existing one.
     *
     * @param consignment StockConsignment aggregate to save
     */
    void save(StockConsignment consignment);

    /**
     * Finds a StockConsignment by ID and tenant ID.
     *
     * @param id       Consignment identifier
     * @param tenantId Tenant identifier
     * @return Optional StockConsignment if found
     */
    Optional<StockConsignment> findByIdAndTenantId(ConsignmentId id, TenantId tenantId);

    /**
     * Finds a StockConsignment by consignment reference and tenant ID.
     *
     * @param reference Consignment reference
     * @param tenantId  Tenant identifier
     * @return Optional StockConsignment if found
     */
    Optional<StockConsignment> findByConsignmentReferenceAndTenantId(ConsignmentReference reference, TenantId tenantId);

    /**
     * Checks if a consignment with the given reference exists for the tenant.
     *
     * @param reference Consignment reference
     * @param tenantId  Tenant identifier
     * @return true if consignment exists with the reference
     */
    boolean existsByConsignmentReferenceAndTenantId(ConsignmentReference reference, TenantId tenantId);
}

