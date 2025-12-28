package com.ccbsa.wms.stock.application.dto.query;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Query DTO: StockAllocationQueryDTO
 * <p>
 * API response DTO for stock allocation queries.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockAllocationQueryDTO {
    private String allocationId;
    private String productId;
    private String locationId;
    private String stockItemId;
    private Integer quantity;
    private String allocationType;
    private String referenceId;
    private String status;
    private LocalDateTime allocatedAt;
    private LocalDateTime releasedAt;
    private String allocatedBy;
    private String notes;
}

