package com.ccbsa.wms.returns.application.dto.query;

import java.time.LocalDateTime;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Query Response DTO: ReturnQueryDTO
 * <p>
 * API response DTO for return queries.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Return query response")
public class ReturnQueryDTO {
    @Schema(description = "Return ID", example = "850e8400-e29b-41d4-a716-446655440000")
    private String returnId;

    @Schema(description = "Order number", example = "ORD-2025-001")
    private String orderNumber;

    @Schema(description = "Return type", example = "PARTIAL")
    private String returnType;

    @Schema(description = "Return status", example = "INITIATED")
    private String status;

    @Schema(description = "Return line items")
    private List<ReturnLineItemQueryDTO> lineItems;

    @Schema(description = "Primary return reason", example = "DEFECTIVE")
    private String primaryReturnReason;

    @Schema(description = "Return notes")
    private String returnNotes;

    @Schema(description = "Return timestamp")
    private LocalDateTime returnedAt;

    @Schema(description = "Creation timestamp")
    private LocalDateTime createdAt;

    @Schema(description = "Last modification timestamp")
    private LocalDateTime lastModifiedAt;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Return line item query response")
    public static class ReturnLineItemQueryDTO {
        @Schema(description = "Line item ID")
        private String lineItemId;

        @Schema(description = "Product ID")
        private String productId;

        @Schema(description = "Ordered quantity")
        private Integer orderedQuantity;

        @Schema(description = "Picked quantity")
        private Integer pickedQuantity;

        @Schema(description = "Accepted quantity")
        private Integer acceptedQuantity;

        @Schema(description = "Returned quantity")
        private Integer returnedQuantity;

        @Schema(description = "Product condition")
        private String productCondition;

        @Schema(description = "Return reason")
        private String returnReason;

        @Schema(description = "Line notes")
        private String lineNotes;
    }
}
