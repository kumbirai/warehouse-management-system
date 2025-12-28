package com.ccbsa.wms.user.application.service.command;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.ccbsa.common.domain.DomainEvent;
import com.ccbsa.common.domain.valueobject.EmailAddress;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.common.domain.valueobject.UserId;
import com.ccbsa.wms.user.application.service.command.dto.CreateUserCommand;
import com.ccbsa.wms.user.application.service.command.dto.CreateUserResult;
import com.ccbsa.wms.user.application.service.exception.DuplicateUserException;
import com.ccbsa.wms.user.application.service.exception.KeycloakServiceException;
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
import com.ccbsa.wms.user.domain.core.valueobject.RoleConstants;
import com.ccbsa.wms.user.domain.core.valueobject.UserStatus;
import com.ccbsa.wms.user.domain.core.valueobject.Username;

import lombok.extern.slf4j.Slf4j;

/**
 * Command Handler: CreateUserCommandHandler
 * <p>
 * Handles user creation use case.
 * <p>
 * Responsibilities: - Validate tenant exists and is ACTIVE - Create user domain entity - Persist user aggregate - Create user in Keycloak - Assign roles in Keycloak - Publish
 * domain events
 */
@Slf4j
@Component
public class CreateUserCommandHandler {

    private final UserRepository userRepository;
    private final UserEventPublisher eventPublisher;
    private final AuthenticationServicePort authenticationService;
    private final TenantServicePort tenantService;
    private final String frontendBaseUrl;

    public CreateUserCommandHandler(UserRepository userRepository, UserEventPublisher eventPublisher, AuthenticationServicePort authenticationService,
                                    TenantServicePort tenantService, @Value("${frontend.base-url:http://localhost:3000}") String frontendBaseUrl) {
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
        this.authenticationService = authenticationService;
        this.tenantService = tenantService;
        this.frontendBaseUrl = frontendBaseUrl;
    }

    /**
     * Handles the CreateUserCommand.
     * <p>
     * Transaction boundary: One transaction per command execution. Events published after successful commit.
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

        log.debug("Creating user: username={}, tenantId={}", command.getUsername(), command.getTenantId());
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
            throw new TenantNotActiveException(String.format("Cannot create user: tenant is not active: %s", tenantId.getValue()));
        }

        // 3. Build domain entity
        UserId userId = UserId.of(UUID.randomUUID().toString());
        User user = User.builder().userId(userId).tenantId(tenantId).username(Username.of(command.getUsername())).email(EmailAddress.of(command.getEmail()))
                .firstName(command.getFirstName() != null ? FirstName.of(command.getFirstName()) : null)
                .lastName(command.getLastName() != null ? LastName.of(command.getLastName()) : null).status(UserStatus.ACTIVE).build();

        // 4. Persist domain entity
        userRepository.save(user);

        // 5. Create user in Keycloak
        try {
            KeycloakUserId keycloakUserId =
                    authenticationService.createUser(command.getTenantId(), command.getUsername(), command.getEmail(), command.getPassword(), command.getFirstName(),
                            command.getLastName());

            // 6. Link Keycloak user ID
            user.linkKeycloakUser(keycloakUserId);
            userRepository.save(user);

            // 7. Assign base USER role to all new users
            authenticationService.assignRole(keycloakUserId, RoleConstants.BASE_ROLE);
            log.debug("Base USER role assigned to new user: userId={}", keycloakUserId.getValue());

            // 8. Assign additional roles if provided
            if (!command.getRoles().isEmpty()) {
                for (String role : command.getRoles()) {
                    // Skip USER role if already in the list to avoid duplicate assignment
                    if (!RoleConstants.BASE_ROLE.equals(role)) {
                        authenticationService.assignRole(keycloakUserId, role);
                    }
                }
            }

            // 9. Send email verification and password reset email
            // This sends a Keycloak email with links for both VERIFY_EMAIL and UPDATE_PASSWORD actions
            try {
                String redirectUri = String.format("%s/verify-email", frontendBaseUrl);
                authenticationService.sendEmailVerificationAndPasswordReset(keycloakUserId, redirectUri);
                log.debug("Email verification and password reset email sent: userId={}", keycloakUserId.getValue());
            } catch (Exception emailException) {
                // Log error but don't fail user creation - email can be resent later
                log.warn("Failed to send email verification and password reset email: userId={}, error={}", keycloakUserId.getValue(), emailException.getMessage(), emailException);
            }
        } catch (KeycloakServiceException e) {
            // Check if it's a duplicate user error (HTTP 409)
            if (e.getMessage() != null && e.getMessage().contains("already exists")) {
                // Rollback: delete user from database
                log.warn("Duplicate user detected, rolling back user creation: {}", e.getMessage());
                try {
                    userRepository.deleteById(userId);
                } catch (Exception deleteException) {
                    log.error("Failed to delete user during rollback", deleteException);
                }
                throw new DuplicateUserException(e.getMessage(), e);
            }
            // Other Keycloak errors
            log.error("Failed to create user in Keycloak, rolling back user creation", e);
            try {
                userRepository.deleteById(userId);
            } catch (Exception deleteException) {
                log.error("Failed to delete user during rollback", deleteException);
            }
            throw new UserCreationException(String.format("Failed to create user in Keycloak: %s", e.getMessage()), e);
        } catch (Exception e) {
            // Rollback: delete user from database
            log.error("Failed to create user in Keycloak, rolling back user creation", e);
            try {
                userRepository.deleteById(userId);
            } catch (Exception deleteException) {
                log.error("Failed to delete user during rollback", deleteException);
            }
            throw new UserCreationException(String.format("Failed to create user in Keycloak: %s", e.getMessage()), e);
        }

        // 10. Get domain events BEFORE clearing
        List<DomainEvent<?>> domainEvents = List.copyOf(user.getDomainEvents());
        user.clearDomainEvents();

        // 11. Publish events after transaction commit
        if (!domainEvents.isEmpty()) {
            publishEventsAfterCommit(domainEvents);
        }

        log.info("User created successfully: userId={}, username={}", userId.getValue(), command.getUsername());

        // 12. Return result
        return new CreateUserResult(userId.getValue(), true, "User created successfully");
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
            log.debug("No active transaction - publishing events immediately");
            eventPublisher.publish(domainEvents);
            return;
        }

        // Register synchronization to publish events after transaction commit
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    log.debug("Transaction committed - publishing {} domain events", domainEvents.size());
                    eventPublisher.publish(domainEvents);
                } catch (Exception e) {
                    log.error("Failed to publish domain events after transaction commit", e);
                    // Don't throw - transaction already committed, event publishing failure
                    // should be handled by retry mechanisms or dead letter queue
                }
            }
        });
    }
}

