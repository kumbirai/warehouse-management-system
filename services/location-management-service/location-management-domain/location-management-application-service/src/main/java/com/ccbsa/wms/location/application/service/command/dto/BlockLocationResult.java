package com.ccbsa.wms.location.application.service.command.dto;

import java.time.LocalDateTime;

import com.ccbsa.wms.location.domain.core.valueobject.LocationId;
import com.ccbsa.wms.location.domain.core.valueobject.LocationStatus;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * Result DTO: BlockLocationResult
 * <p>
 * Result for blocking a location.
 */
@Getter
@Builder
@EqualsAndHashCode
public final class BlockLocationResult {
    private final LocationId locationId;
    private final LocationStatus status;
    private final LocalDateTime lastModifiedAt;
}

