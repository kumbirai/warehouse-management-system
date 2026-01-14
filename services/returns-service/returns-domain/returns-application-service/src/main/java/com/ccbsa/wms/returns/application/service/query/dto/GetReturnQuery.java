package com.ccbsa.wms.returns.application.service.query.dto;

import com.ccbsa.common.domain.valueobject.ReturnId;
import com.ccbsa.common.domain.valueobject.TenantId;

import lombok.Builder;
import lombok.Getter;

/**
 * Query DTO: GetReturnQuery
 * <p>
 * Query object for retrieving a return by ID.
 */
@Getter
@Builder
public final class GetReturnQuery {
    private final ReturnId returnId;
    private final TenantId tenantId;

    public GetReturnQuery(ReturnId returnId, TenantId tenantId) {
        if (returnId == null) {
            throw new IllegalArgumentException("ReturnId is required");
        }
        if (tenantId == null) {
            throw new IllegalArgumentException("TenantId is required");
        }
        this.returnId = returnId;
        this.tenantId = tenantId;
    }
}
