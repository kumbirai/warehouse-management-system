package com.ccbsa.wms.product.application.dto.command;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Command DTO: CreateProductCommandDTO
 * <p>
 * API request DTO for creating a new product.
 */
public final class CreateProductCommandDTO {
    @NotBlank(message = "Product code is required")
    @Size(max = 50, message = "Product code must not exceed 50 characters")
    private String productCode;

    @NotBlank(message = "Description is required")
    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @NotBlank(message = "Primary barcode is required")
    private String primaryBarcode;

    @NotNull(message = "Unit of measure is required")
    private String unitOfMeasure;

    private List<String> secondaryBarcodes;
    private String category;
    private String brand;

    public String getProductCode() {
        return productCode;
    }

    public void setProductCode(String productCode) {
        this.productCode = productCode;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPrimaryBarcode() {
        return primaryBarcode;
    }

    public void setPrimaryBarcode(String primaryBarcode) {
        this.primaryBarcode = primaryBarcode;
    }

    public String getUnitOfMeasure() {
        return unitOfMeasure;
    }

    public void setUnitOfMeasure(String unitOfMeasure) {
        this.unitOfMeasure = unitOfMeasure;
    }

    public List<String> getSecondaryBarcodes() {
        return secondaryBarcodes;
    }

    public void setSecondaryBarcodes(List<String> secondaryBarcodes) {
        this.secondaryBarcodes = secondaryBarcodes;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }
}

