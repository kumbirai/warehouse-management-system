package com.ccbsa.wms.returns.dataaccess.jpa;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ccbsa.common.domain.valueobject.ReturnStatus;
import com.ccbsa.wms.returns.dataaccess.entity.ReturnEntity;

/**
 * JPA Repository: ReturnJpaRepository
 * <p>
 * Spring Data JPA repository for ReturnEntity. Provides database access methods with multi-tenant support.
 */
public interface ReturnJpaRepository extends JpaRepository<ReturnEntity, UUID> {
    /**
     * Finds a return by tenant ID and return ID.
     *
     * @param tenantId Tenant identifier
     * @param returnId Return identifier
     * @return Optional ReturnEntity if found
     */
    Optional<ReturnEntity> findByTenantIdAndReturnId(String tenantId, UUID returnId);

    /**
     * Finds returns by tenant ID and status.
     *
     * @param tenantId Tenant identifier
     * @param status   Return status
     * @return List of ReturnEntity
     */
    List<ReturnEntity> findByTenantIdAndReturnStatus(String tenantId, ReturnStatus status);

    /**
     * Finds returns by tenant ID and order number.
     *
     * @param tenantId    Tenant identifier
     * @param orderNumber Order number
     * @return List of ReturnEntity
     */
    List<ReturnEntity> findByTenantIdAndOrderNumber(String tenantId, String orderNumber);
}
