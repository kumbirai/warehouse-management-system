package com.ccbsa.wms.returns.application.service.command.dto;

import java.time.LocalDateTime;

import com.ccbsa.common.domain.valueobject.OrderNumber;
import com.ccbsa.common.domain.valueobject.ReturnReason;
import com.ccbsa.common.domain.valueobject.ReturnStatus;
import com.ccbsa.common.domain.valueobject.ReturnId;
import com.ccbsa.wms.returns.domain.core.valueobject.ReturnType;

import lombok.Builder;
import lombok.Getter;

/**
 * Command Result DTO: ProcessFullOrderReturnResult
 * <p>
 * Result object returned from ProcessFullOrderReturnCommand execution.
 */
@Getter
@Builder
public final class ProcessFullOrderReturnResult {
    private final ReturnId returnId;
    private final OrderNumber orderNumber;
    private final ReturnType returnType;
    private final ReturnStatus status;
    private final ReturnReason primaryReturnReason;
    private final LocalDateTime returnedAt;

    public ProcessFullOrderReturnResult(ReturnId returnId, OrderNumber orderNumber, ReturnType returnType, ReturnStatus status, ReturnReason primaryReturnReason,
                                        LocalDateTime returnedAt) {
        if (returnId == null) {
            throw new IllegalArgumentException("ReturnId is required");
        }
        if (orderNumber == null) {
            throw new IllegalArgumentException("OrderNumber is required");
        }
        if (returnType == null) {
            throw new IllegalArgumentException("ReturnType is required");
        }
        if (status == null) {
            throw new IllegalArgumentException("Status is required");
        }
        if (primaryReturnReason == null) {
            throw new IllegalArgumentException("PrimaryReturnReason is required");
        }
        if (returnedAt == null) {
            throw new IllegalArgumentException("ReturnedAt is required");
        }
        this.returnId = returnId;
        this.orderNumber = orderNumber;
        this.returnType = returnType;
        this.status = status;
        this.primaryReturnReason = primaryReturnReason;
        this.returnedAt = returnedAt;
    }
}
