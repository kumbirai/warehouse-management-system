package com.ccbsa.wms.picking.application.service.query.dto;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.picking.domain.core.valueobject.PickingTaskStatus;

import lombok.Builder;
import lombok.Getter;

/**
 * Query DTO: ListPickingTasksQuery
 * <p>
 * Query object for listing picking tasks with filtering and pagination.
 */
@Getter
@Builder
public final class ListPickingTasksQuery {
    private final TenantId tenantId;
    private final PickingTaskStatus status;
    private final int page;
    private final int size;

    public ListPickingTasksQuery(TenantId tenantId, PickingTaskStatus status, int page, int size) {
        if (tenantId == null) {
            throw new IllegalArgumentException("TenantId is required");
        }
        if (page < 0) {
            throw new IllegalArgumentException("Page must be non-negative");
        }
        if (size <= 0) {
            throw new IllegalArgumentException("Size must be positive");
        }
        this.tenantId = tenantId;
        this.status = status;
        this.page = page;
        this.size = size;
    }
}
