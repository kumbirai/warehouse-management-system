package com.ccbsa.wms.returns.domain.core.valueobject;

/**
 * Value Object: ReturnType
 * <p>
 * Represents the type of return. Immutable enum.
 * <p>
 * Business Rules:
 * - ReturnType cannot be null
 * - Each type represents a specific return scenario
 */
public enum ReturnType {
    /**
     * Partial order acceptance - customer accepts some items, returns others
     */
    PARTIAL,

    /**
     * Full order return - entire order is returned
     */
    FULL,

    /**
     * Damage in transit - products damaged during delivery
     */
    DAMAGE_IN_TRANSIT
}
