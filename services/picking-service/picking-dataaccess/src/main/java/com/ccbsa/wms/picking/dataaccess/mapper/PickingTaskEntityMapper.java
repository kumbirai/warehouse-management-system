package com.ccbsa.wms.picking.dataaccess.mapper;

import org.springframework.stereotype.Component;

import com.ccbsa.common.domain.valueobject.Quantity;
import com.ccbsa.wms.picking.dataaccess.entity.PickingTaskEntity;
import com.ccbsa.wms.picking.domain.core.entity.PickingTask;
import com.ccbsa.wms.picking.domain.core.valueobject.LoadId;
import com.ccbsa.wms.picking.domain.core.valueobject.LocationId;
import com.ccbsa.wms.picking.domain.core.valueobject.OrderId;
import com.ccbsa.wms.picking.domain.core.valueobject.PickingTaskId;
import com.ccbsa.wms.picking.domain.core.valueobject.ProductCode;

/**
 * Mapper: PickingTaskEntityMapper
 * <p>
 * Maps between PickingTask domain entity and PickingTaskEntity JPA entity.
 */
@Component
public class PickingTaskEntityMapper {

    public PickingTaskEntity toEntity(PickingTask pickingTask) {
        if (pickingTask == null) {
            throw new IllegalArgumentException("PickingTask cannot be null");
        }

        PickingTaskEntity entity = new PickingTaskEntity();
        entity.setId(pickingTask.getId().getValue());
        entity.setLoadId(pickingTask.getLoadId().getValue());
        entity.setOrderId(pickingTask.getOrderId().getValue());
        entity.setProductCode(pickingTask.getProductCode().getValue());
        entity.setLocationId(pickingTask.getLocationId().getValue());
        entity.setQuantity(pickingTask.getQuantity().getValue());
        entity.setStatus(pickingTask.getStatus());
        entity.setSequence(pickingTask.getSequence());

        return entity;
    }

    public PickingTask toDomain(PickingTaskEntity entity) {
        if (entity == null) {
            throw new IllegalArgumentException("PickingTaskEntity cannot be null");
        }

        // Build domain entity using builder
        return PickingTask.builder().id(PickingTaskId.of(entity.getId())).loadId(LoadId.of(entity.getLoadId())).orderId(OrderId.of(entity.getOrderId()))
                .productCode(ProductCode.of(entity.getProductCode())).locationId(LocationId.of(entity.getLocationId())).quantity(Quantity.of(entity.getQuantity()))
                .status(entity.getStatus()).sequence(entity.getSequence()).build();
    }
}
