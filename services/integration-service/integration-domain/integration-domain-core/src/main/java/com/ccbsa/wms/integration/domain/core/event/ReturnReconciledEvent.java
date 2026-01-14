package com.ccbsa.wms.integration.domain.core.event;

import java.util.Optional;

import com.ccbsa.common.domain.DomainEvent;
import com.ccbsa.common.domain.EventMetadata;
import com.ccbsa.common.domain.valueobject.ReturnId;
import com.ccbsa.common.domain.valueobject.TenantId;

/**
 * Domain Event: ReturnReconciledEvent
 * <p>
 * Published after successful D365 reconciliation for a return.
 * <p>
 * This event indicates that:
 * - Return has been reconciled with D365
 * - D365 return order has been created
 * - Inventory has been adjusted in D365
 * - Financial reconciliation (credit notes, write-offs) has been processed
 */
public class ReturnReconciledEvent extends DomainEvent<ReturnId> {
    private final TenantId tenantId;
    private final String d365ReturnOrderId;
    private final boolean inventoryAdjusted;
    private final Optional<String> creditNoteId;
    private final boolean writeOffProcessed;

    /**
     * Constructor for ReturnReconciledEvent.
     *
     * @param returnId          Return identifier
     * @param tenantId          Tenant identifier
     * @param d365ReturnOrderId D365 return order ID
     * @param inventoryAdjusted Whether inventory was adjusted
     * @param creditNoteId      Optional credit note ID
     * @param writeOffProcessed Whether write-off was processed
     */
    public ReturnReconciledEvent(ReturnId returnId, TenantId tenantId, String d365ReturnOrderId, boolean inventoryAdjusted, Optional<String> creditNoteId,
                                 boolean writeOffProcessed) {
        super(returnId.getValueAsString(), "Return");
        this.tenantId = tenantId;
        this.d365ReturnOrderId = d365ReturnOrderId;
        this.inventoryAdjusted = inventoryAdjusted;
        this.creditNoteId = creditNoteId;
        this.writeOffProcessed = writeOffProcessed;
    }

    /**
     * Constructor for ReturnReconciledEvent with metadata.
     *
     * @param returnId          Return identifier
     * @param tenantId          Tenant identifier
     * @param d365ReturnOrderId D365 return order ID
     * @param inventoryAdjusted Whether inventory was adjusted
     * @param creditNoteId      Optional credit note ID
     * @param writeOffProcessed Whether write-off was processed
     * @param metadata          Event metadata (correlation ID, user ID, etc.)
     */
    public ReturnReconciledEvent(ReturnId returnId, TenantId tenantId, String d365ReturnOrderId, boolean inventoryAdjusted, Optional<String> creditNoteId,
                                 boolean writeOffProcessed, EventMetadata metadata) {
        super(returnId.getValueAsString(), "Return", metadata);
        this.tenantId = tenantId;
        this.d365ReturnOrderId = d365ReturnOrderId;
        this.inventoryAdjusted = inventoryAdjusted;
        this.creditNoteId = creditNoteId;
        this.writeOffProcessed = writeOffProcessed;
    }

    public TenantId getTenantId() {
        return tenantId;
    }

    public String getD365ReturnOrderId() {
        return d365ReturnOrderId;
    }

    public boolean isInventoryAdjusted() {
        return inventoryAdjusted;
    }

    public Optional<String> getCreditNoteId() {
        return creditNoteId;
    }

    public boolean isWriteOffProcessed() {
        return writeOffProcessed;
    }
}
