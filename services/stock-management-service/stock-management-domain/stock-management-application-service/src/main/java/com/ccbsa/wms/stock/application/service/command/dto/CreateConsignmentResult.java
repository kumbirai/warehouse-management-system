package com.ccbsa.wms.stock.application.service.command.dto;

import java.time.LocalDateTime;

import com.ccbsa.wms.stock.domain.core.valueobject.ConsignmentId;
import com.ccbsa.wms.stock.domain.core.valueobject.ConsignmentStatus;

import lombok.Builder;
import lombok.Getter;

/**
 * Command Result DTO: CreateConsignmentResult
 * <p>
 * Result object returned from CreateConsignmentCommand execution.
 */
@Getter
@Builder
public final class CreateConsignmentResult {
    private final ConsignmentId consignmentId;
    private final ConsignmentStatus status;
    private final LocalDateTime receivedAt;

    public CreateConsignmentResult(ConsignmentId consignmentId, ConsignmentStatus status, LocalDateTime receivedAt) {
        if (consignmentId == null) {
            throw new IllegalArgumentException("ConsignmentId is required");
        }
        if (status == null) {
            throw new IllegalArgumentException("Status is required");
        }
        if (receivedAt == null) {
            throw new IllegalArgumentException("ReceivedAt is required");
        }
        this.consignmentId = consignmentId;
        this.status = status;
        this.receivedAt = receivedAt;
    }
}

