package com.ccbsa.common.domain.valueobject;

/**
 * Value Object: StockClassification
 * <p>
 * Enum representing stock classification based on expiration dates.
 * <p>
 * This enum is shared across services (DRY principle).
 * <p>
 * Classification Rules:
 * - EXPIRED: Expiration date is in the past
 * - CRITICAL: Expiration date is within 7 days
 * - NEAR_EXPIRY: Expiration date is within 30 days (but > 7 days)
 * - NORMAL: Expiration date is more than 30 days away (or null for non-perishable)
 * - EXTENDED_SHELF_LIFE: Expiration date is more than 1 year away
 */
public enum StockClassification {
    EXPIRED("Expired"), CRITICAL("Critical - Expiring within 7 days"), NEAR_EXPIRY("Near Expiry - Expiring within 30 days"), NORMAL("Normal"),
    EXTENDED_SHELF_LIFE("Extended Shelf Life");

    private final String description;

    StockClassification(String description) {
        this.description = description;
    }

    /**
     * Returns the human-readable description of the classification.
     *
     * @return Description string
     */
    public String getDescription() {
        return description;
    }
}

