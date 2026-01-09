package com.ccbsa.wms.picking.dataaccess.jpa;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.ccbsa.wms.picking.dataaccess.entity.PickingListEntity;
import com.ccbsa.wms.picking.domain.core.valueobject.PickingListStatus;

/**
 * JPA Repository: PickingListJpaRepository
 * <p>
 * Spring Data JPA repository for PickingListEntity.
 */
public interface PickingListJpaRepository extends JpaRepository<PickingListEntity, UUID> {
    Optional<PickingListEntity> findByTenantIdAndId(String tenantId, UUID id);

    List<PickingListEntity> findByTenantIdAndStatusOrderByReceivedAtDesc(String tenantId, PickingListStatus status, Pageable pageable);

    List<PickingListEntity> findByTenantIdOrderByReceivedAtDesc(String tenantId, Pageable pageable);

    long countByTenantIdAndStatus(String tenantId, PickingListStatus status);

    long countByTenantId(String tenantId);
}
