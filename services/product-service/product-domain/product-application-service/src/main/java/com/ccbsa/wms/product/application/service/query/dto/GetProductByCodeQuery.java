package com.ccbsa.wms.product.application.service.query.dto;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.product.domain.core.valueobject.ProductCode;

/**
 * Query DTO: GetProductByCodeQuery
 * <p>
 * Query object for retrieving a product by product code.
 */
public final class GetProductByCodeQuery {
    private final ProductCode productCode;
    private final TenantId tenantId;

    private GetProductByCodeQuery(Builder builder) {
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

        public GetProductByCodeQuery build() {
            if (productCode == null) {
                throw new IllegalArgumentException("ProductCode is required");
            }
            if (tenantId == null) {
                throw new IllegalArgumentException("TenantId is required");
            }
            return new GetProductByCodeQuery(this);
        }
    }
}

