package com.ccbsa.wms.stock.application.service.command.dto;

import java.time.LocalDateTime;

import com.ccbsa.common.domain.valueobject.AdjustmentReason;
import com.ccbsa.common.domain.valueobject.AdjustmentType;
import com.ccbsa.common.domain.valueobject.ProductId;
import com.ccbsa.common.domain.valueobject.Quantity;
import com.ccbsa.common.domain.valueobject.StockItemId;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;
import com.ccbsa.wms.stock.domain.core.valueobject.StockAdjustmentId;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * Result DTO: AdjustStockResult
 * <p>
 * Result for adjusting stock.
 */
@Getter
@Builder
@EqualsAndHashCode
public final class AdjustStockResult {
    private final StockAdjustmentId adjustmentId;
    private final ProductId productId;
    private final LocationId locationId;
    private final StockItemId stockItemId;
    private final AdjustmentType adjustmentType;
    private final Quantity quantity;
    private final int quantityBefore;
    private final int quantityAfter;
    private final AdjustmentReason reason;
    private final String notes;
    private final LocalDateTime adjustedAt;
}

