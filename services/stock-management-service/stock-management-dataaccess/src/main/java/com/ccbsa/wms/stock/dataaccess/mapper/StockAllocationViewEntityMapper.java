package com.ccbsa.wms.stock.dataaccess.mapper;

import org.springframework.stereotype.Component;

import com.ccbsa.common.domain.valueobject.ProductId;
import com.ccbsa.common.domain.valueobject.Quantity;
import com.ccbsa.common.domain.valueobject.StockItemId;
import com.ccbsa.common.domain.valueobject.UserId;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;
import com.ccbsa.wms.stock.application.service.port.data.dto.StockAllocationView;
import com.ccbsa.wms.stock.dataaccess.entity.StockAllocationViewEntity;
import com.ccbsa.wms.stock.domain.core.valueobject.StockAllocationId;

/**
 * Entity Mapper: StockAllocationViewEntityMapper
 * <p>
 * Maps between StockAllocationViewEntity (JPA) and StockAllocationView (read model DTO).
 */
@Component
public class StockAllocationViewEntityMapper {

    /**
     * Converts StockAllocationViewEntity JPA entity to StockAllocationView read model DTO.
     *
     * @param entity StockAllocationViewEntity JPA entity
     * @return StockAllocationView read model DTO
     * @throws IllegalArgumentException if entity is null
     */
    public StockAllocationView toView(StockAllocationViewEntity entity) {
        if (entity == null) {
            throw new IllegalArgumentException("StockAllocationViewEntity cannot be null");
        }

        // Build StockAllocationView
        return StockAllocationView.builder().allocationId(StockAllocationId.of(entity.getId())).tenantId(entity.getTenantId()).productId(ProductId.of(entity.getProductId()))
                .locationId(entity.getLocationId() != null ? LocationId.of(entity.getLocationId()) : null).stockItemId(StockItemId.of(entity.getStockItemId()))
                .quantity(Quantity.of(entity.getQuantity())).allocationType(entity.getAllocationType()).referenceId(entity.getReferenceId()).status(entity.getStatus())
                .allocatedAt(entity.getAllocatedAt()).releasedAt(entity.getReleasedAt()).allocatedBy(UserId.of(entity.getAllocatedBy().toString())).notes(entity.getNotes())
                .build();
    }
}

