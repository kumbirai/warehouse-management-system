package com.ccbsa.wms.stock.application.service.command.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.common.domain.valueobject.WarehouseId;
import com.ccbsa.wms.stock.domain.core.valueobject.ConsignmentLineItem;
import com.ccbsa.wms.stock.domain.core.valueobject.ConsignmentReference;

/**
 * Command DTO: CreateConsignmentCommand
 * <p>
 * Command object for creating a new stock consignment.
 */
public final class CreateConsignmentCommand {
    private final TenantId tenantId;
    private final ConsignmentReference consignmentReference;
    private final WarehouseId warehouseId;
    private final LocalDateTime receivedAt;
    private final String receivedBy;
    private final List<ConsignmentLineItem> lineItems;

    private CreateConsignmentCommand(Builder builder) {
        this.tenantId = builder.tenantId;
        this.consignmentReference = builder.consignmentReference;
        this.warehouseId = builder.warehouseId;
        this.receivedAt = builder.receivedAt;
        this.receivedBy = builder.receivedBy;
        this.lineItems = builder.lineItems != null ? List.copyOf(builder.lineItems) : List.of();
    }

    public static Builder builder() {
        return new Builder();
    }

    public TenantId getTenantId() {
        return tenantId;
    }

    public ConsignmentReference getConsignmentReference() {
        return consignmentReference;
    }

    public WarehouseId getWarehouseId() {
        return warehouseId;
    }

    public LocalDateTime getReceivedAt() {
        return receivedAt;
    }

    public String getReceivedBy() {
        return receivedBy;
    }

    public List<ConsignmentLineItem> getLineItems() {
        return lineItems;
    }

    public static class Builder {
        private TenantId tenantId;
        private ConsignmentReference consignmentReference;
        private WarehouseId warehouseId;
        private LocalDateTime receivedAt;
        private String receivedBy;
        private List<ConsignmentLineItem> lineItems;

        public Builder tenantId(TenantId tenantId) {
            this.tenantId = tenantId;
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

        public Builder receivedAt(LocalDateTime receivedAt) {
            this.receivedAt = receivedAt;
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

        public CreateConsignmentCommand build() {
            if (tenantId == null) {
                throw new IllegalArgumentException("TenantId is required");
            }
            if (consignmentReference == null) {
                throw new IllegalArgumentException("ConsignmentReference is required");
            }
            if (warehouseId == null) {
                throw new IllegalArgumentException("WarehouseId is required");
            }
            if (receivedAt == null) {
                throw new IllegalArgumentException("ReceivedAt is required");
            }
            if (lineItems == null || lineItems.isEmpty()) {
                throw new IllegalArgumentException("At least one line item is required");
            }
            return new CreateConsignmentCommand(this);
        }
    }
}

