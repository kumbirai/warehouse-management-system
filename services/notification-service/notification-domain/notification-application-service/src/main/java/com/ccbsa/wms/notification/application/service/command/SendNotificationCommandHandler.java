package com.ccbsa.wms.notification.application.service.command;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.ccbsa.wms.notification.application.service.command.dto.DeliveryResult;
import com.ccbsa.wms.notification.application.service.command.dto.SendNotificationCommand;
import com.ccbsa.wms.notification.application.service.command.dto.SendNotificationResult;
import com.ccbsa.wms.notification.application.service.exception.NotificationNotFoundException;
import com.ccbsa.wms.notification.application.service.exception.UnsupportedChannelException;
import com.ccbsa.wms.notification.application.service.port.delivery.NotificationDeliveryPort;
import com.ccbsa.wms.notification.application.service.port.messaging.NotificationEventPublisher;
import com.ccbsa.wms.notification.application.service.port.repository.NotificationRepository;
import com.ccbsa.wms.notification.domain.core.entity.Notification;
import com.ccbsa.wms.notification.domain.core.event.NotificationSentEvent;

/**
 * Command Handler: SendNotificationCommandHandler
 * <p>
 * Handles sending notifications via delivery channels (email, SMS, WhatsApp). Orchestrates adapter discovery, delivery execution, and status updates.
 */
@Component
public class SendNotificationCommandHandler {
    private static final Logger logger = LoggerFactory.getLogger(SendNotificationCommandHandler.class);

    private final NotificationRepository repository;
    private final List<NotificationDeliveryPort> deliveryAdapters;
    private final NotificationEventPublisher eventPublisher;

    public SendNotificationCommandHandler(NotificationRepository repository, List<NotificationDeliveryPort> deliveryAdapters, NotificationEventPublisher eventPublisher) {
        this.repository = repository;
        this.deliveryAdapters = deliveryAdapters;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public SendNotificationResult handle(SendNotificationCommand command) {
        // 1. Validate command
        validateCommand(command);

        // 2. Load notification
        Notification notification = repository.findById(command.getNotificationId())
                .orElseThrow(() -> new NotificationNotFoundException(command.getNotificationId()
                        .getValue()
                        .toString(), "Notification not found for sending"));

        // 3. Find adapter supporting the channel
        NotificationDeliveryPort adapter = findAdapter(command.getChannel());

        // 4. Send notification via adapter
        DeliveryResult deliveryResult = adapter.send(notification, command.getChannel());

        // 5. Update notification status based on result
        if (deliveryResult.isSuccess()) {
            notification.markAsSent();
            logger.info("Notification sent successfully: notificationId={}, channel={}, externalId={}", notification.getId(), command.getChannel(), deliveryResult.getExternalId());
        } else {
            notification.markAsFailed();
            logger.error("Notification delivery failed: notificationId={}, channel={}, error={}", notification.getId(), command.getChannel(), deliveryResult.getErrorMessage());
        }

        // 6. Persist updated notification
        repository.save(notification);

        // 7. Publish domain event on success after transaction commit
        if (deliveryResult.isSuccess()) {
            NotificationSentEvent sentEvent = new NotificationSentEvent(notification.getId(), command.getChannel(), deliveryResult.getSentAt());
            publishEventAfterCommit(sentEvent);
        }

        // 8. Return result
        return SendNotificationResult.builder()
                .success(deliveryResult.isSuccess())
                .externalId(deliveryResult.getExternalId())
                .status(notification.getStatus())
                .build();
    }

    private void validateCommand(SendNotificationCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("Command cannot be null");
        }
        if (command.getNotificationId() == null) {
            throw new IllegalArgumentException("NotificationId is required");
        }
        if (command.getChannel() == null) {
            throw new IllegalArgumentException("NotificationChannel is required");
        }
    }

    /**
     * Finds an adapter that supports the given channel.
     *
     * @param channel Delivery channel
     * @return Adapter supporting the channel
     * @throws UnsupportedChannelException if no adapter supports the channel
     */
    private NotificationDeliveryPort findAdapter(com.ccbsa.wms.notification.domain.core.valueobject.NotificationChannel channel) {
        return deliveryAdapters.stream()
                .filter(adapter -> adapter.supports(channel))
                .findFirst()
                .orElseThrow(() -> new UnsupportedChannelException(channel, "No delivery adapter registered for this channel"));
    }

    /**
     * Publishes domain event after transaction commit to avoid race conditions.
     * <p>
     * Events are published using TransactionSynchronizationManager to ensure they are only published after the database transaction has successfully committed.
     *
     * @param event Domain event to publish
     */
    private void publishEventAfterCommit(NotificationSentEvent event) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            // No active transaction - publish immediately
            logger.debug("No active transaction - publishing event immediately");
            eventPublisher.publish(event);
            return;
        }

        // Register synchronization to publish event after transaction commit
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    logger.debug("Transaction committed - publishing NotificationSentEvent");
                    eventPublisher.publish(event);
                } catch (Exception e) {
                    logger.error("Failed to publish NotificationSentEvent after transaction commit", e);
                    // Don't throw - transaction already committed, event publishing failure
                    // should be handled by retry mechanisms or dead letter queue
                }
            }
        });
    }
}

