package com.ccbsa.wms.location.application.dto.query;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Query Result DTO: LocationAvailabilityQueryResultDTO
 * <p>
 * API response DTO for location availability check.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationAvailabilityQueryResultDTO {
    private boolean available;
    private boolean hasCapacity;
    private Integer availableCapacity;
    private String reason;
}

