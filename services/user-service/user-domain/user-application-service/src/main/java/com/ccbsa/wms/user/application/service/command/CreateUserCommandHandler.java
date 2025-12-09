package com.ccbsa.wms.user.application.service.command;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ccbsa.common.domain.DomainEvent;
import com.ccbsa.common.domain.valueobject.EmailAddress;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.common.domain.valueobject.UserId;
import com.ccbsa.wms.user.application.service.command.dto.CreateUserCommand;
import com.ccbsa.wms.user.application.service.command.dto.CreateUserResult;
import com.ccbsa.wms.user.application.service.exception.TenantNotActiveException;
import com.ccbsa.wms.user.application.service.exception.UserCreationException;
import com.ccbsa.wms.user.application.service.port.auth.AuthenticationServicePort;
import com.ccbsa.wms.user.application.service.port.messaging.UserEventPublisher;
import com.ccbsa.wms.user.application.service.port.repository.UserRepository;
import com.ccbsa.wms.user.application.service.port.service.TenantServicePort;
import com.ccbsa.wms.user.domain.core.entity.User;
import com.ccbsa.wms.user.domain.core.valueobject.FirstName;
import com.ccbsa.wms.user.domain.core.valueobject.KeycloakUserId;
import com.ccbsa.wms.user.domain.core.valueobject.LastName;
import com.ccbsa.wms.user.domain.core.valueobject.UserStatus;
import com.ccbsa.wms.user.domain.core.valueobject.Username;

/**
 * Command Handler: CreateUserCommandHandler
 * <p>
 * Handles user creation use case.
 * <p>
 * Responsibilities:
 * - Validate tenant exists and is ACTIVE
 * - Create user domain entity
 * - Persist user aggregate
 * - Create user in Keycloak
 * - Assign roles in Keycloak
 * - Publish domain events
 */
@Component
public class CreateUserCommandHandler {
    private static final Logger logger = LoggerFactory.getLogger(CreateUserCommandHandler.class);

    private final UserRepository userRepository;
    private final UserEventPublisher eventPublisher;
    private final AuthenticationServicePort authenticationService;
    private final TenantServicePort tenantService;

    public CreateUserCommandHandler(
            UserRepository userRepository,
            UserEventPublisher eventPublisher,
            AuthenticationServicePort authenticationService,
            TenantServicePort tenantService) {
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
        this.authenticationService = authenticationService;
        this.tenantService = tenantService;
    }

    /**
     * Handles the CreateUserCommand.
     * <p>
     * Transaction boundary: One transaction per command execution.
     * Events published after successful commit.
     *
     * @param command Command to execute
     * @return CreateUserResult with user ID and success status
     * @throws TenantNotActiveException if tenant is not active
     * @throws UserCreationException    if user creation fails
     */
    @Transactional
    public CreateUserResult handle(CreateUserCommand command) {
        // 1. Validate command
        if (command == null) {
            throw new IllegalArgumentException("Command cannot be null");
        }

        logger.debug("Creating user: username={}, tenantId={}", command.getUsername(), command.getTenantId());
        if (command.getTenantId() == null || command.getTenantId().trim().isEmpty()) {
            throw new IllegalArgumentException("TenantId is required");
        }
        if (command.getUsername() == null || command.getUsername().trim().isEmpty()) {
            throw new IllegalArgumentException("Username is required");
        }
        if (command.getEmail() == null || command.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("EmailAddress is required");
        }
        if (command.getPassword() == null || command.getPassword().trim().isEmpty()) {
            throw new IllegalArgumentException("Password is required");
        }

        // 2. Validate tenant exists and is ACTIVE
        TenantId tenantId = TenantId.of(command.getTenantId());
        if (!tenantService.isTenantActive(tenantId)) {
            throw new TenantNotActiveException(
                    String.format("Cannot create user: tenant is not active: %s", tenantId.getValue()));
        }

        // 3. Build domain entity
        UserId userId = UserId.of(UUID.randomUUID().toString());
        User user = User.builder()
                .userId(userId)
                .tenantId(tenantId)
                .username(Username.of(command.getUsername()))
                .email(EmailAddress.of(command.getEmail()))
                .firstName(command.getFirstName() != null ? FirstName.of(command.getFirstName()) : null)
                .lastName(command.getLastName() != null ? LastName.of(command.getLastName()) : null)
                .status(UserStatus.ACTIVE)
                .build();

        // 4. Persist domain entity
        userRepository.save(user);

        // 5. Create user in Keycloak
        try {
            KeycloakUserId keycloakUserId = authenticationService.createUser(
                    command.getTenantId(),
                    command.getUsername(),
                    command.getEmail(),
                    command.getPassword(),
                    command.getFirstName(),
                    command.getLastName());

            // 6. Link Keycloak user ID
            user.linkKeycloakUser(keycloakUserId);
            userRepository.save(user);

            // 7. Assign roles
            if (!command.getRoles().isEmpty()) {
                for (String role : command.getRoles()) {
                    authenticationService.assignRole(keycloakUserId, role);
                }
            }
        } catch (Exception e) {
            // Rollback: delete user from database
            logger.error("Failed to create user in Keycloak, rolling back user creation", e);
            try {
                userRepository.deleteById(userId);
            } catch (Exception deleteException) {
                logger.error("Failed to delete user during rollback", deleteException);
            }
            throw new UserCreationException(
                    String.format("Failed to create user in Keycloak: %s", e.getMessage()), e);
        }

        // 8. Publish events
        List<DomainEvent<?>> domainEvents = user.getDomainEvents();
        if (!domainEvents.isEmpty()) {
            eventPublisher.publish(domainEvents);
            user.clearDomainEvents();
        }

        logger.info("User created successfully: userId={}, username={}", userId.getValue(), command.getUsername());

        // 9. Return result
        return new CreateUserResult(userId.getValue(), true, "User created successfully");
    }
}

