package com.ccbsa.wms.location.application.dto.query;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO: LocationCoordinatesDTO
 * <p>
 * Nested DTO for location coordinates (used in query DTOs).
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

