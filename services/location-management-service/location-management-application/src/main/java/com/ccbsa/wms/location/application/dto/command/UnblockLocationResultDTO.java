package com.ccbsa.wms.location.application.dto.command;

import java.util.UUID;

import com.ccbsa.wms.location.domain.core.valueobject.LocationStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Result DTO: UnblockLocationResultDTO
 * <p>
 * Response DTO for unblocking a location.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnblockLocationResultDTO {
    private UUID locationId;
    private LocationStatus status;
}

