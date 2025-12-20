package com.ccbsa.wms.user.application.service.command;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ccbsa.common.domain.DomainEvent;
import com.ccbsa.common.domain.valueobject.EmailAddress;
import com.ccbsa.wms.user.application.service.command.dto.UpdateUserProfileCommand;
import com.ccbsa.wms.user.application.service.exception.UserNotFoundException;
import com.ccbsa.wms.user.application.service.port.auth.AuthenticationServicePort;
import com.ccbsa.wms.user.application.service.port.messaging.UserEventPublisher;
import com.ccbsa.wms.user.application.service.port.repository.UserRepository;
import com.ccbsa.wms.user.domain.core.entity.User;
import com.ccbsa.wms.user.domain.core.valueobject.FirstName;
import com.ccbsa.wms.user.domain.core.valueobject.LastName;

/**
 * Command Handler: UpdateUserProfileCommandHandler
 * <p>
 * Handles user profile update use case.
 * <p>
 * Responsibilities: - Load user aggregate - Update profile information - Persist aggregate changes - Sync with Keycloak - Publish domain events
 */
@Component
public class UpdateUserProfileCommandHandler {
    private static final Logger logger = LoggerFactory.getLogger(UpdateUserProfileCommandHandler.class);

    private final UserRepository userRepository;
    private final UserEventPublisher eventPublisher;
    private final AuthenticationServicePort authenticationService;

    public UpdateUserProfileCommandHandler(UserRepository userRepository, UserEventPublisher eventPublisher, AuthenticationServicePort authenticationService) {
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
        this.authenticationService = authenticationService;
    }

    /**
     * Handles the UpdateUserProfileCommand.
     * <p>
     * Transaction boundary: One transaction per command execution. Events published after successful commit.
     *
     * @param command Command to execute
     * @throws UserNotFoundException if user not found
     */
    @Transactional
    public void handle(UpdateUserProfileCommand command) {
        // 1. Validate command
        if (command == null) {
            throw new IllegalArgumentException("Command cannot be null");
        }

        logger.debug("Updating user profile: userId={}", command.getUserId().getValue());
        if (command.getUserId() == null) {
            throw new IllegalArgumentException("UserId is required");
        }
        if (command.getEmail() == null || command.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("EmailAddress is required");
        }

        // 2. Load user
        User user = userRepository.findById(command.getUserId()).orElseThrow(() -> new UserNotFoundException(String.format("User not found: %s", command.getUserId().getValue())));

        // 3. Update profile (domain logic)
        user.updateProfile(EmailAddress.of(command.getEmail()), FirstName.of(command.getFirstName()), LastName.of(command.getLastName()));

        // 4. Persist
        userRepository.save(user);

        // 5. Sync with Keycloak
        if (user.getKeycloakUserId().isPresent()) {
            try {
                authenticationService.updateUser(user.getKeycloakUserId().get(), command.getEmail(), command.getFirstName(), command.getLastName());
            } catch (Exception e) {
                // Log error but don't fail the operation
                // User data is source of truth, Keycloak sync can be retried
                logger.error("Failed to sync user profile with Keycloak: {}", e.getMessage(), e);
            }
        }

        // 6. Publish events
        List<DomainEvent<?>> domainEvents = user.getDomainEvents();
        if (!domainEvents.isEmpty()) {
            eventPublisher.publish(domainEvents);
            user.clearDomainEvents();
        }

        logger.info("User profile updated successfully: userId={}", command.getUserId().getValue());
    }
}

