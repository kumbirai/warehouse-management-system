package com.ccbsa.wms.product.application.service.query.dto;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.product.domain.core.valueobject.ProductCode;

/**
 * Query DTO: CheckProductCodeUniquenessQuery
 * <p>
 * Query object for checking if a product code is unique for a tenant.
 */
public final class CheckProductCodeUniquenessQuery {
    private final ProductCode productCode;
    private final TenantId tenantId;

    private CheckProductCodeUniquenessQuery(Builder builder) {
        this.productCode = builder.productCode;
        this.tenantId = builder.tenantId;
    }

    public static Builder builder() {
        return new Builder();
    }

    public ProductCode getProductCode() {
        return productCode;
    }

    public TenantId getTenantId() {
        return tenantId;
    }

    public static class Builder {
        private ProductCode productCode;
        private TenantId tenantId;

        public Builder productCode(ProductCode productCode) {
            this.productCode = productCode;
            return this;
        }

        public Builder tenantId(TenantId tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public CheckProductCodeUniquenessQuery build() {
            if (productCode == null) {
                throw new IllegalArgumentException("ProductCode is required");
            }
            if (tenantId == null) {
                throw new IllegalArgumentException("TenantId is required");
            }
            return new CheckProductCodeUniquenessQuery(this);
        }
    }
}

