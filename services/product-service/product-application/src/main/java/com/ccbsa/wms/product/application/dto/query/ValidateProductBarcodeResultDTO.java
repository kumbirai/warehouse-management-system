package com.ccbsa.wms.product.application.dto.query;

import com.ccbsa.wms.product.domain.core.valueobject.BarcodeType;

/**
 * Query Result DTO: ValidateProductBarcodeResultDTO
 * <p>
 * API response DTO for product barcode validation.
 */
public class ValidateProductBarcodeResultDTO {
    private boolean valid;
    private ProductInfoDTO productInfo;
    private BarcodeType barcodeFormat;
    private String errorMessage;

    public ValidateProductBarcodeResultDTO() {
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public ProductInfoDTO getProductInfo() {
        return productInfo;
    }

    public void setProductInfo(ProductInfoDTO productInfo) {
        this.productInfo = productInfo;
    }

    public BarcodeType getBarcodeFormat() {
        return barcodeFormat;
    }

    public void setBarcodeFormat(BarcodeType barcodeFormat) {
        this.barcodeFormat = barcodeFormat;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    /**
     * Nested DTO for product information.
     */
    public static class ProductInfoDTO {
        private String productId;
        private String productCode;
        private String description;
        private String barcode;
        private BarcodeType barcodeType;

        public ProductInfoDTO() {
        }

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

        public String getBarcode() {
            return barcode;
        }

        public void setBarcode(String barcode) {
            this.barcode = barcode;
        }

        public BarcodeType getBarcodeType() {
            return barcodeType;
        }

        public void setBarcodeType(BarcodeType barcodeType) {
            this.barcodeType = barcodeType;
        }
    }
}

