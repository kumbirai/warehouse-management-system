package com.ccbsa.wms.notification.application.service.command;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.ccbsa.common.domain.DomainEvent;
import com.ccbsa.wms.notification.application.service.command.dto.CreateNotificationCommand;
import com.ccbsa.wms.notification.application.service.command.dto.CreateNotificationResult;
import com.ccbsa.wms.notification.application.service.port.messaging.NotificationEventPublisher;
import com.ccbsa.wms.notification.application.service.port.repository.NotificationRepository;
import com.ccbsa.wms.notification.domain.core.entity.Notification;
import com.ccbsa.wms.notification.domain.core.valueobject.NotificationId;

/**
 * Command Handler: CreateNotificationCommandHandler
 * <p>
 * Handles creation of new Notification aggregate.
 */
@Component
public class CreateNotificationCommandHandler {
    private static final Logger logger = LoggerFactory.getLogger(CreateNotificationCommandHandler.class);

    private final NotificationRepository repository;
    private final NotificationEventPublisher eventPublisher;

    public CreateNotificationCommandHandler(NotificationRepository repository, NotificationEventPublisher eventPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public CreateNotificationResult handle(CreateNotificationCommand command) {
        // 1. Validate command
        validateCommand(command);

        // 2. Create aggregate using builder
        Notification.Builder builder =
                Notification.builder().notificationId(NotificationId.generate()).tenantId(command.getTenantId()).recipientUserId(command.getRecipientUserId())
                        .title(command.getTitle()).message(command.getMessage()).type(command.getType());

        // Set recipient email if provided (from event payload)
        if (command.getRecipientEmail() != null) {
            builder.recipientEmail(command.getRecipientEmail());
        }

        Notification notification = builder.build();

        // 3. Get domain events BEFORE saving (save() returns a new instance without events)
        // This is critical - domain events are added during build() and must be captured
        // before the repository save() which returns a new mapped instance
        List<DomainEvent<?>> domainEvents = new ArrayList<>(notification.getDomainEvents());

        // 4. Persist aggregate (this returns a new instance from mapper, without domain events)
        Notification savedNotification = repository.save(notification);

        // 5. Publish events after transaction commit to avoid race conditions
        // This ensures events are only published after the database transaction has committed,
        // preventing race conditions where event listeners consume events before the notification
        // is visible in the database
        if (!domainEvents.isEmpty()) {
            publishEventsAfterCommit(domainEvents);
            // Clear events from original notification (savedNotification doesn't have them)
            notification.clearDomainEvents();
        }

        // 6. Return result (use savedNotification which has updated version from DB)
        return CreateNotificationResult.builder().notificationId(savedNotification.getId()).status(savedNotification.getStatus()).createdAt(savedNotification.getCreatedAt())
                .build();
    }

    private void validateCommand(CreateNotificationCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("Command cannot be null");
        }
        if (command.getTenantId() == null) {
            throw new IllegalArgumentException("TenantId is required");
        }
        if (command.getRecipientUserId() == null) {
            throw new IllegalArgumentException("RecipientUserId is required");
        }
        if (command.getTitle() == null) {
            throw new IllegalArgumentException("Title is required");
        }
        if (command.getMessage() == null) {
            throw new IllegalArgumentException("Message is required");
        }
        if (command.getType() == null) {
            throw new IllegalArgumentException("NotificationType is required");
        }
    }

    /**
     * Publishes domain events after transaction commit to avoid race conditions.
     * <p>
     * Events are published using TransactionSynchronizationManager to ensure they are only published after the database transaction has successfully committed. This prevents race
     * conditions where event listeners consume events before the
     * notification is visible in the database.
     *
     * @param domainEvents Domain events to publish
     */
    private void publishEventsAfterCommit(List<DomainEvent<?>> domainEvents) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            // No active transaction - publish immediately
            logger.debug("No active transaction - publishing events immediately");
            eventPublisher.publish(domainEvents);
            return;
        }

        // Register synchronization to publish events after transaction commit
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    logger.debug("Transaction committed - publishing {} domain events", domainEvents.size());
                    eventPublisher.publish(domainEvents);
                } catch (Exception e) {
                    logger.error("Failed to publish domain events after transaction commit", e);
                    // Don't throw - transaction already committed, event publishing failure
                    // should be handled by retry mechanisms or dead letter queue
                }
            }
        });
    }
}

