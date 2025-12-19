# UserManagementTest Implementation Plan

## Overview

`UserManagementTest` validates all user management functionality through the gateway service. Tests authenticate as both SYSTEM_ADMIN and TENANT_ADMIN to verify user CRUD
operations, role assignment, user lifecycle management (activate, deactivate, suspend), and tenant-scoped access control.

---

## Objectives

1. **User Creation**: Test user creation by SYSTEM_ADMIN and TENANT_ADMIN with tenant isolation
2. **User Lifecycle Management**: Test activation, deactivation, and suspension workflows
3. **Role Assignment**: Test assigning and removing roles with hierarchy validation
4. **User Profile Management**: Test profile updates (self-service and admin)
5. **User Queries**: Test list users with pagination, filtering by status and tenant
6. **Authorization Checks**: Verify role-based access control for user operations
7. **Tenant Isolation**: Validate TENANT_ADMIN can only manage users in own tenant
8. **Event Publishing**: Validate user events (UserCreatedEvent, UserRoleAssignedEvent, etc.)
9. **Multi-Role Support**: Test users with multiple roles
10. **User Context Validation**: Verify JWT token reflects user roles correctly

---

## Test Scenarios

### Phase 1: SYSTEM_ADMIN Tests

#### Setup: Create Test Tenant

```java
@BeforeAll
public static void setupTestData() {
    // Login as SYSTEM_ADMIN
    systemAdminAuth = loginAsSystemAdmin();

    // Find first active tenant or create one
    testTenant = findOrCreateActiveTenant();
}
```

---

### 1. User Creation Tests (SYSTEM_ADMIN)

#### Test: Create User Successfully (SYSTEM_ADMIN)

- **Setup**: Login as SYSTEM_ADMIN, get active tenant
- **Action**: POST `/api/v1/users` with valid user data
- **Request Body**:
  ```json
  {
    "username": "john.doe",
    "email": "john.doe@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "tenantId": "{tenantId}"
  }
  ```
- **Assertions**:
    - Status: 201 CREATED
    - Response contains `userId`, `username`, `email`, `status=ACTIVE`
    - UserCreatedEvent published
    - User appears in list users query
    - User created in correct tenant schema

#### Test: Create User with Invalid Email

- **Setup**: Login as SYSTEM_ADMIN
- **Action**: POST `/api/v1/users` with invalid email format
- **Assertions**:
    - Status: 400 BAD REQUEST
    - Validation error for email format

#### Test: Create User with Duplicate Username

- **Setup**: Login as SYSTEM_ADMIN, create user "john.doe"
- **Action**: POST `/api/v1/users` with same username
- **Assertions**:
    - Status: 400 BAD REQUEST or 409 CONFLICT
    - Error message indicates duplicate username

#### Test: Create User with Missing Required Fields

- **Setup**: Login as SYSTEM_ADMIN
- **Action**: POST `/api/v1/users` with missing `username` or `email`
- **Assertions**:
    - Status: 400 BAD REQUEST
    - Validation errors for missing fields

#### Test: Create User in Non-Existent Tenant

- **Setup**: Login as SYSTEM_ADMIN
- **Action**: POST `/api/v1/users` with random `tenantId`
- **Assertions**:
    - Status: 404 NOT FOUND or 400 BAD REQUEST
    - Error message indicates tenant not found

#### Test: Create User Without Authentication

- **Setup**: No authentication
- **Action**: POST `/api/v1/users` without Bearer token
- **Assertions**:
    - Status: 401 UNAUTHORIZED

---

### 2. Role Assignment Tests (SYSTEM_ADMIN)

#### Test: Assign Single Role to User

- **Setup**: Login as SYSTEM_ADMIN, create user
- **Action**: POST `/api/v1/users/{userId}/roles` with `WAREHOUSE_MANAGER` role
- **Request Body**:
  ```json
  {
    "roleName": "WAREHOUSE_MANAGER"
  }
  ```
- **Assertions**:
    - Status: 200 OK or 201 CREATED
    - UserRoleAssignedEvent published
    - Role appears in user's roles list
    - JWT token includes role after re-login

#### Test: Assign Multiple Roles to User

- **Setup**: Login as SYSTEM_ADMIN, create user
- **Action**:
    1. Assign `PICKER` role
    2. Assign `STOCK_CLERK` role
- **Assertions**:
    - Both roles assigned successfully
    - User has both roles in JWT token
    - UserRoleAssignedEvent published for each role

