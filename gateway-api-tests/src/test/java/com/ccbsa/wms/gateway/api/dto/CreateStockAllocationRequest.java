package com.ccbsa.wms.gateway.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateStockAllocationRequest {
    private String productId;
    private String sourceLocationId;
    private Integer quantity;
    private String allocationType;
    private String referenceId;
}

