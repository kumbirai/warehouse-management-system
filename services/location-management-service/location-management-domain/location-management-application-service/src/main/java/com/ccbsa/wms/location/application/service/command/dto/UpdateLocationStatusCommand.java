package com.ccbsa.wms.location.application.service.command.dto;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;
import com.ccbsa.wms.location.domain.core.valueobject.LocationStatus;

import lombok.Builder;
import lombok.Getter;

/**
 * Command DTO: UpdateLocationStatusCommand
 * <p>
 * Command object for updating a location's status.
 */
@Getter
@Builder
public final class UpdateLocationStatusCommand {
    private final LocationId locationId;
    private final TenantId tenantId;
    private final LocationStatus status;
    private final String reason;
}

