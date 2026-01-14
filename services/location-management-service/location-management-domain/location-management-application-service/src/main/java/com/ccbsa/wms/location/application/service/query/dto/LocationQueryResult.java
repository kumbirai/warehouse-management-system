package com.ccbsa.wms.location.application.service.query.dto;

import java.time.LocalDateTime;

import com.ccbsa.wms.location.domain.core.valueobject.LocationBarcode;
import com.ccbsa.wms.location.domain.core.valueobject.LocationCapacity;
import com.ccbsa.wms.location.domain.core.valueobject.LocationCoordinates;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;
import com.ccbsa.wms.location.domain.core.valueobject.LocationStatus;

import lombok.Builder;
import lombok.Getter;

/**
 * Query Result DTO: LocationQueryResult
 * <p>
 * Result object returned from location queries. Contains optimized read model data for location information.
 */
@Getter
@Builder
public final class LocationQueryResult {
    private final LocationId locationId;
    private final LocationBarcode barcode;
    private final LocationCoordinates coordinates;
    private final LocationStatus status;
    private final LocationCapacity capacity;
    private final String code;
    private final String name;
    private final String type;
    private final String path;
    private final String description;
    private final LocationId parentLocationId;
    private final LocalDateTime createdAt;
    private final LocalDateTime lastModifiedAt;

    public LocationQueryResult(LocationId locationId, LocationBarcode barcode, LocationCoordinates coordinates, LocationStatus status, LocationCapacity capacity, String code,
                               String name, String type, String path, String description, LocationId parentLocationId, LocalDateTime createdAt, LocalDateTime lastModifiedAt) {
        if (locationId == null) {
            throw new IllegalArgumentException("LocationId is required");
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
        this.barcode = barcode;
        this.coordinates = coordinates;
        this.status = status;
        this.capacity = capacity;
        this.code = code;
        this.name = name;
        this.type = type;
        this.path = path;
        this.description = description;
        this.parentLocationId = parentLocationId;
        this.createdAt = createdAt;
        this.lastModifiedAt = lastModifiedAt;
    }
}

