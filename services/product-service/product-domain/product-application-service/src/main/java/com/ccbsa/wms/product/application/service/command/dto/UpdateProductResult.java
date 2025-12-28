package com.ccbsa.wms.product.application.service.command.dto;

import java.time.LocalDateTime;

import com.ccbsa.common.domain.valueobject.ProductId;

import lombok.Builder;
import lombok.Getter;

/**
 * Result DTO: UpdateProductResult
 * <p>
 * Result object returned after updating a product.
 */
@Getter
@Builder
public final class UpdateProductResult {
    private final ProductId productId;
    private final LocalDateTime lastModifiedAt;

    public UpdateProductResult(ProductId productId, LocalDateTime lastModifiedAt) {
        if (productId == null) {
            throw new IllegalArgumentException("ProductId is required");
        }
        this.productId = productId;
        this.lastModifiedAt = lastModifiedAt;
    }
}

