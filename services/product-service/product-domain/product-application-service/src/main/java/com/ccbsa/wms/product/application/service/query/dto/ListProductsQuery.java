package com.ccbsa.wms.product.application.service.query.dto;

import com.ccbsa.common.domain.valueobject.TenantId;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Query DTO: ListProductsQuery
 * <p>
 * Query object for listing products with optional filters.
 */
@Getter
@Builder
@AllArgsConstructor
public final class ListProductsQuery {
    private final TenantId tenantId;
    private final Integer page;
    private final Integer size;
    private final String category;
    private final String brand;
    private final String search;

    /**
     * Static factory method with validation.
     */
    public static ListProductsQuery of(TenantId tenantId, Integer page, Integer size, String category, String brand, String search) {
        if (tenantId == null) {
            throw new IllegalArgumentException("TenantId is required");
        }
        return ListProductsQuery.builder().tenantId(tenantId).page(page).size(size).category(category).brand(brand).search(search).build();
    }
}

