package com.ccbsa.wms.stock.application.dto.query;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Query Result DTO: StockLevelQueryDTO
 * <p>
 * API response DTO for stock level queries.
 * Represents aggregated stock information for a product at a location.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockLevelQueryDTO {
    private String stockLevelId; // Generated ID for this stock level snapshot
    private String productId;
    private String locationId;
    private Integer availableQuantity; // Total - Allocated
    private Integer allocatedQuantity; // Reserved for orders
    private Integer totalQuantity; // Total physical stock
    private Integer minimumQuantity; // Optional threshold
    private Integer maximumQuantity; // Optional threshold
}
