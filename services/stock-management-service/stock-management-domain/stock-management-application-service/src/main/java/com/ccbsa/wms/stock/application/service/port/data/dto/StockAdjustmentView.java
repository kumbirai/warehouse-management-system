package com.ccbsa.wms.stock.application.service.port.data.dto;

import java.time.LocalDateTime;

import com.ccbsa.common.domain.valueobject.AdjustmentReason;
import com.ccbsa.common.domain.valueobject.AdjustmentType;
import com.ccbsa.common.domain.valueobject.ProductId;
import com.ccbsa.common.domain.valueobject.Quantity;
import com.ccbsa.common.domain.valueobject.StockItemId;
import com.ccbsa.common.domain.valueobject.UserId;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;
import com.ccbsa.wms.stock.domain.core.valueobject.StockAdjustmentId;

import lombok.Builder;
import lombok.Getter;

/**
 * Read Model DTO: StockAdjustmentView
 * <p>
 * Optimized read model representation of StockAdjustment aggregate for query operations.
 */
@Getter
@Builder
public final class StockAdjustmentView {
    private final StockAdjustmentId adjustmentId;
    private final String tenantId;
    private final ProductId productId;
    private final LocationId locationId;
    private final StockItemId stockItemId;
    private final AdjustmentType adjustmentType;
    private final Quantity quantity;
    private final int quantityBefore;
    private final int quantityAfter;
    private final AdjustmentReason reason;
    private final String notes;
    private final UserId adjustedBy;
    private final String authorizationCode;
    private final LocalDateTime adjustedAt;

    public StockAdjustmentView(StockAdjustmentId adjustmentId, String tenantId, ProductId productId, LocationId locationId, StockItemId stockItemId, AdjustmentType adjustmentType,
                               Quantity quantity, int quantityBefore, int quantityAfter, AdjustmentReason reason, String notes, UserId adjustedBy, String authorizationCode,
                               LocalDateTime adjustedAt) {
        if (adjustmentId == null) {
            throw new IllegalArgumentException("AdjustmentId is required");
        }
        if (tenantId == null) {
            throw new IllegalArgumentException("TenantId is required");
        }
        if (productId == null) {
            throw new IllegalArgumentException("ProductId is required");
        }
        if (adjustmentType == null) {
            throw new IllegalArgumentException("AdjustmentType is required");
        }
        if (quantity == null) {
            throw new IllegalArgumentException("Quantity is required");
        }
        if (reason == null) {
            throw new IllegalArgumentException("Reason is required");
        }
        this.adjustmentId = adjustmentId;
        this.tenantId = tenantId;
        this.productId = productId;
        this.locationId = locationId;
        this.stockItemId = stockItemId;
        this.adjustmentType = adjustmentType;
        this.quantity = quantity;
        this.quantityBefore = quantityBefore;
        this.quantityAfter = quantityAfter;
        this.reason = reason;
        this.notes = notes;
        this.adjustedBy = adjustedBy;
        this.authorizationCode = authorizationCode;
        this.adjustedAt = adjustedAt;
    }
}

