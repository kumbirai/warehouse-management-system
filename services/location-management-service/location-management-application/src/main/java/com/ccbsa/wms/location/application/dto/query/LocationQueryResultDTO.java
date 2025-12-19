package com.ccbsa.wms.location.application.dto.query;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Query Result DTO: LocationQueryResultDTO
 * <p>
 * Response DTO for location query operations.
 */
public final class LocationQueryResultDTO {
    private String locationId;
    private String code;
    private String name;
    private String type;
    private String path;
    private String barcode;
    private LocationCoordinatesDTO coordinates;
    private String status;
    @JsonIgnore // Hide from JSON serialization - use getCapacity()/setCapacity() for Integer instead
    private LocationCapacityDTO capacityDTO;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime lastModifiedAt;

    public LocationQueryResultDTO() {
    }

    public LocationQueryResultDTO(String locationId, String barcode, LocationCoordinatesDTO coordinates, String status, LocationCapacityDTO capacity, String description,
                                  LocalDateTime createdAt, LocalDateTime lastModifiedAt) {
        this.locationId = locationId;
        this.barcode = barcode;
        this.coordinates = coordinates;
        this.status = status;
        this.capacityDTO = capacity;
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

    @JsonIgnore // Internal use only - not serialized to JSON
    public LocationCapacityDTO getCapacityDTO() {
        return capacityDTO;
    }

    @JsonIgnore // Internal use only - not deserialized from JSON
    public void setCapacityDTO(LocationCapacityDTO capacity) {
        this.capacityDTO = capacity;
    }

    // JSON getter for capacity as Integer (maps to "capacity" field)
    @JsonProperty("capacity")
    public Integer getCapacity() {
        if (capacityDTO != null && capacityDTO.getMaximumQuantity() != null) {
            return capacityDTO.getMaximumQuantity().intValue();
        }
        return null;
    }

    // JSON setter for capacity as Integer (maps from "capacity" field)
    @JsonProperty("capacity")
    public void setCapacity(Integer capacity) {
        // When deserializing, create a LocationCapacityDTO with the Integer as maximumQuantity
        if (capacity != null) {
            this.capacityDTO = new LocationCapacityDTO(
                    java.math.BigDecimal.ZERO, // currentQuantity defaults to 0
                    java.math.BigDecimal.valueOf(capacity) // maximumQuantity from Integer
            );
        }
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

