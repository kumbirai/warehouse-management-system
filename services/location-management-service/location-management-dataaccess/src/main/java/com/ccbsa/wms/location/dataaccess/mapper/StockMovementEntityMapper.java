package com.ccbsa.wms.location.dataaccess.mapper;

import org.springframework.stereotype.Component;

import com.ccbsa.common.domain.valueobject.ProductId;
import com.ccbsa.common.domain.valueobject.Quantity;
import com.ccbsa.common.domain.valueobject.StockItemId;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.common.domain.valueobject.UserId;
import com.ccbsa.wms.location.dataaccess.entity.StockMovementEntity;
import com.ccbsa.wms.location.domain.core.entity.StockMovement;
import com.ccbsa.wms.location.domain.core.valueobject.CancellationReason;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;
import com.ccbsa.wms.location.domain.core.valueobject.StockMovementId;

/**
 * Mapper: StockMovementEntityMapper
 * <p>
 * Maps between StockMovement domain aggregate and StockMovementEntity JPA entity.
 * Handles conversion between domain value objects and JPA entity fields.
 */
@Component
public class StockMovementEntityMapper {

    /**
     * Converts StockMovement domain entity to StockMovementEntity JPA entity.
     * <p>
     * For new entities (version == 0), version is set to null to let Hibernate manage it.
     * For existing entities (version > 0), version is set to enable optimistic locking.
     *
     * @param stockMovement StockMovement domain entity
     * @return StockMovementEntity JPA entity
     * @throws IllegalArgumentException if stockMovement is null
     */
    public StockMovementEntity toEntity(StockMovement stockMovement) {
        if (stockMovement == null) {
            throw new IllegalArgumentException("StockMovement cannot be null");
        }

        StockMovementEntity entity = new StockMovementEntity();
        entity.setId(stockMovement.getId().getValue());
        entity.setTenantId(stockMovement.getTenantId().getValue());
        entity.setStockItemId(stockMovement.getStockItemId().getValueAsString());
        entity.setProductId(stockMovement.getProductId().getValue());
        entity.setSourceLocationId(stockMovement.getSourceLocationId().getValue());
        entity.setDestinationLocationId(stockMovement.getDestinationLocationId().getValue());
        entity.setQuantity(stockMovement.getQuantity().getValue());
        entity.setMovementType(stockMovement.getMovementType());
        entity.setReason(stockMovement.getReason());
        entity.setStatus(stockMovement.getStatus());
        entity.setInitiatedBy(java.util.UUID.fromString(stockMovement.getInitiatedBy().getValue()));
        entity.setInitiatedAt(stockMovement.getInitiatedAt());
        entity.setCreatedAt(stockMovement.getInitiatedAt()); // Use initiatedAt as createdAt
        entity.setLastModifiedAt(stockMovement.getInitiatedAt());

        // Set completion fields if movement is completed
        if (stockMovement.getCompletedBy() != null) {
            entity.setCompletedBy(java.util.UUID.fromString(stockMovement.getCompletedBy().getValue()));
        }
        if (stockMovement.getCompletedAt() != null) {
            entity.setCompletedAt(stockMovement.getCompletedAt());
            entity.setLastModifiedAt(stockMovement.getCompletedAt());
        }

        // Set cancellation fields if movement is cancelled
        if (stockMovement.getCancelledBy() != null) {
            entity.setCancelledBy(java.util.UUID.fromString(stockMovement.getCancelledBy().getValue()));
        }
        if (stockMovement.getCancelledAt() != null) {
            entity.setCancelledAt(stockMovement.getCancelledAt());
            entity.setLastModifiedAt(stockMovement.getCancelledAt());
        }
        if (stockMovement.getCancellationReason() != null) {
            entity.setCancellationReason(stockMovement.getCancellationReason().getValue());
        }

        // For new entities, version will be set by Hibernate when persisting
        // For existing entities loaded from DB, version is already set
        // We only set version when mapping from domain if it's > 0 (existing entity)
        int domainVersion = stockMovement.getVersion();
        if (domainVersion > 0) {
            entity.setVersion(Long.valueOf(domainVersion));
        }
        // For new entities (version == 0), don't set version - let Hibernate manage it

        return entity;
    }

    /**
     * Converts StockMovementEntity JPA entity to StockMovement domain entity.
     *
     * @param entity StockMovementEntity JPA entity
     * @return StockMovement domain entity
     * @throws IllegalArgumentException if entity is null
     */
    public StockMovement toDomain(StockMovementEntity entity) {
        if (entity == null) {
            throw new IllegalArgumentException("StockMovementEntity cannot be null");
        }

        StockMovement.Builder builder = StockMovement.builder().stockMovementId(StockMovementId.of(entity.getId())).tenantId(TenantId.of(entity.getTenantId()))
                .stockItemId(StockItemId.of(entity.getStockItemId())).productId(ProductId.of(entity.getProductId())).sourceLocationId(LocationId.of(entity.getSourceLocationId()))
                .destinationLocationId(LocationId.of(entity.getDestinationLocationId())).quantity(Quantity.of(entity.getQuantity())).movementType(entity.getMovementType())
                .reason(entity.getReason()).initiatedBy(UserId.of(entity.getInitiatedBy())).initiatedAt(entity.getInitiatedAt())
                .version(entity.getVersion() != null ? entity.getVersion().intValue() : 0);

        // Set status
        builder.status(entity.getStatus());

        // Set completion fields if present
        if (entity.getCompletedBy() != null) {
            builder.completedBy(UserId.of(entity.getCompletedBy()));
        }
        if (entity.getCompletedAt() != null) {
            builder.completedAt(entity.getCompletedAt());
        }

        // Set cancellation fields if present
        if (entity.getCancelledBy() != null) {
            builder.cancelledBy(UserId.of(entity.getCancelledBy()));
        }
        if (entity.getCancelledAt() != null) {
            builder.cancelledAt(entity.getCancelledAt());
        }
        if (entity.getCancellationReason() != null) {
            builder.cancellationReason(CancellationReason.of(entity.getCancellationReason()));
        }

        return builder.buildWithoutEvents();
    }
}

