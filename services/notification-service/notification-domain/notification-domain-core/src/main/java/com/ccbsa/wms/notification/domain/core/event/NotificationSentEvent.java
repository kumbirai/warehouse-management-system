package com.ccbsa.wms.notification.domain.core.event;

import java.time.LocalDateTime;

import com.ccbsa.common.domain.EventMetadata;
import com.ccbsa.wms.notification.domain.core.valueobject.NotificationChannel;
import com.ccbsa.wms.notification.domain.core.valueobject.NotificationId;

/**
 * Domain Event: NotificationSentEvent
 * <p>
 * Published when a notification is successfully sent via a delivery channel.
 * <p>
 * Event Version: 1.0
 */
public final class NotificationSentEvent
        extends NotificationEvent {
    private final NotificationChannel channel;
    private final LocalDateTime sentAt;

    /**
     * Constructor for NotificationSentEvent without metadata.
     *
     * @param notificationId Notification identifier
     * @param channel        Delivery channel used
     * @param sentAt         Timestamp when notification was sent
     * @throws IllegalArgumentException if channel is null or sentAt is null
     */
    public NotificationSentEvent(NotificationId notificationId, NotificationChannel channel, LocalDateTime sentAt) {
        super(notificationId);
        if (channel == null) {
            throw new IllegalArgumentException("NotificationChannel cannot be null");
        }
        if (sentAt == null) {
            throw new IllegalArgumentException("sentAt cannot be null");
        }
        this.channel = channel;
        this.sentAt = sentAt;
    }

    /**
     * Constructor for NotificationSentEvent with metadata.
     *
     * @param notificationId Notification identifier
     * @param channel        Delivery channel used
     * @param sentAt         Timestamp when notification was sent
     * @param metadata       Event metadata for traceability
     * @throws IllegalArgumentException if channel is null or sentAt is null
     */
    public NotificationSentEvent(NotificationId notificationId, NotificationChannel channel, LocalDateTime sentAt, EventMetadata metadata) {
        super(notificationId, metadata);
        if (channel == null) {
            throw new IllegalArgumentException("NotificationChannel cannot be null");
        }
        if (sentAt == null) {
            throw new IllegalArgumentException("sentAt cannot be null");
        }
        this.channel = channel;
        this.sentAt = sentAt;
    }

    public NotificationChannel getChannel() {
        return channel;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }
}

