package com.ccbsa.wms.gateway.api;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.ccbsa.common.application.api.ApiResponse;
import com.ccbsa.wms.gateway.api.dto.AuthenticationResult;
import com.ccbsa.wms.gateway.api.dto.CreateUserRequest;
import com.ccbsa.wms.gateway.api.dto.CreateUserResponse;
import com.ccbsa.wms.gateway.api.dto.UpdateUserProfileRequest;
import com.ccbsa.wms.gateway.api.dto.UserResponse;
import com.ccbsa.wms.gateway.api.fixture.UserTestDataBuilder;
import com.ccbsa.wms.gateway.api.helper.TenantHelper;
import com.ccbsa.wms.gateway.api.util.RequestHeaderHelper;

import static org.assertj.core.api.Assertions.assertThat;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class UserManagementTest extends BaseIntegrationTest {

    private static AuthenticationResult systemAdminAuth;
    private static AuthenticationResult tenantAdminAuth;
    private static String testTenantId;

    // ==================== SYSTEM_ADMIN TESTS ====================

    // --- User Creation Tests ---

    @BeforeEach
    public void setUpUserTest() {
        if (systemAdminAuth == null) {
            systemAdminAuth = loginAsSystemAdmin();
            TenantHelper tenantHelper = new TenantHelper(webTestClient);
            testTenantId = tenantHelper.findOrCreateActiveTenant(systemAdminAuth);
        }
    }

    @Test
    @Order(1)
    public void testCreateUser_Success_SystemAdmin() {
        // Arrange
        CreateUserRequest request = UserTestDataBuilder.buildCreateUserRequest(testTenantId);

        // Act
        WebTestClient.ResponseSpec response = authenticatedPost("/api/v1/users", systemAdminAuth.getAccessToken(), testTenantId, request).exchange();

        // Assert
        EntityExchangeResult<ApiResponse<CreateUserResponse>> exchangeResult =
                response.expectStatus().isCreated().expectBody(new ParameterizedTypeReference<ApiResponse<CreateUserResponse>>() {
                }).returnResult();

        ApiResponse<CreateUserResponse> apiResponse = exchangeResult.getResponseBody();
        assertThat(apiResponse).isNotNull();
        assertThat(apiResponse.isSuccess()).isTrue();

        CreateUserResponse user = apiResponse.getData();
        assertThat(user).isNotNull();
        assertThat(user.getUserId()).isNotBlank();
        assertThat(user.isSuccess()).isTrue();
        assertThat(user.getMessage()).isNotBlank();
    }

    @Test
    @Order(2)
    public void testCreateUser_DuplicateUsername() {
        // Arrange
        String username = "testuser_" + System.currentTimeMillis();
        CreateUserRequest request = UserTestDataBuilder.buildCreateUserRequestWithUsername(username, testTenantId);

        // Create first user
        authenticatedPost("/api/v1/users", systemAdminAuth.getAccessToken(), testTenantId, request).exchange().expectStatus().isCreated();

        // Act - Try to create duplicate
        WebTestClient.ResponseSpec response = authenticatedPost("/api/v1/users", systemAdminAuth.getAccessToken(), testTenantId, request).exchange();

        // Assert
        response.expectStatus().isBadRequest(); // or 409 CONFLICT
    }

    // --- User Read Tests ---

    @Test
    @Order(3)
    public void testCreateUser_InvalidEmail() {
        // Arrange
        CreateUserRequest request = UserTestDataBuilder.buildCreateUserRequest(testTenantId);
        request.setEmailAddress("invalid-email-format");

        // Act
        WebTestClient.ResponseSpec response = authenticatedPost("/api/v1/users", systemAdminAuth.getAccessToken(), testTenantId, request).exchange();

        // Assert
        response.expectStatus().isBadRequest();
    }

    @Test
    @Order(4)
    public void testCreateUser_MissingRequiredFields() {
        // Arrange
        CreateUserRequest request = CreateUserRequest.builder().tenantId(testTenantId)
                // Missing username, email, etc.
                .build();

        // Act
        WebTestClient.ResponseSpec response = authenticatedPost("/api/v1/users", systemAdminAuth.getAccessToken(), testTenantId, request).exchange();

        // Assert
        response.expectStatus().isBadRequest();
    }

    @Test
    @Order(10)
    public void testGetUserById_Success() {
        // Arrange - Create user
        CreateUserRequest request = UserTestDataBuilder.buildCreateUserRequest(testTenantId);
        EntityExchangeResult<ApiResponse<CreateUserResponse>> createExchangeResult =
                authenticatedPost("/api/v1/users", systemAdminAuth.getAccessToken(), testTenantId, request).exchange().expectStatus().isCreated()
                        .expectBody(new ParameterizedTypeReference<ApiResponse<CreateUserResponse>>() {
                        }).returnResult();

        ApiResponse<CreateUserResponse> createApiResponse = createExchangeResult.getResponseBody();
        assertThat(createApiResponse).isNotNull();
        assertThat(createApiResponse.isSuccess()).isTrue();

        CreateUserResponse createdUser = createApiResponse.getData();
        assertThat(createdUser).isNotNull();

        // Act
        WebTestClient.ResponseSpec response = authenticatedGet("/api/v1/users/" + createdUser.getUserId(), systemAdminAuth.getAccessToken(), testTenantId).exchange();

        // Assert
        EntityExchangeResult<ApiResponse<UserResponse>> getExchangeResult = response.expectStatus().isOk().expectBody(new ParameterizedTypeReference<ApiResponse<UserResponse>>() {
        }).returnResult();

        ApiResponse<UserResponse> getApiResponse = getExchangeResult.getResponseBody();
        assertThat(getApiResponse).isNotNull();
        assertThat(getApiResponse.isSuccess()).isTrue();

        UserResponse user = getApiResponse.getData();
        assertThat(user).isNotNull();
        assertThat(user.getUserId()).isEqualTo(createdUser.getUserId());
        assertThat(user.getUsername()).isEqualTo(request.getUsername());
        assertThat(user.getEmailAddress()).isEqualTo(request.getEmailAddress());
    }

    @Test
    @Order(11)
    public void testGetUserById_NotFound() {
        // Arrange
        String nonExistentUserId = "usr-" + System.currentTimeMillis();

        // Act
        WebTestClient.ResponseSpec response = authenticatedGet("/api/v1/users/" + nonExistentUserId, systemAdminAuth.getAccessToken(), testTenantId).exchange();

        // Assert
        response.expectStatus().isNotFound();
    }

    // --- User Update Tests ---

    @Test
    @Order(12)
    public void testListUsers_Success() {
        // Arrange - Create a few users
        for (int i = 0; i < 3; i++) {
            CreateUserRequest request = UserTestDataBuilder.buildCreateUserRequest(testTenantId);
            authenticatedPost("/api/v1/users", systemAdminAuth.getAccessToken(), testTenantId, request).exchange().expectStatus().isCreated();
        }

        // Act
        WebTestClient.ResponseSpec response = authenticatedGet("/api/v1/users?page=0&size=10", systemAdminAuth.getAccessToken(), testTenantId).exchange();

        // Assert
        response.expectStatus().isOk();
    }

    // --- Role Management Tests ---

    @Test
    @Order(13)
    public void testListUsers_WithPagination() {
        // Act
        WebTestClient.ResponseSpec response = authenticatedGet("/api/v1/users?page=0&size=5", systemAdminAuth.getAccessToken(), testTenantId).exchange();

        // Assert
        response.expectStatus().isOk();
    }

    @Test
    @Order(20)
    public void testUpdateUserProfile_Success() {
        // Arrange - Create user
        CreateUserRequest createRequest = UserTestDataBuilder.buildCreateUserRequest(testTenantId);
        EntityExchangeResult<ApiResponse<CreateUserResponse>> createResult =
                authenticatedPost("/api/v1/users", systemAdminAuth.getAccessToken(), testTenantId, createRequest).exchange().expectStatus().isCreated()
                        .expectBody(new ParameterizedTypeReference<ApiResponse<CreateUserResponse>>() {
                        }).returnResult();

        String userId = createResult.getResponseBody().getData().getUserId();

        UpdateUserProfileRequest updateRequest =
                UpdateUserProfileRequest.builder().firstName("UpdatedFirstName").lastName("UpdatedLastName").emailAddress("updated.email@example.com").build();

        // Act
        WebTestClient.ResponseSpec response = authenticatedPut("/api/v1/users/" + userId + "/profile", systemAdminAuth.getAccessToken(), updateRequest).exchange();

        // Assert
        response.expectStatus().is2xxSuccessful();

        // Verify update by getting user
        EntityExchangeResult<ApiResponse<UserResponse>> getResult =
                authenticatedGet("/api/v1/users/" + userId, systemAdminAuth.getAccessToken(), testTenantId).exchange().expectStatus().isOk()
                        .expectBody(new ParameterizedTypeReference<ApiResponse<UserResponse>>() {
                        }).returnResult();

        UserResponse updatedUser = getResult.getResponseBody().getData();
        assertThat(updatedUser.getFirstName()).isEqualTo("UpdatedFirstName");
        assertThat(updatedUser.getLastName()).isEqualTo("UpdatedLastName");
    }

    @Test
    @Order(30)
    public void testAssignRole_Success() {
        // Arrange - Create user
        CreateUserRequest createRequest = UserTestDataBuilder.buildCreateUserRequest(testTenantId);
        EntityExchangeResult<ApiResponse<CreateUserResponse>> createExchangeResult =
                authenticatedPost("/api/v1/users", systemAdminAuth.getAccessToken(), testTenantId, createRequest).exchange().expectStatus().isCreated()
                        .expectBody(new ParameterizedTypeReference<ApiResponse<CreateUserResponse>>() {
                        }).returnResult();

        ApiResponse<CreateUserResponse> createApiResponse = createExchangeResult.getResponseBody();
        assertThat(createApiResponse).isNotNull();
        assertThat(createApiResponse.isSuccess()).isTrue();

        CreateUserResponse user = createApiResponse.getData();
        assertThat(user).isNotNull();

        // Act - Assign role
        WebTestClient.ResponseSpec response = authenticatedPost("/api/v1/users/" + user.getUserId() + "/roles", systemAdminAuth.getAccessToken(), testTenantId,
                UserTestDataBuilder.buildAssignRoleRequest(UserTestDataBuilder.Roles.WAREHOUSE_MANAGER)).exchange();

        // Assert
        response.expectStatus().is2xxSuccessful();

        // Verify role was assigned
        EntityExchangeResult<ApiResponse<UserResponse>> getResult =
                authenticatedGet("/api/v1/users/" + user.getUserId(), systemAdminAuth.getAccessToken(), testTenantId).exchange().expectStatus().isOk()
                        .expectBody(new ParameterizedTypeReference<ApiResponse<UserResponse>>() {
                        }).returnResult();

        UserResponse userWithRole = getResult.getResponseBody().getData();
        assertThat(userWithRole.getRoles()).isNotNull();
        assertThat(userWithRole.getRoles()).contains(UserTestDataBuilder.Roles.WAREHOUSE_MANAGER);
    }

    @Test
    @Order(31)
    public void testAssignMultipleRoles_Success() {
        // Arrange - Create user
        CreateUserRequest createRequest = UserTestDataBuilder.buildCreateUserRequest(testTenantId);
        EntityExchangeResult<ApiResponse<CreateUserResponse>> createResult =
                authenticatedPost("/api/v1/users", systemAdminAuth.getAccessToken(), testTenantId, createRequest).exchange().expectStatus().isCreated()
                        .expectBody(new ParameterizedTypeReference<ApiResponse<CreateUserResponse>>() {
                        }).returnResult();

        String userId = createResult.getResponseBody().getData().getUserId();

        // Act - Assign multiple roles
        authenticatedPost("/api/v1/users/" + userId + "/roles", systemAdminAuth.getAccessToken(), testTenantId,
                UserTestDataBuilder.buildAssignRoleRequest(UserTestDataBuilder.Roles.PICKER)).exchange().expectStatus().is2xxSuccessful();

        authenticatedPost("/api/v1/users/" + userId + "/roles", systemAdminAuth.getAccessToken(), testTenantId,
                UserTestDataBuilder.buildAssignRoleRequest(UserTestDataBuilder.Roles.STOCK_CLERK)).exchange().expectStatus().is2xxSuccessful();

        // Assert - Verify both roles assigned
        EntityExchangeResult<ApiResponse<UserResponse>> getResult =
                authenticatedGet("/api/v1/users/" + userId, systemAdminAuth.getAccessToken(), testTenantId).exchange().expectStatus().isOk()
                        .expectBody(new ParameterizedTypeReference<ApiResponse<UserResponse>>() {
                        }).returnResult();

        UserResponse user = getResult.getResponseBody().getData();
        assertThat(user.getRoles()).contains(UserTestDataBuilder.Roles.PICKER);
        assertThat(user.getRoles()).contains(UserTestDataBuilder.Roles.STOCK_CLERK);
    }

    @Test
    @Order(32)
    public void testRemoveRole_Success() {
        // Arrange - Create user and assign role
        CreateUserRequest createRequest = UserTestDataBuilder.buildCreateUserRequest(testTenantId);
        EntityExchangeResult<ApiResponse<CreateUserResponse>> createResult =
                authenticatedPost("/api/v1/users", systemAdminAuth.getAccessToken(), testTenantId, createRequest).exchange().expectStatus().isCreated()
                        .expectBody(new ParameterizedTypeReference<ApiResponse<CreateUserResponse>>() {
                        }).returnResult();

        String userId = createResult.getResponseBody().getData().getUserId();

        // Assign role
        authenticatedPost("/api/v1/users/" + userId + "/roles", systemAdminAuth.getAccessToken(), testTenantId,
                UserTestDataBuilder.buildAssignRoleRequest(UserTestDataBuilder.Roles.PICKER)).exchange().expectStatus().is2xxSuccessful();

        // Act - Remove role
        WebTestClient.ResponseSpec response =
                authenticatedDelete("/api/v1/users/" + userId + "/roles/" + UserTestDataBuilder.Roles.PICKER, systemAdminAuth.getAccessToken()).exchange();

        // Assert
        response.expectStatus().is2xxSuccessful();

        // Verify role was removed
        EntityExchangeResult<ApiResponse<UserResponse>> getResult =
                authenticatedGet("/api/v1/users/" + userId, systemAdminAuth.getAccessToken(), testTenantId).exchange().expectStatus().isOk()
                        .expectBody(new ParameterizedTypeReference<ApiResponse<UserResponse>>() {
                        }).returnResult();

        UserResponse user = getResult.getResponseBody().getData();
        assertThat(user.getRoles()).doesNotContain(UserTestDataBuilder.Roles.PICKER);
    }

    // --- User Lifecycle Tests ---

    @Test
    @Order(33)
    public void testAssignRole_InvalidRole() {
        // Arrange - Create user
        CreateUserRequest createRequest = UserTestDataBuilder.buildCreateUserRequest(testTenantId);
        EntityExchangeResult<ApiResponse<CreateUserResponse>> createResult =
                authenticatedPost("/api/v1/users", systemAdminAuth.getAccessToken(), testTenantId, createRequest).exchange().expectStatus().isCreated()
                        .expectBody(new ParameterizedTypeReference<ApiResponse<CreateUserResponse>>() {
                        }).returnResult();

        String userId = createResult.getResponseBody().getData().getUserId();

        // Act - Try to assign invalid role
        WebTestClient.ResponseSpec response = authenticatedPost("/api/v1/users/" + userId + "/roles", systemAdminAuth.getAccessToken(), testTenantId,
                UserTestDataBuilder.buildAssignRoleRequest("INVALID_ROLE")).exchange();

        // Assert
        response.expectStatus().isBadRequest();
    }

    @Test
    @Order(34)
    public void testGetUserRoles_Success() {
        // Arrange - Create user and assign roles
        CreateUserRequest createRequest = UserTestDataBuilder.buildCreateUserRequest(testTenantId);
        EntityExchangeResult<ApiResponse<CreateUserResponse>> createResult =
                authenticatedPost("/api/v1/users", systemAdminAuth.getAccessToken(), testTenantId, createRequest).exchange().expectStatus().isCreated()
                        .expectBody(new ParameterizedTypeReference<ApiResponse<CreateUserResponse>>() {
                        }).returnResult();

        String userId = createResult.getResponseBody().getData().getUserId();

        // Assign roles
        authenticatedPost("/api/v1/users/" + userId + "/roles", systemAdminAuth.getAccessToken(), testTenantId,
                UserTestDataBuilder.buildAssignRoleRequest(UserTestDataBuilder.Roles.PICKER)).exchange().expectStatus().is2xxSuccessful();

        authenticatedPost("/api/v1/users/" + userId + "/roles", systemAdminAuth.getAccessToken(), testTenantId,
                UserTestDataBuilder.buildAssignRoleRequest(UserTestDataBuilder.Roles.STOCK_CLERK)).exchange().expectStatus().is2xxSuccessful();

        // Act - Get user roles
        WebTestClient.ResponseSpec response = authenticatedGet("/api/v1/users/" + userId + "/roles", systemAdminAuth.getAccessToken(), testTenantId).exchange();

        // Assert
        EntityExchangeResult<ApiResponse<List<String>>> getResult = response.expectStatus().isOk().expectBody(new ParameterizedTypeReference<ApiResponse<List<String>>>() {
        }).returnResult();

        ApiResponse<List<String>> apiResponse = getResult.getResponseBody();
        assertThat(apiResponse).isNotNull();
        assertThat(apiResponse.isSuccess()).isTrue();

        List<String> roles = apiResponse.getData();
        assertThat(roles).isNotNull();
        assertThat(roles).contains(UserTestDataBuilder.Roles.PICKER);
        assertThat(roles).contains(UserTestDataBuilder.Roles.STOCK_CLERK);
    }

    @Test
    @Order(40)
    public void testActivateUser_Success() {
        // Arrange - Create and deactivate user
        CreateUserRequest createRequest = UserTestDataBuilder.buildCreateUserRequest(testTenantId);
        EntityExchangeResult<ApiResponse<CreateUserResponse>> createResult =
                authenticatedPost("/api/v1/users", systemAdminAuth.getAccessToken(), testTenantId, createRequest).exchange().expectStatus().isCreated()
                        .expectBody(new ParameterizedTypeReference<ApiResponse<CreateUserResponse>>() {
                        }).returnResult();

        String userId = createResult.getResponseBody().getData().getUserId();

        // Deactivate user first
        authenticatedPut("/api/v1/users/" + userId + "/deactivate", systemAdminAuth.getAccessToken(), null).exchange().expectStatus().is2xxSuccessful();

        // Act - Activate user
        WebTestClient.ResponseSpec response = authenticatedPut("/api/v1/users/" + userId + "/activate", systemAdminAuth.getAccessToken(), null).exchange();

        // Assert
        response.expectStatus().is2xxSuccessful();

        // Verify status changed
        EntityExchangeResult<ApiResponse<UserResponse>> getResult =
                authenticatedGet("/api/v1/users/" + userId, systemAdminAuth.getAccessToken(), testTenantId).exchange().expectStatus().isOk()
                        .expectBody(new ParameterizedTypeReference<ApiResponse<UserResponse>>() {
                        }).returnResult();

        UserResponse user = getResult.getResponseBody().getData();
        assertThat(user.getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    @Order(41)
    public void testDeactivateUser_Success() {
        // Arrange - Create user
        CreateUserRequest createRequest = UserTestDataBuilder.buildCreateUserRequest(testTenantId);
        EntityExchangeResult<ApiResponse<CreateUserResponse>> createResult =
                authenticatedPost("/api/v1/users", systemAdminAuth.getAccessToken(), testTenantId, createRequest).exchange().expectStatus().isCreated()
                        .expectBody(new ParameterizedTypeReference<ApiResponse<CreateUserResponse>>() {
                        }).returnResult();

        String userId = createResult.getResponseBody().getData().getUserId();

        // Act - Deactivate user
        WebTestClient.ResponseSpec response = authenticatedPut("/api/v1/users/" + userId + "/deactivate", systemAdminAuth.getAccessToken(), null).exchange();

        // Assert
        response.expectStatus().is2xxSuccessful();

        // Verify status changed
        EntityExchangeResult<ApiResponse<UserResponse>> getResult =
                authenticatedGet("/api/v1/users/" + userId, systemAdminAuth.getAccessToken(), testTenantId).exchange().expectStatus().isOk()
                        .expectBody(new ParameterizedTypeReference<ApiResponse<UserResponse>>() {
                        }).returnResult();

        UserResponse user = getResult.getResponseBody().getData();
        assertThat(user.getStatus()).isEqualTo("INACTIVE");
    }

    // ==================== TENANT_ADMIN TESTS ====================

    @Test
    @Order(42)
    public void testSuspendUser_Success() {
        // Arrange - Create user
        CreateUserRequest createRequest = UserTestDataBuilder.buildCreateUserRequest(testTenantId);
        EntityExchangeResult<ApiResponse<CreateUserResponse>> createResult =
                authenticatedPost("/api/v1/users", systemAdminAuth.getAccessToken(), testTenantId, createRequest).exchange().expectStatus().isCreated()
                        .expectBody(new ParameterizedTypeReference<ApiResponse<CreateUserResponse>>() {
                        }).returnResult();

        String userId = createResult.getResponseBody().getData().getUserId();

        // Act - Suspend user
        WebTestClient.ResponseSpec response = authenticatedPut("/api/v1/users/" + userId + "/suspend", systemAdminAuth.getAccessToken(), null).exchange();

        // Assert
        response.expectStatus().is2xxSuccessful();

        // Verify status changed
        EntityExchangeResult<ApiResponse<UserResponse>> getResult =
                authenticatedGet("/api/v1/users/" + userId, systemAdminAuth.getAccessToken(), testTenantId).exchange().expectStatus().isOk()
                        .expectBody(new ParameterizedTypeReference<ApiResponse<UserResponse>>() {
                        }).returnResult();

        UserResponse user = getResult.getResponseBody().getData();
        assertThat(user.getStatus()).isEqualTo("SUSPENDED");
    }

    @Test
    @Order(43)
    public void testUserStatusTransitions_Success() {
        // Arrange - Create user
        CreateUserRequest createRequest = UserTestDataBuilder.buildCreateUserRequest(testTenantId);
        EntityExchangeResult<ApiResponse<CreateUserResponse>> createResult =
                authenticatedPost("/api/v1/users", systemAdminAuth.getAccessToken(), testTenantId, createRequest).exchange().expectStatus().isCreated()
                        .expectBody(new ParameterizedTypeReference<ApiResponse<CreateUserResponse>>() {
                        }).returnResult();

        String userId = createResult.getResponseBody().getData().getUserId();

        // Act & Assert - Test valid transitions
        // ACTIVE -> SUSPENDED
        authenticatedPut("/api/v1/users/" + userId + "/suspend", systemAdminAuth.getAccessToken(), null).exchange().expectStatus().is2xxSuccessful();

        // SUSPENDED -> ACTIVE
        authenticatedPut("/api/v1/users/" + userId + "/activate", systemAdminAuth.getAccessToken(), null).exchange().expectStatus().is2xxSuccessful();

        // ACTIVE -> INACTIVE
        authenticatedPut("/api/v1/users/" + userId + "/deactivate", systemAdminAuth.getAccessToken(), null).exchange().expectStatus().is2xxSuccessful();

        // INACTIVE -> ACTIVE
        authenticatedPut("/api/v1/users/" + userId + "/activate", systemAdminAuth.getAccessToken(), null).exchange().expectStatus().is2xxSuccessful();
    }

    @Test
    @Order(100)
    public void testCreateUser_Success_TenantAdmin() {
        // Skip if credentials not set
        if (shouldSkipTenantAdminTests()) {
            System.out.println("Skipping TENANT_ADMIN test - credentials not set");
            return;
        }

        // Arrange
        AuthenticationResult auth = getTenantAdminAuth();
        String tenantId = auth.getTenantId();
        CreateUserRequest request = UserTestDataBuilder.buildCreateUserRequest(tenantId);

        // Act
        WebTestClient.ResponseSpec response = authenticatedPost("/api/v1/users", auth.getAccessToken(), tenantId, request).exchange();

        // Assert
        EntityExchangeResult<ApiResponse<CreateUserResponse>> exchangeResult =
                response.expectStatus().isCreated().expectBody(new ParameterizedTypeReference<ApiResponse<CreateUserResponse>>() {
                }).returnResult();

        ApiResponse<CreateUserResponse> apiResponse = exchangeResult.getResponseBody();
        assertThat(apiResponse).isNotNull();
        assertThat(apiResponse.isSuccess()).isTrue();

        CreateUserResponse user = apiResponse.getData();
        assertThat(user).isNotNull();
        assertThat(user.getUserId()).isNotBlank();
    }

    // --- User Creation Tests (TENANT_ADMIN) ---

    private boolean shouldSkipTenantAdminTests() {
        return tenantAdminUsername == null || tenantAdminUsername.isEmpty() || tenantAdminPassword == null || tenantAdminPassword.isEmpty();
    }

    private AuthenticationResult getTenantAdminAuth() {
        if (tenantAdminAuth == null) {
            tenantAdminAuth = loginAsTenantAdmin();
        }
        return tenantAdminAuth;
    }

    // --- User Read Tests (TENANT_ADMIN) ---

    @Test
    @Order(101)
    public void testCreateUser_DifferentTenant_Forbidden() {
        // Skip if credentials not set
        if (shouldSkipTenantAdminTests()) {
            System.out.println("Skipping TENANT_ADMIN test - credentials not set");
            return;
        }

        // Arrange
        AuthenticationResult auth = getTenantAdminAuth();
        String tenantId = auth.getTenantId();

        // Try to create user in different tenant (testTenantId from SYSTEM_ADMIN)
        CreateUserRequest request = UserTestDataBuilder.buildCreateUserRequest(testTenantId);

        // Act
        WebTestClient.ResponseSpec response = authenticatedPost("/api/v1/users", auth.getAccessToken(), testTenantId, // Different tenant
                request).exchange();

        // Assert - Should be forbidden or return empty list
        int statusCode = response.returnResult(Void.class).getStatus().value();
        assertThat(statusCode).isIn(403, 400); // FORBIDDEN or BAD_REQUEST
    }

    @Test
    @Order(110)
    public void testGetUserById_Success_TenantAdmin() {
        // Skip if credentials not set
        if (shouldSkipTenantAdminTests()) {
            System.out.println("Skipping TENANT_ADMIN test - credentials not set");
            return;
        }

        // Arrange
        AuthenticationResult auth = getTenantAdminAuth();
        String tenantId = auth.getTenantId();

        // Create user in own tenant
        CreateUserRequest createRequest = UserTestDataBuilder.buildCreateUserRequest(tenantId);
        EntityExchangeResult<ApiResponse<CreateUserResponse>> createResult =
                authenticatedPost("/api/v1/users", auth.getAccessToken(), tenantId, createRequest).exchange().expectStatus().isCreated()
                        .expectBody(new ParameterizedTypeReference<ApiResponse<CreateUserResponse>>() {
                        }).returnResult();

        String userId = createResult.getResponseBody().getData().getUserId();

        // Act
        WebTestClient.ResponseSpec response = authenticatedGet("/api/v1/users/" + userId, auth.getAccessToken(), tenantId).exchange();

        // Assert
        EntityExchangeResult<ApiResponse<UserResponse>> getResult = response.expectStatus().isOk().expectBody(new ParameterizedTypeReference<ApiResponse<UserResponse>>() {
        }).returnResult();

        ApiResponse<UserResponse> apiResponse = getResult.getResponseBody();
        assertThat(apiResponse).isNotNull();
        assertThat(apiResponse.isSuccess()).isTrue();

        UserResponse user = apiResponse.getData();
        assertThat(user).isNotNull();
        assertThat(user.getUserId()).isEqualTo(userId);
        assertThat(user.getTenantId()).isEqualTo(tenantId);
    }

    @Test
    @Order(111)
    public void testListUsers_OwnTenantOnly_TenantAdmin() {
        // Skip if credentials not set
        if (shouldSkipTenantAdminTests()) {
            System.out.println("Skipping TENANT_ADMIN test - credentials not set");
            return;
        }

        // Arrange
        AuthenticationResult auth = getTenantAdminAuth();
        String tenantId = auth.getTenantId();

        // Create users in own tenant
        for (int i = 0; i < 2; i++) {
            CreateUserRequest request = UserTestDataBuilder.buildCreateUserRequest(tenantId);
            authenticatedPost("/api/v1/users", auth.getAccessToken(), tenantId, request).exchange().expectStatus().isCreated();
        }

        // Act
        WebTestClient.ResponseSpec response = authenticatedGet("/api/v1/users?page=0&size=10", auth.getAccessToken(), tenantId).exchange();

        // Assert
        response.expectStatus().isOk();
    }

    // --- User Update Tests (TENANT_ADMIN) ---

    @Test
    @Order(112)
    public void testTenantAdmin_CannotAccessOtherTenantUsers() {
        // Skip if credentials not set
        if (shouldSkipTenantAdminTests()) {
            System.out.println("Skipping TENANT_ADMIN test - credentials not set");
            return;
        }

        // Arrange
        AuthenticationResult auth = getTenantAdminAuth();
        String tenantId = auth.getTenantId();

        // Create user in SYSTEM_ADMIN tenant
        CreateUserRequest createRequest = UserTestDataBuilder.buildCreateUserRequest(testTenantId);
        EntityExchangeResult<ApiResponse<CreateUserResponse>> createResult =
                authenticatedPost("/api/v1/users", systemAdminAuth.getAccessToken(), testTenantId, createRequest).exchange().expectStatus().isCreated()
                        .expectBody(new ParameterizedTypeReference<ApiResponse<CreateUserResponse>>() {
                        }).returnResult();

        String otherTenantUserId = createResult.getResponseBody().getData().getUserId();

        // Act - Try to access user from different tenant
        WebTestClient.ResponseSpec response = authenticatedGet("/api/v1/users/" + otherTenantUserId, auth.getAccessToken(), testTenantId // Different tenant
        ).exchange();

        // Assert - Should be forbidden or not found
        int statusCode = response.returnResult(Void.class).getStatus().value();
        assertThat(statusCode).isIn(403, 404); // FORBIDDEN or NOT_FOUND
    }

    // --- Role Management Tests (TENANT_ADMIN) ---

    @Test
    @Order(120)
    public void testUpdateUserProfile_Success_TenantAdmin() {
        // Skip if credentials not set
        if (shouldSkipTenantAdminTests()) {
            System.out.println("Skipping TENANT_ADMIN test - credentials not set");
            return;
        }

        // Arrange
        AuthenticationResult auth = getTenantAdminAuth();
        String tenantId = auth.getTenantId();

        // Create user in own tenant
        CreateUserRequest createRequest = UserTestDataBuilder.buildCreateUserRequest(tenantId);
        EntityExchangeResult<ApiResponse<CreateUserResponse>> createResult =
                authenticatedPost("/api/v1/users", auth.getAccessToken(), tenantId, createRequest).exchange().expectStatus().isCreated()
                        .expectBody(new ParameterizedTypeReference<ApiResponse<CreateUserResponse>>() {
                        }).returnResult();

        String userId = createResult.getResponseBody().getData().getUserId();

        UpdateUserProfileRequest updateRequest =
                UpdateUserProfileRequest.builder().firstName("TenantAdminUpdatedFirstName").lastName("TenantAdminUpdatedLastName").emailAddress("tenantadmin.updated@example.com")
                        .build();

        // Act
        WebTestClient.ResponseSpec response = authenticatedPut("/api/v1/users/" + userId + "/profile", auth.getAccessToken(), updateRequest).exchange();

        // Assert
        response.expectStatus().is2xxSuccessful();
    }

    @Test
    @Order(130)
    public void testAssignRole_Success_TenantAdmin() {
        // Skip if credentials not set
        if (shouldSkipTenantAdminTests()) {
            System.out.println("Skipping TENANT_ADMIN test - credentials not set");
            return;
        }

        // Arrange
        AuthenticationResult auth = getTenantAdminAuth();
        String tenantId = auth.getTenantId();

        // Create user in own tenant
        CreateUserRequest createRequest = UserTestDataBuilder.buildCreateUserRequest(tenantId);
        EntityExchangeResult<ApiResponse<CreateUserResponse>> createResult =
                authenticatedPost("/api/v1/users", auth.getAccessToken(), tenantId, createRequest).exchange().expectStatus().isCreated()
                        .expectBody(new ParameterizedTypeReference<ApiResponse<CreateUserResponse>>() {
                        }).returnResult();

        String userId = createResult.getResponseBody().getData().getUserId();

        // Act - Assign role
        WebTestClient.ResponseSpec response = authenticatedPost("/api/v1/users/" + userId + "/roles", auth.getAccessToken(), tenantId,
                UserTestDataBuilder.buildAssignRoleRequest(UserTestDataBuilder.Roles.WAREHOUSE_MANAGER)).exchange();

        // Assert
        response.expectStatus().is2xxSuccessful();
    }

    @Test
    @Order(131)
    public void testAssignMultipleRoles_Success_TenantAdmin() {
        // Skip if credentials not set
        if (shouldSkipTenantAdminTests()) {
            System.out.println("Skipping TENANT_ADMIN test - credentials not set");
            return;
        }

        // Arrange
        AuthenticationResult auth = getTenantAdminAuth();
        String tenantId = auth.getTenantId();

        // Create user in own tenant
        CreateUserRequest createRequest = UserTestDataBuilder.buildCreateUserRequest(tenantId);
        EntityExchangeResult<ApiResponse<CreateUserResponse>> createResult =
                authenticatedPost("/api/v1/users", auth.getAccessToken(), tenantId, createRequest).exchange().expectStatus().isCreated()
                        .expectBody(new ParameterizedTypeReference<ApiResponse<CreateUserResponse>>() {
                        }).returnResult();

        String userId = createResult.getResponseBody().getData().getUserId();

        // Act - Assign multiple roles
        authenticatedPost("/api/v1/users/" + userId + "/roles", auth.getAccessToken(), tenantId,
                UserTestDataBuilder.buildAssignRoleRequest(UserTestDataBuilder.Roles.PICKER)).exchange().expectStatus().is2xxSuccessful();

        authenticatedPost("/api/v1/users/" + userId + "/roles", auth.getAccessToken(), tenantId,
                UserTestDataBuilder.buildAssignRoleRequest(UserTestDataBuilder.Roles.STOCK_CLERK)).exchange().expectStatus().is2xxSuccessful();

        // Assert - Verify both roles assigned
        EntityExchangeResult<ApiResponse<UserResponse>> getResult = authenticatedGet("/api/v1/users/" + userId, auth.getAccessToken(), tenantId).exchange().expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<ApiResponse<UserResponse>>() {
                }).returnResult();

        UserResponse user = getResult.getResponseBody().getData();
        assertThat(user.getRoles()).contains(UserTestDataBuilder.Roles.PICKER);
        assertThat(user.getRoles()).contains(UserTestDataBuilder.Roles.STOCK_CLERK);
    }

    // --- User Lifecycle Tests (TENANT_ADMIN) ---

    @Test
    @Order(132)
    public void testRemoveRole_Success_TenantAdmin() {
        // Skip if credentials not set
        if (shouldSkipTenantAdminTests()) {
            System.out.println("Skipping TENANT_ADMIN test - credentials not set");
            return;
        }

        // Arrange
        AuthenticationResult auth = getTenantAdminAuth();
        String tenantId = auth.getTenantId();

        // Create user in own tenant
        CreateUserRequest createRequest = UserTestDataBuilder.buildCreateUserRequest(tenantId);
        EntityExchangeResult<ApiResponse<CreateUserResponse>> createResult =
                authenticatedPost("/api/v1/users", auth.getAccessToken(), tenantId, createRequest).exchange().expectStatus().isCreated()
                        .expectBody(new ParameterizedTypeReference<ApiResponse<CreateUserResponse>>() {
                        }).returnResult();

        String userId = createResult.getResponseBody().getData().getUserId();

        // Assign role first
        authenticatedPost("/api/v1/users/" + userId + "/roles", auth.getAccessToken(), tenantId,
                UserTestDataBuilder.buildAssignRoleRequest(UserTestDataBuilder.Roles.PICKER)).exchange().expectStatus().is2xxSuccessful();

        // Act - Remove role
        WebTestClient.ResponseSpec response = authenticatedDelete("/api/v1/users/" + userId + "/roles/" + UserTestDataBuilder.Roles.PICKER, auth.getAccessToken()).exchange();

        // Assert
        response.expectStatus().is2xxSuccessful();
    }

    @Test
    @Order(140)
    public void testActivateUser_Success_TenantAdmin() {
        // Skip if credentials not set
        if (shouldSkipTenantAdminTests()) {
            System.out.println("Skipping TENANT_ADMIN test - credentials not set");
            return;
        }

        // Arrange
        AuthenticationResult auth = getTenantAdminAuth();
        String tenantId = auth.getTenantId();

        // Create user in own tenant
        CreateUserRequest createRequest = UserTestDataBuilder.buildCreateUserRequest(tenantId);
        EntityExchangeResult<ApiResponse<CreateUserResponse>> createResult =
                authenticatedPost("/api/v1/users", auth.getAccessToken(), tenantId, createRequest).exchange().expectStatus().isCreated()
                        .expectBody(new ParameterizedTypeReference<ApiResponse<CreateUserResponse>>() {
                        }).returnResult();

        String userId = createResult.getResponseBody().getData().getUserId();

        // Deactivate user first
        authenticatedPut("/api/v1/users/" + userId + "/deactivate", auth.getAccessToken(), null).exchange().expectStatus().is2xxSuccessful();

        // Act - Activate user
        WebTestClient.ResponseSpec response = authenticatedPut("/api/v1/users/" + userId + "/activate", auth.getAccessToken(), null).exchange();

        // Assert
        response.expectStatus().is2xxSuccessful();
    }

    @Test
    @Order(141)
    public void testDeactivateUser_Success_TenantAdmin() {
        // Skip if credentials not set
        if (shouldSkipTenantAdminTests()) {
            System.out.println("Skipping TENANT_ADMIN test - credentials not set");
            return;
        }

        // Arrange
        AuthenticationResult auth = getTenantAdminAuth();
        String tenantId = auth.getTenantId();

        // Create user in own tenant
        CreateUserRequest createRequest = UserTestDataBuilder.buildCreateUserRequest(tenantId);
        EntityExchangeResult<ApiResponse<CreateUserResponse>> createResult =
                authenticatedPost("/api/v1/users", auth.getAccessToken(), tenantId, createRequest).exchange().expectStatus().isCreated()
                        .expectBody(new ParameterizedTypeReference<ApiResponse<CreateUserResponse>>() {
                        }).returnResult();

        String userId = createResult.getResponseBody().getData().getUserId();

        // Act - Deactivate user
        WebTestClient.ResponseSpec response = authenticatedPut("/api/v1/users/" + userId + "/deactivate", auth.getAccessToken(), null).exchange();

        // Assert
        response.expectStatus().is2xxSuccessful();
    }

    @Test
    @Order(142)
    public void testSuspendUser_Success_TenantAdmin() {
        // Skip if credentials not set
        if (shouldSkipTenantAdminTests()) {
            System.out.println("Skipping TENANT_ADMIN test - credentials not set");
            return;
        }

        // Arrange
        AuthenticationResult auth = getTenantAdminAuth();
        String tenantId = auth.getTenantId();

        // Create user in own tenant
        CreateUserRequest createRequest = UserTestDataBuilder.buildCreateUserRequest(tenantId);
        EntityExchangeResult<ApiResponse<CreateUserResponse>> createResult =
                authenticatedPost("/api/v1/users", auth.getAccessToken(), tenantId, createRequest).exchange().expectStatus().isCreated()
                        .expectBody(new ParameterizedTypeReference<ApiResponse<CreateUserResponse>>() {
                        }).returnResult();

        String userId = createResult.getResponseBody().getData().getUserId();

        // Act - Suspend user
        WebTestClient.ResponseSpec response = authenticatedPut("/api/v1/users/" + userId + "/suspend", auth.getAccessToken(), null).exchange();

        // Assert
        response.expectStatus().is2xxSuccessful();
    }

    // --- Helper Methods ---

    @Test
    @Order(143)
    public void testUserStatusTransitions_Success_TenantAdmin() {
        // Skip if credentials not set
        if (shouldSkipTenantAdminTests()) {
            System.out.println("Skipping TENANT_ADMIN test - credentials not set");
            return;
        }

        // Arrange
        AuthenticationResult auth = getTenantAdminAuth();
        String tenantId = auth.getTenantId();

        // Create user in own tenant
        CreateUserRequest createRequest = UserTestDataBuilder.buildCreateUserRequest(tenantId);
        EntityExchangeResult<ApiResponse<CreateUserResponse>> createResult =
                authenticatedPost("/api/v1/users", auth.getAccessToken(), tenantId, createRequest).exchange().expectStatus().isCreated()
                        .expectBody(new ParameterizedTypeReference<ApiResponse<CreateUserResponse>>() {
                        }).returnResult();

        String userId = createResult.getResponseBody().getData().getUserId();

        // Act & Assert - Test valid transitions
        // ACTIVE -> SUSPENDED
        authenticatedPut("/api/v1/users/" + userId + "/suspend", auth.getAccessToken(), null).exchange().expectStatus().is2xxSuccessful();

        // SUSPENDED -> ACTIVE
        authenticatedPut("/api/v1/users/" + userId + "/activate", auth.getAccessToken(), null).exchange().expectStatus().is2xxSuccessful();

        // ACTIVE -> INACTIVE
        authenticatedPut("/api/v1/users/" + userId + "/deactivate", auth.getAccessToken(), null).exchange().expectStatus().is2xxSuccessful();

        // INACTIVE -> ACTIVE
        authenticatedPut("/api/v1/users/" + userId + "/activate", auth.getAccessToken(), null).exchange().expectStatus().is2xxSuccessful();
    }

    /**
     * Create authenticated PUT request with Bearer token and tenant context.
     */
    private WebTestClient.RequestHeadersSpec<?> authenticatedPutWithTenant(String uri, String accessToken, String tenantId, Object requestBody) {
        WebTestClient.RequestBodySpec spec = webTestClient.put().uri(uri).contentType(MediaType.APPLICATION_JSON).headers(headers -> {
            RequestHeaderHelper.addAuthHeaders(headers, accessToken);
            RequestHeaderHelper.addTenantHeader(headers, tenantId);
        });

        if (requestBody != null) {
            return spec.bodyValue(requestBody);
        }
        return spec;
    }

    /**
     * Create authenticated DELETE request with Bearer token and tenant context.
     */
    private WebTestClient.RequestHeadersSpec<?> authenticatedDeleteWithTenant(String uri, String accessToken, String tenantId) {
        return webTestClient.delete().uri(uri).headers(headers -> {
            RequestHeaderHelper.addAuthHeaders(headers, accessToken);
            RequestHeaderHelper.addTenantHeader(headers, tenantId);
        });
    }
}

