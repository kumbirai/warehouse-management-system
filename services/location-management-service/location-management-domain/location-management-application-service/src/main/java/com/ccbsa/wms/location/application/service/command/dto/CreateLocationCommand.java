package com.ccbsa.wms.location.application.service.command.dto;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.location.domain.core.valueobject.LocationBarcode;
import com.ccbsa.wms.location.domain.core.valueobject.LocationCoordinates;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Command DTO: CreateLocationCommand
 * <p>
 * Command object for creating a new warehouse location.
 */
@Getter
@Builder
@AllArgsConstructor
public final class CreateLocationCommand {
    private final TenantId tenantId;
    private final LocationCoordinates coordinates;
    private final LocationBarcode barcode; // Optional - will be generated if null
    private final String description;
    private final String code; // Original location code from request
    private final String name; // Location name from request
    private final String type; // Location type from request
    private final String parentLocationId; // Parent location ID for hierarchical relationships

    /**
     * Static factory method with validation.
     */
    public static CreateLocationCommand of(TenantId tenantId, LocationCoordinates coordinates, LocationBarcode barcode, String description, String code, String name, String type,
                                           String parentLocationId) {
        if (tenantId == null) {
            throw new IllegalArgumentException("TenantId is required");
        }
        if (coordinates == null) {
            throw new IllegalArgumentException("LocationCoordinates is required");
        }
        return CreateLocationCommand.builder().tenantId(tenantId).coordinates(coordinates).barcode(barcode).description(description).code(code).name(name).type(type)
                .parentLocationId(parentLocationId).build();
    }
}

