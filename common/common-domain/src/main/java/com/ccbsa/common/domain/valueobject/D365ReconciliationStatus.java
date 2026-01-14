package com.ccbsa.common.domain.valueobject;

/**
 * Value Object: D365ReconciliationStatus
 * <p>
 * Represents the status of reconciliation with Microsoft Dynamics 365. Immutable enum.
 * <p>
 * This value object is shared across services (DRY principle).
 * <p>
 * Business Rules:
 * - D365ReconciliationStatus cannot be null
 * - Status transitions: PENDING → IN_PROGRESS → SUCCESS or FAILED → RETRYING
 */
public enum D365ReconciliationStatus {
    /**
     * Reconciliation is pending
     */
    PENDING,

    /**
     * Reconciliation is in progress
     */
    IN_PROGRESS,

    /**
     * Reconciliation completed successfully
     */
    SUCCESS,

    /**
     * Reconciliation failed
     */
    FAILED,

    /**
     * Reconciliation is being retried after failure
     */
    RETRYING
}
