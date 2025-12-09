package com.ccbsa.wms.notification.application.service.query.dto;

import com.ccbsa.wms.notification.domain.core.valueobject.NotificationId;

/**
 * Query DTO: GetNotificationQuery
 * <p>
 * Query for retrieving a single notification by ID.
 */
public final class GetNotificationQuery {
    private final NotificationId notificationId;

    public GetNotificationQuery(NotificationId notificationId) {
        if (notificationId == null) {
            throw new IllegalArgumentException("NotificationId cannot be null");
        }
        this.notificationId = notificationId;
    }

    public NotificationId getNotificationId() {
        return notificationId;
    }
}

