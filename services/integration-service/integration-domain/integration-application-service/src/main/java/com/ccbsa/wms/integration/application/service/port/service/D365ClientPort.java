package com.ccbsa.wms.integration.application.service.port.service;

import java.util.Optional;

import com.ccbsa.common.domain.valueobject.OrderNumber;
import com.ccbsa.common.domain.valueobject.TenantId;

/**
 * Service Port: D365ClientPort
 * <p>
 * Defines the contract for D365 Finance and Operations integration via OData API.
 * <p>
 * This port is used for:
 * - Creating return orders in D365
 * - Adjusting inventory in D365
 * - Processing financial reconciliation (credit notes, write-offs)
 */
public interface D365ClientPort {
    /**
     * Creates a return order in D365.
     *
     * @param returnOrderData Return order data
     * @param tenantId        Tenant identifier
     * @return D365 return order ID if successful
     */
    Optional<String> createReturnOrder(ReturnOrderData returnOrderData, TenantId tenantId);

    /**
     * Adjusts inventory in D365 for a return.
     *
     * @param inventoryAdjustmentData Inventory adjustment data
     * @param tenantId                Tenant identifier
     * @return true if successful
     */
    boolean adjustInventory(InventoryAdjustmentData inventoryAdjustmentData, TenantId tenantId);

    /**
     * Creates a credit note in D365.
     *
     * @param creditNoteData Credit note data
     * @param tenantId       Tenant identifier
     * @return D365 credit note ID if successful
     */
    Optional<String> createCreditNote(CreditNoteData creditNoteData, TenantId tenantId);

    /**
     * Processes a write-off in D365.
     *
     * @param writeOffData Write-off data
     * @param tenantId     Tenant identifier
     * @return true if successful
     */
    boolean processWriteOff(WriteOffData writeOffData, TenantId tenantId);

    /**
     * DTO for return order data.
     */
    record ReturnOrderData(OrderNumber orderNumber, String returnReason, java.util.List<ReturnOrderLineItemData> lineItems) {
    }

    /**
     * DTO for return order line item data.
     */
    record ReturnOrderLineItemData(String productId, int quantity, String productCondition, String returnReason) {
    }

    /**
     * DTO for inventory adjustment data.
     */
    record InventoryAdjustmentData(OrderNumber orderNumber, String adjustmentReason, java.util.List<InventoryAdjustmentLineItemData> lineItems) {
    }

    /**
     * DTO for inventory adjustment line item data.
     */
    record InventoryAdjustmentLineItemData(String productId, int quantity, String adjustmentType // INCREASE, DECREASE
    ) {
    }

    /**
     * DTO for credit note data.
     */
    record CreditNoteData(OrderNumber orderNumber, java.math.BigDecimal amount, String currencyCode, String reason) {
    }

    /**
     * DTO for write-off data.
     */
    record WriteOffData(OrderNumber orderNumber, java.math.BigDecimal amount, String currencyCode, String reason) {
    }
}
