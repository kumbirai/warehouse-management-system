package com.ccbsa.wms.stock.application.service.command.dto;

import java.time.LocalDateTime;

import com.ccbsa.wms.stock.domain.core.valueobject.ConsignmentId;
import com.ccbsa.wms.stock.domain.core.valueobject.ConsignmentStatus;

/**
 * Result DTO: ConfirmConsignmentResult
 * <p>
 * Result object returned after confirming a consignment.
 */
public final class ConfirmConsignmentResult {
    private final ConsignmentId consignmentId;
    private final ConsignmentStatus status;
    private final LocalDateTime confirmedAt;

    private ConfirmConsignmentResult(Builder builder) {
        this.consignmentId = builder.consignmentId;
        this.status = builder.status;
        this.confirmedAt = builder.confirmedAt;
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

    public LocalDateTime getConfirmedAt() {
        return confirmedAt;
    }

    public static class Builder {
        private ConsignmentId consignmentId;
        private ConsignmentStatus status;
        private LocalDateTime confirmedAt;

        public Builder consignmentId(ConsignmentId consignmentId) {
            this.consignmentId = consignmentId;
            return this;
        }

        public Builder status(ConsignmentStatus status) {
            this.status = status;
            return this;
        }

        public Builder confirmedAt(LocalDateTime confirmedAt) {
            this.confirmedAt = confirmedAt;
            return this;
        }

        public ConfirmConsignmentResult build() {
            if (consignmentId == null) {
                throw new IllegalArgumentException("ConsignmentId is required");
            }
            if (status == null) {
                throw new IllegalArgumentException("Status is required");
            }
            return new ConfirmConsignmentResult(this);
        }
    }
}

