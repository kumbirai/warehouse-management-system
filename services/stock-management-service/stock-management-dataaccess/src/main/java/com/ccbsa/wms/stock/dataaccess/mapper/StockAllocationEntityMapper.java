package com.ccbsa.wms.stock.dataaccess.mapper;

import org.springframework.stereotype.Component;

import com.ccbsa.common.domain.valueobject.ProductId;
import com.ccbsa.common.domain.valueobject.Quantity;
import com.ccbsa.common.domain.valueobject.StockItemId;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.common.domain.valueobject.UserId;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;
import com.ccbsa.wms.stock.dataaccess.entity.StockAllocationEntity;
import com.ccbsa.wms.stock.domain.core.entity.StockAllocation;
import com.ccbsa.wms.stock.domain.core.valueobject.Notes;
import com.ccbsa.wms.stock.domain.core.valueobject.ReferenceId;
import com.ccbsa.wms.stock.domain.core.valueobject.StockAllocationId;

/**
 * Mapper: StockAllocationEntityMapper
 * <p>
 * Maps between StockAllocation domain aggregate and StockAllocationEntity JPA entity.
 * Handles conversion between domain value objects and JPA entity fields.
 */
@Component
public class StockAllocationEntityMapper {

    /**
     * Converts StockAllocation domain entity to StockAllocationEntity JPA entity.
     * <p>
     * For new entities (version == 0), version is set to null to let Hibernate manage it.
     * For existing entities (version > 0), version is set to enable optimistic locking.
     *
     * @param stockAllocation StockAllocation domain entity
     * @return StockAllocationEntity JPA entity
     * @throws IllegalArgumentException if stockAllocation is null
     */
    public StockAllocationEntity toEntity(StockAllocation stockAllocation) {
        if (stockAllocation == null) {
            throw new IllegalArgumentException("StockAllocation cannot be null");
        }

        StockAllocationEntity entity = new StockAllocationEntity();
        entity.setId(stockAllocation.getId().getValue());
        entity.setTenantId(stockAllocation.getTenantId().getValue());
        entity.setProductId(stockAllocation.getProductId().getValue());
        entity.setLocationId(stockAllocation.getLocationId() != null ? stockAllocation.getLocationId().getValue() : null);
        entity.setStockItemId(stockAllocation.getStockItemId().getValue());
        entity.setQuantity(stockAllocation.getQuantity().getValue());
        entity.setAllocationType(stockAllocation.getAllocationType());
        entity.setReferenceId(stockAllocation.getReferenceId() != null ? stockAllocation.getReferenceId().getValue() : null);
        entity.setStatus(stockAllocation.getStatus());
        entity.setAllocatedBy(java.util.UUID.fromString(stockAllocation.getAllocatedBy().getValue()));
        entity.setAllocatedAt(stockAllocation.getAllocatedAt());
        entity.setCreatedAt(stockAllocation.getAllocatedAt()); // Use allocatedAt as createdAt
        entity.setLastModifiedAt(stockAllocation.getAllocatedAt());

        // Set release fields if allocation is released
        if (stockAllocation.getReleasedAt() != null) {
            entity.setReleasedAt(stockAllocation.getReleasedAt());
            entity.setLastModifiedAt(stockAllocation.getReleasedAt());
        }

        // Set notes if present
        if (stockAllocation.getNotes() != null && !stockAllocation.getNotes().isEmpty()) {
            entity.setNotes(stockAllocation.getNotes().getValue());
        }

        // For new entities, version will be set by Hibernate when persisting
        // For existing entities loaded from DB, version is already set
        // We only set version when mapping from domain if it's > 0 (existing entity)
        int domainVersion = stockAllocation.getVersion();
        if (domainVersion > 0) {
            entity.setVersion(Long.valueOf(domainVersion));
        }
        // For new entities (version == 0), don't set version - let Hibernate manage it

        return entity;
    }

    /**
     * Converts StockAllocationEntity JPA entity to StockAllocation domain entity.
     *
     * @param entity StockAllocationEntity JPA entity
     * @return StockAllocation domain entity
     * @throws IllegalArgumentException if entity is null
     */
    public StockAllocation toDomain(StockAllocationEntity entity) {
        if (entity == null) {
            throw new IllegalArgumentException("StockAllocationEntity cannot be null");
        }

        StockAllocation.Builder builder = StockAllocation.builder().stockAllocationId(StockAllocationId.of(entity.getId())).tenantId(TenantId.of(entity.getTenantId()))
                .productId(ProductId.of(entity.getProductId())).stockItemId(StockItemId.of(entity.getStockItemId())).quantity(Quantity.of(entity.getQuantity()))
                .allocationType(entity.getAllocationType()).allocatedBy(UserId.of(entity.getAllocatedBy().toString())).allocatedAt(entity.getAllocatedAt())
                .version(entity.getVersion() != null ? entity.getVersion().intValue() : 0);

        // Set optional location ID
        if (entity.getLocationId() != null) {
            builder.locationId(LocationId.of(entity.getLocationId()));
        }

        // Set status
        builder.status(entity.getStatus());

        // Set reference ID if present
        if (entity.getReferenceId() != null) {
            builder.referenceId(ReferenceId.of(entity.getReferenceId()));
        }

        // Set release fields if present
        if (entity.getReleasedAt() != null) {
            builder.releasedAt(entity.getReleasedAt());
        }

        // Set notes if present
        if (entity.getNotes() != null) {
            builder.notes(Notes.ofNullable(entity.getNotes()));
        }

        return builder.buildWithoutEvents();
    }
}

