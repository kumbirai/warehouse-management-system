package com.ccbsa.wms.location.application.dto.command;

import jakarta.validation.constraints.Size;

/**
 * Command DTO: CreateLocationCommandDTO
 * <p>
 * Request DTO for creating a new warehouse location.
 * Supports both hierarchical model (code, name, type, parentLocationId) and coordinate-based model (zone, aisle, rack, level).
 */
public final class CreateLocationCommandDTO {
    // Hierarchical model fields
    @Size(max = 100,
            message = "Code must not exceed 100 characters")
    private String code;

    @Size(max = 255,
            message = "Name must not exceed 255 characters")
    private String name;

    @Size(max = 50,
            message = "Type must not exceed 50 characters")
    private String type;

    @Size(max = 255,
            message = "Parent location ID must not exceed 255 characters")
    private String parentLocationId;

    private Integer capacity;

    private LocationDimensionsDTO dimensions;

    // Coordinate-based model fields (optional - used for direct coordinate specification)
    @Size(max = 100,
            message = "Zone must not exceed 100 characters")
    private String zone;

    @Size(max = 100,
            message = "Aisle must not exceed 100 characters")
    private String aisle;

    @Size(max = 100,
            message = "Rack must not exceed 100 characters")
    private String rack;

    @Size(max = 100,
            message = "Level must not exceed 100 characters")
    private String level;

    @Size(max = 255,
            message = "Barcode must not exceed 255 characters")
    private String barcode; // Optional - will be auto-generated if not provided

    @Size(max = 500,
            message = "Description must not exceed 500 characters")
    private String description;

    public CreateLocationCommandDTO() {
    }

    public CreateLocationCommandDTO(String zone, String aisle, String rack, String level, String barcode, String description) {
        this.zone = zone;
        this.aisle = aisle;
        this.rack = rack;
        this.level = level;
        this.barcode = barcode;
        this.description = description;
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

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getParentLocationId() {
        return parentLocationId;
    }

    public void setParentLocationId(String parentLocationId) {
        this.parentLocationId = parentLocationId;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public LocationDimensionsDTO getDimensions() {
        return dimensions;
    }

    public void setDimensions(LocationDimensionsDTO dimensions) {
        this.dimensions = dimensions;
    }

    /**
     * Checks if this DTO uses the hierarchical model (has code/type).
     *
     * @return true if hierarchical model is used
     */
    public boolean isHierarchicalModel() {
        return code != null || type != null;
    }

    /**
     * Checks if this DTO uses the coordinate-based model (has zone/aisle/rack/level).
     *
     * @return true if coordinate-based model is used
     */
    public boolean isCoordinateBasedModel() {
        return zone != null || aisle != null || rack != null || level != null;
    }
}

