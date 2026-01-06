package com.ccbsa.wms.stock.application.service.query;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ccbsa.common.domain.valueobject.ProductId;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;
import com.ccbsa.wms.stock.application.service.port.data.StockItemViewRepository;
import com.ccbsa.wms.stock.application.service.port.service.LocationServicePort;
import com.ccbsa.wms.stock.application.service.port.service.ProductServicePort;
import com.ccbsa.wms.stock.application.service.query.dto.GetStockItemQueryResult;
import com.ccbsa.wms.stock.application.service.query.dto.GetStockItemsQuery;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Query Handler: GetStockItemsQueryHandler
 * <p>
 * Handles retrieval of all stock item views for a tenant.
 * <p>
 * Responsibilities:
 * - Load all stock item views from data port (read model) for a tenant
 * - Enrich with product and location information for user-friendly display
 * - Map views to query result DTOs
 * - Return optimized read model
 * <p>
 * Uses data port (StockItemViewRepository) instead of repository port for CQRS compliance.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class GetStockItemsQueryHandler {
    private final StockItemViewRepository viewRepository;
    private final ProductServicePort productServicePort;
    private final LocationServicePort locationServicePort;

    @Transactional(readOnly = true)
    public List<GetStockItemQueryResult> handle(GetStockItemsQuery query) {
        log.debug("Handling GetStockItemsQuery for tenant: {}", query.getTenantId().getValue());

        // 1. Load read models (views) from data port
        var stockItemViews = viewRepository.findByTenantId(query.getTenantId());

        log.debug("Found {} stock item view(s) for tenant: {}", stockItemViews.size(), query.getTenantId().getValue());

        if (stockItemViews.isEmpty()) {
            return List.of();
        }

        // 2. Collect unique product IDs and location IDs for batch enrichment
        Set<ProductId> productIds = stockItemViews.stream().map(view -> view.getProductId()).filter(productId -> productId != null).collect(Collectors.toSet());

        Set<LocationId> locationIds = stockItemViews.stream().map(view -> view.getLocationId()).filter(locationId -> locationId != null).collect(Collectors.toSet());

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

        log.debug("Enriched {} products and {} locations", productInfoMap.size(), locationInfoMap.size());

        // 4. Map views to query results with enriched information
        return stockItemViews.stream().map(view -> {
            ProductServicePort.ProductInfo productInfo = productInfoMap.get(view.getProductId());
            LocationServicePort.LocationInfo locationInfo = view.getLocationId() != null ? locationInfoMap.get(view.getLocationId()) : null;

            return GetStockItemQueryResult.builder().stockItemId(view.getStockItemId()).productId(view.getProductId())
                    .productCode(productInfo != null ? productInfo.getProductCode() : null).productDescription(productInfo != null ? productInfo.getDescription() : null)
                    .locationId(view.getLocationId()).locationCode(locationInfo != null ? locationInfo.code() : null)
                    .locationName(locationInfo != null ? locationInfo.getDisplayName() : null).quantity(view.getQuantity()).expirationDate(view.getExpirationDate())
                    .classification(view.getClassification()).consignmentId(view.getConsignmentId()).createdAt(view.getCreatedAt()).lastModifiedAt(view.getLastModifiedAt())
                    .build();
        }).collect(Collectors.toList());
    }
}
