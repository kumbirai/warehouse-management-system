package com.ccbsa.wms.stock.dataaccess.mapper;

import org.springframework.stereotype.Component;

import com.ccbsa.common.domain.valueobject.ProductId;
import com.ccbsa.common.domain.valueobject.Quantity;
import com.ccbsa.common.domain.valueobject.StockItemId;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.common.domain.valueobject.UserId;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;
import com.ccbsa.wms.stock.dataaccess.entity.StockAdjustmentEntity;
import com.ccbsa.wms.stock.domain.core.entity.StockAdjustment;
import com.ccbsa.wms.stock.domain.core.valueobject.AuthorizationCode;
import com.ccbsa.wms.stock.domain.core.valueobject.Notes;
import com.ccbsa.wms.stock.domain.core.valueobject.StockAdjustmentId;

/**
 * Mapper: StockAdjustmentEntityMapper
 * <p>
 * Maps between StockAdjustment domain aggregate and StockAdjustmentEntity JPA entity.
 * Handles conversion between domain value objects and JPA entity fields.
 */
@Component
public class StockAdjustmentEntityMapper {

    /**
     * Converts StockAdjustment domain entity to StockAdjustmentEntity JPA entity.
     * <p>
     * For new entities (version == 0), version is set to null to let Hibernate manage it.
     * For existing entities (version > 0), version is set to enable optimistic locking.
     *
     * @param stockAdjustment StockAdjustment domain entity
     * @return StockAdjustmentEntity JPA entity
     * @throws IllegalArgumentException if stockAdjustment is null
     */
    public StockAdjustmentEntity toEntity(StockAdjustment stockAdjustment) {
        if (stockAdjustment == null) {
            throw new IllegalArgumentException("StockAdjustment cannot be null");
        }

        StockAdjustmentEntity entity = new StockAdjustmentEntity();
        entity.setId(stockAdjustment.getId().getValue());
        entity.setTenantId(stockAdjustment.getTenantId().getValue());
        entity.setProductId(stockAdjustment.getProductId().getValue());
        entity.setLocationId(stockAdjustment.getLocationId() != null ? stockAdjustment.getLocationId().getValue() : null);
        entity.setStockItemId(stockAdjustment.getStockItemId() != null ? stockAdjustment.getStockItemId().getValue() : null);
        entity.setAdjustmentType(stockAdjustment.getAdjustmentType());
        entity.setQuantity(stockAdjustment.getQuantity().getValue());
        entity.setReason(stockAdjustment.getReason());
        entity.setAdjustedBy(java.util.UUID.fromString(stockAdjustment.getAdjustedBy().getValue()));
        entity.setAdjustedAt(stockAdjustment.getAdjustedAt());
        entity.setQuantityBefore(stockAdjustment.getQuantityBefore() != null ? stockAdjustment.getQuantityBefore().getValue() : null);
        entity.setQuantityAfter(stockAdjustment.getQuantityAfter() != null ? stockAdjustment.getQuantityAfter().getValue() : null);
        entity.setCreatedAt(stockAdjustment.getAdjustedAt()); // Use adjustedAt as createdAt
        entity.setLastModifiedAt(stockAdjustment.getAdjustedAt());

        // Set optional fields
        if (stockAdjustment.getNotes() != null && !stockAdjustment.getNotes().isEmpty()) {
            entity.setNotes(stockAdjustment.getNotes().getValue());
        }
        if (stockAdjustment.getAuthorizationCode() != null && !stockAdjustment.getAuthorizationCode().isEmpty()) {
            entity.setAuthorizationCode(stockAdjustment.getAuthorizationCode().getValue());
        }

        // For new entities, version will be set by Hibernate when persisting
        // For existing entities loaded from DB, version is already set
        // We only set version when mapping from domain if it's > 0 (existing entity)
        int domainVersion = stockAdjustment.getVersion();
        if (domainVersion > 0) {
            entity.setVersion(Long.valueOf(domainVersion));
        }
        // For new entities (version == 0), don't set version - let Hibernate manage it

        return entity;
    }

    /**
     * Converts StockAdjustmentEntity JPA entity to StockAdjustment domain entity.
     *
     * @param entity StockAdjustmentEntity JPA entity
     * @return StockAdjustment domain entity
     * @throws IllegalArgumentException if entity is null
     */
    public StockAdjustment toDomain(StockAdjustmentEntity entity) {
        if (entity == null) {
            throw new IllegalArgumentException("StockAdjustmentEntity cannot be null");
        }

        StockAdjustment.Builder builder = StockAdjustment.builder().stockAdjustmentId(StockAdjustmentId.of(entity.getId())).tenantId(TenantId.of(entity.getTenantId()))
                .productId(ProductId.of(entity.getProductId())).adjustmentType(entity.getAdjustmentType()).quantity(Quantity.of(entity.getQuantity())).reason(entity.getReason())
                .adjustedBy(UserId.of(entity.getAdjustedBy().toString())).adjustedAt(entity.getAdjustedAt())
                .quantityBefore(entity.getQuantityBefore() != null ? Quantity.of(entity.getQuantityBefore()) : null)
                .quantityAfter(entity.getQuantityAfter() != null ? Quantity.of(entity.getQuantityAfter()) : null)
                .version(entity.getVersion() != null ? entity.getVersion().intValue() : 0);

        // Set optional location ID
        if (entity.getLocationId() != null) {
            builder.locationId(LocationId.of(entity.getLocationId()));
        }

        // Set optional stock item ID
        if (entity.getStockItemId() != null) {
            builder.stockItemId(StockItemId.of(entity.getStockItemId()));
        }

        // Set optional fields
        if (entity.getNotes() != null) {
            builder.notes(Notes.ofNullable(entity.getNotes()));
        }
        if (entity.getAuthorizationCode() != null) {
            builder.authorizationCode(AuthorizationCode.ofNullable(entity.getAuthorizationCode()));
        }

        return builder.buildWithoutEvents();
    }
}

