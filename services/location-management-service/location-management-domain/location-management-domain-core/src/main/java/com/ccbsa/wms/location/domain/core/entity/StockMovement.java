package com.ccbsa.wms.location.domain.core.entity;

import java.time.LocalDateTime;

import com.ccbsa.common.domain.TenantAwareAggregateRoot;
import com.ccbsa.common.domain.valueobject.MovementReason;
import com.ccbsa.common.domain.valueobject.ProductId;
import com.ccbsa.common.domain.valueobject.Quantity;
import com.ccbsa.common.domain.valueobject.StockItemId;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.common.domain.valueobject.UserId;
import com.ccbsa.wms.location.domain.core.event.StockMovementCancelledEvent;
import com.ccbsa.wms.location.domain.core.event.StockMovementCompletedEvent;
import com.ccbsa.wms.location.domain.core.event.StockMovementInitiatedEvent;
import com.ccbsa.wms.location.domain.core.valueobject.CancellationReason;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;
import com.ccbsa.wms.location.domain.core.valueobject.MovementStatus;
import com.ccbsa.wms.location.domain.core.valueobject.MovementType;
import com.ccbsa.wms.location.domain.core.valueobject.StockMovementId;

/**
 * Aggregate Root: StockMovement
 * <p>
 * Represents a stock movement from one location to another.
 * Maintains complete audit trail of stock movements.
 * <p>
 * Business Rules:
 * - Movement must have valid source and destination locations
 * - Source and destination locations must be different
 * - Movement quantity must be positive
 * - Movement can only be completed if status is INITIATED
 * - Movement can only be cancelled if status is INITIATED
 * - Completed movements cannot be modified
 */
public class StockMovement extends TenantAwareAggregateRoot<StockMovementId> {

    private StockItemId stockItemId;
    private ProductId productId;
    private LocationId sourceLocationId;
    private LocationId destinationLocationId;
    private Quantity quantity;
    private MovementType movementType;
    private MovementReason reason;
    private MovementStatus status;

    // Audit fields
    private UserId initiatedBy;
    private LocalDateTime initiatedAt;
    private UserId completedBy;
    private LocalDateTime completedAt;
    private UserId cancelledBy;
    private LocalDateTime cancelledAt;
    private CancellationReason cancellationReason;

