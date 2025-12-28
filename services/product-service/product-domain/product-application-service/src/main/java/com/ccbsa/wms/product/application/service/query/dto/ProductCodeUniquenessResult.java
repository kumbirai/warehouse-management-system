package com.ccbsa.wms.product.application.service.query.dto;

import com.ccbsa.wms.product.domain.core.valueobject.ProductCode;

import lombok.Builder;
import lombok.Getter;

/**
 * Query Result DTO: ProductCodeUniquenessResult
 * <p>
 * Result object returned from product code uniqueness check.
 */
@Getter
@Builder
public final class ProductCodeUniquenessResult {
    private final ProductCode productCode;
    private final boolean isUnique;

    public ProductCodeUniquenessResult(ProductCode productCode, boolean isUnique) {
        if (productCode == null) {
            throw new IllegalArgumentException("ProductCode is required");
        }
        this.productCode = productCode;
        this.isUnique = isUnique;
    }
}

