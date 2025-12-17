package com.ccbsa.wms.location.application.dto.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Command DTO: CreateLocationCommandDTO
 * <p>
 * Request DTO for creating a new warehouse location.
 */
public final class CreateLocationCommandDTO {
    @NotBlank(message = "Zone is required")
    @Size(max = 100,
            message = "Zone must not exceed 100 characters")
    private String zone;

    @NotBlank(message = "Aisle is required")
    @Size(max = 100,
            message = "Aisle must not exceed 100 characters")
    private String aisle;

    @NotBlank(message = "Rack is required")
    @Size(max = 100,
            message = "Rack must not exceed 100 characters")
    private String rack;

    @NotBlank(message = "Level is required")
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
}

