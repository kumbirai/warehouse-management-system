package com.ccbsa.wms.stock.application.service.query;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ccbsa.common.domain.valueobject.ProductId;
import com.ccbsa.wms.stock.application.service.query.dto.GetFEFOStockItemsQuery;
import com.ccbsa.wms.stock.application.service.query.dto.GetStockItemQueryResult;
import com.ccbsa.wms.stock.application.service.query.dto.QueryStockAvailabilityForProductsQuery;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Query Handler: QueryStockAvailabilityForProductsQueryHandler
 * <p>
 * Handles retrieval of stock availability for multiple products using FEFO principles.
 * <p>
 * Responsibilities:
 * - Query stock items for multiple products using FEFO handler
 * - Filter by available quantity and exclude expired stock
 * - Return stock availability grouped by product
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class QueryStockAvailabilityForProductsQueryHandler {
    private final GetFEFOStockItemsQueryHandler fefoStockItemsQueryHandler;

    @Transactional(readOnly = true)
    public Map<ProductId, List<GetStockItemQueryResult>> handle(QueryStockAvailabilityForProductsQuery query) {
        log.debug("Handling QueryStockAvailabilityForProductsQuery for {} products, tenant: {}", query.getProductQuantities().size(), query.getTenantId().getValue());

        Map<ProductId, List<GetStockItemQueryResult>> result = new HashMap<>();

        // Query stock availability for each product using FEFO handler
        for (Map.Entry<ProductId, Integer> entry : query.getProductQuantities().entrySet()) {
            ProductId productId = entry.getKey();
            Integer requiredQuantity = entry.getValue();

            log.debug("Querying stock availability for product: {}, required quantity: {}", productId.getValueAsString(), requiredQuantity);

            // Use FEFO handler to get available stock items
            GetFEFOStockItemsQuery fefoQuery = GetFEFOStockItemsQuery.builder().tenantId(query.getTenantId()).productId(productId).locationId(null) // All locations for FEFO
                    .build();

            List<GetStockItemQueryResult> stockItems = fefoStockItemsQueryHandler.handle(fefoQuery);

            // Limit to required quantity (take first N items that satisfy the quantity)
            List<GetStockItemQueryResult> limitedItems = limitToQuantity(stockItems, requiredQuantity);

            result.put(productId, limitedItems);

            log.debug("Found {} stock items for product: {}", limitedItems.size(), productId.getValueAsString());
        }

        return result;
    }

    /**
     * Limits stock items to satisfy the required quantity.
     * Takes items in FEFO order until quantity requirement is met.
     */
    private List<GetStockItemQueryResult> limitToQuantity(List<GetStockItemQueryResult> stockItems, Integer requiredQuantity) {
        int remainingQuantity = requiredQuantity;
        List<GetStockItemQueryResult> selectedItems = new java.util.ArrayList<>();

        for (GetStockItemQueryResult item : stockItems) {
            if (remainingQuantity <= 0) {
                break;
            }

            int availableQuantity = item.getQuantity().getValue() - (item.getAllocatedQuantity() != null ? item.getAllocatedQuantity().getValue() : 0);

            if (availableQuantity > 0) {
                selectedItems.add(item);
                remainingQuantity -= availableQuantity;
            }
        }

        return selectedItems;
    }
}
