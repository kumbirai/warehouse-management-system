package com.ccbsa.wms.stock.application.service.command.dto;

import java.time.LocalDateTime;

import com.ccbsa.wms.stock.domain.core.valueobject.ConsignmentId;
import com.ccbsa.wms.stock.domain.core.valueobject.ConsignmentStatus;

import lombok.Builder;
import lombok.Getter;

/**
 * Result DTO: ConfirmConsignmentResult
 * <p>
 * Result object returned after confirming a consignment.
 */
@Getter
@Builder
public final class ConfirmConsignmentResult {
    private final ConsignmentId consignmentId;
    private final ConsignmentStatus status;
    private final LocalDateTime confirmedAt;

    public ConfirmConsignmentResult(ConsignmentId consignmentId, ConsignmentStatus status, LocalDateTime confirmedAt) {
        if (consignmentId == null) {
            throw new IllegalArgumentException("ConsignmentId is required");
        }
        if (status == null) {
            throw new IllegalArgumentException("Status is required");
        }
        this.consignmentId = consignmentId;
        this.status = status;
        this.confirmedAt = confirmedAt;
    }
}

