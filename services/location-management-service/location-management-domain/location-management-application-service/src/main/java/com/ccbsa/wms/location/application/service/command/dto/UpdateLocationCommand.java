package com.ccbsa.wms.location.application.service.command.dto;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.location.domain.core.valueobject.LocationBarcode;
import com.ccbsa.wms.location.domain.core.valueobject.LocationCoordinates;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Command DTO: UpdateLocationCommand
 * <p>
 * Command object for updating an existing warehouse location.
 */
@Getter
@Builder
@AllArgsConstructor
public final class UpdateLocationCommand {
    private final LocationId locationId;
    private final TenantId tenantId;
    private final LocationCoordinates coordinates;
    private final LocationBarcode barcode; // Optional - can be null if not updating
    private final String description; // Optional - can be null if not updating

    /**
     * Static factory method with validation.
     */
    public static UpdateLocationCommand of(LocationId locationId, TenantId tenantId, LocationCoordinates coordinates, LocationBarcode barcode, String description) {
        if (locationId == null) {
            throw new IllegalArgumentException("LocationId is required");
        }
        if (tenantId == null) {
            throw new IllegalArgumentException("TenantId is required");
        }
        if (coordinates == null) {
            throw new IllegalArgumentException("LocationCoordinates is required");
        }
        return UpdateLocationCommand.builder().locationId(locationId).tenantId(tenantId).coordinates(coordinates).barcode(barcode).description(description).build();
    }
}

