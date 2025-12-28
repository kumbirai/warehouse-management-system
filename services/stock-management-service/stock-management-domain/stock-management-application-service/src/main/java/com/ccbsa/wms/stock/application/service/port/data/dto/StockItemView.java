package com.ccbsa.wms.stock.application.service.port.data.dto;

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
 * Read Model DTO: StockItemView
 * <p>
 * Optimized read model representation of StockItem aggregate for query operations.
 * <p>
 * This is a denormalized view optimized for read queries, separate from the write model (StockItem aggregate).
 * <p>
 * Fields are flattened and optimized for query performance.
 */
@Getter
@Builder
public final class StockItemView {
    private final StockItemId stockItemId;
    private final String tenantId;
    private final ProductId productId;
    private final LocationId locationId;
    private final Quantity quantity;
    private final ExpirationDate expirationDate;
    private final StockClassification classification;
    private final ConsignmentId consignmentId;
    private final LocalDateTime createdAt;
    private final LocalDateTime lastModifiedAt;

    public StockItemView(StockItemId stockItemId, String tenantId, ProductId productId, LocationId locationId, Quantity quantity, ExpirationDate expirationDate,
                         StockClassification classification, ConsignmentId consignmentId, LocalDateTime createdAt, LocalDateTime lastModifiedAt) {
        if (stockItemId == null) {
            throw new IllegalArgumentException("StockItemId is required");
        }
        if (tenantId == null) {
            throw new IllegalArgumentException("TenantId is required");
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
        this.tenantId = tenantId;
        this.productId = productId;
        this.locationId = locationId;
        this.quantity = quantity;
        this.expirationDate = expirationDate;
        this.classification = classification;
        this.consignmentId = consignmentId;
        this.createdAt = createdAt;
        this.lastModifiedAt = lastModifiedAt;
    }
}

