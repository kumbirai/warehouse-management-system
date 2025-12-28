package com.ccbsa.wms.location.application.dto.query;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO: LocationCapacityDTO
 * <p>
 * Nested DTO for location capacity information.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public final class LocationCapacityDTO {
    private BigDecimal currentQuantity;
    private BigDecimal maximumQuantity;
}

