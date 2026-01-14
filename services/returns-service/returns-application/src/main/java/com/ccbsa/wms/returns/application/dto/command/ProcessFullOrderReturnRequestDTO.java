package com.ccbsa.wms.returns.application.dto.command;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request DTO: ProcessFullOrderReturnRequestDTO
 * <p>
 * API request DTO for processing full order returns.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to process full order return")
public class ProcessFullOrderReturnRequestDTO {
    @NotBlank(message = "Order number is required")
    @Schema(description = "Order number", example = "ORD-2025-001", required = true)
    private String orderNumber;

    @NotEmpty(message = "Line items are required")
    @Valid
    @Schema(description = "Return line items", required = true)
    private List<FullReturnLineItemRequestDTO> lineItems;

    @NotBlank(message = "Primary return reason is required")
    @Schema(description = "Primary return reason", example = "DEFECTIVE", required = true)
    private String primaryReturnReason;

    @Schema(description = "Optional return notes")
    private String returnNotes;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Full return line item")
    public static class FullReturnLineItemRequestDTO {
        @NotBlank(message = "Product ID is required")
        @Schema(description = "Product ID", example = "750e8400-e29b-41d4-a716-446655440000", required = true)
        private String productId;

        @NotNull(message = "Ordered quantity is required")
        @Schema(description = "Ordered quantity", example = "100", required = true)
        private Integer orderedQuantity;

        @NotNull(message = "Picked quantity is required")
        @Schema(description = "Picked quantity", example = "100", required = true)
        private Integer pickedQuantity;

        @NotBlank(message = "Product condition is required")
        @Schema(description = "Product condition", example = "GOOD", required = true)
        private String productCondition;

        @NotBlank(message = "Return reason is required")
        @Schema(description = "Return reason", example = "DEFECTIVE", required = true)
        private String returnReason;

        @Schema(description = "Optional notes about the return")
        private String lineNotes;
    }
}
