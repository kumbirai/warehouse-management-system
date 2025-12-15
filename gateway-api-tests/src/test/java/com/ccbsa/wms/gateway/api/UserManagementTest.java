package com.ccbsa.wms.gateway.api;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.BodyInserters;

import com.ccbsa.wms.gateway.api.fixture.UserTestDataBuilder;
import com.ccbsa.wms.gateway.api.util.RequestHeaderHelper;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Integration tests for user management endpoints.
 * Tests user CRUD operations through the gateway.
 */
@DisplayName("User Management API Tests")
class UserManagementTest extends BaseIntegrationTest {

    @Test
    @DisplayName("Should list users with valid authentication")
    void shouldListUsers() {
        // When/Then
        RequestHeaderHelper.addTenantHeaderIfNeeded(
                        webTestClient
                                .get()
                                .uri("/users?page=0&size=10")
                                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", accessToken)),
                        authHelper,
                        accessToken)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data").exists();
    }

    @Test
    @DisplayName("Should get user by ID with valid authentication")
    void shouldGetUserById() {
        // Given - Create a real user
        String testUserId = createActiveUser();

        // When/Then
        RequestHeaderHelper.addTenantHeaderIfNeeded(
                        webTestClient
                                .get()
                                .uri(String.format("/users/%s", testUserId))
                                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", accessToken)),
                        authHelper,
                        accessToken)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data.userId").isEqualTo(testUserId);
    }

    /**
     * Creates a user in ACTIVE status using an active tenant.
     * Overrides the base class method to ensure an active tenant is used.
     *
     * @return User ID
     */
    @Override
    protected String createActiveUser() {
        String activeTenantId = findOrCreateActiveTenant();
        return userBuilder()
                .withTenantId(activeTenantId)
                .withStatus(UserTestDataBuilder.UserStatus.ACTIVE)
                .build();
    }

    /**
     * Finds an active tenant or creates one if none exists.
     * First attempts to find an active tenant from the list of tenants.
     * Only creates a new tenant if no active tenant is found.
     *
     * @return The active tenant ID
     */
    private String findOrCreateActiveTenant() {
        // First, try to find an active tenant
        String activeTenantId = findActiveTenant();
        if (activeTenantId != null) {
            return activeTenantId;
        }

        // If no active tenant exists, create and activate one using TestData
        return createAndActivateTenant();
    }

