package com.ccbsa.wms.product.application.service.query.dto;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.product.domain.core.valueobject.ProductId;

/**
 * Query DTO: GetProductQuery
 * <p>
 * Query object for retrieving a product by ID.
 */
public final class GetProductQuery {
    private final ProductId productId;
    private final TenantId tenantId;

    private GetProductQuery(Builder builder) {
        this.productId = builder.productId;
        this.tenantId = builder.tenantId;
    }

    public static Builder builder() {
        return new Builder();
    }

    public ProductId getProductId() {
        return productId;
    }

    public TenantId getTenantId() {
        return tenantId;
    }

    public static class Builder {
        private ProductId productId;
        private TenantId tenantId;

        public Builder productId(ProductId productId) {
            this.productId = productId;
            return this;
        }

        public Builder tenantId(TenantId tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public GetProductQuery build() {
            if (productId == null) {
                throw new IllegalArgumentException("ProductId is required");
            }
            if (tenantId == null) {
                throw new IllegalArgumentException("TenantId is required");
            }
            return new GetProductQuery(this);
        }
    }
}

