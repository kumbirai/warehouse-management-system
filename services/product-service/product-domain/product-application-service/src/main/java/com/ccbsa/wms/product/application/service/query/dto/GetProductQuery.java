package com.ccbsa.wms.product.application.service.query.dto;

import com.ccbsa.common.domain.valueobject.ProductId;
import com.ccbsa.common.domain.valueobject.TenantId;

import lombok.Builder;
import lombok.Getter;

/**
 * Query DTO: GetProductQuery
 * <p>
 * Query object for retrieving a product by ID.
 */
@Getter
@Builder
public final class GetProductQuery {
    private final ProductId productId;
    private final TenantId tenantId;

    public GetProductQuery(ProductId productId, TenantId tenantId) {
        if (productId == null) {
            throw new IllegalArgumentException("ProductId is required");
        }
        if (tenantId == null) {
            throw new IllegalArgumentException("TenantId is required");
        }
        this.productId = productId;
        this.tenantId = tenantId;
    }
}

