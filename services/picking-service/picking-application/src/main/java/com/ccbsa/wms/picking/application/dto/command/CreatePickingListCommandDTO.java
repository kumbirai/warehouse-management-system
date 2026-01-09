package com.ccbsa.wms.picking.application.dto.command;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

/**
 * Command DTO: CreatePickingListCommandDTO
 * <p>
 * DTO for manual picking list creation.
 */
@Getter
@Setter
public class CreatePickingListCommandDTO {
    @NotEmpty(message = "At least one load is required")
    @Valid
    private List<LoadDTO> loads;
    private String notes;

    @Getter
    @Setter
    public static class LoadDTO {
        @NotEmpty(message = "Load number is required")
        private String loadNumber;
        @NotEmpty(message = "At least one order is required")
        @Valid
        private List<OrderDTO> orders;
    }

    @Getter
    @Setter
    public static class OrderDTO {
        @NotEmpty(message = "Order number is required")
        private String orderNumber;
        @NotEmpty(message = "Customer code is required")
        private String customerCode;
        private String customerName;
        private String priority;
        @NotEmpty(message = "At least one line item is required")
        @Valid
        private List<OrderLineItemDTO> lineItems;
    }

    @Getter
    @Setter
    public static class OrderLineItemDTO {
        @NotEmpty(message = "Product code is required")
        private String productCode;
        private int quantity;
        private String notes;
    }
}
