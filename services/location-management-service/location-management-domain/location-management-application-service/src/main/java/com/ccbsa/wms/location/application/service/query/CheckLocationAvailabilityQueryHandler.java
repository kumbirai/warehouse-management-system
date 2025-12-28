package com.ccbsa.wms.location.application.service.query;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ccbsa.wms.location.application.service.port.data.LocationViewRepository;
import com.ccbsa.wms.location.application.service.query.dto.CheckLocationAvailabilityQuery;
import com.ccbsa.wms.location.application.service.query.dto.LocationAvailabilityResult;
import com.ccbsa.wms.location.domain.core.exception.LocationNotFoundException;
import com.ccbsa.wms.location.domain.core.valueobject.LocationStatus;

import lombok.RequiredArgsConstructor;

/**
 * Query Handler: CheckLocationAvailabilityQueryHandler
 * <p>
 * Handles checking location availability and capacity using read model.
 * <p>
 * Responsibilities:
 * - Load location view from data port (read model)
 * - Check availability status
 * - Check capacity
 * - Return availability result
 * <p>
 * Uses data port (LocationViewRepository) instead of repository port for CQRS compliance.
 * <p>
 * Note: This handler uses the read model, but availability and capacity checks are business logic
 * that could be computed from the view data. The domain methods (isAvailable, hasCapacity) are
 * not available on the view, so we compute the logic here.
 */
@Component
@RequiredArgsConstructor
public class CheckLocationAvailabilityQueryHandler {
    private final LocationViewRepository viewRepository;

    @Transactional(readOnly = true)
    public LocationAvailabilityResult handle(CheckLocationAvailabilityQuery query) {
        // 1. Load location view
        var locationView = viewRepository.findByTenantIdAndId(query.getTenantId(), query.getLocationId())
                .orElseThrow(() -> new LocationNotFoundException(String.format("Location not found: %s", query.getLocationId().getValueAsString())));

        // 2. Check availability (status must be AVAILABLE or RESERVED)
        LocationStatus status = locationView.getStatus();
        if (status != LocationStatus.AVAILABLE && status != LocationStatus.RESERVED) {
            return LocationAvailabilityResult.builder().available(false).hasCapacity(false).reason(String.format("Location is not available. Status: %s", status)).build();
        }

        // 3. Check capacity
        boolean hasCapacity = checkCapacity(locationView, query.getRequiredQuantity());
        BigDecimal availableCapacity = locationView.getCapacity() != null ? locationView.getCapacity().getAvailableCapacity() : null;

        return LocationAvailabilityResult.builder().available(true).hasCapacity(hasCapacity).availableCapacity(availableCapacity)
                .reason(hasCapacity ? null : "Insufficient capacity").build();
    }

    /**
     * Checks if location has capacity for the required quantity.
     *
     * @param locationView     Location view
     * @param requiredQuantity Required quantity
     * @return true if location can accommodate the quantity
     */
    private boolean checkCapacity(com.ccbsa.wms.location.application.service.port.data.dto.LocationView locationView, BigDecimal requiredQuantity) {
        if (requiredQuantity == null || requiredQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            return true;
        }

        if (locationView.getCapacity() == null) {
            return true; // No capacity constraints
        }

        return locationView.getCapacity().canAccommodate(requiredQuantity);
    }
}

