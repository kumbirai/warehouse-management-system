package com.ccbsa.wms.stock.domain.core.entity;

import java.time.LocalDateTime;

import com.ccbsa.common.domain.TenantAwareAggregateRoot;
import com.ccbsa.common.domain.valueobject.AllocationType;
import com.ccbsa.common.domain.valueobject.ProductId;
import com.ccbsa.common.domain.valueobject.Quantity;
import com.ccbsa.common.domain.valueobject.StockItemId;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.common.domain.valueobject.UserId;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;
import com.ccbsa.wms.stock.domain.core.event.StockAllocatedEvent;
import com.ccbsa.wms.stock.domain.core.event.StockAllocationReleasedEvent;
import com.ccbsa.wms.stock.domain.core.valueobject.AllocationStatus;
import com.ccbsa.wms.stock.domain.core.valueobject.Notes;
import com.ccbsa.wms.stock.domain.core.valueobject.ReferenceId;
import com.ccbsa.wms.stock.domain.core.valueobject.StockAllocationId;

/**
 * Aggregate Root: StockAllocation
 * <p>
 * Represents a stock allocation for picking orders or reservations.
 * Tracks allocated quantity separately from available quantity.
 * <p>
 * Business Rules:
 * - Allocation quantity cannot exceed available stock (validated at application service)
 * - Allocation must reference valid stock item
 * - Allocation can be released if not yet picked
 * - FEFO allocation prioritizes earliest expiring stock (handled at application service)
 */
public class StockAllocation extends TenantAwareAggregateRoot<StockAllocationId> {

    private ProductId productId;
    private LocationId locationId; // Optional - null for product-wide allocation
    private StockItemId stockItemId; // Reference to specific stock item
    private Quantity quantity;
    private AllocationType allocationType;
    private ReferenceId referenceId; // Order ID, picking list ID, etc.
    private AllocationStatus status;
    private LocalDateTime allocatedAt;
    private LocalDateTime releasedAt;
    private UserId allocatedBy;
    private Notes notes;

