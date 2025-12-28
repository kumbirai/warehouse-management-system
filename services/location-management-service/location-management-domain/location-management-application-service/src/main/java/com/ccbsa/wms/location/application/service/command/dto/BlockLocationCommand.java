package com.ccbsa.wms.location.application.service.command.dto;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.common.domain.valueobject.UserId;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * Command DTO: BlockLocationCommand
 * <p>
 * Command for blocking a location.
 */
@Getter
@Builder
@EqualsAndHashCode
public final class BlockLocationCommand {
    private final TenantId tenantId;
    private final LocationId locationId;
    private final UserId blockedBy;
    private final String reason;
}

