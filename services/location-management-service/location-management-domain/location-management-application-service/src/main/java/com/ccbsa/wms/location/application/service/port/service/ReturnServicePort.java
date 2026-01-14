package com.ccbsa.wms.location.application.service.port.service;

import java.util.Optional;

import com.ccbsa.common.domain.valueobject.OrderNumber;
import com.ccbsa.common.domain.valueobject.ProductCondition;
import com.ccbsa.common.domain.valueobject.ProductId;
import com.ccbsa.common.domain.valueobject.ReturnId;
import com.ccbsa.common.domain.valueobject.ReturnLineItemId;
import com.ccbsa.common.domain.valueobject.ReturnStatus;
import com.ccbsa.common.domain.valueobject.TenantId;

/**
 * Service Port: ReturnServicePort
 * <p>
 * Defines the contract for Returns Service integration. Implemented by infrastructure adapters.
 * <p>
 * This port is used for fetching return details during location assignment.
 */
public interface ReturnServicePort {
    /**
     * Gets return details by return ID.
     *
     * @param returnId Return identifier
     * @param tenantId Tenant identifier
     * @return Optional ReturnDetails if return exists
     */
    Optional<ReturnDetails> getReturnDetails(ReturnId returnId, TenantId tenantId);

    /**
     * DTO for return details.
     */
    record ReturnDetails(ReturnId returnId, OrderNumber orderNumber, ReturnStatus status, java.util.List<ReturnLineItemDetails> lineItems) {
    }

    /**
     * DTO for return line item details.
     */
    record ReturnLineItemDetails(ReturnLineItemId lineItemId, ProductId productId, int returnedQuantity, ProductCondition productCondition) {
    }
}
