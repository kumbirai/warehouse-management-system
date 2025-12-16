package com.ccbsa.wms.location.application.dto.query;

import java.time.LocalDateTime;

/**
 * Query Result DTO: LocationQueryResultDTO
 * <p>
 * Response DTO for location query operations.
 */
public final class LocationQueryResultDTO {
    private String locationId;
    private String barcode;
    private LocationCoordinatesDTO coordinates;
    private String status;
    private LocationCapacityDTO capacity;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime lastModifiedAt;

    public LocationQueryResultDTO() {
    }

    public LocationQueryResultDTO(String locationId, String barcode, LocationCoordinatesDTO coordinates,
                                  String status, LocationCapacityDTO capacity, String description,
                                  LocalDateTime createdAt, LocalDateTime lastModifiedAt) {
        this.locationId = locationId;
        this.barcode = barcode;
        this.coordinates = coordinates;
        this.status = status;
        this.capacity = capacity;
        this.description = description;
        this.createdAt = createdAt;
        this.lastModifiedAt = lastModifiedAt;
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

    public LocationCapacityDTO getCapacity() {
        return capacity;
    }

    public void setCapacity(LocationCapacityDTO capacity) {
        this.capacity = capacity;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getLastModifiedAt() {
        return lastModifiedAt;
    }

    public void setLastModifiedAt(LocalDateTime lastModifiedAt) {
        this.lastModifiedAt = lastModifiedAt;
    }
}

