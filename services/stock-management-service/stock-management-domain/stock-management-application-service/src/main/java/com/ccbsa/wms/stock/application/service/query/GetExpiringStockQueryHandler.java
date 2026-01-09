package com.ccbsa.wms.stock.application.service.query;

import java.time.LocalDate;
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
import com.ccbsa.wms.stock.application.service.port.data.dto.StockItemView;
import com.ccbsa.wms.stock.application.service.port.service.LocationServicePort;
import com.ccbsa.wms.stock.application.service.port.service.ProductServicePort;
import com.ccbsa.wms.stock.application.service.query.dto.GetExpiringStockQuery;
import com.ccbsa.wms.stock.application.service.query.dto.GetStockItemQueryResult;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Query Handler: GetExpiringStockQueryHandler
 * <p>
 * Handles retrieval of expiring stock items.
 * <p>
 * Responsibilities:
 * - Query stock items from read model by expiration date range and classification
 * - Enrich with product and location information
 * - Map views to query result DTOs
 * - Return optimized read model
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class GetExpiringStockQueryHandler {

    private final StockItemViewRepository viewRepository;
    private final ProductServicePort productServicePort;
    private final LocationServicePort locationServicePort;

    @Transactional(readOnly = true)
    public List<GetStockItemQueryResult> handle(GetExpiringStockQuery query) {
        log.debug("Handling GetExpiringStockQuery for tenant: {}, startDate: {}, endDate: {}, classification: {}", query.getTenantId().getValue(), query.getStartDate(),
                query.getEndDate(), query.getClassification());

        // 1. Load all stock items for the tenant
        List<StockItemView> allStockItems = viewRepository.findByTenantId(query.getTenantId());

        // 2. Filter by expiration date range and classification
        List<StockItemView> filteredItems = allStockItems.stream().filter(item -> {
            // Filter by classification if specified
            if (query.getClassification() != null && item.getClassification() != query.getClassification()) {
                return false;
            }

            // Filter by expiration date range if specified
            if (item.getExpirationDate() != null) {
                LocalDate expirationDate = item.getExpirationDate().getValue();
                if (query.getStartDate() != null && expirationDate.isBefore(query.getStartDate())) {
                    return false;
                }
                if (query.getEndDate() != null && expirationDate.isAfter(query.getEndDate())) {
                    return false;
                }
            } else {
                // If no expiration date but we're filtering by date range, exclude it
                if (query.getStartDate() != null || query.getEndDate() != null) {
                    return false;
                }
            }

            return true;
        }).collect(Collectors.toList());

        log.debug("Filtered to {} stock items matching criteria", filteredItems.size());

        if (filteredItems.isEmpty()) {
            return List.of();
        }

        // 3. Collect unique product IDs and location IDs for batch enrichment
        Set<ProductId> productIds = filteredItems.stream().map(StockItemView::getProductId).filter(productId -> productId != null).collect(Collectors.toSet());

        Set<LocationId> locationIds = filteredItems.stream().map(StockItemView::getLocationId).filter(locationId -> locationId != null).collect(Collectors.toSet());

        // 4. Batch fetch product and location information
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

        // 5. Map views to query results with enriched information
        return filteredItems.stream().map(view -> {
            ProductServicePort.ProductInfo productInfo = productInfoMap.get(view.getProductId());
            LocationServicePort.LocationInfo locationInfo = view.getLocationId() != null ? locationInfoMap.get(view.getLocationId()) : null;

            // Calculate days until expiry
            Integer daysUntilExpiry = null;
            if (view.getExpirationDate() != null && view.getExpirationDate().getValue() != null) {
                LocalDate today = LocalDate.now();
                LocalDate expiryDate = view.getExpirationDate().getValue();
                long days = java.time.temporal.ChronoUnit.DAYS.between(today, expiryDate);
                daysUntilExpiry = (int) days;
            }

            return GetStockItemQueryResult.builder().stockItemId(view.getStockItemId()).productId(view.getProductId())
                    .productCode(productInfo != null ? productInfo.getProductCode() : null).productDescription(productInfo != null ? productInfo.getDescription() : null)
                    .locationId(view.getLocationId()).locationCode(locationInfo != null ? locationInfo.code() : null)
                    .locationName(locationInfo != null ? locationInfo.getDisplayName() : null).locationHierarchy(locationInfo != null ? locationInfo.getHierarchy() : null)
                    .quantity(view.getQuantity()).expirationDate(view.getExpirationDate()).classification(view.getClassification()).consignmentId(view.getConsignmentId())
                    .createdAt(view.getCreatedAt()).lastModifiedAt(view.getLastModifiedAt()).daysUntilExpiry(daysUntilExpiry).build();
        }).collect(Collectors.toList());
    }
}
