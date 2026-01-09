package com.ccbsa.wms.picking.dataaccess.mapper;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.ccbsa.common.domain.valueobject.CustomerInfo;
import com.ccbsa.common.domain.valueobject.OrderNumber;
import com.ccbsa.wms.picking.dataaccess.entity.OrderEntity;
import com.ccbsa.wms.picking.dataaccess.entity.OrderLineItemEntity;
import com.ccbsa.wms.picking.domain.core.entity.Order;
import com.ccbsa.wms.picking.domain.core.entity.OrderLineItem;
import com.ccbsa.wms.picking.domain.core.valueobject.OrderId;

import lombok.RequiredArgsConstructor;

/**
 * Mapper: OrderEntityMapper
 * <p>
 * Maps between Order domain entity and OrderEntity JPA entity.
 */
@Component
@RequiredArgsConstructor
public class OrderEntityMapper {
    private final OrderLineItemEntityMapper orderLineItemEntityMapper;

    public OrderEntity toEntity(Order order) {
        if (order == null) {
            throw new IllegalArgumentException("Order cannot be null");
        }
        if (order.getId() == null) {
            throw new IllegalStateException("Order ID cannot be null when mapping to entity");
        }

        OrderEntity entity = new OrderEntity();
        UUID orderId = order.getId().getValue();
        if (orderId == null) {
            throw new IllegalStateException("Order ID value cannot be null when mapping to entity");
        }
        entity.setId(orderId);
        entity.setOrderNumber(order.getOrderNumber().getValue());
        entity.setCustomerCode(order.getCustomerInfo().getCustomerCode());
        entity.setCustomerName(order.getCustomerInfo().getCustomerName());
        entity.setPriority(order.getPriority());
        entity.setStatus(order.getStatus());
        entity.setCreatedAt(order.getCreatedAt());
        entity.setCompletedAt(order.getCompletedAt());

        // Map line items
        List<OrderLineItemEntity> lineItemEntities = new ArrayList<>();
        for (OrderLineItem lineItem : order.getLineItems()) {
            OrderLineItemEntity lineItemEntity = orderLineItemEntityMapper.toEntity(lineItem);
            lineItemEntity.setOrder(entity);
            lineItemEntities.add(lineItemEntity);
        }
        entity.setLineItems(lineItemEntities);

        return entity;
    }

    public Order toDomain(OrderEntity entity) {
        if (entity == null) {
            throw new IllegalArgumentException("OrderEntity cannot be null");
        }

        // Convert line items
        List<OrderLineItem> lineItems = new ArrayList<>();
        if (entity.getLineItems() != null) {
            for (OrderLineItemEntity lineItemEntity : entity.getLineItems()) {
                lineItems.add(orderLineItemEntityMapper.toDomain(lineItemEntity));
            }
        }

        // Build domain entity using builder
        return Order.builder().id(OrderId.of(entity.getId())).orderNumber(OrderNumber.of(entity.getOrderNumber()))
                .customerInfo(CustomerInfo.of(entity.getCustomerCode(), entity.getCustomerName())).priority(entity.getPriority()).status(entity.getStatus()).lineItems(lineItems)
                .createdAt(entity.getCreatedAt()).completedAt(entity.getCompletedAt()).build();
    }
}
