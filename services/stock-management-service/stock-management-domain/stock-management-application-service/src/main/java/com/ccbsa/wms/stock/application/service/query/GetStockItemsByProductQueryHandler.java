package com.ccbsa.wms.stock.application.service.query;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ccbsa.wms.stock.application.service.port.repository.StockItemRepository;
import com.ccbsa.wms.stock.application.service.query.dto.GetStockItemQueryResult;
import com.ccbsa.wms.stock.application.service.query.dto.GetStockItemsByProductQuery;
import com.ccbsa.wms.stock.domain.core.entity.StockItem;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Query Handler: GetStockItemsByProductQueryHandler
 * <p>
 * Handles retrieval of stock items by product ID only (including items without location assignment).
 * <p>
 * Responsibilities:
 * - Query stock items from repository by product
 * - Map stock items to query result DTOs
 * - Return list of stock items
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class GetStockItemsByProductQueryHandler {
    private final StockItemRepository stockItemRepository;

    @Transactional(readOnly = true)
    public List<GetStockItemQueryResult> handle(GetStockItemsByProductQuery query) {
        log.debug("Handling GetStockItemsByProductQuery for product: {}, tenant: {}", query.getProductId().getValueAsString(), query.getTenantId().getValue());

        // 1. Query stock items from repository by product
        List<StockItem> stockItems = stockItemRepository.findByTenantIdAndProductId(query.getTenantId(), query.getProductId());

        log.debug("Found {} stock item(s) for product: {}", stockItems.size(), query.getProductId().getValueAsString());

        // 2. Map stock items to query results
        return stockItems.stream().map(this::mapToQueryResult).collect(Collectors.toList());
    }

    /**
     * Maps a StockItem aggregate to GetStockItemQueryResult.
     *
     * @param stockItem Stock item aggregate
     * @return Query result DTO
     */
    private GetStockItemQueryResult mapToQueryResult(StockItem stockItem) {
        return GetStockItemQueryResult.builder().stockItemId(stockItem.getId()).productId(stockItem.getProductId()).locationId(stockItem.getLocationId())
                .quantity(stockItem.getQuantity()).expirationDate(stockItem.getExpirationDate()).classification(stockItem.getClassification())
                .consignmentId(stockItem.getConsignmentId()).createdAt(stockItem.getCreatedAt()).lastModifiedAt(stockItem.getLastModifiedAt()).build();
    }
}
