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
import jakarta.persistence.Version;

/**
 * JPA Entity: NotificationEntity
 * <p>
 * JPA representation of Notification aggregate. Uses tenant schema resolver for multi-tenant isolation (schema-per-tenant strategy).
 * <p>
 * This entity maps to the Notification domain aggregate and uses domain enums directly to maintain consistency between domain and persistence layers.
 * <p>
 * The schema "tenant_schema" is a placeholder that will be dynamically replaced with the actual tenant schema at runtime by TenantAwarePhysicalNamingStrategy.
 */
@Entity
@Table(name = "notifications", schema = "tenant_schema")
public class NotificationEntity {
    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "tenant_id", length = 50, nullable = false)
    private String tenantId;

    @Column(name = "recipient_user_id", length = 50, nullable = false)
    private String recipientUserId;

    @Column(name = "recipient_email", length = 255)
    private String recipientEmail;

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

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    // JPA requires no-arg constructor
    public NotificationEntity() {
    }

    // Getters and setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getRecipientUserId() {
        return recipientUserId;
    }

    public void setRecipientUserId(String recipientUserId) {
        this.recipientUserId = recipientUserId;
    }

    public String getRecipientEmail() {
        return recipientEmail;
    }

    public void setRecipientEmail(String recipientEmail) {
        this.recipientEmail = recipientEmail;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public NotificationType getType() {
        return type;
    }

    public void setType(NotificationType type) {
        this.type = type;
    }

    public NotificationStatus getStatus() {
        return status;
    }

    public void setStatus(NotificationStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getLastModifiedAt() {
        return lastModifiedAt;
    }

    public void setLastModifiedAt(LocalDateTime lastModifiedAt) {
        this.lastModifiedAt = lastModifiedAt;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }

    public LocalDateTime getReadAt() {
        return readAt;
    }

    public void setReadAt(LocalDateTime readAt) {
        this.readAt = readAt;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}

