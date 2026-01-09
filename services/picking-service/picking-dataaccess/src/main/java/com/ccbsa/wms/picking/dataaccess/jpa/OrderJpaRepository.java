package com.ccbsa.wms.picking.dataaccess.jpa;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ccbsa.wms.picking.dataaccess.entity.OrderEntity;

/**
 * JPA Repository: OrderJpaRepository
 * <p>
 * Spring Data JPA repository for OrderEntity.
 */
public interface OrderJpaRepository extends JpaRepository<OrderEntity, UUID> {
    Optional<OrderEntity> findById(UUID id);

    Optional<OrderEntity> findByOrderNumberAndLoadTenantId(String orderNumber, String tenantId);

    List<OrderEntity> findByLoadId(UUID loadId);
}
