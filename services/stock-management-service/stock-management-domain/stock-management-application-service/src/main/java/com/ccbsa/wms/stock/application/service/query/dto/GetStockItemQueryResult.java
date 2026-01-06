package com.ccbsa.wms.stock.application.service.query.dto;

import java.time.LocalDateTime;

import com.ccbsa.common.domain.valueobject.ExpirationDate;
import com.ccbsa.common.domain.valueobject.ProductId;
import com.ccbsa.common.domain.valueobject.Quantity;
import com.ccbsa.common.domain.valueobject.StockClassification;
import com.ccbsa.common.domain.valueobject.StockItemId;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;
import com.ccbsa.wms.stock.domain.core.valueobject.ConsignmentId;

import lombok.Builder;
import lombok.Getter;

/**
 * Query Result DTO: GetStockItemQueryResult
 * <p>
 * Query result object for stock item retrieval.
 */
@Getter
@Builder
public final class GetStockItemQueryResult {
    private final StockItemId stockItemId;
    private final ProductId productId;
    private final String productCode;
    private final String productDescription;
    private final LocationId locationId;
    private final String locationCode;
    private final String locationName;
    private final Quantity quantity;
    private final Quantity allocatedQuantity;
    private final ExpirationDate expirationDate;
    private final StockClassification classification;
    private final ConsignmentId consignmentId;
    private final LocalDateTime createdAt;
    private final LocalDateTime lastModifiedAt;

    public GetStockItemQueryResult(StockItemId stockItemId, ProductId productId, String productCode, String productDescription, LocationId locationId, String locationCode,
                                   String locationName, Quantity quantity, Quantity allocatedQuantity, ExpirationDate expirationDate, StockClassification classification,
                                   ConsignmentId consignmentId, LocalDateTime createdAt, LocalDateTime lastModifiedAt) {
        if (stockItemId == null) {
            throw new IllegalArgumentException("StockItemId is required");
        }
        if (productId == null) {
            throw new IllegalArgumentException("ProductId is required");
        }
        if (quantity == null) {
            throw new IllegalArgumentException("Quantity is required");
        }
        if (classification == null) {
            throw new IllegalArgumentException("Classification is required");
        }
        this.stockItemId = stockItemId;
        this.productId = productId;
        this.productCode = productCode;
        this.productDescription = productDescription;
        this.locationId = locationId;
        this.locationCode = locationCode;
        this.locationName = locationName;
        this.quantity = quantity;
        this.allocatedQuantity = allocatedQuantity != null ? allocatedQuantity : Quantity.of(0);
        this.expirationDate = expirationDate;
        this.classification = classification;
        this.consignmentId = consignmentId;
        this.createdAt = createdAt;
        this.lastModifiedAt = lastModifiedAt;
    }
}

