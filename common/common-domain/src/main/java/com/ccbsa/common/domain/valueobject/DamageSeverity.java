package com.ccbsa.common.domain.valueobject;

/**
 * Value Object: DamageSeverity
 * <p>
 * Represents the severity level of damage to a product. Immutable enum.
 * <p>
 * This value object is shared across services (DRY principle).
 * <p>
 * Business Rules:
 * - DamageSeverity cannot be null
 * - Severity levels are ordered from least to most severe
 */
public enum DamageSeverity {
    /**
     * Minor damage that may be repairable or sellable at discount
     */
    MINOR,

    /**
     * Moderate damage requiring assessment
     */
    MODERATE,

    /**
     * Severe damage, likely requires write-off
     */
    SEVERE,

    /**
     * Total loss, product is completely unusable
     */
    TOTAL
}
