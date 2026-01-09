package com.ccbsa.wms.picking.application.service.query;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.picking.application.service.port.data.LoadViewRepository;
import com.ccbsa.wms.picking.application.service.port.repository.OrderRepository;
import com.ccbsa.wms.picking.application.service.port.service.ProductServicePort;
import com.ccbsa.wms.picking.application.service.query.dto.GetLoadQuery;
import com.ccbsa.wms.picking.application.service.query.dto.LoadQueryResult;
import com.ccbsa.wms.picking.application.service.query.dto.LoadView;
import com.ccbsa.wms.picking.domain.core.entity.Order;
import com.ccbsa.wms.picking.domain.core.entity.OrderLineItem;
import com.ccbsa.wms.picking.domain.core.exception.LoadNotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Query Handler: GetLoadQueryHandler
 * <p>
 * Handles query for getting a load by ID.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class GetLoadQueryHandler {
    private final LoadViewRepository viewRepository;
    private final OrderRepository orderRepository;
    private final ProductServicePort productServicePort;

    @Transactional(readOnly = true)
    public LoadQueryResult handle(GetLoadQuery query) {
        LoadView view =
                viewRepository.findByIdAndTenantId(query.getLoadId(), query.getTenantId()).orElseThrow(() -> new LoadNotFoundException(query.getLoadId().getValueAsString()));

        // Fetch orders for this load
        List<Order> orders = orderRepository.findByLoadId(query.getLoadId());

        return toQueryResult(view, orders, query.getTenantId());
    }

    private LoadQueryResult toQueryResult(LoadView view, List<Order> orders, TenantId tenantId) {
        List<LoadQueryResult.OrderQueryResult> orderResults = orders.stream().map(order -> toOrderQueryResult(order, tenantId)).collect(Collectors.toList());

        return LoadQueryResult.builder().id(view.getId()).loadNumber(view.getLoadNumber()).status(view.getStatus()).createdAt(view.getCreatedAt()).plannedAt(view.getPlannedAt())
                .orders(orderResults).build();
    }

    private LoadQueryResult.OrderQueryResult toOrderQueryResult(Order order, TenantId tenantId) {
        List<LoadQueryResult.OrderLineItemQueryResult> lineItemResults =
                order.getLineItems().stream().map(lineItem -> toLineItemQueryResult(lineItem, tenantId)).collect(Collectors.toList());

        return LoadQueryResult.OrderQueryResult.builder().orderId(order.getId().getValueAsString()).orderNumber(order.getOrderNumber().getValue())
                .customerCode(order.getCustomerInfo().getCustomerCode()).customerName(order.getCustomerInfo().getCustomerName())
                .priority(order.getPriority() != null ? order.getPriority().name() : null).status(order.getStatus() != null ? order.getStatus().name() : null)
                .lineItems(lineItemResults).build();
    }

    private LoadQueryResult.OrderLineItemQueryResult toLineItemQueryResult(OrderLineItem lineItem, TenantId tenantId) {
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

        return LoadQueryResult.OrderLineItemQueryResult.builder().lineItemId(lineItem.getId().getValueAsString()).productCode(lineItem.getProductCode().getValue())
                .productDescription(productDescription).quantity(lineItem.getQuantity().getValue()).notes(lineItem.getNotes() != null ? lineItem.getNotes().getValue() : null)
                .build();
    }
}
