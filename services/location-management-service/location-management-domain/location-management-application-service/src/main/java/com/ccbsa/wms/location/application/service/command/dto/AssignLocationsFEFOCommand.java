package com.ccbsa.wms.location.application.service.command.dto;

import java.util.List;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.location.domain.core.valueobject.StockItemAssignmentRequest;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Builder;
import lombok.Getter;

/**
 * Command DTO: AssignLocationsFEFOCommand
 * <p>
 * Command object for assigning locations to stock items based on FEFO principles.
 */
@Getter
@Builder
@SuppressFBWarnings(value = {"EI_EXPOSE_REP2", "RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE"}, justification =
        "EI_EXPOSE_REP2: Lombok builder stores list directly. Defensive copy made in constructor. "
                + "RCN_REDUNDANT_NULLCHECK: Null check removed, stockItems validated as non-null before use.")
public final class AssignLocationsFEFOCommand {
    private final TenantId tenantId;
    private final List<StockItemAssignmentRequest> stockItems;

    public AssignLocationsFEFOCommand(TenantId tenantId, List<StockItemAssignmentRequest> stockItems) {
        if (tenantId == null) {
            throw new IllegalArgumentException("TenantId is required");
        }
        if (stockItems == null || stockItems.isEmpty()) {
            throw new IllegalArgumentException("Stock items list cannot be empty");
        }
        this.tenantId = tenantId;
        // Defensive copy - stockItems is already validated as non-null above
        this.stockItems = List.copyOf(stockItems);
    }
}

