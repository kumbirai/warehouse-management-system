package com.ccbsa.wms.stock.application.dto.query;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Query DTO: StockAdjustmentQueryDTO
 * <p>
 * API response DTO for stock adjustment queries.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockAdjustmentQueryDTO {
    private String adjustmentId;
    private String productId;
    private String locationId;
    private String stockItemId;
    private String adjustmentType;
    private Integer quantity;
    private Integer quantityBefore;
    private Integer quantityAfter;
    private String reason;
    private String notes;
    private String adjustedBy;
    private String authorizationCode;
    private LocalDateTime adjustedAt;
}

