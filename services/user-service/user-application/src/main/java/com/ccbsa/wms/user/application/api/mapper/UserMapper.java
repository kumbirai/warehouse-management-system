package com.ccbsa.wms.user.application.api.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.common.domain.valueobject.UserId;
import com.ccbsa.wms.user.application.api.dto.AssignRoleRequest;
import com.ccbsa.wms.user.application.api.dto.CreateUserRequest;
import com.ccbsa.wms.user.application.api.dto.CreateUserResponse;
import com.ccbsa.wms.user.application.api.dto.UpdateUserProfileRequest;
import com.ccbsa.wms.user.application.api.dto.UserResponse;
import com.ccbsa.wms.user.application.service.command.dto.AssignUserRoleCommand;
import com.ccbsa.wms.user.application.service.command.dto.CreateUserCommand;
import com.ccbsa.wms.user.application.service.command.dto.CreateUserResult;
import com.ccbsa.wms.user.application.service.command.dto.RemoveUserRoleCommand;
import com.ccbsa.wms.user.application.service.command.dto.UpdateUserProfileCommand;
import com.ccbsa.wms.user.application.service.query.dto.GetUserQuery;
import com.ccbsa.wms.user.application.service.query.dto.GetUserQueryResult;
import com.ccbsa.wms.user.application.service.query.dto.ListUsersQuery;
import com.ccbsa.wms.user.application.service.query.dto.ListUsersQueryResult;
import com.ccbsa.wms.user.domain.core.valueobject.UserStatus;

/**
 * Mapper for converting between API DTOs and application service DTOs.
 */
@Component
public class UserMapper {

    // Command mappers
    public CreateUserCommand toCreateUserCommand(CreateUserRequest request, String tenantId) {
        return new CreateUserCommand(tenantId != null ? tenantId : request.getTenantId(), request.getUsername(), request.getEmailAddress(), request.getPassword(),
                request.getFirstName(), request.getLastName(), request.getRoles());
    }

    public CreateUserResponse toCreateUserResponse(CreateUserResult result) {
        return new CreateUserResponse(result.getUserId(), result.isSuccess(), result.getMessage());
    }

    public UpdateUserProfileCommand toUpdateUserProfileCommand(String userId, UpdateUserProfileRequest request) {
        return new UpdateUserProfileCommand(UserId.of(userId), request.getEmailAddress(), request.getFirstName(), request.getLastName());
    }

    public AssignUserRoleCommand toAssignUserRoleCommand(String userId, AssignRoleRequest request) {
        return new AssignUserRoleCommand(UserId.of(userId), request.getRoleId());
    }

    public RemoveUserRoleCommand toRemoveUserRoleCommand(String userId, String roleId) {
        return new RemoveUserRoleCommand(UserId.of(userId), roleId);
    }

    // Query mappers
    public GetUserQuery toGetUserQuery(String userId) {
        return new GetUserQuery(UserId.of(userId));
    }

    public ListUsersQuery toListUsersQuery(String tenantId, String status, Integer page, Integer size) {
        return new ListUsersQuery(tenantId != null ? TenantId.of(tenantId) : null, status != null ? UserStatus.valueOf(status) : null, page, size);
    }

    public List<UserResponse> toUserResponseList(ListUsersQueryResult result) {
        return result.getItems()
                .stream()
                .map(this::toUserResponse)
                .toList();
    }

    public UserResponse toUserResponse(GetUserQueryResult result) {
        UserResponse response = new UserResponse();
        response.setUserId(result.getUserId()
                .getValue());
        response.setTenantId(result.getTenantId()
                .getValue());
        response.setUsername(result.getUsername());
        response.setEmailAddress(result.getEmail());
        response.setFirstName(result.getFirstName());
        response.setLastName(result.getLastName());
        response.setStatus(result.getStatus()
                .name());
        response.setKeycloakUserId(result.getKeycloakUserId());
        response.setRoles(result.getRoles());
        response.setCreatedAt(result.getCreatedAt());
        response.setLastModifiedAt(result.getLastModifiedAt());
        return response;
    }
}

