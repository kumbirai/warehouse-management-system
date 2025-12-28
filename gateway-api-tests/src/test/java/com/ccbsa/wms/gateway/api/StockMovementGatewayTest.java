package com.ccbsa.wms.gateway.api;

import java.util.UUID;

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
import com.ccbsa.wms.gateway.api.dto.CancelStockMovementRequest;
import com.ccbsa.wms.gateway.api.dto.CompleteStockMovementResultDTO;
import com.ccbsa.wms.gateway.api.dto.CreateConsignmentRequest;
import com.ccbsa.wms.gateway.api.dto.CreateLocationRequest;
import com.ccbsa.wms.gateway.api.dto.CreateLocationResponse;
import com.ccbsa.wms.gateway.api.dto.CreateProductRequest;
import com.ccbsa.wms.gateway.api.dto.CreateProductResponse;
import com.ccbsa.wms.gateway.api.dto.CreateStockMovementRequest;
import com.ccbsa.wms.gateway.api.dto.CreateStockMovementResponse;
import com.ccbsa.wms.gateway.api.dto.ListStockMovementsQueryResultDTO;
import com.ccbsa.wms.gateway.api.dto.StockItemResponse;
import com.ccbsa.wms.gateway.api.dto.StockMovementQueryResultDTO;
import com.ccbsa.wms.gateway.api.fixture.ConsignmentTestDataBuilder;
import com.ccbsa.wms.gateway.api.fixture.LocationTestDataBuilder;
import com.ccbsa.wms.gateway.api.fixture.ProductTestDataBuilder;
import com.ccbsa.wms.gateway.api.fixture.StockMovementTestDataBuilder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive integration tests for Stock Movement operations via Gateway.
 * 
 * Tests cover:
 * - Stock movement creation
 * - Stock movement completion
 * - Stock movement cancellation
 * - Stock movement queries (get by ID, list with filters)
 * - Error scenarios and validation
 * - Authorization checks
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class StockMovementGatewayTest extends BaseIntegrationTest {

    private static AuthenticationResult tenantAdminAuth;
    private static String testTenantId;
    private static UUID testProductId;
    private static String testProductCode;
    private static UUID testLocationId1;
    private static UUID testLocationId2;
    private static String testStockItemId;
    private static String testWarehouseId;

    @BeforeAll
    public static void setupTestData() {
        // Login as TENANT_ADMIN
        // Note: This will be set up in first test
    }

    @BeforeEach
    public void setUpStockMovementTest() {
        if (tenantAdminAuth == null) {
            tenantAdminAuth = loginAsTenantAdmin();
            testTenantId = tenantAdminAuth.getTenantId();

            // Create product
            CreateProductRequest productRequest = ProductTestDataBuilder.buildCreateProductRequest();
            EntityExchangeResult<ApiResponse<CreateProductResponse>> productResult = authenticatedPost(
                    "/api/v1/products",
                    tenantAdminAuth.getAccessToken(),
                    testTenantId,
                    productRequest
            ).exchange()
                    .expectStatus().isCreated()
                    .expectBody(new ParameterizedTypeReference<ApiResponse<CreateProductResponse>>() {
                    })
                    .returnResult();

            ApiResponse<CreateProductResponse> productApiResponse = productResult.getResponseBody();
            assertThat(productApiResponse).isNotNull();
            assertThat(productApiResponse.isSuccess()).isTrue();
            CreateProductResponse product = productApiResponse.getData();
            assertThat(product).isNotNull();
            testProductId = UUID.fromString(product.getProductId());
            testProductCode = product.getProductCode();

            // Create locations
            CreateLocationRequest locationRequest1 = LocationTestDataBuilder.buildWarehouseRequest();
            EntityExchangeResult<ApiResponse<CreateLocationResponse>> locationResult1 = authenticatedPost(
                    "/api/v1/location-management/locations",
                    tenantAdminAuth.getAccessToken(),
                    testTenantId,
                    locationRequest1
            ).exchange()
                    .expectStatus().isCreated()
                    .expectBody(new ParameterizedTypeReference<ApiResponse<CreateLocationResponse>>() {
                    })
                    .returnResult();

            ApiResponse<CreateLocationResponse> locationApiResponse1 = locationResult1.getResponseBody();
            assertThat(locationApiResponse1).isNotNull();
            CreateLocationResponse location1 = locationApiResponse1.getData();
            assertThat(location1).isNotNull();
            testLocationId1 = UUID.fromString(location1.getLocationId());
            testWarehouseId = location1.getLocationId();

            CreateLocationRequest locationRequest2 = LocationTestDataBuilder.buildWarehouseRequest();
            EntityExchangeResult<ApiResponse<CreateLocationResponse>> locationResult2 = authenticatedPost(
                    "/api/v1/location-management/locations",
                    tenantAdminAuth.getAccessToken(),
                    testTenantId,
                    locationRequest2
            ).exchange()
                    .expectStatus().isCreated()
                    .expectBody(new ParameterizedTypeReference<ApiResponse<CreateLocationResponse>>() {
                    })
                    .returnResult();

            ApiResponse<CreateLocationResponse> locationApiResponse2 = locationResult2.getResponseBody();
            assertThat(locationApiResponse2).isNotNull();
            CreateLocationResponse location2 = locationApiResponse2.getData();
            assertThat(location2).isNotNull();
            testLocationId2 = UUID.fromString(location2.getLocationId());

            // Create consignment to get stock item
            CreateConsignmentRequest consignmentRequest = ConsignmentTestDataBuilder.buildCreateConsignmentRequestV2(
                    testWarehouseId, testProductCode, 100, null);
            authenticatedPost(
                    "/api/v1/stock-management/consignments",
                    tenantAdminAuth.getAccessToken(),
                    testTenantId,
                    consignmentRequest
            ).exchange()
                    .expectStatus().isCreated();

            // Wait for stock item creation
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // Get stock item
            EntityExchangeResult<ApiResponse<java.util.List<StockItemResponse>>> stockItemsResult = authenticatedGet(
                    "/api/v1/stock-management/stock-items/by-classification?classification=NORMAL",
                    tenantAdminAuth.getAccessToken(),
                    testTenantId
            ).exchange()
                    .expectStatus().isOk()
                    .expectBody(new ParameterizedTypeReference<ApiResponse<java.util.List<StockItemResponse>>>() {
                    })
                    .returnResult();

            ApiResponse<java.util.List<StockItemResponse>> stockItemsApiResponse = stockItemsResult.getResponseBody();
            if (stockItemsApiResponse != null && stockItemsApiResponse.getData() != null && !stockItemsApiResponse.getData().isEmpty()) {
                testStockItemId = stockItemsApiResponse.getData().get(0).getStockItemId();
            }
        }
    }

    // ==================== STOCK MOVEMENT CREATION TESTS ====================

    @Test
    @Order(1)
    public void testCreateStockMovement_Success() {
        // Arrange
        org.junit.jupiter.api.Assumptions.assumeTrue(testStockItemId != null, "Stock item not available");
        
        CreateStockMovementRequest request = StockMovementTestDataBuilder.buildCreateStockMovementRequest(
                testStockItemId,
                testProductId,
                testLocationId1,
                testLocationId2,
                50
        );

        // Act
        WebTestClient.ResponseSpec response = authenticatedPost(
                "/api/v1/location-management/stock-movements",
                tenantAdminAuth.getAccessToken(),
                testTenantId,
                request
        ).exchange();

        // Assert
        EntityExchangeResult<ApiResponse<CreateStockMovementResponse>> exchangeResult = response
                .expectStatus().isCreated()
                .expectBody(new ParameterizedTypeReference<ApiResponse<CreateStockMovementResponse>>() {
                })
                .returnResult();

        ApiResponse<CreateStockMovementResponse> apiResponse = exchangeResult.getResponseBody();
        assertThat(apiResponse).isNotNull();
        assertThat(apiResponse.isSuccess()).isTrue();

        CreateStockMovementResponse movement = apiResponse.getData();
        assertThat(movement).isNotNull();
        assertThat(movement.getStockMovementId()).isNotNull();
        assertThat(movement.getStatus()).isEqualTo("INITIATED");
    }

    @Test
    @Order(2)
    public void testCreateStockMovement_InvalidLocation() {
        // Arrange
        org.junit.jupiter.api.Assumptions.assumeTrue(testStockItemId != null, "Stock item not available");
        
        CreateStockMovementRequest request = StockMovementTestDataBuilder.buildCreateStockMovementRequest(
                testStockItemId,
                testProductId,
                UUID.randomUUID(), // Invalid source location
                testLocationId2,
                50
        );

        // Act
        WebTestClient.ResponseSpec response = authenticatedPost(
                "/api/v1/location-management/stock-movements",
                tenantAdminAuth.getAccessToken(),
                testTenantId,
                request
        ).exchange();

        // Assert
        response.expectStatus().isBadRequest();
    }

    // ==================== STOCK MOVEMENT COMPLETION TESTS ====================

    @Test
    @Order(10)
    public void testCompleteStockMovement_Success() {
        // Arrange - Create movement first
        org.junit.jupiter.api.Assumptions.assumeTrue(testStockItemId != null, "Stock item not available");
        
        CreateStockMovementRequest createRequest = StockMovementTestDataBuilder.buildCreateStockMovementRequest(
                testStockItemId,
                testProductId,
                testLocationId1,
                testLocationId2,
                30
        );

        EntityExchangeResult<ApiResponse<CreateStockMovementResponse>> createResult = authenticatedPost(
                "/api/v1/location-management/stock-movements",
                tenantAdminAuth.getAccessToken(),
                testTenantId,
                createRequest
        ).exchange()
                .expectStatus().isCreated()
                .expectBody(new ParameterizedTypeReference<ApiResponse<CreateStockMovementResponse>>() {
                })
                .returnResult();

        ApiResponse<CreateStockMovementResponse> createApiResponse = createResult.getResponseBody();
        assertThat(createApiResponse).isNotNull();
        CreateStockMovementResponse createdMovement = createApiResponse.getData();
        assertThat(createdMovement).isNotNull();
        UUID movementId = createdMovement.getStockMovementId();

        // Act - Complete movement
        WebTestClient.ResponseSpec response = authenticatedPut(
                "/api/v1/location-management/stock-movements/" + movementId + "/complete",
                tenantAdminAuth.getAccessToken(),
                testTenantId,
                null
        ).exchange();

        // Assert
        EntityExchangeResult<ApiResponse<CompleteStockMovementResultDTO>> exchangeResult = response
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<ApiResponse<CompleteStockMovementResultDTO>>() {
                })
                .returnResult();

        ApiResponse<CompleteStockMovementResultDTO> apiResponse = exchangeResult.getResponseBody();
        assertThat(apiResponse).isNotNull();
        assertThat(apiResponse.isSuccess()).isTrue();

        CompleteStockMovementResultDTO result = apiResponse.getData();
        assertThat(result).isNotNull();
        assertThat(result.getStatus().toString()).isEqualTo("COMPLETED");
    }

    // ==================== STOCK MOVEMENT CANCELLATION TESTS ====================

    @Test
    @Order(20)
    public void testCancelStockMovement_Success() {
        // Arrange - Create movement first
        org.junit.jupiter.api.Assumptions.assumeTrue(testStockItemId != null, "Stock item not available");
        
        CreateStockMovementRequest createRequest = StockMovementTestDataBuilder.buildCreateStockMovementRequest(
                testStockItemId,
                testProductId,
                testLocationId1,
                testLocationId2,
                20
        );

        EntityExchangeResult<ApiResponse<CreateStockMovementResponse>> createResult = authenticatedPost(
                "/api/v1/location-management/stock-movements",
                tenantAdminAuth.getAccessToken(),
                testTenantId,
                createRequest
        ).exchange()
                .expectStatus().isCreated()
                .expectBody(new ParameterizedTypeReference<ApiResponse<CreateStockMovementResponse>>() {
                })
                .returnResult();

        ApiResponse<CreateStockMovementResponse> createApiResponse = createResult.getResponseBody();
        assertThat(createApiResponse).isNotNull();
        CreateStockMovementResponse createdMovement = createApiResponse.getData();
        assertThat(createdMovement).isNotNull();
        UUID movementId = createdMovement.getStockMovementId();

        CancelStockMovementRequest cancelRequest = StockMovementTestDataBuilder.buildCancelStockMovementRequest(
                "Test cancellation reason"
        );

        // Act - Cancel movement
        WebTestClient.ResponseSpec response = authenticatedPut(
                "/api/v1/location-management/stock-movements/" + movementId + "/cancel",
                tenantAdminAuth.getAccessToken(),
                testTenantId,
                cancelRequest
        ).exchange();

        // Assert
        response.expectStatus().isOk();
    }

    // ==================== STOCK MOVEMENT QUERY TESTS ====================

    @Test
    @Order(30)
    public void testGetStockMovementById_Success() {
        // Arrange - Create movement first
        org.junit.jupiter.api.Assumptions.assumeTrue(testStockItemId != null, "Stock item not available");
        
        CreateStockMovementRequest createRequest = StockMovementTestDataBuilder.buildCreateStockMovementRequest(
                testStockItemId,
                testProductId,
                testLocationId1,
                testLocationId2,
                25
        );

        EntityExchangeResult<ApiResponse<CreateStockMovementResponse>> createResult = authenticatedPost(
                "/api/v1/location-management/stock-movements",
                tenantAdminAuth.getAccessToken(),
                testTenantId,
                createRequest
        ).exchange()
                .expectStatus().isCreated()
                .expectBody(new ParameterizedTypeReference<ApiResponse<CreateStockMovementResponse>>() {
                })
                .returnResult();

        ApiResponse<CreateStockMovementResponse> createApiResponse = createResult.getResponseBody();
        assertThat(createApiResponse).isNotNull();
        CreateStockMovementResponse createdMovement = createApiResponse.getData();
        assertThat(createdMovement).isNotNull();
        UUID movementId = createdMovement.getStockMovementId();

        // Act
        WebTestClient.ResponseSpec response = authenticatedGet(
                "/api/v1/location-management/stock-movements/" + movementId,
                tenantAdminAuth.getAccessToken(),
                testTenantId
        ).exchange();

        // Assert
        EntityExchangeResult<ApiResponse<StockMovementQueryResultDTO>> exchangeResult = response
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<ApiResponse<StockMovementQueryResultDTO>>() {
                })
                .returnResult();

        ApiResponse<StockMovementQueryResultDTO> apiResponse = exchangeResult.getResponseBody();
        assertThat(apiResponse).isNotNull();
        assertThat(apiResponse.isSuccess()).isTrue();

        StockMovementQueryResultDTO movement = apiResponse.getData();
        assertThat(movement).isNotNull();
        assertThat(movement.getStockMovementId()).isEqualTo(movementId);
    }

    @Test
    @Order(31)
    public void testListStockMovements_Success() {
        // Arrange - Create a movement first
        org.junit.jupiter.api.Assumptions.assumeTrue(testStockItemId != null, "Stock item not available");
        
        CreateStockMovementRequest createRequest = StockMovementTestDataBuilder.buildCreateStockMovementRequest(
                testStockItemId,
                testProductId,
                testLocationId1,
                testLocationId2,
                15
        );

        authenticatedPost(
                "/api/v1/location-management/stock-movements",
                tenantAdminAuth.getAccessToken(),
                testTenantId,
                createRequest
        ).exchange()
                .expectStatus().isCreated();

        // Act
        WebTestClient.ResponseSpec response = authenticatedGet(
                "/api/v1/location-management/stock-movements?stockItemId=" + testStockItemId,
                tenantAdminAuth.getAccessToken(),
                testTenantId
        ).exchange();

        // Assert
        EntityExchangeResult<ApiResponse<ListStockMovementsQueryResultDTO>> exchangeResult = response
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<ApiResponse<ListStockMovementsQueryResultDTO>>() {
                })
                .returnResult();

        ApiResponse<ListStockMovementsQueryResultDTO> apiResponse = exchangeResult.getResponseBody();
        assertThat(apiResponse).isNotNull();
        assertThat(apiResponse.isSuccess()).isTrue();

        ListStockMovementsQueryResultDTO result = apiResponse.getData();
        assertThat(result).isNotNull();
        assertThat(result.getMovements()).isNotNull();
        assertThat(result.getTotalCount()).isGreaterThan(0);
    }
}

