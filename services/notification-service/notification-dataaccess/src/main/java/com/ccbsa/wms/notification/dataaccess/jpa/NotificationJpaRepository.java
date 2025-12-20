package com.ccbsa.wms.notification.dataaccess.jpa;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ccbsa.wms.notification.dataaccess.entity.NotificationEntity;
import com.ccbsa.wms.notification.domain.core.valueobject.NotificationStatus;
import com.ccbsa.wms.notification.domain.core.valueobject.NotificationType;

/**
 * JPA Repository: NotificationJpaRepository
 * <p>
 * Spring Data JPA repository for NotificationEntity.
 */
@Repository
public interface NotificationJpaRepository extends JpaRepository<NotificationEntity, UUID> {

    /**
     * Finds notification by tenant ID and ID.
     *
     * @param tenantId Tenant identifier
     * @param id       Notification identifier
     * @return Optional notification if found
     */
    Optional<NotificationEntity> findByTenantIdAndId(String tenantId, UUID id);

    /**
     * Finds notifications by tenant ID and recipient user ID.
     *
     * @param tenantId        Tenant identifier
     * @param recipientUserId Recipient user identifier
     * @return List of notifications
     */
    List<NotificationEntity> findByTenantIdAndRecipientUserId(String tenantId, String recipientUserId);

    /**
     * Finds notifications by tenant ID, recipient user ID, and status.
     *
     * @param tenantId        Tenant identifier
     * @param recipientUserId Recipient user identifier
     * @param status          Notification status
     * @return List of notifications
     */
    List<NotificationEntity> findByTenantIdAndRecipientUserIdAndStatus(String tenantId, String recipientUserId, NotificationStatus status);

    /**
     * Finds notifications by tenant ID and type.
     *
     * @param tenantId Tenant identifier
     * @param type     Notification type
     * @return List of notifications
     */
    List<NotificationEntity> findByTenantIdAndType(String tenantId, NotificationType type);

    /**
     * Counts unread notifications for a user.
     *
     * @param tenantId        Tenant identifier
     * @param recipientUserId Recipient user identifier
     * @return Count of unread notifications
     */
    @Query("SELECT COUNT(n) FROM NotificationEntity n " + "WHERE n.tenantId = :tenantId " + "AND n.recipientUserId = :recipientUserId " + "AND n.status != 'READ'")
    long countUnreadByTenantIdAndRecipientUserId(@Param("tenantId") String tenantId, @Param("recipientUserId") String recipientUserId);
}

