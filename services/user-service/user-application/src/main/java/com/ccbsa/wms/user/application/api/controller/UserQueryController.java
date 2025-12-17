package com.ccbsa.wms.user.application.api.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ccbsa.common.application.api.ApiMeta;
import com.ccbsa.common.application.api.ApiResponse;
import com.ccbsa.common.application.api.ApiResponseBuilder;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.common.security.TenantContext;
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
@Tag(name = "User Queries",
        description = "User management query operations")
public class UserQueryController {
    private final GetUserQueryHandler getUserQueryHandler;
    private final ListUsersQueryHandler listUsersQueryHandler;
    private final UserMapper mapper;

    public UserQueryController(GetUserQueryHandler getUserQueryHandler, ListUsersQueryHandler listUsersQueryHandler, UserMapper mapper) {
        this.getUserQueryHandler = getUserQueryHandler;
        this.listUsersQueryHandler = listUsersQueryHandler;
        this.mapper = mapper;
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get User by ID",
            description = "Retrieves a user by ID")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'TENANT_ADMIN') or (hasRole('USER') and #id == authentication.principal.claims['sub'])")
    public ResponseEntity<ApiResponse<UserResponse>> getUser(
            @PathVariable String id) {
        GetUserQueryResult result = getUserQueryHandler.handle(mapper.toGetUserQuery(id));
        UserResponse response = mapper.toUserResponse(result);
        return ApiResponseBuilder.ok(response);
    }

    @GetMapping
    @Operation(summary = "List Users",
            description = "Lists users with optional filtering and pagination")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'TENANT_ADMIN')")
    public ResponseEntity<ApiResponse<List<UserResponse>>> listUsers(
            @RequestHeader(value = "X-Tenant-Id",
                    required = false) String tenantId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false,
                    defaultValue = "1") Integer page,
            @RequestParam(required = false,
                    defaultValue = "20") Integer size) {
        // Extract user roles from SecurityContext
        boolean isTenantAdmin = isTenantAdmin();

        // Enforce tenant isolation based on role
        String resolvedTenantId = tenantId;
        if (isTenantAdmin) {
            // TENANT_ADMIN can only query their own tenant (from TenantContext, not request header)
            TenantId contextTenantId = TenantContext.getTenantId();
            if (contextTenantId != null) {
                resolvedTenantId = contextTenantId.getValue();
            } else {
                // If tenant context is not set, this is a security issue
                throw new IllegalStateException("Tenant context not set for TENANT_ADMIN user");
            }
        }
        // SYSTEM_ADMIN can use tenantId from header (or null for all tenants)

        // Convert 1-indexed page from frontend to 0-indexed for backend processing
        // Frontend uses 1-indexed pages (page 1 = first page), backend uses 0-indexed for array slicing
        int zeroIndexedPage = page != null && page > 0 ? page - 1 : 0;

        ListUsersQueryResult result = listUsersQueryHandler.handle(mapper.toListUsersQuery(resolvedTenantId, status, zeroIndexedPage, size));
        List<UserResponse> responses = mapper.toUserResponseList(result);

        // Log result for debugging
        org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(UserQueryController.class);
        logger.info("ListUsers query result: tenantId={}, totalCount={}, itemsCount={}, page={}, size={}", 
            resolvedTenantId, result.getTotalCount(), responses.size(), page, size);

        // Build pagination metadata (using 1-indexed page for response)
        ApiMeta.Pagination pagination = ApiMeta.Pagination.of(page != null && page > 0 ? page : 1, size != null && size > 0 ? size : 20, result.getTotalCount());
        ApiMeta meta = ApiMeta.builder()
                .pagination(pagination)
                .build();

        return ApiResponseBuilder.ok(responses, null, meta);
    }

    /**
     * Checks if the current user has TENANT_ADMIN role.
     * <p>
     * Uses Spring Security authorities (set by GatewayRoleHeaderAuthenticationFilter from X-Role header)
     * rather than reading directly from JWT token, following the gateway-trust architectural pattern.
     *
     * @return true if user has TENANT_ADMIN role, false otherwise
     */
    private boolean isTenantAdmin() {
        Authentication authentication = SecurityContextHolder.getContext()
                .getAuthentication();
        if (authentication == null) {
            return false;
        }

        // Check Spring Security authorities (set by GatewayRoleHeaderAuthenticationFilter)
        // Authorities have ROLE_ prefix, so check for ROLE_TENANT_ADMIN
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_TENANT_ADMIN"));
    }
}

