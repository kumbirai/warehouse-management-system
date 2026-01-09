package com.ccbsa.wms.gateway.api.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpiringStockItemResponse {
    private String stockItemId;
    private String productId;
    private String productCode;
    private String locationId;
    private Integer quantity;
    private LocalDate expirationDate;
    private String classification; // CRITICAL, NEAR_EXPIRY, EXPIRED, etc.
    private Integer daysUntilExpiry;
}
