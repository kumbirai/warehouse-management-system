package com.ccbsa.wms.stock.domain.core.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import com.ccbsa.common.domain.TenantAwareAggregateRoot;
import com.ccbsa.common.domain.valueobject.ExpirationDate;
import com.ccbsa.common.domain.valueobject.ProductId;
import com.ccbsa.common.domain.valueobject.Quantity;
import com.ccbsa.common.domain.valueobject.StockClassification;
import com.ccbsa.common.domain.valueobject.StockItemId;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;
import com.ccbsa.wms.stock.domain.core.event.LocationAssignedEvent;
import com.ccbsa.wms.stock.domain.core.event.StockClassifiedEvent;
import com.ccbsa.wms.stock.domain.core.event.StockExpiredEvent;
import com.ccbsa.wms.stock.domain.core.event.StockExpiringAlertEvent;
import com.ccbsa.wms.stock.domain.core.valueobject.ConsignmentId;

/**
 * Aggregate Root: StockItem
 * <p>
 * Represents individual stock item with expiration tracking and classification.
 * <p>
 * Business Rules:
 * - Stock items must have valid expiration dates (or null for non-perishable)
 * - Stock classification automatically assigned based on expiration date
 * - Expired stock cannot be picked
 * - Stock within 7 days of expiration generates alert
 * - Stock within 30 days of expiration classified as "Near Expiry"
 */
public class StockItem extends TenantAwareAggregateRoot<StockItemId> {

    private ProductId productId;
    private LocationId locationId; // May be null initially
    private Quantity quantity;
    private Quantity allocatedQuantity; // Quantity allocated for picking orders
    private ExpirationDate expirationDate; // May be null for non-perishable
    private StockClassification classification;
    private ConsignmentId consignmentId; // Reference to source consignment
    private LocalDateTime createdAt;
    private LocalDateTime lastModifiedAt;

