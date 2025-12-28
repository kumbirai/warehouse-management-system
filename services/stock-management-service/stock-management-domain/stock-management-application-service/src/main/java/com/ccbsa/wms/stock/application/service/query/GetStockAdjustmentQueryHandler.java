package com.ccbsa.wms.stock.application.service.query;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ccbsa.wms.stock.application.service.port.data.StockAdjustmentViewRepository;
import com.ccbsa.wms.stock.application.service.query.dto.GetStockAdjustmentQuery;
import com.ccbsa.wms.stock.application.service.query.dto.GetStockAdjustmentQueryResult;
import com.ccbsa.wms.stock.domain.core.exception.StockAdjustmentNotFoundException;

import lombok.RequiredArgsConstructor;

/**
 * Query Handler: GetStockAdjustmentQueryHandler
 * <p>
 * Handles retrieval of StockAdjustment read model by ID.
 * <p>
 * Responsibilities:
 * - Load StockAdjustment view from data port (read model)
 * - Map view to query result DTO
 * - Return optimized read model
 * <p>
 * Uses data port (StockAdjustmentViewRepository) instead of repository port for CQRS compliance.
 */
@Component
@RequiredArgsConstructor
public class GetStockAdjustmentQueryHandler {
    private final StockAdjustmentViewRepository viewRepository;

    @Transactional(readOnly = true)
    public GetStockAdjustmentQueryResult handle(GetStockAdjustmentQuery query) {
        // 1. Load read model (view) from data port
        var adjustmentView = viewRepository.findByTenantIdAndId(query.getTenantId(), query.getAdjustmentId())
                .orElseThrow(() -> new StockAdjustmentNotFoundException(String.format("Stock adjustment not found: %s", query.getAdjustmentId().getValueAsString())));

        // 2. Map view to query result
        return GetStockAdjustmentQueryResult.builder().adjustmentId(adjustmentView.getAdjustmentId()).productId(adjustmentView.getProductId())
                .locationId(adjustmentView.getLocationId()).stockItemId(adjustmentView.getStockItemId()).adjustmentType(adjustmentView.getAdjustmentType())
                .quantity(adjustmentView.getQuantity()).quantityBefore(adjustmentView.getQuantityBefore()).quantityAfter(adjustmentView.getQuantityAfter())
                .reason(adjustmentView.getReason()).notes(adjustmentView.getNotes()).adjustedBy(adjustmentView.getAdjustedBy())
                .authorizationCode(adjustmentView.getAuthorizationCode()).adjustedAt(adjustmentView.getAdjustedAt()).build();
    }
}

