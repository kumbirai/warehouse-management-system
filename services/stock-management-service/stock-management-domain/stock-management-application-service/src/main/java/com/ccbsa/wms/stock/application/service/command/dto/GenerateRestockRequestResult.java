package com.ccbsa.wms.stock.application.service.command.dto;

import com.ccbsa.common.domain.valueobject.RestockPriority;
import com.ccbsa.wms.stock.domain.core.valueobject.RestockRequestId;

import lombok.Builder;
import lombok.Getter;

/**
 * Command Result DTO: GenerateRestockRequestResult
 * <p>
 * Result object returned after generating a restock request.
 */
@Getter
@Builder
public final class GenerateRestockRequestResult {
    private final RestockRequestId restockRequestId;
    private final RestockPriority priority;
    private final boolean isNewRequest;
}
