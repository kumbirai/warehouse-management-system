package com.ccbsa.wms.gateway.api.fixture;

import java.util.HashMap;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

/**
 * Test data builder for Location entities.
 * Provides fluent API for creating location test data.
 */
@Slf4j
public final class LocationTestDataBuilder {
    private String zone;
    private String aisle;
    private String rack;
    private String level;
    private String barcode;
    private String description;

    private LocationTestDataBuilder() {
        // Private constructor
    }

    public static LocationTestDataBuilder builder() {
        return new LocationTestDataBuilder();
    }

    public LocationTestDataBuilder zone(String zone) {
        this.zone = zone;
        return this;
    }

    public LocationTestDataBuilder aisle(String aisle) {
        this.aisle = aisle;
        return this;
    }

    public LocationTestDataBuilder rack(String rack) {
        this.rack = rack;
        return this;
    }

    public LocationTestDataBuilder level(String level) {
        this.level = level;
        return this;
    }

    public LocationTestDataBuilder barcode(String barcode) {
        this.barcode = barcode;
        return this;
    }

    public LocationTestDataBuilder description(String description) {
        this.description = description;
        return this;
    }

    /**
     * Builds a map representing the location creation request.
     *
     * @return Map with location data
     */
    public Map<String, Object> build() {
        Map<String, Object> request = new HashMap<>();
        if (zone != null) {
            request.put("zone", zone);
        }
        if (aisle != null) {
            request.put("aisle", aisle);
        }
        if (rack != null) {
            request.put("rack", rack);
        }
        if (level != null) {
            request.put("level", level);
        }
        if (barcode != null) {
            request.put("barcode", barcode);
        }
        if (description != null) {
            request.put("description", description);
        }
        return request;
    }

    /**
     * Creates a default location with all required fields.
     *
     * @return Map with default location data
     */
    public static Map<String, Object> createDefault() {
        return builder()
                .zone("A")
                .aisle("01")
                .rack("01")
                .level("01")
                .description("Test location created by LocationTestDataBuilder")
                .build();
    }
}

