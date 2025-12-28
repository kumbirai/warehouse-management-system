package com.ccbsa.wms.stock.application.service.command.dto;

import com.ccbsa.common.domain.valueobject.AdjustmentReason;
import com.ccbsa.common.domain.valueobject.AdjustmentType;
import com.ccbsa.common.domain.valueobject.ProductId;
import com.ccbsa.common.domain.valueobject.Quantity;
import com.ccbsa.common.domain.valueobject.StockItemId;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.common.domain.valueobject.UserId;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * Command DTO: AdjustStockCommand
 * <p>
 * Command for adjusting stock levels.
 */
@Getter
@Builder
@AllArgsConstructor
@EqualsAndHashCode
public final class AdjustStockCommand {
    private final TenantId tenantId;
    private final ProductId productId;
    private final LocationId locationId; // Optional - null for product-wide adjustment
    private final StockItemId stockItemId; // Optional - null for product/location adjustment
    private final AdjustmentType adjustmentType;
    private final Quantity quantity;
    private final AdjustmentReason reason;
    private final String notes;
    private final UserId userId;
    private final String authorizationCode; // For large adjustments
}

