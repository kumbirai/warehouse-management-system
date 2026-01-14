package com.ccbsa.wms.returns.domain.core.valueobject;

/**
 * Value Object: DamageAssessmentStatus
 * <p>
 * Represents the status of a damage assessment. Immutable enum.
 * <p>
 * Business Rules:
 * - DamageAssessmentStatus cannot be null
 * - Status transitions: DRAFT → SUBMITTED → UNDER_REVIEW → COMPLETED
 * - CANCELLED can occur at any stage
 */
public enum DamageAssessmentStatus {
    /**
     * Assessment is being prepared
     */
    DRAFT,

    /**
     * Assessment has been submitted
     */
    SUBMITTED,

    /**
     * Assessment is being reviewed by management
     */
    UNDER_REVIEW,

    /**
     * Assessment has been completed and locations assigned
     */
    COMPLETED,

    /**
     * Assessment has been cancelled
     */
    CANCELLED
}
