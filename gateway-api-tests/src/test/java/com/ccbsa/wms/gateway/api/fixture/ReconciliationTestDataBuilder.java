package com.ccbsa.wms.gateway.api.fixture;

import java.time.LocalDate;

import com.ccbsa.wms.gateway.api.dto.CreateReconciliationCountRequest;

/**
 * Builder for creating reconciliation test data.
 * <p>
 * Note: Reconciliation data is typically created through the returns flow,
 * so this builder provides helper methods for reconciliation-related assertions.
 */
public class ReconciliationTestDataBuilder {

    /**
     * Creates a mock D365 return order ID for testing.
     *
     * @return Mock D365 return order ID
     */
    public static String mockD365ReturnOrderId() {
        return "D365-RET-" + TestData.faker().number().digits(8);
    }

    /**
     * Creates a mock D365 credit note ID for testing.
     *
     * @return Mock D365 credit note ID
     */
    public static String mockD365CreditNoteId() {
        return "D365-CN-" + TestData.faker().number().digits(8);
    }

    /**
     * Creates a CreateReconciliationCountRequest for testing.
     *
     * @param locationId Location ID for the count
     * @param assignedTo User ID to assign the count to
     * @return CreateReconciliationCountRequest instance
     */
    public static CreateReconciliationCountRequest buildCreateReconciliationCountRequest(String locationId, String assignedTo) {
        return CreateReconciliationCountRequest.builder().locationId(locationId).countType("CYCLE_COUNT").scheduledDate(LocalDate.now().plusDays(1)).assignedTo(assignedTo).build();
    }
}
