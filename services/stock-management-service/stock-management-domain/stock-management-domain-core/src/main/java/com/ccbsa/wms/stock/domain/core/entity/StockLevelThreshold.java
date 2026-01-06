package com.ccbsa.wms.stock.domain.core.entity;

import java.time.LocalDateTime;

import com.ccbsa.common.domain.TenantAwareAggregateRoot;
import com.ccbsa.common.domain.valueobject.BigDecimalQuantity;
import com.ccbsa.common.domain.valueobject.MaximumQuantity;
import com.ccbsa.common.domain.valueobject.MinimumQuantity;
import com.ccbsa.common.domain.valueobject.ProductId;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;
import com.ccbsa.wms.stock.domain.core.event.StockLevelAboveMaximumEvent;
import com.ccbsa.wms.stock.domain.core.event.StockLevelBelowMinimumEvent;
import com.ccbsa.wms.stock.domain.core.valueobject.StockLevelThresholdId;

/**
 * Aggregate Root: StockLevelThreshold
 * <p>
 * Maintains minimum and maximum stock level thresholds.
 * <p>
 * Business Rules:
 * - Minimum quantity must be less than maximum quantity
 * - Thresholds can be defined per product or per product/location
 * - One threshold configuration per product/location combination
 * - Validates stock levels and publishes events when thresholds are breached
 */
public class StockLevelThreshold extends TenantAwareAggregateRoot<StockLevelThresholdId> {

    private ProductId productId;
    private LocationId locationId; // NULL for warehouse-wide threshold
    private MinimumQuantity minimumQuantity;
    private MaximumQuantity maximumQuantity;
    private boolean enableAutoRestock;
    private LocalDateTime createdAt;
    private LocalDateTime lastModifiedAt;

