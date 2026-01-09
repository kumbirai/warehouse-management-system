package com.ccbsa.wms.stock.dataaccess.jpa;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ccbsa.wms.stock.dataaccess.entity.RestockRequestEntity;
import com.ccbsa.wms.stock.domain.core.valueobject.RestockRequestStatus;

/**
 * JPA Repository: RestockRequestJpaRepository
 * <p>
 * Spring Data JPA repository for RestockRequestEntity.
 */
public interface RestockRequestJpaRepository extends JpaRepository<RestockRequestEntity, UUID> {
    /**
     * Finds a restock request by tenant ID and restock request ID.
     *
     * @param tenantId Tenant identifier
     * @param id       Restock request identifier
     * @return Optional RestockRequestEntity if found
     */
    Optional<RestockRequestEntity> findByTenantIdAndId(String tenantId, UUID id);

    /**
     * Finds active restock request for a product and location.
     * <p>
     * Active means status is PENDING or SENT_TO_D365.
     *
     * @param tenantId   Tenant identifier
     * @param productId  Product identifier
     * @param locationId Location identifier (optional)
     * @return Optional active RestockRequestEntity
     */
    Optional<RestockRequestEntity> findByTenantIdAndProductIdAndLocationIdAndStatusIn(String tenantId, UUID productId, UUID locationId, List<RestockRequestStatus> statuses);

    /**
     * Finds all restock requests for a tenant with optional status filter.
     *
     * @param tenantId Tenant identifier
     * @param status   Optional status filter
     * @return List of RestockRequestEntity
     */
    List<RestockRequestEntity> findByTenantIdAndStatusOrderByCreatedAtDesc(String tenantId, RestockRequestStatus status);

    /**
     * Finds all restock requests for a tenant.
     *
     * @param tenantId Tenant identifier
     * @return List of RestockRequestEntity
     */
    List<RestockRequestEntity> findByTenantIdOrderByCreatedAtDesc(String tenantId);
}
