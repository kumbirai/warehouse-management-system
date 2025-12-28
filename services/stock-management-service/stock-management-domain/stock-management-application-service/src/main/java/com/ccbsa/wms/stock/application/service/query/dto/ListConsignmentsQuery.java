package com.ccbsa.wms.stock.application.service.query.dto;

import com.ccbsa.common.domain.valueobject.TenantId;

import lombok.Builder;
import lombok.Getter;

/**
 * Query DTO: ListConsignmentsQuery
 * <p>
 * Query object for listing consignments with pagination.
 */
@Getter
@Builder
public final class ListConsignmentsQuery {
    private final TenantId tenantId;
    private final Integer page;
    private final Integer size;

    public ListConsignmentsQuery(TenantId tenantId, Integer page, Integer size) {
        if (tenantId == null) {
            throw new IllegalArgumentException("TenantId is required");
        }
        this.tenantId = tenantId;
        this.page = page;
        this.size = size;
    }
}

