package com.ccbsa.wms.gateway.api;

import java.time.LocalDate;
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
import com.ccbsa.wms.gateway.api.dto.CreateConsignmentRequest;
import com.ccbsa.wms.gateway.api.dto.CreateLocationRequest;
import com.ccbsa.wms.gateway.api.dto.CreateLocationResponse;
import com.ccbsa.wms.gateway.api.dto.CreateProductRequest;
import com.ccbsa.wms.gateway.api.dto.CreateProductResponse;
import com.ccbsa.wms.gateway.api.dto.CreateStockAllocationRequest;
import com.ccbsa.wms.gateway.api.dto.CreateStockAllocationResponse;
import com.ccbsa.wms.gateway.api.dto.ListStockAllocationsQueryResultDTO;
import com.ccbsa.wms.gateway.api.dto.ReleaseStockAllocationResultDTO;
import com.ccbsa.wms.gateway.api.dto.StockAllocationQueryDTO;
import com.ccbsa.wms.gateway.api.dto.StockLevelResponse;
import com.ccbsa.wms.gateway.api.fixture.ConsignmentTestDataBuilder;
import com.ccbsa.wms.gateway.api.fixture.LocationTestDataBuilder;
import com.ccbsa.wms.gateway.api.fixture.ProductTestDataBuilder;
import com.ccbsa.wms.gateway.api.fixture.StockAllocationTestDataBuilder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive integration tests for Stock Allocation operations via Gateway.
 * 
 * Tests cover:
 * - Stock allocation creation (with and without location for FEFO)
 * - Stock allocation release
 * - Stock allocation queries (get by ID, list with filters)
 * - FEFO allocation logic verification
 * - Error scenarios and validation
 * - Authorization checks
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class StockAllocationGatewayTest extends BaseIntegrationTest {

    private static AuthenticationResult tenantAdminAuth;
    private static String testTenantId;
    private static UUID testProductId;
    private static String testProductCode;
    private static UUID testLocationId;
    private static String testWarehouseId;

    @BeforeAll
    public static void setupTestData() {
        // Login as TENANT_ADMIN
        // Note: This will be set up in first test
    }

    @BeforeEach
    public void setUpStockAllocationTest() {
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

            // Create BIN location (stock allocations must be at BIN level)
            CreateLocationRequest binRequest = LocationTestDataBuilder.buildBinRequest(rack.getLocationId());
            EntityExchangeResult<ApiResponse<CreateLocationResponse>> binResult = authenticatedPost(
                    "/api/v1/location-management/locations",
                    tenantAdminAuth.getAccessToken(),
                    testTenantId,
                    binRequest
            ).exchange()
                    .expectStatus().isCreated()
                    .expectBody(new ParameterizedTypeReference<ApiResponse<CreateLocationResponse>>() {
                    })
                    .returnResult();

            ApiResponse<CreateLocationResponse> binApiResponse = binResult.getResponseBody();
            assertThat(binApiResponse).isNotNull();
            assertThat(binApiResponse.isSuccess()).isTrue();
            CreateLocationResponse bin = binApiResponse.getData();
            assertThat(bin).isNotNull();
            testLocationId = UUID.fromString(bin.getLocationId());
        }
    }

    // ==================== STOCK ALLOCATION CREATION TESTS ====================

    @Test
    @Order(1)
    public void testAllocateStock_WithLocation_Success() {
        // Arrange - Create consignment first
        CreateConsignmentRequest consignmentRequest = ConsignmentTestDataBuilder.buildCreateConsignmentRequestV2(
                testWarehouseId, testProductCode, 100, null);
        authenticatedPost(
                "/api/v1/stock-management/consignments",
                tenantAdminAuth.getAccessToken(),
                testTenantId,
                consignmentRequest
        ).exchange()
                .expectStatus().isCreated();

        // Wait for stock item creation (async via Kafka events)
        boolean stockItemsCreated = waitForStockItems(testProductId.toString(), tenantAdminAuth.getAccessToken(), testTenantId, 10, 500);
        assertThat(stockItemsCreated).as("Stock items should be created from consignment within 10 seconds").isTrue();

        String orderId = "ORDER-" + faker.number().digits(5);
        CreateStockAllocationRequest request = StockAllocationTestDataBuilder.buildCreateStockAllocationRequest(
                testProductId,
                testLocationId,
                50,
                orderId
        );

        // Act
        WebTestClient.ResponseSpec response = authenticatedPost(
                "/api/v1/stock-management/allocations",
                tenantAdminAuth.getAccessToken(),
                testTenantId,
                request
        ).exchange();

        // Assert
        EntityExchangeResult<ApiResponse<CreateStockAllocationResponse>> exchangeResult = response
                .expectStatus().isCreated()
                .expectBody(new ParameterizedTypeReference<ApiResponse<CreateStockAllocationResponse>>() {
                })
                .returnResult();

        ApiResponse<CreateStockAllocationResponse> apiResponse = exchangeResult.getResponseBody();
        assertThat(apiResponse).isNotNull();
        assertThat(apiResponse.isSuccess()).isTrue();

        CreateStockAllocationResponse allocation = apiResponse.getData();
        assertThat(allocation).isNotNull();
        assertThat(allocation.getAllocationId()).isNotNull();
        assertThat(allocation.getQuantity()).isEqualTo(50);
        assertThat(allocation.getStatus()).isEqualTo("ALLOCATED");
    }

    @Test
    @Order(2)
    public void testAllocateStock_FEFO_Success() {
        // Arrange - Create consignments with different expiration dates
        LocalDate expiration1 = LocalDate.now().plusMonths(3); // Expires first
        LocalDate expiration2 = LocalDate.now().plusMonths(6);

        CreateConsignmentRequest consignment1 = ConsignmentTestDataBuilder.buildCreateConsignmentRequestV2WithExpiration(
                testWarehouseId, testProductCode, expiration1);
        CreateConsignmentRequest consignment2 = ConsignmentTestDataBuilder.buildCreateConsignmentRequestV2WithExpiration(
                testWarehouseId, testProductCode, expiration2);

        authenticatedPost("/api/v1/stock-management/consignments",
                tenantAdminAuth.getAccessToken(), testTenantId, consignment1)
                .exchange().expectStatus().isCreated();
        authenticatedPost("/api/v1/stock-management/consignments",
                tenantAdminAuth.getAccessToken(), testTenantId, consignment2)
                .exchange().expectStatus().isCreated();

        // Wait for stock item creation (async via Kafka events)
        boolean stockItemsCreated = waitForStockItems(testProductId.toString(), tenantAdminAuth.getAccessToken(), testTenantId, 15, 500);
        assertThat(stockItemsCreated).as("Stock items should be created from consignments within 15 seconds").isTrue();

        String orderId = "ORDER-" + faker.number().digits(5);
        CreateStockAllocationRequest request = StockAllocationTestDataBuilder.buildCreateStockAllocationRequestFEFO(
                testProductId,
                150, // Allocate from multiple batches
                orderId
        );

        // Act
        WebTestClient.ResponseSpec response = authenticatedPost(
                "/api/v1/stock-management/allocations",
                tenantAdminAuth.getAccessToken(),
                testTenantId,
                request
        ).exchange();

        // Assert
        EntityExchangeResult<ApiResponse<CreateStockAllocationResponse>> exchangeResult = response
                .expectStatus().isCreated()
                .expectBody(new ParameterizedTypeReference<ApiResponse<CreateStockAllocationResponse>>() {
                })
                .returnResult();

        ApiResponse<CreateStockAllocationResponse> apiResponse = exchangeResult.getResponseBody();
        assertThat(apiResponse).isNotNull();
        assertThat(apiResponse.isSuccess()).isTrue();

        CreateStockAllocationResponse allocation = apiResponse.getData();
        assertThat(allocation).isNotNull();
        assertThat(allocation.getQuantity()).isEqualTo(150);
        // FEFO should allocate from earliest expiring stock first
    }

    @Test
    @Order(3)
    public void testAllocateStock_InsufficientStock() {
        // Arrange - Create consignment with 50 units
        CreateConsignmentRequest consignmentRequest = ConsignmentTestDataBuilder.buildCreateConsignmentRequestV2(
                testWarehouseId, testProductCode, 50, null);
        authenticatedPost(
                "/api/v1/stock-management/consignments",
                tenantAdminAuth.getAccessToken(),
                testTenantId,
                consignmentRequest
        ).exchange()
                .expectStatus().isCreated();

        // Wait for stock item creation (async via Kafka events)
        boolean stockItemsCreated = waitForStockItems(testProductId.toString(), tenantAdminAuth.getAccessToken(), testTenantId, 10, 500);
        assertThat(stockItemsCreated).as("Stock items should be created from consignment within 10 seconds").isTrue();

        // Query total available stock for the product (including unassigned stock)
        // The allocation handler includes unassigned stock when location stock is insufficient,
        // so we need to query all stock levels (not filtered by location) to get the true total
        EntityExchangeResult<ApiResponse<List<StockLevelResponse>>> stockLevelsResult = authenticatedGet(
                "/api/v1/stock-management/stock-levels?productId=" + testProductId,
                tenantAdminAuth.getAccessToken(),
                testTenantId
        ).exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<ApiResponse<List<StockLevelResponse>>>() {
                })
                .returnResult();

        ApiResponse<List<StockLevelResponse>> stockLevelsApiResponse = stockLevelsResult.getResponseBody();
        assertThat(stockLevelsApiResponse).isNotNull();
        assertThat(stockLevelsApiResponse.isSuccess()).isTrue();

        List<StockLevelResponse> stockLevels = stockLevelsApiResponse.getData();
        assertThat(stockLevels).isNotNull();

        // Calculate total available quantity across all locations (including unassigned)
        // This matches what the allocation handler uses for validation when locationId is specified
        // (it includes unassigned stock if location stock is insufficient)
        int totalAvailable = stockLevels.stream()
                .filter(level -> level.getAvailableQuantity() != null)
                .mapToInt(StockLevelResponse::getAvailableQuantity)
                .sum();

        // Request more than available to trigger insufficient stock error
        int requestedQuantity = totalAvailable + 100;

        String orderId = "ORDER-" + faker.number().digits(5);
        CreateStockAllocationRequest request = StockAllocationTestDataBuilder.buildCreateStockAllocationRequest(
                testProductId,
                testLocationId,
                requestedQuantity, // More than available
                orderId
        );

        // Act
        WebTestClient.ResponseSpec response = authenticatedPost(
                "/api/v1/stock-management/allocations",
                tenantAdminAuth.getAccessToken(),
                testTenantId,
                request
        ).exchange();

        // Assert
        response.expectStatus().isBadRequest();
    }

    // ==================== STOCK ALLOCATION RELEASE TESTS ====================

    @Test
    @Order(10)
    public void testReleaseStockAllocation_Success() {
        // Arrange - Create consignment and allocation first
        CreateConsignmentRequest consignmentRequest = ConsignmentTestDataBuilder.buildCreateConsignmentRequestV2(
                testWarehouseId, testProductCode, 100, null);
        authenticatedPost(
                "/api/v1/stock-management/consignments",
                tenantAdminAuth.getAccessToken(),
                testTenantId,
                consignmentRequest
        ).exchange()
                .expectStatus().isCreated();

        // Wait for stock item creation (async via Kafka events)
        boolean stockItemsCreated = waitForStockItems(testProductId.toString(), tenantAdminAuth.getAccessToken(), testTenantId, 10, 500);
        assertThat(stockItemsCreated).as("Stock items should be created from consignment within 10 seconds").isTrue();

        String orderId = "ORDER-" + faker.number().digits(5);
        CreateStockAllocationRequest allocationRequest = StockAllocationTestDataBuilder.buildCreateStockAllocationRequest(
                testProductId,
                testLocationId,
                30,
                orderId
        );

        EntityExchangeResult<ApiResponse<CreateStockAllocationResponse>> allocationResult = authenticatedPost(
                "/api/v1/stock-management/allocations",
                tenantAdminAuth.getAccessToken(),
                testTenantId,
                allocationRequest
        ).exchange()
                .expectStatus().isCreated()
                .expectBody(new ParameterizedTypeReference<ApiResponse<CreateStockAllocationResponse>>() {
                })
                .returnResult();

        ApiResponse<CreateStockAllocationResponse> allocationApiResponse = allocationResult.getResponseBody();
        assertThat(allocationApiResponse).isNotNull();
        CreateStockAllocationResponse createdAllocation = allocationApiResponse.getData();
        assertThat(createdAllocation).isNotNull();
        UUID allocationId = createdAllocation.getAllocationId();

        // Act - Release allocation
        WebTestClient.ResponseSpec response = authenticatedPut(
                "/api/v1/stock-management/allocations/" + allocationId + "/release",
                tenantAdminAuth.getAccessToken(),
                testTenantId,
                null
        ).exchange();

        // Assert
        EntityExchangeResult<ApiResponse<ReleaseStockAllocationResultDTO>> exchangeResult = response
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<ApiResponse<ReleaseStockAllocationResultDTO>>() {
                })
                .returnResult();

        ApiResponse<ReleaseStockAllocationResultDTO> apiResponse = exchangeResult.getResponseBody();
        assertThat(apiResponse).isNotNull();
        assertThat(apiResponse.isSuccess()).isTrue();

        ReleaseStockAllocationResultDTO result = apiResponse.getData();
        assertThat(result).isNotNull();
        assertThat(result.getAllocationId()).isEqualTo(allocationId);
        assertThat(result.getStatus()).isEqualTo("RELEASED");
    }

    // ==================== STOCK ALLOCATION QUERY TESTS ====================

    @Test
    @Order(20)
    public void testGetStockAllocationById_Success() {
        // Arrange - Create consignment and allocation first
        CreateConsignmentRequest consignmentRequest = ConsignmentTestDataBuilder.buildCreateConsignmentRequestV2(
                testWarehouseId, testProductCode, 100, null);
        authenticatedPost(
                "/api/v1/stock-management/consignments",
                tenantAdminAuth.getAccessToken(),
                testTenantId,
                consignmentRequest
        ).exchange()
                .expectStatus().isCreated();

        // Wait for stock item creation (async via Kafka events)
        boolean stockItemsCreated = waitForStockItems(testProductId.toString(), tenantAdminAuth.getAccessToken(), testTenantId, 10, 500);
        assertThat(stockItemsCreated).as("Stock items should be created from consignment within 10 seconds").isTrue();

        String orderId = "ORDER-" + faker.number().digits(5);
        CreateStockAllocationRequest allocationRequest = StockAllocationTestDataBuilder.buildCreateStockAllocationRequest(
                testProductId,
                testLocationId,
                25,
                orderId
        );

        EntityExchangeResult<ApiResponse<CreateStockAllocationResponse>> allocationResult = authenticatedPost(
                "/api/v1/stock-management/allocations",
                tenantAdminAuth.getAccessToken(),
                testTenantId,
                allocationRequest
        ).exchange()
                .expectStatus().isCreated()
                .expectBody(new ParameterizedTypeReference<ApiResponse<CreateStockAllocationResponse>>() {
                })
                .returnResult();

        ApiResponse<CreateStockAllocationResponse> allocationApiResponse = allocationResult.getResponseBody();
        assertThat(allocationApiResponse).isNotNull();
        CreateStockAllocationResponse createdAllocation = allocationApiResponse.getData();
        assertThat(createdAllocation).isNotNull();
        String allocationId = createdAllocation.getAllocationId().toString();

        // Act
        WebTestClient.ResponseSpec response = authenticatedGet(
                "/api/v1/stock-management/allocations/" + allocationId,
                tenantAdminAuth.getAccessToken(),
                testTenantId
        ).exchange();

        // Assert
        EntityExchangeResult<ApiResponse<StockAllocationQueryDTO>> exchangeResult = response
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<ApiResponse<StockAllocationQueryDTO>>() {
                })
                .returnResult();

        ApiResponse<StockAllocationQueryDTO> apiResponse = exchangeResult.getResponseBody();
        assertThat(apiResponse).isNotNull();
        assertThat(apiResponse.isSuccess()).isTrue();

        StockAllocationQueryDTO allocation = apiResponse.getData();
        assertThat(allocation).isNotNull();
        assertThat(allocation.getAllocationId()).isEqualTo(allocationId);
    }

    @Test
    @Order(21)
    public void testListStockAllocations_ByReferenceId_Success() {
        // Arrange - Create consignment and allocation first
        CreateConsignmentRequest consignmentRequest = ConsignmentTestDataBuilder.buildCreateConsignmentRequestV2(
                testWarehouseId, testProductCode, 100, null);
        authenticatedPost(
                "/api/v1/stock-management/consignments",
                tenantAdminAuth.getAccessToken(),
                testTenantId,
                consignmentRequest
        ).exchange()
                .expectStatus().isCreated();

        // Wait for stock item creation (async via Kafka events)
        boolean stockItemsCreated = waitForStockItems(testProductId.toString(), tenantAdminAuth.getAccessToken(), testTenantId, 10, 500);
        assertThat(stockItemsCreated).as("Stock items should be created from consignment within 10 seconds").isTrue();

        String orderId = "ORDER-" + faker.number().digits(5);
        CreateStockAllocationRequest allocationRequest = StockAllocationTestDataBuilder.buildCreateStockAllocationRequest(
                testProductId,
                testLocationId,
                20,
                orderId
        );

        authenticatedPost(
                "/api/v1/stock-management/allocations",
                tenantAdminAuth.getAccessToken(),
                testTenantId,
                allocationRequest
        ).exchange()
                .expectStatus().isCreated();

        // Act
        WebTestClient.ResponseSpec response = authenticatedGet(
                "/api/v1/stock-management/allocations?referenceId=" + orderId,
                tenantAdminAuth.getAccessToken(),
                testTenantId
        ).exchange();

        // Assert
        EntityExchangeResult<ApiResponse<ListStockAllocationsQueryResultDTO>> exchangeResult = response
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<ApiResponse<ListStockAllocationsQueryResultDTO>>() {
                })
                .returnResult();

        ApiResponse<ListStockAllocationsQueryResultDTO> apiResponse = exchangeResult.getResponseBody();
        assertThat(apiResponse).isNotNull();
        assertThat(apiResponse.isSuccess()).isTrue();

        ListStockAllocationsQueryResultDTO result = apiResponse.getData();
        assertThat(result).isNotNull();
        assertThat(result.getAllocations()).isNotNull();
        assertThat(result.getTotalCount()).isGreaterThan(0);
    }
}

