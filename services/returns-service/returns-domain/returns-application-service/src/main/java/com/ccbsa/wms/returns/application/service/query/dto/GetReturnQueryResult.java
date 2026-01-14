package com.ccbsa.wms.returns.application.service.query.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.ccbsa.common.domain.valueobject.OrderNumber;
import com.ccbsa.common.domain.valueobject.ReturnReason;
import com.ccbsa.common.domain.valueobject.ReturnStatus;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.returns.domain.core.valueobject.ReturnType;
import com.ccbsa.common.domain.valueobject.ReturnId;

import lombok.Builder;
import lombok.Getter;

/**
 * Query Result DTO: GetReturnQueryResult
 * <p>
 * Query result object for return retrieval.
 */
@Getter
@Builder
public final class GetReturnQueryResult {
    private final ReturnId returnId;
    private final OrderNumber orderNumber;
    private final TenantId tenantId;
    private final ReturnType returnType;
    private final ReturnStatus status;
    private final List<ReturnLineItemResult> lineItems;
    private final ReturnReason primaryReturnReason;
    private final String returnNotes;
    private final LocalDateTime returnedAt;
    private final LocalDateTime createdAt;
    private final LocalDateTime lastModifiedAt;

    @Getter
    @Builder
    public static final class ReturnLineItemResult {
        private final String lineItemId;
        private final String productId;
        private final Integer orderedQuantity;
        private final Integer pickedQuantity;
        private final Integer acceptedQuantity;
        private final Integer returnedQuantity;
        private final String productCondition;
        private final ReturnReason returnReason;
        private final String lineNotes;
    }
}
