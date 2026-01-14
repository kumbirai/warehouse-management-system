package com.ccbsa.wms.returns.application.service.port.repository;

import java.util.Optional;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.returns.domain.core.entity.DamageAssessment;
import com.ccbsa.wms.returns.domain.core.valueobject.DamageAssessmentId;

/**
 * Repository Port: DamageAssessmentRepository
 * <p>
 * Defines the contract for DamageAssessment aggregate persistence. Implemented by data access adapters.
 * <p>
 * This port is defined in the application service layer to maintain proper dependency direction in hexagonal architecture.
 */
public interface DamageAssessmentRepository {
    /**
     * Saves a DamageAssessment aggregate.
     * <p>
     * Creates a new damage assessment if it doesn't exist, or updates an existing one.
     *
     * @param damageAssessment DamageAssessment aggregate to save
     * @return Saved damage assessment aggregate
     */
    DamageAssessment save(DamageAssessment damageAssessment);

    /**
     * Finds a DamageAssessment by ID and tenant ID.
     *
     * @param id       Damage assessment identifier
     * @param tenantId Tenant identifier
     * @return Optional DamageAssessment if found
     */
    Optional<DamageAssessment> findByIdAndTenantId(DamageAssessmentId id, TenantId tenantId);
}
