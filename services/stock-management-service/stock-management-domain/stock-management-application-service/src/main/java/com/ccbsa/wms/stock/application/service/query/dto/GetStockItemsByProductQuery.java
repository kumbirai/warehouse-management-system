package com.ccbsa.wms.stock.application.service.query.dto;

import com.ccbsa.common.domain.valueobject.ProductId;
import com.ccbsa.common.domain.valueobject.TenantId;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * Query DTO: GetStockItemsByProductQuery
 * <p>
 * Query for retrieving stock items by product ID only (including items without location assignment).
 */
@Getter
@Builder
@EqualsAndHashCode
public final class GetStockItemsByProductQuery {
    private final TenantId tenantId;
    private final ProductId productId;

    /**
     * Static factory method with validation.
     */
    public static GetStockItemsByProductQuery of(TenantId tenantId, ProductId productId) {
        if (tenantId == null) {
            throw new IllegalArgumentException("TenantId is required");
        }
        if (productId == null) {
            throw new IllegalArgumentException("ProductId is required");
        }
        return GetStockItemsByProductQuery.builder().tenantId(tenantId).productId(productId).build();
    }
}
