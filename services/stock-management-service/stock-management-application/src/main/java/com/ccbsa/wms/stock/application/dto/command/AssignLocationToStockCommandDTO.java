package com.ccbsa.wms.stock.application.dto.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Command DTO: AssignLocationToStockCommandDTO
 * <p>
 * API request DTO for assigning a location to a stock item.
 */
public class AssignLocationToStockCommandDTO {
    @NotBlank(message = "Location ID is required")
    private String locationId;

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be positive")
    private Integer quantity;

    public AssignLocationToStockCommandDTO() {
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

