package com.ccbsa.wms.location.dataaccess.mapper;

import org.springframework.stereotype.Component;

import com.ccbsa.common.domain.valueobject.ProductId;
import com.ccbsa.common.domain.valueobject.Quantity;
import com.ccbsa.common.domain.valueobject.UserId;
import com.ccbsa.wms.location.application.service.port.data.dto.StockMovementView;
import com.ccbsa.wms.location.dataaccess.entity.StockMovementViewEntity;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;
import com.ccbsa.wms.location.domain.core.valueobject.StockMovementId;

/**
 * Entity Mapper: StockMovementViewEntityMapper
 * <p>
 * Maps between StockMovementViewEntity (JPA) and StockMovementView (read model DTO).
 */
@Component
public class StockMovementViewEntityMapper {

    /**
     * Converts StockMovementViewEntity JPA entity to StockMovementView read model DTO.
     *
     * @param entity StockMovementViewEntity JPA entity
     * @return StockMovementView read model DTO
     * @throws IllegalArgumentException if entity is null
     */
    public StockMovementView toView(StockMovementViewEntity entity) {
        if (entity == null) {
            throw new IllegalArgumentException("StockMovementViewEntity cannot be null");
        }

        // Build StockMovementView
        return StockMovementView.builder().stockMovementId(StockMovementId.of(entity.getId())).tenantId(entity.getTenantId()).stockItemId(entity.getStockItemId())
                .productId(ProductId.of(entity.getProductId())).sourceLocationId(LocationId.of(entity.getSourceLocationId()))
                .destinationLocationId(LocationId.of(entity.getDestinationLocationId())).quantity(Quantity.of(entity.getQuantity())).movementType(entity.getMovementType())
                .reason(entity.getReason()).status(entity.getStatus()).initiatedBy(UserId.of(entity.getInitiatedBy().toString())).initiatedAt(entity.getInitiatedAt())
                .completedBy(entity.getCompletedBy() != null ? UserId.of(entity.getCompletedBy().toString()) : null).completedAt(entity.getCompletedAt())
                .cancelledBy(entity.getCancelledBy() != null ? UserId.of(entity.getCancelledBy().toString()) : null).cancelledAt(entity.getCancelledAt())
                .cancellationReason(entity.getCancellationReason()).build();
    }
}

