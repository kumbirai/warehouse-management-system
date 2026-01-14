package com.ccbsa.wms.picking.application.service.query;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.picking.application.service.port.repository.OrderRepository;
import com.ccbsa.wms.picking.application.service.port.repository.PickingTaskRepository;
import com.ccbsa.wms.picking.application.service.port.service.ProductServicePort;
import com.ccbsa.wms.picking.application.service.query.dto.GetOrderQuery;
import com.ccbsa.wms.picking.application.service.query.dto.OrderQueryResult;
import com.ccbsa.wms.picking.domain.core.entity.Order;
import com.ccbsa.wms.picking.domain.core.entity.OrderLineItem;
import com.ccbsa.wms.picking.domain.core.entity.PickingTask;
import com.ccbsa.wms.picking.domain.core.exception.OrderNotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Query Handler: GetOrderQueryHandler
 * <p>
 * Handles query for getting an order by order number with picked quantities.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class GetOrderQueryHandler {
    private final OrderRepository orderRepository;
    private final PickingTaskRepository pickingTaskRepository;
    private final ProductServicePort productServicePort;

    @Transactional(readOnly = true)
    public OrderQueryResult handle(GetOrderQuery query) {
        log.debug("Getting order by order number: {}, tenant: {}", query.getOrderNumber().getValue(), query.getTenantId().getValue());

        // 1. Find order by order number
        Order order = orderRepository.findByOrderNumberAndTenantId(query.getOrderNumber(), query.getTenantId())
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + query.getOrderNumber().getValue()));

        // 2. Get all picking tasks for this order
        List<PickingTask> pickingTasks = pickingTaskRepository.findByOrderId(order.getId());

        // 3. Aggregate picked quantities by product code
        Map<String, Integer> pickedQuantitiesByProductCode = pickingTasks.stream()
                .filter(task -> task.getPickedQuantity() != null && task.getPickedQuantity().isPositive())
                .collect(Collectors.groupingBy(
                        task -> task.getProductCode().getValue(),
                        Collectors.summingInt(task -> task.getPickedQuantity().getValue())
                ));

        // 4. Build line items with picked quantities
        List<OrderQueryResult.OrderLineItemQueryResult> lineItemResults = order.getLineItems().stream()
                .map(lineItem -> toLineItemQueryResult(lineItem, pickedQuantitiesByProductCode, query.getTenantId()))
                .collect(Collectors.toList());

        // Create defensive copy of line items list for builder
        return OrderQueryResult.builder()
                .orderNumber(order.getOrderNumber().getValue())
                .lineItems(new ArrayList<>(lineItemResults))
                .build();
    }

    private OrderQueryResult.OrderLineItemQueryResult toLineItemQueryResult(OrderLineItem lineItem,
                                                                              Map<String, Integer> pickedQuantitiesByProductCode,
                                                                              TenantId tenantId) {
        // Get picked quantity for this product code
        Integer pickedQuantity = pickedQuantitiesByProductCode.getOrDefault(lineItem.getProductCode().getValue(), 0);

        // Get product ID from product service
        String productId = null;
        try {
            var productInfo = productServicePort.getProductByCode(lineItem.getProductCode().getValue(), tenantId);
            if (productInfo.isPresent()) {
                productId = productInfo.get().getProductId();
            }
        } catch (Exception e) {
            log.warn("Failed to fetch product ID for code: {}. Error: {}", lineItem.getProductCode().getValue(), e.getMessage());
            // Continue without product ID - this should not happen in production
        }

        return OrderQueryResult.OrderLineItemQueryResult.builder()
                .productId(productId)
                .orderedQuantity(lineItem.getQuantity().getValue())
                .pickedQuantity(pickedQuantity)
                .build();
    }
}
