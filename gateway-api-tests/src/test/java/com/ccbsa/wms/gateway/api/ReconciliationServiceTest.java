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
import com.ccbsa.wms.gateway.api.dto.CreateLocationResponse;
import com.ccbsa.wms.gateway.api.dto.CreateReconciliationCountRequest;
import com.ccbsa.wms.gateway.api.dto.CreateReconciliationCountResponse;
import com.ccbsa.wms.gateway.api.dto.CreateUserResponse;
import com.ccbsa.wms.gateway.api.fixture.ReconciliationTestDataBuilder;
import com.ccbsa.wms.gateway.api.fixture.TestDataManager;

import java.util.Optional;

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

            // Try to reuse location from repository, otherwise create new
            Optional<CreateLocationResponse> existingLocation = TestDataManager.getLocationByType("WAREHOUSE", testTenantId);
            CreateLocationResponse location;
            if (existingLocation.isPresent()) {
                location = existingLocation.get();
            } else {
                location = createLocation(tenantAdminAuth.getAccessToken(), testTenantId);
            }
            testLocationId = location.getLocationId();

            // Use createUser() helper which checks repository first and saves new users
            CreateUserResponse user = createUser(tenantAdminAuth.getAccessToken(), testTenantId);
            assertThat(user).isNotNull();
            testUserId = user.getUserId();
        }
    }

    // ==================== RECONCILIATION COUNT CREATION TESTS ====================

    @Test
    @Order(1)
    public void testCreateReconciliationCount_Success() {
        // Arrange
        CreateReconciliationCountRequest request = ReconciliationTestDataBuilder.buildCreateReconciliationCountRequest(testLocationId, testUserId);

        // Act
        WebTestClient.ResponseSpec response = authenticatedPost("/api/v1/reconciliation/counts", tenantAdminAuth.getAccessToken(), testTenantId, request).exchange();

        // Assert
        EntityExchangeResult<ApiResponse<CreateReconciliationCountResponse>> exchangeResult =
                response.expectStatus().isCreated().expectBody(new ParameterizedTypeReference<ApiResponse<CreateReconciliationCountResponse>>() {
                }).returnResult();

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
        WebTestClient.ResponseSpec response = authenticatedGet("/api/v1/reconciliation/counts?page=0&size=10", tenantAdminAuth.getAccessToken(), testTenantId).exchange();

        // Assert
        response.expectStatus().isOk();
    }
}

