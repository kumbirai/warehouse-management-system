package com.ccbsa.wms.user.application.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
import com.ccbsa.common.domain.valueobject.UserId;
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
@Tag(name = "User Commands", description = "User management command operations")
public class UserCommandController {
    private final CreateUserCommandHandler createUserCommandHandler;
    private final UpdateUserProfileCommandHandler updateUserProfileCommandHandler;
    private final ActivateUserCommandHandler activateUserCommandHandler;
    private final DeactivateUserCommandHandler deactivateUserCommandHandler;
    private final SuspendUserCommandHandler suspendUserCommandHandler;
    private final AssignUserRoleCommandHandler assignUserRoleCommandHandler;
    private final RemoveUserRoleCommandHandler removeUserRoleCommandHandler;
    private final UserMapper mapper;

    public UserCommandController(
            CreateUserCommandHandler createUserCommandHandler,
            UpdateUserProfileCommandHandler updateUserProfileCommandHandler,
            ActivateUserCommandHandler activateUserCommandHandler,
            DeactivateUserCommandHandler deactivateUserCommandHandler,
            SuspendUserCommandHandler suspendUserCommandHandler,
            AssignUserRoleCommandHandler assignUserRoleCommandHandler,
            RemoveUserRoleCommandHandler removeUserRoleCommandHandler,
            UserMapper mapper) {
        this.createUserCommandHandler = createUserCommandHandler;
        this.updateUserProfileCommandHandler = updateUserProfileCommandHandler;
        this.activateUserCommandHandler = activateUserCommandHandler;
        this.deactivateUserCommandHandler = deactivateUserCommandHandler;
        this.suspendUserCommandHandler = suspendUserCommandHandler;
        this.assignUserRoleCommandHandler = assignUserRoleCommandHandler;
        this.removeUserRoleCommandHandler = removeUserRoleCommandHandler;
        this.mapper = mapper;
    }

    @PostMapping
    @Operation(summary = "Create User", description = "Creates a new user in the system")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'TENANT_ADMIN')")
    public ResponseEntity<ApiResponse<CreateUserResponse>> createUser(
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantId,
            @Valid @RequestBody CreateUserRequest request) {
        CreateUserCommand command = mapper.toCreateUserCommand(request, tenantId);
        CreateUserResult result = createUserCommandHandler.handle(command);
        CreateUserResponse response = mapper.toCreateUserResponse(result);
        return ApiResponseBuilder.created(response);
    }

    @PutMapping("/{id}/profile")
    @Operation(summary = "Update User Profile", description = "Updates user profile information")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'TENANT_ADMIN') or (hasRole('USER') and #id == authentication.principal.claims['sub'])")
    public ResponseEntity<ApiResponse<Void>> updateUserProfile(
            @PathVariable String id,
            @Valid @RequestBody UpdateUserProfileRequest request) {
        UpdateUserProfileCommand command = mapper.toUpdateUserProfileCommand(id, request);
        updateUserProfileCommandHandler.handle(command);
        return ApiResponseBuilder.noContent();
    }

    @PutMapping("/{id}/activate")
    @Operation(summary = "Activate User", description = "Activates a user account")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'TENANT_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> activateUser(@PathVariable String id) {
        ActivateUserCommand command = new ActivateUserCommand(UserId.of(id));
        activateUserCommandHandler.handle(command);
        return ApiResponseBuilder.noContent();
    }

    @PutMapping("/{id}/deactivate")
    @Operation(summary = "Deactivate User", description = "Deactivates a user account")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'TENANT_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deactivateUser(@PathVariable String id) {
        DeactivateUserCommand command = new DeactivateUserCommand(UserId.of(id));
        deactivateUserCommandHandler.handle(command);
        return ApiResponseBuilder.noContent();
    }

    @PutMapping("/{id}/suspend")
    @Operation(summary = "Suspend User", description = "Suspends a user account")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'TENANT_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> suspendUser(@PathVariable String id) {
        SuspendUserCommand command = new SuspendUserCommand(UserId.of(id));
        suspendUserCommandHandler.handle(command);
        return ApiResponseBuilder.noContent();
    }

    @PostMapping("/{id}/roles")
    @Operation(summary = "Assign Role", description = "Assigns a role to a user")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'TENANT_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> assignRole(
            @PathVariable String id,
            @Valid @RequestBody AssignRoleRequest request) {
        AssignUserRoleCommand command = mapper.toAssignUserRoleCommand(id, request);
        assignUserRoleCommandHandler.handle(command);
        return ApiResponseBuilder.noContent();
    }

    @DeleteMapping("/{id}/roles/{roleId}")
    @Operation(summary = "Remove Role", description = "Removes a role from a user")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'TENANT_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> removeRole(
            @PathVariable String id,
            @PathVariable String roleId) {
        RemoveUserRoleCommand command = mapper.toRemoveUserRoleCommand(id, roleId);
        removeUserRoleCommandHandler.handle(command);
        return ApiResponseBuilder.noContent();
    }
}

