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
public class CreateProductRequest {
    private String productCode;  // Required - maps to productCode in service
    private String description;  // Required
    private String primaryBarcode;  // Required
    private String unitOfMeasure;  // Required
    private List<String> secondaryBarcodes;  // Optional
    private String category;  // Optional
    private String brand;  // Optional
}

