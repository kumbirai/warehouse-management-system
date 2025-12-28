package com.ccbsa.wms.product.application.service.query.dto;

import com.ccbsa.common.domain.valueobject.ProductId;
import com.ccbsa.wms.product.domain.core.valueobject.ProductBarcode;
import com.ccbsa.wms.product.domain.core.valueobject.ProductCode;

import lombok.Builder;
import lombok.Getter;

/**
 * Product Info DTO
 * <p>
 * Lightweight product information for barcode validation results. Used in caching and query results.
 */
@Getter
@Builder
public final class ProductInfo {
    private final ProductId productId;
    private final ProductCode productCode;
    private final String description;
    private final ProductBarcode barcode;

    public ProductInfo(ProductId productId, ProductCode productCode, String description, ProductBarcode barcode) {
        if (productId == null) {
            throw new IllegalArgumentException("ProductId is required");
        }
        if (productCode == null) {
            throw new IllegalArgumentException("ProductCode is required");
        }
        if (description == null) {
            throw new IllegalArgumentException("Description is required");
        }
        if (barcode == null) {
            throw new IllegalArgumentException("Barcode is required");
        }
        this.productId = productId;
        this.productCode = productCode;
        this.description = description;
        this.barcode = barcode;
    }
}

