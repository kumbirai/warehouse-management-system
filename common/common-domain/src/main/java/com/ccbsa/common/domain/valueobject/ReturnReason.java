package com.ccbsa.common.domain.valueobject;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * Value Object: ReturnReason
 * <p>
 * Represents the reason for a return. Immutable enum.
 * <p>
 * This value object is shared across services (DRY principle).
 * <p>
 * Business Rules:
 * - ReturnReason cannot be null
 * - Each reason represents a valid business scenario for returns
 */
@JsonDeserialize(using = ReturnReasonDeserializer.class)
public enum ReturnReason {
    /**
     * Product is defective or not working as expected
     */
    DEFECTIVE,

    /**
     * Wrong item was delivered
     */
    WRONG_ITEM,

    /**
     * Product was damaged during delivery
     */
    DAMAGED,

    /**
     * Product has expired
     */
    EXPIRED,

    /**
     * Customer requested return for other reasons
     */
    CUSTOMER_REQUEST,

    /**
     * Other reasons not covered by specific categories
     */
    OTHER
}
