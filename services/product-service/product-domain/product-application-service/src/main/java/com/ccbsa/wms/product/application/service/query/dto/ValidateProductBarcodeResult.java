package com.ccbsa.wms.product.application.service.query.dto;

import com.ccbsa.wms.product.domain.core.valueobject.BarcodeType;

/**
 * Query Result DTO: ValidateProductBarcodeResult
 * <p>
 * Result object returned from barcode validation query.
 * Contains validation status, product information (if found), and barcode format.
 */
public final class ValidateProductBarcodeResult {
    private final boolean valid;
    private final ProductInfo productInfo;
    private final BarcodeType barcodeFormat;
    private final String errorMessage;

    private ValidateProductBarcodeResult(Builder builder) {
        this.valid = builder.valid;
        this.productInfo = builder.productInfo;
        this.barcodeFormat = builder.barcodeFormat;
        this.errorMessage = builder.errorMessage;
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean isValid() {
        return valid;
    }

    public ProductInfo getProductInfo() {
        return productInfo;
    }

    public BarcodeType getBarcodeFormat() {
        return barcodeFormat;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public static class Builder {
        private boolean valid;
        private ProductInfo productInfo;
        private BarcodeType barcodeFormat;
        private String errorMessage;

        public Builder valid(boolean valid) {
            this.valid = valid;
            return this;
        }

        public Builder productInfo(ProductInfo productInfo) {
            this.productInfo = productInfo;
            return this;
        }

        public Builder barcodeFormat(BarcodeType barcodeFormat) {
            this.barcodeFormat = barcodeFormat;
            return this;
        }

        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        public ValidateProductBarcodeResult build() {
            if (barcodeFormat == null) {
                throw new IllegalArgumentException("BarcodeFormat is required");
            }
            if (valid && productInfo == null) {
                throw new IllegalArgumentException("ProductInfo is required when valid is true");
            }
            if (!valid && errorMessage == null) {
                throw new IllegalArgumentException("ErrorMessage is required when valid is false");
            }
            return new ValidateProductBarcodeResult(this);
        }
    }
}

