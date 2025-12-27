package com.ccbsa.wms.stock.dataaccess.jpa;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.ccbsa.wms.stock.dataaccess.entity.StockConsignmentEntity;

/**
 * JPA Repository: StockConsignmentJpaRepository
 * <p>
 * Spring Data JPA repository for StockConsignmentEntity. Provides database access methods with multi-tenant support.
 */
public interface StockConsignmentJpaRepository extends JpaRepository<StockConsignmentEntity, UUID> {
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

    /**
     * Finds all consignments for a tenant with pagination.
     *
     * @param tenantId Tenant identifier
     * @param pageable Pagination parameters
     * @return List of StockConsignmentEntity
     */
    List<StockConsignmentEntity> findByTenantIdOrderByCreatedAtDesc(String tenantId, Pageable pageable);

    /**
     * Counts all consignments for a tenant.
     *
     * @param tenantId Tenant identifier
     * @return Total count of consignments
     */
    long countByTenantId(String tenantId);
}

