package com.ccbsa.wms.product.application.service.query.dto;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.product.domain.core.valueobject.ProductCode;

import lombok.Builder;
import lombok.Getter;

/**
 * Query DTO: CheckProductCodeUniquenessQuery
 * <p>
 * Query object for checking if a product code is unique for a tenant.
 */
@Getter
@Builder
public final class CheckProductCodeUniquenessQuery {
    private final ProductCode productCode;
    private final TenantId tenantId;

    public CheckProductCodeUniquenessQuery(ProductCode productCode, TenantId tenantId) {
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

