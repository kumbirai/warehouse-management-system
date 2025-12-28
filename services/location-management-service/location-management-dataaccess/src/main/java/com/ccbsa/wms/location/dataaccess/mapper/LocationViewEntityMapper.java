package com.ccbsa.wms.location.dataaccess.mapper;

import org.springframework.stereotype.Component;

import com.ccbsa.wms.location.application.service.port.data.dto.LocationView;
import com.ccbsa.wms.location.dataaccess.entity.LocationViewEntity;
import com.ccbsa.wms.location.domain.core.valueobject.LocationBarcode;
import com.ccbsa.wms.location.domain.core.valueobject.LocationCapacity;
import com.ccbsa.wms.location.domain.core.valueobject.LocationCoordinates;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;

/**
 * Entity Mapper: LocationViewEntityMapper
 * <p>
 * Maps between LocationViewEntity (JPA) and LocationView (read model DTO).
 */
@Component
public class LocationViewEntityMapper {

    /**
     * Converts LocationViewEntity JPA entity to LocationView read model DTO.
     *
     * @param entity LocationViewEntity JPA entity
     * @return LocationView read model DTO
     * @throws IllegalArgumentException if entity is null
     */
    public LocationView toView(LocationViewEntity entity) {
        if (entity == null) {
            throw new IllegalArgumentException("LocationViewEntity cannot be null");
        }

        // Build LocationCoordinates from entity fields
        LocationCoordinates coordinates = LocationCoordinates.of(entity.getZone(), entity.getAisle(), entity.getRack(), entity.getLevel());

        // Build LocationCapacity from entity fields
        LocationCapacity capacity =
                entity.getMaximumQuantity() != null ? LocationCapacity.of(entity.getCurrentQuantity() != null ? entity.getCurrentQuantity() : java.math.BigDecimal.ZERO,
                        entity.getMaximumQuantity())
                        : LocationCapacity.withCurrentQuantity(entity.getCurrentQuantity() != null ? entity.getCurrentQuantity() : java.math.BigDecimal.ZERO);

        // Build LocationView
        return LocationView.builder().locationId(LocationId.of(entity.getId())).tenantId(entity.getTenantId()).barcode(LocationBarcode.of(entity.getBarcode()))
                .coordinates(coordinates).status(entity.getStatus()).capacity(capacity).code(entity.getCode()).name(entity.getName()).type(entity.getType())
                .description(entity.getDescription()).parentLocationId(entity.getParentLocationId() != null ? LocationId.of(entity.getParentLocationId()) : null)
                .createdAt(entity.getCreatedAt()).lastModifiedAt(entity.getLastModifiedAt()).build();
    }
}

