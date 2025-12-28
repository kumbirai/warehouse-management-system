package com.ccbsa.wms.user.application.service.command;

import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.ccbsa.common.domain.EventMetadata;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.common.domain.valueobject.UserId;
import com.ccbsa.wms.user.application.service.command.dto.AssignUserRoleCommand;
import com.ccbsa.wms.user.application.service.exception.RoleAssignmentException;
import com.ccbsa.wms.user.application.service.exception.UserNotFoundException;
import com.ccbsa.wms.user.application.service.port.auth.AuthenticationServicePort;
import com.ccbsa.wms.user.application.service.port.messaging.UserEventPublisher;
import com.ccbsa.wms.user.application.service.port.repository.UserRepository;
import com.ccbsa.wms.user.application.service.port.security.SecurityContextPort;
import com.ccbsa.wms.user.application.service.validation.RoleAssignmentValidator;
import com.ccbsa.wms.user.domain.core.entity.User;
import com.ccbsa.wms.user.domain.core.event.UserRoleAssignedEvent;
import com.ccbsa.wms.user.domain.core.valueobject.RoleConstants;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Command Handler: AssignUserRoleCommandHandler
 * <p>
 * Handles user role assignment use case.
 * <p>
 * Validates role assignment permissions according to Roles_and_Permissions_Definition.md.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AssignUserRoleCommandHandler {
    private final UserRepository userRepository;
    private final UserEventPublisher eventPublisher;
    private final AuthenticationServicePort authenticationService;
    private final RoleAssignmentValidator roleAssignmentValidator;
    private final SecurityContextPort securityContextPort;

    /**
     * Handles the AssignUserRoleCommand.
     *
     * @param command Command to execute
     * @throws UserNotFoundException           if user not found
     * @throws IllegalArgumentException        if role is invalid
     * @throws InsufficientPrivilegesException if user cannot assign the role
     * @throws TenantMismatchException         if tenant mismatch
     * @throws RoleAssignmentException         if role assignment fails
     */
    @Transactional
    public void handle(AssignUserRoleCommand command) {
        log.debug("Assigning role to user: userId={}, role={}", command.getUserId().getValue(), command.getRoleName());

        // 1. Validate role exists
        if (!RoleConstants.isValidRole(command.getRoleName())) {
            throw new IllegalArgumentException(String.format("Invalid role: %s. Valid roles: %s", command.getRoleName(), RoleConstants.VALID_ROLES));
        }

        // 2. Load target user
        User targetUser =
                userRepository.findById(command.getUserId()).orElseThrow(() -> new UserNotFoundException(String.format("User not found: %s", command.getUserId().getValue())));

        // 3. Get current user (assigner) from security context
        UserId currentUserId = securityContextPort.getCurrentUserId();
        List<String> currentUserRoles = securityContextPort.getCurrentUserRoles();
        TenantId currentUserTenantId = securityContextPort.getCurrentUserTenantId();

        // 4. Validate assignment permissions
        roleAssignmentValidator.validateRoleAssignment(currentUserId, currentUserRoles, currentUserTenantId, targetUser, command.getRoleName());

        // 5. Check if user already has this role
        if (targetUser.getKeycloakUserId().isPresent()) {
            List<String> existingRoles = authenticationService.getUserRoles(targetUser.getKeycloakUserId().get());
            if (existingRoles.contains(command.getRoleName())) {
                log.warn("User already has role: userId={}, role={}", targetUser.getId().getValue(), command.getRoleName());
                return; // Idempotent: already assigned
            }
        }

        // 6. Assign role in Keycloak
        if (targetUser.getKeycloakUserId().isPresent()) {
            try {
                authenticationService.assignRole(targetUser.getKeycloakUserId().get(), command.getRoleName());
            } catch (Exception e) {
                log.error("Failed to assign role in Keycloak: {}", e.getMessage(), e);
                throw new RoleAssignmentException(String.format("Failed to assign role: %s", e.getMessage()), e);
            }
        } else {
            throw new IllegalStateException("Cannot assign role: user has no Keycloak ID");
        }

        // 7. Update last modified timestamp
        // Note: Role information is stored in Keycloak, not in domain model
        // We just update the timestamp to track changes
        userRepository.save(targetUser);

        // 8. Publish event with metadata (including assignedBy) after transaction commit
        EventMetadata metadata = EventMetadata.builder().userId(currentUserId.getValue()).build();
        UserRoleAssignedEvent event = new UserRoleAssignedEvent(targetUser.getId(), targetUser.getTenantId(), command.getRoleName(), metadata);
        publishEventsAfterCommit(Collections.singletonList(event));

        log.info("Role assigned successfully: userId={}, role={}, assignedBy={}", targetUser.getId().getValue(), command.getRoleName(), currentUserId.getValue());
    }

    /**
     * Publishes domain events after transaction commit to avoid race conditions.
     * <p>
     * Events are published using TransactionSynchronizationManager to ensure they are only published after the database transaction has successfully committed. This prevents race
     * conditions where event listeners consume events before the aggregate is visible in the database.
     *
     * @param domainEvents Domain events to publish
     */
    private void publishEventsAfterCommit(List<com.ccbsa.common.domain.DomainEvent<?>> domainEvents) {
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

