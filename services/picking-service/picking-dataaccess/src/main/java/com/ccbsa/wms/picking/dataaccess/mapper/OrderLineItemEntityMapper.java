package com.ccbsa.wms.picking.dataaccess.mapper;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.ccbsa.common.domain.valueobject.Quantity;
import com.ccbsa.wms.picking.dataaccess.entity.OrderLineItemEntity;
import com.ccbsa.wms.picking.domain.core.entity.OrderLineItem;
import com.ccbsa.wms.picking.domain.core.valueobject.OrderLineItemId;
import com.ccbsa.wms.picking.domain.core.valueobject.ProductCode;

/**
 * Mapper: OrderLineItemEntityMapper
 * <p>
 * Maps between OrderLineItem domain entity and OrderLineItemEntity JPA entity.
 */
@Component
public class OrderLineItemEntityMapper {

    public OrderLineItemEntity toEntity(OrderLineItem lineItem) {
        if (lineItem == null) {
            throw new IllegalArgumentException("OrderLineItem cannot be null");
        }

        if (lineItem.getProductCode() == null) {
            throw new IllegalStateException("OrderLineItem has null product code. LineItem ID: " + (lineItem.getId() != null ? lineItem.getId().getValueAsString() : "null"));
        }
        if (lineItem.getId() == null) {
            throw new IllegalStateException("OrderLineItem ID cannot be null when mapping to entity");
        }

        OrderLineItemEntity entity = new OrderLineItemEntity();
        UUID lineItemId = lineItem.getId().getValue();
        if (lineItemId == null) {
            throw new IllegalStateException("OrderLineItem ID value cannot be null when mapping to entity");
        }
        entity.setId(lineItemId);
        entity.setProductCode(lineItem.getProductCode().getValue());
        entity.setQuantity(lineItem.getQuantity().getValue());
        entity.setNotes(lineItem.getNotes() != null ? lineItem.getNotes().getValue() : null);

        return entity;
    }

    public OrderLineItem toDomain(OrderLineItemEntity entity) {
        if (entity == null) {
            throw new IllegalArgumentException("OrderLineItemEntity cannot be null");
        }

        if (entity.getProductCode() == null || entity.getProductCode().isBlank()) {
            throw new IllegalStateException("OrderLineItemEntity has null or blank product code. Entity ID: " + entity.getId());
        }

        // Build domain entity using builder
        return OrderLineItem.builder().id(OrderLineItemId.of(entity.getId())).productCode(ProductCode.of(entity.getProductCode())).quantity(Quantity.of(entity.getQuantity()))
                .notes(entity.getNotes()).build();
    }
}
