package com.ccbsa.wms.picking.dataaccess.jpa;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.ccbsa.wms.picking.dataaccess.entity.LoadEntity;
import com.ccbsa.wms.picking.domain.core.valueobject.LoadStatus;

/**
 * JPA Repository: LoadJpaRepository
 * <p>
 * Spring Data JPA repository for LoadEntity.
 */
public interface LoadJpaRepository extends JpaRepository<LoadEntity, UUID> {
    Optional<LoadEntity> findByTenantIdAndId(String tenantId, UUID id);

    Optional<LoadEntity> findByTenantIdAndLoadNumber(String tenantId, String loadNumber);

    List<LoadEntity> findByTenantIdAndStatusOrderByCreatedAtDesc(String tenantId, LoadStatus status, Pageable pageable);

    List<LoadEntity> findByTenantIdOrderByCreatedAtDesc(String tenantId, Pageable pageable);
}
