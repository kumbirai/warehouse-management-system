package com.ccbsa.wms.location.application.service.query.dto;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;
import com.ccbsa.wms.location.domain.core.valueobject.MovementStatus;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * Query DTO: ListStockMovementsQuery
 * <p>
 * Query for listing stock movements with optional filters.
 */
@Getter
@Builder
@EqualsAndHashCode
public final class ListStockMovementsQuery {
    private final TenantId tenantId;
    private final String stockItemId;
    private final LocationId sourceLocationId;
    private final LocationId destinationLocationId;
    private final MovementStatus status;
}

