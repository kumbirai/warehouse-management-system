package com.ccbsa.wms.gateway.api.fixture;

import com.ccbsa.wms.gateway.api.dto.CreateLocationRequest;
import com.ccbsa.wms.gateway.api.dto.LocationDimensions;

/**
 * Builder for creating location test data with realistic coordinates.
 */
public class LocationTestDataBuilder {

    public static CreateLocationRequest buildWarehouseRequest() {
        String code = TestData.warehouseCode();
        // Extract zone identifier from code (e.g., "WH-01" -> "WH01")
        String zone = extractZoneFromCode(code);
        return CreateLocationRequest.builder()
                .code(code)
                .name(TestData.faker().company().name() + " Warehouse")
                .type("WAREHOUSE")
                .parentLocationId(null)
                .capacity(TestData.locationCapacity("WAREHOUSE"))
                .dimensions(warehouseDimensions())
                .zone(zone)
                .aisle("00")
                .rack("00")
                .level("00")
                .build();
    }

    private static LocationDimensions warehouseDimensions() {
        return LocationDimensions.builder().length(100.0).width(80.0).height(10.0).unit("M").build();
    }

    public static CreateLocationRequest buildZoneRequest(String parentLocationId) {
        String code = TestData.zoneCode();
        // Extract zone identifier from code (e.g., "ZONE-A" -> "ZONEA" or "ZONE-01" -> "ZONE01")
        String zone = extractZoneFromCode(code);
        return CreateLocationRequest.builder()
                .code(code)
                .name("Zone " + code)
                .type("ZONE")
                .parentLocationId(parentLocationId)
                .capacity(TestData.locationCapacity("ZONE"))
                .zone(zone)
                .aisle("00")
                .rack("00")
                .level("00")
                .build();
    }

    public static CreateLocationRequest buildAisleRequest(String parentLocationId) {
        String code = TestData.aisleCode();
        // Extract aisle identifier from code (e.g., "AISLE-01" -> "AISLE01")
        String aisle = extractAisleFromCode(code);
        return CreateLocationRequest.builder()
                .code(code)
                .name("Aisle " + code)
                .type("AISLE")
                .parentLocationId(parentLocationId)
                .capacity(TestData.locationCapacity("AISLE"))
                .zone("ZONE") // Will be derived from parent in hierarchy
                .aisle(aisle)
                .rack("00")
                .level("00")
                .build();
    }

    public static CreateLocationRequest buildRackRequest(String parentLocationId) {
        String code = TestData.rackCode();
        // Extract rack identifier from code (e.g., "RACK-A1" -> "RACKA1")
        String rack = extractRackFromCode(code);
        return CreateLocationRequest.builder()
                .code(code)
                .name("Rack " + code)
                .type("RACK")
                .parentLocationId(parentLocationId)
                .capacity(TestData.locationCapacity("RACK"))
                .zone("ZONE") // Will be derived from parent in hierarchy
                .aisle("AISLE") // Will be derived from parent in hierarchy
                .rack(rack)
                .level("00")
                .build();
    }

    public static CreateLocationRequest buildBinRequest(String parentLocationId) {
        String code = TestData.binCode();
        // Extract level/bin identifier from code (e.g., "BIN-01" -> "BIN01")
        String level = extractLevelFromCode(code);
        return CreateLocationRequest.builder()
                .code(code)
                .name("Bin " + code)
                .type("BIN")
                .parentLocationId(parentLocationId)
                .capacity(TestData.locationCapacity("BIN"))
                .zone("ZONE") // Will be derived from parent in hierarchy
                .aisle("AISLE") // Will be derived from parent in hierarchy
                .rack("RACK") // Will be derived from parent in hierarchy
                .level(level)
                .build();
    }

    /**
     * Extracts zone identifier from location code by removing non-alphanumeric characters
     * and truncating to 10 characters max (coordinate limit).
     * Example: "WH-01" -> "WH01", "ZONE-A" -> "ZONEA"
     */
    private static String extractZoneFromCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return "00";
        }
        String sanitized = code.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
        return sanitized.length() > 10 ? sanitized.substring(0, 10) : sanitized;
    }

    /**
     * Extracts aisle identifier from location code.
     * Example: "AISLE-01" -> "AISLE01"
     */
    private static String extractAisleFromCode(String code) {
        return extractZoneFromCode(code); // Same sanitization logic
    }

    /**
     * Extracts rack identifier from location code.
     * Example: "RACK-A1" -> "RACKA1"
     */
    private static String extractRackFromCode(String code) {
        return extractZoneFromCode(code); // Same sanitization logic
    }

    /**
     * Extracts level/bin identifier from location code.
     * Example: "BIN-01" -> "BIN01"
     */
    private static String extractLevelFromCode(String code) {
        return extractZoneFromCode(code); // Same sanitization logic
    }
}

