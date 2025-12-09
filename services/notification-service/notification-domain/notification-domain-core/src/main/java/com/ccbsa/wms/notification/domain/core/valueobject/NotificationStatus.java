package com.ccbsa.wms.notification.domain.core.valueobject;

/**
 * Value Object: NotificationStatus
 * <p>
 * Represents the status of a notification.
 * Immutable enum-like value object.
 */
public enum NotificationStatus {
    PENDING("Notification is pending delivery"),
    SENT("Notification has been sent"),
    DELIVERED("Notification has been delivered"),
    FAILED("Notification delivery failed"),
    READ("Notification has been read");

    private final String description;

    NotificationStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}

