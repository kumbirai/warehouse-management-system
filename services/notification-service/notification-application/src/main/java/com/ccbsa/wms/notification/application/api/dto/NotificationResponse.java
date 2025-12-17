package com.ccbsa.wms.notification.application.api.dto;

import java.time.LocalDateTime;

import com.ccbsa.wms.notification.domain.core.valueobject.NotificationStatus;
import com.ccbsa.wms.notification.domain.core.valueobject.NotificationType;

/**
 * Response DTO: NotificationResponse
 * <p>
 * Response DTO for notification details.
 */
public final class NotificationResponse {
    private final String notificationId;
    private final String tenantId;
    private final String recipientUserId;
    private final String title;
    private final String message;
    private final NotificationType type;
    private final NotificationStatus status;
    private final LocalDateTime createdAt;
    private final LocalDateTime lastModifiedAt;
    private final LocalDateTime sentAt;
    private final LocalDateTime readAt;

    public NotificationResponse(String notificationId, String tenantId, String recipientUserId, String title, String message, NotificationType type, NotificationStatus status,
                                LocalDateTime createdAt, LocalDateTime lastModifiedAt,
                                LocalDateTime sentAt, LocalDateTime readAt) {
        this.notificationId = notificationId;
        this.tenantId = tenantId;
        this.recipientUserId = recipientUserId;
        this.title = title;
        this.message = message;
        this.type = type;
        this.status = status;
        this.createdAt = createdAt;
        this.lastModifiedAt = lastModifiedAt;
        this.sentAt = sentAt;
        this.readAt = readAt;
    }

    public String getNotificationId() {
        return notificationId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getRecipientUserId() {
        return recipientUserId;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public NotificationType getType() {
        return type;
    }

    public NotificationStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getLastModifiedAt() {
        return lastModifiedAt;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public LocalDateTime getReadAt() {
        return readAt;
    }
}

