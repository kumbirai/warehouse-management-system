package com.ccbsa.wms.user.application.api.controller;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ccbsa.common.application.api.ApiResponse;
import com.ccbsa.common.application.api.ApiResponseBuilder;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.common.domain.valueobject.UserId;
import com.ccbsa.wms.common.security.TenantContext;
import com.ccbsa.wms.user.application.api.dto.AssignRoleRequest;
import com.ccbsa.wms.user.application.api.dto.CreateUserRequest;
import com.ccbsa.wms.user.application.api.dto.CreateUserResponse;
import com.ccbsa.wms.user.application.api.dto.UpdateUserProfileRequest;
import com.ccbsa.wms.user.application.api.mapper.UserMapper;
import com.ccbsa.wms.user.application.service.command.ActivateUserCommandHandler;
import com.ccbsa.wms.user.application.service.command.AssignUserRoleCommandHandler;
import com.ccbsa.wms.user.application.service.command.CreateUserCommandHandler;
import com.ccbsa.wms.user.application.service.command.DeactivateUserCommandHandler;
import com.ccbsa.wms.user.application.service.command.RemoveUserRoleCommandHandler;
import com.ccbsa.wms.user.application.service.command.SuspendUserCommandHandler;
import com.ccbsa.wms.user.application.service.command.UpdateUserProfileCommandHandler;
import com.ccbsa.wms.user.application.service.command.dto.ActivateUserCommand;
import com.ccbsa.wms.user.application.service.command.dto.AssignUserRoleCommand;
import com.ccbsa.wms.user.application.service.command.dto.CreateUserCommand;
import com.ccbsa.wms.user.application.service.command.dto.CreateUserResult;
import com.ccbsa.wms.user.application.service.command.dto.DeactivateUserCommand;
import com.ccbsa.wms.user.application.service.command.dto.RemoveUserRoleCommand;
import com.ccbsa.wms.user.application.service.command.dto.SuspendUserCommand;
import com.ccbsa.wms.user.application.service.command.dto.UpdateUserProfileCommand;
import com.ccbsa.wms.user.application.service.exception.UserNotFoundException;
import com.ccbsa.wms.user.application.service.port.repository.UserRepository;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * REST Controller: UserCommandController
 * <p>
 * Handles user management command operations (write operations).
 */
@RestController
@RequestMapping("/users")
@Tag(name = "User Commands",
        description = "User management command operations")