    /**
     * Lists tenants and finds the first one with ACTIVE status.
     *
     * @return Active tenant ID, or null if none found
     */
    private String findActiveTenant() {
        byte[] responseBodyBytes = RequestHeaderHelper.addTenantHeaderIfNeeded(
                        webTestClient
                                .get()
                                .uri("/tenants?page=0&size=100")
                                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", accessToken)),
                        authHelper,
                        accessToken)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .returnResult()
                .getResponseBody();

        try {
            String responseBody = new String(responseBodyBytes, java.nio.charset.StandardCharsets.UTF_8);
            JsonNode response = objectMapper.readTree(responseBody);
            JsonNode data = response.get("data");
            if (data != null && data.isArray()) {
                // data is already an array (List<TenantSummaryResponse>)
                for (JsonNode tenant : data) {
                    if (tenant.has("status") && "ACTIVE".equals(tenant.get("status").asText())) {
                        return tenant.get("tenantId").asText();
                    }
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Creates a tenant using TestData and activates it.
     * Follows the same pattern as TenantManagementTest.createTestTenant().
     *
     * @return The created and activated tenant ID
     */
    private String createAndActivateTenant() {
        // Create tenant using TestData (same pattern as TenantManagementTest)
        Map<String, Object> createTenantRequest = new HashMap<>();
        createTenantRequest.put("tenantId", testData.generateUniqueTenantId());
        createTenantRequest.put("name", testData.generateCompanyName());
        createTenantRequest.put("emailAddress", testData.generateEmail());
        createTenantRequest.put("phone", testData.generatePhoneNumber());

        byte[] responseBodyBytes = RequestHeaderHelper.addTenantHeaderIfNeeded(
                        webTestClient
                                .post()
                                .uri("/tenants")
                                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", accessToken))
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(BodyInserters.fromValue(createTenantRequest)),
                        authHelper,
                        accessToken)
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .returnResult()
                .getResponseBody();

        String tenantId;
        try {
            String responseBody = new String(responseBodyBytes, java.nio.charset.StandardCharsets.UTF_8);
            JsonNode response = objectMapper.readTree(responseBody);
            JsonNode data = response.get("data");
            if (data != null && data.has("tenantId")) {
                tenantId = data.get("tenantId").asText();
            } else {
                throw new RuntimeException("Failed to extract tenant ID from create response");
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse create tenant response", e);
        }

        // Activate the tenant
        RequestHeaderHelper.addTenantHeaderIfNeeded(
                        webTestClient
                                .put()
                                .uri(String.format("/tenants/%s/activate", tenantId))
                                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", accessToken)),
                        authHelper,
                        accessToken)
                .exchange()
                .expectStatus().isNoContent();

        return tenantId;
    }

    @Test
    @DisplayName("Should create user with valid authentication")
    void shouldCreateUser() {
        // Given - Find or create active tenant, use TestData for realistic user data
        String activeTenantId = findOrCreateActiveTenant();
        String createUserRequest = String.format("""
                        {
                            "tenantId": "%s",
                            "username": "%s",
                            "emailAddress": "%s",
                            "firstName": "%s",
                            "lastName": "%s",
                            "password": "%s"
                        }
                        """,
                activeTenantId,
                testData.generateUsername(),
                testData.generateEmail(),
                testData.generateFirstName(),
                testData.generateLastName(),
                testData.getDefaultPassword());

        // When/Then
        RequestHeaderHelper.addTenantHeaderIfNeeded(
                        webTestClient
                                .post()
                                .uri("/users")
                                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", accessToken))
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(BodyInserters.fromValue(createUserRequest)),
                        authHelper,
                        accessToken)
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data.userId").exists();
    }

    @Test
    @DisplayName("Should update user profile with valid authentication")
    void shouldUpdateUserProfile() {
        // Given - Create a real user
        String testUserId = createActiveUser();
        String updateRequest = String.format("""
                        {
                            "firstName": "%s",
                            "lastName": "%s",
                            "emailAddress": "%s"
                        }
                        """,
                testData.generateFirstName(),
                testData.generateLastName(),
                testData.generateEmail());

        // When/Then
        RequestHeaderHelper.addTenantHeaderIfNeeded(
                        webTestClient
                                .put()
                                .uri(String.format("/users/%s/profile", testUserId))
                                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", accessToken))
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(BodyInserters.fromValue(updateRequest)),
                        authHelper,
                        accessToken)
                .exchange()
                .expectStatus().isNoContent();
    }

    @Test
    @DisplayName("Should activate user with valid authentication")
    void shouldActivateUser() {
        // Given - Create user in INACTIVE status (can transition to ACTIVE)
        String testUserId = createInactiveUser();

        // When/Then
        RequestHeaderHelper.addTenantHeaderIfNeeded(
                        webTestClient
                                .put()
                                .uri(String.format("/users/%s/activate", testUserId))
                                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", accessToken)),
                        authHelper,
                        accessToken)
                .exchange()
                .expectStatus().isNoContent();
    }

    /**
     * Creates a user in INACTIVE status using an active tenant.
     * Overrides the base class method to ensure an active tenant is used.
     *
     * @return User ID
     */
    @Override
    protected String createInactiveUser() {
        String activeTenantId = findOrCreateActiveTenant();
        return userBuilder()
                .withTenantId(activeTenantId)
                .withStatus(UserTestDataBuilder.UserStatus.INACTIVE)
                .build();
    }

    @Test
    @DisplayName("Should deactivate user with valid authentication")
    void shouldDeactivateUser() {
        // Given - Create user in ACTIVE status (can transition to INACTIVE)
        String testUserId = createActiveUser();

        // When/Then
        RequestHeaderHelper.addTenantHeaderIfNeeded(
                        webTestClient
                                .put()
                                .uri(String.format("/users/%s/deactivate", testUserId))
                                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", accessToken)),
                        authHelper,
                        accessToken)
                .exchange()
                .expectStatus().isNoContent();
    }

    @Test
    @DisplayName("Should suspend user with valid authentication")
    void shouldSuspendUser() {
        // Given - Create user in ACTIVE status (can transition to SUSPENDED)
        String testUserId = createActiveUser();

        // When/Then
        RequestHeaderHelper.addTenantHeaderIfNeeded(
                        webTestClient
                                .put()
                                .uri(String.format("/users/%s/suspend", testUserId))
                                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", accessToken)),
                        authHelper,
                        accessToken)
                .exchange()
                .expectStatus().isNoContent();
    }

    @Test
    @DisplayName("Should assign role to user with valid authentication")
    void shouldAssignRoleToUser() {
        // Given - Create a real user
        String testUserId = createActiveUser();
        String assignRoleRequest = """
                {
                    "roleId": "USER"
                }
                """;

        // When/Then
        RequestHeaderHelper.addTenantHeaderIfNeeded(
                        webTestClient
                                .post()
                                .uri(String.format("/users/%s/roles", testUserId))
                                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", accessToken))
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(BodyInserters.fromValue(assignRoleRequest)),
                        authHelper,
                        accessToken)
                .exchange()
                .expectStatus().isNoContent();
    }

    @Test
    @DisplayName("Should get user roles with valid authentication")
    void shouldGetUserRoles() {
        // Given - Create a real user
        String testUserId = createActiveUser();

        // When/Then - Note: GET endpoint may not be available, checking if endpoint exists
        // If GET is not supported, this test may need to be removed or the endpoint verified
        RequestHeaderHelper.addTenantHeaderIfNeeded(
                        webTestClient
                                .get()
                                .uri(String.format("/users/%s/roles", testUserId))
                                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", accessToken)),
                        authHelper,
                        accessToken)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data").exists();
    }

    @Test
    @DisplayName("Should reject user operations without authentication")
    void shouldRejectUserOperationsWithoutAuthentication() {
        // When/Then
        webTestClient
                .get()
                .uri("/users")
                .exchange()
                .expectStatus().isUnauthorized();
    }
}

