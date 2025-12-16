package com.ccbsa.wms.notification.application.service.command.dto;

import com.ccbsa.common.domain.valueobject.EmailAddress;
import com.ccbsa.common.domain.valueobject.Message;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.common.domain.valueobject.Title;
import com.ccbsa.common.domain.valueobject.UserId;
import com.ccbsa.wms.notification.domain.core.valueobject.NotificationType;

/**
 * Command DTO: CreateNotificationCommand
 * <p>
 * Command for creating a new notification.
 */
public final class CreateNotificationCommand {
    private final TenantId tenantId;
    private final UserId recipientUserId;
    private final EmailAddress recipientEmailAddress; // Optional: from event payload
    private final Title title;
    private final Message message;
    private final NotificationType type;

    private CreateNotificationCommand(Builder builder) {
        this.tenantId = builder.tenantId;
        this.recipientUserId = builder.recipientUserId;
        this.recipientEmailAddress = builder.recipientEmailAddress;
        this.title = builder.title;
        this.message = builder.message;
        this.type = builder.type;
    }

    public static Builder builder() {
        return new Builder();
    }

    public TenantId getTenantId() {
        return tenantId;
    }

    public UserId getRecipientUserId() {
        return recipientUserId;
    }

    public EmailAddress getRecipientEmail() {
        return recipientEmailAddress;
    }

    public Title getTitle() {
        return title;
    }

    public Message getMessage() {
        return message;
    }

    public NotificationType getType() {
        return type;
    }

    public static class Builder {
        private TenantId tenantId;
        private UserId recipientUserId;
        private EmailAddress recipientEmailAddress;
        private Title title;
        private Message message;
        private NotificationType type;

        public Builder tenantId(TenantId tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public Builder recipientUserId(UserId recipientUserId) {
            this.recipientUserId = recipientUserId;
            return this;
        }

        public Builder recipientEmail(EmailAddress recipientEmailAddress) {
            this.recipientEmailAddress = recipientEmailAddress;
            return this;
        }

        public Builder title(Title title) {
            this.title = title;
            return this;
        }

        public Builder message(Message message) {
            this.message = message;
            return this;
        }

        public Builder type(NotificationType type) {
            this.type = type;
            return this;
        }

        public CreateNotificationCommand build() {
            return new CreateNotificationCommand(this);
        }
    }
}

