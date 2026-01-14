package com.ccbsa.common.domain.valueobject;

/**
 * Value Object: ReturnStatus
 * <p>
 * Represents the status of a return order. Immutable enum.
 * <p>
 * This value object is shared across services (DRY principle).
 * <p>
 * Business Rules:
 * - ReturnStatus cannot be null
 * - Status transitions: INITIATED → PROCESSED → LOCATION_ASSIGNED → RECONCILED
 * - CANCELLED can occur at any stage
 */
public enum ReturnStatus {
    /**
     * Return has been initiated but not yet processed
     */
    INITIATED,

    /**
     * Return has been processed and validated
     */
    PROCESSED,

    /**
     * Return location has been assigned for storage
     */
    LOCATION_ASSIGNED,

    /**
     * Return has been reconciled with D365
     */
    RECONCILED,

    /**
     * Return has been cancelled
     */
    CANCELLED
}
