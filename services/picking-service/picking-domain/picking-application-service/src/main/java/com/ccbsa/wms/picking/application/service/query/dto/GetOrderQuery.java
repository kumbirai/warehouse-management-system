package com.ccbsa.wms.picking.application.service.query.dto;

import com.ccbsa.common.domain.valueobject.OrderNumber;
import com.ccbsa.common.domain.valueobject.TenantId;

import lombok.Builder;
import lombok.Getter;

/**
 * Query DTO: GetOrderQuery
 * <p>
 * Query object for getting an order by order number.
 */
@Getter
@Builder
public final class GetOrderQuery {
    private final TenantId tenantId;
    private final OrderNumber orderNumber;

    public GetOrderQuery(TenantId tenantId, OrderNumber orderNumber) {
        if (tenantId == null) {
            throw new IllegalArgumentException("TenantId is required");
        }
        if (orderNumber == null) {
            throw new IllegalArgumentException("OrderNumber is required");
        }
        this.tenantId = tenantId;
        this.orderNumber = orderNumber;
    }
}
