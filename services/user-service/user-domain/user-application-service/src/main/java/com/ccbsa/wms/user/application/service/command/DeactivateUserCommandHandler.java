package com.ccbsa.wms.user.application.service.command;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.ccbsa.common.domain.DomainEvent;
import com.ccbsa.wms.user.application.service.command.dto.DeactivateUserCommand;
import com.ccbsa.wms.user.application.service.exception.UserNotFoundException;
import com.ccbsa.wms.user.application.service.port.auth.AuthenticationServicePort;
import com.ccbsa.wms.user.application.service.port.messaging.UserEventPublisher;
import com.ccbsa.wms.user.application.service.port.repository.UserRepository;
import com.ccbsa.wms.user.domain.core.entity.User;

/**
 * Command Handler: DeactivateUserCommandHandler
 * <p>
 * Handles user deactivation use case.
 */
@Component
public class DeactivateUserCommandHandler {
    private static final Logger logger = LoggerFactory.getLogger(DeactivateUserCommandHandler.class);

    private final UserRepository userRepository;
    private final UserEventPublisher eventPublisher;
    private final AuthenticationServicePort authenticationService;

    public DeactivateUserCommandHandler(UserRepository userRepository, UserEventPublisher eventPublisher, AuthenticationServicePort authenticationService) {
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
        this.authenticationService = authenticationService;
    }

    /**
     * Handles the DeactivateUserCommand.
     *
     * @param command Command to execute
     * @throws UserNotFoundException if user not found
     * @throws IllegalStateException if user cannot be deactivated
     */
    @Transactional
    public void handle(DeactivateUserCommand command) {
        logger.debug("Deactivating user: userId={}", command.getUserId().getValue());

        // 1. Load user
        User user = userRepository.findById(command.getUserId()).orElseThrow(() -> new UserNotFoundException(String.format("User not found: %s", command.getUserId().getValue())));

        // 2. Validate can deactivate
        if (!user.canDeactivate()) {
            throw new IllegalStateException(String.format("Cannot deactivate user: current status is %s", user.getStatus()));
        }

        // 3. Deactivate user (domain logic)
        user.deactivate();

        // 4. Persist
        userRepository.save(user);

        // 5. Disable user in Keycloak
        if (user.getKeycloakUserId().isPresent()) {
            try {
                authenticationService.disableUser(user.getKeycloakUserId().get());
            } catch (Exception e) {
                logger.error("Failed to disable user in Keycloak: {}", e.getMessage(), e);
                // Don't fail the operation - domain state is updated
            }
        }

        // 6. Get domain events BEFORE clearing
        List<DomainEvent<?>> domainEvents = List.copyOf(user.getDomainEvents());
        user.clearDomainEvents();

        // 7. Publish events after transaction commit
        if (!domainEvents.isEmpty()) {
            publishEventsAfterCommit(domainEvents);
        }

        logger.info("User deactivated successfully: userId={}", command.getUserId().getValue());
    }

    /**
     * Publishes domain events after transaction commit to avoid race conditions.
     * <p>
     * Events are published using TransactionSynchronizationManager to ensure they are only published after the database transaction has successfully committed. This prevents race
     * conditions where event listeners consume events before the aggregate is visible in the database.
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

