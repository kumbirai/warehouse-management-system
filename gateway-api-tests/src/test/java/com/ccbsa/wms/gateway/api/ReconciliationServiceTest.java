package com.ccbsa.wms.gateway.api;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.ccbsa.common.application.api.ApiResponse;
import com.ccbsa.wms.gateway.api.dto.AuthenticationResult;
import com.ccbsa.wms.gateway.api.dto.CreateLocationRequest;
import com.ccbsa.wms.gateway.api.dto.CreateLocationResponse;
import com.ccbsa.wms.gateway.api.dto.CreateReconciliationCountRequest;
import com.ccbsa.wms.gateway.api.dto.CreateReconciliationCountResponse;
import com.ccbsa.wms.gateway.api.dto.CreateUserRequest;
import com.ccbsa.wms.gateway.api.dto.CreateUserResponse;
import com.ccbsa.wms.gateway.api.fixture.LocationTestDataBuilder;
import com.ccbsa.wms.gateway.api.fixture.ReconciliationTestDataBuilder;
import com.ccbsa.wms.gateway.api.fixture.UserTestDataBuilder;

import static org.assertj.core.api.Assertions.assertThat;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ReconciliationServiceTest extends BaseIntegrationTest {

    private static AuthenticationResult tenantAdminAuth;
    private static String testTenantId;
    private static String testLocationId;
    private static String testUserId;

    @BeforeAll
    public static void setupTestData() {
        // Login as TENANT_ADMIN
        // Note: This will be set up in first test
    }

    @BeforeEach
    public void setUpReconciliationTest() {
        if (tenantAdminAuth == null) {
            tenantAdminAuth = loginAsTenantAdmin();
            testTenantId = tenantAdminAuth.getTenantId();

            // Create location and user for tests
            CreateLocationRequest locationRequest = LocationTestDataBuilder.buildWarehouseRequest();
            EntityExchangeResult<ApiResponse<CreateLocationResponse>> locationExchangeResult = authenticatedPost(
                    "/api/v1/location-management/locations",
                    tenantAdminAuth.getAccessToken(),
                    testTenantId,
                    locationRequest
            ).exchange()
                    .expectStatus().isCreated()
                    .expectBody(new ParameterizedTypeReference<ApiResponse<CreateLocationResponse>>() {
                    })
                    .returnResult();

            ApiResponse<CreateLocationResponse> locationApiResponse = locationExchangeResult.getResponseBody();
            assertThat(locationApiResponse).isNotNull();
            assertThat(locationApiResponse.isSuccess()).isTrue();
            CreateLocationResponse location = locationApiResponse.getData();
            assertThat(location).isNotNull();
            testLocationId = location.getLocationId();

            CreateUserRequest userRequest = UserTestDataBuilder.buildCreateUserRequest(testTenantId);
            EntityExchangeResult<ApiResponse<CreateUserResponse>> userExchangeResult = authenticatedPost(
                    "/api/v1/users",
                    tenantAdminAuth.getAccessToken(),
                    testTenantId,
                    userRequest
            ).exchange()
                    .expectStatus().isCreated()
                    .expectBody(new ParameterizedTypeReference<ApiResponse<CreateUserResponse>>() {
                    })
                    .returnResult();

            ApiResponse<CreateUserResponse> userApiResponse = userExchangeResult.getResponseBody();
            assertThat(userApiResponse).isNotNull();
            assertThat(userApiResponse.isSuccess()).isTrue();
            CreateUserResponse user = userApiResponse.getData();
            assertThat(user).isNotNull();
            testUserId = user.getUserId();
        }
    }

    // ==================== RECONCILIATION COUNT CREATION TESTS ====================

    @Test
    @Order(1)
    public void testCreateReconciliationCount_Success() {
        // Arrange
        CreateReconciliationCountRequest request = ReconciliationTestDataBuilder.buildCreateReconciliationCountRequest(
                testLocationId, testUserId);

        // Act
        WebTestClient.ResponseSpec response = authenticatedPost(
                "/api/v1/reconciliation/counts",
                tenantAdminAuth.getAccessToken(),
                testTenantId,
                request
        ).exchange();

        // Assert
        EntityExchangeResult<ApiResponse<CreateReconciliationCountResponse>> exchangeResult = response
                .expectStatus().isCreated()
                .expectBody(new ParameterizedTypeReference<ApiResponse<CreateReconciliationCountResponse>>() {
                })
                .returnResult();

        ApiResponse<CreateReconciliationCountResponse> apiResponse = exchangeResult.getResponseBody();
        assertThat(apiResponse).isNotNull();
        assertThat(apiResponse.isSuccess()).isTrue();

        CreateReconciliationCountResponse count = apiResponse.getData();
        assertThat(count).isNotNull();
        assertThat(count.getCountId()).isNotBlank();
        assertThat(count.getStatus()).isEqualTo("SCHEDULED");
    }

    @Test
    @Order(2)
    public void testListReconciliationCounts_Success() {
        // Act
        WebTestClient.ResponseSpec response = authenticatedGet(
                "/api/v1/reconciliation/counts?page=0&size=10",
                tenantAdminAuth.getAccessToken(),
                testTenantId
        ).exchange();

        // Assert
        response.expectStatus().isOk();
    }
}

