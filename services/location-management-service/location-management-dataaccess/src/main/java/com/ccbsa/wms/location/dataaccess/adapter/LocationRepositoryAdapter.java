package com.ccbsa.wms.location.dataaccess.adapter;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.location.application.service.port.repository.LocationRepository;
import com.ccbsa.wms.location.dataaccess.entity.LocationEntity;
import com.ccbsa.wms.location.dataaccess.jpa.LocationJpaRepository;
import com.ccbsa.wms.location.dataaccess.mapper.LocationEntityMapper;
import com.ccbsa.wms.location.domain.core.entity.Location;
import com.ccbsa.wms.location.domain.core.valueobject.LocationBarcode;
import com.ccbsa.wms.location.domain.core.valueobject.LocationCapacity;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;

/**
 * Repository Adapter: LocationRepositoryAdapter
 * <p>
 * Implements LocationRepository port interface. Adapts between domain Location aggregate and JPA LocationEntity.
 */
@Repository
public class LocationRepositoryAdapter
        implements LocationRepository {
    private final LocationJpaRepository jpaRepository;
    private final LocationEntityMapper mapper;

    public LocationRepositoryAdapter(LocationJpaRepository jpaRepository, LocationEntityMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Location save(Location location) {
        // Check if entity already exists to handle version correctly
        Optional<LocationEntity> existingEntity = jpaRepository.findByTenantIdAndId(location.getTenantId()
                .getValue(), location.getId()
                .getValue());

        LocationEntity entity;
        if (existingEntity.isPresent()) {
            // Update existing entity - preserve JPA managed state and version
            entity = existingEntity.get();
            updateEntityFromDomain(entity, location);
        } else {
            // New entity - create from domain model
            entity = mapper.toEntity(location);
        }

        LocationEntity savedEntity = jpaRepository.save(entity);

        // Domain events are preserved by the command handler before calling save()
        // The command handler gets domain events from the original location before save()
        // and publishes them after transaction commit. We return the saved location
        // which may not have events, but that's OK since events are already captured.
        return mapper.toDomain(savedEntity);
    }

    /**
     * Updates an existing entity with values from the domain model. Preserves JPA managed state and version for optimistic locking.
     *
     * @param entity   Existing JPA entity
     * @param location Domain location aggregate
     */
    private void updateEntityFromDomain(LocationEntity entity, Location location) {
        entity.setBarcode(location.getBarcode()
                .getValue());
        entity.setZone(location.getCoordinates()
                .getZone());
        entity.setAisle(location.getCoordinates()
                .getAisle());
        entity.setRack(location.getCoordinates()
                .getRack());
        entity.setLevel(location.getCoordinates()
                .getLevel());
        entity.setStatus(location.getStatus());

        // Update capacity
        LocationCapacity capacity = location.getCapacity();
        if (capacity != null) {
            entity.setCurrentQuantity(capacity.getCurrentQuantity());
            entity.setMaximumQuantity(capacity.getMaximumQuantity());
        }

        entity.setDescription(location.getDescription());
        entity.setLastModifiedAt(location.getLastModifiedAt());
        // Version is managed by JPA - don't update it manually
    }

    @Override
    public Optional<Location> findByIdAndTenantId(LocationId id, TenantId tenantId) {
        return jpaRepository.findByTenantIdAndId(tenantId.getValue(), id.getValue())
                .map(mapper::toDomain);
    }

    @Override
    public boolean existsByBarcodeAndTenantId(LocationBarcode barcode, TenantId tenantId) {
        return jpaRepository.existsByTenantIdAndBarcode(tenantId.getValue(), barcode.getValue());
    }

    @Override
    public List<Location> findByTenantId(TenantId tenantId) {
        return jpaRepository.findByTenantId(tenantId.getValue())
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
}

