package com.ccbsa.wms.stock.application.service.port.service;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;
import com.ccbsa.wms.stock.domain.core.valueobject.Quantity;

/**
 * Port: LocationServicePort
 * <p>
 * Interface for Location Management Service integration.
 * <p>
 * This port is used for synchronous calls to Location Management Service
 * for location availability and capacity validation.
 */
public interface LocationServicePort {

    /**
     * Checks location availability and capacity.
     *
     * @param locationId       Location ID to check
     * @param requiredQuantity Required quantity
     * @param tenantId         Tenant ID
     * @return LocationAvailability result
     */
    LocationAvailability checkLocationAvailability(LocationId locationId, Quantity requiredQuantity, TenantId tenantId);

    /**
     * Result object for location availability check.
     */
    class LocationAvailability {
        private final boolean available;
        private final boolean hasCapacity;
        private final Quantity availableCapacity;
        private final String reason;

        private LocationAvailability(boolean available, boolean hasCapacity, Quantity availableCapacity, String reason) {
            this.available = available;
            this.hasCapacity = hasCapacity;
            this.availableCapacity = availableCapacity;
            this.reason = reason;
        }

        public static LocationAvailability available(Quantity availableCapacity) {
            return new LocationAvailability(true, true, availableCapacity, null);
        }

        public static LocationAvailability unavailable(String reason) {
            return new LocationAvailability(false, false, null, reason);
        }

        public static LocationAvailability insufficientCapacity(Quantity availableCapacity) {
            return new LocationAvailability(true, false, availableCapacity, "Insufficient capacity");
        }

        public boolean isAvailable() {
            return available;
        }

        public boolean hasCapacity() {
            return hasCapacity;
        }

        public Quantity getAvailableCapacity() {
            return availableCapacity;
        }

        public String getReason() {
            return reason;
        }
    }
}

