package com.ccbsa.wms.stock.application.service.query;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ccbsa.common.domain.valueobject.StockClassification;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;
import com.ccbsa.wms.stock.application.service.port.data.StockItemViewRepository;
import com.ccbsa.wms.stock.application.service.port.data.dto.StockItemView;
import com.ccbsa.wms.stock.application.service.port.service.LocationServicePort;
import com.ccbsa.wms.stock.application.service.port.service.ProductServicePort;
import com.ccbsa.wms.stock.application.service.query.dto.GetFEFOStockItemsQuery;
import com.ccbsa.wms.stock.application.service.query.dto.GetStockItemQueryResult;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Query Handler: GetFEFOStockItemsQueryHandler
 * <p>
 * Handles retrieval of stock items sorted by FEFO (First-Expired, First-Out) principles.
 * <p>
 * Responsibilities:
 * - Query stock items from read model by product (and optionally location) (CQRS compliant)
 * - Filter by available quantity > 0 and exclude expired stock
 * - Sort by expiration date (earliest first), then by received date (earliest first)
 * - Enrich with product and location information for user-friendly display
 * - Map stock item views to query result DTOs
 * - Return list of stock items sorted by FEFO
 * <p>
 * FEFO Algorithm:
 * 1. Query stock items by product ID (and location if specified)
 * 2. Filter by available quantity > 0
 * 3. Exclude expired stock items
 * 4. Sort by expiration date (earliest first)
 * 5. If expiration dates equal, sort by received date (earliest first)
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class GetFEFOStockItemsQueryHandler {
    private final StockItemViewRepository stockItemViewRepository;
    private final ProductServicePort productServicePort;
    private final LocationServicePort locationServicePort;

    @Transactional(readOnly = true)
    public List<GetStockItemQueryResult> handle(GetFEFOStockItemsQuery query) {
        log.debug("Handling GetFEFOStockItemsQuery for product: {}, location: {}, tenant: {}", query.getProductId().getValueAsString(),
                query.getLocationId() != null ? query.getLocationId().getValueAsString() : "all locations", query.getTenantId().getValue());

        // 1. Query stock items from read model (CQRS compliant)
        List<StockItemView> stockItemViews;
        if (query.getLocationId() != null) {
            stockItemViews = stockItemViewRepository.findByTenantIdAndProductIdAndLocationId(query.getTenantId(), query.getProductId(), query.getLocationId());
        } else {
            stockItemViews = stockItemViewRepository.findByTenantIdAndProductId(query.getTenantId(), query.getProductId());
        }

        log.debug("Found {} stock item(s) for product: {} at location: {}", stockItemViews.size(), query.getProductId().getValueAsString(),
                query.getLocationId() != null ? query.getLocationId().getValueAsString() : "all locations");

        // 2. Filter by available quantity > 0, exclude expired stock, and filter to BIN locations or unassigned
        List<StockItemView> filteredItems = stockItemViews.stream().filter(item -> {
            // Exclude expired stock items
            if (item.getClassification() == StockClassification.EXPIRED) {
                return false;
            }
            // Calculate available quantity (total - allocated)
            int availableQuantity = item.getQuantity().getValue() - item.getAllocatedQuantity().getValue();
            if (availableQuantity <= 0) {
                return false;
            }
            // When locationId is null (FEFO query), filter to only BIN locations or unassigned
            if (query.getLocationId() == null) {
                if (item.getLocationId() == null) {
                    return true; // Unassigned items are allowed
                }
                // Check if location is BIN type
                Optional<LocationServicePort.LocationInfo> locationInfo = locationServicePort.getLocationInfo(item.getLocationId(), query.getTenantId());
                if (locationInfo.isEmpty()) {
                    log.warn("Location not found for stock item: {}, locationId: {}", item.getStockItemId().getValueAsString(), item.getLocationId().getValueAsString());
                    return false; // Exclude items with invalid locations
                }
                return locationInfo.get().isBinType(); // Only include BIN locations
            }
            // When locationId is specified, include all items at that location (validation done at command layer)
            return true;
        }).collect(Collectors.toList());

        log.debug("Filtered to {} stock item(s) with available quantity > 0", filteredItems.size());

        if (filteredItems.isEmpty()) {
            return List.of();
        }

        // 3. Sort by FEFO: expiration date (earliest first), then by received date (earliest first)
        List<StockItemView> sortedItems = filteredItems.stream().sorted(createFEFOComparator()).collect(Collectors.toList());

        // 4. Fetch product and location information for enrichment
        Optional<ProductServicePort.ProductInfo> productInfo = productServicePort.getProductById(query.getProductId(), query.getTenantId());

        // Collect unique location IDs for batch enrichment
        Map<LocationId, Optional<LocationServicePort.LocationInfo>> locationIds =
                sortedItems.stream().map(StockItemView::getLocationId).filter(locationId -> locationId != null).distinct()
                        .collect(Collectors.toMap(locationId -> locationId, locationId -> locationServicePort.getLocationInfo(locationId, query.getTenantId())));

        log.debug("Enriched product: {}, locations: {}", productInfo.isPresent(), locationIds.size());

        // 5. Map stock item views to query results with enriched information
        final ProductServicePort.ProductInfo productInfoValue = productInfo.orElse(null);

        return sortedItems.stream().map(stockItemView -> {
            Optional<LocationServicePort.LocationInfo> locationInfo = locationIds.get(stockItemView.getLocationId());
            return GetStockItemQueryResult.builder().stockItemId(stockItemView.getStockItemId()).productId(stockItemView.getProductId())
                    .productCode(productInfoValue != null ? productInfoValue.getProductCode() : null)
                    .productDescription(productInfoValue != null ? productInfoValue.getDescription() : null).locationId(stockItemView.getLocationId())
                    .locationCode(locationInfo.map(LocationServicePort.LocationInfo::code).orElse(null))
                    .locationName(locationInfo.map(LocationServicePort.LocationInfo::getDisplayName).orElse(null))
                    .locationHierarchy(locationInfo.map(LocationServicePort.LocationInfo::getHierarchy).orElse(null)).quantity(stockItemView.getQuantity())
                    .allocatedQuantity(stockItemView.getAllocatedQuantity()).expirationDate(stockItemView.getExpirationDate()).classification(stockItemView.getClassification())
                    .consignmentId(stockItemView.getConsignmentId()).createdAt(stockItemView.getCreatedAt()).lastModifiedAt(stockItemView.getLastModifiedAt()).build();
        }).collect(Collectors.toList());
    }

    /**
     * Creates a FEFO comparator for sorting stock items.
     * <p>
     * Sorting order:
     * 1. Expiration date (earliest first, nulls last)
     * 2. Created date (earliest first) - used as proxy for received date
     *
     * @return Comparator for FEFO sorting
     */
    private Comparator<StockItemView> createFEFOComparator() {
        return Comparator.<StockItemView, LocalDate>comparing(item -> item.getExpirationDate() != null ? item.getExpirationDate().getValue() : null,
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(item -> item.getCreatedAt() != null ? item.getCreatedAt() : LocalDateTime.MIN, Comparator.nullsLast(Comparator.naturalOrder()));
    }
}