    /**
     * Private constructor for builder pattern. Prevents direct instantiation.
     */
    private StockAllocation() {
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
     * Business logic method: Allocates stock for picking order or reservation.
     * <p>
     * Business Rules:
     * - Sets allocation status to ALLOCATED
     * - Records allocation timestamp
     * - Publishes StockAllocatedEvent
     *
     * @throws IllegalStateException if allocation already exists and is not released
     */
    public void allocate() {
        if (this.status != null && this.status != AllocationStatus.RELEASED) {
            throw new IllegalStateException("Allocation already exists and is not released");
        }

        this.status = AllocationStatus.ALLOCATED;
        if (this.allocatedAt == null) {
            this.allocatedAt = LocalDateTime.now();
        }

        // Publish domain event
        addDomainEvent(new StockAllocatedEvent(this.getId(), this.getTenantId(), this.productId, this.locationId, this.stockItemId, this.quantity, this.allocationType,
                this.referenceId != null ? this.referenceId.getValue() : null, this.allocatedBy, this.allocatedAt));
    }

    /**
     * Business logic method: Releases allocation.
     * <p>
     * Business Rules:
     * - Only ALLOCATED allocations can be released
     * - Sets status to RELEASED
     * - Records release timestamp
     * - Publishes StockAllocationReleasedEvent
     *
     * @throws IllegalStateException if allocation cannot be released
     */
    public void release() {
        if (this.status != AllocationStatus.ALLOCATED) {
            throw new IllegalStateException(String.format("Cannot release allocation in status: %s", this.status));
        }

        this.status = AllocationStatus.RELEASED;
        this.releasedAt = LocalDateTime.now();

        // Publish domain event
        addDomainEvent(new StockAllocationReleasedEvent(this.getId(), this.getTenantId(), this.productId, this.locationId, this.stockItemId, this.quantity));
    }

    /**
     * Business logic method: Marks allocation as picked.
     * <p>
     * Business Rules:
     * - Only ALLOCATED allocations can be marked as picked
     * - Sets status to PICKED
     *
     * @throws IllegalStateException if allocation cannot be marked as picked
     */
    public void markAsPicked() {
        if (this.status != AllocationStatus.ALLOCATED) {
            throw new IllegalStateException(String.format("Cannot mark allocation as picked in status: %s", this.status));
        }

        this.status = AllocationStatus.PICKED;
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

    public Quantity getQuantity() {
        return quantity;
    }

    public AllocationType getAllocationType() {
        return allocationType;
    }

    public ReferenceId getReferenceId() {
        return referenceId;
    }

    public AllocationStatus getStatus() {
        return status;
    }

    public LocalDateTime getAllocatedAt() {
        return allocatedAt;
    }

    public LocalDateTime getReleasedAt() {
        return releasedAt;
    }

    public UserId getAllocatedBy() {
        return allocatedBy;
    }

    public Notes getNotes() {
        return notes;
    }

    /**
     * Builder class for constructing StockAllocation instances. Ensures all required fields are set and validated.
     */
    public static class Builder {
        private StockAllocation allocation = new StockAllocation();

        public Builder stockAllocationId(StockAllocationId id) {
            allocation.setId(id);
            return this;
        }

        public Builder tenantId(TenantId tenantId) {
            allocation.setTenantId(tenantId);
            return this;
        }

        public Builder productId(ProductId productId) {
            allocation.productId = productId;
            return this;
        }

        public Builder locationId(LocationId locationId) {
            allocation.locationId = locationId;
            return this;
        }

        public Builder stockItemId(StockItemId stockItemId) {
            allocation.stockItemId = stockItemId;
            return this;
        }

        public Builder quantity(Quantity quantity) {
            allocation.quantity = quantity;
            return this;
        }

        public Builder allocationType(AllocationType allocationType) {
            allocation.allocationType = allocationType;
            return this;
        }

        public Builder referenceId(ReferenceId referenceId) {
            allocation.referenceId = referenceId;
            return this;
        }

        public Builder referenceId(String referenceId) {
            if (referenceId == null || referenceId.trim().isEmpty()) {
                throw new IllegalArgumentException("ReferenceId cannot be null or empty");
            }
            allocation.referenceId = ReferenceId.of(referenceId);
            return this;
        }

        public Builder allocatedBy(UserId allocatedBy) {
            allocation.allocatedBy = allocatedBy;
            return this;
        }

        public Builder notes(Notes notes) {
            allocation.notes = notes;
            return this;
        }

        public Builder notes(String notes) {
            allocation.notes = Notes.ofNullable(notes);
            return this;
        }

        public Builder status(AllocationStatus status) {
            allocation.status = status;
            return this;
        }

        public Builder allocatedAt(LocalDateTime allocatedAt) {
            allocation.allocatedAt = allocatedAt;
            return this;
        }

        public Builder releasedAt(LocalDateTime releasedAt) {
            allocation.releasedAt = releasedAt;
            return this;
        }

        /**
         * Sets the version (for loading from database).
         *
         * @param version Version number
         * @return Builder instance
         */
        public Builder version(int version) {
            allocation.setVersion(version);
            return this;
        }

        /**
         * Sets the version as Long (for loading from database).
         *
         * @param version Version number
         * @return Builder instance
         */
        public Builder version(Long version) {
            allocation.setVersion(version != null ? version.intValue() : 0);
            return this;
        }

        /**
         * Builds and validates the StockAllocation instance.
         * <p>
         * Does NOT call allocate() - that must be called explicitly after building.
         *
         * @return Validated StockAllocation instance
         * @throws IllegalArgumentException if validation fails
         */
        public StockAllocation build() {
            validate();
            initializeDefaults();
            return consumeAllocation();
        }

        /**
         * Validates all required fields are set.
         *
         * @throws IllegalArgumentException if validation fails
         */
        private void validate() {
            if (allocation.getId() == null) {
                throw new IllegalArgumentException("StockAllocationId is required");
            }
            if (allocation.getTenantId() == null) {
                throw new IllegalArgumentException("TenantId is required");
            }
            if (allocation.productId == null) {
                throw new IllegalArgumentException("ProductId is required");
            }
            if (allocation.stockItemId == null) {
                throw new IllegalArgumentException("StockItemId is required");
            }
            if (allocation.quantity == null || !allocation.quantity.isPositive()) {
                throw new IllegalArgumentException("Quantity must be positive");
            }
            if (allocation.allocationType == null) {
                throw new IllegalArgumentException("AllocationType is required");
            }
            if (allocation.allocationType == AllocationType.PICKING_ORDER && allocation.referenceId == null) {
                throw new IllegalArgumentException("ReferenceId is required for PICKING_ORDER allocation");
            }
            if (allocation.allocatedBy == null) {
                throw new IllegalArgumentException("AllocatedBy is required");
            }
        }

        /**
         * Initializes default values for optional fields.
         */
        private void initializeDefaults() {
            if (allocation.status == null) {
                allocation.status = AllocationStatus.ALLOCATED;
            }
            if (allocation.allocatedAt == null) {
                allocation.allocatedAt = LocalDateTime.now();
            }
        }

        /**
         * Consumes the allocation from the builder and returns it. Creates a new allocation instance for the next build.
         *
         * @return Built allocation
         */
        private StockAllocation consumeAllocation() {
            StockAllocation builtAllocation = allocation;
            allocation = new StockAllocation();
            return builtAllocation;
        }

        /**
         * Builds StockAllocation without publishing events. Used when reconstructing from database.
         *
         * @return Validated StockAllocation instance
         * @throws IllegalArgumentException if validation fails
         */
        public StockAllocation buildWithoutEvents() {
            validate();
            initializeDefaults();
            // Do not publish events when loading from database
            return consumeAllocation();
        }
    }
}

