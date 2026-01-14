package com.ccbsa.wms.returns.application.service.port.service;

import java.util.Optional;

import com.ccbsa.common.domain.valueobject.OrderNumber;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.returns.application.service.port.service.dto.PickingOrderDetails;

/**
 * Service Port: PickingServicePort
 * <p>
 * Defines the contract for Picking Service integration. Implemented by infrastructure adapters.
 * <p>
 * This port is used for fetching picking order details during return processing.
 */
public interface PickingServicePort {
    /**
     * Gets picking order details by order number.
     *
     * @param orderNumber Order number
     * @param tenantId    Tenant identifier
     * @return Optional PickingOrderDetails if order exists and picking is completed
     */
    Optional<PickingOrderDetails> getPickingOrderDetails(OrderNumber orderNumber, TenantId tenantId);

    /**
     * Checks if order picking is completed.
     *
     * @param orderNumber Order number
     * @param tenantId    Tenant identifier
     * @return true if picking is completed, false otherwise
     */
    boolean isOrderPickingCompleted(OrderNumber orderNumber, TenantId tenantId);
}
