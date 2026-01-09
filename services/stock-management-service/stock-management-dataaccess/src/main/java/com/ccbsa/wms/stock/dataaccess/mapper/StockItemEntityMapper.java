package com.ccbsa.wms.stock.dataaccess.mapper;

import org.springframework.stereotype.Component;

import com.ccbsa.common.domain.valueobject.ExpirationDate;
import com.ccbsa.common.domain.valueobject.ProductId;
import com.ccbsa.common.domain.valueobject.Quantity;
import com.ccbsa.common.domain.valueobject.StockItemId;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;
import com.ccbsa.wms.stock.dataaccess.entity.StockItemEntity;
import com.ccbsa.wms.stock.domain.core.entity.StockItem;
import com.ccbsa.wms.stock.domain.core.valueobject.ConsignmentId;

/**
 * Mapper: StockItemEntityMapper
 * <p>
 * Maps between StockItem domain aggregate and StockItemEntity JPA entity. Handles conversion between domain value objects and JPA entity fields.
 */
@Component
public class StockItemEntityMapper {

    /**
     * Converts StockItem domain entity to StockItemEntity JPA entity.
     *
     * @param stockItem StockItem domain entity
     * @return StockItemEntity JPA entity
     * @throws IllegalArgumentException if stockItem is null
     */
    public StockItemEntity toEntity(StockItem stockItem) {
        if (stockItem == null) {
            throw new IllegalArgumentException("StockItem cannot be null");
        }

        StockItemEntity entity = new StockItemEntity();
        entity.setId(stockItem.getId().getValue());
        entity.setTenantId(stockItem.getTenantId().getValue());
        entity.setProductId(stockItem.getProductId().getValue());
        entity.setLocationId(stockItem.getLocationId() != null ? stockItem.getLocationId().getValue() : null);
        entity.setQuantity(stockItem.getQuantity().getValue());
        entity.setAllocatedQuantity(stockItem.getAllocatedQuantity().getValue());
        entity.setExpirationDate(stockItem.getExpirationDate() != null ? stockItem.getExpirationDate().getValue() : null);
        entity.setClassification(stockItem.getClassification());
        entity.setConsignmentId(stockItem.getConsignmentId() != null ? stockItem.getConsignmentId().getValue() : null);
        entity.setCreatedAt(stockItem.getCreatedAt());
        entity.setLastModifiedAt(stockItem.getLastModifiedAt());

        // Set version for optimistic locking
        int domainVersion = stockItem.getVersion();
        if (domainVersion > 0) {
            entity.setVersion(Long.valueOf(domainVersion));
        }

        return entity;
    }

    /**
     * Converts StockItemEntity JPA entity to StockItem domain entity.
     *
     * @param entity StockItemEntity JPA entity
     * @return StockItem domain entity
     * @throws IllegalArgumentException if entity is null
     */
    public StockItem toDomain(StockItemEntity entity) {
        if (entity == null) {
            throw new IllegalArgumentException("StockItemEntity cannot be null");
        }

        // Build domain entity using builder
        StockItem.Builder builder =
                StockItem.builder().stockItemId(StockItemId.of(entity.getId())).tenantId(TenantId.of(entity.getTenantId())).productId(ProductId.of(entity.getProductId()))
                        .quantity(Quantity.of(entity.getQuantity())).allocatedQuantity(Quantity.of(entity.getAllocatedQuantity() != null ? entity.getAllocatedQuantity() : 0));

        if (entity.getLocationId() != null) {
            builder.locationId(LocationId.of(entity.getLocationId()));
        }

        if (entity.getExpirationDate() != null) {
            builder.expirationDate(ExpirationDate.of(entity.getExpirationDate()));
        }

        if (entity.getConsignmentId() != null) {
            builder.consignmentId(ConsignmentId.of(entity.getConsignmentId()));
        }

        builder.createdAt(entity.getCreatedAt()).lastModifiedAt(entity.getLastModifiedAt()).version(entity.getVersion() != null ? entity.getVersion().intValue() : 0);

        // Build without events (loading from database)
        return builder.buildWithoutEvents();
    }
}