#### Test: Assign All Available Roles

- **Setup**: Login as SYSTEM_ADMIN, create user
- **Action**: Assign all roles from Roles_and_Permissions_Definition.md:
    - SYSTEM_ADMIN
    - TENANT_ADMIN
    - WAREHOUSE_MANAGER
    - STOCK_MANAGER
    - LOCATION_MANAGER
    - RECONCILIATION_MANAGER
    - RETURNS_MANAGER
    - PICKER
    - STOCK_CLERK
    - RECONCILIATION_CLERK
    - RETURNS_CLERK
    - OPERATOR
    - VIEWER
- **Assertions**:
    - All roles assigned successfully
    - JWT token includes all roles

#### Test: Assign Invalid Role

- **Setup**: Login as SYSTEM_ADMIN, create user
- **Action**: POST `/api/v1/users/{userId}/roles` with `INVALID_ROLE`
- **Assertions**:
    - Status: 400 BAD REQUEST
    - Error message indicates invalid role

#### Test: Assign Role to Non-Existent User

- **Setup**: Login as SYSTEM_ADMIN
- **Action**: POST `/api/v1/users/{randomUUID}/roles` with valid role
- **Assertions**:
    - Status: 404 NOT FOUND

#### Test: Remove Role from User

- **Setup**: Login as SYSTEM_ADMIN, create user, assign `PICKER` role
- **Action**: DELETE `/api/v1/users/{userId}/roles/{roleId}`
- **Assertions**:
    - Status: 200 OK or 204 NO CONTENT
    - UserRoleRemovedEvent published
    - Role removed from user's roles list
    - JWT token does not include role after re-login

#### Test: Remove Non-Existent Role from User

- **Setup**: Login as SYSTEM_ADMIN, create user
- **Action**: DELETE `/api/v1/users/{userId}/roles/{randomRoleId}`
- **Assertions**:
    - Status: 404 NOT FOUND or 400 BAD REQUEST

---

### 3. User Status Lifecycle Tests (SYSTEM_ADMIN)

#### Test: Activate User

- **Setup**: Login as SYSTEM_ADMIN, create user, deactivate user
- **Action**: PUT `/api/v1/users/{userId}/activate`
- **Assertions**:
    - Status: 200 OK
    - User status changed to ACTIVE
    - User can login

#### Test: Deactivate User

- **Setup**: Login as SYSTEM_ADMIN, create user
- **Action**: PUT `/api/v1/users/{userId}/deactivate`
- **Assertions**:
    - Status: 200 OK
    - User status changed to INACTIVE
    - UserDeactivatedEvent published
    - User cannot login (401 UNAUTHORIZED)

#### Test: Suspend User

- **Setup**: Login as SYSTEM_ADMIN, create user
- **Action**: PUT `/api/v1/users/{userId}/suspend`
- **Assertions**:
    - Status: 200 OK
    - User status changed to SUSPENDED
    - User cannot perform operations (403 FORBIDDEN)

#### Test: Valid Status Transitions

- **Setup**: Login as SYSTEM_ADMIN, create user
- **Action**: Perform state transitions:
    1. ACTIVE → SUSPENDED
    2. SUSPENDED → ACTIVE
    3. ACTIVE → INACTIVE
    4. INACTIVE → ACTIVE
- **Assertions**:
    - All valid transitions succeed (200 OK)

---

### 4. User Profile Management Tests (SYSTEM_ADMIN)

#### Test: Update User Profile (Self-Service)

- **Setup**: Create user, login as that user
- **Action**: PUT `/api/v1/users/{userId}/profile` with updated profile
- **Request Body**:
  ```json
  {
    "firstName": "Jane",
    "lastName": "Smith",
    "email": "jane.smith@example.com"
  }
  ```
- **Assertions**:
    - Status: 200 OK
    - Profile updated successfully
    - UserUpdatedEvent published

#### Test: Admin Update Any User Profile

- **Setup**: Login as SYSTEM_ADMIN, create user
- **Action**: PUT `/api/v1/users/{userId}/profile` with updated profile
- **Assertions**:
    - Status: 200 OK
    - Profile updated by admin

#### Test: User Cannot Update Other User Profile

- **Setup**: Create two users, login as user1
- **Action**: PUT `/api/v1/users/{user2Id}/profile` with updated profile
- **Assertions**:
    - Status: 403 FORBIDDEN

---

### 5. User Query Tests (SYSTEM_ADMIN)

#### Test: List All Users (SYSTEM_ADMIN)

