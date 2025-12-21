package com.ccbsa.wms.gateway.api.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProductRequest {
    private String description;
    private String primaryBarcode;
    private String unitOfMeasure;
    private List<String> secondaryBarcodes;
    private String category;
    private String brand;
}

