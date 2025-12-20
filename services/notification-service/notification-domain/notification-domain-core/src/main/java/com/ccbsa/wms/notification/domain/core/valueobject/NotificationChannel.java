package com.ccbsa.wms.notification.domain.core.valueobject;

/**
 * Value Object: NotificationChannel
 * <p>
 * Represents the delivery channel for notifications. Immutable enum-like value object.
 */
public enum NotificationChannel {
    EMAIL("EmailAddress delivery channel"), SMS("SMS delivery channel"), WHATSAPP("WhatsApp delivery channel");

    private final String description;

    NotificationChannel(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}

