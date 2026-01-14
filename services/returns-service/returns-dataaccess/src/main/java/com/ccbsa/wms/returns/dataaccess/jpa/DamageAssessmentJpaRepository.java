package com.ccbsa.wms.returns.dataaccess.jpa;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ccbsa.wms.returns.dataaccess.entity.DamageAssessmentEntity;

/**
 * JPA Repository: DamageAssessmentJpaRepository
 * <p>
 * Spring Data JPA repository for DamageAssessmentEntity. Provides database access methods with multi-tenant support.
 */
public interface DamageAssessmentJpaRepository extends JpaRepository<DamageAssessmentEntity, UUID> {
    /**
     * Finds a damage assessment by tenant ID and assessment ID.
     *
     * @param tenantId     Tenant identifier
     * @param assessmentId Assessment identifier
     * @return Optional DamageAssessmentEntity if found
     */
    Optional<DamageAssessmentEntity> findByTenantIdAndAssessmentId(String tenantId, UUID assessmentId);
}
