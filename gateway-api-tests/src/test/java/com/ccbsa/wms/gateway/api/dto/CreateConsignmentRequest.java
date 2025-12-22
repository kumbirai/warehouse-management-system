package com.ccbsa.wms.gateway.api.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateConsignmentRequest {
    // Legacy fields (for backward compatibility with existing tests)
    private String productId;
    private String locationId;
    private Integer quantity;
    private String batchNumber;
    private LocalDate expirationDate;
    private LocalDate manufactureDate;
    private String supplierReference;
    private LocalDate receivedDate;

    // New fields (matching actual API structure)
    private String consignmentReference;
    private String warehouseId;
    private LocalDateTime receivedAt;
    private String receivedBy;
    private List<ConsignmentLineItem> lineItems;

    /**
     * Nested DTO for consignment line items.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConsignmentLineItem {
        private String productCode;
        private Integer quantity;
        private LocalDate expirationDate;
    }
}

