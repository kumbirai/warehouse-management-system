package com.ccbsa.wms.picking.domain.core.entity;

import java.time.LocalDateTime;
import java.util.Objects;

import com.ccbsa.common.domain.AggregateRoot;
import com.ccbsa.common.domain.valueobject.Quantity;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.common.domain.valueobject.UserId;
import com.ccbsa.wms.picking.domain.core.event.PartialPickingCompletedEvent;
import com.ccbsa.wms.picking.domain.core.event.PickingTaskCompletedEvent;
import com.ccbsa.wms.picking.domain.core.exception.InvalidPickingQuantityException;
import com.ccbsa.wms.picking.domain.core.exception.PickingTaskAlreadyCompletedException;
import com.ccbsa.wms.picking.domain.core.valueobject.LoadId;
import com.ccbsa.wms.picking.domain.core.valueobject.LocationId;
import com.ccbsa.wms.picking.domain.core.valueobject.OrderId;
import com.ccbsa.wms.picking.domain.core.valueobject.PartialReason;
import com.ccbsa.wms.picking.domain.core.valueobject.PickingListId;
import com.ccbsa.wms.picking.domain.core.valueobject.PickingTaskId;
import com.ccbsa.wms.picking.domain.core.valueobject.PickingTaskStatus;
import com.ccbsa.wms.picking.domain.core.valueobject.ProductCode;

/**
 * Aggregate Root: PickingTask
 * <p>
 * Represents a picking task for a specific location/product combination within a load.
 * <p>
 * Business Rules:
 * - Task must have a load ID
 * - Task must have an order ID
 * - Task must have a product code
 * - Task must have a location ID
 * - Task must have a positive quantity
 * - Task must have a sequence number
 * - Cannot execute a task that is already completed
 * - Picked quantity cannot exceed required quantity
 * - Partial picking requires a reason
 */
public class PickingTask extends AggregateRoot<PickingTaskId> {
    private LoadId loadId;
    private OrderId orderId;
    private ProductCode productCode;
    private LocationId locationId;
    private Quantity quantity; // Required quantity
    private Quantity pickedQuantity; // Actual picked quantity
    private PickingTaskStatus status;
    private int sequence;
    private boolean isPartialPicking;
    private PartialReason partialReason;
    private UserId pickedByUserId;
    private LocalDateTime pickedAt;

