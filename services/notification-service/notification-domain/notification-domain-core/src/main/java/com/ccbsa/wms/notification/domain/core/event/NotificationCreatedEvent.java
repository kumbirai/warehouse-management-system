package com.ccbsa.wms.notification.domain.core.event;

import com.ccbsa.common.domain.EventMetadata;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.notification.domain.core.valueobject.NotificationId;
import com.ccbsa.wms.notification.domain.core.valueobject.NotificationType;

/**
 * Domain Event: NotificationCreatedEvent
 * <p>
 * Published when a new notification is created.
 * <p>
 * Event Version: 1.0
 */
public final class NotificationCreatedEvent
        extends NotificationEvent {
    private final NotificationType type;
    private final TenantId tenantId;

    /**
     * Constructor for NotificationCreatedEvent without metadata.
     *
     * @param notificationId Notification identifier
     * @param tenantId       Tenant identifier
     * @param type           Notification type
     * @throws IllegalArgumentException if type or tenantId is null
     */
    public NotificationCreatedEvent(NotificationId notificationId, TenantId tenantId, NotificationType type) {
        super(notificationId);
        if (tenantId == null) {
            throw new IllegalArgumentException("TenantId cannot be null");
        }
        if (type == null) {
            throw new IllegalArgumentException("NotificationType cannot be null");
        }
        this.tenantId = tenantId;
        this.type = type;
    }

    /**
     * Constructor for NotificationCreatedEvent with metadata.
     *
     * @param notificationId Notification identifier
     * @param tenantId       Tenant identifier
     * @param type           Notification type
     * @param metadata       Event metadata for traceability
     * @throws IllegalArgumentException if type or tenantId is null
     */
    public NotificationCreatedEvent(NotificationId notificationId, TenantId tenantId, NotificationType type, EventMetadata metadata) {
        super(notificationId, metadata);
        if (tenantId == null) {
            throw new IllegalArgumentException("TenantId cannot be null");
        }
        if (type == null) {
            throw new IllegalArgumentException("NotificationType cannot be null");
        }
        this.tenantId = tenantId;
        this.type = type;
    }

    public NotificationType getType() {
        return type;
    }

    public TenantId getTenantId() {
        return tenantId;
    }
}

