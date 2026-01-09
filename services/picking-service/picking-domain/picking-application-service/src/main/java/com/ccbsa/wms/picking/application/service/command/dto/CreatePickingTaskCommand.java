package com.ccbsa.wms.picking.application.service.command.dto;

import java.time.LocalDate;
import java.util.List;

import com.ccbsa.common.domain.valueobject.TenantId;

import lombok.Builder;
import lombok.Getter;

/**
 * Command DTO: CreatePickingTaskCommand
 * <p>
 * Command object for creating picking tasks.
 */
@Getter
@Builder
public final class CreatePickingTaskCommand {
    private final TenantId tenantId;
    private final String orderId;
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
        this.items = items != null ? List.copyOf(items) : List.of();
        this.priority = priority;
        this.dueDate = dueDate;
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
