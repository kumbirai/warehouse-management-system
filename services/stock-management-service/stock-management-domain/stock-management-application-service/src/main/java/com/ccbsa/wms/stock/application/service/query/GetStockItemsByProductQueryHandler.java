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
import com.ccbsa.wms.stock.application.service.port.data.StockItemViewRepository;
import com.ccbsa.wms.stock.application.service.port.data.dto.StockItemView;
import com.ccbsa.wms.stock.application.service.port.service.LocationServicePort;
import com.ccbsa.wms.stock.application.service.port.service.ProductServicePort;
import com.ccbsa.wms.stock.application.service.query.dto.GetStockItemQueryResult;
import com.ccbsa.wms.stock.application.service.query.dto.GetStockItemsByProductQuery;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Query Handler: GetStockItemsByProductQueryHandler
 * <p>
 * Handles retrieval of stock items by product ID only (including items without location assignment).
 * <p>
 * Responsibilities:
 * - Query stock items from read model by product (CQRS compliant)
 * - Enrich with product and location information for user-friendly display
 * - Map stock item views to query result DTOs
 * - Return list of stock items
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class GetStockItemsByProductQueryHandler {
    private final StockItemViewRepository stockItemViewRepository;
    private final ProductServicePort productServicePort;
    private final LocationServicePort locationServicePort;

    @Transactional(readOnly = true)
    public List<GetStockItemQueryResult> handle(GetStockItemsByProductQuery query) {
        log.debug("Handling GetStockItemsByProductQuery for product: {}, tenant: {}", query.getProductId().getValueAsString(), query.getTenantId().getValue());

        // 1. Query stock items from read model by product (CQRS compliant)
        List<StockItemView> stockItemViews = stockItemViewRepository.findByTenantIdAndProductId(query.getTenantId(), query.getProductId());

        log.debug("Found {} stock item(s) for product: {}", stockItemViews.size(), query.getProductId().getValueAsString());

        if (stockItemViews.isEmpty()) {
            return List.of();
        }

        // 2. Collect unique location IDs for batch enrichment
        Set<LocationId> locationIds = stockItemViews.stream().map(StockItemView::getLocationId).filter(locationId -> locationId != null).collect(Collectors.toSet());

        // 3. Batch fetch product and location information
        Optional<ProductServicePort.ProductInfo> productInfo = productServicePort.getProductById(query.getProductId(), query.getTenantId());

        Map<LocationId, LocationServicePort.LocationInfo> locationInfoMap = new HashMap<>();
        for (LocationId locationId : locationIds) {
            Optional<LocationServicePort.LocationInfo> locationInfo = locationServicePort.getLocationInfo(locationId, query.getTenantId());
            locationInfo.ifPresent(info -> locationInfoMap.put(locationId, info));
        }

        log.debug("Enriched product: {}, locations: {}", productInfo.isPresent(), locationInfoMap.size());

        // 4. Map stock item views to query results with enriched information
        final ProductServicePort.ProductInfo productInfoValue = productInfo.orElse(null);
        return stockItemViews.stream().map(stockItemView -> {
            LocationServicePort.LocationInfo locationInfo = stockItemView.getLocationId() != null ? locationInfoMap.get(stockItemView.getLocationId()) : null;

            return GetStockItemQueryResult.builder().stockItemId(stockItemView.getStockItemId()).productId(stockItemView.getProductId())
                    .productCode(productInfoValue != null ? productInfoValue.getProductCode() : null)
                    .productDescription(productInfoValue != null ? productInfoValue.getDescription() : null).locationId(stockItemView.getLocationId())
                    .locationCode(locationInfo != null ? locationInfo.code() : null).locationName(locationInfo != null ? locationInfo.getDisplayName() : null)
                    .quantity(stockItemView.getQuantity()).expirationDate(stockItemView.getExpirationDate()).classification(stockItemView.getClassification())
                    .consignmentId(stockItemView.getConsignmentId()).createdAt(stockItemView.getCreatedAt()).lastModifiedAt(stockItemView.getLastModifiedAt()).build();
        }).collect(Collectors.toList());
    }
}
