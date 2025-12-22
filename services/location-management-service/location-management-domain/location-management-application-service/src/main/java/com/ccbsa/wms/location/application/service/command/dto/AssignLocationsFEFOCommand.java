package com.ccbsa.wms.location.application.service.command.dto;

import java.util.List;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.location.domain.core.valueobject.StockItemAssignmentRequest;

/**
 * Command DTO: AssignLocationsFEFOCommand
 * <p>
 * Command object for assigning locations to stock items based on FEFO principles.
 */
public final class AssignLocationsFEFOCommand {
    private final TenantId tenantId;
    private final List<StockItemAssignmentRequest> stockItems;

    private AssignLocationsFEFOCommand(Builder builder) {
        this.tenantId = builder.tenantId;
        this.stockItems = builder.stockItems != null ? List.copyOf(builder.stockItems) : List.of();
    }

    public static Builder builder() {
        return new Builder();
    }

    public TenantId getTenantId() {
        return tenantId;
    }

    public List<StockItemAssignmentRequest> getStockItems() {
        return stockItems;
    }

    public static class Builder {
        private TenantId tenantId;
        private List<StockItemAssignmentRequest> stockItems;

        public Builder tenantId(TenantId tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public Builder stockItems(List<StockItemAssignmentRequest> stockItems) {
            this.stockItems = stockItems != null ? new java.util.ArrayList<>(stockItems) : null;
            return this;
        }

        public AssignLocationsFEFOCommand build() {
            if (tenantId == null) {
                throw new IllegalArgumentException("TenantId is required");
            }
            if (stockItems == null || stockItems.isEmpty()) {
                throw new IllegalArgumentException("Stock items list cannot be empty");
            }
            return new AssignLocationsFEFOCommand(this);
        }
    }
}

