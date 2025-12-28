package com.ccbsa.wms.product.application.service.query.dto;

import com.ccbsa.wms.product.domain.core.valueobject.BarcodeType;

import lombok.Builder;
import lombok.Getter;

/**
 * Query Result DTO: ValidateProductBarcodeResult
 * <p>
 * Result object returned from barcode validation query. Contains validation status, product information (if found), and barcode format.
 */
@Getter
@Builder
public final class ValidateProductBarcodeResult {
    private final boolean valid;
    private final ProductInfo productInfo;
    private final BarcodeType barcodeFormat;
    private final String errorMessage;

    public ValidateProductBarcodeResult(boolean valid, ProductInfo productInfo, BarcodeType barcodeFormat, String errorMessage) {
        if (barcodeFormat == null) {
            throw new IllegalArgumentException("BarcodeFormat is required");
        }
        if (valid && productInfo == null) {
            throw new IllegalArgumentException("ProductInfo is required when valid is true");
        }
        if (!valid && errorMessage == null) {
            throw new IllegalArgumentException("ErrorMessage is required when valid is false");
        }
        this.valid = valid;
        this.productInfo = productInfo;
        this.barcodeFormat = barcodeFormat;
        this.errorMessage = errorMessage;
    }
}

