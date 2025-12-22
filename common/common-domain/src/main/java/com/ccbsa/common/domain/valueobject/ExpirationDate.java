package com.ccbsa.common.domain.valueobject;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * Value Object: ExpirationDate
 * <p>
 * Represents product expiration date with validation and utility methods.
 * <p>
 * This value object is shared across services (DRY principle).
 * <p>
 * Business Rules:
 * - ExpirationDate cannot be null (use Optional or nullable methods for non-perishable items)
 * - Expiration date must be a valid date
 * - Provides utility methods for expiration checks
 */
public final class ExpirationDate {
    private final LocalDate value;

    private ExpirationDate(LocalDate value) {
        if (value == null) {
            throw new IllegalArgumentException("ExpirationDate cannot be null");
        }
        this.value = value;
    }

    /**
     * Creates an ExpirationDate from a LocalDate value.
     *
     * @param value LocalDate value (must not be null)
     * @return ExpirationDate instance
     * @throws IllegalArgumentException if value is null
     */
    public static ExpirationDate of(LocalDate value) {
        return new ExpirationDate(value);
    }

    /**
     * Returns the LocalDate value.
     *
     * @return LocalDate value
     */
    public LocalDate getValue() {
        return value;
    }

    /**
     * Checks if the expiration date is within the specified number of days.
     *
     * @param days Number of days to check
     * @return true if expiration date is within the specified days (and not expired)
     */
    public boolean isExpiringWithinDays(int days) {
        LocalDate threshold = LocalDate.now().plusDays(days);
        return !value.isAfter(threshold) && !isExpired();
    }

    /**
     * Checks if the expiration date is in the past (expired).
     *
     * @return true if expiration date is before today
     */
    public boolean isExpired() {
        return value.isBefore(LocalDate.now());
    }

    /**
     * Calculates the number of days until expiration.
     * <p>
     * Returns negative value if expired, 0 if expiring today, positive if in the future.
     *
     * @return Number of days until expiration
     */
    public long daysUntilExpiration() {
        return ChronoUnit.DAYS.between(LocalDate.now(), value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ExpirationDate that = (ExpirationDate) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}