- **Setup**: Login as SYSTEM_ADMIN, create 5 users with Faker
- **Action**: GET `/api/v1/users?page=0&size=10`
- **Assertions**:
    - Status: 200 OK
    - Response contains all users
    - Pagination metadata present

#### Test: List Users with Status Filter

- **Setup**: Login as SYSTEM_ADMIN, create 3 ACTIVE and 2 SUSPENDED users
- **Action**: GET `/api/v1/users?status=ACTIVE`
- **Assertions**:
    - Status: 200 OK
    - Response contains only ACTIVE users

#### Test: Get User by ID

- **Setup**: Login as SYSTEM_ADMIN, create user
- **Action**: GET `/api/v1/users/{userId}`
- **Assertions**:
    - Status: 200 OK
    - Response contains user details (id, username, email, roles, status)

#### Test: Get Non-Existent User

- **Setup**: Login as SYSTEM_ADMIN
- **Action**: GET `/api/v1/users/{randomUUID}`
- **Assertions**:
    - Status: 404 NOT FOUND

---

### Phase 2: TENANT_ADMIN Tests

#### Setup: Wait for TEST_TENANT_ADMIN Credentials

```java
@BeforeAll
public static void waitForTenantAdminCredentials() {
    // Prompt test executor to provide TENANT_ADMIN credentials
    System.out.println("==========================================");
    System.out.println("SYSTEM_ADMIN tests completed successfully.");
    System.out.println("Please set the following environment variables:");
    System.out.println("  TEST_TENANT_ADMIN_USERNAME=<username>");
    System.out.println("  TEST_TENANT_ADMIN_PASSWORD=Password123@");
    System.out.println("==========================================");
    System.out.println("Press ENTER when ready to continue...");

    // Wait for user input
    Scanner scanner = new Scanner(System.in);
    scanner.nextLine();

    // Verify credentials are set
    tenantAdminUsername = System.getenv("TEST_TENANT_ADMIN_USERNAME");
    tenantAdminPassword = System.getenv("TEST_TENANT_ADMIN_PASSWORD");

    if (tenantAdminUsername == null || tenantAdminPassword == null) {
        throw new IllegalStateException("TEST_TENANT_ADMIN_USERNAME and TEST_TENANT_ADMIN_PASSWORD must be set");
    }

    // Login as TENANT_ADMIN
    tenantAdminAuth = loginAsTenantAdmin();
}
```

---

### 6. User Creation Tests (TENANT_ADMIN)

#### Test: TENANT_ADMIN Creates User in Own Tenant

- **Setup**: Login as TENANT_ADMIN
- **Action**: POST `/api/v1/users` with user in own tenant
- **Assertions**:
    - Status: 201 CREATED
    - User created successfully
    - User belongs to TENANT_ADMIN's tenant

#### Test: TENANT_ADMIN Cannot Create User in Different Tenant

- **Setup**: Login as TENANT_ADMIN, create second tenant
- **Action**: POST `/api/v1/users` with user in different tenant
- **Assertions**:
    - Status: 403 FORBIDDEN
    - Error message indicates insufficient permissions

---

### 7. Role Assignment Tests (TENANT_ADMIN)

#### Test: TENANT_ADMIN Assigns Role to User in Own Tenant

- **Setup**: Login as TENANT_ADMIN, create user in own tenant
- **Action**: POST `/api/v1/users/{userId}/roles` with `WAREHOUSE_MANAGER` role
- **Assertions**:
    - Status: 200 OK
    - Role assigned successfully

#### Test: TENANT_ADMIN Cannot Assign Role to User in Different Tenant

- **Setup**: Login as TENANT_ADMIN, login as SYSTEM_ADMIN, create user in different tenant
- **Action**: POST `/api/v1/users/{userId}/roles` with role (as TENANT_ADMIN)
- **Assertions**:
    - Status: 403 FORBIDDEN

#### Test: TENANT_ADMIN Assigns Multiple Roles

- **Setup**: Login as TENANT_ADMIN, create user
- **Action**: Assign `PICKER`, `STOCK_CLERK`, `WAREHOUSE_MANAGER` roles
- **Assertions**:
    - All roles assigned successfully
    - User has all roles in JWT token

#### Test: TENANT_ADMIN Removes Role from User

- **Setup**: Login as TENANT_ADMIN, create user, assign role
- **Action**: DELETE `/api/v1/users/{userId}/roles/{roleId}`
- **Assertions**:
    - Status: 200 OK
    - Role removed successfully

