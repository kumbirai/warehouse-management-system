package com.ccbsa.wms.notification.application.service.port.repository;

import java.util.List;
import java.util.Optional;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.common.domain.valueobject.UserId;
import com.ccbsa.wms.notification.domain.core.entity.Notification;
import com.ccbsa.wms.notification.domain.core.valueobject.NotificationId;
import com.ccbsa.wms.notification.domain.core.valueobject.NotificationStatus;
import com.ccbsa.wms.notification.domain.core.valueobject.NotificationType;

/**
 * Repository Port: NotificationRepository
 * <p>
 * Defines the contract for Notification aggregate persistence. Implemented by data access adapters.
 */
public interface NotificationRepository {

    /**
     * Saves a notification aggregate.
     *
     * @param notification Notification aggregate to save
     * @return Saved notification aggregate
     */
    Notification save(Notification notification);

    /**
     * Finds a notification by ID.
     *
     * @param notificationId Notification identifier
     * @return Optional notification if found
     */
    Optional<Notification> findById(NotificationId notificationId);

    /**
     * Finds notifications by recipient user ID.
     *
     * @param tenantId        Tenant identifier
     * @param recipientUserId Recipient user identifier
     * @return List of notifications for the user
     */
    List<Notification> findByRecipientUserId(TenantId tenantId, UserId recipientUserId);

    /**
     * Finds notifications by recipient user ID and status.
     *
     * @param tenantId        Tenant identifier
     * @param recipientUserId Recipient user identifier
     * @param status          Notification status
     * @return List of notifications matching criteria
     */
    List<Notification> findByRecipientUserIdAndStatus(TenantId tenantId, UserId recipientUserId, NotificationStatus status);

    /**
     * Finds notifications by type.
     *
     * @param tenantId Tenant identifier
     * @param type     Notification type
     * @return List of notifications matching type
     */
    List<Notification> findByType(TenantId tenantId, NotificationType type);

    /**
     * Counts unread notifications for a user.
     *
     * @param tenantId        Tenant identifier
     * @param recipientUserId Recipient user identifier
     * @return Count of unread notifications
     */
    long countUnreadByRecipientUserId(TenantId tenantId, UserId recipientUserId);
}

