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
public class CountedItem {
    private String productId;
    private String batchNumber;
    private Integer countedQuantity;
    private Integer systemQuantity;
    private Integer variance;
}

