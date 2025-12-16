package com.ccbsa.wms.product.application.dto.query;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Query Result DTO: ProductQueryResultDTO
 * <p>
 * API response DTO for product query results.
 */
public final class ProductQueryResultDTO {
    private String productId;
    private String productCode;
    private String description;
    private String primaryBarcode;
    private List<String> secondaryBarcodes;
    private String unitOfMeasure;
    private String category;
    private String brand;
    private LocalDateTime createdAt;
    private LocalDateTime lastModifiedAt;

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

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

    public List<String> getSecondaryBarcodes() {
        return secondaryBarcodes;
    }

    public void setSecondaryBarcodes(List<String> secondaryBarcodes) {
        this.secondaryBarcodes = secondaryBarcodes;
    }

    public String getUnitOfMeasure() {
        return unitOfMeasure;
    }

    public void setUnitOfMeasure(String unitOfMeasure) {
        this.unitOfMeasure = unitOfMeasure;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getLastModifiedAt() {
        return lastModifiedAt;
    }

    public void setLastModifiedAt(LocalDateTime lastModifiedAt) {
        this.lastModifiedAt = lastModifiedAt;
    }
}

