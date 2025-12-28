package com.ccbsa.wms.stock.application.service.query.dto;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.stock.domain.core.valueobject.ConsignmentId;

import lombok.Builder;
import lombok.Getter;

/**
 * Query DTO: GetConsignmentQuery
 * <p>
 * Query object for retrieving a consignment by ID.
 */
@Getter
@Builder
public final class GetConsignmentQuery {
    private final ConsignmentId consignmentId;
    private final TenantId tenantId;

    public GetConsignmentQuery(ConsignmentId consignmentId, TenantId tenantId) {
        if (consignmentId == null) {
            throw new IllegalArgumentException("ConsignmentId is required");
        }
        if (tenantId == null) {
            throw new IllegalArgumentException("TenantId is required");
        }
        this.consignmentId = consignmentId;
        this.tenantId = tenantId;
    }
}

