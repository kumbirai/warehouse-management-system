package com.ccbsa.common.domain.valueobject;

/**
 * Value Object: ProductCondition
 * <p>
 * Represents the condition of a returned product. Immutable enum.
 * <p>
 * This value object is shared across services (DRY principle).
 * <p>
 * Business Rules:
 * - ProductCondition cannot be null
 * - Each condition represents a valid state for returned products
 */
public enum ProductCondition {
    /**
     * Product is in good condition and can be restocked
     */
    GOOD,

    /**
     * Product is damaged but may be repairable or sellable at discount
     */
    DAMAGED,

    /**
     * Product has expired and cannot be sold
     */
    EXPIRED,

    /**
     * Product requires quarantine for quality inspection
     */
    QUARANTINE,

    /**
     * Product must be written off and disposed
     */
    WRITE_OFF
}