    /**
     * Private constructor for builder pattern. Prevents direct instantiation.
     */
    private StockLevelThreshold() {
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
     * Business logic method: Update minimum quantity.
     * <p>
     * Business Rules:
     * - Minimum must be less than maximum
     * - Updates lastModifiedAt timestamp
     *
     * @param newMinimum New minimum quantity
     * @throws IllegalArgumentException if minimum is not less than maximum
     */
    public void updateMinimumQuantity(MinimumQuantity newMinimum) {
        if (newMinimum == null) {
            throw new IllegalArgumentException("MinimumQuantity cannot be null");
        }
        if (this.maximumQuantity != null && newMinimum.getValue().compareTo(this.maximumQuantity.getValue()) >= 0) {
            throw new IllegalArgumentException("Minimum quantity must be less than maximum quantity");
        }

        this.minimumQuantity = newMinimum;
        this.lastModifiedAt = LocalDateTime.now();
    }

    /**
     * Business logic method: Update maximum quantity.
     * <p>
     * Business Rules:
     * - Maximum must be greater than minimum
     * - Updates lastModifiedAt timestamp
     *
     * @param newMaximum New maximum quantity
     * @throws IllegalArgumentException if maximum is not greater than minimum
     */
    public void updateMaximumQuantity(MaximumQuantity newMaximum) {
        if (newMaximum == null) {
            throw new IllegalArgumentException("MaximumQuantity cannot be null");
        }
        if (this.minimumQuantity != null && newMaximum.getValue().compareTo(this.minimumQuantity.getValue()) <= 0) {
            throw new IllegalArgumentException("Maximum quantity must be greater than minimum quantity");
        }

        // Validate maximum is greater than minimum
        newMaximum.validateGreaterThanMinimum(this.minimumQuantity);

        this.maximumQuantity = newMaximum;
        this.lastModifiedAt = LocalDateTime.now();
    }

    /**
     * Business logic method: Validate stock level against thresholds.
     * <p>
     * Business Rules:
     * - Publishes StockLevelBelowMinimumEvent if below minimum
     * - Publishes StockLevelAboveMaximumEvent if above maximum
     *
     * @param currentQuantity Current stock quantity
     */
    public void checkMinMaxThresholds(BigDecimalQuantity currentQuantity) {
        if (currentQuantity == null) {
            throw new IllegalArgumentException("CurrentQuantity cannot be null");
        }

        if (this.minimumQuantity != null && currentQuantity.isLessThan(this.minimumQuantity.getValue())) {
            addDomainEvent(
                    new StockLevelBelowMinimumEvent(this.getId(), this.getTenantId(), this.productId, this.locationId, currentQuantity.getValue(), this.minimumQuantity.getValue(),
                            this.enableAutoRestock));
        }

        if (this.maximumQuantity != null && currentQuantity.isGreaterThan(this.maximumQuantity.getValue())) {
            addDomainEvent(new StockLevelAboveMaximumEvent(this.getId(), this.getTenantId(), this.productId, this.locationId, currentQuantity.getValue(),
                    this.maximumQuantity.getValue()));
        }
    }

    /**
     * Business logic method: Check if quantity would exceed maximum.
     *
     * @param quantityToAdd   Quantity to add
     * @param currentQuantity Current quantity
     * @return true if adding quantity would exceed maximum
     */
    public boolean wouldExceedMaximum(BigDecimalQuantity quantityToAdd, BigDecimalQuantity currentQuantity) {
        if (this.maximumQuantity == null) {
            return false; // No maximum limit
        }
        if (quantityToAdd == null || currentQuantity == null) {
            throw new IllegalArgumentException("Quantity values cannot be null");
        }
        BigDecimalQuantity newQuantity = currentQuantity.add(quantityToAdd);
        return newQuantity.isGreaterThan(this.maximumQuantity.getValue());
    }

    /**
     * Business logic method: Check if quantity is below minimum.
     *
     * @param currentQuantity Current quantity
     * @return true if below minimum
     */
    public boolean isBelowMinimum(BigDecimalQuantity currentQuantity) {
        if (this.minimumQuantity == null) {
            return false; // No minimum limit
        }
        if (currentQuantity == null) {
            throw new IllegalArgumentException("CurrentQuantity cannot be null");
        }
        return currentQuantity.isLessThan(this.minimumQuantity.getValue());
    }

    // Getters (read-only access)

    public ProductId getProductId() {
        return productId;
    }

    public LocationId getLocationId() {
        return locationId;
    }

    public MinimumQuantity getMinimumQuantity() {
        return minimumQuantity;
    }

    public MaximumQuantity getMaximumQuantity() {
        return maximumQuantity;
    }

    public boolean isEnableAutoRestock() {
        return enableAutoRestock;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getLastModifiedAt() {
        return lastModifiedAt;
    }

    /**
     * Builder class for constructing StockLevelThreshold instances. Ensures all required fields are set and validated.
     */
    public static class Builder {
        private StockLevelThreshold threshold = new StockLevelThreshold();

        public Builder stockLevelThresholdId(StockLevelThresholdId id) {
            threshold.setId(id);
            return this;
        }

        public Builder tenantId(TenantId tenantId) {
            threshold.setTenantId(tenantId);
            return this;
        }

        public Builder productId(ProductId productId) {
            threshold.productId = productId;
            return this;
        }

        public Builder locationId(LocationId locationId) {
            threshold.locationId = locationId;
            return this;
        }

        public Builder minimumQuantity(MinimumQuantity minimumQuantity) {
            threshold.minimumQuantity = minimumQuantity;
            return this;
        }

        public Builder maximumQuantity(MaximumQuantity maximumQuantity) {
            threshold.maximumQuantity = maximumQuantity;
            return this;
        }

        public Builder enableAutoRestock(boolean enableAutoRestock) {
            threshold.enableAutoRestock = enableAutoRestock;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            threshold.createdAt = createdAt;
            return this;
        }

        public Builder lastModifiedAt(LocalDateTime lastModifiedAt) {
            threshold.lastModifiedAt = lastModifiedAt;
            return this;
        }

        /**
         * Sets the version (for loading from database).
         *
         * @param version Version number
         * @return Builder instance
         */
        public Builder version(int version) {
            threshold.setVersion(version);
            return this;
        }

        /**
         * Sets the version as Long (for loading from database).
         *
         * @param version Version number
         * @return Builder instance
         */
        public Builder version(Long version) {
            threshold.setVersion(version != null ? version.intValue() : 0);
            return this;
        }

        /**
         * Builds and validates the StockLevelThreshold instance.
         *
         * @return Validated StockLevelThreshold instance
         * @throws IllegalArgumentException if validation fails
         */
        public StockLevelThreshold build() {
            validate();
            initializeDefaults();
            return consumeThreshold();
        }

        /**
         * Validates all required fields are set.
         *
         * @throws IllegalArgumentException if validation fails
         */
        private void validate() {
            if (threshold.getId() == null) {
                throw new IllegalArgumentException("StockLevelThresholdId is required");
            }
            if (threshold.getTenantId() == null) {
                throw new IllegalArgumentException("TenantId is required");
            }
            if (threshold.productId == null) {
                throw new IllegalArgumentException("ProductId is required");
            }
            if (threshold.minimumQuantity == null) {
                throw new IllegalArgumentException("MinimumQuantity is required");
            }
            if (threshold.maximumQuantity == null) {
                throw new IllegalArgumentException("MaximumQuantity is required");
            }
            // Validate minimum < maximum
            threshold.maximumQuantity.validateGreaterThanMinimum(threshold.minimumQuantity);
        }

        /**
         * Initializes default values for optional fields.
         */
        private void initializeDefaults() {
            if (threshold.createdAt == null) {
                threshold.createdAt = LocalDateTime.now();
            }
            if (threshold.lastModifiedAt == null) {
                threshold.lastModifiedAt = LocalDateTime.now();
            }
        }

        /**
         * Consumes the threshold from the builder and returns it. Creates a new threshold instance for the next build.
         *
         * @return Built threshold
         */
        private StockLevelThreshold consumeThreshold() {
            StockLevelThreshold builtThreshold = threshold;
            threshold = new StockLevelThreshold();
            return builtThreshold;
        }

        /**
         * Builds StockLevelThreshold without publishing events. Used when reconstructing from database.
         *
         * @return Validated StockLevelThreshold instance
         * @throws IllegalArgumentException if validation fails
         */
        public StockLevelThreshold buildWithoutEvents() {
            validate();
            initializeDefaults();
            // Do not publish events when loading from database
            return consumeThreshold();
        }
    }
}

