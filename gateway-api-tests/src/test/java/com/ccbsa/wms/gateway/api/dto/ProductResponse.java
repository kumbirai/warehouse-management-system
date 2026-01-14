package com.ccbsa.wms.gateway.api.dto;

import java.util.List;

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
public class ProductResponse {
    private String productId;
    private String sku;
    private String name;
    private String description;
    private String primaryBarcode;
    private List<String> secondaryBarcodes;
    private String category;
    private String unitOfMeasure;
    private Double weight;
    private ProductDimensions dimensions;
}

