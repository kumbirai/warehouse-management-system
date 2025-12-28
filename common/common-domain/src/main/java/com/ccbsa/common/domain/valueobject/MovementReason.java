package com.ccbsa.common.domain.valueobject;

/**
 * Value Object: MovementReason
 * <p>
 * Represents the reason for a stock movement. Immutable enum.
 * <p>
 * This value object is shared across services (DRY principle).
 * <p>
 * Business Rules:
 * - MovementReason cannot be null
 * - Each reason represents a valid business scenario for stock movement
 */
public enum MovementReason {
    /**
     * Movement for picking operations (from storage to picking location)
     */
    PICKING,

    /**
     * Movement for restocking operations (from receiving to storage)
     */
    RESTOCKING,

    /**
     * Movement for warehouse reorganization
     */
    REORGANIZATION,

    /**
     * Movement due to damaged stock
     */
    DAMAGE,

    /**
     * Movement for stock correction/adjustment
     */
    CORRECTION,

    /**
     * Other reasons not covered by specific categories
     */
    OTHER
}

