package com.ccbsa.wms.gateway.api.fixture;

import java.util.List;

import com.ccbsa.wms.gateway.api.dto.CreateProductRequest;

/**
 * Builder for creating product test data.
 */
public class ProductTestDataBuilder {

    public static CreateProductRequest buildCreateProductRequest() {
        return CreateProductRequest.builder().productCode(TestData.productSKU())  // Use SKU as productCode
                .description(TestData.productDescription()).primaryBarcode(TestData.barcode()).unitOfMeasure(TestData.unitOfMeasure()).secondaryBarcodes(List.of())
                .category(TestData.productCategory()).build();
    }

    public static CreateProductRequest buildCreateProductRequestWithBarcode(String barcode) {
        return CreateProductRequest.builder().productCode(TestData.productSKU()).description(TestData.productDescription()).primaryBarcode(barcode)
                .unitOfMeasure(TestData.unitOfMeasure()).category(TestData.productCategory()).build();
    }

    public static CreateProductRequest buildCreateProductRequestWithSecondaryBarcodes(int count) {
        return CreateProductRequest.builder().productCode(TestData.productSKU()).description(TestData.productDescription()).primaryBarcode(TestData.barcode())
                .unitOfMeasure(TestData.unitOfMeasure()).secondaryBarcodes(TestData.secondaryBarcodes(count)).category(TestData.productCategory()).build();
    }
}

