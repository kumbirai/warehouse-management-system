package com.ccbsa.wms.stock.dataaccess.mapper;

import org.springframework.stereotype.Component;

import com.ccbsa.common.domain.valueobject.MaximumQuantity;
import com.ccbsa.common.domain.valueobject.MinimumQuantity;
import com.ccbsa.common.domain.valueobject.ProductId;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;
import com.ccbsa.wms.stock.dataaccess.entity.StockLevelThresholdEntity;
import com.ccbsa.wms.stock.domain.core.entity.StockLevelThreshold;
import com.ccbsa.wms.stock.domain.core.valueobject.StockLevelThresholdId;

/**
 * Mapper: StockLevelThresholdEntityMapper
 * <p>
 * Maps between StockLevelThreshold domain aggregate and StockLevelThresholdEntity JPA entity.
 * Handles conversion between domain value objects and JPA entity fields.
 */
@Component
public class StockLevelThresholdEntityMapper {

    /**
     * Converts StockLevelThreshold domain entity to StockLevelThresholdEntity JPA entity.
     * <p>
     * For new entities (version == 0), version is set to null to let Hibernate manage it.
     * For existing entities (version > 0), version is set to enable optimistic locking.
     *
     * @param stockLevelThreshold StockLevelThreshold domain entity
     * @return StockLevelThresholdEntity JPA entity
     * @throws IllegalArgumentException if stockLevelThreshold is null
     */
    public StockLevelThresholdEntity toEntity(StockLevelThreshold stockLevelThreshold) {
        if (stockLevelThreshold == null) {
            throw new IllegalArgumentException("StockLevelThreshold cannot be null");
        }

        StockLevelThresholdEntity entity = new StockLevelThresholdEntity();
        entity.setId(stockLevelThreshold.getId().getValue());
        entity.setTenantId(stockLevelThreshold.getTenantId().getValue());
        entity.setProductId(stockLevelThreshold.getProductId().getValue());
        entity.setLocationId(stockLevelThreshold.getLocationId() != null ? stockLevelThreshold.getLocationId().getValue() : null);
        entity.setMinimumQuantity(stockLevelThreshold.getMinimumQuantity() != null ? stockLevelThreshold.getMinimumQuantity().getValue() : null);
        entity.setMaximumQuantity(stockLevelThreshold.getMaximumQuantity() != null ? stockLevelThreshold.getMaximumQuantity().getValue() : null);
        entity.setEnableAutoRestock(stockLevelThreshold.isEnableAutoRestock());
        entity.setCreatedAt(stockLevelThreshold.getCreatedAt());
        entity.setLastModifiedAt(stockLevelThreshold.getLastModifiedAt());

        // For new entities, version will be set by Hibernate when persisting
        // For existing entities loaded from DB, version is already set
        // We only set version when mapping from domain if it's > 0 (existing entity)
        int domainVersion = stockLevelThreshold.getVersion();
        if (domainVersion > 0) {
            entity.setVersion(Long.valueOf(domainVersion));
        }
        // For new entities (version == 0), don't set version - let Hibernate manage it

        return entity;
    }

    /**
     * Converts StockLevelThresholdEntity JPA entity to StockLevelThreshold domain entity.
     *
     * @param entity StockLevelThresholdEntity JPA entity
     * @return StockLevelThreshold domain entity
     * @throws IllegalArgumentException if entity is null
     */
    public StockLevelThreshold toDomain(StockLevelThresholdEntity entity) {
        if (entity == null) {
            throw new IllegalArgumentException("StockLevelThresholdEntity cannot be null");
        }

        StockLevelThreshold.Builder builder =
                StockLevelThreshold.builder().stockLevelThresholdId(StockLevelThresholdId.of(entity.getId())).tenantId(TenantId.of(entity.getTenantId()))
                        .productId(ProductId.of(entity.getProductId())).enableAutoRestock(entity.getEnableAutoRestock()).createdAt(entity.getCreatedAt())
                        .lastModifiedAt(entity.getLastModifiedAt()).version(entity.getVersion() != null ? entity.getVersion().intValue() : 0);

        // Set optional location ID
        if (entity.getLocationId() != null) {
            builder.locationId(LocationId.of(entity.getLocationId()));
        }

        // Set optional minimum quantity
        if (entity.getMinimumQuantity() != null) {
            builder.minimumQuantity(MinimumQuantity.of(entity.getMinimumQuantity()));
        }

        // Set optional maximum quantity
        if (entity.getMaximumQuantity() != null) {
            builder.maximumQuantity(MaximumQuantity.of(entity.getMaximumQuantity()));
        }

        return builder.buildWithoutEvents();
    }
}