---

### 8. User Status Lifecycle Tests (TENANT_ADMIN)

#### Test: TENANT_ADMIN Activates User in Own Tenant

- **Setup**: Login as TENANT_ADMIN, create and deactivate user
- **Action**: PUT `/api/v1/users/{userId}/activate`
- **Assertions**:
    - Status: 200 OK
    - User activated successfully

#### Test: TENANT_ADMIN Deactivates User in Own Tenant

- **Setup**: Login as TENANT_ADMIN, create user
- **Action**: PUT `/api/v1/users/{userId}/deactivate`
- **Assertions**:
    - Status: 200 OK
    - User deactivated successfully

#### Test: TENANT_ADMIN Suspends User in Own Tenant

- **Setup**: Login as TENANT_ADMIN, create user
- **Action**: PUT `/api/v1/users/{userId}/suspend`
- **Assertions**:
    - Status: 200 OK
    - User suspended successfully

---

### 9. User Query Tests (TENANT_ADMIN)

#### Test: TENANT_ADMIN Lists Only Own Tenant Users

- **Setup**:
    - Login as SYSTEM_ADMIN, create Tenant A and Tenant B
    - Create 3 users in Tenant A
    - Create 2 users in Tenant B
    - Login as TENANT_ADMIN (Tenant A)
- **Action**: GET `/api/v1/users`
- **Assertions**:
    - Status: 200 OK
    - Response contains only Tenant A users (3 users)
    - Tenant B users not visible

#### Test: TENANT_ADMIN Cannot Access User from Different Tenant

- **Setup**:
    - Login as SYSTEM_ADMIN, create Tenant A and Tenant B
    - Create user in Tenant B
    - Login as TENANT_ADMIN (Tenant A)
- **Action**: GET `/api/v1/users/{tenantBUserId}`
- **Assertions**:
    - Status: 403 FORBIDDEN or 404 NOT FOUND

---

### 10. Authorization Tests

#### Test: WAREHOUSE_MANAGER Can Read Users

- **Setup**: Create user with WAREHOUSE_MANAGER role, login
- **Action**: GET `/api/v1/users`
- **Assertions**:
    - Status: 200 OK (read access granted)

#### Test: WAREHOUSE_MANAGER Cannot Create Users

- **Setup**: Login as WAREHOUSE_MANAGER
- **Action**: POST `/api/v1/users` with valid data
- **Assertions**:
    - Status: 403 FORBIDDEN

#### Test: OPERATOR Cannot Access User Management

- **Setup**: Create user with OPERATOR role, login
- **Action**: GET `/api/v1/users`
- **Assertions**:
    - Status: 403 FORBIDDEN

#### Test: VIEWER Can Read Users

- **Setup**: Create user with VIEWER role, login
- **Action**: GET `/api/v1/users`
- **Assertions**:
    - Status: 200 OK

#### Test: VIEWER Cannot Modify Users

- **Setup**: Login as VIEWER
- **Action**: POST `/api/v1/users` with valid data
- **Assertions**:
    - Status: 403 FORBIDDEN

---

### 11. Role Hierarchy Tests

#### Test: User with WAREHOUSE_MANAGER Role Has Manager Permissions

- **Setup**: Create user with WAREHOUSE_MANAGER role, login
- **Action**: Access stock management: GET `/api/v1/stock-management/consignments`
- **Assertions**:
    - Status: 200 OK
    - WAREHOUSE_MANAGER inherits permissions

#### Test: User with STOCK_MANAGER Role Limited to Stock Operations

- **Setup**: Create user with STOCK_MANAGER role, login
- **Action**:
    - Access stock service: GET `/api/v1/stock-management/consignments` (should succeed)
    - Access picking service: GET `/api/v1/picking/tasks` (should fail)
- **Assertions**:
    - Stock access: 200 OK
    - Picking access: 403 FORBIDDEN

---

### 12. Edge Case Tests

#### Test: Create User with Same Email in Different Tenants

- **Setup**: Login as SYSTEM_ADMIN
- **Action**:
    - Create user with email "john@example.com" in Tenant A
    - Create user with email "john@example.com" in Tenant B
- **Assertions**:
    - Both users created successfully (email unique per tenant, not globally)
    - Or error if email must be globally unique

#### Test: Create User with Very Long Username

- **Setup**: Login as SYSTEM_ADMIN
- **Action**: POST `/api/v1/users` with 500-character username
- **Assertions**:
    - Status: 400 BAD REQUEST
    - Validation error for username length

