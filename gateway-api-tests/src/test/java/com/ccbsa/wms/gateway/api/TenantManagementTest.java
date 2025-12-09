package com.ccbsa.wms.gateway.api;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.BodyInserters;

import com.ccbsa.wms.gateway.api.util.RequestHeaderHelper;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Integration tests for tenant management endpoints.
 * Tests tenant CRUD operations through the gateway.
 */
@DisplayName("Tenant Management API Tests")
class TenantManagementTest extends BaseIntegrationTest {

    @Test
    @DisplayName("Should list tenants with valid authentication")
    void shouldListTenants() {
        // When/Then
        RequestHeaderHelper.addTenantHeaderIfNeeded(
                        webTestClient
                                .get()
                                .uri("/tenants?page=0&size=10")
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
    @DisplayName("Should get tenant by ID with valid authentication")
    void shouldGetTenantById() {
        // Given - Create a tenant first
        String tenantId = createTestTenant();

        // When/Then
        RequestHeaderHelper.addTenantHeaderIfNeeded(
                        webTestClient
                                .get()
                                .uri(String.format("/tenants/%s", tenantId))
                                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", accessToken)),
                        authHelper,
                        accessToken)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data.tenantId").isEqualTo(tenantId);
    }

    /**
     * Helper method to create a tenant and return its ID.
     *
     * @return The created tenant ID
     */
    private String createTestTenant() {
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

        try {
            String responseBody = new String(responseBodyBytes, java.nio.charset.StandardCharsets.UTF_8);
            JsonNode response = objectMapper.readTree(responseBody);
            JsonNode data = response.get("data");
            if (data != null && data.has("tenantId")) {
                return data.get("tenantId").asText();
            }
            throw new RuntimeException("Failed to extract tenant ID from create response");
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse create tenant response", e);
        }
    }

    @Test
    @DisplayName("Should create tenant with valid authentication")
    void shouldCreateTenant() {
        // Given - use TestData for realistic data
        Map<String, Object> createTenantRequest = new HashMap<>();
        createTenantRequest.put("tenantId", testData.generateUniqueTenantId());
        createTenantRequest.put("name", testData.generateCompanyName());
        createTenantRequest.put("emailAddress", testData.generateEmail());
        createTenantRequest.put("phone", testData.generatePhoneNumber());

        // When/Then
        RequestHeaderHelper.addTenantHeaderIfNeeded(
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
                .jsonPath("$.data.tenantId").exists();
    }

    @Test
    @DisplayName("Should activate tenant with valid authentication")
    void shouldActivateTenant() {
        // Given - Create a tenant first (starts in PENDING status)
        String tenantId = createTestTenant();

        // When/Then
        RequestHeaderHelper.addTenantHeaderIfNeeded(
                        webTestClient
                                .put()
                                .uri(String.format("/tenants/%s/activate", tenantId))
                                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", accessToken)),
                        authHelper,
                        accessToken)
                .exchange()
                .expectStatus().isNoContent();
    }

    @Test
    @DisplayName("Should deactivate tenant with valid authentication")
    void shouldDeactivateTenant() {
        // Given - Create and activate a tenant first
        String tenantId = createTestTenant();
        // Activate the tenant first (required for deactivation)
        RequestHeaderHelper.addTenantHeaderIfNeeded(
                        webTestClient
                                .put()
                                .uri(String.format("/tenants/%s/activate", tenantId))
                                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", accessToken)),
                        authHelper,
                        accessToken)
                .exchange()
                .expectStatus().isNoContent();

        // When/Then - Deactivate the tenant
        RequestHeaderHelper.addTenantHeaderIfNeeded(
                        webTestClient
                                .put()
                                .uri(String.format("/tenants/%s/deactivate", tenantId))
                                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", accessToken)),
                        authHelper,
                        accessToken)
                .exchange()
                .expectStatus().isNoContent();
    }

    @Test
    @DisplayName("Should suspend tenant with valid authentication")
    void shouldSuspendTenant() {
        // Given - Create and activate a tenant first
        String tenantId = createTestTenant();
        // Activate the tenant first (required for suspension)
        RequestHeaderHelper.addTenantHeaderIfNeeded(
                        webTestClient
                                .put()
                                .uri(String.format("/tenants/%s/activate", tenantId))
                                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", accessToken)),
                        authHelper,
                        accessToken)
                .exchange()
                .expectStatus().isNoContent();

        // When/Then - Suspend the tenant
        RequestHeaderHelper.addTenantHeaderIfNeeded(
                        webTestClient
                                .put()
                                .uri(String.format("/tenants/%s/suspend", tenantId))
                                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", accessToken)),
                        authHelper,
                        accessToken)
                .exchange()
                .expectStatus().isNoContent();
    }

    @Test
    @DisplayName("Should reject tenant operations without authentication")
    void shouldRejectTenantOperationsWithoutAuthentication() {
        // When/Then
        webTestClient
                .get()
                .uri("/tenants")
                .exchange()
                .expectStatus().isUnauthorized();
    }
}

