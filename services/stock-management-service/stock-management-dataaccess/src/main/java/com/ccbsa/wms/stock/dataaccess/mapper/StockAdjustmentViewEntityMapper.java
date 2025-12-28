package com.ccbsa.wms.stock.dataaccess.mapper;

import org.springframework.stereotype.Component;

import com.ccbsa.common.domain.valueobject.ProductId;
import com.ccbsa.common.domain.valueobject.Quantity;
import com.ccbsa.common.domain.valueobject.StockItemId;
import com.ccbsa.common.domain.valueobject.UserId;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;
import com.ccbsa.wms.stock.application.service.port.data.dto.StockAdjustmentView;
import com.ccbsa.wms.stock.dataaccess.entity.StockAdjustmentViewEntity;
import com.ccbsa.wms.stock.domain.core.valueobject.StockAdjustmentId;

/**
 * Entity Mapper: StockAdjustmentViewEntityMapper
 * <p>
 * Maps between StockAdjustmentViewEntity (JPA) and StockAdjustmentView (read model DTO).
 */
@Component
public class StockAdjustmentViewEntityMapper {

    /**
     * Converts StockAdjustmentViewEntity JPA entity to StockAdjustmentView read model DTO.
     *
     * @param entity StockAdjustmentViewEntity JPA entity
     * @return StockAdjustmentView read model DTO
     * @throws IllegalArgumentException if entity is null
     */
    public StockAdjustmentView toView(StockAdjustmentViewEntity entity) {
        if (entity == null) {
            throw new IllegalArgumentException("StockAdjustmentViewEntity cannot be null");
        }

        // Build StockAdjustmentView
        return StockAdjustmentView.builder().adjustmentId(StockAdjustmentId.of(entity.getId())).tenantId(entity.getTenantId()).productId(ProductId.of(entity.getProductId()))
                .locationId(entity.getLocationId() != null ? LocationId.of(entity.getLocationId()) : null)
                .stockItemId(entity.getStockItemId() != null ? StockItemId.of(entity.getStockItemId()) : null).adjustmentType(entity.getAdjustmentType())
                .quantity(Quantity.of(entity.getQuantity())).quantityBefore(entity.getQuantityBefore()).quantityAfter(entity.getQuantityAfter()).reason(entity.getReason())
                .notes(entity.getNotes()).adjustedBy(UserId.of(entity.getAdjustedBy().toString())).authorizationCode(entity.getAuthorizationCode())
                .adjustedAt(entity.getAdjustedAt()).build();
    }
}