    /**
     * Private constructor for builder pattern. Prevents direct instantiation.
     */
    private StockItem() {
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
     * Business logic method: Updates expiration date and reclassifies.
     *
     * @param newExpirationDate New expiration date (can be null for non-perishable)
     */
    public void updateExpirationDate(ExpirationDate newExpirationDate) {
        this.expirationDate = newExpirationDate;
        this.lastModifiedAt = LocalDateTime.now();
        classify(); // Reclassify after expiration date change
    }

    /**
     * Business logic method: Classifies stock based on expiration date.
     * <p>
     * Classification Rules:
     * - EXPIRED: Expiration date is in the past
     * - CRITICAL: Expiration date is within 7 days
     * - NEAR_EXPIRY: Expiration date is within 30 days (but > 7 days)
     * - NORMAL: Expiration date is more than 30 days away (or null)
     * - EXTENDED_SHELF_LIFE: Expiration date is more than 1 year away
     */
    public void classify() {
        if (expirationDate == null) {
            // Non-perishable items are always NORMAL
            if (this.classification != StockClassification.NORMAL) {
                StockClassification oldClassification = this.classification;
                this.classification = StockClassification.NORMAL;
                this.lastModifiedAt = LocalDateTime.now();

                // Publish classification change event
                // Always publish for initial classification (oldClassification == null) to trigger FEFO assignment
                addDomainEvent(
                        new StockClassifiedEvent(this.getId().getValueAsString(), this.productId, this.getTenantId(), oldClassification, this.classification, null, this.quantity));
            }
            return;
        }

        LocalDate today = LocalDate.now();
        LocalDate expiryDate = expirationDate.getValue();
        long daysUntilExpiry = ChronoUnit.DAYS.between(today, expiryDate);

        StockClassification newClassification;

        if (daysUntilExpiry < 0) {
            newClassification = StockClassification.EXPIRED;
            if (this.classification != StockClassification.EXPIRED) {
                addDomainEvent(new StockExpiredEvent(this.getId(), this.productId, expirationDate));
            }
        } else if (daysUntilExpiry <= 7) {
            newClassification = StockClassification.CRITICAL;
            if (this.classification != StockClassification.CRITICAL) {
                addDomainEvent(new StockExpiringAlertEvent(this.getId(), this.productId, expirationDate, 7));
            }
        } else if (daysUntilExpiry <= 30) {
            newClassification = StockClassification.NEAR_EXPIRY;
            if (this.classification != StockClassification.NEAR_EXPIRY) {
                addDomainEvent(new StockExpiringAlertEvent(this.getId(), this.productId, expirationDate, 30));
            }
        } else if (daysUntilExpiry > 365) {
            newClassification = StockClassification.EXTENDED_SHELF_LIFE;
        } else {
            newClassification = StockClassification.NORMAL;
        }

        if (this.classification != newClassification) {
            StockClassification oldClassification = this.classification;
            this.classification = newClassification;
            this.lastModifiedAt = LocalDateTime.now();

            // Publish classification change event
            addDomainEvent(new StockClassifiedEvent(this.getId().getValueAsString(), this.productId, this.getTenantId(), oldClassification, newClassification, expirationDate,
                    this.quantity));
        }
    }

    /**
     * Business logic method: Assigns location to stock item.
     * <p>
     * Business Rules:
     * - Location must be available (validated at application service layer)
     * - Location must have sufficient capacity (validated at application service layer)
     * - Stock item must be in valid state (not expired, quantity > 0)
     * - Publishes LocationAssignedEvent
     *
     * @param locationId Location ID to assign
     * @param quantity   Quantity to assign (must not exceed stock item quantity)
     * @throws IllegalStateException    if stock item cannot be assigned
     * @throws IllegalArgumentException if quantity exceeds stock item quantity
     */
    public void assignLocation(LocationId locationId, Quantity quantity) {
        // Validate stock item can be assigned
        if (this.classification == StockClassification.EXPIRED) {
            throw new IllegalStateException("Cannot assign location to expired stock");
        }
        if (this.quantity == null || this.quantity.getValue() <= 0) {
            throw new IllegalStateException("Cannot assign location to stock with zero quantity");
        }

        // Validate quantity doesn't exceed stock item quantity
        if (quantity.getValue() > this.quantity.getValue()) {
            throw new IllegalArgumentException(String.format("Assigned quantity (%d) exceeds stock item quantity (%d)", quantity.getValue(), this.quantity.getValue()));
        }

        // Update location
        this.locationId = locationId;
        this.lastModifiedAt = LocalDateTime.now();

        // Publish domain event
        addDomainEvent(new LocationAssignedEvent(this.getId(), this.productId, locationId, quantity, this.expirationDate, this.classification));
    }

    /**
     * Business logic method: Updates allocated quantity.
     * <p>
     * Business Rules:
     * - Allocated quantity cannot exceed total quantity
     * - Allocated quantity cannot be negative
     *
     * @param newAllocatedQuantity New allocated quantity
     * @throws IllegalArgumentException if allocated quantity is invalid
     */
    public void updateAllocatedQuantity(Quantity newAllocatedQuantity) {
        if (newAllocatedQuantity == null) {
            throw new IllegalArgumentException("AllocatedQuantity cannot be null");
        }
        if (this.quantity == null) {
            throw new IllegalStateException("Cannot set allocated quantity before total quantity is set");
        }
        if (newAllocatedQuantity.getValue() > this.quantity.getValue()) {
            throw new IllegalArgumentException(
                    String.format("Allocated quantity (%d) cannot exceed total quantity (%d)", newAllocatedQuantity.getValue(), this.quantity.getValue()));
        }

        this.allocatedQuantity = newAllocatedQuantity;
        this.lastModifiedAt = LocalDateTime.now();
    }

    /**
     * Business logic method: Checks if stock can be picked.
     *
     * @return true if stock can be picked
     */
    public boolean canBePicked() {
        return classification != StockClassification.EXPIRED && quantity != null && quantity.getValue() > 0 && getAvailableQuantity().getValue() > 0;
    }

    /**
     * Business logic method: Gets available quantity (total - allocated).
     *
     * @return Available quantity
     */
    public Quantity getAvailableQuantity() {
        if (this.quantity == null) {
            return Quantity.of(0);
        }
        int allocated = this.allocatedQuantity != null ? this.allocatedQuantity.getValue() : 0;
        int available = this.quantity.getValue() - allocated;
        return Quantity.of(Math.max(0, available));
    }

    /**
     * Business logic method: Increases stock quantity.
     * <p>
     * Business Rules:
     * - Quantity must be positive
     * - Updates lastModifiedAt timestamp
     *
     * @param additionalQuantity Additional quantity to add
     * @throws IllegalArgumentException if additionalQuantity is null or non-positive
     */
    public void increaseQuantity(Quantity additionalQuantity) {
        if (additionalQuantity == null) {
            throw new IllegalArgumentException("AdditionalQuantity cannot be null");
        }
        if (additionalQuantity.getValue() <= 0) {
            throw new IllegalArgumentException("AdditionalQuantity must be positive");
        }
        if (this.quantity == null) {
            this.quantity = additionalQuantity;
        } else {
            this.quantity = this.quantity.add(additionalQuantity);
        }
        this.lastModifiedAt = LocalDateTime.now();
    }

    /**
     * Business logic method: Decreases stock quantity.
     * <p>
     * Business Rules:
     * - Quantity must be positive
     * - Resulting quantity cannot be negative
     * - Updates lastModifiedAt timestamp
     *
     * @param quantityToSubtract Quantity to subtract
     * @throws IllegalArgumentException if quantityToSubtract is null or non-positive
     * @throws IllegalStateException    if resulting quantity would be negative
     */
    public void decreaseQuantity(Quantity quantityToSubtract) {
        if (quantityToSubtract == null) {
            throw new IllegalArgumentException("QuantityToSubtract cannot be null");
        }
        if (quantityToSubtract.getValue() <= 0) {
            throw new IllegalArgumentException("QuantityToSubtract must be positive");
        }
        if (this.quantity == null) {
            throw new IllegalStateException("Cannot decrease quantity when current quantity is null");
        }
        if (quantityToSubtract.getValue() > this.quantity.getValue()) {
            throw new IllegalStateException(String.format("Cannot decrease quantity by %d when current quantity is %d", quantityToSubtract.getValue(), this.quantity.getValue()));
        }
        this.quantity = this.quantity.subtract(quantityToSubtract);
        this.lastModifiedAt = LocalDateTime.now();
    }

    /**
     * Business logic method: Updates stock quantity to a specific value.
     * <p>
     * Business Rules:
     * - Quantity must be non-negative (can be zero)
     * - Updates lastModifiedAt timestamp
     * <p>
     * Used for stock adjustments where the exact quantity after adjustment is known.
     *
     * @param newQuantity New quantity value
     * @throws IllegalArgumentException if newQuantity is null or negative
     */
    public void updateQuantity(Quantity newQuantity) {
        if (newQuantity == null) {
            throw new IllegalArgumentException("NewQuantity cannot be null");
        }
        if (newQuantity.getValue() < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative");
        }
        this.quantity = newQuantity;
        this.lastModifiedAt = LocalDateTime.now();
    }

    // Getters

    public ProductId getProductId() {
        return productId;
    }

    public LocationId getLocationId() {
        return locationId;
    }

    public Quantity getQuantity() {
        return quantity;
    }

    public Quantity getAllocatedQuantity() {
        return allocatedQuantity != null ? allocatedQuantity : Quantity.of(0);
    }

    public ExpirationDate getExpirationDate() {
        return expirationDate;
    }

    public StockClassification getClassification() {
        return classification;
    }

    public ConsignmentId getConsignmentId() {
        return consignmentId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getLastModifiedAt() {
        return lastModifiedAt;
    }

    /**
     * Builder class for constructing StockItem instances. Ensures all required fields are set and validated.
     */
    public static class Builder {
        private StockItem stockItem = new StockItem();

        public Builder stockItemId(StockItemId id) {
            stockItem.setId(id);
            return this;
        }

        public Builder tenantId(TenantId tenantId) {
            stockItem.setTenantId(tenantId);
            return this;
        }

        public Builder productId(ProductId productId) {
            stockItem.productId = productId;
            return this;
        }

        public Builder locationId(LocationId locationId) {
            stockItem.locationId = locationId;
            return this;
        }

        public Builder quantity(Quantity quantity) {
            stockItem.quantity = quantity;
            return this;
        }

        public Builder allocatedQuantity(Quantity allocatedQuantity) {
            stockItem.allocatedQuantity = allocatedQuantity;
            return this;
        }

        public Builder expirationDate(ExpirationDate expirationDate) {
            stockItem.expirationDate = expirationDate;
            return this;
        }

        public Builder consignmentId(ConsignmentId consignmentId) {
            stockItem.consignmentId = consignmentId;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            stockItem.createdAt = createdAt;
            return this;
        }

        public Builder lastModifiedAt(LocalDateTime lastModifiedAt) {
            stockItem.lastModifiedAt = lastModifiedAt;
            return this;
        }

        public Builder version(int version) {
            stockItem.setVersion(version);
            return this;
        }

        /**
         * Builds and validates the StockItem instance.
         *
         * @return Validated StockItem instance
         * @throws IllegalArgumentException if validation fails
         */
        public StockItem build() {
            validate();
            initializeDefaults();

            if (stockItem.createdAt == null) {
                stockItem.createdAt = LocalDateTime.now();
            }
            if (stockItem.lastModifiedAt == null) {
                stockItem.lastModifiedAt = LocalDateTime.now();
            }

            // Classify on creation
            stockItem.classify();

            return consumeStockItem();
        }

        /**
         * Validates all required fields are set.
         *
         * @throws IllegalArgumentException if validation fails
         */
        private void validate() {
            if (stockItem.getId() == null) {
                throw new IllegalArgumentException("StockItemId is required");
            }
            if (stockItem.getTenantId() == null) {
                throw new IllegalArgumentException("TenantId is required");
            }
            if (stockItem.productId == null) {
                throw new IllegalArgumentException("ProductId is required");
            }
            if (stockItem.quantity == null) {
                throw new IllegalArgumentException("Quantity is required");
            }
        }

        /**
         * Initializes default values for optional fields.
         */
        private void initializeDefaults() {
            // Classification will be set by classify() method
            // LocationId can be null initially
            // ExpirationDate can be null for non-perishable items
        }

        /**
         * Consumes the stock item from the builder and returns it. Creates a new stock item instance for the next build.
         *
         * @return Built stock item
         */
        private StockItem consumeStockItem() {
            StockItem builtStockItem = stockItem;
            stockItem = new StockItem();
            return builtStockItem;
        }

        /**
         * Builds StockItem without publishing creation events. Used when reconstructing from persistence.
         *
         * @return Validated StockItem instance
         * @throws IllegalArgumentException if validation fails
         */
        public StockItem buildWithoutEvents() {
            validate();
            initializeDefaults();

            if (stockItem.createdAt == null) {
                stockItem.createdAt = LocalDateTime.now();
            }
            if (stockItem.lastModifiedAt == null) {
                stockItem.lastModifiedAt = LocalDateTime.now();
            }

            // Classify but don't publish events when loading from database
            stockItem.classify();
            stockItem.clearDomainEvents(); // Clear any events generated during classification

            return consumeStockItem();
        }
    }
}

