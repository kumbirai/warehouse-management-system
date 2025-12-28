package com.ccbsa.wms.location.application.service.command.dto;

import java.time.LocalDateTime;

import com.ccbsa.wms.location.domain.core.valueobject.LocationBarcode;
import com.ccbsa.wms.location.domain.core.valueobject.LocationCoordinates;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;
import com.ccbsa.wms.location.domain.core.valueobject.LocationStatus;

import lombok.Builder;
import lombok.Getter;

/**
 * Result DTO: CreateLocationResult
 * <p>
 * Result object returned after creating a location. Contains only the information needed by the caller (not the full domain entity).
 */
@Getter
@Builder
public final class CreateLocationResult {
    private final LocationId locationId;
    private final LocationBarcode barcode;
    private final LocationCoordinates coordinates;
    private final LocationStatus status;
    private final LocalDateTime createdAt;
    private final String code; // Original location code
    private final String name; // Location name
    private final String type; // Location type
    private final String path; // Hierarchical path

    public CreateLocationResult(LocationId locationId, LocationBarcode barcode, LocationCoordinates coordinates, LocationStatus status, LocalDateTime createdAt, String code,
                                String name, String type, String path) {
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
        this.createdAt = createdAt;
        this.code = code;
        this.name = name;
        this.type = type;
        this.path = path;
    }
}

