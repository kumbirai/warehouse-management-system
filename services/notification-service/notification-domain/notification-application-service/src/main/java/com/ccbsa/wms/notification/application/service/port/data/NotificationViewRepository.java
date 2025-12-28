package com.ccbsa.wms.notification.application.service.port.data;

import java.util.List;
import java.util.Optional;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.common.domain.valueobject.UserId;
import com.ccbsa.wms.notification.application.service.port.data.dto.NotificationView;
import com.ccbsa.wms.notification.domain.core.valueobject.NotificationId;
import com.ccbsa.wms.notification.domain.core.valueobject.NotificationStatus;
import com.ccbsa.wms.notification.domain.core.valueobject.NotificationType;

/**
 * Data Port: NotificationViewRepository
 * <p>
 * Read model repository for notification queries. Provides optimized read access to notification data.
 * <p>
 * This is a data port (read model) used by query handlers, not a repository port (write model).
 */
public interface NotificationViewRepository {

    /**
     * Finds a notification view by notification ID.
     *
     * @param notificationId Notification ID
     * @return Optional NotificationView
     */
    Optional<NotificationView> findById(NotificationId notificationId);

    /**
     * Finds notification views by recipient user ID.
     *
     * @param tenantId        Tenant ID
     * @param recipientUserId Recipient user ID
     * @return List of NotificationView
     */
    List<NotificationView> findByRecipientUserId(TenantId tenantId, UserId recipientUserId);

    /**
     * Finds notification views by recipient user ID and status.
     *
     * @param tenantId        Tenant ID
     * @param recipientUserId Recipient user ID
     * @param status          Notification status
     * @return List of NotificationView
     */
    List<NotificationView> findByRecipientUserIdAndStatus(TenantId tenantId, UserId recipientUserId, NotificationStatus status);

    /**
     * Finds notification views by type.
     *
     * @param tenantId Tenant ID
     * @param type     Notification type
     * @return List of NotificationView
     */
    List<NotificationView> findByType(TenantId tenantId, NotificationType type);
}

