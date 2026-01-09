package com.ccbsa.wms.stock.application.service.query.dto;

import com.ccbsa.common.domain.valueobject.ProductId;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;

import lombok.Builder;
import lombok.Getter;

/**
 * Query DTO: CheckStockExpirationQuery
 * <p>
 * Query object for checking if stock is expired at a specific location.
 */
@Getter
@Builder
public final class CheckStockExpirationQuery {
    private final ProductId productId;
    private final LocationId locationId;
    private final TenantId tenantId;

    public CheckStockExpirationQuery(ProductId productId, LocationId locationId, TenantId tenantId) {
        if (productId == null) {
            throw new IllegalArgumentException("ProductId is required");
        }
        if (locationId == null) {
            throw new IllegalArgumentException("LocationId is required");
        }
        if (tenantId == null) {
            throw new IllegalArgumentException("TenantId is required");
        }
        this.productId = productId;
        this.locationId = locationId;
        this.tenantId = tenantId;
    }
}
