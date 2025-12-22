package com.ccbsa.wms.stock.application.service.command.dto;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.stock.domain.core.valueobject.ConsignmentId;

/**
 * Command DTO: ConfirmConsignmentCommand
 * <p>
 * Command object for confirming a stock consignment receipt.
 */
public final class ConfirmConsignmentCommand {
    private final ConsignmentId consignmentId;
    private final TenantId tenantId;

    private ConfirmConsignmentCommand(Builder builder) {
        this.consignmentId = builder.consignmentId;
        this.tenantId = builder.tenantId;
    }

    public static Builder builder() {
        return new Builder();
    }

    public ConsignmentId getConsignmentId() {
        return consignmentId;
    }

    public TenantId getTenantId() {
        return tenantId;
    }

    public static class Builder {
        private ConsignmentId consignmentId;
        private TenantId tenantId;

        public Builder consignmentId(ConsignmentId consignmentId) {
            this.consignmentId = consignmentId;
            return this;
        }

        public Builder tenantId(TenantId tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public ConfirmConsignmentCommand build() {
            if (consignmentId == null) {
                throw new IllegalArgumentException("ConsignmentId is required");
            }
            if (tenantId == null) {
                throw new IllegalArgumentException("TenantId is required");
            }
            return new ConfirmConsignmentCommand(this);
        }
    }
}

