package com.ccbsa.wms.product.application.service.command.dto;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * DTO: ProductCsvRow
 * <p>
 * Represents a single row from a CSV file for product data.
 */
@Getter
@Builder
@EqualsAndHashCode
public final class ProductCsvRow {
    private final long rowNumber;
    private final String productCode;
    private final String description;
    private final String primaryBarcode;
    private final String unitOfMeasure;
    private final String secondaryBarcode; // Optional
    private final String category; // Optional
    private final String brand; // Optional

    public ProductCsvRow(long rowNumber, String productCode, String description, String primaryBarcode, String unitOfMeasure, String secondaryBarcode, String category,
                         String brand) {
        if (productCode == null || productCode.trim().isEmpty()) {
            throw new IllegalArgumentException("ProductCode is required");
        }
        if (description == null || description.trim().isEmpty()) {
            throw new IllegalArgumentException("Description is required");
        }
        if (primaryBarcode == null || primaryBarcode.trim().isEmpty()) {
            throw new IllegalArgumentException("PrimaryBarcode is required");
        }
        if (unitOfMeasure == null || unitOfMeasure.trim().isEmpty()) {
            throw new IllegalArgumentException("UnitOfMeasure is required");
        }
        this.rowNumber = rowNumber;
        this.productCode = productCode;
        this.description = description;
        this.primaryBarcode = primaryBarcode;
        this.unitOfMeasure = unitOfMeasure;
        this.secondaryBarcode = secondaryBarcode;
        this.category = category;
        this.brand = brand;
    }
}

