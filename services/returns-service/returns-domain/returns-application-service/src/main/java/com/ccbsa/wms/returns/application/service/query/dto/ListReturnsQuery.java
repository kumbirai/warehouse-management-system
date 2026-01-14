package com.ccbsa.wms.returns.application.service.query.dto;

import com.ccbsa.common.domain.valueobject.ReturnStatus;
import com.ccbsa.common.domain.valueobject.TenantId;

import lombok.Builder;
import lombok.Getter;

/**
 * Query DTO: ListReturnsQuery
 * <p>
 * Query object for listing returns with optional status filter and pagination.
 */
@Getter
@Builder
public final class ListReturnsQuery {
    private final TenantId tenantId;
    private final ReturnStatus status;
    private final int page;
    private final int size;

    public ListReturnsQuery(TenantId tenantId, ReturnStatus status, int page, int size) {
        if (tenantId == null) {
            throw new IllegalArgumentException("TenantId is required");
        }
        if (page < 0) {
            throw new IllegalArgumentException("Page must be >= 0");
        }
        if (size <= 0) {
            throw new IllegalArgumentException("Size must be > 0");
        }
        this.tenantId = tenantId;
        this.status = status;
        this.page = page;
        this.size = size;
    }
}
