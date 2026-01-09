package com.ccbsa.wms.stock.application.service.port.service;

import java.util.Optional;

import com.ccbsa.common.domain.valueobject.Quantity;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;

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
     * Gets location information by location ID.
     *
     * @param locationId Location ID
     * @param tenantId   Tenant ID
     * @return Optional LocationInfo containing location details
     */
    Optional<LocationInfo> getLocationInfo(LocationId locationId, TenantId tenantId);

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

    /**
     * Location information result object.
     */
    record LocationInfo(String locationId, String name, String description, String code, String type, LocationCoordinates coordinates) {
        /**
         * Gets the display name for the location.
         * Prefers description, then name, then code, finally locationId.
         *
         * @return Display name string
         */
        public String getDisplayName() {
            if (description != null && !description.trim().isEmpty()) {
                return description.trim();
            }
            if (name != null && !name.trim().isEmpty()) {
                return name.trim();
            }
            if (code != null && !code.trim().isEmpty()) {
                return code.trim();
            }
            return locationId;
        }

        /**
         * Checks if the location is of BIN type (lowest hierarchy level).
         *
         * @return true if location type is BIN, false otherwise
         */
        public boolean isBinType() {
            return type != null && "BIN".equalsIgnoreCase(type.trim());
        }

        /**
         * Gets the location hierarchy as a formatted string.
         * Format: {zone}-{aisle}-{rack}-{level}
         *
         * @return Hierarchy string or null if coordinates are not available
         */
        public String getHierarchy() {
            if (coordinates == null) {
                return null;
            }
            return String.format("%s-%s-%s-%s", coordinates.zone() != null ? coordinates.zone() : "", coordinates.aisle() != null ? coordinates.aisle() : "",
                    coordinates.rack() != null ? coordinates.rack() : "", coordinates.level() != null ? coordinates.level() : "");
        }
    }

    /**
     * Location coordinates (zone, aisle, rack, level).
     */
    record LocationCoordinates(String zone, String aisle, String rack, String level) {
    }
}

