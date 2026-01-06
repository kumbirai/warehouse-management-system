package com.ccbsa.wms.stock.application.service.port.data.dto;

import java.math.BigDecimal;

import com.ccbsa.common.domain.valueobject.ProductId;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;
import com.ccbsa.wms.stock.domain.core.valueobject.StockLevelThresholdId;

import lombok.Builder;
import lombok.Getter;

/**
 * Read Model DTO: StockLevelThresholdView
 * <p>
 * Optimized read model representation of StockLevelThreshold aggregate for query operations.
 * <p>
 * This is a denormalized view optimized for read queries, separate from the write model.
 */
@Getter
@Builder
public final class StockLevelThresholdView {
    private final StockLevelThresholdId thresholdId;
    private final String tenantId;
    private final ProductId productId;
    private final LocationId locationId;
    private final BigDecimal minimumQuantity;
    private final BigDecimal maximumQuantity;
    private final boolean enableAutoRestock;

    public StockLevelThresholdView(StockLevelThresholdId thresholdId, String tenantId, ProductId productId, LocationId locationId, BigDecimal minimumQuantity,
                                   BigDecimal maximumQuantity, boolean enableAutoRestock) {
        if (thresholdId == null) {
            throw new IllegalArgumentException("ThresholdId is required");
        }
        if (tenantId == null) {
            throw new IllegalArgumentException("TenantId is required");
        }
        if (productId == null) {
            throw new IllegalArgumentException("ProductId is required");
        }
        this.thresholdId = thresholdId;
        this.tenantId = tenantId;
        this.productId = productId;
        this.locationId = locationId;
        this.minimumQuantity = minimumQuantity;
        this.maximumQuantity = maximumQuantity;
        this.enableAutoRestock = enableAutoRestock;
    }
}
