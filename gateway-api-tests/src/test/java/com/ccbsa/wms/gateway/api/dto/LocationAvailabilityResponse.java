package com.ccbsa.wms.gateway.api.dto;

/**
 * DTO: LocationAvailabilityResponse
 * <p>
 * Response DTO for location availability check.
 */
public class LocationAvailabilityResponse {
    private boolean available;
    private boolean hasCapacity;
    private Integer availableCapacity;
    private String reason;

    public LocationAvailabilityResponse() {
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public boolean isHasCapacity() {
        return hasCapacity;
    }

    public void setHasCapacity(boolean hasCapacity) {
        this.hasCapacity = hasCapacity;
    }

    public Integer getAvailableCapacity() {
        return availableCapacity;
    }

    public void setAvailableCapacity(Integer availableCapacity) {
        this.availableCapacity = availableCapacity;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}

