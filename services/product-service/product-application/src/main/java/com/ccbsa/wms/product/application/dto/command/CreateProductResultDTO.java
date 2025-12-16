package com.ccbsa.wms.product.application.dto.command;

import java.time.LocalDateTime;

/**
 * Result DTO: CreateProductResultDTO
 * <p>
 * API response DTO for product creation result.
 */
public final class CreateProductResultDTO {
    private String productId;
    private String productCode;
    private String description;
    private String primaryBarcode;
    private LocalDateTime createdAt;

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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

