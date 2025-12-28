package com.ccbsa.wms.stock.application.service.query;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ccbsa.wms.stock.application.service.port.data.StockAllocationViewRepository;
import com.ccbsa.wms.stock.application.service.query.dto.GetStockAllocationQuery;
import com.ccbsa.wms.stock.application.service.query.dto.GetStockAllocationQueryResult;
import com.ccbsa.wms.stock.domain.core.exception.StockAllocationNotFoundException;

import lombok.RequiredArgsConstructor;

/**
 * Query Handler: GetStockAllocationQueryHandler
 * <p>
 * Handles retrieval of StockAllocation read model by ID.
 * <p>
 * Responsibilities:
 * - Load StockAllocation view from data port (read model)
 * - Map view to query result DTO
 * - Return optimized read model
 * <p>
 * Uses data port (StockAllocationViewRepository) instead of repository port for CQRS compliance.
 */
@Component
@RequiredArgsConstructor
public class GetStockAllocationQueryHandler {
    private final StockAllocationViewRepository viewRepository;

    @Transactional(readOnly = true)
    public GetStockAllocationQueryResult handle(GetStockAllocationQuery query) {
        // 1. Load read model (view) from data port
        var allocationView = viewRepository.findByTenantIdAndId(query.getTenantId(), query.getAllocationId())
                .orElseThrow(() -> new StockAllocationNotFoundException(String.format("Stock allocation not found: %s", query.getAllocationId().getValueAsString())));

        // 2. Map view to query result
        return GetStockAllocationQueryResult.builder().allocationId(allocationView.getAllocationId()).productId(allocationView.getProductId())
                .locationId(allocationView.getLocationId()).stockItemId(allocationView.getStockItemId()).quantity(allocationView.getQuantity())
                .allocationType(allocationView.getAllocationType()).referenceId(allocationView.getReferenceId()).status(allocationView.getStatus())
                .allocatedAt(allocationView.getAllocatedAt()).releasedAt(allocationView.getReleasedAt()).allocatedBy(allocationView.getAllocatedBy())
                .notes(allocationView.getNotes()).build();
    }
}

