package com.ccbsa.wms.location.application.service.command.dto;

import java.util.List;

import com.ccbsa.common.domain.valueobject.ReturnId;
import com.ccbsa.common.domain.valueobject.TenantId;

import lombok.Builder;
import lombok.Getter;

/**
 * Command DTO: AssignReturnLocationsCommand
 * <p>
 * Command for assigning locations to return line items based on product condition.
 */
@Getter
@Builder
public class AssignReturnLocationsCommand {
    private final ReturnId returnId;
    private final TenantId tenantId;
    private final List<ReturnLineItemAssignment> lineItemAssignments;

    /**
     * DTO for return line item assignment.
     */
    @Getter
    @Builder
    public static class ReturnLineItemAssignment {
        private final String lineItemId;
        private final String productId;
        private final int quantity;
        private final String productCondition;
    }
}
