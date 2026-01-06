package com.ccbsa.wms.stock.application.service.query;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ccbsa.wms.stock.application.service.port.data.StockItemViewRepository;
import com.ccbsa.wms.stock.application.service.port.data.dto.StockItemView;
import com.ccbsa.wms.stock.application.service.port.service.LocationServicePort;
import com.ccbsa.wms.stock.application.service.port.service.ProductServicePort;
import com.ccbsa.wms.stock.application.service.query.dto.GetStockItemQueryResult;
import com.ccbsa.wms.stock.application.service.query.dto.GetStockItemsByProductAndLocationQuery;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Query Handler: GetStockItemsByProductAndLocationQueryHandler
 * <p>
 * Handles retrieval of stock items by product ID and location ID.
 * <p>
 * Responsibilities:
 * - Query stock items from read model by product and location (CQRS compliant)
 * - Enrich with product and location information for user-friendly display
 * - Map stock item views to query result DTOs
 * - Return list of stock items
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class GetStockItemsByProductAndLocationQueryHandler {
    private final StockItemViewRepository stockItemViewRepository;
    private final ProductServicePort productServicePort;
    private final LocationServicePort locationServicePort;

    @Transactional(readOnly = true)
    public List<GetStockItemQueryResult> handle(GetStockItemsByProductAndLocationQuery query) {
        log.debug("Handling GetStockItemsByProductAndLocationQuery for product: {}, location: {}, tenant: {}", query.getProductId().getValueAsString(),
                query.getLocationId().getValueAsString(), query.getTenantId().getValue());

        // 1. Query stock items from read model (CQRS compliant)
        List<StockItemView> stockItemViews = stockItemViewRepository.findByTenantIdAndProductIdAndLocationId(query.getTenantId(), query.getProductId(), query.getLocationId());

        log.debug("Found {} stock item(s) for product: {} at location: {}", stockItemViews.size(), query.getProductId().getValueAsString(),
                query.getLocationId().getValueAsString());

        if (stockItemViews.isEmpty()) {
            return List.of();
        }

        // 2. Fetch product and location information for enrichment
        Optional<ProductServicePort.ProductInfo> productInfo = productServicePort.getProductById(query.getProductId(), query.getTenantId());
        Optional<LocationServicePort.LocationInfo> locationInfo = locationServicePort.getLocationInfo(query.getLocationId(), query.getTenantId());

        log.debug("Enriched product: {}, location: {}", productInfo.isPresent(), locationInfo.isPresent());

        // 3. Map stock item views to query results with enriched information
        final ProductServicePort.ProductInfo productInfoValue = productInfo.orElse(null);
        final LocationServicePort.LocationInfo locationInfoValue = locationInfo.orElse(null);

        return stockItemViews.stream().map(stockItemView -> GetStockItemQueryResult.builder().stockItemId(stockItemView.getStockItemId()).productId(stockItemView.getProductId())
                        .productCode(productInfoValue != null ? productInfoValue.getProductCode() : null)
                        .productDescription(productInfoValue != null ? productInfoValue.getDescription() : null).locationId(stockItemView.getLocationId())
                        .locationCode(locationInfoValue != null ? locationInfoValue.code() : null).locationName(locationInfoValue != null ? locationInfoValue.getDisplayName() :
                                null)
                        .quantity(stockItemView.getQuantity()).expirationDate(stockItemView.getExpirationDate()).classification(stockItemView.getClassification())
                        .consignmentId(stockItemView.getConsignmentId()).createdAt(stockItemView.getCreatedAt()).lastModifiedAt(stockItemView.getLastModifiedAt()).build())
                .collect(Collectors.toList());
    }
}
