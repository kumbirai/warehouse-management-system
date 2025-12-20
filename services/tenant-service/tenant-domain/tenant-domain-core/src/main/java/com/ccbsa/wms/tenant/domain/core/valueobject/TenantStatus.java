package com.ccbsa.wms.tenant.domain.core.valueobject;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Value Object: TenantStatus
 * <p>
 * Represents the lifecycle status of a tenant. Immutable enum representing tenant states.
 */
public enum TenantStatus {
    /**
     * Tenant created but not yet activated. Cannot access the system.
     */
    PENDING,

    /**
     * Tenant is active and operational. Can access the system.
     */
    ACTIVE,

    /**
     * Tenant is deactivated. Cannot access the system.
     */
    INACTIVE,

    /**
     * Tenant is temporarily suspended. Cannot access the system.
     */
    SUSPENDED;

    /**
     * Checks if tenant can access the system.
     *
     * @return true if tenant status allows system access
     */
    public boolean canAccessSystem() {
        return this == ACTIVE;
    }

    /**
     * Checks if tenant status transition is valid.
     *
     * @param newStatus Target status
     * @return true if transition is valid
     */
    @SuppressFBWarnings(value = "DB_DUPLICATE_SWITCH_CLAUSES", justification = "Enum switch intentionally allows overlapping transitions for clarity")
    public boolean canTransitionTo(TenantStatus newStatus) {
        if (newStatus == null) {
            return false;
        }

        return switch (this) {
            case PENDING -> newStatus == ACTIVE;
            case ACTIVE -> newStatus == INACTIVE || newStatus == SUSPENDED;
            case SUSPENDED -> newStatus != SUSPENDED && newStatus != PENDING;
            case INACTIVE -> newStatus == ACTIVE;
        };
    }
}

