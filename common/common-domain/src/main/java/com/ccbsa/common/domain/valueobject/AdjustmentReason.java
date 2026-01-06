package com.ccbsa.common.domain.valueobject;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * Value Object: AdjustmentReason
 * <p>
 * Represents the reason for a stock adjustment. Immutable enum.
 * <p>
 * This value object is shared across services (DRY principle).
 * <p>
 * Business Rules:
 * - AdjustmentReason cannot be null
 * - Each reason represents a valid business scenario for stock adjustment
 */
@JsonDeserialize(using = AdjustmentReasonDeserializer.class)
public enum AdjustmentReason {
    /**
     * Adjustment from stock count/physical inventory
     */
    STOCK_COUNT,

    /**
     * Adjustment due to damaged stock
     */
    DAMAGE,

    /**
     * Adjustment for stock correction
     */
    CORRECTION,

    /**
     * Adjustment due to theft
     */
    THEFT,

    /**
     * Adjustment due to expired stock
     */
    EXPIRATION,

    /**
     * Other reasons not covered by specific categories
     */
    OTHER
}

