package com.ccbsa.wms.notification.domain.core.entity;

import java.time.LocalDateTime;

import com.ccbsa.common.domain.TenantAwareAggregateRoot;
import com.ccbsa.common.domain.valueobject.EmailAddress;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.common.domain.valueobject.UserId;
import com.ccbsa.wms.notification.domain.core.event.NotificationCreatedEvent;
import com.ccbsa.wms.notification.domain.core.valueobject.Message;
import com.ccbsa.wms.notification.domain.core.valueobject.NotificationId;
import com.ccbsa.wms.notification.domain.core.valueobject.NotificationStatus;
import com.ccbsa.wms.notification.domain.core.valueobject.NotificationType;
import com.ccbsa.wms.notification.domain.core.valueobject.Title;

/**
 * Aggregate Root: Notification
 * <p>
 * Represents a notification sent to a user.
 * <p>
 * Business Rules:
 * - Notifications are tenant-aware
 * - Notifications have a recipient user
 * - Notifications can be marked as read
 * - Notifications have a status (PENDING, SENT, DELIVERED, FAILED, READ)
 */
public class Notification extends TenantAwareAggregateRoot<NotificationId> {

    // Value Objects
    private Title title;
    private Message message;
    private NotificationType type;
    private NotificationStatus status;
    private UserId recipientUserId;
    private EmailAddress recipientEmailAddress; // Optional: stored from event payload for efficient delivery

    // Primitives
    private LocalDateTime createdAt;
    private LocalDateTime lastModifiedAt;
    private LocalDateTime sentAt;
    private LocalDateTime readAt;

    /**
     * Private constructor for builder pattern.
     * Prevents direct instantiation.
     */
    private Notification() {
    }

    /**
     * Factory method to create builder instance.
     *
     * @return Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Business logic method: Marks notification as sent.
     * <p>
     * Business Rules:
     * - Only PENDING notifications can be marked as sent
     * - Sets sentAt timestamp
     *
     * @throws IllegalStateException if notification is not in PENDING status
     */
    public void markAsSent() {
        if (status != NotificationStatus.PENDING) {
            throw new IllegalStateException(
                    String.format("Cannot mark notification as sent: current status is %s", status)
            );
        }

        this.status = NotificationStatus.SENT;
        this.sentAt = LocalDateTime.now();
        this.lastModifiedAt = LocalDateTime.now();
    }

    /**
     * Business logic method: Marks notification as delivered.
     * <p>
     * Business Rules:
     * - Only SENT notifications can be marked as delivered
     *
     * @throws IllegalStateException if notification is not in SENT status
     */
    public void markAsDelivered() {
        if (status != NotificationStatus.SENT) {
            throw new IllegalStateException(
                    String.format("Cannot mark notification as delivered: current status is %s", status)
            );
        }

        this.status = NotificationStatus.DELIVERED;
        this.lastModifiedAt = LocalDateTime.now();
    }

    /**
     * Business logic method: Marks notification as failed.
     * <p>
     * Business Rules:
     * - PENDING or SENT notifications can be marked as failed
     *
     * @throws IllegalStateException if notification is in invalid status
     */
    public void markAsFailed() {
        if (status != NotificationStatus.PENDING && status != NotificationStatus.SENT) {
            throw new IllegalStateException(
                    String.format("Cannot mark notification as failed: current status is %s", status)
            );
        }

        this.status = NotificationStatus.FAILED;
        this.lastModifiedAt = LocalDateTime.now();
    }

    /**
     * Business logic method: Marks notification as read.
     * <p>
     * Business Rules:
     * - Only DELIVERED notifications can be marked as read
     * - Sets readAt timestamp
     *
     * @throws IllegalStateException if notification is not in DELIVERED status
     */
    public void markAsRead() {
        if (status != NotificationStatus.DELIVERED) {
            throw new IllegalStateException(
                    String.format("Cannot mark notification as read: current status is %s", status)
            );
        }

        this.status = NotificationStatus.READ;
        this.readAt = LocalDateTime.now();
        this.lastModifiedAt = LocalDateTime.now();
    }

    /**
     * Query method: Checks if notification can be sent.
     *
     * @return true if notification is in PENDING status
     */
    public boolean canBeSent() {
        return status == NotificationStatus.PENDING;
    }

    /**
     * Query method: Checks if notification is read.
     *
     * @return true if notification is in READ status
     */
    public boolean isRead() {
        return status == NotificationStatus.READ;
    }

    public Title getTitle() {
        return title;
    }

    // Getters (read-only access)

    public Message getMessage() {
        return message;
    }

    public NotificationType getType() {
        return type;
    }

    public NotificationStatus getStatus() {
        return status;
    }

    public UserId getRecipientUserId() {
        return recipientUserId;
    }

