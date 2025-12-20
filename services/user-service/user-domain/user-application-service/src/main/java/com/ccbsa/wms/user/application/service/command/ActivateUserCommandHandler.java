package com.ccbsa.wms.user.application.service.command;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ccbsa.common.domain.DomainEvent;
import com.ccbsa.wms.user.application.service.command.dto.ActivateUserCommand;
import com.ccbsa.wms.user.application.service.exception.UserNotFoundException;
import com.ccbsa.wms.user.application.service.port.auth.AuthenticationServicePort;
import com.ccbsa.wms.user.application.service.port.messaging.UserEventPublisher;
import com.ccbsa.wms.user.application.service.port.repository.UserRepository;
import com.ccbsa.wms.user.domain.core.entity.User;

/**
 * Command Handler: ActivateUserCommandHandler
 * <p>
 * Handles user activation use case.
 */
@Component
public class ActivateUserCommandHandler {
    private static final Logger logger = LoggerFactory.getLogger(ActivateUserCommandHandler.class);

    private final UserRepository userRepository;
    private final UserEventPublisher eventPublisher;
    private final AuthenticationServicePort authenticationService;

    public ActivateUserCommandHandler(UserRepository userRepository, UserEventPublisher eventPublisher, AuthenticationServicePort authenticationService) {
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
        this.authenticationService = authenticationService;
    }

    /**
     * Handles the ActivateUserCommand.
     *
     * @param command Command to execute
     * @throws UserNotFoundException if user not found
     * @throws IllegalStateException if user cannot be activated
     */
    @Transactional
    public void handle(ActivateUserCommand command) {
        logger.debug("Activating user: userId={}", command.getUserId().getValue());

        // 1. Load user
        User user = userRepository.findById(command.getUserId()).orElseThrow(() -> new UserNotFoundException(String.format("User not found: %s", command.getUserId().getValue())));

        // 2. Validate can activate
        if (!user.canActivate()) {
            throw new IllegalStateException(String.format("Cannot activate user: current status is %s", user.getStatus()));
        }

        // 3. Activate user (domain logic)
        user.activate();

        // 4. Persist
        userRepository.save(user);

        // 5. Enable user in Keycloak
        if (user.getKeycloakUserId().isPresent()) {
            try {
                authenticationService.enableUser(user.getKeycloakUserId().get());
            } catch (Exception e) {
                logger.error("Failed to enable user in Keycloak: {}", e.getMessage(), e);
                // Don't fail the operation - domain state is updated
            }
        }

        // 6. Publish events
        List<DomainEvent<?>> domainEvents = user.getDomainEvents();
        if (!domainEvents.isEmpty()) {
            eventPublisher.publish(domainEvents);
            user.clearDomainEvents();
        }

        logger.info("User activated successfully: userId={}", command.getUserId().getValue());
    }
}

