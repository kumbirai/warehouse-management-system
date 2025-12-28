package com.ccbsa.wms.stock.application.service.query;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ccbsa.wms.location.domain.core.valueobject.LocationId;
import com.ccbsa.wms.stock.application.service.port.repository.StockAllocationRepository;
import com.ccbsa.wms.stock.application.service.port.repository.StockItemRepository;
import com.ccbsa.wms.stock.application.service.query.dto.GetStockLevelsQuery;
import com.ccbsa.wms.stock.application.service.query.dto.GetStockLevelsQueryResult;
import com.ccbsa.wms.stock.domain.core.entity.StockAllocation;
import com.ccbsa.wms.stock.domain.core.entity.StockItem;
import com.ccbsa.wms.stock.domain.core.valueobject.AllocationStatus;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Query Handler: GetStockLevelsQueryHandler
 * <p>
 * Handles retrieval of stock levels by aggregating stock items and allocations.
 * <p>
 * Responsibilities:
 * - Query stock items by product (and optionally location)
 * - Query allocations for the same product/location
 * - Calculate total, allocated, and available quantities
 * - Return aggregated stock level information
 * <p>
 * Stock levels are calculated on-the-fly from stock items and allocations.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class GetStockLevelsQueryHandler {
    private final StockItemRepository stockItemRepository;
    private final StockAllocationRepository allocationRepository;

    @Transactional(readOnly = true)
    public GetStockLevelsQueryResult handle(GetStockLevelsQuery query) {
        log.debug("Handling GetStockLevelsQuery for product: {}, location: {}", query.getProductId(), query.getLocationId());

        // 1. Query stock items
        List<StockItem> stockItems =
                query.getLocationId() != null ? stockItemRepository.findByTenantIdAndProductIdAndLocationId(query.getTenantId(), query.getProductId(), query.getLocationId())
                        : stockItemRepository.findByTenantIdAndProductId(query.getTenantId(), query.getProductId());

        // 2. If no stock items exist, return empty list (not an error)
        if (stockItems.isEmpty()) {
            log.debug("No stock items found for product: {}, location: {}", query.getProductId(), query.getLocationId());
            return GetStockLevelsQueryResult.builder().stockLevels(List.of()).build();
        }

        // 3. Group stock items by location for aggregation
        // Use a special key for items without location (null locationId)
        Map<String, List<StockItem>> itemsByLocation = stockItems.stream()
                .collect(Collectors.groupingBy(item -> item.getLocationId() != null ? item.getLocationId().getValueAsString() : "UNASSIGNED", // Group unassigned items together
                        Collectors.toList()));

        // 4. Calculate stock levels per location
        List<GetStockLevelsQueryResult.StockLevelResult> stockLevels = new ArrayList<>();

        for (Map.Entry<String, List<StockItem>> entry : itemsByLocation.entrySet()) {
            String locationIdStr = entry.getKey();
            List<StockItem> locationItems = entry.getValue();

            // Skip unassigned items if query specifies a location
            if ("UNASSIGNED".equals(locationIdStr) && query.getLocationId() != null) {
                continue;
            }

            LocationId locationId = "UNASSIGNED".equals(locationIdStr) ? null : LocationId.of(UUID.fromString(locationIdStr));

            // Calculate total quantity from stock items
            int totalQuantity = locationItems.stream().mapToInt(item -> item.getQuantity().getValue()).sum();

            // Query allocations for this product
            // If query specifies a location, only process matching locations
            if (query.getLocationId() != null && locationId != null && !query.getLocationId().equals(locationId)) {
                // Query specified a location but this location doesn't match - skip it
                continue;
            }

            // Get allocations - if locationId is available, use it for filtering
            List<StockAllocation> allocations;
            if (locationId != null) {
                allocations = allocationRepository.findByTenantIdAndProductIdAndLocationId(query.getTenantId(), query.getProductId(), locationId);
            } else {
                // No locationId - get all allocations for product, then filter by stock items
                allocations = allocationRepository.findByTenantIdAndProductId(query.getTenantId(), query.getProductId());
            }

            // Filter allocations for this location's stock items (only count allocations for items in this location)
            List<String> locationStockItemIds = locationItems.stream().map(item -> item.getId().getValueAsString()).collect(Collectors.toList());

            int allocatedQuantity = allocations.stream()
                    .filter(allocation -> allocation.getStatus() == AllocationStatus.ALLOCATED && locationStockItemIds.contains(allocation.getStockItemId().getValueAsString()))
                    .mapToInt(allocation -> allocation.getQuantity().getValue()).sum();

            // Calculate available quantity
            @SuppressFBWarnings(value = "DLS_DEAD_LOCAL_STORE", justification = "availableQuantity is used in builder - SpotBugs false positive") int availableQuantity =
                    Math.max(0, totalQuantity - allocatedQuantity);

            // Build stock level result
            GetStockLevelsQueryResult.StockLevelResult stockLevel = GetStockLevelsQueryResult.StockLevelResult.builder().productId(query.getProductId().getValueAsString())
                    .locationId(locationId != null ? locationId.getValueAsString() : null).totalQuantity(totalQuantity).allocatedQuantity(allocatedQuantity)
                    .availableQuantity(availableQuantity).minimumQuantity(null) // TODO: Load from StockLevelThreshold if needed
                    .maximumQuantity(null) // TODO: Load from StockLevelThreshold if needed
                    .build();

            stockLevels.add(stockLevel);
        }

        log.debug("Calculated {} stock level(s) for product: {}", stockLevels.size(), query.getProductId());

        return GetStockLevelsQueryResult.builder().stockLevels(stockLevels).build();
    }
}
