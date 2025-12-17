package com.ccbsa.wms.user.application.service.command;

import java.util.Collections;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ccbsa.wms.user.application.service.command.dto.RemoveUserRoleCommand;
import com.ccbsa.wms.user.application.service.exception.UserNotFoundException;
import com.ccbsa.wms.user.application.service.port.auth.AuthenticationServicePort;
import com.ccbsa.wms.user.application.service.port.messaging.UserEventPublisher;
import com.ccbsa.wms.user.application.service.port.repository.UserRepository;
import com.ccbsa.wms.user.domain.core.entity.User;
import com.ccbsa.wms.user.domain.core.event.UserRoleRemovedEvent;

/**
 * Command Handler: RemoveUserRoleCommandHandler
 * <p>
 * Handles user role removal use case.
 */
@Component
public class RemoveUserRoleCommandHandler {
    private static final Logger logger = LoggerFactory.getLogger(RemoveUserRoleCommandHandler.class);
    private static final Set<String> VALID_ROLES = Set.of("SYSTEM_ADMIN", "TENANT_ADMIN", "WAREHOUSE_MANAGER", "PICKER", "USER");

    private final UserRepository userRepository;
    private final UserEventPublisher eventPublisher;
    private final AuthenticationServicePort authenticationService;

    public RemoveUserRoleCommandHandler(UserRepository userRepository, UserEventPublisher eventPublisher, AuthenticationServicePort authenticationService) {
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
        this.authenticationService = authenticationService;
    }

    /**
     * Handles the RemoveUserRoleCommand.
     *
     * @param command Command to execute
     * @throws UserNotFoundException    if user not found
     * @throws IllegalArgumentException if role is invalid
     */
    @Transactional
    public void handle(RemoveUserRoleCommand command) {
        logger.debug("Removing role from user: userId={}, role={}", command.getUserId()
                .getValue(), command.getRoleName());

        // 1. Validate role
        if (!VALID_ROLES.contains(command.getRoleName())) {
            throw new IllegalArgumentException(String.format("Invalid role: %s", command.getRoleName()));
        }

        // 2. Load user
        User user = userRepository.findById(command.getUserId())
                .orElseThrow(() -> new UserNotFoundException(String.format("User not found: %s", command.getUserId()
                        .getValue())));

        // 3. Remove role in Keycloak
        if (user.getKeycloakUserId()
                .isPresent()) {
            try {
                authenticationService.removeRole(user.getKeycloakUserId()
                        .get(), command.getRoleName());
            } catch (Exception e) {
                logger.error("Failed to remove role in Keycloak: {}", e.getMessage(), e);
                throw new RuntimeException(String.format("Failed to remove role: %s", e.getMessage()), e);
            }
        }

        // 4. Update last modified timestamp
        // Note: Role information is stored in Keycloak, not in domain model
        userRepository.save(user);

        // 5. Publish event
        UserRoleRemovedEvent event = new UserRoleRemovedEvent(user.getId(), user.getTenantId(), command.getRoleName());
        eventPublisher.publish(Collections.singletonList(event));

        logger.info("Role removed successfully: userId={}, role={}", command.getUserId()
                .getValue(), command.getRoleName());
    }
}

