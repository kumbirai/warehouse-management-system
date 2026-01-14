package com.ccbsa.wms.integration.application.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import com.ccbsa.common.domain.valueobject.OrderNumber;
import com.ccbsa.common.domain.valueobject.ProductCondition;
import com.ccbsa.common.domain.valueobject.ReturnId;
import com.ccbsa.common.domain.valueobject.ReturnReason;
import com.ccbsa.common.domain.valueobject.ReturnStatus;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.integration.application.service.port.service.D365ClientPort;
import com.ccbsa.wms.integration.application.service.port.service.ReturnServicePort;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Application Service: D365ReturnReconciliationService
 * <p>
 * Handles D365 reconciliation for returns:
 * - Create D365 return orders
 * - Adjust inventory in D365
 * - Process financial reconciliation (credit notes, write-offs)
 * <p>
 * Uses @Retryable for retry logic on transient failures.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class D365ReturnReconciliationService {
    private final D365ClientPort d365ClientPort;
    private final ReturnServicePort returnServicePort;

    /**
     * Reconciles a return with D365.
     * <p>
     * Steps:
     * 1. Load return details
     * 2. Create return order in D365
     * 3. Adjust inventory in D365
     * 4. Process financial reconciliation (credit notes, write-offs)
     *
     * @param returnId Return identifier
     * @param tenantId Tenant identifier
     * @return Reconciliation result
     */
    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public ReconciliationResult reconcileReturn(ReturnId returnId, TenantId tenantId) {
        log.info("Starting D365 reconciliation for return: returnId={}, tenantId={}", returnId.getValueAsString(), tenantId.getValue());

        // 1. Load return details
        ReturnServicePort.ReturnDetails returnDetails =
                returnServicePort.getReturnDetails(returnId, tenantId).orElseThrow(() -> new IllegalStateException("Return not found: " + returnId.getValueAsString()));

        // 2. Validate return is in correct status
        if (returnDetails.status() != ReturnStatus.PROCESSED && returnDetails.status() != ReturnStatus.LOCATION_ASSIGNED) {
            throw new IllegalStateException("Return must be in PROCESSED or LOCATION_ASSIGNED status for reconciliation. Current status: " + returnDetails.status());
        }

        // 3. Create return order in D365
        D365ClientPort.ReturnOrderData returnOrderData = buildReturnOrderData(returnDetails);
        Optional<String> d365ReturnOrderId = d365ClientPort.createReturnOrder(returnOrderData, tenantId);

        if (d365ReturnOrderId.isEmpty()) {
            throw new RuntimeException("Failed to create return order in D365");
        }

        log.info("Created return order in D365: returnId={}, d365ReturnOrderId={}", returnId.getValueAsString(), d365ReturnOrderId.get());

        // 4. Adjust inventory in D365
        D365ClientPort.InventoryAdjustmentData inventoryAdjustmentData = buildInventoryAdjustmentData(returnDetails);
        boolean inventoryAdjusted = d365ClientPort.adjustInventory(inventoryAdjustmentData, tenantId);

        if (!inventoryAdjusted) {
            throw new RuntimeException("Failed to adjust inventory in D365");
        }

        log.info("Adjusted inventory in D365: returnId={}", returnId.getValueAsString());

        // 5. Process financial reconciliation
        FinancialReconciliationResult financialResult = processFinancialReconciliation(returnDetails, tenantId);

        log.info("Completed D365 reconciliation for return: returnId={}, d365ReturnOrderId={}, creditNoteId={}, writeOffProcessed={}", returnId.getValueAsString(),
                d365ReturnOrderId.get(), financialResult.creditNoteId().orElse("N/A"), financialResult.writeOffProcessed());

        return ReconciliationResult.builder().returnId(returnId).d365ReturnOrderId(d365ReturnOrderId.get()).inventoryAdjusted(inventoryAdjusted)
                .creditNoteId(financialResult.creditNoteId()).writeOffProcessed(financialResult.writeOffProcessed()).build();
    }

    /**
     * Builds return order data for D365.
     */
    private D365ClientPort.ReturnOrderData buildReturnOrderData(ReturnServicePort.ReturnDetails returnDetails) {
        List<D365ClientPort.ReturnOrderLineItemData> lineItems = returnDetails.lineItems().stream()
                .map(item -> new D365ClientPort.ReturnOrderLineItemData(item.productId().getValueAsString(), item.returnedQuantity(),
                        item.productCondition() != null ? item.productCondition().name() : null, null)) // Return reason would come from line item
                .collect(Collectors.toList());

        return new D365ClientPort.ReturnOrderData(returnDetails.orderNumber(), returnDetails.status().name(), lineItems);
    }

    /**
     * Builds inventory adjustment data for D365.
     */
    private D365ClientPort.InventoryAdjustmentData buildInventoryAdjustmentData(ReturnServicePort.ReturnDetails returnDetails) {
        List<D365ClientPort.InventoryAdjustmentLineItemData> lineItems = returnDetails.lineItems().stream().map(item -> {
            // For returns, we increase inventory (products coming back)
            String adjustmentType = "INCREASE";
            return new D365ClientPort.InventoryAdjustmentLineItemData(item.productId().getValueAsString(), item.returnedQuantity(), adjustmentType);
        }).collect(Collectors.toList());

        return new D365ClientPort.InventoryAdjustmentData(returnDetails.orderNumber(), "RETURN", lineItems);
    }

    /**
     * Processes financial reconciliation (credit notes, write-offs).
     */
    private FinancialReconciliationResult processFinancialReconciliation(ReturnServicePort.ReturnDetails returnDetails, TenantId tenantId) {
        Optional<String> creditNoteId = Optional.empty();
        boolean writeOffProcessed = false;

        // Determine if credit note or write-off is needed based on product condition
        for (ReturnServicePort.ReturnLineItemDetails lineItem : returnDetails.lineItems()) {
            ProductCondition condition = lineItem.productCondition();
            if (condition == null) {
                continue;
            }

            switch (condition) {
                case GOOD:
                case DAMAGED:
                    // Create credit note for good/damaged products
                    if (creditNoteId.isEmpty()) {
                        D365ClientPort.CreditNoteData creditNoteData =
                                new D365ClientPort.CreditNoteData(returnDetails.orderNumber(), java.math.BigDecimal.ZERO, "USD", "Return credit");
                        creditNoteId = d365ClientPort.createCreditNote(creditNoteData, tenantId);
                    }
                    break;

                case EXPIRED:
                case WRITE_OFF:
                    // Process write-off for expired/write-off products
                    D365ClientPort.WriteOffData writeOffData = new D365ClientPort.WriteOffData(returnDetails.orderNumber(), java.math.BigDecimal.ZERO, "USD", "Return write-off");
                    writeOffProcessed = d365ClientPort.processWriteOff(writeOffData, tenantId);
                    break;

                default:
                    // No financial reconciliation needed
                    break;
            }
        }

        return new FinancialReconciliationResult(creditNoteId, writeOffProcessed);
    }

    /**
     * Result of financial reconciliation.
     */
    private record FinancialReconciliationResult(Optional<String> creditNoteId, boolean writeOffProcessed) {
    }

    /**
     * Result of D365 reconciliation.
     */
    @lombok.Builder
    @lombok.Getter
    public static class ReconciliationResult {
        private final ReturnId returnId;
        private final String d365ReturnOrderId;
        private final boolean inventoryAdjusted;
        private final Optional<String> creditNoteId;
        private final boolean writeOffProcessed;
    }
}
