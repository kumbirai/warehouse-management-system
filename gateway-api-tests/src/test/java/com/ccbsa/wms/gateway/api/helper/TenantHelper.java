package com.ccbsa.wms.gateway.api.helper;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.ccbsa.common.application.api.ApiResponse;
import com.ccbsa.wms.gateway.api.dto.AuthenticationResult;
import com.ccbsa.wms.gateway.api.dto.CreateTenantRequest;
import com.ccbsa.wms.gateway.api.dto.CreateTenantResponse;
import com.ccbsa.wms.gateway.api.fixture.TenantTestDataBuilder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Helper for tenant-related test operations.
 */
public class TenantHelper {

    private final WebTestClient webTestClient;

    public TenantHelper(WebTestClient webTestClient) {
        this.webTestClient = webTestClient;
    }

    /**
     * Find first active tenant or create and activate one.
     *
     * <p>This method creates a tenant and immediately activates it so it can be used for testing.
     * Tenants are created with status PENDING and must be activated before they can be used.</p>
     */
    public String findOrCreateActiveTenant(AuthenticationResult auth) {
        // Create tenant (status will be PENDING)
        String tenantId = createTenant(auth);

        // Activate tenant so it can be used for testing
        activateTenant(auth, tenantId);

        return tenantId;
    }

    /**
     * Create tenant and return tenant ID.
     */
    public String createTenant(AuthenticationResult auth) {
        CreateTenantRequest request = TenantTestDataBuilder.buildCreateTenantRequest();

        EntityExchangeResult<ApiResponse<CreateTenantResponse>> exchangeResult = webTestClient.post().uri("/api/v1/tenants").headers(headers -> {
                    headers.setBearerAuth(auth.getAccessToken());
                }).contentType(MediaType.APPLICATION_JSON).bodyValue(request).exchange().expectStatus().isCreated()
                .expectBody(new ParameterizedTypeReference<ApiResponse<CreateTenantResponse>>() {
                }).returnResult();

        ApiResponse<CreateTenantResponse> apiResponse = exchangeResult.getResponseBody();
        assertThat(apiResponse).as("API response should not be null").isNotNull();
        assertThat(apiResponse.isSuccess()).as("API response should be successful").isTrue();

        CreateTenantResponse response = apiResponse.getData();
        assertThat(response).as("Create tenant response data should not be null").isNotNull();

        return response.getTenantId();
    }

    /**
     * Activate a tenant.
     *
     * @param auth     the authentication result with SYSTEM_ADMIN credentials
     * @param tenantId the tenant ID to activate
     */
    public void activateTenant(AuthenticationResult auth, String tenantId) {
        webTestClient.put().uri("/api/v1/tenants/" + tenantId + "/activate").headers(headers -> {
            headers.setBearerAuth(auth.getAccessToken());
        }).exchange().expectStatus().isNoContent();
    }
}

