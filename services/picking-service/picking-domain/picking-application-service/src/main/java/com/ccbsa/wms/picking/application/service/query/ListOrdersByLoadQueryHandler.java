package com.ccbsa.wms.picking.application.service.query;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.picking.application.service.port.repository.OrderRepository;
import com.ccbsa.wms.picking.application.service.port.service.ProductServicePort;
import com.ccbsa.wms.picking.application.service.query.dto.ListOrdersByLoadQuery;
import com.ccbsa.wms.picking.application.service.query.dto.ListOrdersByLoadQueryResult;
import com.ccbsa.wms.picking.domain.core.entity.Order;
import com.ccbsa.wms.picking.domain.core.entity.OrderLineItem;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Query Handler: ListOrdersByLoadQueryHandler
 * <p>
 * Handles query for listing orders by load.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ListOrdersByLoadQueryHandler {
    private final OrderRepository orderRepository;
    private final ProductServicePort productServicePort;

    @Transactional(readOnly = true)
    public ListOrdersByLoadQueryResult handle(ListOrdersByLoadQuery query) {
        var orders = orderRepository.findByLoadId(query.getLoadId());

        List<ListOrdersByLoadQueryResult.OrderQueryResult> orderResults = orders.stream().map(order -> toOrderQueryResult(order, query.getTenantId())).collect(Collectors.toList());

        // Create defensive copy of orders list for builder
        return ListOrdersByLoadQueryResult.builder().loadId(query.getLoadId().getValueAsString()).orders(new java.util.ArrayList<>(orderResults)).build();
    }

    private ListOrdersByLoadQueryResult.OrderQueryResult toOrderQueryResult(Order order, TenantId tenantId) {
        List<ListOrdersByLoadQueryResult.OrderLineItemQueryResult> lineItemResults =
                order.getLineItems().stream().map(lineItem -> toLineItemQueryResult(lineItem, tenantId)).collect(Collectors.toList());

        // Create defensive copy of line items list for builder
        return ListOrdersByLoadQueryResult.OrderQueryResult.builder().orderId(order.getId().getValueAsString()).orderNumber(order.getOrderNumber().getValue())
                .customerCode(order.getCustomerInfo().getCustomerCode()).customerName(order.getCustomerInfo().getCustomerName())
                .priority(order.getPriority() != null ? order.getPriority().name() : null).status(order.getStatus() != null ? order.getStatus().name() : null)
                .lineItems(new java.util.ArrayList<>(lineItemResults)).build();
    }

    private ListOrdersByLoadQueryResult.OrderLineItemQueryResult toLineItemQueryResult(OrderLineItem lineItem, TenantId tenantId) {
        // Fetch product description from Product Service
        String productDescription = null;
        try {
            var productInfo = productServicePort.getProductByCode(lineItem.getProductCode().getValue(), tenantId);
            if (productInfo.isPresent()) {
                productDescription = productInfo.get().getDescription();
            }
        } catch (Exception e) {
            log.warn("Failed to fetch product description for code: {}. Error: {}", lineItem.getProductCode().getValue(), e.getMessage());
            // Graceful degradation: continue without description
        }

        return ListOrdersByLoadQueryResult.OrderLineItemQueryResult.builder().lineItemId(lineItem.getId().getValueAsString()).productCode(lineItem.getProductCode().getValue())
                .productDescription(productDescription).quantity(lineItem.getQuantity().getValue()).notes(lineItem.getNotes() != null ? lineItem.getNotes().getValue() : null)
                .build();
    }
}