    public EmailAddress getRecipientEmail() {
        return recipientEmailAddress;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getLastModifiedAt() {
        return lastModifiedAt;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public LocalDateTime getReadAt() {
        return readAt;
    }

    /**
     * Builder class for constructing Notification instances.
     * Ensures all required fields are set and validated.
     */
    public static class Builder {
        private Notification notification = new Notification();

        public Builder notificationId(NotificationId id) {
            notification.setId(id);
            return this;
        }

        public Builder tenantId(TenantId tenantId) {
            notification.setTenantId(tenantId);
            return this;
        }

        public Builder title(Title title) {
            notification.title = title;
            return this;
        }

        public Builder message(Message message) {
            notification.message = message;
            return this;
        }

        public Builder type(NotificationType type) {
            notification.type = type;
            return this;
        }

        public Builder recipientUserId(UserId recipientUserId) {
            notification.recipientUserId = recipientUserId;
            return this;
        }

        public Builder recipientEmail(EmailAddress recipientEmailAddress) {
            notification.recipientEmailAddress = recipientEmailAddress;
            return this;
        }

        public Builder status(NotificationStatus status) {
            notification.status = status;
            return this;
        }

        /**
         * Sets the creation timestamp (for loading from database).
         *
         * @param createdAt Creation timestamp
         * @return Builder instance
         */
        public Builder createdAt(LocalDateTime createdAt) {
            notification.createdAt = createdAt;
            return this;
        }

        /**
         * Sets the last modified timestamp (for loading from database).
         *
         * @param lastModifiedAt Last modified timestamp
         * @return Builder instance
         */
        public Builder lastModifiedAt(LocalDateTime lastModifiedAt) {
            notification.lastModifiedAt = lastModifiedAt;
            return this;
        }

        /**
         * Sets the sent timestamp (for loading from database).
         *
         * @param sentAt Sent timestamp
         * @return Builder instance
         */
        public Builder sentAt(LocalDateTime sentAt) {
            notification.sentAt = sentAt;
            return this;
        }

        /**
         * Sets the read timestamp (for loading from database).
         *
         * @param readAt Read timestamp
         * @return Builder instance
         */
        public Builder readAt(LocalDateTime readAt) {
            notification.readAt = readAt;
            return this;
        }

        /**
         * Sets the version (for loading from database).
         *
         * @param version Version number
         * @return Builder instance
         */
        public Builder version(int version) {
            notification.setVersion(version);
            return this;
        }

        /**
         * Sets the version as Long (for loading from database).
         *
         * @param version Version number
         * @return Builder instance
         */
        public Builder version(Long version) {
            notification.setVersion(version != null ? version.intValue() : 0);
            return this;
        }

        /**
         * Builds and validates the Notification instance.
         *
         * @return Validated Notification instance
         * @throws IllegalArgumentException if validation fails
         */
        public Notification build() {
            validate();
            initializeDefaults();

            // Set createdAt if not already set (for new notifications)
            if (notification.createdAt == null) {
                notification.createdAt = LocalDateTime.now();
            }
            if (notification.lastModifiedAt == null) {
                notification.lastModifiedAt = LocalDateTime.now();
            }

            // Publish creation event only if this is a new notification (no version set)
            if (notification.getVersion() == 0) {
                notification.addDomainEvent(new NotificationCreatedEvent(
                        notification.getId(),
                        notification.getTenantId(),
                        notification.type
                ));
            }

            return consumeNotification();
        }

        /**
         * Validates all required fields are set.
         *
         * @throws IllegalArgumentException if validation fails
         */
        private void validate() {
            if (notification.getId() == null) {
                throw new IllegalArgumentException("NotificationId is required");
            }
            if (notification.getTenantId() == null) {
                throw new IllegalArgumentException("TenantId is required");
            }
            if (notification.title == null) {
                throw new IllegalArgumentException("Title is required");
            }
            if (notification.message == null) {
                throw new IllegalArgumentException("Message is required");
            }
            if (notification.type == null) {
                throw new IllegalArgumentException("NotificationType is required");
            }
            if (notification.recipientUserId == null) {
                throw new IllegalArgumentException("RecipientUserId is required");
            }
        }

        /**
         * Initializes default values for optional fields.
         */
        private void initializeDefaults() {
            if (notification.status == null) {
                notification.status = NotificationStatus.PENDING;
            }
        }

        /**
         * Consumes the notification from the builder and returns it.
         * Creates a new notification instance for the next build.
         *
         * @return Built notification
         */
        private Notification consumeNotification() {
            Notification builtNotification = notification;
            notification = new Notification();
            return builtNotification;
        }

        /**
         * Builds Notification without publishing creation event.
         * Used when reconstructing from persistence.
         *
         * @return Validated Notification instance
         * @throws IllegalArgumentException if validation fails
         */
        public Notification buildWithoutEvents() {
            validate();
            initializeDefaults();

            // Set createdAt if not already set
            if (notification.createdAt == null) {
                notification.createdAt = LocalDateTime.now();
            }
            if (notification.lastModifiedAt == null) {
                notification.lastModifiedAt = LocalDateTime.now();
            }

            // Do not publish events when loading from database
            return consumeNotification();
        }
    }
}

