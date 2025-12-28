package com.ccbsa.wms.stock.application.service.query.dto;

import java.time.LocalDateTime;

import com.ccbsa.common.domain.valueobject.AllocationType;
import com.ccbsa.common.domain.valueobject.ProductId;
import com.ccbsa.common.domain.valueobject.Quantity;
import com.ccbsa.common.domain.valueobject.StockItemId;
import com.ccbsa.common.domain.valueobject.UserId;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;
import com.ccbsa.wms.stock.domain.core.valueobject.AllocationStatus;
import com.ccbsa.wms.stock.domain.core.valueobject.StockAllocationId;

import lombok.Builder;
import lombok.Getter;

/**
 * Query Result DTO: GetStockAllocationQueryResult
 * <p>
 * Query result object for stock allocation retrieval.
 */
@Getter
@Builder
public final class GetStockAllocationQueryResult {
    private final StockAllocationId allocationId;
    private final ProductId productId;
    private final LocationId locationId;
    private final StockItemId stockItemId;
    private final Quantity quantity;
    private final AllocationType allocationType;
    private final String referenceId;
    private final AllocationStatus status;
    private final LocalDateTime allocatedAt;
    private final LocalDateTime releasedAt;
    private final UserId allocatedBy;
    private final String notes;

    public GetStockAllocationQueryResult(StockAllocationId allocationId, ProductId productId, LocationId locationId, StockItemId stockItemId, Quantity quantity,
                                         AllocationType allocationType, String referenceId, AllocationStatus status, LocalDateTime allocatedAt, LocalDateTime releasedAt,
                                         UserId allocatedBy, String notes) {
        if (allocationId == null) {
            throw new IllegalArgumentException("AllocationId is required");
        }
        if (productId == null) {
            throw new IllegalArgumentException("ProductId is required");
        }
        if (quantity == null) {
            throw new IllegalArgumentException("Quantity is required");
        }
        if (allocationType == null) {
            throw new IllegalArgumentException("AllocationType is required");
        }
        if (status == null) {
            throw new IllegalArgumentException("Status is required");
        }
        this.allocationId = allocationId;
        this.productId = productId;
        this.locationId = locationId;
        this.stockItemId = stockItemId;
        this.quantity = quantity;
        this.allocationType = allocationType;
        this.referenceId = referenceId;
        this.status = status;
        this.allocatedAt = allocatedAt;
        this.releasedAt = releasedAt;
        this.allocatedBy = allocatedBy;
        this.notes = notes;
    }
}

