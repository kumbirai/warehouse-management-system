package com.ccbsa.wms.stock.application.service.query.dto;

import java.util.Map;

import com.ccbsa.common.domain.valueobject.ProductId;
import com.ccbsa.common.domain.valueobject.TenantId;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * Query DTO: QueryStockAvailabilityForProductsQuery
 * <p>
 * Query for retrieving stock availability for multiple products.
 */
@Getter
@Builder
@EqualsAndHashCode
public final class QueryStockAvailabilityForProductsQuery {
    private final TenantId tenantId;
    private final Map<ProductId, Integer> productQuantities; // ProductId -> required quantity

    /**
     * Static factory method with validation.
     */
    public static QueryStockAvailabilityForProductsQuery of(TenantId tenantId, Map<ProductId, Integer> productQuantities) {
        if (tenantId == null) {
            throw new IllegalArgumentException("TenantId is required");
        }
        if (productQuantities == null || productQuantities.isEmpty()) {
            throw new IllegalArgumentException("Product quantities map is required and cannot be empty");
        }
        return QueryStockAvailabilityForProductsQuery.builder().tenantId(tenantId).productQuantities(productQuantities).build();
    }
}
