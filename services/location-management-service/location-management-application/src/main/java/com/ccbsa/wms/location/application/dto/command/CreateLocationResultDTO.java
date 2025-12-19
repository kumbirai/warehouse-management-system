package com.ccbsa.wms.location.application.dto.command;

import java.time.LocalDateTime;

/**
 * Result DTO: CreateLocationResultDTO
 * <p>
 * Response DTO returned after creating a location.
 */
public final class CreateLocationResultDTO {
    private String locationId;
    private String code;
    private String name;
    private String type;
    private String path;
    private String barcode;
    private LocationCoordinatesDTO coordinates;
    private String status;
    private LocalDateTime createdAt;

    public CreateLocationResultDTO() {
    }

    public CreateLocationResultDTO(String locationId, String barcode, LocationCoordinatesDTO coordinates, String status, LocalDateTime createdAt) {
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

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}

