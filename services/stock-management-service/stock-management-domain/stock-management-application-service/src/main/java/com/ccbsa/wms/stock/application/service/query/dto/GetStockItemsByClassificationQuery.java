package com.ccbsa.wms.stock.application.service.query.dto;

import com.ccbsa.common.domain.valueobject.StockClassification;
import com.ccbsa.common.domain.valueobject.TenantId;

/**
 * Query DTO: GetStockItemsByClassificationQuery
 * <p>
 * Query object for retrieving stock items by classification.
 */
public final class GetStockItemsByClassificationQuery {
    private final StockClassification classification;
    private final TenantId tenantId;

    private GetStockItemsByClassificationQuery(Builder builder) {
        this.classification = builder.classification;
        this.tenantId = builder.tenantId;
    }

    public static Builder builder() {
        return new Builder();
    }

    public StockClassification getClassification() {
        return classification;
    }

    public TenantId getTenantId() {
        return tenantId;
    }

    public static class Builder {
        private StockClassification classification;
        private TenantId tenantId;

        public Builder classification(StockClassification classification) {
            this.classification = classification;
            return this;
        }

        public Builder tenantId(TenantId tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public GetStockItemsByClassificationQuery build() {
            if (classification == null) {
                throw new IllegalArgumentException("Classification is required");
            }
            if (tenantId == null) {
                throw new IllegalArgumentException("TenantId is required");
            }
            return new GetStockItemsByClassificationQuery(this);
        }
    }
}

