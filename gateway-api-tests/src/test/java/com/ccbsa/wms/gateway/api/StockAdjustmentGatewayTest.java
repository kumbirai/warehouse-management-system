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
import com.ccbsa.wms.gateway.api.dto.CreateConsignmentRequest;
import com.ccbsa.wms.gateway.api.dto.CreateLocationRequest;
import com.ccbsa.wms.gateway.api.dto.CreateLocationResponse;
import com.ccbsa.wms.gateway.api.dto.CreateProductRequest;
import com.ccbsa.wms.gateway.api.dto.CreateProductResponse;
import com.ccbsa.wms.gateway.api.dto.CreateStockAdjustmentRequest;
import com.ccbsa.wms.gateway.api.dto.CreateStockAdjustmentResponse;
import com.ccbsa.wms.gateway.api.dto.ListStockAdjustmentsQueryResultDTO;
import com.ccbsa.wms.gateway.api.dto.StockAdjustmentQueryDTO;
import com.ccbsa.wms.gateway.api.fixture.ConsignmentTestDataBuilder;
import com.ccbsa.wms.gateway.api.fixture.LocationTestDataBuilder;
import com.ccbsa.wms.gateway.api.fixture.ProductTestDataBuilder;
import com.ccbsa.wms.gateway.api.fixture.StockAdjustmentTestDataBuilder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive integration tests for Stock Adjustment operations via Gateway.
 * 
 * Tests cover:
 * - Stock adjustment creation (increase, decrease, correction)
 * - Stock adjustment queries (get by ID, list with filters)
 * - Negative quantity prevention
 * - Error scenarios and validation
 * - Authorization checks
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class StockAdjustmentGatewayTest extends BaseIntegrationTest {

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
    public void setUpStockAdjustmentTest() {
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

            // Create location
            CreateLocationRequest locationRequest = LocationTestDataBuilder.buildWarehouseRequest();
            EntityExchangeResult<ApiResponse<CreateLocationResponse>> locationResult = authenticatedPost(
                    "/api/v1/location-management/locations",
                    tenantAdminAuth.getAccessToken(),
                    testTenantId,
                    locationRequest
            ).exchange()
                    .expectStatus().isCreated()
                    .expectBody(new ParameterizedTypeReference<ApiResponse<CreateLocationResponse>>() {
                    })
                    .returnResult();

            ApiResponse<CreateLocationResponse> locationApiResponse = locationResult.getResponseBody();
            assertThat(locationApiResponse).isNotNull();
            assertThat(locationApiResponse.isSuccess()).isTrue();
            CreateLocationResponse location = locationApiResponse.getData();
            assertThat(location).isNotNull();
            testLocationId = UUID.fromString(location.getLocationId());
            testWarehouseId = location.getLocationId();
        }
    }

    // ==================== STOCK ADJUSTMENT CREATION TESTS ====================

    @Test
    @Order(1)
    public void testIncreaseStock_Success() {
        // Arrange - Create consignment with 100 units
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

        CreateStockAdjustmentRequest request = StockAdjustmentTestDataBuilder.buildIncreaseStockRequest(
                testProductId,
                testLocationId,
                20,
                "STOCK_COUNT"
        );

        // Act
        WebTestClient.ResponseSpec response = authenticatedPost(
                "/api/v1/stock-management/adjustments",
                tenantAdminAuth.getAccessToken(),
                testTenantId,
                request
        ).exchange();

        // Assert
        EntityExchangeResult<ApiResponse<CreateStockAdjustmentResponse>> exchangeResult = response
                .expectStatus().isCreated()
                .expectBody(new ParameterizedTypeReference<ApiResponse<CreateStockAdjustmentResponse>>() {
                })
                .returnResult();

        ApiResponse<CreateStockAdjustmentResponse> apiResponse = exchangeResult.getResponseBody();
        assertThat(apiResponse).isNotNull();
        assertThat(apiResponse.isSuccess()).isTrue();

        CreateStockAdjustmentResponse adjustment = apiResponse.getData();
        assertThat(adjustment).isNotNull();
        assertThat(adjustment.getAdjustmentId()).isNotNull();
        assertThat(adjustment.getQuantityBefore()).isEqualTo(100);
        assertThat(adjustment.getQuantityAfter()).isEqualTo(120);
    }

    @Test
    @Order(2)
    public void testDecreaseStock_Success() {
        // Arrange - Create consignment with 100 units
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

        CreateStockAdjustmentRequest request = StockAdjustmentTestDataBuilder.buildDecreaseStockRequest(
                testProductId,
                testLocationId,
                30,
                "DAMAGE"
        );

        // Act
        WebTestClient.ResponseSpec response = authenticatedPost(
                "/api/v1/stock-management/adjustments",
                tenantAdminAuth.getAccessToken(),
                testTenantId,
                request
        ).exchange();

        // Assert
        EntityExchangeResult<ApiResponse<CreateStockAdjustmentResponse>> exchangeResult = response
                .expectStatus().isCreated()
                .expectBody(new ParameterizedTypeReference<ApiResponse<CreateStockAdjustmentResponse>>() {
                })
                .returnResult();

        ApiResponse<CreateStockAdjustmentResponse> apiResponse = exchangeResult.getResponseBody();
        assertThat(apiResponse).isNotNull();
        assertThat(apiResponse.isSuccess()).isTrue();

        CreateStockAdjustmentResponse adjustment = apiResponse.getData();
        assertThat(adjustment).isNotNull();
        assertThat(adjustment.getQuantityBefore()).isEqualTo(100);
        assertThat(adjustment.getQuantityAfter()).isEqualTo(70);
    }

    @Test
    @Order(3)
    public void testDecreaseStock_ResultingInNegative_ShouldFail() {
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

        // Wait for stock item creation
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        CreateStockAdjustmentRequest request = StockAdjustmentTestDataBuilder.buildDecreaseStockRequest(
                testProductId,
                testLocationId,
                100, // More than available (50)
                "CORRECTION"
        );

        // Act
        WebTestClient.ResponseSpec response = authenticatedPost(
                "/api/v1/stock-management/adjustments",
                tenantAdminAuth.getAccessToken(),
                testTenantId,
                request
        ).exchange();

        // Assert
        response.expectStatus().isBadRequest();
    }

    // ==================== STOCK ADJUSTMENT QUERY TESTS ====================

    @Test
    @Order(20)
    public void testGetStockAdjustmentById_Success() {
        // Arrange - Create consignment and adjustment first
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

        CreateStockAdjustmentRequest adjustmentRequest = StockAdjustmentTestDataBuilder.buildIncreaseStockRequest(
                testProductId,
                testLocationId,
                15,
                "STOCK_COUNT"
        );

        EntityExchangeResult<ApiResponse<CreateStockAdjustmentResponse>> adjustmentResult = authenticatedPost(
                "/api/v1/stock-management/adjustments",
                tenantAdminAuth.getAccessToken(),
                testTenantId,
                adjustmentRequest
        ).exchange()
                .expectStatus().isCreated()
                .expectBody(new ParameterizedTypeReference<ApiResponse<CreateStockAdjustmentResponse>>() {
                })
                .returnResult();

        ApiResponse<CreateStockAdjustmentResponse> adjustmentApiResponse = adjustmentResult.getResponseBody();
        assertThat(adjustmentApiResponse).isNotNull();
        CreateStockAdjustmentResponse createdAdjustment = adjustmentApiResponse.getData();
        assertThat(createdAdjustment).isNotNull();
        String adjustmentId = createdAdjustment.getAdjustmentId().toString();

        // Act
        WebTestClient.ResponseSpec response = authenticatedGet(
                "/api/v1/stock-management/adjustments/" + adjustmentId,
                tenantAdminAuth.getAccessToken(),
                testTenantId
        ).exchange();

        // Assert
        EntityExchangeResult<ApiResponse<StockAdjustmentQueryDTO>> exchangeResult = response
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<ApiResponse<StockAdjustmentQueryDTO>>() {
                })
                .returnResult();

        ApiResponse<StockAdjustmentQueryDTO> apiResponse = exchangeResult.getResponseBody();
        assertThat(apiResponse).isNotNull();
        assertThat(apiResponse.isSuccess()).isTrue();

        StockAdjustmentQueryDTO adjustment = apiResponse.getData();
        assertThat(adjustment).isNotNull();
        assertThat(adjustment.getAdjustmentId()).isEqualTo(adjustmentId);
    }

    @Test
    @Order(21)
    public void testListStockAdjustments_ByProductId_Success() {
        // Arrange - Create consignment and adjustment first
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

        CreateStockAdjustmentRequest adjustmentRequest = StockAdjustmentTestDataBuilder.buildIncreaseStockRequest(
                testProductId,
                testLocationId,
                10,
                "STOCK_COUNT"
        );

        authenticatedPost(
                "/api/v1/stock-management/adjustments",
                tenantAdminAuth.getAccessToken(),
                testTenantId,
                adjustmentRequest
        ).exchange()
                .expectStatus().isCreated();

        // Act
        WebTestClient.ResponseSpec response = authenticatedGet(
                "/api/v1/stock-management/adjustments?productId=" + testProductId,
                tenantAdminAuth.getAccessToken(),
                testTenantId
        ).exchange();

        // Assert
        EntityExchangeResult<ApiResponse<ListStockAdjustmentsQueryResultDTO>> exchangeResult = response
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<ApiResponse<ListStockAdjustmentsQueryResultDTO>>() {
                })
                .returnResult();

        ApiResponse<ListStockAdjustmentsQueryResultDTO> apiResponse = exchangeResult.getResponseBody();
        assertThat(apiResponse).isNotNull();
        assertThat(apiResponse.isSuccess()).isTrue();

        ListStockAdjustmentsQueryResultDTO result = apiResponse.getData();
        assertThat(result).isNotNull();
        assertThat(result.getAdjustments()).isNotNull();
        assertThat(result.getTotalCount()).isGreaterThan(0);
    }
}

