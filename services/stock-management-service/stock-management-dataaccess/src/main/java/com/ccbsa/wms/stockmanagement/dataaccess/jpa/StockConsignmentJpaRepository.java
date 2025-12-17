package com.ccbsa.wms.stockmanagement.dataaccess.jpa;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ccbsa.wms.stockmanagement.dataaccess.entity.StockConsignmentEntity;

/**
 * JPA Repository: StockConsignmentJpaRepository
 * <p>
 * Spring Data JPA repository for StockConsignmentEntity. Provides database access methods with multi-tenant support.
 */
public interface StockConsignmentJpaRepository
        extends JpaRepository<StockConsignmentEntity, UUID> {
    /**
     * Finds a consignment by tenant ID and consignment ID.
     *
     * @param tenantId Tenant identifier
     * @param id       Consignment identifier
     * @return Optional StockConsignmentEntity if found
     */
    Optional<StockConsignmentEntity> findByTenantIdAndId(String tenantId, UUID id);

    /**
     * Finds a consignment by tenant ID and consignment reference.
     *
     * @param tenantId             Tenant identifier
     * @param consignmentReference Consignment reference
     * @return Optional StockConsignmentEntity if found
     */
    Optional<StockConsignmentEntity> findByTenantIdAndConsignmentReference(String tenantId, String consignmentReference);

    /**
     * Checks if a consignment exists with the given reference for the tenant.
     *
     * @param tenantId             Tenant identifier
     * @param consignmentReference Consignment reference
     * @return true if consignment exists
     */
    boolean existsByTenantIdAndConsignmentReference(String tenantId, String consignmentReference);
}

