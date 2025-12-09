package com.ccbsa.wms.user.application.api.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ccbsa.common.application.api.ApiResponse;
import com.ccbsa.common.application.api.ApiResponseBuilder;
import com.ccbsa.wms.user.application.api.dto.UserResponse;
import com.ccbsa.wms.user.application.api.mapper.UserMapper;
import com.ccbsa.wms.user.application.service.query.GetUserQueryHandler;
import com.ccbsa.wms.user.application.service.query.ListUsersQueryHandler;
import com.ccbsa.wms.user.application.service.query.dto.GetUserQueryResult;
import com.ccbsa.wms.user.application.service.query.dto.ListUsersQueryResult;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST Controller: UserQueryController
 * <p>
 * Handles user management query operations (read operations).
 */
@RestController
@RequestMapping("/users")
@Tag(name = "User Queries", description = "User management query operations")
public class UserQueryController {
    private final GetUserQueryHandler getUserQueryHandler;
    private final ListUsersQueryHandler listUsersQueryHandler;
    private final UserMapper mapper;

    public UserQueryController(
            GetUserQueryHandler getUserQueryHandler,
            ListUsersQueryHandler listUsersQueryHandler,
            UserMapper mapper) {
        this.getUserQueryHandler = getUserQueryHandler;
        this.listUsersQueryHandler = listUsersQueryHandler;
        this.mapper = mapper;
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get User by ID", description = "Retrieves a user by ID")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'TENANT_ADMIN') or (hasRole('USER') and #id == authentication.principal.claims['sub'])")
    public ResponseEntity<ApiResponse<UserResponse>> getUser(
            @PathVariable String id) {
        GetUserQueryResult result = getUserQueryHandler.handle(mapper.toGetUserQuery(id));
        UserResponse response = mapper.toUserResponse(result);
        return ApiResponseBuilder.ok(response);
    }

    @GetMapping
    @Operation(summary = "List Users", description = "Lists users with optional filtering and pagination")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'TENANT_ADMIN')")
    public ResponseEntity<ApiResponse<List<UserResponse>>> listUsers(
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "20") Integer size) {
        ListUsersQueryResult result = listUsersQueryHandler.handle(
                mapper.toListUsersQuery(tenantId, status, page, size));
        List<UserResponse> responses = mapper.toUserResponseList(result);
        return ApiResponseBuilder.ok(responses);
    }
}

