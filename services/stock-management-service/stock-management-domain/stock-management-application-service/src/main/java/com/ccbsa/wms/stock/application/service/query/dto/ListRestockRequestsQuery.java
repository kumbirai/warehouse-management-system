package com.ccbsa.wms.stock.application.service.query.dto;

import com.ccbsa.common.domain.valueobject.RestockPriority;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.stock.domain.core.valueobject.RestockRequestStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Query DTO: ListRestockRequestsQuery
 * <p>
 * Query object for listing restock requests with optional filters.
 */
@Getter
@Builder
@AllArgsConstructor
public final class ListRestockRequestsQuery {
    private final TenantId tenantId;
    private final RestockRequestStatus status;
    private final RestockPriority priority;
    private final String productId;
    private final Integer page;
    private final Integer size;

    /**
     * Static factory method with validation.
     */
    public static ListRestockRequestsQuery of(TenantId tenantId, RestockRequestStatus status, RestockPriority priority, String productId, Integer page, Integer size) {
        if (tenantId == null) {
            throw new IllegalArgumentException("TenantId is required");
        }
        return ListRestockRequestsQuery.builder().tenantId(tenantId).status(status).priority(priority).productId(productId).page(page).size(size).build();
    }
}
