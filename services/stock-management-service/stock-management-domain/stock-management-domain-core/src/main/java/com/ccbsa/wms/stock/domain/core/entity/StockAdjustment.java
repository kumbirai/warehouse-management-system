package com.ccbsa.wms.stock.domain.core.entity;

import java.time.LocalDateTime;

import com.ccbsa.common.domain.TenantAwareAggregateRoot;
import com.ccbsa.common.domain.valueobject.AdjustmentReason;
import com.ccbsa.common.domain.valueobject.AdjustmentType;
import com.ccbsa.common.domain.valueobject.ProductId;
import com.ccbsa.common.domain.valueobject.Quantity;
import com.ccbsa.common.domain.valueobject.StockItemId;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.common.domain.valueobject.UserId;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;
import com.ccbsa.wms.stock.domain.core.event.StockAdjustedEvent;
import com.ccbsa.wms.stock.domain.core.valueobject.StockAdjustmentId;

/**
 * Aggregate Root: StockAdjustment
 * <p>
 * Represents a manual stock level adjustment.
 * Used for correcting discrepancies from stock counts, damage, etc.
 * <p>
 * Business Rules:
 * - Adjustment quantity must be positive
 * - DECREASE adjustments cannot result in negative stock
 * - Adjustment reason is required
 * - Authorization may be required for large adjustments (validated at application service)
 */
public class StockAdjustment extends TenantAwareAggregateRoot<StockAdjustmentId> {

    private ProductId productId;
    private LocationId locationId; // Optional - null for product-wide adjustment
    private StockItemId stockItemId; // Optional - null for product/location adjustment
    private AdjustmentType adjustmentType;
    private Quantity quantity;
    private AdjustmentReason reason;
    private String notes;
    private UserId adjustedBy;
    private String authorizationCode; // For large adjustments
    private LocalDateTime adjustedAt;

    // Before/after quantities for audit
    private int quantityBefore;
    private int quantityAfter;

    /**
     * Private constructor for builder pattern. Prevents direct instantiation.
     */
    private StockAdjustment() {
    }

    /**
     * Factory method to create builder instance.
     *
     * @return Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Business logic method: Adjusts stock level.
     * <p>
     * Business Rules:
     * - Validates adjustment doesn't result in negative stock
     * - Calculates before/after quantities
     * - Records adjustment timestamp
     * - Publishes StockAdjustedEvent
     *
     * @param currentQuantity Current stock quantity before adjustment
     * @throws IllegalStateException if adjustment would result in negative stock
     */
    public void adjust(int currentQuantity) {
        if (currentQuantity < 0) {
            throw new IllegalArgumentException("Current quantity cannot be negative");
        }

        // Validate quantity is set (defensive check - builder should have validated this)
        if (this.quantity == null) {
            throw new IllegalStateException("Quantity must be set before adjusting stock");
        }

        // Calculate after quantity
        int afterQuantity;
        if (this.adjustmentType == AdjustmentType.INCREASE) {
            afterQuantity = currentQuantity + this.quantity.getValue();
        } else { // DECREASE
            afterQuantity = currentQuantity - this.quantity.getValue();

            // Validate doesn't result in negative
            if (afterQuantity < 0) {
                throw new IllegalStateException(
                        String.format("Adjustment would result in negative stock. Current: %d, Adjustment: %d, Result: %d", currentQuantity, this.quantity.getValue(),
                                afterQuantity));
            }
        }

        // Set before/after quantities
        this.quantityBefore = currentQuantity;
        this.quantityAfter = afterQuantity;

        // Publish domain event
        addDomainEvent(
                new StockAdjustedEvent(this.getId(), this.getTenantId(), this.productId, this.locationId, this.stockItemId, this.adjustmentType, this.quantity, this.quantityBefore,
                        this.quantityAfter, this.reason, this.notes));
    }

    // Getters (read-only access)

    public ProductId getProductId() {
        return productId;
    }

    public LocationId getLocationId() {
        return locationId;
    }

    public StockItemId getStockItemId() {
        return stockItemId;
    }

    public AdjustmentType getAdjustmentType() {
        return adjustmentType;
    }

    public Quantity getQuantity() {
        return quantity;
    }

    public AdjustmentReason getReason() {
        return reason;
    }

    public String getNotes() {
        return notes;
    }

    public UserId getAdjustedBy() {
        return adjustedBy;
    }

    public String getAuthorizationCode() {
        return authorizationCode;
    }

    public LocalDateTime getAdjustedAt() {
        return adjustedAt;
    }

