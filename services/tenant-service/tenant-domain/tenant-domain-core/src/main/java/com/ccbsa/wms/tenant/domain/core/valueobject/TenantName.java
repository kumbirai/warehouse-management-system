package com.ccbsa.wms.tenant.domain.core.valueobject;

import java.util.Objects;

/**
 * Value Object: TenantName
 * <p>
 * Represents the name of a tenant (LDP).
 * Immutable and validated on construction.
 */
public final class TenantName {
    private final String value;

    private TenantName(String value) {
        if (value == null || value.trim()
                .isEmpty()) {
            throw new IllegalArgumentException("Tenant name cannot be null or empty");
        }
        if (value.length() > 200) {
            throw new IllegalArgumentException("Tenant name cannot exceed 200 characters");
        }
        this.value = value.trim();
    }

    public static TenantName of(String value) {
        return new TenantName(value);
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
        TenantName that = (TenantName) o;
        return Objects.equals(value,
                that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return String.format("TenantName{value='%s'}",
                value);
    }
}

