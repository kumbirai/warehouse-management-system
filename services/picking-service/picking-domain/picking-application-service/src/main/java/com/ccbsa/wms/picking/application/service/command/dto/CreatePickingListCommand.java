package com.ccbsa.wms.picking.application.service.command.dto;

import java.util.Collections;
import java.util.List;

import com.ccbsa.common.domain.valueobject.TenantId;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Builder;
import lombok.Getter;

/**
 * Command DTO: CreatePickingListCommand
 * <p>
 * Command object for manually creating a picking list.
 */
@Getter
@Builder
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Defensive copies are created in constructor and getter returns defensive copy")
public final class CreatePickingListCommand {
    private final TenantId tenantId;
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Defensive copies are created in constructor")
    private final List<LoadCommand> loads;
    private final String notes;

    public CreatePickingListCommand(TenantId tenantId, List<LoadCommand> loads, String notes) {
        if (tenantId == null) {
            throw new IllegalArgumentException("TenantId is required");
        }
        if (loads == null || loads.isEmpty()) {
            throw new IllegalArgumentException("At least one load is required");
        }
        this.tenantId = tenantId;
        // Create defensive copy (loads is already validated as non-null above)
        this.loads = List.copyOf(loads);
        this.notes = notes;
    }

    /**
     * Returns a defensive copy of the loads list to prevent external modification.
     *
     * @return unmodifiable copy of the loads list
     */
    public List<LoadCommand> getLoads() {
        return Collections.unmodifiableList(loads);
    }

    /**
     * DTO: LoadCommand
     * <p>
     * Represents a load with orders in the command.
     */
    @Getter
    @Builder
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Defensive copies are created in constructor and getter returns defensive copy")
    public static final class LoadCommand {
        private final String loadNumber;
        @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Defensive copies are created in constructor")
        private final List<OrderCommand> orders;

        public LoadCommand(String loadNumber, List<OrderCommand> orders) {
            if (loadNumber == null || loadNumber.isBlank()) {
                throw new IllegalArgumentException("Load number is required");
            }
            if (orders == null || orders.isEmpty()) {
                throw new IllegalArgumentException("At least one order is required");
            }
            this.loadNumber = loadNumber;
            // Create defensive copy (orders is already validated as non-null above)
            this.orders = List.copyOf(orders);
        }

        /**
         * Returns a defensive copy of the orders list to prevent external modification.
         *
         * @return unmodifiable copy of the orders list
         */
        public List<OrderCommand> getOrders() {
            return Collections.unmodifiableList(orders);
        }
    }

    /**
     * DTO: OrderCommand
     * <p>
     * Represents an order with line items in the command.
     */
    @Getter
    @Builder
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Defensive copies are created in constructor and getter returns defensive copy")
    public static final class OrderCommand {
        private final String orderNumber;
        private final String customerCode;
        private final String customerName;
        private final String priority;
        @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Defensive copies are created in constructor")
        private final List<OrderLineItemCommand> lineItems;

        public OrderCommand(String orderNumber, String customerCode, String customerName, String priority, List<OrderLineItemCommand> lineItems) {
            if (orderNumber == null || orderNumber.isBlank()) {
                throw new IllegalArgumentException("Order number is required");
            }
            if (customerCode == null || customerCode.isBlank()) {
                throw new IllegalArgumentException("Customer code is required");
            }
            if (lineItems == null || lineItems.isEmpty()) {
                throw new IllegalArgumentException("At least one line item is required");
            }
            this.orderNumber = orderNumber;
            this.customerCode = customerCode;
            this.customerName = customerName;
            this.priority = priority;
            // Create defensive copy (lineItems is already validated as non-null above)
            this.lineItems = List.copyOf(lineItems);
        }

        /**
         * Returns a defensive copy of the line items list to prevent external modification.
         *
         * @return unmodifiable copy of the line items list
         */
        public List<OrderLineItemCommand> getLineItems() {
            return Collections.unmodifiableList(lineItems);
        }
    }

    /**
     * DTO: OrderLineItemCommand
     * <p>
     * Represents an order line item in the command.
     */
    @Getter
    @Builder
    public static final class OrderLineItemCommand {
        private final String productCode;
        private final int quantity;
        private final String notes;

        public OrderLineItemCommand(String productCode, int quantity, String notes) {
            if (productCode == null || productCode.isBlank()) {
                throw new IllegalArgumentException("Product code is required");
            }
            if (quantity <= 0) {
                throw new IllegalArgumentException("Quantity must be positive");
            }
            this.productCode = productCode;
            this.quantity = quantity;
            this.notes = notes;
        }
    }
}
