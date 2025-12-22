package com.ccbsa.wms.gateway.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateStockAdjustmentResponse {
    private String adjustmentId;
    private String consignmentId;
    private String adjustmentType;
    private Integer quantity;
    private Integer newQuantity;
}

