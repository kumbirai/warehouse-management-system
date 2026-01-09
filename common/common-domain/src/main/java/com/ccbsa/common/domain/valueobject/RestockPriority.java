package com.ccbsa.common.domain.valueobject;

/**
 * Value Object: RestockPriority
 * <p>
 * Represents the priority level for restock requests. Immutable enum.
 * <p>
 * This value object is shared across services (DRY principle).
 * <p>
 * Priority levels:
 * - HIGH: Urgent restock required (stock critically low)
 * - MEDIUM: Standard restock priority
 * - LOW: Low priority restock (stock slightly below minimum)
 */
public enum RestockPriority {
    /**
     * High priority - Urgent restock required
     */
    HIGH,

    /**
     * Medium priority - Standard restock priority
     */
    MEDIUM,

    /**
     * Low priority - Low priority restock
     */
    LOW
}
