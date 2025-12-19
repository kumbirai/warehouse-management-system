package com.ccbsa.wms.gateway.api.fixture;

import com.ccbsa.wms.gateway.api.dto.AssignRoleRequest;
import com.ccbsa.wms.gateway.api.dto.CreateUserRequest;

/**
 * Builder for creating user test data.
 */
public class UserTestDataBuilder {

    public static CreateUserRequest buildCreateUserRequest(String tenantId) {
        return CreateUserRequest.builder()
                .tenantId(tenantId)
                .username(TestData.username())
                .emailAddress(TestData.email())
                .password(TestData.password())
                .firstName(TestData.firstName())
                .lastName(TestData.lastName())
                .build();
    }

    public static CreateUserRequest buildCreateUserRequestWithUsername(String username, String tenantId) {
        return CreateUserRequest.builder()
                .tenantId(tenantId)
                .username(username)
                .emailAddress(TestData.email())
                .password(TestData.password())
                .firstName(TestData.firstName())
                .lastName(TestData.lastName())
                .build();
    }

    public static CreateUserRequest buildCreateUserRequestWithRole(String tenantId, String role) {
        // Note: Role assignment happens after user creation
        return buildCreateUserRequest(tenantId);
    }

    public static AssignRoleRequest buildAssignRoleRequest(String roleName) {
        // Note: roleId is the same as roleName in this system
        return AssignRoleRequest.builder()
                .roleId(roleName)
                .build();
    }

    /**
     * All available roles in the system.
     */
    public static class Roles {
        public static final String SYSTEM_ADMIN = "SYSTEM_ADMIN";
        public static final String TENANT_ADMIN = "TENANT_ADMIN";
        public static final String WAREHOUSE_MANAGER = "WAREHOUSE_MANAGER";
        public static final String STOCK_MANAGER = "STOCK_MANAGER";
        public static final String LOCATION_MANAGER = "LOCATION_MANAGER";
        public static final String RECONCILIATION_MANAGER = "RECONCILIATION_MANAGER";
        public static final String RETURNS_MANAGER = "RETURNS_MANAGER";
        public static final String PICKER = "PICKER";
        public static final String STOCK_CLERK = "STOCK_CLERK";
        public static final String RECONCILIATION_CLERK = "RECONCILIATION_CLERK";
        public static final String RETURNS_CLERK = "RETURNS_CLERK";
        public static final String OPERATOR = "OPERATOR";
        public static final String VIEWER = "VIEWER";
    }
}

