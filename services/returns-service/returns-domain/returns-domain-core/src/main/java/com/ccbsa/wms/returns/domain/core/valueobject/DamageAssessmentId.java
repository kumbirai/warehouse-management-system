package com.ccbsa.wms.returns.domain.core.valueobject;

import java.util.Objects;
import java.util.UUID;

/**
 * Value Object: DamageAssessmentId
 * <p>
 * Represents a unique identifier for a damage assessment. Immutable and self-validating.
 */
public final class DamageAssessmentId {
    private final UUID value;

    private DamageAssessmentId(UUID value) {
        if (value == null) {
            throw new IllegalArgumentException("DamageAssessmentId cannot be null");
        }
        this.value = value;
    }

    /**
     * Factory method to create DamageAssessmentId from UUID.
     *
     * @param value UUID value
     * @return DamageAssessmentId instance
     * @throws IllegalArgumentException if value is null
     */
    public static DamageAssessmentId of(UUID value) {
        return new DamageAssessmentId(value);
    }

    /**
     * Factory method to create DamageAssessmentId from string.
     *
     * @param value UUID string value
     * @return DamageAssessmentId instance
     * @throws IllegalArgumentException if value is null or invalid UUID format
     */
    public static DamageAssessmentId of(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("DamageAssessmentId string cannot be null or empty");
        }
        try {
            return new DamageAssessmentId(UUID.fromString(value.trim()));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(String.format("Invalid UUID format for DamageAssessmentId: %s", value), e);
        }
    }

    /**
     * Creates a new DamageAssessmentId with random UUID.
     *
     * @return New DamageAssessmentId instance
     */
    public static DamageAssessmentId generate() {
        return new DamageAssessmentId(UUID.randomUUID());
    }

    /**
     * Returns the UUID value.
     *
     * @return UUID value
     */
    public UUID getValue() {
        return value;
    }

    /**
     * Returns the UUID value as string.
     *
     * @return UUID string value
     */
    public String getValueAsString() {
        return value.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DamageAssessmentId that = (DamageAssessmentId) o;
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
