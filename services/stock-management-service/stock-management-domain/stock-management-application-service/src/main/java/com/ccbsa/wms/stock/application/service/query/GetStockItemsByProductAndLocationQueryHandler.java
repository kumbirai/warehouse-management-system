package com.ccbsa.wms.stock.application.service.query;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ccbsa.wms.stock.application.service.port.repository.StockItemRepository;
import com.ccbsa.wms.stock.application.service.query.dto.GetStockItemQueryResult;
import com.ccbsa.wms.stock.application.service.query.dto.GetStockItemsByProductAndLocationQuery;
import com.ccbsa.wms.stock.domain.core.entity.StockItem;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Query Handler: GetStockItemsByProductAndLocationQueryHandler
 * <p>
 * Handles retrieval of stock items by product ID and location ID.
 * <p>
 * Responsibilities:
 * - Query stock items from repository by product and location
 * - Map stock items to query result DTOs
 * - Return list of stock items
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class GetStockItemsByProductAndLocationQueryHandler {
    private final StockItemRepository stockItemRepository;

    @Transactional(readOnly = true)
    public List<GetStockItemQueryResult> handle(GetStockItemsByProductAndLocationQuery query) {
        log.debug("Handling GetStockItemsByProductAndLocationQuery for product: {}, location: {}, tenant: {}", query.getProductId().getValueAsString(),
                query.getLocationId().getValueAsString(), query.getTenantId().getValue());

        // 1. Query stock items from repository
        List<StockItem> stockItems = stockItemRepository.findByTenantIdAndProductIdAndLocationId(query.getTenantId(), query.getProductId(), query.getLocationId());

        log.debug("Found {} stock item(s) for product: {} at location: {}", stockItems.size(), query.getProductId().getValueAsString(), query.getLocationId().getValueAsString());

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
