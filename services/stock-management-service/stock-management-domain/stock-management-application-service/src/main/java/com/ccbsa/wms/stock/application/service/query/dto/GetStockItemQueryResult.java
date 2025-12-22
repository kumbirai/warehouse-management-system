package com.ccbsa.wms.stock.application.service.query.dto;

import java.time.LocalDateTime;

import com.ccbsa.common.domain.valueobject.ExpirationDate;
import com.ccbsa.common.domain.valueobject.StockClassification;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;
import com.ccbsa.wms.product.domain.core.valueobject.ProductId;
import com.ccbsa.wms.stock.domain.core.valueobject.ConsignmentId;
import com.ccbsa.wms.stock.domain.core.valueobject.Quantity;
import com.ccbsa.wms.stock.domain.core.valueobject.StockItemId;

/**
 * Query Result DTO: GetStockItemQueryResult
 * <p>
 * Query result object for stock item retrieval.
 */
public final class GetStockItemQueryResult {
    private final StockItemId stockItemId;
    private final ProductId productId;
    private final LocationId locationId;
    private final Quantity quantity;
    private final ExpirationDate expirationDate;
    private final StockClassification classification;
    private final ConsignmentId consignmentId;
    private final LocalDateTime createdAt;
    private final LocalDateTime lastModifiedAt;

    private GetStockItemQueryResult(Builder builder) {
        this.stockItemId = builder.stockItemId;
        this.productId = builder.productId;
        this.locationId = builder.locationId;
        this.quantity = builder.quantity;
        this.expirationDate = builder.expirationDate;
        this.classification = builder.classification;
        this.consignmentId = builder.consignmentId;
        this.createdAt = builder.createdAt;
        this.lastModifiedAt = builder.lastModifiedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public StockItemId getStockItemId() {
        return stockItemId;
    }

    public ProductId getProductId() {
        return productId;
    }

    public LocationId getLocationId() {
        return locationId;
    }

    public Quantity getQuantity() {
        return quantity;
    }

    public ExpirationDate getExpirationDate() {
        return expirationDate;
    }

    public StockClassification getClassification() {
        return classification;
    }

    public ConsignmentId getConsignmentId() {
        return consignmentId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getLastModifiedAt() {
        return lastModifiedAt;
    }

    public static class Builder {
        private StockItemId stockItemId;
        private ProductId productId;
        private LocationId locationId;
        private Quantity quantity;
        private ExpirationDate expirationDate;
        private StockClassification classification;
        private ConsignmentId consignmentId;
        private LocalDateTime createdAt;
        private LocalDateTime lastModifiedAt;

        public Builder stockItemId(StockItemId stockItemId) {
            this.stockItemId = stockItemId;
            return this;
        }

        public Builder productId(ProductId productId) {
            this.productId = productId;
            return this;
        }

        public Builder locationId(LocationId locationId) {
            this.locationId = locationId;
            return this;
        }

        public Builder quantity(Quantity quantity) {
            this.quantity = quantity;
            return this;
        }

        public Builder expirationDate(ExpirationDate expirationDate) {
            this.expirationDate = expirationDate;
            return this;
        }

        public Builder classification(StockClassification classification) {
            this.classification = classification;
            return this;
        }

        public Builder consignmentId(ConsignmentId consignmentId) {
            this.consignmentId = consignmentId;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder lastModifiedAt(LocalDateTime lastModifiedAt) {
            this.lastModifiedAt = lastModifiedAt;
            return this;
        }

        public GetStockItemQueryResult build() {
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
            return new GetStockItemQueryResult(this);
        }
    }
}

