package com.ccbsa.wms.stock.application.dto.query;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Query Result DTO: ConsignmentQueryDTO
 * <p>
 * API response DTO for consignment queries.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Lombok builder stores list directly. Lists are immutable when returned from API.")
public class ConsignmentQueryDTO {
    private String consignmentId;
    private String consignmentReference;
    private String warehouseId;
    private String warehouseName;
    private String status;
    private LocalDateTime receivedAt;
    private LocalDateTime confirmedAt;
    private String receivedBy;
    private String receivedByName;
    private List<ConsignmentLineItemDTO> lineItems;
    private String productId; // Product ID from first line item (for backward compatibility)
    private LocalDateTime createdAt;
    private LocalDateTime lastModifiedAt;

    /**
     * Nested DTO for consignment line items.
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConsignmentLineItemDTO {
        private String productCode;
        private int quantity;
        private LocalDate expirationDate;
    }
}

