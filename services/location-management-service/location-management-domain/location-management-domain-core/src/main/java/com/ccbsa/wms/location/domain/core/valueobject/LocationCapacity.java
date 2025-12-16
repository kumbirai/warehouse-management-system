package com.ccbsa.wms.location.domain.core.valueobject;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Value Object: LocationCapacity
 *
 * Represents the capacity information for a warehouse location.
 * Immutable and self-validating.
 *
 * Capacity consists of:
 * - currentQuantity: Current quantity stored in the location
 * - maximumQuantity: Maximum quantity the location can hold (optional)
 *
 * Business Rules:
 * - currentQuantity cannot exceed maximumQuantity if maximumQuantity is set
 * - currentQuantity cannot be negative
 * - maximumQuantity must be positive if set
 */
public final class LocationCapacity {
    private final BigDecimal currentQuantity;
    private final BigDecimal maximumQuantity;

    /**
     * Private constructor to enforce immutability.
     *
     * @param currentQuantity Current quantity in location
     * @param maximumQuantity Maximum quantity capacity (can be null)
     * @throws IllegalArgumentException if validation fails
     */
    private LocationCapacity(BigDecimal currentQuantity, BigDecimal maximumQuantity) {
        validate(currentQuantity, maximumQuantity);
        this.currentQuantity = currentQuantity;
        this.maximumQuantity = maximumQuantity;
    }

    /**
     * Validates the capacity values according to business rules.
     *
     * @param currentQuantity Current quantity
     * @param maximumQuantity Maximum quantity
     * @throws IllegalArgumentException if validation fails
     */
    private void validate(BigDecimal currentQuantity, BigDecimal maximumQuantity) {
        if (currentQuantity == null) {
            throw new IllegalArgumentException("CurrentQuantity cannot be null");
        }
        if (currentQuantity.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("CurrentQuantity cannot be negative");
        }

        if (maximumQuantity != null) {
            if (maximumQuantity.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("MaximumQuantity must be positive if set");
            }
            if (currentQuantity.compareTo(maximumQuantity) > 0) {
                throw new IllegalArgumentException(
                        String.format("CurrentQuantity (%s) cannot exceed MaximumQuantity (%s)",
                                currentQuantity, maximumQuantity)
                );
            }
        }
    }

    /**
     * Factory method to create LocationCapacity with current and maximum quantities.
     *
     * @param currentQuantity Current quantity in location
     * @param maximumQuantity Maximum quantity capacity
     * @return LocationCapacity instance
     * @throws IllegalArgumentException if validation fails
     */
    public static LocationCapacity of(BigDecimal currentQuantity, BigDecimal maximumQuantity) {
        return new LocationCapacity(currentQuantity, maximumQuantity);
    }

    /**
     * Factory method to create empty LocationCapacity (no current quantity, no maximum).
     *
     * @return Empty LocationCapacity instance
     */
    public static LocationCapacity empty() {
        return new LocationCapacity(BigDecimal.ZERO, null);
    }

    /**
     * Factory method to create LocationCapacity with only current quantity.
     *
     * @param currentQuantity Current quantity in location
     * @return LocationCapacity instance
     * @throws IllegalArgumentException if validation fails
     */
    public static LocationCapacity withCurrentQuantity(BigDecimal currentQuantity) {
        return new LocationCapacity(currentQuantity, null);
    }

    /**
     * Returns the current quantity.
     *
     * @return Current quantity
     */
    public BigDecimal getCurrentQuantity() {
        return currentQuantity;
    }

    /**
     * Returns the maximum quantity (can be null if unlimited).
     *
     * @return Maximum quantity, or null if unlimited
     */
    public BigDecimal getMaximumQuantity() {
        return maximumQuantity;
    }

    /**
     * Business logic method: Checks if location can accommodate additional quantity.
     *
     * @param additionalQuantity Quantity to add
     * @return true if location can accommodate the additional quantity
     */
    public boolean canAccommodate(BigDecimal additionalQuantity) {
        if (additionalQuantity == null || additionalQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }

        if (maximumQuantity == null) {
            return true; // Unlimited capacity
        }

        BigDecimal newTotal = currentQuantity.add(additionalQuantity);
        return newTotal.compareTo(maximumQuantity) <= 0;
    }

    /**
     * Business logic method: Calculates available capacity.
     *
     * @return Available capacity, or null if unlimited
     */
    public BigDecimal getAvailableCapacity() {
        if (maximumQuantity == null) {
            return null; // Unlimited
        }
        return maximumQuantity.subtract(currentQuantity);
    }

    /**
     * Business logic method: Checks if location is at full capacity.
     *
     * @return true if location is at full capacity
     */
    public boolean isFull() {
        if (maximumQuantity == null) {
            return false; // Unlimited capacity, never full
        }
        return currentQuantity.compareTo(maximumQuantity) >= 0;
    }

    /**
     * Business logic method: Checks if location is empty.
     *
     * @return true if location has no current quantity
     */
    public boolean isEmpty() {
        return currentQuantity.compareTo(BigDecimal.ZERO) == 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        LocationCapacity that = (LocationCapacity) o;
        return Objects.equals(currentQuantity, that.currentQuantity) &&
                Objects.equals(maximumQuantity, that.maximumQuantity);
    }

    @Override
    public int hashCode() {
        return Objects.hash(currentQuantity, maximumQuantity);
    }

    @Override
    public String toString() {
        if (maximumQuantity == null) {
            return String.format("LocationCapacity{current=%s, maximum=unlimited}", currentQuantity);
        }
        return String.format("LocationCapacity{current=%s, maximum=%s}", currentQuantity, maximumQuantity);
    }
}

