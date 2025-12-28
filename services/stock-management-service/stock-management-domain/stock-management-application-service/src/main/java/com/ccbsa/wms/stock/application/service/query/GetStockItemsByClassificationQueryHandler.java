package com.ccbsa.wms.stock.application.service.query;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ccbsa.wms.stock.application.service.port.data.StockItemViewRepository;
import com.ccbsa.wms.stock.application.service.query.dto.GetStockItemQueryResult;
import com.ccbsa.wms.stock.application.service.query.dto.GetStockItemsByClassificationQuery;

import lombok.RequiredArgsConstructor;

/**
 * Query Handler: GetStockItemsByClassificationQueryHandler
 * <p>
 * Handles retrieval of stock item views by classification.
 * <p>
 * Responsibilities:
 * - Load stock item views from data port (read model) by classification
 * - Map views to query result DTOs
 * - Return optimized read model
 * <p>
 * Uses data port (StockItemViewRepository) instead of repository port for CQRS compliance.
 */
@Component
@RequiredArgsConstructor
public class GetStockItemsByClassificationQueryHandler {
    private final StockItemViewRepository viewRepository;

    @Transactional(readOnly = true)
    public List<GetStockItemQueryResult> handle(GetStockItemsByClassificationQuery query) {
        // 1. Load read models (views) from data port
        var stockItemViews = viewRepository.findByTenantIdAndClassification(query.getTenantId(), query.getClassification());

        // 2. Map views to query results
        return stockItemViews.stream()
                .map(view -> GetStockItemQueryResult.builder().stockItemId(view.getStockItemId()).productId(view.getProductId()).locationId(view.getLocationId())
                        .quantity(view.getQuantity()).expirationDate(view.getExpirationDate()).classification(view.getClassification()).consignmentId(view.getConsignmentId())
                        .createdAt(view.getCreatedAt()).lastModifiedAt(view.getLastModifiedAt()).build()).collect(Collectors.toList());
    }
}

