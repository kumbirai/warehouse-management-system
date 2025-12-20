package com.ccbsa.wms.gateway.api.fixture;

import java.util.Locale;
import java.util.UUID;

import com.ccbsa.wms.gateway.api.dto.AssignRoleRequest;
import com.ccbsa.wms.gateway.api.dto.CreateUserRequest;

/**
 * Builder for creating user test data.
 * Generates user data starting with name and surname, then derives email and username from them.
 */
public class UserTestDataBuilder {

    public static CreateUserRequest buildCreateUserRequestWithUsername(String username, String tenantId) {
        // Generate name and surname first
        String firstName = TestData.firstName();
        String lastName = TestData.lastName();

        return CreateUserRequest.builder()
                .tenantId(tenantId)
                .username(username)
                .emailAddress(generateEmailFromUsername(username))
                .password(TestData.password())
                .firstName(firstName)
                .lastName(lastName)
                .build();
    }

    /**
     * Generates an email address using the username.
     */
    private static String generateEmailFromUsername(String username) {
        return String.format("%s@%s", username, TestData.domain());
    }

    public static CreateUserRequest buildCreateUserRequestWithRole(String tenantId, String role) {
        // Note: Role assignment happens after user creation
        return buildCreateUserRequest(tenantId);
    }

    public static CreateUserRequest buildCreateUserRequest(String tenantId) {
        // Generate name and surname first
        String firstName = TestData.firstName();
        String lastName = TestData.lastName();

        // Derive username from name and surname, then use it for email
        String username = generateUsernameFromName(firstName, lastName);
        String email = generateEmailFromUsername(username);

        return CreateUserRequest.builder()
                .tenantId(tenantId)
                .username(username)
                .emailAddress(email)
                .password(TestData.password())
                .firstName(firstName)
                .lastName(lastName)
                .build();
    }

    /**
     * Generates a username from first name and last name.
     */
    private static String generateUsernameFromName(String firstName, String lastName) {
        String base = (firstName + "." + lastName).toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9.]", "");
        String unique = UUID.randomUUID().toString().substring(0, 3).replace("-", "");
        return base + "." + unique;
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

