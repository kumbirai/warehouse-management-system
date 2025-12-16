package com.ccbsa.wms.location.application.dto.query;

/**
 * DTO: LocationCoordinatesDTO
 * <p>
 * Nested DTO for location coordinates (used in query DTOs).
 */
public final class LocationCoordinatesDTO {
    private String zone;
    private String aisle;
    private String rack;
    private String level;

    public LocationCoordinatesDTO() {
    }

    public LocationCoordinatesDTO(String zone, String aisle, String rack, String level) {
        this.zone = zone;
        this.aisle = aisle;
        this.rack = rack;
        this.level = level;
    }

    public String getZone() {
        return zone;
    }

    public void setZone(String zone) {
        this.zone = zone;
    }

    public String getAisle() {
        return aisle;
    }

    public void setAisle(String aisle) {
        this.aisle = aisle;
    }

    public String getRack() {
        return rack;
    }

    public void setRack(String rack) {
        this.rack = rack;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }
}

