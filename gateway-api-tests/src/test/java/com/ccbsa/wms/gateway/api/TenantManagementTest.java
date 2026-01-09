package com.ccbsa.wms.gateway.api;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.ccbsa.common.application.api.ApiResponse;
import com.ccbsa.wms.gateway.api.dto.AuthenticationResult;
import com.ccbsa.wms.gateway.api.dto.CreateTenantRequest;
import com.ccbsa.wms.gateway.api.dto.CreateTenantResponse;
import com.ccbsa.wms.gateway.api.dto.CreateUserRequest;
import com.ccbsa.wms.gateway.api.dto.CreateUserResponse;
import com.ccbsa.wms.gateway.api.dto.TenantResponse;
import com.ccbsa.wms.gateway.api.dto.UserResponse;
import com.ccbsa.wms.gateway.api.fixture.TenantTestDataBuilder;
import com.ccbsa.wms.gateway.api.fixture.TestData;
import com.ccbsa.wms.gateway.api.fixture.UserTestDataBuilder;

import static org.assertj.core.api.Assertions.assertThat;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TenantManagementTest extends BaseIntegrationTest {

    private AuthenticationResult systemAdminAuth;

    @BeforeEach
    public void setUpTenantTest() {
        // Login as SYSTEM_ADMIN before each test
        systemAdminAuth = loginAsSystemAdmin();
    }

    // ==================== TENANT CREATION TESTS ====================

    @Test
    @Order(1)
    public void testCreateTenant_Success() {
        // Arrange
        CreateTenantRequest request = TenantTestDataBuilder.buildCreateTenantRequest();

        // Act
        WebTestClient.ResponseSpec response = authenticatedPost("/api/v1/tenants", systemAdminAuth.getAccessToken(), request).exchange();

        // Assert
        EntityExchangeResult<ApiResponse<CreateTenantResponse>> exchangeResult =
                response.expectStatus().isCreated().expectBody(new ParameterizedTypeReference<ApiResponse<CreateTenantResponse>>() {
                }).returnResult();

        ApiResponse<CreateTenantResponse> apiResponse = exchangeResult.getResponseBody();
        assertThat(apiResponse).isNotNull();
        assertThat(apiResponse.isSuccess()).isTrue();

        CreateTenantResponse tenant = apiResponse.getData();
        assertThat(tenant).isNotNull();
        assertThat(tenant.getTenantId()).isNotBlank();
        assertThat(tenant.isSuccess()).isTrue();
        assertThat(tenant.getMessage()).isNotBlank();
    }

    @Test
    @Order(2)
    public void testCreateTenant_DuplicateTenantId() {
        // Arrange - Create tenant with specific tenantId
        String duplicateTenantId = TestData.tenantId();
        CreateTenantRequest firstRequest = CreateTenantRequest.builder().tenantId(duplicateTenantId).name(TestData.tenantName()).emailAddress(TestData.tenantEmail()).build();

        // Create first tenant
        authenticatedPost("/api/v1/tenants", systemAdminAuth.getAccessToken(), firstRequest).exchange().expectStatus().isCreated();

        // Act - Try to create duplicate with same tenantId (different name)
        CreateTenantRequest duplicateRequest = CreateTenantRequest.builder().tenantId(duplicateTenantId) // Same tenantId
                .name("Different Name") // Different name
                .emailAddress(TestData.tenantEmail()).build();

        EntityExchangeResult<ApiResponse<CreateTenantResponse>> exchangeResult =
                authenticatedPost("/api/v1/tenants", systemAdminAuth.getAccessToken(), duplicateRequest).exchange().expectStatus()
                        .isCreated() // Service returns 201 even for duplicates
                        .expectBody(new ParameterizedTypeReference<ApiResponse<CreateTenantResponse>>() {
                        }).returnResult();

        // Assert - Check that the response indicates failure
        ApiResponse<CreateTenantResponse> apiResponse = exchangeResult.getResponseBody();
        assertThat(apiResponse).isNotNull();
        // The service returns success=true wrapper, but data.success=false for duplicates
        CreateTenantResponse responseData = apiResponse.getData();
        assertThat(responseData).isNotNull();
        assertThat(responseData.isSuccess()).isFalse();
        assertThat(responseData.getMessage()).contains("already exists");
    }

    @Test
    @Order(3)
    public void testCreateTenant_InvalidData() {
        // Arrange
        CreateTenantRequest request = CreateTenantRequest.builder().tenantId(TestData.tenantId()).name("") // Empty name
                .emailAddress("invalid-email") // Invalid email
                .build();

        // Act
        WebTestClient.ResponseSpec response = authenticatedPost("/api/v1/tenants", systemAdminAuth.getAccessToken(), request).exchange();

        // Assert
        response.expectStatus().isBadRequest();
    }

    @Test
    @Order(4)
    public void testCreateTenant_Unauthorized() {
        // Arrange
        CreateTenantRequest request = TenantTestDataBuilder.buildCreateTenantRequest();

        // Act - No authentication
        WebTestClient.ResponseSpec response = webTestClient.post().uri("/api/v1/tenants").contentType(MediaType.APPLICATION_JSON).bodyValue(request).exchange();

        // Assert
        response.expectStatus().isUnauthorized();
    }

    // ==================== TENANT QUERY TESTS ====================

    @Test
    @Order(10)
    public void testListTenants_Success() {
        // Act
        WebTestClient.ResponseSpec response = authenticatedGet("/api/v1/tenants?page=0&size=10", systemAdminAuth.getAccessToken()).exchange();

        // Assert
        response.expectStatus().isOk();
    }

    @Test
    @Order(11)
    public void testGetTenantById_Success() {
        // Arrange - Create tenant first
        CreateTenantRequest request = TenantTestDataBuilder.buildCreateTenantRequest();
        EntityExchangeResult<ApiResponse<CreateTenantResponse>> createExchangeResult =
                authenticatedPost("/api/v1/tenants", systemAdminAuth.getAccessToken(), request).exchange().expectStatus().isCreated()
                        .expectBody(new ParameterizedTypeReference<ApiResponse<CreateTenantResponse>>() {
                        }).returnResult();

        ApiResponse<CreateTenantResponse> createApiResponse = createExchangeResult.getResponseBody();
        assertThat(createApiResponse).isNotNull();
        assertThat(createApiResponse.isSuccess()).isTrue();

        CreateTenantResponse createdTenant = createApiResponse.getData();
        assertThat(createdTenant).isNotNull();

        // Act
        WebTestClient.ResponseSpec response = authenticatedGet("/api/v1/tenants/" + createdTenant.getTenantId(), systemAdminAuth.getAccessToken()).exchange();

        // Assert
        EntityExchangeResult<ApiResponse<TenantResponse>> getExchangeResult =
                response.expectStatus().isOk().expectBody(new ParameterizedTypeReference<ApiResponse<TenantResponse>>() {
                }).returnResult();

        ApiResponse<TenantResponse> getApiResponse = getExchangeResult.getResponseBody();
        assertThat(getApiResponse).isNotNull();
        assertThat(getApiResponse.isSuccess()).isTrue();

        TenantResponse tenant = getApiResponse.getData();
        assertThat(tenant).isNotNull();
        assertThat(tenant.getTenantId()).isEqualTo(createdTenant.getTenantId());
    }

    @Test
    @Order(12)
    public void testGetTenantById_NotFound() {
        // Act
        WebTestClient.ResponseSpec response = authenticatedGet("/api/v1/tenants/" + randomUUID(), systemAdminAuth.getAccessToken()).exchange();

        // Assert
        response.expectStatus().isNotFound();
    }

    // ==================== TENANT LIFECYCLE TESTS ====================

    @Test
    @Order(20)
    public void testActivateTenant_Success() {
        // Arrange - Create tenant (status will be PENDING)
        CreateTenantRequest request = TenantTestDataBuilder.buildCreateTenantRequest();
        EntityExchangeResult<ApiResponse<CreateTenantResponse>> exchangeResult =
                authenticatedPost("/api/v1/tenants", systemAdminAuth.getAccessToken(), request).exchange().expectStatus().isCreated()
                        .expectBody(new ParameterizedTypeReference<ApiResponse<CreateTenantResponse>>() {
                        }).returnResult();

        ApiResponse<CreateTenantResponse> apiResponse = exchangeResult.getResponseBody();
        assertThat(apiResponse).isNotNull();
        assertThat(apiResponse.isSuccess()).isTrue();

        CreateTenantResponse tenant = apiResponse.getData();
        assertThat(tenant).isNotNull();

        // Act - Activate tenant (PENDING -> ACTIVE)
        WebTestClient.ResponseSpec response = authenticatedPut("/api/v1/tenants/" + tenant.getTenantId() + "/activate", systemAdminAuth.getAccessToken(), null).exchange();

        // Assert - Activation returns 204 NO_CONTENT (as per ApiResponseBuilder.noContent())
        response.expectStatus().isNoContent();
    }

    @Test
    @Order(21)
    public void testDeactivateTenant_Success() {
        // Arrange - Create and activate tenant first (can only deactivate ACTIVE tenants)
        CreateTenantRequest request = TenantTestDataBuilder.buildCreateTenantRequest();
        EntityExchangeResult<ApiResponse<CreateTenantResponse>> exchangeResult =
                authenticatedPost("/api/v1/tenants", systemAdminAuth.getAccessToken(), request).exchange().expectStatus().isCreated()
                        .expectBody(new ParameterizedTypeReference<ApiResponse<CreateTenantResponse>>() {
                        }).returnResult();

        ApiResponse<CreateTenantResponse> apiResponse = exchangeResult.getResponseBody();
        assertThat(apiResponse).isNotNull();
        assertThat(apiResponse.isSuccess()).isTrue();

        CreateTenantResponse tenant = apiResponse.getData();
        assertThat(tenant).isNotNull();

        // Activate tenant first (PENDING -> ACTIVE)
        authenticatedPut("/api/v1/tenants/" + tenant.getTenantId() + "/activate", systemAdminAuth.getAccessToken(), null).exchange().expectStatus().isNoContent();

        // Act - Deactivate tenant (ACTIVE -> INACTIVE)
        WebTestClient.ResponseSpec response = authenticatedPut("/api/v1/tenants/" + tenant.getTenantId() + "/deactivate", systemAdminAuth.getAccessToken(), null).exchange();

        // Assert - Deactivation returns 204 NO_CONTENT
        response.expectStatus().isNoContent();
    }

    @Test
    @Order(22)
    public void testSuspendTenant_Success() {
        // Arrange - Create and activate tenant first (can only suspend ACTIVE tenants)
        CreateTenantRequest request = TenantTestDataBuilder.buildCreateTenantRequest();
        EntityExchangeResult<ApiResponse<CreateTenantResponse>> exchangeResult =
                authenticatedPost("/api/v1/tenants", systemAdminAuth.getAccessToken(), request).exchange().expectStatus().isCreated()
                        .expectBody(new ParameterizedTypeReference<ApiResponse<CreateTenantResponse>>() {
                        }).returnResult();

        ApiResponse<CreateTenantResponse> apiResponse = exchangeResult.getResponseBody();
        assertThat(apiResponse).isNotNull();
        assertThat(apiResponse.isSuccess()).isTrue();

        CreateTenantResponse tenant = apiResponse.getData();
        assertThat(tenant).isNotNull();

        // Activate tenant first (PENDING -> ACTIVE)
        authenticatedPut("/api/v1/tenants/" + tenant.getTenantId() + "/activate", systemAdminAuth.getAccessToken(), null).exchange().expectStatus().isNoContent();

        // Act - Suspend tenant (ACTIVE -> SUSPENDED)
        WebTestClient.ResponseSpec response = authenticatedPut("/api/v1/tenants/" + tenant.getTenantId() + "/suspend", systemAdminAuth.getAccessToken(), null).exchange();

        // Assert - Suspension returns 204 NO_CONTENT
        response.expectStatus().isNoContent();
    }

    // ==================== USER CREATION IN ACTIVE TENANT TESTS ====================

    @Test
    @Order(30)
    public void testCreateUser_InFirstActiveTenant_KeycloakUserExists() {
        // Arrange - List tenants and find first active tenant
        EntityExchangeResult<ApiResponse<List<TenantResponse>>> listExchangeResult =
                authenticatedGet("/api/v1/tenants?page=0&size=100", systemAdminAuth.getAccessToken()).exchange().expectStatus().isOk()
                        .expectBody(new ParameterizedTypeReference<ApiResponse<List<TenantResponse>>>() {
                        }).returnResult();

        ApiResponse<List<TenantResponse>> listApiResponse = listExchangeResult.getResponseBody();
        assertThat(listApiResponse).isNotNull();
        assertThat(listApiResponse.isSuccess()).isTrue();

        List<TenantResponse> tenants = listApiResponse.getData();
        assertThat(tenants).isNotNull();

        // Find first active tenant
        TenantResponse activeTenant =
                tenants.stream().filter(t -> "ACTIVE".equals(t.getStatus())).findFirst().orElseThrow(() -> new AssertionError("No active tenant found in the list"));

        assertThat(activeTenant).isNotNull();
        assertThat(activeTenant.getTenantId()).isNotBlank();

        // Arrange - Create user request with specific username and email
        // Username: testuser, Email: tenantuser@cm-sol.co.za
        CreateUserRequest createUserRequest =
                CreateUserRequest.builder().tenantId(activeTenant.getTenantId()).username("testuser").emailAddress("tenantuser@cm-sol.co.za").password("Password123@")
                        .firstName("Tenant").lastName("User").build();

        // Act - Create user (user will be created successfully in Keycloak and database)
        EntityExchangeResult<ApiResponse<CreateUserResponse>> createExchangeResult =
                authenticatedPost("/api/v1/users", systemAdminAuth.getAccessToken(), activeTenant.getTenantId(), createUserRequest).exchange().expectStatus().isCreated()
                        .expectBody(new ParameterizedTypeReference<ApiResponse<CreateUserResponse>>() {
                        }).returnResult();

        // Assert - Check response indicates successful user creation
        ApiResponse<CreateUserResponse> createApiResponse = createExchangeResult.getResponseBody();
        assertThat(createApiResponse).isNotNull();
        assertThat(createApiResponse.isSuccess()).isTrue();

        CreateUserResponse userResponse = createApiResponse.getData();
        assertThat(userResponse).isNotNull();
        assertThat(userResponse.getUserId()).isNotBlank();
        assertThat(userResponse.isSuccess()).isTrue();

        // Verify user exists in database
        EntityExchangeResult<ApiResponse<List<UserResponse>>> listUsersResult =
                authenticatedGet("/api/v1/users?page=0&size=100&search=testuser", systemAdminAuth.getAccessToken(), activeTenant.getTenantId()).exchange().expectStatus().isOk()
                        .expectBody(new ParameterizedTypeReference<ApiResponse<List<UserResponse>>>() {
                        }).returnResult();

        ApiResponse<List<UserResponse>> listUsersApiResponse = listUsersResult.getResponseBody();
        assertThat(listUsersApiResponse).isNotNull();
        assertThat(listUsersApiResponse.isSuccess()).isTrue();

        List<UserResponse> users = listUsersApiResponse.getData();
        assertThat(users).isNotNull();

        // Verify the user with username "testuser" exists in the database
        boolean userExists = users.stream().anyMatch(u -> "testuser".equals(u.getUsername()) && "tenantuser@cm-sol.co.za".equals(u.getEmailAddress()));
        assertThat(userExists).as("User 'testuser' with email 'tenantuser@cm-sol.co.za' should exist in the database").isTrue();

        // Find the created user from the list
        UserResponse createdUser = users.stream().filter(u -> "testuser".equals(u.getUsername()) && "tenantuser@cm-sol.co.za".equals(u.getEmailAddress())).findFirst()
                .orElseThrow(() -> new AssertionError("Created user not found in the list"));

        // Act - Assign TENANT_ADMIN role to the user
        WebTestClient.ResponseSpec assignRoleResponse =
                authenticatedPost("/api/v1/users/" + createdUser.getUserId() + "/roles", systemAdminAuth.getAccessToken(), activeTenant.getTenantId(),
                        UserTestDataBuilder.buildAssignRoleRequest(UserTestDataBuilder.Roles.TENANT_ADMIN)).exchange();

        // Assert - Role assignment successful
        assignRoleResponse.expectStatus().is2xxSuccessful();

        // Verify role was assigned
        EntityExchangeResult<ApiResponse<UserResponse>> getUserResult =
                authenticatedGet("/api/v1/users/" + createdUser.getUserId(), systemAdminAuth.getAccessToken(), activeTenant.getTenantId()).exchange().expectStatus().isOk()
                        .expectBody(new ParameterizedTypeReference<ApiResponse<UserResponse>>() {
                        }).returnResult();

        ApiResponse<UserResponse> getUserApiResponse = getUserResult.getResponseBody();
        assertThat(getUserApiResponse).isNotNull();
        assertThat(getUserApiResponse.isSuccess()).isTrue();

        UserResponse userWithRole = getUserApiResponse.getData();
        assertThat(userWithRole).isNotNull();
        assertThat(userWithRole.getRoles()).isNotNull();
        assertThat(userWithRole.getRoles()).as("User should have TENANT_ADMIN role assigned").contains(UserTestDataBuilder.Roles.TENANT_ADMIN);

        // Act - Verify email and clear required actions using Keycloak Admin Client
        String keycloakServerUrl = System.getProperty("keycloak.server.url", System.getenv().getOrDefault("KEYCLOAK_SERVER_URL", "http://localhost:7080"));
        String keycloakAdminRealm = System.getProperty("keycloak.admin.realm", System.getenv().getOrDefault("KEYCLOAK_ADMIN_REALM", "master"));
        String keycloakAdminUsername = System.getProperty("keycloak.admin.username", System.getenv().getOrDefault("KEYCLOAK_ADMIN_USERNAME", "admin"));
        String keycloakAdminPassword = System.getProperty("keycloak.admin.password", System.getenv().getOrDefault("KEYCLOAK_ADMIN_PASSWORD", "admin"));
        String keycloakDefaultRealm = System.getProperty("keycloak.default.realm", System.getenv().getOrDefault("KEYCLOAK_DEFAULT_REALM", "wms-realm"));

        try (Keycloak keycloak = KeycloakBuilder.builder().serverUrl(keycloakServerUrl).realm(keycloakAdminRealm).username(keycloakAdminUsername).password(keycloakAdminPassword)
                .clientId("admin-cli").build()) {

            RealmResource realm = keycloak.realm(keycloakDefaultRealm);

            // Find user by username
            List<UserRepresentation> keycloakUsers = realm.users().searchByUsername("testuser", true);
            assertThat(keycloakUsers).as("User 'testuser' should exist in Keycloak").isNotEmpty();

            UserRepresentation keycloakUser = keycloakUsers.get(0);
            assertThat(keycloakUser.getUsername()).as("Found user should have username 'testuser'").isEqualTo("testuser");

            // Get user resource
            UserResource userResource = realm.users().get(keycloakUser.getId());

            // Update user: verify email and clear required actions
            UserRepresentation userToUpdate = userResource.toRepresentation();
            userToUpdate.setEmailVerified(true);
            userToUpdate.setRequiredActions(Collections.emptyList());
            userResource.update(userToUpdate);

            // Verify the changes were applied
            UserRepresentation updatedUser = userResource.toRepresentation();
            assertThat(updatedUser.isEmailVerified()).as("User email should be verified").isTrue();
            assertThat(updatedUser.getRequiredActions()).as("User should have no required actions").isEmpty();
        }
    }
}

