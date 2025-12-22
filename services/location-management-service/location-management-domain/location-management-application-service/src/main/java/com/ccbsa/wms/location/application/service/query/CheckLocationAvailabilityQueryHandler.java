package com.ccbsa.wms.location.application.service.query;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ccbsa.wms.location.application.service.port.repository.LocationRepository;
import com.ccbsa.wms.location.application.service.query.dto.CheckLocationAvailabilityQuery;
import com.ccbsa.wms.location.application.service.query.dto.LocationAvailabilityResult;
import com.ccbsa.wms.location.domain.core.entity.Location;
import com.ccbsa.wms.location.domain.core.exception.LocationNotFoundException;

/**
 * Query Handler: CheckLocationAvailabilityQueryHandler
 * <p>
 * Handles checking location availability and capacity.
 * <p>
 * Responsibilities:
 * - Load location from repository
 * - Check availability status
 * - Check capacity
 * - Return availability result
 */
@Component
public class CheckLocationAvailabilityQueryHandler {
    private final LocationRepository repository;

    public CheckLocationAvailabilityQueryHandler(LocationRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public LocationAvailabilityResult handle(CheckLocationAvailabilityQuery query) {
        // 1. Load location
        Location location = repository.findByIdAndTenantId(query.getLocationId(), query.getTenantId())
                .orElseThrow(() -> new LocationNotFoundException(String.format("Location not found: %s", query.getLocationId().getValueAsString())));

        // 2. Check availability
        if (!location.isAvailable() && location.getStatus() != com.ccbsa.wms.location.domain.core.valueobject.LocationStatus.RESERVED) {
            return LocationAvailabilityResult.builder().available(false).hasCapacity(false).reason(String.format("Location is not available. Status: %s", location.getStatus()))
                    .build();
        }

        // 3. Check capacity
        boolean hasCapacity = location.hasCapacity(query.getRequiredQuantity());
        BigDecimal availableCapacity = location.getAvailableCapacity();

        return LocationAvailabilityResult.builder().available(true).hasCapacity(hasCapacity).availableCapacity(availableCapacity)
                .reason(hasCapacity ? null : "Insufficient capacity").build();
    }
}

