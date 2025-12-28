package com.ccbsa.wms.location.application.dto.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO: LocationDimensionsDTO
 * <p>
 * Represents physical dimensions of a location.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public final class LocationDimensionsDTO {
    private Double length;
    private Double width;
    private Double height;
    private String unit;
}

