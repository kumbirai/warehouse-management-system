package com.ccbsa.wms.picking.application.dto.command;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

/**
 * Command DTO: CreatePickingTaskCommandDTO
 * <p>
 * DTO for creating picking tasks.
 */
@Getter
@Setter
public class CreatePickingTaskCommandDTO {
    @NotBlank(message = "Order ID is required")
    private String orderId;

    @NotEmpty(message = "At least one item is required")
    @Valid
    private List<PickingItemDTO> items;

    private String priority;

    private LocalDate dueDate;

    @Getter
    @Setter
    public static class PickingItemDTO {
        @NotBlank(message = "Product ID is required")
        private String productId;

        private int quantity;

        @NotBlank(message = "Location ID is required")
        private String locationId;
    }
}
