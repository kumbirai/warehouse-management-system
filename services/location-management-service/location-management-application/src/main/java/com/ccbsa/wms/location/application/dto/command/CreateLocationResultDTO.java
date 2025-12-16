package com.ccbsa.wms.location.application.dto.command;

import java.time.LocalDateTime;

/**
 * Result DTO: CreateLocationResultDTO
 * <p>
 * Response DTO returned after creating a location.
 */
public final class CreateLocationResultDTO {
    private String locationId;
    private String barcode;
    private LocationCoordinatesDTO coordinates;
    private String status;
    private LocalDateTime createdAt;

    public CreateLocationResultDTO() {
    }

    public CreateLocationResultDTO(String locationId, String barcode, LocationCoordinatesDTO coordinates,
                                   String status, LocalDateTime createdAt) {
        this.locationId = locationId;
        this.barcode = barcode;
        this.coordinates = coordinates;
        this.status = status;
        this.createdAt = createdAt;
    }

    public String getLocationId() {
        return locationId;
    }

    public void setLocationId(String locationId) {
        this.locationId = locationId;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public LocationCoordinatesDTO getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(LocationCoordinatesDTO coordinates) {
        this.coordinates = coordinates;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

