package com.ccbsa.wms.stock.application.service.query.dto;

import java.time.LocalDate;

import com.ccbsa.common.domain.valueobject.StockClassification;
import com.ccbsa.common.domain.valueobject.TenantId;

import lombok.Builder;
import lombok.Getter;

/**
 * Query DTO: GetExpiringStockQuery
 * <p>
 * Query object for retrieving expiring stock items.
 */
@Getter
@Builder
public final class GetExpiringStockQuery {
    private final TenantId tenantId;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final StockClassification classification;
}
