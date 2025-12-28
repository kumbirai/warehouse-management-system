package com.ccbsa.wms.product.application.service.query.dto;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.product.domain.core.valueobject.ProductCode;

import lombok.Builder;
import lombok.Getter;

/**
 * Query DTO: GetProductByCodeQuery
 * <p>
 * Query object for retrieving a product by product code.
 */
@Getter
@Builder
public final class GetProductByCodeQuery {
    private final ProductCode productCode;
    private final TenantId tenantId;

    public GetProductByCodeQuery(ProductCode productCode, TenantId tenantId) {
        if (productCode == null) {
            throw new IllegalArgumentException("ProductCode is required");
        }
        if (tenantId == null) {
            throw new IllegalArgumentException("TenantId is required");
        }
        this.productCode = productCode;
        this.tenantId = tenantId;
    }
}

