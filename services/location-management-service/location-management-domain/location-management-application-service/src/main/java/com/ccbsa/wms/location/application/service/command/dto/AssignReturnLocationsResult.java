package com.ccbsa.wms.location.application.service.command.dto;

import java.util.Map;

import com.ccbsa.wms.location.domain.core.valueobject.LocationId;

import lombok.Builder;
import lombok.Getter;

/**
 * Result DTO: AssignReturnLocationsResult
 * <p>
 * Result of location assignment for return line items.
 */
@Getter
@Builder
public class AssignReturnLocationsResult {
    /**
     * Map of line item ID to assigned location ID.
     */
    private final Map<String, LocationId> assignments;
}