    /**
     * Private constructor for builder pattern. Prevents direct instantiation.
     */
    private PickingTask() {
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
     * Business logic method: Executes the picking task with full quantity.
     * <p>
     * Business Rules:
     * - Task must be in PENDING status
     * - Picked quantity must be positive
     * - Picked quantity cannot exceed required quantity
     * - If picked quantity is less than required, status becomes PARTIALLY_COMPLETED
     * - If picked quantity equals required, status becomes COMPLETED
     * - Publishes PickingTaskCompletedEvent or PartialPickingCompletedEvent
     *
     * @param pickedQuantity Picked quantity
     * @param pickedByUserId User ID who picked the stock
     * @param pickingListId  Picking list ID (for event)
     * @param tenantId       Tenant ID (for event)
     * @throws PickingTaskAlreadyCompletedException if task is already completed
     * @throws InvalidPickingQuantityException      if picked quantity is invalid
     */
    public void execute(Quantity pickedQuantity, UserId pickedByUserId, PickingListId pickingListId, TenantId tenantId) {
        validateExecutionPreconditions();
        validatePickedQuantity(pickedQuantity);

        this.pickedQuantity = pickedQuantity;
        this.pickedByUserId = pickedByUserId;
        this.pickedAt = LocalDateTime.now();

        // Determine if partial or full picking
        if (pickedQuantity.getValue() < quantity.getValue()) {
            this.isPartialPicking = true;
            this.status = PickingTaskStatus.PARTIALLY_COMPLETED;
            // Note: partialReason should be set via executePartial() method
            // For execute() method without reason, use default reason
            if (this.partialReason == null) {
                this.partialReason = PartialReason.of("Partial picking - no reason provided");
            }
        } else {
            this.isPartialPicking = false;
            this.status = PickingTaskStatus.COMPLETED;
        }

        // Publish appropriate event
        if (this.isPartialPicking) {
            addDomainEvent(new PartialPickingCompletedEvent(this.getId().getValueAsString(), tenantId, pickingListId, this.loadId, this.orderId, this.productCode, this.locationId,
                    this.quantity.getValue(), this.pickedQuantity.getValue(), this.partialReason.getValue(),
                    this.pickedByUserId != null ? this.pickedByUserId.getValue() : null));
        } else {
            addDomainEvent(new PickingTaskCompletedEvent(this.getId().getValueAsString(), tenantId, pickingListId, this.loadId, this.orderId, this.productCode, this.locationId,
                    this.pickedQuantity.getValue(), this.pickedByUserId != null ? this.pickedByUserId.getValue() : null));
        }
    }

    private void validateExecutionPreconditions() {
        if (this.status == PickingTaskStatus.COMPLETED || this.status == PickingTaskStatus.PARTIALLY_COMPLETED) {
            throw new PickingTaskAlreadyCompletedException("Cannot execute picking task that is already completed: " + this.getId().getValueAsString());
        }
        if (this.status != PickingTaskStatus.PENDING && this.status != PickingTaskStatus.IN_PROGRESS) {
            throw new IllegalStateException("Cannot execute picking task in status: " + this.status + ". Task must be PENDING or IN_PROGRESS.");
        }
    }

    private void validatePickedQuantity(Quantity pickedQuantity) {
        if (pickedQuantity == null) {
            throw new InvalidPickingQuantityException("Picked quantity cannot be null");
        }
        if (!pickedQuantity.isPositive()) {
            throw new InvalidPickingQuantityException("Picked quantity must be greater than zero");
        }
        if (pickedQuantity.getValue() > quantity.getValue()) {
            throw new InvalidPickingQuantityException(String.format("Picked quantity (%d) cannot exceed required quantity (%d)", pickedQuantity.getValue(), quantity.getValue()));
        }
    }

    /**
     * Business logic method: Executes the picking task with partial quantity.
     * <p>
     * Business Rules:
     * - Task must be in PENDING status
     * - Picked quantity must be positive
     * - Picked quantity cannot exceed required quantity
     * - Partial reason is required
     * - Status becomes PARTIALLY_COMPLETED
     * - Publishes PartialPickingCompletedEvent
     *
     * @param pickedQuantity Picked quantity
     * @param partialReason  Reason for partial picking
     * @param pickedByUserId User ID who picked the stock
     * @param pickingListId  Picking list ID (for event)
     * @param tenantId       Tenant ID (for event)
     * @throws PickingTaskAlreadyCompletedException if task is already completed
     * @throws InvalidPickingQuantityException      if picked quantity is invalid
     * @throws IllegalArgumentException             if partial reason is null or empty
     */
    public void executePartial(Quantity pickedQuantity, PartialReason partialReason, UserId pickedByUserId, PickingListId pickingListId, TenantId tenantId) {
        validateExecutionPreconditions();
        validatePickedQuantity(pickedQuantity);
        if (partialReason == null) {
            throw new IllegalArgumentException("Partial reason is required for partial picking");
        }

        this.pickedQuantity = pickedQuantity;
        this.isPartialPicking = true;
        this.partialReason = partialReason;
        this.pickedByUserId = pickedByUserId;
        this.pickedAt = LocalDateTime.now();
        this.status = PickingTaskStatus.PARTIALLY_COMPLETED;

        addDomainEvent(new PartialPickingCompletedEvent(this.getId().getValueAsString(), tenantId, pickingListId, this.loadId, this.orderId, this.productCode, this.locationId,
                this.quantity.getValue(), this.pickedQuantity.getValue(), this.partialReason.getValue(), this.pickedByUserId != null ? this.pickedByUserId.getValue() : null));
    }

    /**
     * Business logic method: Updates the task status.
     *
     * @param newStatus New status
     * @throws IllegalStateException if status transition is invalid
     */
    public void updateStatus(PickingTaskStatus newStatus) {
        if (this.status == PickingTaskStatus.COMPLETED || this.status == PickingTaskStatus.PARTIALLY_COMPLETED) {
            throw new IllegalStateException("Cannot update status of completed picking task");
        }
        this.status = newStatus;
    }

    /**
     * Business logic method: Marks the task as completed.
     * <p>
     * Note: Prefer using execute() method which includes business logic and event publishing.
     */
    public void markAsCompleted() {
        this.status = PickingTaskStatus.COMPLETED;
    }

    /**
     * Query method: Checks if task is completed.
     *
     * @return true if task is completed
     */
    public boolean isCompleted() {
        return this.status == PickingTaskStatus.COMPLETED;
    }

    /**
     * Query method: Checks if task is partially completed.
     *
     * @return true if task is partially completed
     */
    public boolean isPartiallyCompleted() {
        return this.status == PickingTaskStatus.PARTIALLY_COMPLETED;
    }

    // Getters

    public LoadId getLoadId() {
        return loadId;
    }

    public OrderId getOrderId() {
        return orderId;
    }

    public ProductCode getProductCode() {
        return productCode;
    }

    public LocationId getLocationId() {
        return locationId;
    }

    public Quantity getQuantity() {
        return quantity;
    }

    public Quantity getPickedQuantity() {
        return pickedQuantity;
    }

    public PickingTaskStatus getStatus() {
        return status;
    }

    public int getSequence() {
        return sequence;
    }

    public boolean isPartialPicking() {
        return isPartialPicking;
    }

    public PartialReason getPartialReason() {
        return partialReason;
    }

    /**
     * Returns the partial reason as a string value, or null if not set.
     *
     * @return Partial reason string value or null
     */
    public String getPartialReasonValue() {
        return partialReason != null ? partialReason.getValue() : null;
    }

    public UserId getPickedByUserId() {
        return pickedByUserId;
    }

    public LocalDateTime getPickedAt() {
        return pickedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PickingTask that = (PickingTask) o;
        return Objects.equals(getId(), that.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }

    @Override
    public String toString() {
        return String.format("PickingTask{id=%s, loadId=%s, productCode=%s, locationId=%s, sequence=%d, status=%s}", getId(), loadId, productCode, locationId, sequence, status);
    }

    /**
     * Builder class for constructing PickingTask instances.
     */
    public static class Builder {
        private PickingTask task = new PickingTask();

        public Builder id(PickingTaskId id) {
            task.setId(id);
            return this;
        }

        public Builder loadId(LoadId loadId) {
            task.loadId = loadId;
            return this;
        }

        public Builder orderId(OrderId orderId) {
            task.orderId = orderId;
            return this;
        }

        public Builder productCode(ProductCode productCode) {
            task.productCode = productCode;
            return this;
        }

        public Builder locationId(LocationId locationId) {
            task.locationId = locationId;
            return this;
        }

        public Builder quantity(Quantity quantity) {
            task.quantity = quantity;
            return this;
        }

        public Builder status(PickingTaskStatus status) {
            task.status = status;
            return this;
        }

        public Builder sequence(int sequence) {
            task.sequence = sequence;
            return this;
        }

        public Builder pickedQuantity(Quantity pickedQuantity) {
            task.pickedQuantity = pickedQuantity;
            return this;
        }

        public Builder isPartialPicking(boolean isPartialPicking) {
            task.isPartialPicking = isPartialPicking;
            return this;
        }

        public Builder partialReason(PartialReason partialReason) {
            task.partialReason = partialReason;
            return this;
        }

        public Builder partialReason(String partialReason) {
            if (partialReason != null && !partialReason.trim().isEmpty()) {
                task.partialReason = PartialReason.of(partialReason);
            }
            return this;
        }

        public Builder pickedByUserId(UserId pickedByUserId) {
            task.pickedByUserId = pickedByUserId;
            return this;
        }

        public Builder pickedAt(LocalDateTime pickedAt) {
            task.pickedAt = pickedAt;
            return this;
        }

        public Builder version(int version) {
            task.setVersion(version);
            return this;
        }

        /**
         * Builds PickingTask without publishing creation events. Used when reconstructing from persistence.
         *
         * @return Validated PickingTask instance
         */
        public PickingTask buildWithoutEvents() {
            PickingTask builtTask = build();
            builtTask.clearDomainEvents();
            return builtTask;
        }

        public PickingTask build() {
            if (task.getId() == null) {
                task.setId(PickingTaskId.generate());
            }
            if (task.status == null) {
                task.status = PickingTaskStatus.PENDING;
            }
            if (task.loadId == null) {
                throw new IllegalStateException("Picking task must have a load ID");
            }
            if (task.orderId == null) {
                throw new IllegalStateException("Picking task must have an order ID");
            }
            if (task.productCode == null) {
                throw new IllegalStateException("Picking task must have a product code");
            }
            if (task.locationId == null) {
                throw new IllegalStateException("Picking task must have a location ID");
            }
            if (task.quantity == null || !task.quantity.isPositive()) {
                throw new IllegalStateException("Picking task must have a positive quantity");
            }
            if (task.sequence < 0) {
                throw new IllegalStateException("Picking task sequence must be non-negative");
            }
            return task;
        }
    }
}
