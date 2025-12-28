package com.ccbsa.wms.stock.application.service.command.dto;

import java.time.LocalDateTime;

import com.ccbsa.wms.stock.domain.core.valueobject.AllocationStatus;
import com.ccbsa.wms.stock.domain.core.valueobject.StockAllocationId;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * Result DTO: ReleaseStockAllocationResult
 * <p>
 * Result for releasing a stock allocation.
 */
@Getter
@Builder
@EqualsAndHashCode
public final class ReleaseStockAllocationResult {
    private final StockAllocationId allocationId;
    private final AllocationStatus status;
    private final LocalDateTime releasedAt;
}

