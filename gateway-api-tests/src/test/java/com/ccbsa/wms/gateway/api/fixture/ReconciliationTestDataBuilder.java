package com.ccbsa.wms.gateway.api.fixture;

import java.time.LocalDate;

import com.ccbsa.wms.gateway.api.dto.CreateReconciliationCountRequest;

/**
 * Builder for creating reconciliation count test data.
 */
public class ReconciliationTestDataBuilder {

    public static CreateReconciliationCountRequest buildCreateReconciliationCountRequest(
            String locationId, String userId) {
        return CreateReconciliationCountRequest.builder()
                .locationId(locationId)
                .countType("CYCLE_COUNT")
                .scheduledDate(LocalDate.now().plusDays(1))
                .assignedTo(userId)
                .build();
    }

    public static CreateReconciliationCountRequest buildCreateReconciliationCountRequestWithType(
            String locationId, String userId, String countType) {
        return CreateReconciliationCountRequest.builder()
                .locationId(locationId)
                .countType(countType)
                .scheduledDate(LocalDate.now().plusDays(1))
                .assignedTo(userId)
                .build();
    }
}

