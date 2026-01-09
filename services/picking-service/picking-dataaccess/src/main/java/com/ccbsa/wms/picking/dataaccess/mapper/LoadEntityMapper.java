package com.ccbsa.wms.picking.dataaccess.mapper;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.ccbsa.common.domain.valueobject.LoadNumber;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.picking.dataaccess.entity.LoadEntity;
import com.ccbsa.wms.picking.dataaccess.entity.OrderEntity;
import com.ccbsa.wms.picking.domain.core.entity.Load;
import com.ccbsa.wms.picking.domain.core.entity.Order;
import com.ccbsa.wms.picking.domain.core.valueobject.LoadId;

import lombok.RequiredArgsConstructor;

/**
 * Mapper: LoadEntityMapper
 * <p>
 * Maps between Load domain aggregate and LoadEntity JPA entity.
 */
@Component
@RequiredArgsConstructor
public class LoadEntityMapper {
    private final OrderEntityMapper orderEntityMapper;

    public LoadEntity toEntity(Load load) {
        if (load == null) {
            throw new IllegalArgumentException("Load cannot be null");
        }
        if (load.getId() == null) {
            throw new IllegalStateException("Load ID cannot be null when mapping to entity");
        }

        LoadEntity entity = new LoadEntity();
        UUID loadId = load.getId().getValue();
        if (loadId == null) {
            throw new IllegalStateException("Load ID value cannot be null when mapping to entity");
        }
        entity.setId(loadId);
        entity.setTenantId(load.getTenantId().getValue());
        entity.setLoadNumber(load.getLoadNumber().getValue());
        entity.setStatus(load.getStatus());
        entity.setCreatedAt(load.getCreatedAt());
        entity.setPlannedAt(load.getPlannedAt());

        // Map orders
        List<OrderEntity> orderEntities = new ArrayList<>();
        for (Order order : load.getOrders()) {
            OrderEntity orderEntity = orderEntityMapper.toEntity(order);
            orderEntity.setLoad(entity);
            orderEntities.add(orderEntity);
        }
        entity.setOrders(orderEntities);

        // Set version for optimistic locking (never set to 0 for new entities)
        int domainVersion = load.getVersion();
        if (domainVersion > 0) {
            entity.setVersion(Long.valueOf(domainVersion));
        }

        return entity;
    }

    public Load toDomain(LoadEntity entity) {
        if (entity == null) {
            throw new IllegalArgumentException("LoadEntity cannot be null");
        }

        // Convert orders
        List<Order> orders = new ArrayList<>();
        if (entity.getOrders() != null) {
            for (OrderEntity orderEntity : entity.getOrders()) {
                orders.add(orderEntityMapper.toDomain(orderEntity));
            }
        }

        // Build domain entity using builder
        return Load.builder().id(LoadId.of(entity.getId())).tenantId(TenantId.of(entity.getTenantId())).loadNumber(LoadNumber.of(entity.getLoadNumber())).orders(orders)
                .status(entity.getStatus()).createdAt(entity.getCreatedAt()).plannedAt(entity.getPlannedAt())
                .version(entity.getVersion() != null ? entity.getVersion().intValue() : 0).buildWithoutEvents();
    }
}