#### Test: Create User with Special Characters in Name

- **Setup**: Login as SYSTEM_ADMIN
- **Action**: POST `/api/v1/users` with name "O'Brien-Smith"
- **Assertions**:
    - Status: 201 CREATED (or 400 if special characters not allowed)

#### Test: Concurrent User Creation

- **Setup**: Login as SYSTEM_ADMIN
- **Action**: Send 5 concurrent POST requests to create users
- **Assertions**:
    - All requests succeed (201 CREATED)
    - Each user has unique ID

---

## Test Data Strategy

### Faker Data Generation

```java
private CreateUserRequest createRandomUserRequest(String tenantId) {
    return CreateUserRequest.builder()
            .username(faker.name().username())
            .email(faker.internet().emailAddress())
            .firstName(faker.name().firstName())
            .lastName(faker.name().lastName())
            .tenantId(tenantId)
            .build();
}
```

### User Naming Convention

Prefix usernames with test identifier:

- `TEST_USER_CREATION_001`
- `TEST_USER_ROLE_002`
- `TEST_USER_LIFECYCLE_003`

---

## Test Class Structure

```java
package com.ccbsa.wms.gateway.api;

import com.ccbsa.wms.gateway.api.dto.AuthenticationResult;
import com.ccbsa.wms.gateway.api.fixture.UserTestDataBuilder;
import org.junit.jupiter.api.*;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.Scanner;

import static org.assertj.core.api.Assertions.assertThat;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class UserManagementTest extends BaseIntegrationTest {

    private static AuthenticationResult systemAdminAuth;
    private static AuthenticationResult tenantAdminAuth;
    private static String testTenantId;

    @BeforeAll
    public static void setupTestData() {
        // Login as SYSTEM_ADMIN
        systemAdminAuth = loginAsSystemAdmin();

        // Find or create active tenant
        testTenantId = findOrCreateActiveTenant(systemAdminAuth);
    }

    // ==================== SYSTEM_ADMIN TESTS ====================

    @Test
    @Order(1)
    public void testCreateUser_Success_SystemAdmin() {
        // Arrange
        CreateUserRequest request = UserTestDataBuilder.buildCreateUserRequest(testTenantId, faker);

        // Act
        WebTestClient.ResponseSpec response = authenticatedPost(
                "/api/v1/users",
                systemAdminAuth.getAccessToken(),
                request
        ).exchange();

        // Assert
        response.expectStatus().isCreated();

        CreateUserResponse user = response.expectBody(CreateUserResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(user).isNotNull();
        assertThat(user.getUserId()).isNotBlank();
        assertThat(user.getUsername()).isEqualTo(request.getUsername());
        assertThat(user.getStatus()).isEqualTo("ACTIVE");
    }

    // ... Additional SYSTEM_ADMIN tests

    // ==================== TENANT_ADMIN TESTS ====================

    @BeforeAll
    public static void setupTenantAdminTests() {
        // Wait for TENANT_ADMIN credentials from user
        waitForTenantAdminCredentials();

        // Login as TENANT_ADMIN
        tenantAdminAuth = loginAsTenantAdmin();
    }

    @Test
    @Order(100)
    public void testCreateUser_Success_TenantAdmin() {
        // Arrange
        String tenantId = tenantAdminAuth.getTenantId();
        CreateUserRequest request = UserTestDataBuilder.buildCreateUserRequest(tenantId, faker);

        // Act
        WebTestClient.ResponseSpec response = authenticatedPost(
                "/api/v1/users",
                tenantAdminAuth.getAccessToken(),
                request
        ).exchange();

        // Assert
        response.expectStatus().isCreated();
    }

    // ... Additional TENANT_ADMIN tests

    private static void waitForTenantAdminCredentials() {
        System.out.println("==========================================");
        System.out.println("Please set TEST_TENANT_ADMIN_USERNAME environment variable.");
        System.out.println("Press ENTER when ready...");
        System.out.println("==========================================");

        Scanner scanner = new Scanner(System.in);
        scanner.nextLine();
    }

    private static String findOrCreateActiveTenant(AuthenticationResult auth) {
        // Query for active tenants
        WebTestClient.ResponseSpec response = authenticatedGet(
                "/api/v1/tenants?status=ACTIVE&page=0&size=1",
                auth.getAccessToken()
        ).exchange();

        // If exists, return first tenant ID
        // Otherwise, create new tenant and return ID

        // Implementation details...
    }
}
```

