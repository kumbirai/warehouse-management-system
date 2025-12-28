package com.ccbsa.wms.notification.dataaccess.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import com.ccbsa.wms.notification.domain.core.valueobject.NotificationStatus;
import com.ccbsa.wms.notification.domain.core.valueobject.NotificationType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA Entity: NotificationViewEntity
 * <p>
 * Read model entity for notification queries. This is a denormalized view optimized for read operations.
 * <p>
 * Note: Currently maps to the notifications table. In the future, this can be migrated to a separate
 * notification_views table that is maintained via event listeners for better read/write separation.
 * <p>
 * The schema "tenant_schema" is a placeholder that will be dynamically replaced with
 * the actual tenant schema at runtime by TenantAwarePhysicalNamingStrategy.
 */
@Entity
@Table(name = "notifications", schema = "tenant_schema")
@Getter
@Setter
@NoArgsConstructor
public class NotificationViewEntity {
    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "tenant_id", length = 50, nullable = false)
    private String tenantId;

    @Column(name = "recipient_user_id", length = 50, nullable = false)
    private String recipientUserId;

    @Column(name = "title", length = 200, nullable = false)
    private String title;

    @Column(name = "message", length = 1000, nullable = false)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 50, nullable = false)
    private NotificationType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private NotificationStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_modified_at")
    private LocalDateTime lastModifiedAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "read_at")
    private LocalDateTime readAt;
}

