package com.ccbsa.wms.gateway.api;

import java.util.UUID;

import org.junit.jupiter.api.Assumptions;
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
import com.ccbsa.wms.gateway.api.dto.AssignLocationToStockRequest;
import com.ccbsa.wms.gateway.api.dto.StockItemResponse;
import com.ccbsa.wms.gateway.api.dto.StockItemsByClassificationResponse;
import com.ccbsa.wms.gateway.api.dto.StockMovementQueryResultDTO;
import com.ccbsa.wms.gateway.api.fixture.ConsignmentTestDataBuilder;
import com.ccbsa.wms.gateway.api.fixture.LocationTestDataBuilder;
import com.ccbsa.wms.gateway.api.fixture.ProductTestDataBuilder;
import com.ccbsa.wms.gateway.api.fixture.StockItemTestDataBuilder;
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
    private static UUID testLocationId1; // Source BIN location
    private static UUID testLocationId2; // Destination BIN location
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

            // Create warehouse first
            CreateLocationRequest warehouseRequest = LocationTestDataBuilder.buildWarehouseRequest();
            EntityExchangeResult<ApiResponse<CreateLocationResponse>> warehouseResult = authenticatedPost(
                    "/api/v1/location-management/locations",
                    tenantAdminAuth.getAccessToken(),
                    testTenantId,
                    warehouseRequest
            ).exchange()
                    .expectStatus().isCreated()
                    .expectBody(new ParameterizedTypeReference<ApiResponse<CreateLocationResponse>>() {
                    })
                    .returnResult();

            ApiResponse<CreateLocationResponse> warehouseApiResponse = warehouseResult.getResponseBody();
            assertThat(warehouseApiResponse).isNotNull();
            assertThat(warehouseApiResponse.isSuccess()).isTrue();
            CreateLocationResponse warehouse = warehouseApiResponse.getData();
            assertThat(warehouse).isNotNull();
            testWarehouseId = warehouse.getLocationId();

            // Create location hierarchy: WAREHOUSE -> ZONE -> AISLE -> RACK -> BIN
            // Create zone
            CreateLocationRequest zoneRequest = LocationTestDataBuilder.buildZoneRequest(testWarehouseId);
            EntityExchangeResult<ApiResponse<CreateLocationResponse>> zoneResult = authenticatedPost(
                    "/api/v1/location-management/locations",
                    tenantAdminAuth.getAccessToken(),
                    testTenantId,
                    zoneRequest
            ).exchange()
                    .expectStatus().isCreated()
                    .expectBody(new ParameterizedTypeReference<ApiResponse<CreateLocationResponse>>() {
                    })
                    .returnResult();

            ApiResponse<CreateLocationResponse> zoneApiResponse = zoneResult.getResponseBody();
            assertThat(zoneApiResponse).isNotNull();
            assertThat(zoneApiResponse.isSuccess()).isTrue();
            CreateLocationResponse zone = zoneApiResponse.getData();
            assertThat(zone).isNotNull();

            // Create aisle
            CreateLocationRequest aisleRequest = LocationTestDataBuilder.buildAisleRequest(zone.getLocationId());
            EntityExchangeResult<ApiResponse<CreateLocationResponse>> aisleResult = authenticatedPost(
                    "/api/v1/location-management/locations",
                    tenantAdminAuth.getAccessToken(),
                    testTenantId,
                    aisleRequest
            ).exchange()
                    .expectStatus().isCreated()
                    .expectBody(new ParameterizedTypeReference<ApiResponse<CreateLocationResponse>>() {
                    })
                    .returnResult();

            ApiResponse<CreateLocationResponse> aisleApiResponse = aisleResult.getResponseBody();
            assertThat(aisleApiResponse).isNotNull();
            assertThat(aisleApiResponse.isSuccess()).isTrue();
            CreateLocationResponse aisle = aisleApiResponse.getData();
            assertThat(aisle).isNotNull();

            // Create rack
            CreateLocationRequest rackRequest = LocationTestDataBuilder.buildRackRequest(aisle.getLocationId());
            EntityExchangeResult<ApiResponse<CreateLocationResponse>> rackResult = authenticatedPost(
                    "/api/v1/location-management/locations",
                    tenantAdminAuth.getAccessToken(),
                    testTenantId,
                    rackRequest
            ).exchange()
                    .expectStatus().isCreated()
                    .expectBody(new ParameterizedTypeReference<ApiResponse<CreateLocationResponse>>() {
                    })
                    .returnResult();

            ApiResponse<CreateLocationResponse> rackApiResponse = rackResult.getResponseBody();
            assertThat(rackApiResponse).isNotNull();
            assertThat(rackApiResponse.isSuccess()).isTrue();
            CreateLocationResponse rack = rackApiResponse.getData();
            assertThat(rack).isNotNull();

            // Create source BIN location
            CreateLocationRequest binRequest1 = LocationTestDataBuilder.buildBinRequest(rack.getLocationId());
            EntityExchangeResult<ApiResponse<CreateLocationResponse>> binResult1 = authenticatedPost(
                    "/api/v1/location-management/locations",
                    tenantAdminAuth.getAccessToken(),
                    testTenantId,
                    binRequest1
            ).exchange()
                    .expectStatus().isCreated()
                    .expectBody(new ParameterizedTypeReference<ApiResponse<CreateLocationResponse>>() {
                    })
                    .returnResult();

            ApiResponse<CreateLocationResponse> binApiResponse1 = binResult1.getResponseBody();
            assertThat(binApiResponse1).isNotNull();
            assertThat(binApiResponse1.isSuccess()).isTrue();
            CreateLocationResponse bin1 = binApiResponse1.getData();
            assertThat(bin1).isNotNull();
            testLocationId1 = UUID.fromString(bin1.getLocationId());

            // Create destination BIN location (another rack in same aisle)
            CreateLocationRequest rackRequest2 = LocationTestDataBuilder.buildRackRequest(aisle.getLocationId());
            EntityExchangeResult<ApiResponse<CreateLocationResponse>> rackResult2 = authenticatedPost(
                    "/api/v1/location-management/locations",
                    tenantAdminAuth.getAccessToken(),
                    testTenantId,
                    rackRequest2
            ).exchange()
                    .expectStatus().isCreated()
                    .expectBody(new ParameterizedTypeReference<ApiResponse<CreateLocationResponse>>() {
                    })
                    .returnResult();

            ApiResponse<CreateLocationResponse> rackApiResponse2 = rackResult2.getResponseBody();
            assertThat(rackApiResponse2).isNotNull();
            assertThat(rackApiResponse2.isSuccess()).isTrue();
            CreateLocationResponse rack2 = rackApiResponse2.getData();
            assertThat(rack2).isNotNull();

            CreateLocationRequest binRequest2 = LocationTestDataBuilder.buildBinRequest(rack2.getLocationId());
            EntityExchangeResult<ApiResponse<CreateLocationResponse>> binResult2 = authenticatedPost(
                    "/api/v1/location-management/locations",
                    tenantAdminAuth.getAccessToken(),
                    testTenantId,
                    binRequest2
            ).exchange()
                    .expectStatus().isCreated()
                    .expectBody(new ParameterizedTypeReference<ApiResponse<CreateLocationResponse>>() {
                    })
                    .returnResult();

            ApiResponse<CreateLocationResponse> binApiResponse2 = binResult2.getResponseBody();
            assertThat(binApiResponse2).isNotNull();
            assertThat(binApiResponse2.isSuccess()).isTrue();
            CreateLocationResponse bin2 = binApiResponse2.getData();
            assertThat(bin2).isNotNull();
            testLocationId2 = UUID.fromString(bin2.getLocationId());

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

            // Wait for stock items to be created from consignment (async via Kafka events)
            boolean stockItemsCreated = waitForStockItems(testProductId.toString(), tenantAdminAuth.getAccessToken(), testTenantId, 10, 500);
            assertThat(stockItemsCreated).as("Stock items should be created from consignment within 10 seconds").isTrue();

            // Get stock item
            EntityExchangeResult<ApiResponse<StockItemsByClassificationResponse>> stockItemsResult = authenticatedGet(
                    "/api/v1/stock-management/stock-items/by-classification?classification=NORMAL",
                    tenantAdminAuth.getAccessToken(),
                    testTenantId
            ).exchange()
                    .expectStatus().isOk()
                    .expectBody(new ParameterizedTypeReference<ApiResponse<StockItemsByClassificationResponse>>() {
                    })
                    .returnResult();

            ApiResponse<StockItemsByClassificationResponse> stockItemsApiResponse = stockItemsResult.getResponseBody();
            if (stockItemsApiResponse != null && stockItemsApiResponse.getData() != null 
                    && stockItemsApiResponse.getData().getStockItems() != null 
                    && !stockItemsApiResponse.getData().getStockItems().isEmpty()) {
                StockItemResponse stockItem = stockItemsApiResponse.getData().getStockItems().get(0);
                testStockItemId = stockItem.getStockItemId();

                // Assign stock item to source BIN location (stock must be at BIN level)
                AssignLocationToStockRequest assignRequest =
                        StockItemTestDataBuilder.buildAssignLocationRequest(
                                testLocationId1.toString(), stockItem.getQuantity());

                authenticatedPost(
                        "/api/v1/stock-management/stock-items/" + testStockItemId + "/assign-location",
                        tenantAdminAuth.getAccessToken(),
                        testTenantId,
                        assignRequest
                ).exchange()
                        .expectStatus().isOk();
            }
        }
    }

    // ==================== STOCK MOVEMENT CREATION TESTS ====================

    @Test
    @Order(1)
    public void testCreateStockMovement_Success() {
        // Arrange
        Assumptions.assumeTrue(testStockItemId != null, "Stock item not available");
        
        CreateStockMovementRequest request = StockMovementTestDataBuilder.buildCreateStockMovementRequest(
                testStockItemId,
                testProductId,
                testLocationId1,
                testLocationId2,
                10
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
        Assumptions.assumeTrue(testStockItemId != null, "Stock item not available");
        
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

        // Assert - LocationNotFoundException returns 404 NOT_FOUND
        response.expectStatus().isNotFound();
    }

    @Test
    @Order(3)
    public void testCreateStockMovement_NonBinSourceLocation_ShouldFail() {
        // Arrange - Create non-BIN location (warehouse) to test validation
        Assumptions.assumeTrue(testStockItemId != null, "Stock item not available");
        
        CreateLocationRequest warehouseRequest = LocationTestDataBuilder.buildWarehouseRequest();
        EntityExchangeResult<ApiResponse<CreateLocationResponse>> warehouseResult = authenticatedPost(
                "/api/v1/location-management/locations",
                tenantAdminAuth.getAccessToken(),
                testTenantId,
                warehouseRequest
        ).exchange()
                .expectStatus().isCreated()
                .expectBody(new ParameterizedTypeReference<ApiResponse<CreateLocationResponse>>() {
                })
                .returnResult();

        ApiResponse<CreateLocationResponse> warehouseApiResponse = warehouseResult.getResponseBody();
        assertThat(warehouseApiResponse).isNotNull();
        assertThat(warehouseApiResponse.isSuccess()).isTrue();
        CreateLocationResponse warehouse = warehouseApiResponse.getData();
        assertThat(warehouse).isNotNull();
        UUID warehouseLocationId = UUID.fromString(warehouse.getLocationId());

        // Act - Try to create stock movement with warehouse (non-BIN) as source location
        CreateStockMovementRequest request = StockMovementTestDataBuilder.buildCreateStockMovementRequest(
                testStockItemId,
                testProductId,
                warehouseLocationId, // Non-BIN source location
                testLocationId2,
                10
        );

        WebTestClient.ResponseSpec response = authenticatedPost(
                "/api/v1/location-management/stock-movements",
                tenantAdminAuth.getAccessToken(),
                testTenantId,
                request
        ).exchange();

        // Assert - Should fail with 400 Bad Request because source location is not BIN type
        // Note: This test documents the requirement. If implementation doesn't validate yet,
        // the test may need to be updated when validation is added.
        response.expectStatus().isBadRequest();
    }

    @Test
    @Order(4)
    public void testCreateStockMovement_NonBinDestinationLocation_ShouldFail() {
        // Arrange - Create non-BIN location (warehouse) to test validation
        Assumptions.assumeTrue(testStockItemId != null, "Stock item not available");
        
        CreateLocationRequest warehouseRequest = LocationTestDataBuilder.buildWarehouseRequest();
        EntityExchangeResult<ApiResponse<CreateLocationResponse>> warehouseResult = authenticatedPost(
                "/api/v1/location-management/locations",
                tenantAdminAuth.getAccessToken(),
                testTenantId,
                warehouseRequest
        ).exchange()
                .expectStatus().isCreated()
                .expectBody(new ParameterizedTypeReference<ApiResponse<CreateLocationResponse>>() {
                })
                .returnResult();

        ApiResponse<CreateLocationResponse> warehouseApiResponse = warehouseResult.getResponseBody();
        assertThat(warehouseApiResponse).isNotNull();
        assertThat(warehouseApiResponse.isSuccess()).isTrue();
        CreateLocationResponse warehouse = warehouseApiResponse.getData();
        assertThat(warehouse).isNotNull();
        UUID warehouseLocationId = UUID.fromString(warehouse.getLocationId());

        // Act - Try to create stock movement with warehouse (non-BIN) as destination location
        CreateStockMovementRequest request = StockMovementTestDataBuilder.buildCreateStockMovementRequest(
                testStockItemId,
                testProductId,
                testLocationId1,
                warehouseLocationId, // Non-BIN destination location
                10
        );

        WebTestClient.ResponseSpec response = authenticatedPost(
                "/api/v1/location-management/stock-movements",
                tenantAdminAuth.getAccessToken(),
                testTenantId,
                request
        ).exchange();

        // Assert - Should fail with 400 Bad Request because destination location is not BIN type
        // Note: This test documents the requirement. If implementation doesn't validate yet,
        // the test may need to be updated when validation is added.
        response.expectStatus().isBadRequest();
    }

    // ==================== STOCK MOVEMENT COMPLETION TESTS ====================

    @Test
    @Order(10)
    public void testCompleteStockMovement_Success() {
        // Arrange - Create movement first
        Assumptions.assumeTrue(testStockItemId != null, "Stock item not available");
        
        CreateStockMovementRequest createRequest = StockMovementTestDataBuilder.buildCreateStockMovementRequest(
                testStockItemId,
                testProductId,
                testLocationId1,
                testLocationId2,
                5
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
        Assumptions.assumeTrue(testStockItemId != null, "Stock item not available");
        
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
        Assumptions.assumeTrue(testStockItemId != null, "Stock item not available");
        
        CreateStockMovementRequest createRequest = StockMovementTestDataBuilder.buildCreateStockMovementRequest(
                testStockItemId,
                testProductId,
                testLocationId1,
                testLocationId2,
                5
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
        Assumptions.assumeTrue(testStockItemId != null, "Stock item not available");
        
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

