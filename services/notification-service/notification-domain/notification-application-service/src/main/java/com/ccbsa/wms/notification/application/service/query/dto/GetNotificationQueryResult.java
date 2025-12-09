package com.ccbsa.wms.notification.application.service.query.dto;

import java.time.LocalDateTime;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.common.domain.valueobject.UserId;
import com.ccbsa.wms.notification.domain.core.valueobject.NotificationId;
import com.ccbsa.wms.notification.domain.core.valueobject.NotificationStatus;
import com.ccbsa.wms.notification.domain.core.valueobject.NotificationType;

/**
 * Result DTO: GetNotificationQueryResult
 * <p>
 * Result of getting a notification by ID.
 */
public final class GetNotificationQueryResult {
    private final NotificationId notificationId;
    private final TenantId tenantId;
    private final UserId recipientUserId;
    private final String title;
    private final String message;
    private final NotificationType type;
    private final NotificationStatus status;
    private final LocalDateTime createdAt;
    private final LocalDateTime lastModifiedAt;
    private final LocalDateTime sentAt;
    private final LocalDateTime readAt;

    private GetNotificationQueryResult(Builder builder) {
        this.notificationId = builder.notificationId;
        this.tenantId = builder.tenantId;
        this.recipientUserId = builder.recipientUserId;
        this.title = builder.title;
        this.message = builder.message;
        this.type = builder.type;
        this.status = builder.status;
        this.createdAt = builder.createdAt;
        this.lastModifiedAt = builder.lastModifiedAt;
        this.sentAt = builder.sentAt;
        this.readAt = builder.readAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public NotificationId getNotificationId() {
        return notificationId;
    }

    public TenantId getTenantId() {
        return tenantId;
    }

    public UserId getRecipientUserId() {
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

    public static class Builder {
        private NotificationId notificationId;
        private TenantId tenantId;
        private UserId recipientUserId;
        private String title;
        private String message;
        private NotificationType type;
        private NotificationStatus status;
        private LocalDateTime createdAt;
        private LocalDateTime lastModifiedAt;
        private LocalDateTime sentAt;
        private LocalDateTime readAt;

        public Builder notificationId(NotificationId notificationId) {
            this.notificationId = notificationId;
            return this;
        }

        public Builder tenantId(TenantId tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public Builder recipientUserId(UserId recipientUserId) {
            this.recipientUserId = recipientUserId;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder type(NotificationType type) {
            this.type = type;
            return this;
        }

        public Builder status(NotificationStatus status) {
            this.status = status;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder lastModifiedAt(LocalDateTime lastModifiedAt) {
            this.lastModifiedAt = lastModifiedAt;
            return this;
        }

        public Builder sentAt(LocalDateTime sentAt) {
            this.sentAt = sentAt;
            return this;
        }

        public Builder readAt(LocalDateTime readAt) {
            this.readAt = readAt;
            return this;
        }

        public GetNotificationQueryResult build() {
            return new GetNotificationQueryResult(this);
        }
    }
}

