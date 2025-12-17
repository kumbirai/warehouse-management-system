package com.ccbsa.wms.notification.domain.core.event;

import com.ccbsa.common.domain.DomainEvent;
import com.ccbsa.common.domain.EventMetadata;
import com.ccbsa.wms.notification.domain.core.valueobject.NotificationId;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Base class for all Notification domain events.
 * <p>
 * All notification-specific events extend this class.
 */
public abstract class NotificationEvent
        extends DomainEvent<NotificationId> {
    /**
     * Constructor for Notification events without metadata.
     *
     * @param aggregateId Aggregate identifier (NotificationId)
     * @throws IllegalArgumentException if aggregateId is null
     */
    @SuppressFBWarnings(value = "CT_CONSTRUCTOR_THROW",
            justification = "Notification events must fail fast if aggregate identifiers are invalid")
    protected NotificationEvent(NotificationId aggregateId) {
        super(extractNotificationIdString(aggregateId), "Notification");
    }

    /**
     * Extracts the NotificationId string value from the aggregate identifier.
     *
     * @param notificationId Notification identifier
     * @return String representation of notification ID
     */
    private static String extractNotificationIdString(NotificationId notificationId) {
        if (notificationId == null) {
            throw new IllegalArgumentException("NotificationId cannot be null");
        }
        return notificationId.getValue()
                .toString();
    }

    /**
     * Constructor for Notification events with metadata.
     *
     * @param aggregateId Aggregate identifier (NotificationId)
     * @param metadata    Event metadata for traceability
     * @throws IllegalArgumentException if aggregateId is null
     */
    @SuppressFBWarnings(value = "CT_CONSTRUCTOR_THROW",
            justification = "Notification events must fail fast if aggregate identifiers are invalid")
    protected NotificationEvent(NotificationId aggregateId, EventMetadata metadata) {
        super(extractNotificationIdString(aggregateId), "Notification", metadata);
    }
}

