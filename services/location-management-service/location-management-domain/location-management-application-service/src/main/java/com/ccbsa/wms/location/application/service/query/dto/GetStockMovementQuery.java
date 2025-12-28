package com.ccbsa.wms.location.application.service.query.dto;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.location.domain.core.valueobject.StockMovementId;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * Query DTO: GetStockMovementQuery
 * <p>
 * Query for retrieving a single stock movement.
 */
@Getter
@Builder
@EqualsAndHashCode
public final class GetStockMovementQuery {
    private final TenantId tenantId;
    private final StockMovementId stockMovementId;
}

