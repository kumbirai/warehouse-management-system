package com.ccbsa.wms.stock.application.service.query;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ccbsa.wms.stock.application.service.port.repository.StockItemRepository;
import com.ccbsa.wms.stock.application.service.query.dto.GetStockItemQueryResult;
import com.ccbsa.wms.stock.application.service.query.dto.GetStockItemsByClassificationQuery;
import com.ccbsa.wms.stock.domain.core.entity.StockItem;

/**
 * Query Handler: GetStockItemsByClassificationQueryHandler
 * <p>
 * Handles retrieval of stock items by classification.
 * <p>
 * Responsibilities:
 * - Load stock items from repository by classification
 * - Map aggregates to query result DTOs
 * - Return optimized read model
 */
@Component
public class GetStockItemsByClassificationQueryHandler {
    private final StockItemRepository repository;

    public GetStockItemsByClassificationQueryHandler(StockItemRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<GetStockItemQueryResult> handle(GetStockItemsByClassificationQuery query) {
        // 1. Load aggregates
        List<StockItem> stockItems = repository.findByClassification(query.getClassification(), query.getTenantId());

        // 2. Map to query results
        return stockItems.stream()
                .map(stockItem -> GetStockItemQueryResult.builder().stockItemId(stockItem.getId()).productId(stockItem.getProductId()).locationId(stockItem.getLocationId())
                        .quantity(stockItem.getQuantity()).expirationDate(stockItem.getExpirationDate()).classification(stockItem.getClassification())
                        .consignmentId(stockItem.getConsignmentId()).createdAt(stockItem.getCreatedAt()).lastModifiedAt(stockItem.getLastModifiedAt()).build())
                .collect(Collectors.toList());
    }
}