public class UserCommandController {
    private final CreateUserCommandHandler createUserCommandHandler;
    private final UpdateUserProfileCommandHandler updateUserProfileCommandHandler;
    private final ActivateUserCommandHandler activateUserCommandHandler;
    private final DeactivateUserCommandHandler deactivateUserCommandHandler;
    private final SuspendUserCommandHandler suspendUserCommandHandler;
    private final AssignUserRoleCommandHandler assignUserRoleCommandHandler;
    private final RemoveUserRoleCommandHandler removeUserRoleCommandHandler;
    private final UserMapper mapper;
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
            justification = "UserRepository is a Spring-managed interface. The field is final and the reference is immutable in Spring's " + "dependency injection context.")
    private final UserRepository userRepository;

    public UserCommandController(CreateUserCommandHandler createUserCommandHandler, UpdateUserProfileCommandHandler updateUserProfileCommandHandler,
                                 ActivateUserCommandHandler activateUserCommandHandler,
                                 DeactivateUserCommandHandler deactivateUserCommandHandler, SuspendUserCommandHandler suspendUserCommandHandler,
                                 AssignUserRoleCommandHandler assignUserRoleCommandHandler,
                                 RemoveUserRoleCommandHandler removeUserRoleCommandHandler, UserMapper mapper, UserRepository userRepository) {
        this.createUserCommandHandler = createUserCommandHandler;
        this.updateUserProfileCommandHandler = updateUserProfileCommandHandler;
        this.activateUserCommandHandler = activateUserCommandHandler;
        this.deactivateUserCommandHandler = deactivateUserCommandHandler;
        this.suspendUserCommandHandler = suspendUserCommandHandler;
        this.assignUserRoleCommandHandler = assignUserRoleCommandHandler;
        this.removeUserRoleCommandHandler = removeUserRoleCommandHandler;
        this.mapper = mapper;
        this.userRepository = userRepository;
    }

    @PostMapping
    @Operation(summary = "Create User",
            description = "Creates a new user in the system")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'TENANT_ADMIN')")
    public ResponseEntity<ApiResponse<CreateUserResponse>> createUser(
            @RequestHeader(value = "X-Tenant-Id",
                    required = false) String tenantId,
            @Valid
            @RequestBody
            CreateUserRequest request) {
        // Extract user roles to determine tenant isolation rules
        boolean isTenantAdmin = isTenantAdmin();

        // Resolve tenantId: TENANT_ADMIN can only create users in their own tenant
        String resolvedTenantId = tenantId;
        if (isTenantAdmin) {
            // TENANT_ADMIN can only create users in their own tenant (from TenantContext)
            TenantId contextTenantId = TenantContext.getTenantId();
            if (contextTenantId != null) {
                resolvedTenantId = contextTenantId.getValue();
            } else {
                throw new IllegalStateException("Tenant context not set for TENANT_ADMIN user");
            }
        } else {
            // SYSTEM_ADMIN can create users in any tenant (tenantId from header or request)
            // Fall back to request body if header is not provided
            if (resolvedTenantId == null) {
                resolvedTenantId = request.getTenantId();
            }
            if (resolvedTenantId == null || resolvedTenantId.trim()
                    .isEmpty()) {
                throw new IllegalArgumentException("TenantId is required when creating a user");
            }
        }

        // Set TenantContext before saving so Hibernate can resolve the correct tenant schema
        // This is critical for schema-per-tenant pattern
        TenantId tenantIdToSet = TenantId.of(resolvedTenantId);
        TenantId previousTenantId = TenantContext.getTenantId();

        // Log context setting for debugging
        Logger logger = LoggerFactory.getLogger(UserCommandController.class);
        logger.info("Setting TenantContext to '{}' before creating user (previous: {})", tenantIdToSet.getValue(), previousTenantId != null ? previousTenantId.getValue() : "null");

        try {
            TenantContext.setTenantId(tenantIdToSet);

            // Verify TenantContext is set correctly
            TenantId verifyTenantId = TenantContext.getTenantId();
            if (verifyTenantId == null || !verifyTenantId.getValue()
                    .equals(resolvedTenantId)) {
                logger.error("TenantContext verification failed! Expected: '{}', Actual: {}", resolvedTenantId, verifyTenantId != null ? verifyTenantId.getValue() : "null");
                throw new IllegalStateException("Failed to set TenantContext correctly");
            }
            logger.debug("TenantContext verified: '{}'", verifyTenantId.getValue());

            CreateUserCommand command = mapper.toCreateUserCommand(request, resolvedTenantId);
            CreateUserResult result = createUserCommandHandler.handle(command);
            CreateUserResponse response = mapper.toCreateUserResponse(result);
            return ApiResponseBuilder.created(response);
        } finally {
            // Restore previous tenant context or clear if it was null
            if (previousTenantId != null) {
                TenantContext.setTenantId(previousTenantId);
                logger.debug("Restored TenantContext to '{}'", previousTenantId.getValue());
            } else {
                TenantContext.clear();
                logger.debug("Cleared TenantContext");
            }
        }
    }

    /**
     * Checks if the current user has TENANT_ADMIN role.
     *
     * @return true if user has TENANT_ADMIN role, false otherwise
     */
    private boolean isTenantAdmin() {
        Authentication authentication = SecurityContextHolder.getContext()
                .getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt)) {
            return false;
        }

        Jwt jwt = (Jwt) authentication.getPrincipal();
        return hasRole(jwt, "TENANT_ADMIN");
    }

    /**
     * Checks if the JWT token contains the specified role.
     *
     * @param jwt  The JWT token
     * @param role The role to check for
     * @return true if the role is present, false otherwise
     */
    private boolean hasRole(Jwt jwt, String role) {
        Object realmAccess = jwt.getClaim("realm_access");
        if (realmAccess instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> realmAccessMap = (Map<String, Object>) realmAccess;
            Object rolesObj = realmAccessMap.get("roles");
            if (rolesObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<String> roles = (List<String>) rolesObj;
                return roles.contains(role);
            }
        }
        return false;
    }

    @PutMapping("/{id}/profile")
    @Operation(summary = "Update User Profile",
            description = "Updates user profile information")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'TENANT_ADMIN') or (hasRole('USER') and #id == authentication.principal.claims['sub'])")
    public ResponseEntity<ApiResponse<Void>> updateUserProfile(
            @PathVariable String id,
            @RequestHeader(value = "X-Tenant-Id",
                    required = false) String tenantId,
            @Valid
            @RequestBody
            UpdateUserProfileRequest request) {
        return executeWithTenantContext(tenantId, UserId.of(id), () -> {
            UpdateUserProfileCommand command = mapper.toUpdateUserProfileCommand(id, request);
            updateUserProfileCommandHandler.handle(command);
            return ApiResponseBuilder.noContent();
        });
    }

    /**
     * Executes an operation with TenantContext set appropriately.
     * <p>
     * For TENANT_ADMIN: Uses TenantContext from interceptor (their own tenant) For SYSTEM_ADMIN: Uses tenantId from header, or finds user across schemas if not provided
     *
     * @param tenantId  Tenant ID from header (for SYSTEM_ADMIN, optional)
     * @param userId    User ID to find tenant for (for SYSTEM_ADMIN when tenantId not provided)
     * @param operation Operation to execute
     * @return Response entity
     */
    private <T> ResponseEntity<ApiResponse<T>> executeWithTenantContext(String tenantId, UserId userId, Supplier<ResponseEntity<ApiResponse<T>>> operation) {
        boolean isTenantAdmin = isTenantAdmin();
        Logger logger = LoggerFactory.getLogger(UserCommandController.class);

        // Resolve tenantId: TENANT_ADMIN uses their own tenant from TenantContext
        String resolvedTenantId = tenantId;
        if (isTenantAdmin) {
            TenantId contextTenantId = TenantContext.getTenantId();
            if (contextTenantId != null) {
                resolvedTenantId = contextTenantId.getValue();
            } else {
                throw new IllegalStateException("Tenant context not set for TENANT_ADMIN user");
            }
        } else {
            // SYSTEM_ADMIN: If tenantId not provided, find user across schemas to get their tenantId
            if (resolvedTenantId == null || resolvedTenantId.trim()
                    .isEmpty()) {
                if (userId != null) {
                    logger.debug("TenantId not provided for SYSTEM_ADMIN, finding user across schemas: userId={}", userId.getValue());
                    var user = userRepository.findByIdAcrossTenants(userId)
                            .orElseThrow(() -> new UserNotFoundException(String.format("User not found: %s", userId.getValue())));
                    resolvedTenantId = user.getTenantId()
                            .getValue();
                    logger.debug("Found user tenantId: {}", resolvedTenantId);
                } else {
                    throw new IllegalArgumentException("X-Tenant-Id header is required for SYSTEM_ADMIN operations when userId is not available");
                }
            }
        }

        // Set TenantContext before executing operation
        TenantId tenantIdToSet = TenantId.of(resolvedTenantId);
        TenantId previousTenantId = TenantContext.getTenantId();

        logger.debug("Setting TenantContext to '{}' for operation (previous: {})", tenantIdToSet.getValue(), previousTenantId != null ? previousTenantId.getValue() : "null");

        try {
            TenantContext.setTenantId(tenantIdToSet);
            return operation.get();
        } finally {
            // Restore previous tenant context or clear if it was null
            if (previousTenantId != null) {
                TenantContext.setTenantId(previousTenantId);
            } else {
                TenantContext.clear();
            }
        }
    }

    @PutMapping("/{id}/activate")
    @Operation(summary = "Activate User",
            description = "Activates a user account")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'TENANT_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> activateUser(
            @PathVariable String id,
            @RequestHeader(value = "X-Tenant-Id",
                    required = false) String tenantId) {
        return executeWithTenantContext(tenantId, UserId.of(id), () -> {
            ActivateUserCommand command = new ActivateUserCommand(UserId.of(id));
            activateUserCommandHandler.handle(command);
            return ApiResponseBuilder.noContent();
        });
    }

    @PutMapping("/{id}/deactivate")
    @Operation(summary = "Deactivate User",
            description = "Deactivates a user account")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'TENANT_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deactivateUser(
            @PathVariable String id,
            @RequestHeader(value = "X-Tenant-Id",
                    required = false) String tenantId) {
        return executeWithTenantContext(tenantId, UserId.of(id), () -> {
            DeactivateUserCommand command = new DeactivateUserCommand(UserId.of(id));
            deactivateUserCommandHandler.handle(command);
            return ApiResponseBuilder.noContent();
        });
    }

    @PutMapping("/{id}/suspend")
    @Operation(summary = "Suspend User",
            description = "Suspends a user account")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'TENANT_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> suspendUser(
            @PathVariable String id,
            @RequestHeader(value = "X-Tenant-Id",
                    required = false) String tenantId) {
        return executeWithTenantContext(tenantId, UserId.of(id), () -> {
            SuspendUserCommand command = new SuspendUserCommand(UserId.of(id));
            suspendUserCommandHandler.handle(command);
            return ApiResponseBuilder.noContent();
        });
    }

    @PostMapping("/{id}/roles")
    @Operation(summary = "Assign Role",
            description = "Assigns a role to a user")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'TENANT_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> assignRole(
            @PathVariable String id,
            @RequestHeader(value = "X-Tenant-Id",
                    required = false) String tenantId,
            @Valid
            @RequestBody
            AssignRoleRequest request) {
        return executeWithTenantContext(tenantId, UserId.of(id), () -> {
            AssignUserRoleCommand command = mapper.toAssignUserRoleCommand(id, request);
            assignUserRoleCommandHandler.handle(command);
            return ApiResponseBuilder.noContent();
        });
    }

    @DeleteMapping("/{id}/roles/{roleId}")
    @Operation(summary = "Remove Role",
            description = "Removes a role from a user")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'TENANT_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> removeRole(
            @PathVariable String id,
            @PathVariable String roleId,
            @RequestHeader(value = "X-Tenant-Id",
                    required = false) String tenantId) {
        return executeWithTenantContext(tenantId, UserId.of(id), () -> {
            RemoveUserRoleCommand command = mapper.toRemoveUserRoleCommand(id, roleId);
            removeUserRoleCommandHandler.handle(command);
            return ApiResponseBuilder.noContent();
        });
    }
}

