package com.ccbsa.wms.stock.application.service.query;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ccbsa.common.domain.valueobject.Quantity;
import com.ccbsa.wms.stock.application.service.port.data.StockAllocationViewRepository;
import com.ccbsa.wms.stock.application.service.port.data.StockItemViewRepository;
import com.ccbsa.wms.stock.application.service.port.data.dto.StockAllocationView;
import com.ccbsa.wms.stock.application.service.port.service.LocationServicePort;
import com.ccbsa.wms.stock.application.service.port.service.ProductServicePort;
import com.ccbsa.wms.stock.application.service.query.dto.GetStockItemQuery;
import com.ccbsa.wms.stock.application.service.query.dto.GetStockItemQueryResult;
import com.ccbsa.wms.stock.domain.core.exception.StockItemNotFoundException;
import com.ccbsa.wms.stock.domain.core.valueobject.AllocationStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Query Handler: GetStockItemQueryHandler
 * <p>
 * Handles retrieval of StockItem read model by ID.
 * <p>
 * Responsibilities:
 * - Load StockItem view from data port (read model)
 * - Enrich with product and location information for user-friendly display
 * - Map view to query result DTO
 * - Return optimized read model
 * <p>
 * Uses data port (StockItemViewRepository) instead of repository port for CQRS compliance.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class GetStockItemQueryHandler {
    private final StockItemViewRepository viewRepository;
    private final ProductServicePort productServicePort;
    private final LocationServicePort locationServicePort;
    private final StockAllocationViewRepository allocationViewRepository;

    @Transactional(readOnly = true)
    public GetStockItemQueryResult handle(GetStockItemQuery query) {
        log.debug("Handling GetStockItemQuery for stockItemId: {}, tenant: {}", query.getStockItemId().getValueAsString(), query.getTenantId().getValue());

        // 1. Load read model (view) from data port
        var stockItemView = viewRepository.findByTenantIdAndId(query.getTenantId(), query.getStockItemId())
                .orElseThrow(() -> new StockItemNotFoundException(String.format("Stock item not found: %s", query.getStockItemId().getValueAsString())));

        // 2. Fetch product and location information for enrichment
        Optional<ProductServicePort.ProductInfo> productInfo = productServicePort.getProductById(stockItemView.getProductId(), query.getTenantId());
        Optional<LocationServicePort.LocationInfo> locationInfo =
                stockItemView.getLocationId() != null ? locationServicePort.getLocationInfo(stockItemView.getLocationId(), query.getTenantId()) : Optional.empty();

        log.debug("Enriched stock item with product: {}, location: {}", productInfo.isPresent(), locationInfo.isPresent());

        // 3. Calculate allocated quantity from allocations (using read model)
        List<StockAllocationView> allocationViews = allocationViewRepository.findByStockItemId(stockItemView.getStockItemId());
        int allocatedQuantity =
                allocationViews.stream().filter(allocation -> allocation.getStatus() == AllocationStatus.ALLOCATED).mapToInt(allocation -> allocation.getQuantity().getValue())
                        .sum();

        // 4. Map view to query result with enriched information
        return GetStockItemQueryResult.builder().stockItemId(stockItemView.getStockItemId()).productId(stockItemView.getProductId())
                .productCode(productInfo.map(ProductServicePort.ProductInfo::getProductCode).orElse(null))
                .productDescription(productInfo.map(ProductServicePort.ProductInfo::getDescription).orElse(null)).locationId(stockItemView.getLocationId())
                .locationCode(locationInfo.map(LocationServicePort.LocationInfo::code).orElse(null))
                .locationName(locationInfo.map(LocationServicePort.LocationInfo::getDisplayName).orElse(null))
                .locationHierarchy(locationInfo.map(LocationServicePort.LocationInfo::getHierarchy).orElse(null)).quantity(stockItemView.getQuantity())
                .allocatedQuantity(Quantity.of(allocatedQuantity)).expirationDate(stockItemView.getExpirationDate()).classification(stockItemView.getClassification())
                .consignmentId(stockItemView.getConsignmentId()).createdAt(stockItemView.getCreatedAt()).lastModifiedAt(stockItemView.getLastModifiedAt()).build();
    }
}

