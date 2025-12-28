package com.ccbsa.common.domain.valueobject;

/**
 * Value Object: AdjustmentType
 * <p>
 * Represents the type of stock adjustment. Immutable enum.
 * <p>
 * This value object is shared across services (DRY principle).
 * <p>
 * Business Rules:
 * - AdjustmentType cannot be null
 * - Each type represents a valid business scenario for stock adjustment
 */
public enum AdjustmentType {
    /**
     * Increase stock level (positive adjustment)
     */
    INCREASE,

    /**
     * Decrease stock level (negative adjustment)
     */
    DECREASE
}

