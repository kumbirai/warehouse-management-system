package com.ccbsa.wms.location.application.service.command.dto;

import java.time.LocalDateTime;

import com.ccbsa.wms.location.domain.core.valueobject.LocationBarcode;
import com.ccbsa.wms.location.domain.core.valueobject.LocationCoordinates;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;
import com.ccbsa.wms.location.domain.core.valueobject.LocationStatus;

import lombok.Builder;
import lombok.Getter;

/**
 * Result DTO: UpdateLocationResult
 * <p>
 * Result object returned after updating a location.
 */
@Getter
@Builder
public final class UpdateLocationResult {
    private final LocationId locationId;
    private final LocationBarcode barcode;
    private final LocationCoordinates coordinates;
    private final LocationStatus status;
    private final String description;
    private final LocalDateTime lastModifiedAt;
}

