package com.ccbsa.wms.picking.application.service.command.dto;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import com.ccbsa.common.domain.valueobject.TenantId;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Builder;
import lombok.Getter;

/**
 * Command DTO: CreatePickingTaskCommand
 * <p>
 * Command object for creating picking tasks.
 */
@Getter
@Builder
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Defensive copies are created in constructor and getter returns defensive copy")
public final class CreatePickingTaskCommand {
    private final TenantId tenantId;
    private final String orderId;
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Defensive copies are created in constructor")
    private final List<PickingItemCommand> items;
    private final String priority;
    private final LocalDate dueDate;

    public CreatePickingTaskCommand(TenantId tenantId, String orderId, List<PickingItemCommand> items, String priority, LocalDate dueDate) {
        if (tenantId == null) {
            throw new IllegalArgumentException("TenantId is required");
        }
        if (orderId == null || orderId.isBlank()) {
            throw new IllegalArgumentException("Order ID is required");
        }
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("At least one item is required");
        }
        this.tenantId = tenantId;
        this.orderId = orderId;
        // Create defensive copy (items is already validated as non-null above)
        this.items = List.copyOf(items);
        this.priority = priority;
        this.dueDate = dueDate;
    }

    /**
     * Returns a defensive copy of the items list to prevent external modification.
     *
     * @return unmodifiable copy of the items list
     */
    public List<PickingItemCommand> getItems() {
        return Collections.unmodifiableList(items);
    }

    /**
     * DTO: PickingItemCommand
     * <p>
     * Represents a picking item in the command.
     */
    @Getter
    @Builder
    public static final class PickingItemCommand {
        private final String productId;
        private final int quantity;
        private final String locationId;

        public PickingItemCommand(String productId, int quantity, String locationId) {
            if (productId == null || productId.isBlank()) {
                throw new IllegalArgumentException("Product ID is required");
            }
            if (quantity <= 0) {
                throw new IllegalArgumentException("Quantity must be positive");
            }
            if (locationId == null || locationId.isBlank()) {
                throw new IllegalArgumentException("Location ID is required");
            }
            this.productId = productId;
            this.quantity = quantity;
            this.locationId = locationId;
        }
    }
}
