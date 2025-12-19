package com.ccbsa.wms.gateway.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
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

