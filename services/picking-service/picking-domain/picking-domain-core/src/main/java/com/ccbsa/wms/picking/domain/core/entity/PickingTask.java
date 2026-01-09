package com.ccbsa.wms.picking.domain.core.entity;

import java.util.Objects;

import com.ccbsa.common.domain.valueobject.Quantity;
import com.ccbsa.wms.picking.domain.core.valueobject.LoadId;
import com.ccbsa.wms.picking.domain.core.valueobject.LocationId;
import com.ccbsa.wms.picking.domain.core.valueobject.OrderId;
import com.ccbsa.wms.picking.domain.core.valueobject.PickingTaskId;
import com.ccbsa.wms.picking.domain.core.valueobject.PickingTaskStatus;
import com.ccbsa.wms.picking.domain.core.valueobject.ProductCode;

/**
 * Entity: PickingTask
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
 */
public class PickingTask {
    private PickingTaskId id;
    private LoadId loadId;
    private OrderId orderId;
    private ProductCode productCode;
    private LocationId locationId;
    private Quantity quantity;
    private PickingTaskStatus status;
    private int sequence;

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
     * Business logic method: Updates the task status.
     *
     * @param newStatus New status
     * @throws IllegalStateException if status transition is invalid
     */
    public void updateStatus(PickingTaskStatus newStatus) {
        if (this.status == PickingTaskStatus.COMPLETED) {
            throw new IllegalStateException("Cannot update status of completed picking task");
        }
        this.status = newStatus;
    }

    /**
     * Business logic method: Marks the task as completed.
     */
    public void markAsCompleted() {
        this.status = PickingTaskStatus.COMPLETED;
    }

    // Getters

    public PickingTaskId getId() {
        return id;
    }

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

    public PickingTaskStatus getStatus() {
        return status;
    }

    public int getSequence() {
        return sequence;
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
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("PickingTask{id=%s, loadId=%s, productCode=%s, locationId=%s, sequence=%d}", id, loadId, productCode, locationId, sequence);
    }

    /**
     * Builder class for constructing PickingTask instances.
     */
    public static class Builder {
        private PickingTask task = new PickingTask();

        public Builder id(PickingTaskId id) {
            task.id = id;
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

        public PickingTask build() {
            if (task.id == null) {
                task.id = PickingTaskId.generate();
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
