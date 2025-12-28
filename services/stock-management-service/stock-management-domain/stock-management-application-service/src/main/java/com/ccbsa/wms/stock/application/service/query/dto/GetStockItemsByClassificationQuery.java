package com.ccbsa.wms.stock.application.service.query.dto;

import com.ccbsa.common.domain.valueobject.StockClassification;
import com.ccbsa.common.domain.valueobject.TenantId;

import lombok.Builder;
import lombok.Getter;

/**
 * Query DTO: GetStockItemsByClassificationQuery
 * <p>
 * Query object for retrieving stock items by classification.
 */
@Getter
@Builder
public final class GetStockItemsByClassificationQuery {
    private final StockClassification classification;
    private final TenantId tenantId;

    public GetStockItemsByClassificationQuery(StockClassification classification, TenantId tenantId) {
        if (classification == null) {
            throw new IllegalArgumentException("Classification is required");
        }
        if (tenantId == null) {
            throw new IllegalArgumentException("TenantId is required");
        }
        this.classification = classification;
        this.tenantId = tenantId;
    }
}

