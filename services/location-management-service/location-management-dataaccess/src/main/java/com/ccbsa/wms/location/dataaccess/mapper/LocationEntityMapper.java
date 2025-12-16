package com.ccbsa.wms.location.dataaccess.mapper;

import org.springframework.stereotype.Component;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.location.dataaccess.entity.LocationEntity;
import com.ccbsa.wms.location.domain.core.entity.Location;
import com.ccbsa.wms.location.domain.core.valueobject.LocationBarcode;
import com.ccbsa.wms.location.domain.core.valueobject.LocationCapacity;
import com.ccbsa.wms.location.domain.core.valueobject.LocationCoordinates;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;

/**
 * Mapper: LocationEntityMapper
 * <p>
 * Maps between Location domain aggregate and LocationEntity JPA entity.
 * Handles conversion between domain value objects and JPA entity fields.
 */
@Component
public class LocationEntityMapper {

    /**
     * Converts Location domain entity to LocationEntity JPA entity.
     * <p>
     * For new entities (version == 0), version is set to null to let Hibernate manage it.
     * For existing entities (version > 0), version is set to enable optimistic locking.
     *
     * @param location Location domain entity
     * @return LocationEntity JPA entity
     * @throws IllegalArgumentException if location is null
     */
    public LocationEntity toEntity(Location location) {
        if (location == null) {
            throw new IllegalArgumentException("Location cannot be null");
        }

        LocationEntity entity = new LocationEntity();
        entity.setId(location.getId().getValue());
        entity.setTenantId(location.getTenantId().getValue());
        entity.setBarcode(location.getBarcode().getValue());
        entity.setZone(location.getCoordinates().getZone());
        entity.setAisle(location.getCoordinates().getAisle());
        entity.setRack(location.getCoordinates().getRack());
        entity.setLevel(location.getCoordinates().getLevel());
        entity.setStatus(location.getStatus());

        // Map capacity
        LocationCapacity capacity = location.getCapacity();
        if (capacity != null) {
            entity.setCurrentQuantity(capacity.getCurrentQuantity());
            entity.setMaximumQuantity(capacity.getMaximumQuantity());
        }

        entity.setDescription(location.getDescription());
        entity.setCreatedAt(location.getCreatedAt());
        entity.setLastModifiedAt(location.getLastModifiedAt());

        // For new entities, version will be set by Hibernate when persisting
        // For existing entities loaded from DB, version is already set
        // We only set version when mapping from domain if it's > 0 (existing entity)
        int domainVersion = location.getVersion();
        if (domainVersion > 0) {
            entity.setVersion(Long.valueOf(domainVersion));
        }
        // For new entities (version == 0), don't set version - let Hibernate manage it

        return entity;
    }

    /**
     * Converts LocationEntity JPA entity to Location domain entity.
     *
     * @param entity LocationEntity JPA entity
     * @return Location domain entity
     * @throws IllegalArgumentException if entity is null
     */
    public Location toDomain(LocationEntity entity) {
        if (entity == null) {
            throw new IllegalArgumentException("LocationEntity cannot be null");
        }

        Location.Builder builder = Location.builder()
                .locationId(LocationId.of(entity.getId()))
                .tenantId(TenantId.of(entity.getTenantId()))
                .barcode(LocationBarcode.of(entity.getBarcode()))
                .coordinates(LocationCoordinates.of(
                        entity.getZone(),
                        entity.getAisle(),
                        entity.getRack(),
                        entity.getLevel()
                ))
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .lastModifiedAt(entity.getLastModifiedAt())
                .version(entity.getVersion());

        // Map capacity
        if (entity.getCurrentQuantity() != null || entity.getMaximumQuantity() != null) {
            LocationCapacity capacity = LocationCapacity.of(
                    entity.getCurrentQuantity() != null ? entity.getCurrentQuantity() : java.math.BigDecimal.ZERO,
                    entity.getMaximumQuantity()
            );
            builder.capacity(capacity);
        }

        // Set description if available
        if (entity.getDescription() != null && !entity.getDescription().trim().isEmpty()) {
            builder.description(entity.getDescription());
        }

        return builder.buildWithoutEvents();
    }
}

