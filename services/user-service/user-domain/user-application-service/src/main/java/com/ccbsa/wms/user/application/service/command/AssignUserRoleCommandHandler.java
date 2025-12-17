package com.ccbsa.wms.user.application.service.command;

import java.util.Collections;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ccbsa.wms.user.application.service.command.dto.AssignUserRoleCommand;
import com.ccbsa.wms.user.application.service.exception.UserNotFoundException;
import com.ccbsa.wms.user.application.service.port.auth.AuthenticationServicePort;
import com.ccbsa.wms.user.application.service.port.messaging.UserEventPublisher;
import com.ccbsa.wms.user.application.service.port.repository.UserRepository;
import com.ccbsa.wms.user.domain.core.entity.User;
import com.ccbsa.wms.user.domain.core.event.UserRoleAssignedEvent;

/**
 * Command Handler: AssignUserRoleCommandHandler
 * <p>
 * Handles user role assignment use case.
 */
@Component
public class AssignUserRoleCommandHandler {
    private static final Logger logger = LoggerFactory.getLogger(AssignUserRoleCommandHandler.class);
    private static final Set<String> VALID_ROLES = Set.of("SYSTEM_ADMIN", "TENANT_ADMIN", "WAREHOUSE_MANAGER", "PICKER", "USER");

    private final UserRepository userRepository;
    private final UserEventPublisher eventPublisher;
    private final AuthenticationServicePort authenticationService;

    public AssignUserRoleCommandHandler(UserRepository userRepository, UserEventPublisher eventPublisher, AuthenticationServicePort authenticationService) {
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
        this.authenticationService = authenticationService;
    }

    /**
     * Handles the AssignUserRoleCommand.
     *
     * @param command Command to execute
     * @throws UserNotFoundException    if user not found
     * @throws IllegalArgumentException if role is invalid
     */
    @Transactional
    public void handle(AssignUserRoleCommand command) {
        logger.debug("Assigning role to user: userId={}, role={}", command.getUserId()
                .getValue(), command.getRoleName());

        // 1. Validate role
        if (!VALID_ROLES.contains(command.getRoleName())) {
            throw new IllegalArgumentException(String.format("Invalid role: %s", command.getRoleName()));
        }

        // 2. Load user
        User user = userRepository.findById(command.getUserId())
                .orElseThrow(() -> new UserNotFoundException(String.format("User not found: %s", command.getUserId()
                        .getValue())));

        // 3. Assign role in Keycloak
        if (user.getKeycloakUserId()
                .isPresent()) {
            try {
                authenticationService.assignRole(user.getKeycloakUserId()
                        .get(), command.getRoleName());
            } catch (Exception e) {
                logger.error("Failed to assign role in Keycloak: {}", e.getMessage(), e);
                throw new RuntimeException(String.format("Failed to assign role: %s", e.getMessage()), e);
            }
        }

        // 4. Update last modified timestamp
        // Note: Role information is stored in Keycloak, not in domain model
        // We just update the timestamp to track changes
        userRepository.save(user);

        // 5. Publish event
        UserRoleAssignedEvent event = new UserRoleAssignedEvent(user.getId(), user.getTenantId(), command.getRoleName());
        eventPublisher.publish(Collections.singletonList(event));

        logger.info("Role assigned successfully: userId={}, role={}", command.getUserId()
                .getValue(), command.getRoleName());
    }
}

