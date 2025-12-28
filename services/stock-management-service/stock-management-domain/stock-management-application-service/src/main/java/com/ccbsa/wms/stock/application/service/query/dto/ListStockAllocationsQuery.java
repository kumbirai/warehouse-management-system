package com.ccbsa.wms.stock.application.service.query.dto;

import com.ccbsa.common.domain.valueobject.ProductId;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;
import com.ccbsa.wms.stock.domain.core.valueobject.AllocationStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Query DTO: ListStockAllocationsQuery
 * <p>
 * Query object for listing stock allocations with optional filters.
 */
@Getter
@Builder
@AllArgsConstructor
public final class ListStockAllocationsQuery {
    private final TenantId tenantId;
    private final ProductId productId;
    private final LocationId locationId;
    private final String referenceId;
    private final AllocationStatus status;
    private final Integer page;
    private final Integer size;

    /**
     * Static factory method with validation.
     */
    public static ListStockAllocationsQuery of(TenantId tenantId, ProductId productId, LocationId locationId, String referenceId, AllocationStatus status, Integer page,
                                               Integer size) {
        if (tenantId == null) {
            throw new IllegalArgumentException("TenantId is required");
        }
        return ListStockAllocationsQuery.builder().tenantId(tenantId).productId(productId).locationId(locationId).referenceId(referenceId).status(status).page(page).size(size)
                .build();
    }
}

