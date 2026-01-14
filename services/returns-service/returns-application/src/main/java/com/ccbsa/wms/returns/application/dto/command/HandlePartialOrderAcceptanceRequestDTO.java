package com.ccbsa.wms.returns.application.dto.command;

import java.time.Instant;
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
 * Request DTO: HandlePartialOrderAcceptanceRequestDTO
 * <p>
 * API request DTO for handling partial order acceptance.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to handle partial order acceptance")
public class HandlePartialOrderAcceptanceRequestDTO {
    @NotBlank(message = "Order number is required")
    @Schema(description = "Order number", example = "ORD-2025-001", required = true)
    private String orderNumber;

    @NotEmpty(message = "Line items are required")
    @Valid
    @Schema(description = "Return line items", required = true)
    private List<PartialReturnLineItemRequestDTO> lineItems;

    @NotBlank(message = "Signature data is required")
    @Schema(description = "Base64 encoded signature image", required = true)
    private String signatureData;

    @NotNull(message = "Signed at timestamp is required")
    @Schema(description = "Signature timestamp", required = true)
    private Instant signedAt;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Partial return line item")
    public static class PartialReturnLineItemRequestDTO {
        @NotBlank(message = "Product ID is required")
        @Schema(description = "Product ID", example = "750e8400-e29b-41d4-a716-446655440000", required = true)
        private String productId;

        @NotNull(message = "Ordered quantity is required")
        @Schema(description = "Ordered quantity", example = "100", required = true)
        private Integer orderedQuantity;

        @NotNull(message = "Picked quantity is required")
        @Schema(description = "Picked quantity", example = "100", required = true)
        private Integer pickedQuantity;

        @NotNull(message = "Accepted quantity is required")
        @Schema(description = "Accepted quantity", example = "80", required = true)
        private Integer acceptedQuantity;

        @Schema(description = "Return reason (required if accepted < picked)", example = "DEFECTIVE")
        private String returnReason;

        @Schema(description = "Optional notes about the return")
        private String lineNotes;
    }
}
