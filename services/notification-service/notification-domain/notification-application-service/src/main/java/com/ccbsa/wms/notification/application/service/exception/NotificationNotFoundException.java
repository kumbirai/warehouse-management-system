package com.ccbsa.wms.notification.application.service.exception;

/**
 * Exception: NotificationNotFoundException
 * <p>
 * Thrown when a notification is not found.
 */
public class NotificationNotFoundException
        extends RuntimeException {
    private final String notificationId;
    private final String reason;

    public NotificationNotFoundException(String notificationId, String reason) {
        super(String.format("Notification not found: %s (ID: %s)", reason, notificationId));
        this.notificationId = notificationId;
        this.reason = reason;
    }

    public String getNotificationId() {
        return notificationId;
    }

    public String getReason() {
        return reason;
    }
}

