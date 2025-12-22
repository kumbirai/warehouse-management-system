package com.ccbsa.wms.gateway.api.dto;

/**
 * DTO: AssignLocationToStockRequest
 * <p>
 * Request DTO for assigning a location to a stock item.
 */
public class AssignLocationToStockRequest {
    private String locationId;
    private Integer quantity;

    public AssignLocationToStockRequest() {
    }

    public String getLocationId() {
        return locationId;
    }

    public void setLocationId(String locationId) {
        this.locationId = locationId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}

