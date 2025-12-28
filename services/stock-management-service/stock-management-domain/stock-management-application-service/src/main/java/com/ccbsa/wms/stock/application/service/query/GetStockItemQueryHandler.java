package com.ccbsa.wms.stock.application.service.query;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ccbsa.wms.stock.application.service.port.data.StockItemViewRepository;
import com.ccbsa.wms.stock.application.service.query.dto.GetStockItemQuery;
import com.ccbsa.wms.stock.application.service.query.dto.GetStockItemQueryResult;
import com.ccbsa.wms.stock.domain.core.exception.StockItemNotFoundException;

import lombok.RequiredArgsConstructor;

/**
 * Query Handler: GetStockItemQueryHandler
 * <p>
 * Handles retrieval of StockItem read model by ID.
 * <p>
 * Responsibilities:
 * - Load StockItem view from data port (read model)
 * - Map view to query result DTO
 * - Return optimized read model
 * <p>
 * Uses data port (StockItemViewRepository) instead of repository port for CQRS compliance.
 */
@Component
@RequiredArgsConstructor
public class GetStockItemQueryHandler {
    private final StockItemViewRepository viewRepository;

    @Transactional(readOnly = true)
    public GetStockItemQueryResult handle(GetStockItemQuery query) {
        // 1. Load read model (view) from data port
        var stockItemView = viewRepository.findByTenantIdAndId(query.getTenantId(), query.getStockItemId())
                .orElseThrow(() -> new StockItemNotFoundException(String.format("Stock item not found: %s", query.getStockItemId().getValueAsString())));

        // 2. Map view to query result
        return GetStockItemQueryResult.builder().stockItemId(stockItemView.getStockItemId()).productId(stockItemView.getProductId()).locationId(stockItemView.getLocationId())
                .quantity(stockItemView.getQuantity()).expirationDate(stockItemView.getExpirationDate()).classification(stockItemView.getClassification())
                .consignmentId(stockItemView.getConsignmentId()).createdAt(stockItemView.getCreatedAt()).lastModifiedAt(stockItemView.getLastModifiedAt()).build();
    }
}

