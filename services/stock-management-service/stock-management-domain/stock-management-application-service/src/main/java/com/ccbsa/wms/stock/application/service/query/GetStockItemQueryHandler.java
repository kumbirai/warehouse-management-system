package com.ccbsa.wms.stock.application.service.query;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ccbsa.wms.stock.application.service.port.repository.StockItemRepository;
import com.ccbsa.wms.stock.application.service.query.dto.GetStockItemQuery;
import com.ccbsa.wms.stock.application.service.query.dto.GetStockItemQueryResult;
import com.ccbsa.wms.stock.domain.core.entity.StockItem;
import com.ccbsa.wms.stock.domain.core.exception.StockItemNotFoundException;

/**
 * Query Handler: GetStockItemQueryHandler
 * <p>
 * Handles retrieval of StockItem aggregate by ID.
 * <p>
 * Responsibilities:
 * - Load StockItem aggregate from repository
 * - Map aggregate to query result DTO
 * - Return optimized read model
 */
@Component
public class GetStockItemQueryHandler {
    private final StockItemRepository repository;

    public GetStockItemQueryHandler(StockItemRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public GetStockItemQueryResult handle(GetStockItemQuery query) {
        // 1. Load aggregate
        StockItem stockItem = repository.findById(query.getStockItemId(), query.getTenantId())
                .orElseThrow(() -> new StockItemNotFoundException(String.format("Stock item not found: %s", query.getStockItemId().getValueAsString())));

        // 2. Map to query result
        return GetStockItemQueryResult.builder().stockItemId(stockItem.getId()).productId(stockItem.getProductId()).locationId(stockItem.getLocationId())
                .quantity(stockItem.getQuantity()).expirationDate(stockItem.getExpirationDate()).classification(stockItem.getClassification())
                .consignmentId(stockItem.getConsignmentId()).createdAt(stockItem.getCreatedAt()).lastModifiedAt(stockItem.getLastModifiedAt()).build();
    }
}

