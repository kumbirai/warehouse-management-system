package com.ccbsa.wms.gateway.api.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for stock item query results.
 * Matches the structure returned by the stock management service.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockItemQueryDTO {
    private String stockItemId;
    private String productId;
    private String productCode;
    private String productDescription;
    private String locationId;
    private String locationCode;
    private String locationName;
    private String locationHierarchy;
    private Integer quantity;
    private Integer allocatedQuantity;
    private LocalDate expirationDate;
    private String classification;
    private Integer daysUntilExpiry;
    private LocalDateTime createdAt;
    private LocalDateTime lastModifiedAt;
}
