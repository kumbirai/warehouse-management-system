package com.ccbsa.wms.user.application.service.command;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ccbsa.common.domain.DomainEvent;
import com.ccbsa.wms.user.application.service.command.dto.SuspendUserCommand;
import com.ccbsa.wms.user.application.service.exception.UserNotFoundException;
import com.ccbsa.wms.user.application.service.port.auth.AuthenticationServicePort;
import com.ccbsa.wms.user.application.service.port.messaging.UserEventPublisher;
import com.ccbsa.wms.user.application.service.port.repository.UserRepository;
import com.ccbsa.wms.user.domain.core.entity.User;

/**
 * Command Handler: SuspendUserCommandHandler
 * <p>
 * Handles user suspension use case.
 */
@Component
public class SuspendUserCommandHandler {
    private static final Logger logger = LoggerFactory.getLogger(SuspendUserCommandHandler.class);

    private final UserRepository userRepository;
    private final UserEventPublisher eventPublisher;
    private final AuthenticationServicePort authenticationService;

    public SuspendUserCommandHandler(
            UserRepository userRepository,
            UserEventPublisher eventPublisher,
            AuthenticationServicePort authenticationService) {
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
        this.authenticationService = authenticationService;
    }

    /**
     * Handles the SuspendUserCommand.
     *
     * @param command Command to execute
     * @throws UserNotFoundException if user not found
     * @throws IllegalStateException if user cannot be suspended
     */
    @Transactional
    public void handle(SuspendUserCommand command) {
        logger.debug("Suspending user: userId={}", command.getUserId().getValue());

        // 1. Load user
        User user = userRepository.findById(command.getUserId())
                .orElseThrow(() -> new UserNotFoundException(
                        String.format("User not found: %s", command.getUserId().getValue())));

        // 2. Validate can suspend
        if (!user.canSuspend()) {
            throw new IllegalStateException(
                    String.format("Cannot suspend user: current status is %s", user.getStatus()));
        }

        // 3. Suspend user (domain logic)
        user.suspend();

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

        logger.info("User suspended successfully: userId={}", command.getUserId().getValue());
    }
}

