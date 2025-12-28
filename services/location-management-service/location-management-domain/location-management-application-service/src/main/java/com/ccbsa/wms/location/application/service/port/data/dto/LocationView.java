package com.ccbsa.wms.location.application.service.port.data.dto;

import java.time.LocalDateTime;

import com.ccbsa.wms.location.domain.core.valueobject.LocationBarcode;
import com.ccbsa.wms.location.domain.core.valueobject.LocationCapacity;
import com.ccbsa.wms.location.domain.core.valueobject.LocationCoordinates;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;
import com.ccbsa.wms.location.domain.core.valueobject.LocationStatus;

import lombok.Builder;
import lombok.Getter;

/**
 * Read Model DTO: LocationView
 * <p>
 * Optimized read model representation of Location aggregate for query operations.
 * <p>
 * This is a denormalized view optimized for read queries, separate from the write model (Location aggregate).
 * <p>
 * Fields are flattened and optimized for query performance.
 */
@Getter
@Builder
public final class LocationView {
    private final LocationId locationId;
    private final String tenantId;
    private final LocationBarcode barcode;
    private final LocationCoordinates coordinates;
    private final LocationStatus status;
    private final LocationCapacity capacity;
    private final String code;
    private final String name;
    private final String type;
    private final String description;
    private final LocationId parentLocationId;
    private final LocalDateTime createdAt;
    private final LocalDateTime lastModifiedAt;

    public LocationView(LocationId locationId, String tenantId, LocationBarcode barcode, LocationCoordinates coordinates, LocationStatus status, LocationCapacity capacity,
                        String code, String name, String type, String description, LocationId parentLocationId, LocalDateTime createdAt, LocalDateTime lastModifiedAt) {
        if (locationId == null) {
            throw new IllegalArgumentException("LocationId is required");
        }
        if (tenantId == null) {
            throw new IllegalArgumentException("TenantId is required");
        }
        if (barcode == null) {
            throw new IllegalArgumentException("LocationBarcode is required");
        }
        if (coordinates == null) {
            throw new IllegalArgumentException("LocationCoordinates is required");
        }
        if (status == null) {
            throw new IllegalArgumentException("LocationStatus is required");
        }
        this.locationId = locationId;
        this.tenantId = tenantId;
        this.barcode = barcode;
        this.coordinates = coordinates;
        this.status = status;
        this.capacity = capacity;
        this.code = code;
        this.name = name;
        this.type = type;
        this.description = description;
        this.parentLocationId = parentLocationId;
        this.createdAt = createdAt;
        this.lastModifiedAt = lastModifiedAt;
    }
}

