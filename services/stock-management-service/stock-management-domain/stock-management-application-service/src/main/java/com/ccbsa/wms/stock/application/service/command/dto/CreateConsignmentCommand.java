package com.ccbsa.wms.stock.application.service.command.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.common.domain.valueobject.WarehouseId;
import com.ccbsa.wms.stock.domain.core.valueobject.ConsignmentLineItem;
import com.ccbsa.wms.stock.domain.core.valueobject.ConsignmentReference;
import com.ccbsa.wms.stock.domain.core.valueobject.ReceivedBy;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Builder;
import lombok.Getter;

/**
 * Command DTO: CreateConsignmentCommand
 * <p>
 * Command object for creating a new stock consignment.
 */
@Getter
@Builder
@SuppressFBWarnings(value = {"EI_EXPOSE_REP2", "RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE"}, justification =
        "EI_EXPOSE_REP2: Lombok builder stores list directly. Defensive copy made in constructor. "
                + "RCN_REDUNDANT_NULLCHECK: Null check removed, lineItems validated as non-null before use.")
public final class CreateConsignmentCommand {
    private final TenantId tenantId;
    private final ConsignmentReference consignmentReference;
    private final WarehouseId warehouseId;
    private final LocalDateTime receivedAt;
    private final ReceivedBy receivedBy;
    private final List<ConsignmentLineItem> lineItems;

    public CreateConsignmentCommand(TenantId tenantId, ConsignmentReference consignmentReference, WarehouseId warehouseId, LocalDateTime receivedAt, ReceivedBy receivedBy,
                                    List<ConsignmentLineItem> lineItems) {
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
        this.tenantId = tenantId;
        this.consignmentReference = consignmentReference;
        this.warehouseId = warehouseId;
        this.receivedAt = receivedAt;
        this.receivedBy = receivedBy;
        // Defensive copy - lineItems is already validated as non-null above
        this.lineItems = List.copyOf(lineItems);
    }
}

