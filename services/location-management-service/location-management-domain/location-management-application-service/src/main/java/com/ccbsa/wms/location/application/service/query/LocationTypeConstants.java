package com.ccbsa.wms.location.application.service.query;

/**
 * Constants for location type values.
 * <p>
 * Used to avoid primitive obsession when working with location type strings.
 * These constants represent the valid location type values: WAREHOUSE, ZONE, AISLE, RACK, BIN.
 */
public final class LocationTypeConstants {
    public static final String WAREHOUSE = "WAREHOUSE";
    public static final String ZONE = "ZONE";
    public static final String AISLE = "AISLE";
    public static final String RACK = "RACK";
    public static final String BIN = "BIN";
    public static final String LEVEL = "LEVEL";

    private LocationTypeConstants() {
        // Utility class - prevent instantiation
    }
}
