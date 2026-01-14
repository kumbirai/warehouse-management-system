package com.ccbsa.wms.location.application.dto.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO: LocationCoordinatesDTO
 * <p>
 * Shared DTO for location coordinates used in both command and query DTOs.
 * Represents the physical coordinates of a warehouse location (zone, aisle, rack, level).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public final class LocationCoordinatesDTO {
    private String zone;
    private String aisle;
    private String rack;
    private String level;
}
