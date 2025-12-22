package com.ccbsa.wms.location.application.service.query.dto;

import java.math.BigDecimal;

/**
 * Query Result DTO: LocationAvailabilityResult
 * <p>
 * Result object for location availability check.
 */
public final class LocationAvailabilityResult {
    private final boolean available;
    private final boolean hasCapacity;
    private final BigDecimal availableCapacity;
    private final String reason;

    private LocationAvailabilityResult(Builder builder) {
        this.available = builder.available;
        this.hasCapacity = builder.hasCapacity;
        this.availableCapacity = builder.availableCapacity;
        this.reason = builder.reason;
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean isAvailable() {
        return available;
    }

    public boolean hasCapacity() {
        return hasCapacity;
    }

    public BigDecimal getAvailableCapacity() {
        return availableCapacity;
    }

    public String getReason() {
        return reason;
    }

    public static class Builder {
        private boolean available;
        private boolean hasCapacity;
        private BigDecimal availableCapacity;
        private String reason;

        public Builder available(boolean available) {
            this.available = available;
            return this;
        }

        public Builder hasCapacity(boolean hasCapacity) {
            this.hasCapacity = hasCapacity;
            return this;
        }

        public Builder availableCapacity(BigDecimal availableCapacity) {
            this.availableCapacity = availableCapacity;
            return this;
        }

        public Builder reason(String reason) {
            this.reason = reason;
            return this;
        }

        public LocationAvailabilityResult build() {
            return new LocationAvailabilityResult(this);
        }
    }
}

