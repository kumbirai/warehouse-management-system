package com.ccbsa.wms.gateway.api.fixture;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

/**
 * Test data builder for Product entities.
 * Provides fluent API for creating product test data.
 */
@Slf4j
public final class ProductTestDataBuilder {
    private String productCode;
    private String description;
    private String primaryBarcode;
    private String unitOfMeasure;
    private List<String> secondaryBarcodes;
    private String category;
    private String brand;

    private ProductTestDataBuilder() {
        this.secondaryBarcodes = new ArrayList<>();
    }

    public static ProductTestDataBuilder builder() {
        return new ProductTestDataBuilder();
    }

    public ProductTestDataBuilder productCode(String productCode) {
        this.productCode = productCode;
        return this;
    }

    public ProductTestDataBuilder description(String description) {
        this.description = description;
        return this;
    }

    public ProductTestDataBuilder primaryBarcode(String primaryBarcode) {
        this.primaryBarcode = primaryBarcode;
        return this;
    }

    public ProductTestDataBuilder unitOfMeasure(String unitOfMeasure) {
        this.unitOfMeasure = unitOfMeasure;
        return this;
    }

    public ProductTestDataBuilder secondaryBarcode(String secondaryBarcode) {
        this.secondaryBarcodes.add(secondaryBarcode);
        return this;
    }

    public ProductTestDataBuilder secondaryBarcodes(List<String> secondaryBarcodes) {
        this.secondaryBarcodes = secondaryBarcodes != null ? new ArrayList<>(secondaryBarcodes) : new ArrayList<>();
        return this;
    }

    public ProductTestDataBuilder category(String category) {
        this.category = category;
        return this;
    }

    public ProductTestDataBuilder brand(String brand) {
        this.brand = brand;
        return this;
    }

    /**
     * Builds a map representing the product creation request.
     *
     * @return Map with product data
     */
    public Map<String, Object> build() {
        Map<String, Object> request = new HashMap<>();
        if (productCode != null) {
            request.put("productCode", productCode);
        }
        if (description != null) {
            request.put("description", description);
        }
        if (primaryBarcode != null) {
            request.put("primaryBarcode", primaryBarcode);
        }
        if (unitOfMeasure != null) {
            request.put("unitOfMeasure", unitOfMeasure);
        }
        if (!secondaryBarcodes.isEmpty()) {
            request.put("secondaryBarcodes", secondaryBarcodes);
        }
        if (category != null) {
            request.put("category", category);
        }
        if (brand != null) {
            request.put("brand", brand);
        }
        return request;
    }

    /**
     * Creates a default product with all required fields.
     *
     * @return Map with default product data
     */
    public static Map<String, Object> createDefault() {
        String timestamp = String.valueOf(System.currentTimeMillis());
        return builder()
                .productCode("PROD-TEST-" + timestamp)
                .description("Test product created by ProductTestDataBuilder")
                .primaryBarcode("600106710" + timestamp.substring(Math.max(0, timestamp.length() - 3)))
                .unitOfMeasure("EA")
                .category("Test Category")
                .brand("Test Brand")
                .build();
    }
}

