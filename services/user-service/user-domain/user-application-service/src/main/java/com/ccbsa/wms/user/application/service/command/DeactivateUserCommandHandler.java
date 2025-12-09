package com.ccbsa.wms.user.application.service.command;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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

    public DeactivateUserCommandHandler(
            UserRepository userRepository,
            UserEventPublisher eventPublisher,
            AuthenticationServicePort authenticationService) {
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
        User user = userRepository.findById(command.getUserId())
                .orElseThrow(() -> new UserNotFoundException(
                        String.format("User not found: %s", command.getUserId().getValue())));

        // 2. Validate can deactivate
        if (!user.canDeactivate()) {
            throw new IllegalStateException(
                    String.format("Cannot deactivate user: current status is %s", user.getStatus()));
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

        // 6. Publish events
        List<DomainEvent<?>> domainEvents = user.getDomainEvents();
        if (!domainEvents.isEmpty()) {
            eventPublisher.publish(domainEvents);
            user.clearDomainEvents();
        }

        logger.info("User deactivated successfully: userId={}", command.getUserId().getValue());
    }
}

