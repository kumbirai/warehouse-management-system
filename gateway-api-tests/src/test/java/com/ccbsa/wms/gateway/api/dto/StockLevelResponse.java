package com.ccbsa.wms.gateway.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockLevelResponse {
    private String stockLevelId;
    private String productId;
    private String locationId;
    private Integer availableQuantity;
    private Integer allocatedQuantity;
    private Integer totalQuantity;
    private Integer minimumQuantity;
    private Integer maximumQuantity;
}

