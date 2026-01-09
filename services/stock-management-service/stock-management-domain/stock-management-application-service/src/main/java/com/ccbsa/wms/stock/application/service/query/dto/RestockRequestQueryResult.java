package com.ccbsa.wms.stock.application.service.query.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.ccbsa.common.domain.valueobject.ProductId;
import com.ccbsa.common.domain.valueobject.RestockPriority;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;
import com.ccbsa.wms.stock.domain.core.valueobject.RestockRequestId;
import com.ccbsa.wms.stock.domain.core.valueobject.RestockRequestStatus;

import lombok.Builder;
import lombok.Getter;

/**
 * Query Result DTO: RestockRequestQueryResult
 * <p>
 * Result object for a single restock request query.
 */
@Getter
@Builder
public final class RestockRequestQueryResult {
    private final RestockRequestId restockRequestId;
    private final ProductId productId;
    private final LocationId locationId;
    private final BigDecimal currentQuantity;
    private final BigDecimal minimumQuantity;
    private final BigDecimal maximumQuantity;
    private final BigDecimal requestedQuantity;
    private final RestockPriority priority;
    private final RestockRequestStatus status;
    private final LocalDateTime createdAt;
    private final LocalDateTime sentToD365At;
    private final String d365OrderReference;
}
