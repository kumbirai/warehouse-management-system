package com.ccbsa.wms.user.domain.core.valueobject;

/**
 * Value Object: UserStatus
 * <p>
 * Represents the status of a user account. Immutable enum-like value object.
 */
public enum UserStatus {
    ACTIVE,
    INACTIVE,
    SUSPENDED;

    /**
     * Checks if a status transition is valid.
     *
     * @param targetStatus Target status
     * @return true if transition is valid
     */
    public boolean canTransitionTo(UserStatus targetStatus) {
        if (targetStatus == null) {
            return false;
        }
        if (this == targetStatus) {
            return true; // Same status is always valid
        }

        // Define valid transitions
        switch (this) {
            case ACTIVE:
                return targetStatus == INACTIVE || targetStatus == SUSPENDED;
            case INACTIVE:
                return targetStatus == ACTIVE;
            case SUSPENDED:
                return targetStatus == ACTIVE || targetStatus == INACTIVE;
            default:
                return false;
        }
    }
}

