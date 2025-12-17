package com.ccbsa.wms.stockmanagement.application.service.query.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.ccbsa.common.domain.valueobject.WarehouseId;
import com.ccbsa.wms.stockmanagement.domain.core.valueobject.ConsignmentId;
import com.ccbsa.wms.stockmanagement.domain.core.valueobject.ConsignmentLineItem;
import com.ccbsa.wms.stockmanagement.domain.core.valueobject.ConsignmentReference;
import com.ccbsa.wms.stockmanagement.domain.core.valueobject.ConsignmentStatus;

/**
 * Query Result DTO: ConsignmentQueryResult
 * <p>
 * Result object returned from consignment queries. Contains optimized read model data for consignment information.
 */
public final class ConsignmentQueryResult {
    private final ConsignmentId consignmentId;
    private final ConsignmentReference consignmentReference;
    private final WarehouseId warehouseId;
    private final ConsignmentStatus status;
    private final LocalDateTime receivedAt;
    private final LocalDateTime confirmedAt;
    private final String receivedBy;
    private final List<ConsignmentLineItem> lineItems;
    private final LocalDateTime createdAt;
    private final LocalDateTime lastModifiedAt;

    private ConsignmentQueryResult(Builder builder) {
        this.consignmentId = builder.consignmentId;
        this.consignmentReference = builder.consignmentReference;
        this.warehouseId = builder.warehouseId;
        this.status = builder.status;
        this.receivedAt = builder.receivedAt;
        this.confirmedAt = builder.confirmedAt;
        this.receivedBy = builder.receivedBy;
        this.lineItems = builder.lineItems != null ? List.copyOf(builder.lineItems) : List.of();
        this.createdAt = builder.createdAt;
        this.lastModifiedAt = builder.lastModifiedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public ConsignmentId getConsignmentId() {
        return consignmentId;
    }

    public ConsignmentReference getConsignmentReference() {
        return consignmentReference;
    }

    public WarehouseId getWarehouseId() {
        return warehouseId;
    }

    public ConsignmentStatus getStatus() {
        return status;
    }

    public LocalDateTime getReceivedAt() {
        return receivedAt;
    }

    public LocalDateTime getConfirmedAt() {
        return confirmedAt;
    }

    public String getReceivedBy() {
        return receivedBy;
    }

    public List<ConsignmentLineItem> getLineItems() {
        return lineItems;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getLastModifiedAt() {
        return lastModifiedAt;
    }

    public static class Builder {
        private ConsignmentId consignmentId;
        private ConsignmentReference consignmentReference;
        private WarehouseId warehouseId;
        private ConsignmentStatus status;
        private LocalDateTime receivedAt;
        private LocalDateTime confirmedAt;
        private String receivedBy;
        private List<ConsignmentLineItem> lineItems;
        private LocalDateTime createdAt;
        private LocalDateTime lastModifiedAt;

        public Builder consignmentId(ConsignmentId consignmentId) {
            this.consignmentId = consignmentId;
            return this;
        }

        public Builder consignmentReference(ConsignmentReference consignmentReference) {
            this.consignmentReference = consignmentReference;
            return this;
        }

        public Builder warehouseId(WarehouseId warehouseId) {
            this.warehouseId = warehouseId;
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

        public Builder confirmedAt(LocalDateTime confirmedAt) {
            this.confirmedAt = confirmedAt;
            return this;
        }

        public Builder receivedBy(String receivedBy) {
            this.receivedBy = receivedBy;
            return this;
        }

        public Builder lineItems(List<ConsignmentLineItem> lineItems) {
            this.lineItems = lineItems != null ? new ArrayList<>(lineItems) : null;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder lastModifiedAt(LocalDateTime lastModifiedAt) {
            this.lastModifiedAt = lastModifiedAt;
            return this;
        }

        public ConsignmentQueryResult build() {
            if (consignmentId == null) {
                throw new IllegalArgumentException("ConsignmentId is required");
            }
            if (consignmentReference == null) {
                throw new IllegalArgumentException("ConsignmentReference is required");
            }
            if (warehouseId == null) {
                throw new IllegalArgumentException("WarehouseId is required");
            }
            if (status == null) {
                throw new IllegalArgumentException("Status is required");
            }
            return new ConsignmentQueryResult(this);
        }
    }
}

