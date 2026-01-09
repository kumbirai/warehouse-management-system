package com.ccbsa.wms.picking.domain.core.entity;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.ccbsa.common.domain.valueobject.CustomerInfo;
import com.ccbsa.common.domain.valueobject.OrderNumber;
import com.ccbsa.common.domain.valueobject.Priority;
import com.ccbsa.common.domain.valueobject.Quantity;
import com.ccbsa.wms.picking.domain.core.valueobject.OrderId;
import com.ccbsa.wms.picking.domain.core.valueobject.OrderStatus;

/**
 * Entity: Order
 * <p>
 * Represents an order within a load. Contains order number, customer information, priority, and line items.
 * <p>
 * Business Rules:
 * - Order must have an order number
 * - Order must have customer information
 * - Order must have at least one line item
 * - Order status transitions must be valid
 */
public class Order {
    private OrderId id;
    private OrderNumber orderNumber;
    private CustomerInfo customerInfo;
    private Priority priority;
    private List<OrderLineItem> lineItems;
    private OrderStatus status;
    private ZonedDateTime createdAt;
    private ZonedDateTime completedAt;

    /**
     * Private constructor for builder pattern. Prevents direct instantiation.
     */
    private Order() {
        this.lineItems = new ArrayList<>();
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
     * Business logic method: Validates the order.
     *
     * @throws IllegalStateException if validation fails
     */
    public void validateOrder() {
        if (orderNumber == null) {
            throw new IllegalStateException("Order must have an order number");
        }
        if (customerInfo == null) {
            throw new IllegalStateException("Order must have customer information");
        }
        if (lineItems == null || lineItems.isEmpty()) {
            throw new IllegalStateException("Order must have at least one line item");
        }
        lineItems.forEach(OrderLineItem::validateLineItem);
    }

    /**
     * Business logic method: Updates the order status.
     *
     * @param newStatus New status
     * @throws IllegalStateException if status transition is invalid
     */
    public void updateStatus(OrderStatus newStatus) {
        if (this.status == OrderStatus.COMPLETED) {
            throw new IllegalStateException("Cannot update status of completed order");
        }
        this.status = newStatus;
        if (newStatus == OrderStatus.COMPLETED) {
            this.completedAt = ZonedDateTime.now();
        }
    }

    /**
     * Query method: Calculates total quantity across all line items.
     *
     * @return Total quantity
     */
    public Quantity getTotalQuantity() {
        return lineItems.stream().map(OrderLineItem::getQuantity).reduce(Quantity.of(0), Quantity::add);
    }

    /**
     * Query method: Gets the number of line items.
     *
     * @return Line item count
     */
    public int getLineItemCount() {
        return lineItems.size();
    }

    // Getters

    public OrderId getId() {
        return id;
    }

    public OrderNumber getOrderNumber() {
        return orderNumber;
    }

    public CustomerInfo getCustomerInfo() {
        return customerInfo;
    }

    public Priority getPriority() {
        return priority;
    }

    public List<OrderLineItem> getLineItems() {
        return Collections.unmodifiableList(lineItems);
    }

    public OrderStatus getStatus() {
        return status;
    }

    public ZonedDateTime getCreatedAt() {
        return createdAt;
    }

    public ZonedDateTime getCompletedAt() {
        return completedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Order order = (Order) o;
        return Objects.equals(id, order.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("Order{id=%s, orderNumber=%s, status=%s}", id, orderNumber, status);
    }

    /**
     * Builder class for constructing Order instances.
     */
    public static class Builder {
        private Order order = new Order();

        public Builder id(OrderId id) {
            order.id = id;
            return this;
        }

        public Builder orderNumber(OrderNumber orderNumber) {
            order.orderNumber = orderNumber;
            return this;
        }

        public Builder customerInfo(CustomerInfo customerInfo) {
            order.customerInfo = customerInfo;
            return this;
        }

        public Builder priority(Priority priority) {
            order.priority = priority;
            return this;
        }

        public Builder lineItems(List<OrderLineItem> lineItems) {
            if (lineItems != null) {
                order.lineItems = new ArrayList<>(lineItems);
            }
            return this;
        }

        public Builder lineItem(OrderLineItem lineItem) {
            if (lineItem != null) {
                order.lineItems.add(lineItem);
            }
            return this;
        }

        public Builder status(OrderStatus status) {
            order.status = status;
            return this;
        }

        public Builder createdAt(ZonedDateTime createdAt) {
            order.createdAt = createdAt;
            return this;
        }

        public Builder completedAt(ZonedDateTime completedAt) {
            order.completedAt = completedAt;
            return this;
        }

        public Order build() {
            if (order.id == null) {
                order.id = OrderId.generate();
            }
            if (order.status == null) {
                order.status = OrderStatus.PENDING;
            }
            if (order.priority == null) {
                order.priority = Priority.NORMAL;
            }
            if (order.createdAt == null) {
                order.createdAt = ZonedDateTime.now();
            }
            order.validateOrder();
            return order;
        }
    }
}
