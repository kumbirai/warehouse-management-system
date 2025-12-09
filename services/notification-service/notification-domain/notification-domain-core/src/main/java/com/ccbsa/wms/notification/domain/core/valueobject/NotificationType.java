package com.ccbsa.wms.notification.domain.core.valueobject;

/**
 * Value Object: NotificationType
 * <p>
 * Represents the type of notification.
 * Immutable enum-like value object.
 */
public enum NotificationType {
    USER_CREATED("User account created"),
    USER_UPDATED("User profile updated"),
    USER_DEACTIVATED("User account deactivated"),
    USER_ACTIVATED("User account activated"),
    USER_SUSPENDED("User account suspended"),
    USER_ROLE_ASSIGNED("Role assigned to user"),
    USER_ROLE_REMOVED("Role removed from user"),
    TENANT_CREATED("Tenant created"),
    TENANT_ACTIVATED("Tenant activated"),
    TENANT_DEACTIVATED("Tenant deactivated"),
    TENANT_SUSPENDED("Tenant suspended"),
    WELCOME("Welcome notification"),
    SYSTEM_ALERT("System alert");

    private final String description;

    NotificationType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}

