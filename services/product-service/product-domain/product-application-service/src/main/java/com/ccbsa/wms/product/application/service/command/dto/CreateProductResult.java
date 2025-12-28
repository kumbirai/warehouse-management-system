package com.ccbsa.wms.product.application.service.command.dto;

import java.time.LocalDateTime;

import com.ccbsa.common.domain.valueobject.ProductId;
import com.ccbsa.wms.product.domain.core.valueobject.ProductBarcode;
import com.ccbsa.wms.product.domain.core.valueobject.ProductCode;

import lombok.Builder;
import lombok.Getter;

/**
 * Result DTO: CreateProductResult
 * <p>
 * Result object returned after creating a product. Contains only the information needed by the caller (not the full domain entity).
 */
@Getter
@Builder
public final class CreateProductResult {
    private final ProductId productId;
    private final ProductCode productCode;
    private final String description;
    private final ProductBarcode primaryBarcode;
    private final LocalDateTime createdAt;

    public CreateProductResult(ProductId productId, ProductCode productCode, String description, ProductBarcode primaryBarcode, LocalDateTime createdAt) {
        if (productId == null) {
            throw new IllegalArgumentException("ProductId is required");
        }
        if (productCode == null) {
            throw new IllegalArgumentException("ProductCode is required");
        }
        if (description == null) {
            throw new IllegalArgumentException("Description is required");
        }
        if (primaryBarcode == null) {
            throw new IllegalArgumentException("PrimaryBarcode is required");
        }
        this.productId = productId;
        this.productCode = productCode;
        this.description = description;
        this.primaryBarcode = primaryBarcode;
        this.createdAt = createdAt;
    }
}

