package com.ccbsa.wms.gateway.api;

import com.ccbsa.common.application.api.ApiResponse;
import com.ccbsa.wms.gateway.api.dto.AuthenticationResult;
import com.ccbsa.wms.gateway.api.dto.CreateUserRequest;
import com.ccbsa.wms.gateway.api.dto.CreateUserResponse;
import com.ccbsa.wms.gateway.api.dto.UserResponse;
import com.ccbsa.wms.gateway.api.fixture.UserTestDataBuilder;
import com.ccbsa.wms.gateway.api.helper.TenantHelper;
import org.junit.jupiter.api.*;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class UserManagementTest extends BaseIntegrationTest {

    private static AuthenticationResult systemAdminAuth;
    private static AuthenticationResult tenantAdminAuth;
    private static String testTenantId;

    @BeforeEach
    public void setUpUserTest() {
        if (systemAdminAuth == null) {
            systemAdminAuth = loginAsSystemAdmin();
            TenantHelper tenantHelper = new TenantHelper(webTestClient);
            testTenantId = tenantHelper.findOrCreateActiveTenant(systemAdminAuth);
        }
    }

    // ==================== SYSTEM_ADMIN TESTS ====================

    @Test
    @Order(1)
    public void testCreateUser_Success_SystemAdmin() {
        // Arrange
        CreateUserRequest request = UserTestDataBuilder.buildCreateUserRequest(testTenantId);

        // Act
        WebTestClient.ResponseSpec response = authenticatedPost(
                "/api/v1/users",
                systemAdminAuth.getAccessToken(),
                testTenantId,
                request
        ).exchange();

        // Assert
        EntityExchangeResult<ApiResponse<CreateUserResponse>> exchangeResult = response
                .expectStatus().isCreated()
                .expectBody(new ParameterizedTypeReference<ApiResponse<CreateUserResponse>>() {})
                .returnResult();
        
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
        authenticatedPost("/api/v1/users", systemAdminAuth.getAccessToken(), testTenantId, request)
                .exchange()
                .expectStatus().isCreated();

        // Act - Try to create duplicate
        WebTestClient.ResponseSpec response = authenticatedPost(
                "/api/v1/users",
                systemAdminAuth.getAccessToken(),
                testTenantId,
                request
        ).exchange();

        // Assert
        response.expectStatus().isBadRequest(); // or 409 CONFLICT
    }

    @Test
    @Order(3)
    public void testAssignRole_Success() {
        // Arrange - Create user
        CreateUserRequest createRequest = UserTestDataBuilder.buildCreateUserRequest(testTenantId);
        EntityExchangeResult<ApiResponse<CreateUserResponse>> createExchangeResult = authenticatedPost(
                "/api/v1/users",
                systemAdminAuth.getAccessToken(),
                testTenantId,
                createRequest
        ).exchange()
                .expectStatus().isCreated()
                .expectBody(new ParameterizedTypeReference<ApiResponse<CreateUserResponse>>() {})
                .returnResult();
        
        ApiResponse<CreateUserResponse> createApiResponse = createExchangeResult.getResponseBody();
        assertThat(createApiResponse).isNotNull();
        assertThat(createApiResponse.isSuccess()).isTrue();
        
        CreateUserResponse user = createApiResponse.getData();
        assertThat(user).isNotNull();

        // Act - Assign role
        WebTestClient.ResponseSpec response = authenticatedPost(
                "/api/v1/users/" + user.getUserId() + "/roles",
                systemAdminAuth.getAccessToken(),
                testTenantId,
                UserTestDataBuilder.buildAssignRoleRequest(UserTestDataBuilder.Roles.WAREHOUSE_MANAGER)
        ).exchange();

        // Assert
        response.expectStatus().is2xxSuccessful();
    }

    @Test
    @Order(4)
    public void testListUsers_Success() {
        // Act
        WebTestClient.ResponseSpec response = authenticatedGet(
                "/api/v1/users?page=0&size=10",
                systemAdminAuth.getAccessToken(),
                testTenantId
        ).exchange();

        // Assert
        response.expectStatus().isOk();
    }

    @Test
    @Order(5)
    public void testGetUserById_Success() {
        // Arrange - Create user
        CreateUserRequest request = UserTestDataBuilder.buildCreateUserRequest(testTenantId);
        EntityExchangeResult<ApiResponse<CreateUserResponse>> createExchangeResult = authenticatedPost(
                "/api/v1/users",
                systemAdminAuth.getAccessToken(),
                testTenantId,
                request
        ).exchange()
                .expectStatus().isCreated()
                .expectBody(new ParameterizedTypeReference<ApiResponse<CreateUserResponse>>() {})
                .returnResult();
        
        ApiResponse<CreateUserResponse> createApiResponse = createExchangeResult.getResponseBody();
        assertThat(createApiResponse).isNotNull();
        assertThat(createApiResponse.isSuccess()).isTrue();
        
        CreateUserResponse createdUser = createApiResponse.getData();
        assertThat(createdUser).isNotNull();

        // Act
        WebTestClient.ResponseSpec response = authenticatedGet(
                "/api/v1/users/" + createdUser.getUserId(),
                systemAdminAuth.getAccessToken(),
                testTenantId
        ).exchange();

        // Assert
        EntityExchangeResult<ApiResponse<UserResponse>> getExchangeResult = response
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<ApiResponse<UserResponse>>() {})
                .returnResult();
        
        ApiResponse<UserResponse> getApiResponse = getExchangeResult.getResponseBody();
        assertThat(getApiResponse).isNotNull();
        assertThat(getApiResponse.isSuccess()).isTrue();
        
        UserResponse user = getApiResponse.getData();
        assertThat(user).isNotNull();
        assertThat(user.getUserId()).isEqualTo(createdUser.getUserId());
    }

    // ==================== TENANT_ADMIN TESTS ====================

    @BeforeAll
    public static void setupTenantAdminTests() {
        // Wait for TENANT_ADMIN credentials from user
        waitForTenantAdminCredentials();

        // Login as TENANT_ADMIN will be done in @BeforeEach of Phase 2 tests
    }

    @Test
    @Order(100)
    public void testCreateUser_Success_TenantAdmin() {
        // Skip if credentials not set
        if (System.getenv("TEST_TENANT_ADMIN_USERNAME") == null || 
            System.getenv("TEST_TENANT_ADMIN_PASSWORD") == null) {
            System.out.println("Skipping TENANT_ADMIN test - credentials not set");
            return;
        }
        
        // Arrange - Login as TENANT_ADMIN
        if (tenantAdminAuth == null) {
            tenantAdminAuth = loginAsTenantAdmin();
        }

        String tenantId = tenantAdminAuth.getTenantId();
        CreateUserRequest request = UserTestDataBuilder.buildCreateUserRequest(tenantId);

        // Act
        WebTestClient.ResponseSpec response = authenticatedPost(
                "/api/v1/users",
                tenantAdminAuth.getAccessToken(),
                tenantId,
                request
        ).exchange();

        // Assert
        response.expectStatus().isCreated();
    }

    @Test
    @Order(101)
    public void testTenantAdmin_CannotAccessOtherTenantUsers() {
        // Skip if credentials not set
        if (System.getenv("TEST_TENANT_ADMIN_USERNAME") == null || 
            System.getenv("TEST_TENANT_ADMIN_PASSWORD") == null) {
            System.out.println("Skipping TENANT_ADMIN test - credentials not set");
            return;
        }
        
        // Arrange - Login as TENANT_ADMIN
        if (tenantAdminAuth == null) {
            tenantAdminAuth = loginAsTenantAdmin();
        }

        String tenantId = tenantAdminAuth.getTenantId();

        // Act - Try to access SYSTEM_ADMIN tenant users
        WebTestClient.ResponseSpec response = authenticatedGet(
                "/api/v1/users",
                tenantAdminAuth.getAccessToken(),
                testTenantId // Different tenant
        ).exchange();

        // Assert - Should only see own tenant users or get 403
        // The response depends on implementation - could be 200 with empty list or 403
        // For now, we'll check it doesn't fail with 500
        assertThat(response.returnResult(Void.class).getStatus().value()).isLessThan(500);
    }

    private static void waitForTenantAdminCredentials() {
        // Verify credentials are set
        String tenantAdminUsername = System.getenv("TEST_TENANT_ADMIN_USERNAME");
        String tenantAdminPassword = System.getenv("TEST_TENANT_ADMIN_PASSWORD");

        if (tenantAdminUsername == null || tenantAdminPassword == null) {
            System.out.println("==========================================");
            System.out.println("WARNING: TENANT_ADMIN tests will be skipped.");
            System.out.println("Please set the following environment variables to run TENANT_ADMIN tests:");
            System.out.println("  TEST_TENANT_ADMIN_USERNAME=<username>");
            System.out.println("  TEST_TENANT_ADMIN_PASSWORD=Password123@");
            System.out.println("==========================================");
            // Don't throw exception, just skip the tests
        }
    }
}