    public int getQuantityBefore() {
        return quantityBefore;
    }

    public int getQuantityAfter() {
        return quantityAfter;
    }

    /**
     * Builder class for constructing StockAdjustment instances. Ensures all required fields are set and validated.
     */
    public static class Builder {
        private StockAdjustment adjustment = new StockAdjustment();

        public Builder stockAdjustmentId(StockAdjustmentId id) {
            adjustment.setId(id);
            return this;
        }

        public Builder tenantId(TenantId tenantId) {
            adjustment.setTenantId(tenantId);
            return this;
        }

        public Builder productId(ProductId productId) {
            adjustment.productId = productId;
            return this;
        }

        public Builder locationId(LocationId locationId) {
            adjustment.locationId = locationId;
            return this;
        }

        public Builder stockItemId(StockItemId stockItemId) {
            adjustment.stockItemId = stockItemId;
            return this;
        }

        public Builder adjustmentType(AdjustmentType adjustmentType) {
            adjustment.adjustmentType = adjustmentType;
            return this;
        }

        public Builder quantity(Quantity quantity) {
            adjustment.quantity = quantity;
            return this;
        }

        public Builder reason(AdjustmentReason reason) {
            adjustment.reason = reason;
            return this;
        }

        public Builder notes(String notes) {
            adjustment.notes = notes;
            return this;
        }

        public Builder adjustedBy(UserId adjustedBy) {
            adjustment.adjustedBy = adjustedBy;
            return this;
        }

        public Builder authorizationCode(String authorizationCode) {
            adjustment.authorizationCode = authorizationCode;
            return this;
        }

        public Builder quantityBefore(int quantityBefore) {
            adjustment.quantityBefore = quantityBefore;
            return this;
        }

        public Builder adjustedAt(LocalDateTime adjustedAt) {
            adjustment.adjustedAt = adjustedAt;
            return this;
        }

        /**
         * Sets the version (for loading from database).
         *
         * @param version Version number
         * @return Builder instance
         */
        public Builder version(int version) {
            adjustment.setVersion(version);
            return this;
        }

        /**
         * Sets the version as Long (for loading from database).
         *
         * @param version Version number
         * @return Builder instance
         */
        public Builder version(Long version) {
            adjustment.setVersion(version != null ? version.intValue() : 0);
            return this;
        }

        /**
         * Builds and validates the StockAdjustment instance.
         * <p>
         * Does NOT call adjust() - that must be called explicitly after building with current quantity.
         *
         * @return Validated StockAdjustment instance
         * @throws IllegalArgumentException if validation fails
         */
        public StockAdjustment build() {
            validate();
            initializeDefaults();
            return consumeAdjustment();
        }

        /**
         * Validates all required fields are set.
         *
         * @throws IllegalArgumentException if validation fails
         */
        private void validate() {
            if (adjustment.getId() == null) {
                throw new IllegalArgumentException("StockAdjustmentId is required");
            }
            if (adjustment.getTenantId() == null) {
                throw new IllegalArgumentException("TenantId is required");
            }
            if (adjustment.productId == null) {
                throw new IllegalArgumentException("ProductId is required");
            }
            if (adjustment.adjustmentType == null) {
                throw new IllegalArgumentException("AdjustmentType is required");
            }
            if (adjustment.quantity == null || !adjustment.quantity.isPositive()) {
                throw new IllegalArgumentException("Quantity must be positive");
            }
            if (adjustment.reason == null) {
                throw new IllegalArgumentException("AdjustmentReason is required");
            }
            if (adjustment.adjustedBy == null) {
                throw new IllegalArgumentException("AdjustedBy is required");
            }
        }

        /**
         * Initializes default values for optional fields.
         */
        private void initializeDefaults() {
            if (adjustment.adjustedAt == null) {
                adjustment.adjustedAt = LocalDateTime.now();
            }
        }

        /**
         * Consumes the adjustment from the builder and returns it. Creates a new adjustment instance for the next build.
         *
         * @return Built adjustment
         */
        private StockAdjustment consumeAdjustment() {
            StockAdjustment builtAdjustment = adjustment;
            adjustment = new StockAdjustment();
            return builtAdjustment;
        }

        /**
         * Builds StockAdjustment without publishing events. Used when reconstructing from database.
         *
         * @return Validated StockAdjustment instance
         * @throws IllegalArgumentException if validation fails
         */
        public StockAdjustment buildWithoutEvents() {
            validate();
            initializeDefaults();
            // Do not publish events when loading from database
            return consumeAdjustment();
        }
    }
}

