package com.ccbsa.wms.stock.application.service.command.dto;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.stock.domain.core.valueobject.ConsignmentId;

import lombok.Builder;
import lombok.Getter;

/**
 * Command DTO: ConfirmConsignmentCommand
 * <p>
 * Command object for confirming a stock consignment receipt.
 */
@Getter
@Builder
public final class ConfirmConsignmentCommand {
    private final ConsignmentId consignmentId;
    private final TenantId tenantId;

    public ConfirmConsignmentCommand(ConsignmentId consignmentId, TenantId tenantId) {
        if (consignmentId == null) {
            throw new IllegalArgumentException("ConsignmentId is required");
        }
        if (tenantId == null) {
            throw new IllegalArgumentException("TenantId is required");
        }
        this.consignmentId = consignmentId;
        this.tenantId = tenantId;
    }
}

