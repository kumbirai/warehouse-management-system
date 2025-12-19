package com.ccbsa.wms.gateway.api.helper;

import com.ccbsa.common.application.api.ApiResponse;
import com.ccbsa.wms.gateway.api.dto.AuthenticationResult;
import com.ccbsa.wms.gateway.api.dto.AssignRoleRequest;
import com.ccbsa.wms.gateway.api.dto.CreateUserRequest;
import com.ccbsa.wms.gateway.api.dto.CreateUserResponse;
import com.ccbsa.wms.gateway.api.fixture.UserTestDataBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Helper for user-related test operations.
 */
public class UserHelper {

    private final WebTestClient webTestClient;

    public UserHelper(WebTestClient webTestClient) {
        this.webTestClient = webTestClient;
    }

    /**
     * Create user and return user ID.
     */
    public String createUser(AuthenticationResult auth, String tenantId) {
        CreateUserRequest request = UserTestDataBuilder.buildCreateUserRequest(tenantId);

        EntityExchangeResult<ApiResponse<CreateUserResponse>> exchangeResult = webTestClient.post()
                .uri("/api/v1/users")
                .headers(headers -> {
                    headers.setBearerAuth(auth.getAccessToken());
                    headers.set("X-Tenant-Id", tenantId);
                })
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(new ParameterizedTypeReference<ApiResponse<CreateUserResponse>>() {})
                .returnResult();

        ApiResponse<CreateUserResponse> apiResponse = exchangeResult.getResponseBody();
        assertThat(apiResponse)
                .as("API response should not be null")
                .isNotNull();
        assertThat(apiResponse.isSuccess())
                .as("API response should be successful")
                .isTrue();

        CreateUserResponse response = apiResponse.getData();
        assertThat(response)
                .as("Create user response data should not be null")
                .isNotNull();

        return response.getUserId();
    }

    /**
     * Create user with specific role.
     */
    public String createUserWithRole(AuthenticationResult auth, String tenantId, String role) {
        String userId = createUser(auth, tenantId);
        assignRole(auth, tenantId, userId, role);
        return userId;
    }

    /**
     * Assign role to user.
     */
    public void assignRole(AuthenticationResult auth, String tenantId, String userId, String role) {
        AssignRoleRequest request = UserTestDataBuilder.buildAssignRoleRequest(role);

        webTestClient.post()
                .uri("/api/v1/users/" + userId + "/roles")
                .headers(headers -> {
                    headers.setBearerAuth(auth.getAccessToken());
                    headers.set("X-Tenant-Id", tenantId);
                })
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().is2xxSuccessful();
    }
}

