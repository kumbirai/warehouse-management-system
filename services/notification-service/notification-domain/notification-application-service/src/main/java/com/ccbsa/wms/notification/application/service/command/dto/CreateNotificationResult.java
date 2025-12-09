package com.ccbsa.wms.notification.application.service.command.dto;

import java.time.LocalDateTime;

import com.ccbsa.wms.notification.domain.core.valueobject.NotificationId;
import com.ccbsa.wms.notification.domain.core.valueobject.NotificationStatus;

/**
 * Result DTO: CreateNotificationResult
 * <p>
 * Result of creating a notification.
 */
public final class CreateNotificationResult {
    private final NotificationId notificationId;
    private final NotificationStatus status;
    private final LocalDateTime createdAt;

    private CreateNotificationResult(Builder builder) {
        this.notificationId = builder.notificationId;
        this.status = builder.status;
        this.createdAt = builder.createdAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public NotificationId getNotificationId() {
        return notificationId;
    }

    public NotificationStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public static class Builder {
        private NotificationId notificationId;
        private NotificationStatus status;
        private LocalDateTime createdAt;

        public Builder notificationId(NotificationId notificationId) {
            this.notificationId = notificationId;
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

        public CreateNotificationResult build() {
            return new CreateNotificationResult(this);
        }
    }
}

