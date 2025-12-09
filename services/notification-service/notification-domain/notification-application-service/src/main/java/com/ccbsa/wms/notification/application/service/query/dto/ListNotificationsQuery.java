package com.ccbsa.wms.notification.application.service.query.dto;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.common.domain.valueobject.UserId;
import com.ccbsa.wms.notification.domain.core.valueobject.NotificationStatus;
import com.ccbsa.wms.notification.domain.core.valueobject.NotificationType;

/**
 * Query DTO: ListNotificationsQuery
 * <p>
 * Query for listing notifications with optional filters.
 */
public final class ListNotificationsQuery {
    private final TenantId tenantId;
    private final UserId recipientUserId;
    private final NotificationStatus status;
    private final NotificationType type;
    private final Integer page;
    private final Integer size;

    private ListNotificationsQuery(Builder builder) {
        this.tenantId = builder.tenantId;
        this.recipientUserId = builder.recipientUserId;
        this.status = builder.status;
        this.type = builder.type;
        this.page = builder.page;
        this.size = builder.size;
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

    public NotificationStatus getStatus() {
        return status;
    }

    public NotificationType getType() {
        return type;
    }

    public Integer getPage() {
        return page;
    }

    public Integer getSize() {
        return size;
    }

    public static class Builder {
        private TenantId tenantId;
        private UserId recipientUserId;
        private NotificationStatus status;
        private NotificationType type;
        private Integer page;
        private Integer size;

        public Builder tenantId(TenantId tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public Builder recipientUserId(UserId recipientUserId) {
            this.recipientUserId = recipientUserId;
            return this;
        }

        public Builder status(NotificationStatus status) {
            this.status = status;
            return this;
        }

        public Builder type(NotificationType type) {
            this.type = type;
            return this;
        }

        public Builder page(Integer page) {
            this.page = page;
            return this;
        }

        public Builder size(Integer size) {
            this.size = size;
            return this;
        }

        public ListNotificationsQuery build() {
            return new ListNotificationsQuery(this);
        }
    }
}

