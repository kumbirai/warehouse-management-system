package com.ccbsa.wms.location.application.dto.query;

import java.math.BigDecimal;

/**
 * DTO: LocationCapacityDTO
 * <p>
 * Nested DTO for location capacity information.
 */
public final class LocationCapacityDTO {
    private BigDecimal currentQuantity;
    private BigDecimal maximumQuantity;

    public LocationCapacityDTO() {
    }

    public LocationCapacityDTO(BigDecimal currentQuantity, BigDecimal maximumQuantity) {
        this.currentQuantity = currentQuantity;
        this.maximumQuantity = maximumQuantity;
    }

    public BigDecimal getCurrentQuantity() {
        return currentQuantity;
    }

    public void setCurrentQuantity(BigDecimal currentQuantity) {
        this.currentQuantity = currentQuantity;
    }

    public BigDecimal getMaximumQuantity() {
        return maximumQuantity;
    }

    public void setMaximumQuantity(BigDecimal maximumQuantity) {
        this.maximumQuantity = maximumQuantity;
    }
}

