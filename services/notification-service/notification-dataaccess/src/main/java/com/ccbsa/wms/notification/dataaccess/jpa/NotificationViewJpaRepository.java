package com.ccbsa.wms.notification.dataaccess.jpa;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ccbsa.wms.notification.dataaccess.entity.NotificationViewEntity;
import com.ccbsa.wms.notification.domain.core.valueobject.NotificationStatus;
import com.ccbsa.wms.notification.domain.core.valueobject.NotificationType;

/**
 * JPA Repository: NotificationViewJpaRepository
 * <p>
 * Spring Data JPA repository for NotificationViewEntity read model queries.
 * <p>
 * Provides optimized read-only queries for notification views.
 */
@Repository
public interface NotificationViewJpaRepository extends JpaRepository<NotificationViewEntity, UUID> {

    /**
     * Finds a notification view by tenant ID and notification ID.
     *
     * @param tenantId       Tenant ID
     * @param notificationId Notification ID
     * @return Optional NotificationViewEntity
     */
    Optional<NotificationViewEntity> findByTenantIdAndId(String tenantId, UUID notificationId);

    /**
     * Finds notification views by tenant ID and recipient user ID.
     *
     * @param tenantId        Tenant ID
     * @param recipientUserId Recipient user ID
     * @return List of NotificationViewEntity
     */
    List<NotificationViewEntity> findByTenantIdAndRecipientUserId(String tenantId, String recipientUserId);

    /**
     * Finds notification views by tenant ID, recipient user ID, and status.
     *
     * @param tenantId        Tenant ID
     * @param recipientUserId Recipient user ID
     * @param status          Notification status
     * @return List of NotificationViewEntity
     */
    List<NotificationViewEntity> findByTenantIdAndRecipientUserIdAndStatus(String tenantId, String recipientUserId, NotificationStatus status);

    /**
     * Finds notification views by tenant ID and type.
     *
     * @param tenantId Tenant ID
     * @param type     Notification type
     * @return List of NotificationViewEntity
     */
    List<NotificationViewEntity> findByTenantIdAndType(String tenantId, NotificationType type);
}

