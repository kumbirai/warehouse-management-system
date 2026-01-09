package com.ccbsa.wms.stock.dataaccess.mapper;

import org.springframework.stereotype.Component;

import com.ccbsa.common.domain.valueobject.ExpirationDate;
import com.ccbsa.common.domain.valueobject.ProductId;
import com.ccbsa.common.domain.valueobject.Quantity;
import com.ccbsa.common.domain.valueobject.StockItemId;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;
import com.ccbsa.wms.stock.application.service.port.data.dto.StockItemView;
import com.ccbsa.wms.stock.dataaccess.entity.StockItemViewEntity;
import com.ccbsa.wms.stock.domain.core.valueobject.ConsignmentId;

/**
 * Entity Mapper: StockItemViewEntityMapper
 * <p>
 * Maps between StockItemViewEntity (JPA) and StockItemView (read model DTO).
 */
@Component
public class StockItemViewEntityMapper {

    /**
     * Converts StockItemViewEntity JPA entity to StockItemView read model DTO.
     *
     * @param entity StockItemViewEntity JPA entity
     * @return StockItemView read model DTO
     * @throws IllegalArgumentException if entity is null
     */
    public StockItemView toView(StockItemViewEntity entity) {
        if (entity == null) {
            throw new IllegalArgumentException("StockItemViewEntity cannot be null");
        }

        // Build StockItemView
        return StockItemView.builder().stockItemId(StockItemId.of(entity.getId())).tenantId(entity.getTenantId()).productId(ProductId.of(entity.getProductId()))
                .locationId(entity.getLocationId() != null ? LocationId.of(entity.getLocationId()) : null).quantity(Quantity.of(entity.getQuantity()))
                .allocatedQuantity(Quantity.of(entity.getAllocatedQuantity() != null ? entity.getAllocatedQuantity() : 0))
                .expirationDate(entity.getExpirationDate() != null ? ExpirationDate.of(entity.getExpirationDate()) : null).classification(entity.getClassification())
                .consignmentId(entity.getConsignmentId() != null ? ConsignmentId.of(entity.getConsignmentId()) : null).createdAt(entity.getCreatedAt())
                .lastModifiedAt(entity.getLastModifiedAt()).build();
    }
}

