package com.ccbsa.common.domain.valueobject;

import java.util.Objects;

/**
 * Value object representing a tenant identifier (LDP identifier). Tenant IDs are immutable and validated.
 */
public final class TenantId {
    private final String value;

    private TenantId(String value) {
        if (value == null || value.trim()
                .isEmpty()) {
            throw new IllegalArgumentException("Tenant ID cannot be null or empty");
        }
        if (value.length() > 50) {
            throw new IllegalArgumentException("Tenant ID cannot exceed 50 characters");
        }
        this.value = value.trim();
    }

    public static TenantId of(String value) {
        return new TenantId(value);
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TenantId tenantId = (TenantId) o;
        return Objects.equals(value, tenantId.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}

