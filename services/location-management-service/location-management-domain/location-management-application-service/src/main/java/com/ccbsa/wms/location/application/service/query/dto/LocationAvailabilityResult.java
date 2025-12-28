package com.ccbsa.wms.location.application.service.query.dto;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Getter;

/**
 * Query Result DTO: LocationAvailabilityResult
 * <p>
 * Result object for location availability check.
 */
@Getter
@Builder
public final class LocationAvailabilityResult {
    private final boolean available;
    private final boolean hasCapacity;
    private final BigDecimal availableCapacity;
    private final String reason;
}

