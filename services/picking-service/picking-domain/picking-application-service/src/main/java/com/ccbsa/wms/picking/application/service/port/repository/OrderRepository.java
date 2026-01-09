package com.ccbsa.wms.picking.application.service.port.repository;

import java.util.List;
import java.util.Optional;

import com.ccbsa.common.domain.valueobject.OrderNumber;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.picking.domain.core.entity.Order;
import com.ccbsa.wms.picking.domain.core.valueobject.LoadId;
import com.ccbsa.wms.picking.domain.core.valueobject.OrderId;

/**
 * Repository Port: OrderRepository
 * <p>
 * Defines the contract for Order entity persistence. Implemented by data access adapters.
 */
public interface OrderRepository {
    /**
     * Saves an Order entity.
     *
     * @param order Order entity to save
     */
    void save(Order order);

    /**
     * Finds an Order by ID.
     *
     * @param id Order identifier
     * @return Optional Order if found
     */
    Optional<Order> findById(OrderId id);

    /**
     * Finds an Order by order number and tenant ID.
     *
     * @param orderNumber Order number
     * @param tenantId    Tenant identifier
     * @return Optional Order if found
     */
    Optional<Order> findByOrderNumberAndTenantId(OrderNumber orderNumber, TenantId tenantId);

    /**
     * Finds all orders for a load.
     *
     * @param loadId Load identifier
     * @return List of Order entities
     */
    List<Order> findByLoadId(LoadId loadId);
}
