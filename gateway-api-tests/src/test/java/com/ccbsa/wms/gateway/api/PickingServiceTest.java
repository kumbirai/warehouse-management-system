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
import com.ccbsa.wms.gateway.api.dto.CreateConsignmentRequest;
import com.ccbsa.wms.gateway.api.dto.CreateLocationRequest;
import com.ccbsa.wms.gateway.api.dto.CreateLocationResponse;
import com.ccbsa.wms.gateway.api.dto.CreatePickingTaskRequest;
import com.ccbsa.wms.gateway.api.dto.CreatePickingTaskResponse;
import com.ccbsa.wms.gateway.api.dto.CreateProductRequest;
import com.ccbsa.wms.gateway.api.dto.CreateProductResponse;
import com.ccbsa.wms.gateway.api.fixture.ConsignmentTestDataBuilder;
import com.ccbsa.wms.gateway.api.fixture.LocationTestDataBuilder;
import com.ccbsa.wms.gateway.api.fixture.PickingTestDataBuilder;
import com.ccbsa.wms.gateway.api.fixture.ProductTestDataBuilder;

import static org.assertj.core.api.Assertions.assertThat;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PickingServiceTest extends BaseIntegrationTest {

    private static AuthenticationResult tenantAdminAuth;
    private static String testTenantId;
    private static String testProductId;
    private static String testLocationId;

    @BeforeAll
    public static void setupTestData() {
        // Login as TENANT_ADMIN
        // Note: This will be set up in first test
    }

    @BeforeEach
    public void setUpPickingTest() {
        if (tenantAdminAuth == null) {
            tenantAdminAuth = loginAsTenantAdmin();
            testTenantId = tenantAdminAuth.getTenantId();

            // Create product, location, and stock for tests
            CreateProductRequest productRequest = ProductTestDataBuilder.buildCreateProductRequest();
            EntityExchangeResult<ApiResponse<CreateProductResponse>> productExchangeResult = authenticatedPost(
                    "/api/v1/products",
                    tenantAdminAuth.getAccessToken(),
                    testTenantId,
                    productRequest
            ).exchange()
                    .expectStatus().isCreated()
                    .expectBody(new ParameterizedTypeReference<ApiResponse<CreateProductResponse>>() {
                    })
                    .returnResult();

            ApiResponse<CreateProductResponse> productApiResponse = productExchangeResult.getResponseBody();
            assertThat(productApiResponse).isNotNull();
            assertThat(productApiResponse.isSuccess()).isTrue();
            CreateProductResponse product = productApiResponse.getData();
            assertThat(product).isNotNull();
            testProductId = product.getProductId();

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

            // Create consignment for picking
            if (testProductId != null && testLocationId != null) {
                CreateConsignmentRequest consignmentRequest = ConsignmentTestDataBuilder.buildCreateConsignmentRequestWithQuantity(
                        testProductId, testLocationId, 100);
                authenticatedPost(
                        "/api/v1/stock-management/consignments",
                        tenantAdminAuth.getAccessToken(),
                        testTenantId,
                        consignmentRequest
                ).exchange()
                        .expectStatus().isCreated();
            }
        }
    }

    // ==================== PICKING TASK CREATION TESTS ====================

    @Test
    @Order(1)
    public void testCreatePickingTask_Success() {
        // Arrange
        CreatePickingTaskRequest request = PickingTestDataBuilder.buildCreatePickingTaskRequest(
                testProductId, testLocationId);

        // Act
        WebTestClient.ResponseSpec response = authenticatedPost(
                "/api/v1/picking/tasks",
                tenantAdminAuth.getAccessToken(),
                testTenantId,
                request
        ).exchange();

        // Assert
        EntityExchangeResult<ApiResponse<CreatePickingTaskResponse>> exchangeResult = response
                .expectStatus().isCreated()
                .expectBody(new ParameterizedTypeReference<ApiResponse<CreatePickingTaskResponse>>() {
                })
                .returnResult();

        ApiResponse<CreatePickingTaskResponse> apiResponse = exchangeResult.getResponseBody();
        assertThat(apiResponse).isNotNull();
        assertThat(apiResponse.isSuccess()).isTrue();

        CreatePickingTaskResponse task = apiResponse.getData();
        assertThat(task).isNotNull();
        assertThat(task.getTaskId()).isNotBlank();
        assertThat(task.getStatus()).isEqualTo("PENDING");
    }

    @Test
    @Order(2)
    public void testListPickingTasks_Success() {
        // Act
        WebTestClient.ResponseSpec response = authenticatedGet(
                "/api/v1/picking/tasks?page=0&size=10",
                tenantAdminAuth.getAccessToken(),
                testTenantId
        ).exchange();

        // Assert
        response.expectStatus().isOk();
    }
}

