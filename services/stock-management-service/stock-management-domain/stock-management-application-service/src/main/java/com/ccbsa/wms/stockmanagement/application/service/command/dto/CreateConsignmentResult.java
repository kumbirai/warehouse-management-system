package com.ccbsa.wms.stockmanagement.application.service.command.dto;

import java.time.LocalDateTime;

import com.ccbsa.wms.stockmanagement.domain.core.valueobject.ConsignmentId;
import com.ccbsa.wms.stockmanagement.domain.core.valueobject.ConsignmentStatus;

/**
 * Command Result DTO: CreateConsignmentResult
 * <p>
 * Result object returned from CreateConsignmentCommand execution.
 */
public final class CreateConsignmentResult {
    private final ConsignmentId consignmentId;
    private final ConsignmentStatus status;
    private final LocalDateTime receivedAt;

    private CreateConsignmentResult(Builder builder) {
        this.consignmentId = builder.consignmentId;
        this.status = builder.status;
        this.receivedAt = builder.receivedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public ConsignmentId getConsignmentId() {
        return consignmentId;
    }

    public ConsignmentStatus getStatus() {
        return status;
    }

    public LocalDateTime getReceivedAt() {
        return receivedAt;
    }

    public static class Builder {
        private ConsignmentId consignmentId;
        private ConsignmentStatus status;
        private LocalDateTime receivedAt;

        public Builder consignmentId(ConsignmentId consignmentId) {
            this.consignmentId = consignmentId;
            return this;
        }

        public Builder status(ConsignmentStatus status) {
            this.status = status;
            return this;
        }

        public Builder receivedAt(LocalDateTime receivedAt) {
            this.receivedAt = receivedAt;
            return this;
        }

        public CreateConsignmentResult build() {
            if (consignmentId == null) {
                throw new IllegalArgumentException("ConsignmentId is required");
            }
            if (status == null) {
                throw new IllegalArgumentException("Status is required");
            }
            if (receivedAt == null) {
                throw new IllegalArgumentException("ReceivedAt is required");
            }
            return new CreateConsignmentResult(this);
        }
    }
}

