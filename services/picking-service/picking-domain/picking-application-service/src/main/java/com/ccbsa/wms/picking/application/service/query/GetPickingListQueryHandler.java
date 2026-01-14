package com.ccbsa.wms.picking.application.service.query;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.picking.application.service.port.repository.OrderRepository;
import com.ccbsa.wms.picking.application.service.port.repository.PickingListRepository;
import com.ccbsa.wms.picking.application.service.port.service.ProductServicePort;
import com.ccbsa.wms.picking.application.service.query.dto.GetPickingListQuery;
import com.ccbsa.wms.picking.application.service.query.dto.PickingListQueryResult;
import com.ccbsa.wms.picking.domain.core.entity.Load;
import com.ccbsa.wms.picking.domain.core.entity.Order;
import com.ccbsa.wms.picking.domain.core.entity.OrderLineItem;
import com.ccbsa.wms.picking.domain.core.exception.PickingListNotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Query Handler: GetPickingListQueryHandler
 * <p>
 * Handles query for getting a picking list by ID.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class GetPickingListQueryHandler {
    private final PickingListRepository pickingListRepository;
    private final OrderRepository orderRepository;
    private final ProductServicePort productServicePort;

    @Transactional(readOnly = true)
    public PickingListQueryResult handle(GetPickingListQuery query) {
        var pickingList = pickingListRepository.findByIdAndTenantId(query.getPickingListId(), query.getTenantId())
                .orElseThrow(() -> new PickingListNotFoundException(query.getPickingListId().getValueAsString()));

        // Fetch loads for this picking list
        List<Load> loads = pickingList.getLoads();

        // Build load results with orders
        List<PickingListQueryResult.LoadQueryResult> loadResults = loads.stream().map(load -> {
            List<Order> orders = orderRepository.findByLoadId(load.getId());
            return toLoadQueryResult(load, orders, query.getTenantId());
        }).collect(Collectors.toList());

        // Create defensive copy of loads list for builder
        return PickingListQueryResult.builder().id(pickingList.getId()).pickingListReference(pickingList.getPickingListReference()).status(pickingList.getStatus())
                .receivedAt(pickingList.getReceivedAt()).processedAt(pickingList.getProcessedAt()).loadCount(pickingList.getLoadCount())
                .totalOrderCount(pickingList.getTotalOrderCount()).notes(pickingList.getNotes() != null ? pickingList.getNotes().getValue() : null)
                .loads(new java.util.ArrayList<>(loadResults)).build();
    }

    private PickingListQueryResult.LoadQueryResult toLoadQueryResult(Load load, List<Order> orders, TenantId tenantId) {
        List<PickingListQueryResult.OrderQueryResult> orderResults = orders.stream().map(order -> toOrderQueryResult(order, tenantId)).collect(Collectors.toList());

        // Create defensive copy of orders list for builder
        return PickingListQueryResult.LoadQueryResult.builder().loadId(load.getId().getValueAsString()).loadNumber(load.getLoadNumber().getValue())
                .status(load.getStatus() != null ? load.getStatus().name() : null).orderCount(load.getOrderCount()).orders(new java.util.ArrayList<>(orderResults)).build();
    }

    private PickingListQueryResult.OrderQueryResult toOrderQueryResult(Order order, TenantId tenantId) {
        List<PickingListQueryResult.OrderLineItemQueryResult> lineItemResults =
                order.getLineItems().stream().map(lineItem -> toLineItemQueryResult(lineItem, tenantId)).collect(Collectors.toList());

        // Create defensive copy of line items list for builder
        return PickingListQueryResult.OrderQueryResult.builder().orderId(order.getId().getValueAsString()).orderNumber(order.getOrderNumber().getValue())
                .customerCode(order.getCustomerInfo().getCustomerCode()).customerName(order.getCustomerInfo().getCustomerName())
                .priority(order.getPriority() != null ? order.getPriority().name() : null).status(order.getStatus() != null ? order.getStatus().name() : null)
                .lineItems(new java.util.ArrayList<>(lineItemResults)).build();
    }

    private PickingListQueryResult.OrderLineItemQueryResult toLineItemQueryResult(OrderLineItem lineItem, TenantId tenantId) {
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

        return PickingListQueryResult.OrderLineItemQueryResult.builder().lineItemId(lineItem.getId().getValueAsString()).productCode(lineItem.getProductCode().getValue())
                .productDescription(productDescription).quantity(lineItem.getQuantity().getValue()).notes(lineItem.getNotes() != null ? lineItem.getNotes().getValue() : null)
                .build();
    }
}
