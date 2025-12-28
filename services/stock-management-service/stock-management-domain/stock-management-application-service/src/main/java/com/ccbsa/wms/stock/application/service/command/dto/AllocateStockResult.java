package com.ccbsa.wms.stock.application.service.command.dto;

import java.time.LocalDateTime;

import com.ccbsa.common.domain.valueobject.AllocationType;
import com.ccbsa.common.domain.valueobject.ProductId;
import com.ccbsa.common.domain.valueobject.Quantity;
import com.ccbsa.common.domain.valueobject.StockItemId;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;
import com.ccbsa.wms.stock.domain.core.valueobject.AllocationStatus;
import com.ccbsa.wms.stock.domain.core.valueobject.StockAllocationId;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * Result DTO: AllocateStockResult
 * <p>
 * Result for allocating stock.
 */
@Getter
@Builder
@EqualsAndHashCode
public final class AllocateStockResult {
    private final StockAllocationId allocationId;
    private final ProductId productId;
    private final LocationId locationId;
    private final StockItemId stockItemId;
    private final Quantity quantity;
    private final AllocationType allocationType;
    private final String referenceId;
    private final AllocationStatus status;
    private final LocalDateTime allocatedAt;
}

