package com.ccbsa.wms.stock.application.service.query;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ccbsa.wms.location.domain.core.valueobject.LocationId;
import com.ccbsa.common.domain.valueobject.ProductId;
import com.ccbsa.wms.stock.application.service.port.data.StockItemViewRepository;
import com.ccbsa.wms.stock.application.service.port.data.dto.StockItemView;
import com.ccbsa.wms.stock.application.service.port.service.LocationServicePort;
import com.ccbsa.wms.stock.application.service.port.service.ProductServicePort;
import com.ccbsa.wms.stock.application.service.query.dto.GetStockItemQueryResult;
import com.ccbsa.wms.stock.application.service.query.dto.GetStockItemsByLocationQuery;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Query Handler: GetStockItemsByLocationQueryHandler
 * <p>
 * Handles retrieval of stock items by location ID.
 * <p>
 * Responsibilities:
 * - Query stock items from read model by location (CQRS compliant)
 * - Enrich with product and location information for user-friendly display
 * - Map stock item views to query result DTOs
 * - Return list of stock items
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class GetStockItemsByLocationQueryHandler {
    private final StockItemViewRepository stockItemViewRepository;
    private final ProductServicePort productServicePort;
    private final LocationServicePort locationServicePort;

    @Transactional(readOnly = true)
    public List<GetStockItemQueryResult> handle(GetStockItemsByLocationQuery query) {
        log.debug("Handling GetStockItemsByLocationQuery for location: {}, tenant: {}", query.getLocationId().getValueAsString(), query.getTenantId().getValue());

        // 1. Query stock items from read model (CQRS compliant)
        List<StockItemView> stockItemViews = stockItemViewRepository.findByTenantIdAndLocationId(query.getTenantId(), query.getLocationId());

        log.debug("Found {} stock item(s) at location: {}", stockItemViews.size(), query.getLocationId().getValueAsString());

        if (stockItemViews.isEmpty()) {
            return List.of();
        }

        // 2. Collect unique product IDs and location IDs for batch enrichment
        Set<ProductId> productIds = stockItemViews.stream().map(StockItemView::getProductId).filter(productId -> productId != null).collect(Collectors.toSet());
        Set<LocationId> locationIds = stockItemViews.stream().map(StockItemView::getLocationId).filter(locationId -> locationId != null).collect(Collectors.toSet());

        // 3. Batch fetch product and location information
        Map<ProductId, ProductServicePort.ProductInfo> productInfoMap = new HashMap<>();
        for (ProductId productId : productIds) {
            Optional<ProductServicePort.ProductInfo> productInfo = productServicePort.getProductById(productId, query.getTenantId());
            productInfo.ifPresent(info -> productInfoMap.put(productId, info));
        }

        Map<LocationId, LocationServicePort.LocationInfo> locationInfoMap = new HashMap<>();
        for (LocationId locationId : locationIds) {
            Optional<LocationServicePort.LocationInfo> locationInfo = locationServicePort.getLocationInfo(locationId, query.getTenantId());
            locationInfo.ifPresent(info -> locationInfoMap.put(locationId, info));
        }

        log.debug("Enriched products: {}, locations: {}", productInfoMap.size(), locationInfoMap.size());

        // 4. Map stock item views to query results with enriched information
        return stockItemViews.stream().map(stockItemView -> {
            ProductServicePort.ProductInfo productInfo = productInfoMap.get(stockItemView.getProductId());
            LocationServicePort.LocationInfo locationInfo = locationInfoMap.get(stockItemView.getLocationId());

            return GetStockItemQueryResult.builder()
                    .stockItemId(stockItemView.getStockItemId())
                    .productId(stockItemView.getProductId())
                    .productCode(productInfo != null ? productInfo.getProductCode() : null)
                    .productDescription(productInfo != null ? productInfo.getDescription() : null)
                    .locationId(stockItemView.getLocationId())
                    .locationCode(locationInfo != null ? locationInfo.code() : null)
                    .locationName(locationInfo != null ? locationInfo.getDisplayName() : null)
                    .locationHierarchy(locationInfo != null ? locationInfo.getHierarchy() : null)
                    .quantity(stockItemView.getQuantity())
                    .expirationDate(stockItemView.getExpirationDate())
                    .classification(stockItemView.getClassification())
                    .consignmentId(stockItemView.getConsignmentId())
                    .createdAt(stockItemView.getCreatedAt())
                    .lastModifiedAt(stockItemView.getLastModifiedAt())
                    .build();
        }).collect(Collectors.toList());
    }
}
