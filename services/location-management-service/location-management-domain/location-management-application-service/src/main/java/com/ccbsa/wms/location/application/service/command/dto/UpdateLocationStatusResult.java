package com.ccbsa.wms.location.application.service.command.dto;

import java.time.LocalDateTime;

import com.ccbsa.wms.location.domain.core.valueobject.LocationId;

import lombok.Builder;
import lombok.Getter;

/**
 * Result DTO: UpdateLocationStatusResult
 * <p>
 * Result object for location status update operation.
 */
@Getter
@Builder
public final class UpdateLocationStatusResult {
    private final LocationId locationId;
    private final String status;
    private final LocalDateTime lastModifiedAt;
}

