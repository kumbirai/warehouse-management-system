package com.ccbsa.wms.product.application.dto.command;

import java.time.LocalDateTime;

/**
 * Result DTO: UpdateProductResultDTO
 * <p>
 * API response DTO for product update result.
 */
public final class UpdateProductResultDTO {
    private String productId;
    private LocalDateTime lastModifiedAt;

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public LocalDateTime getLastModifiedAt() {
        return lastModifiedAt;
    }

    public void setLastModifiedAt(LocalDateTime lastModifiedAt) {
        this.lastModifiedAt = lastModifiedAt;
    }
}

