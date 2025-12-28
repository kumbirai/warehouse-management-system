package com.ccbsa.wms.stock.application.service.query.dto;

import com.ccbsa.common.domain.valueobject.ProductId;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;

import lombok.Builder;
import lombok.Getter;

/**
 * Query DTO: GetStockLevelsQuery
 * <p>
 * Query for retrieving stock levels by product and optionally location.
 */
@Getter
@Builder
public final class GetStockLevelsQuery {
    private final TenantId tenantId;
    private final ProductId productId;
    private final LocationId locationId; // Optional - null means all locations

    public GetStockLevelsQuery(TenantId tenantId, ProductId productId, LocationId locationId) {
        if (tenantId == null) {
            throw new IllegalArgumentException("TenantId is required");
        }
        if (productId == null) {
            throw new IllegalArgumentException("ProductId is required");
        }
        this.tenantId = tenantId;
        this.productId = productId;
        this.locationId = locationId;
    }
}
