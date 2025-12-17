package com.ccbsa.wms.product.application.service.query.dto;

import com.ccbsa.common.domain.valueobject.TenantId;

/**
 * Query DTO: ValidateProductBarcodeQuery
 * <p>
 * Query object for validating a product barcode.
 */
public final class ValidateProductBarcodeQuery {
    private final String barcode;
    private final TenantId tenantId;

    private ValidateProductBarcodeQuery(Builder builder) {
        this.barcode = builder.barcode;
        this.tenantId = builder.tenantId;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getBarcode() {
        return barcode;
    }

    public TenantId getTenantId() {
        return tenantId;
    }

    public static class Builder {
        private String barcode;
        private TenantId tenantId;

        public Builder barcode(String barcode) {
            this.barcode = barcode;
            return this;
        }

        public Builder tenantId(TenantId tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public ValidateProductBarcodeQuery build() {
            if (barcode == null || barcode.trim()
                    .isEmpty()) {
                throw new IllegalArgumentException("Barcode is required");
            }
            if (tenantId == null) {
                throw new IllegalArgumentException("TenantId is required");
            }
            return new ValidateProductBarcodeQuery(this);
        }
    }
}

