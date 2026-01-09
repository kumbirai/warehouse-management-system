package com.ccbsa.wms.picking.dataaccess.jpa;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.ccbsa.wms.picking.dataaccess.entity.PickingTaskEntity;
import com.ccbsa.wms.picking.domain.core.valueobject.PickingTaskStatus;

/**
 * JPA Repository: PickingTaskJpaRepository
 * <p>
 * Spring Data JPA repository for PickingTaskEntity.
 */
public interface PickingTaskJpaRepository extends JpaRepository<PickingTaskEntity, UUID> {
    Optional<PickingTaskEntity> findById(UUID id);

    List<PickingTaskEntity> findByLoadId(UUID loadId);

    List<PickingTaskEntity> findByOrderId(UUID orderId);

    List<PickingTaskEntity> findByStatusOrderBySequenceAsc(PickingTaskStatus status, Pageable pageable);

    List<PickingTaskEntity> findAllByOrderBySequenceAsc(Pageable pageable);

    long countByStatus(PickingTaskStatus status);
}
