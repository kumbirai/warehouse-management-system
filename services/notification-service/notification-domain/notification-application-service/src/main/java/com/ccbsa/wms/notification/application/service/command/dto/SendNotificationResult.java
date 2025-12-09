package com.ccbsa.wms.notification.application.service.command.dto;

import com.ccbsa.wms.notification.domain.core.valueobject.NotificationStatus;

/**
 * Result DTO: SendNotificationResult
 * <p>
 * Result of sending a notification via a delivery channel.
 */
public final class SendNotificationResult {
    private final boolean success;
    private final String externalId;
    private final NotificationStatus status;

    private SendNotificationResult(Builder builder) {
        this.success = builder.success;
        this.externalId = builder.externalId;
        this.status = builder.status;
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean isSuccess() {
        return success;
    }

    public String getExternalId() {
        return externalId;
    }

    public NotificationStatus getStatus() {
        return status;
    }

    public static class Builder {
        private boolean success;
        private String externalId;
        private NotificationStatus status;

        public Builder success(boolean success) {
            this.success = success;
            return this;
        }

        public Builder externalId(String externalId) {
            this.externalId = externalId;
            return this;
        }

        public Builder status(NotificationStatus status) {
            this.status = status;
            return this;
        }

        public SendNotificationResult build() {
            return new SendNotificationResult(this);
        }
    }
}