---

## Test Fixtures

### UserTestDataBuilder

Create `gateway-api-tests/src/test/java/com/ccbsa/wms/gateway/api/fixture/UserTestDataBuilder.java`:

```java
package com.ccbsa.wms.gateway.api.fixture;

import net.datafaker.Faker;

public class UserTestDataBuilder {

    public static CreateUserRequest buildCreateUserRequest(String tenantId, Faker faker) {
        return CreateUserRequest.builder()
                .username(faker.name().username())
                .email(faker.internet().emailAddress())
                .firstName(faker.name().firstName())
                .lastName(faker.name().lastName())
                .tenantId(tenantId)
                .build();
    }

    public static CreateUserRequest buildCreateUserRequestWithUsername(String username, String tenantId, Faker faker) {
        return CreateUserRequest.builder()
                .username(username)
                .email(faker.internet().emailAddress())
                .firstName(faker.name().firstName())
                .lastName(faker.name().lastName())
                .tenantId(tenantId)
                .build();
    }

    public static AssignRoleRequest buildAssignRoleRequest(String roleName) {
        return AssignRoleRequest.builder()
                .roleName(roleName)
                .build();
    }
}
```

---

## DTOs Required

### CreateUserRequest

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserRequest {
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String tenantId;
}
```

### CreateUserResponse

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserResponse {
    private String userId;
    private String username;
    private String email;
    private String status;
}
```

### UserResponse

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private String userId;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String tenantId;
    private String status;
    private List<String> roles;
    private String createdAt;
    private String updatedAt;
}
```

### AssignRoleRequest

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignRoleRequest {
    private String roleName;
}
```

### UpdateUserProfileRequest

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserProfileRequest {
    private String firstName;
    private String lastName;
    private String email;
}
```

---

## Environment Variables

```bash
# System Admin Credentials
TEST_SYSTEM_ADMIN_USERNAME=sysadmin
TEST_SYSTEM_ADMIN_PASSWORD=Password123@

# Tenant Admin Credentials (set after SYSTEM_ADMIN tests)
TEST_TENANT_ADMIN_USERNAME=<user-input>
TEST_TENANT_ADMIN_PASSWORD=Password123@
```

---

## Testing Checklist

- [ ] User creation succeeds with valid data (SYSTEM_ADMIN)
- [ ] User creation succeeds with valid data (TENANT_ADMIN)
- [ ] User creation fails with duplicate username
- [ ] User creation fails with invalid email
- [ ] TENANT_ADMIN cannot create user in different tenant
- [ ] Single role assigned successfully
- [ ] Multiple roles assigned successfully
- [ ] Invalid role assignment fails
- [ ] Role removal succeeds
- [ ] User activation changes status to ACTIVE
- [ ] User deactivation changes status to INACTIVE
- [ ] User suspension changes status to SUSPENDED
- [ ] User profile update succeeds (self-service)
- [ ] Admin can update any user profile
- [ ] User cannot update other user profile
- [ ] List users returns correct results
- [ ] List users with status filter works
- [ ] Get user by ID returns correct data
- [ ] TENANT_ADMIN sees only own tenant users
- [ ] Role hierarchy enforced correctly
- [ ] UserCreatedEvent published on creation
- [ ] UserRoleAssignedEvent published on role assignment
- [ ] UserRoleRemovedEvent published on role removal
- [ ] UserDeactivatedEvent published on deactivation

---

## Next Steps

1. **Implement UserManagementTest** with all test scenarios
2. **Create UserTestDataBuilder** for test data generation
3. **Create DTO classes** for user requests/responses
4. **Implement waitForTenantAdminCredentials()** method for user input
5. **Implement findOrCreateActiveTenant()** helper method
6. **Validate event publishing** (use Kafka consumer or mock)
7. **Test JWT token role claims** after role assignment
8. **Document test results** and edge cases discovered

---

## Notes

- **Test Execution Order**: SYSTEM_ADMIN tests run first (@Order 1-99), TENANT_ADMIN tests run second (@Order 100+)
- **User Input**: Tests pause and wait for TENANT_ADMIN credentials before Phase 2
- **Tenant Isolation**: Critical to verify TENANT_ADMIN cannot access users in other tenants
- **Role Validation**: Verify JWT token includes assigned roles after re-login
- **Password Generation**: Default password `Password123@` for test users
- **Rate Limiting**: User service has 100 req/min limit
