package com.ccbsa.wms.location.domain.core.valueobject;

import java.util.Objects;

/**
 * Value Object: LocationCoordinates
 *
 * Represents the physical coordinates of a warehouse location. Immutable and self-validating.
 *
 * Coordinates consist of: - Zone: Warehouse zone identifier - Aisle: Aisle identifier within the zone - Rack: Rack identifier within the aisle - Level: Level identifier within the
 * rack
 */
public final class LocationCoordinates {
    private final String zone;
    private final String aisle;
    private final String rack;
    private final String level;

    /**
     * Private constructor to enforce immutability.
     *
     * @param zone  Zone identifier
     * @param aisle Aisle identifier
     * @param rack  Rack identifier
     * @param level Level identifier
     * @throws IllegalArgumentException if any parameter is invalid
     */
    private LocationCoordinates(String zone, String aisle, String rack, String level) {
        validate(zone, aisle, rack, level);
        this.zone = zone;
        this.aisle = aisle;
        this.rack = rack;
        this.level = level;
    }

    /**
     * Validates the coordinates according to business rules.
     *
     * @param zone  Zone identifier
     * @param aisle Aisle identifier
     * @param rack  Rack identifier
     * @param level Level identifier
     * @throws IllegalArgumentException if validation fails
     */
    private void validate(String zone, String aisle, String rack, String level) {
        if (zone == null || zone.trim().isEmpty()) {
            throw new IllegalArgumentException("Zone cannot be null or empty");
        }
        if (aisle == null || aisle.trim().isEmpty()) {
            throw new IllegalArgumentException("Aisle cannot be null or empty");
        }
        if (rack == null || rack.trim().isEmpty()) {
            throw new IllegalArgumentException("Rack cannot be null or empty");
        }
        if (level == null || level.trim().isEmpty()) {
            throw new IllegalArgumentException("Level cannot be null or empty");
        }
    }

    /**
     * Factory method to create LocationCoordinates instance.
     *
     * @param zone  Zone identifier
     * @param aisle Aisle identifier
     * @param rack  Rack identifier
     * @param level Level identifier
     * @return LocationCoordinates instance
     * @throws IllegalArgumentException if any parameter is invalid
     */
    public static LocationCoordinates of(String zone, String aisle, String rack, String level) {
        return new LocationCoordinates(zone, aisle, rack, level);
    }

    /**
     * Returns the zone identifier.
     *
     * @return Zone identifier
     */
    public String getZone() {
        return zone;
    }

    /**
     * Returns the aisle identifier.
     *
     * @return Aisle identifier
     */
    public String getAisle() {
        return aisle;
    }

    /**
     * Returns the rack identifier.
     *
     * @return Rack identifier
     */
    public String getRack() {
        return rack;
    }

    /**
     * Returns the level identifier.
     *
     * @return Level identifier
     */
    public String getLevel() {
        return level;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        LocationCoordinates that = (LocationCoordinates) o;
        return Objects.equals(zone, that.zone) && Objects.equals(aisle, that.aisle) && Objects.equals(rack, that.rack) && Objects.equals(level, that.level);
    }

    @Override
    public int hashCode() {
        return Objects.hash(zone, aisle, rack, level);
    }

    @Override
    public String toString() {
        return toFormattedString();
    }

    /**
     * Returns a formatted string representation of the coordinates.
     *
     * @return Formatted coordinates string (e.g., "A-01-01-01")
     */
    public String toFormattedString() {
        return String.format("%s-%s-%s-%s", zone, aisle, rack, level);
    }
}

