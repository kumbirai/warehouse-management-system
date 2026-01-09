package com.ccbsa.wms.stock.application.service.query.dto;

import java.time.LocalDate;

import com.ccbsa.common.domain.valueobject.StockClassification;

import lombok.Builder;
import lombok.Getter;

/**
 * Query Result DTO: CheckStockExpirationQueryResult
 * <p>
 * Result object for stock expiration check query.
 */
@Getter
@Builder
public final class CheckStockExpirationQueryResult {
    private final boolean expired;
    private final LocalDate expirationDate;
    private final StockClassification classification;
    private final int daysUntilExpiration;
    private final String message;
}
