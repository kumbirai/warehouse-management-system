package com.ccbsa.common.domain.valueobject;

/**
 * Value Object: AllocationType
 * <p>
 * Represents the type of stock allocation. Immutable enum.
 * <p>
 * This value object is shared across services (DRY principle).
 * <p>
 * Business Rules:
 * - AllocationType cannot be null
 * - Each type represents a valid business scenario for stock allocation
 */
public enum AllocationType {
    /**
     * Allocation for picking orders (reserved for specific orders)
     */
    PICKING_ORDER,

    /**
     * General reservation (not tied to specific order)
     */
    RESERVATION,

    /**
     * Other allocation types not covered by specific categories
     */
    OTHER
}