    /**
     * Private constructor for builder pattern. Prevents direct instantiation.
     */
    private StockMovement() {
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
     * Business logic method: Complete the movement.
     * <p>
     * Business Rules:
     * - Movement can only be completed if status is INITIATED
     * - Updates status to COMPLETED
     * - Records completion timestamp and user
     * - Publishes StockMovementCompletedEvent
     *
     * @param completedBy User who completed the movement
     * @throws IllegalStateException if movement cannot be completed
     */
    public void complete(UserId completedBy) {
        if (this.status != MovementStatus.INITIATED) {
            throw new IllegalStateException("Can only complete initiated movements. Current status: " + this.status);
        }

        if (completedBy == null) {
            throw new IllegalArgumentException("CompletedBy cannot be null");
        }

        this.status = MovementStatus.COMPLETED;
        this.completedBy = completedBy;
        this.completedAt = LocalDateTime.now();

        // Validate stockItemId is set (defensive check - builder should have validated this)
        if (this.stockItemId == null) {
            throw new IllegalStateException("StockItemId must be set before completing movement");
        }

        // Publish completion event
        addDomainEvent(new StockMovementCompletedEvent(this.getId(), this.getTenantId(), this.stockItemId.getValueAsString(), this.productId, this.sourceLocationId,
                this.destinationLocationId, this.quantity, this.movementType, this.reason, this.initiatedBy, this.initiatedAt, this.completedBy, this.completedAt));
    }

    /**
     * Business logic method: Cancel the movement.
     * <p>
     * Business Rules:
     * - Movement can only be cancelled if status is INITIATED
     * - Cancellation reason is required
     * - Updates status to CANCELLED
     * - Records cancellation timestamp and user
     * - Publishes StockMovementCancelledEvent
     *
     * @param cancelledBy        User who cancelled the movement
     * @param cancellationReason Reason for cancellation (required)
     * @throws IllegalStateException    if movement cannot be cancelled
     * @throws IllegalArgumentException if cancellation reason is null or empty
     */
    public void cancel(UserId cancelledBy, CancellationReason cancellationReason) {
        if (this.status != MovementStatus.INITIATED) {
            throw new IllegalStateException("Can only cancel initiated movements. Current status: " + this.status);
        }

        if (cancelledBy == null) {
            throw new IllegalArgumentException("CancelledBy cannot be null");
        }

        if (cancellationReason == null) {
            throw new IllegalArgumentException("CancellationReason cannot be null");
        }

        this.status = MovementStatus.CANCELLED;
        this.cancelledBy = cancelledBy;
        this.cancelledAt = LocalDateTime.now();
        this.cancellationReason = cancellationReason;

        // Validate stockItemId is set (defensive check - builder should have validated this)
        if (this.stockItemId == null) {
            throw new IllegalStateException("StockItemId must be set before cancelling movement");
        }

        // Publish cancellation event
        addDomainEvent(new StockMovementCancelledEvent(this.getId(), this.getTenantId(), this.stockItemId.getValueAsString(), this.sourceLocationId, this.destinationLocationId,
                this.cancelledBy, this.cancelledAt, this.cancellationReason.getValue()));
    }

    // Getters (read-only access)

    public StockItemId getStockItemId() {
        return stockItemId;
    }

    public ProductId getProductId() {
        return productId;
    }

    public LocationId getSourceLocationId() {
        return sourceLocationId;
    }

    public LocationId getDestinationLocationId() {
        return destinationLocationId;
    }

    public Quantity getQuantity() {
        return quantity;
    }

    public MovementType getMovementType() {
        return movementType;
    }

    public MovementReason getReason() {
        return reason;
    }

    public MovementStatus getStatus() {
        return status;
    }

    public UserId getInitiatedBy() {
        return initiatedBy;
    }

    public LocalDateTime getInitiatedAt() {
        return initiatedAt;
    }

    public UserId getCompletedBy() {
        return completedBy;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public UserId getCancelledBy() {
        return cancelledBy;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    public CancellationReason getCancellationReason() {
        return cancellationReason;
    }

    /**
     * Builder class for constructing StockMovement instances. Ensures all required fields are set and validated.
     */
    public static class Builder {
        private StockMovement movement = new StockMovement();

        public Builder stockMovementId(StockMovementId id) {
            movement.setId(id);
            return this;
        }

        public Builder tenantId(TenantId tenantId) {
            movement.setTenantId(tenantId);
            return this;
        }

        public Builder stockItemId(StockItemId stockItemId) {
            if (stockItemId == null) {
                throw new IllegalArgumentException("StockItemId cannot be null");
            }
            movement.stockItemId = stockItemId;
            return this;
        }

        public Builder stockItemId(String stockItemId) {
            if (stockItemId == null || stockItemId.trim().isEmpty()) {
                throw new IllegalArgumentException("StockItemId cannot be null or empty");
            }
            movement.stockItemId = StockItemId.of(stockItemId);
            return this;
        }

        public Builder productId(ProductId productId) {
            movement.productId = productId;
            return this;
        }

        public Builder sourceLocationId(LocationId sourceLocationId) {
            movement.sourceLocationId = sourceLocationId;
            return this;
        }

        public Builder destinationLocationId(LocationId destinationLocationId) {
            movement.destinationLocationId = destinationLocationId;
            return this;
        }

        public Builder quantity(Quantity quantity) {
            movement.quantity = quantity;
            return this;
        }

        public Builder movementType(MovementType movementType) {
            movement.movementType = movementType;
            return this;
        }

        public Builder reason(MovementReason reason) {
            movement.reason = reason;
            return this;
        }

        public Builder initiatedBy(UserId initiatedBy) {
            movement.initiatedBy = initiatedBy;
            return this;
        }

        public Builder status(MovementStatus status) {
            movement.status = status;
            return this;
        }

        public Builder initiatedAt(LocalDateTime initiatedAt) {
            movement.initiatedAt = initiatedAt;
            return this;
        }

        public Builder completedBy(UserId completedBy) {
            movement.completedBy = completedBy;
            return this;
        }

        public Builder completedAt(LocalDateTime completedAt) {
            movement.completedAt = completedAt;
            return this;
        }

        public Builder cancelledBy(UserId cancelledBy) {
            movement.cancelledBy = cancelledBy;
            return this;
        }

        public Builder cancelledAt(LocalDateTime cancelledAt) {
            movement.cancelledAt = cancelledAt;
            return this;
        }

        public Builder cancellationReason(CancellationReason cancellationReason) {
            movement.cancellationReason = cancellationReason;
            return this;
        }

        public Builder cancellationReason(String cancellationReason) {
            if (cancellationReason == null || cancellationReason.trim().isEmpty()) {
                throw new IllegalArgumentException("CancellationReason cannot be null or empty");
            }
            movement.cancellationReason = CancellationReason.of(cancellationReason);
            return this;
        }

        /**
         * Sets the version (for loading from database).
         *
         * @param version Version number
         * @return Builder instance
         */
        public Builder version(int version) {
            movement.setVersion(version);
            return this;
        }

        /**
         * Sets the version as Long (for loading from database).
         *
         * @param version Version number
         * @return Builder instance
         */
        public Builder version(Long version) {
            movement.setVersion(version != null ? version.intValue() : 0);
            return this;
        }

        /**
         * Builds and validates the StockMovement instance.
         * <p>
         * Sets status to INITIATED and publishes StockMovementInitiatedEvent.
         *
         * @return Validated StockMovement instance
         * @throws IllegalArgumentException if validation fails
         */
        public StockMovement build() {
            validate();
            movement.status = MovementStatus.INITIATED;
            if (movement.initiatedAt == null) {
                movement.initiatedAt = LocalDateTime.now();
            }

            // Publish initiation event only if this is a new movement (no version set)
            if (movement.getVersion() == 0) {
                // Validate stockItemId is set (validate() should have checked this, but defensive check)
                if (movement.stockItemId == null) {
                    throw new IllegalStateException("StockItemId must be set before building movement");
                }
                movement.addDomainEvent(new StockMovementInitiatedEvent(movement.getId(), movement.getTenantId(), movement.stockItemId.getValueAsString(), movement.productId,
                        movement.sourceLocationId, movement.destinationLocationId, movement.quantity, movement.movementType, movement.reason, movement.initiatedBy,
                        movement.initiatedAt));
            }

            return consumeMovement();
        }

        /**
         * Validates all required fields are set.
         *
         * @throws IllegalArgumentException if validation fails
         */
        private void validate() {
            if (movement.getId() == null) {
                throw new IllegalArgumentException("StockMovementId is required");
            }
            if (movement.getTenantId() == null) {
                throw new IllegalArgumentException("TenantId is required");
            }
            if (movement.stockItemId == null) {
                throw new IllegalArgumentException("StockItemId is required");
            }
            if (movement.productId == null) {
                throw new IllegalArgumentException("ProductId is required");
            }
            if (movement.sourceLocationId == null) {
                throw new IllegalArgumentException("Source LocationId is required");
            }
            if (movement.destinationLocationId == null) {
                throw new IllegalArgumentException("Destination LocationId is required");
            }
            if (movement.sourceLocationId.equals(movement.destinationLocationId)) {
                throw new IllegalArgumentException("Source and destination locations must be different");
            }
            if (movement.quantity == null || !movement.quantity.isPositive()) {
                throw new IllegalArgumentException("Quantity must be positive");
            }
            if (movement.movementType == null) {
                throw new IllegalArgumentException("MovementType is required");
            }
            if (movement.reason == null) {
                throw new IllegalArgumentException("MovementReason is required");
            }
            if (movement.initiatedBy == null) {
                throw new IllegalArgumentException("InitiatedBy is required");
            }
        }

        /**
         * Consumes the movement from the builder and returns it. Creates a new movement instance for the next build.
         *
         * @return Built movement
         */
        private StockMovement consumeMovement() {
            StockMovement builtMovement = movement;
            movement = new StockMovement();
            return builtMovement;
        }

        /**
         * Builds StockMovement without publishing events. Used when reconstructing from database.
         *
         * @return Validated StockMovement instance
         * @throws IllegalArgumentException if validation fails
         */
        public StockMovement buildWithoutEvents() {
            validate();
            if (movement.status == null) {
                movement.status = MovementStatus.INITIATED;
            }
            if (movement.initiatedAt == null) {
                movement.initiatedAt = LocalDateTime.now();
            }
            // Do not publish events when loading from database
            return consumeMovement();
        }
    }
}

