package com.ccbsa.wms.notification.application.service.command.dto;

import com.ccbsa.wms.notification.domain.core.valueobject.NotificationChannel;
import com.ccbsa.wms.notification.domain.core.valueobject.NotificationId;

/**
 * Command DTO: SendNotificationCommand
 * <p>
 * Command for sending a notification via a specific delivery channel.
 */
public final class SendNotificationCommand {
    private final NotificationId notificationId;
    private final NotificationChannel channel;

    private SendNotificationCommand(Builder builder) {
        this.notificationId = builder.notificationId;
        this.channel = builder.channel;
    }

    public static Builder builder() {
        return new Builder();
    }

    public NotificationId getNotificationId() {
        return notificationId;
    }

    public NotificationChannel getChannel() {
        return channel;
    }

    public static class Builder {
        private NotificationId notificationId;
        private NotificationChannel channel;

        public Builder notificationId(NotificationId notificationId) {
            this.notificationId = notificationId;
            return this;
        }

        public Builder channel(NotificationChannel channel) {
            this.channel = channel;
            return this;
        }

        public SendNotificationCommand build() {
            if (notificationId == null) {
                throw new IllegalArgumentException("NotificationId is required");
            }
            if (channel == null) {
                throw new IllegalArgumentException("NotificationChannel is required");
            }
            return new SendNotificationCommand(this);
        }
    }
}

