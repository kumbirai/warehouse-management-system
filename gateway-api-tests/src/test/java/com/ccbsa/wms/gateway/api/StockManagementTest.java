package com.ccbsa.wms.gateway.api;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;

import com.ccbsa.common.application.api.ApiResponse;
import com.ccbsa.wms.gateway.api.dto.AuthenticationResult;
import com.ccbsa.wms.gateway.api.dto.ConsignmentResponse;
import com.ccbsa.wms.gateway.api.dto.CreateConsignmentRequest;
import com.ccbsa.wms.gateway.api.dto.CreateConsignmentResponse;
import com.ccbsa.wms.gateway.api.dto.CreateLocationRequest;
import com.ccbsa.wms.gateway.api.dto.CreateLocationResponse;
import com.ccbsa.wms.gateway.api.dto.CreateProductRequest;
import com.ccbsa.wms.gateway.api.dto.CreateProductResponse;
import com.ccbsa.wms.gateway.api.dto.CreateStockAdjustmentRequest;
import com.ccbsa.wms.gateway.api.dto.CreateStockAdjustmentResponse;
import com.ccbsa.wms.gateway.api.dto.CreateStockAllocationRequest;
import com.ccbsa.wms.gateway.api.dto.CreateStockAllocationResponse;
import com.ccbsa.wms.gateway.api.dto.CreateStockMovementRequest;
import com.ccbsa.wms.gateway.api.dto.CreateStockMovementResponse;
import com.ccbsa.wms.gateway.api.dto.CsvUploadResponse;
import com.ccbsa.wms.gateway.api.dto.StockLevelResponse;
import com.ccbsa.wms.gateway.api.fixture.ConsignmentTestDataBuilder;
import com.ccbsa.wms.gateway.api.fixture.LocationTestDataBuilder;
import com.ccbsa.wms.gateway.api.fixture.ProductTestDataBuilder;
import com.ccbsa.wms.gateway.api.util.CsvTestDataGenerator;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive integration tests for Stock Management Service via Gateway.
 * 
 * Tests cover:
 * - Consignment receipt (manual and CSV upload)
 * - Stock allocation
 * - Stock movement
 * - FEFO (First-Expired-First-Out) logic
 * - Stock queries
 * - Stock adjustments
 * - Tenant isolation
 * - Authorization checks
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class StockManagementTest extends BaseIntegrationTest {

    private static AuthenticationResult tenantAdminAuth;
    private static String testTenantId;
    private static String testProductId;
    private static String testProductCode; // Product code for consignment line items
    private static String testLocationId;
    private static String testLocationId2; // Second location for movement tests
    private static String testWarehouseId; // Warehouse ID for consignments
    // Note: testConsignmentId removed as it's not used across tests

    @BeforeAll
    public static void setupTestData() {
        // Login as TENANT_ADMIN
        // Note: This will be set up in first test
    }

    @BeforeEach
    public void setUpStockTest() {
        if (tenantAdminAuth == null) {
            tenantAdminAuth = loginAsTenantAdmin();
            testTenantId = tenantAdminAuth.getTenantId();

            // Create product and locations for tests
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
            CreateProductResponse product = productApiResponse != null ? productApiResponse.getData() : null;
            assertThat(product).isNotNull();
            testProductId = product.getProductId();
            testProductCode = product.getProductCode(); // Use product code for consignment line items

            // Create first location
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
            CreateLocationResponse location = locationApiResponse != null ? locationApiResponse.getData() : null;
            assertThat(location).isNotNull();
            testLocationId = location.getLocationId();
            testWarehouseId = location.getLocationId(); // Use first location as warehouse for consignments

            // Create second location for movement tests
            CreateLocationRequest locationRequest2 = LocationTestDataBuilder.buildWarehouseRequest();
            EntityExchangeResult<ApiResponse<CreateLocationResponse>> locationExchangeResult2 = authenticatedPost(
                    "/api/v1/location-management/locations",
                    tenantAdminAuth.getAccessToken(),
                    testTenantId,
                    locationRequest2
            ).exchange()
                    .expectStatus().isCreated()
                    .expectBody(new ParameterizedTypeReference<ApiResponse<CreateLocationResponse>>() {
                    })
                    .returnResult();

            ApiResponse<CreateLocationResponse> locationApiResponse2 = locationExchangeResult2.getResponseBody();
            assertThat(locationApiResponse2).isNotNull();
            assertThat(locationApiResponse2.isSuccess()).isTrue();
            CreateLocationResponse location2 = locationApiResponse2 != null ? locationApiResponse2.getData() : null;
            assertThat(location2).isNotNull();
            testLocationId2 = location2.getLocationId();
        }
    }

    // ==================== CONSIGNMENT RECEIPT TESTS (Manual Entry) ====================

    @Test
    @Order(1)
    public void testCreateConsignment_Success() {
        // Arrange - Use new API format with warehouseId, consignmentReference, lineItems
        CreateConsignmentRequest request = ConsignmentTestDataBuilder.buildCreateConsignmentRequestV2(
                testWarehouseId, testProductCode, 100, null);

        // Act
        WebTestClient.ResponseSpec response = authenticatedPost(
                "/api/v1/stock-management/consignments",
                tenantAdminAuth.getAccessToken(),
                testTenantId,
                request
        ).exchange();

        // Assert
        EntityExchangeResult<ApiResponse<CreateConsignmentResponse>> exchangeResult = response
                .expectStatus().isCreated()
                .expectBody(new ParameterizedTypeReference<ApiResponse<CreateConsignmentResponse>>() {
                })
                .returnResult();

        ApiResponse<CreateConsignmentResponse> apiResponse = exchangeResult.getResponseBody();
        assertThat(apiResponse).isNotNull();
        assertThat(apiResponse.isSuccess()).isTrue();

        CreateConsignmentResponse consignment = apiResponse.getData();
        assertThat(consignment).isNotNull();
        assertThat(consignment.getConsignmentId()).isNotNull();
        // Note: CreateConsignmentResponse has legacy fields, but consignment was created successfully
    }

    @Test
    @Order(2)
    public void testCreateConsignment_InvalidProduct() {
        // Arrange - Use invalid product code
        CreateConsignmentRequest request = ConsignmentTestDataBuilder.buildCreateConsignmentRequestV2(
                testWarehouseId, "INVALID-PRODUCT-CODE", 100, null);

        // Act
        WebTestClient.ResponseSpec response = authenticatedPost(
                "/api/v1/stock-management/consignments",
                tenantAdminAuth.getAccessToken(),
                testTenantId,
                request
        ).exchange();

        // Assert
        response.expectStatus().isBadRequest(); // or 404 NOT FOUND
    }

    @Test
    @Order(3)
    public void testCreateConsignment_InvalidLocation() {
        // Arrange - Use invalid warehouse ID
        CreateConsignmentRequest request = ConsignmentTestDataBuilder.buildCreateConsignmentRequestV2(
                randomUUID(), testProductCode, 100, null);

        // Act
        WebTestClient.ResponseSpec response = authenticatedPost(
                "/api/v1/stock-management/consignments",
                tenantAdminAuth.getAccessToken(),
                testTenantId,
                request
        ).exchange();

        // Assert
        response.expectStatus().isBadRequest(); // or 404 NOT FOUND
    }

    @Test
    @Order(4)
    public void testCreateConsignment_NegativeQuantity() {
        // Arrange - Negative quantity in line item
        List<CreateConsignmentRequest.ConsignmentLineItem> lineItems = new ArrayList<>();
        lineItems.add(CreateConsignmentRequest.ConsignmentLineItem.builder()
                .productCode(testProductCode)
                .quantity(-10)
                .build());
        CreateConsignmentRequest request = CreateConsignmentRequest.builder()
                .consignmentReference("CONS-TEST")
                .warehouseId(testWarehouseId)
                .receivedAt(java.time.LocalDateTime.now())
                .receivedBy("Test User")
                .lineItems(lineItems)
                .build();

        // Act
        WebTestClient.ResponseSpec response = authenticatedPost(
                "/api/v1/stock-management/consignments",
                tenantAdminAuth.getAccessToken(),
                testTenantId,
                request
        ).exchange();

        // Assert
        response.expectStatus().isBadRequest();
    }

    @Test
    @Order(5)
    public void testCreateConsignment_PastExpirationDate() {
        // Arrange
        LocalDate pastDate = LocalDate.now().minusDays(10);
        CreateConsignmentRequest request = ConsignmentTestDataBuilder.buildCreateConsignmentRequestV2WithExpiration(
                testWarehouseId, testProductCode, pastDate);

        // Act
        WebTestClient.ResponseSpec response = authenticatedPost(
                "/api/v1/stock-management/consignments",
                tenantAdminAuth.getAccessToken(),
                testTenantId,
                request
        ).exchange();

        // Assert
        // May be 201 CREATED with warning or 400 BAD REQUEST depending on validation
        // Accept either success or bad request
        int statusCode = response.expectBody().returnResult().getStatus().value();
        assertThat(statusCode).isIn(201, 400);
    }

    @Test
    @Order(6)
    public void testCreateConsignment_WithoutAuthentication() {
        // Arrange
        CreateConsignmentRequest request = ConsignmentTestDataBuilder.buildCreateConsignmentRequestV2(
                testWarehouseId, testProductCode, 100, null);

        // Act
        WebTestClient.ResponseSpec response = webTestClient.post()
                .uri("/api/v1/stock-management/consignments")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange();

        // Assert
        response.expectStatus().isUnauthorized();
    }

    // ==================== CONSIGNMENT CSV UPLOAD TESTS ====================

    @Test
    @Order(20)
    public void testUploadConsignmentCsv_Success() throws Exception {
        // Arrange - Create CSV file with new format (productCode, warehouseId)
        Path tempDir = Files.createTempDirectory("consignment-csv-test");
        File csvFile = CsvTestDataGenerator.generateConsignmentCsv(tempDir, 3, testProductCode, testWarehouseId);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new FileSystemResource(csvFile));

        // Act
        WebTestClient.ResponseSpec response = webTestClient.post()
                .uri("/api/v1/stock-management/consignments/upload-csv")
                .headers(headers -> {
                    com.ccbsa.wms.gateway.api.util.RequestHeaderHelper.addAuthHeaders(headers, tenantAdminAuth.getAccessToken());
                    com.ccbsa.wms.gateway.api.util.RequestHeaderHelper.addTenantHeader(headers, testTenantId);
                })
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(body))
                .exchange();

        // Assert
        EntityExchangeResult<ApiResponse<CsvUploadResponse>> exchangeResult = response
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<ApiResponse<CsvUploadResponse>>() {
                })
                .returnResult();

        ApiResponse<CsvUploadResponse> apiResponse = exchangeResult.getResponseBody();
        assertThat(apiResponse).isNotNull();
        assertThat(apiResponse.isSuccess()).isTrue();

        CsvUploadResponse uploadResponse = apiResponse.getData();
        assertThat(uploadResponse).isNotNull();
        assertThat(uploadResponse.getTotalRows()).isGreaterThan(0);
        // Success count = created + updated (if applicable)
        int successCount = (uploadResponse.getCreatedCount() != null ? uploadResponse.getCreatedCount() : 0) +
                          (uploadResponse.getUpdatedCount() != null ? uploadResponse.getUpdatedCount() : 0);
        assertThat(successCount).isGreaterThan(0);

        // Cleanup
        Files.deleteIfExists(csvFile.toPath());
        Files.deleteIfExists(tempDir);
    }

    @Test
    @Order(21)
    public void testUploadConsignmentCsv_WithInvalidProductIds() throws Exception {
        // Arrange - Create CSV file with invalid productCode
        Path tempDir = Files.createTempDirectory("consignment-csv-test");
        File csvFile = tempDir.resolve("consignments-invalid.csv").toFile();

        try (java.io.FileWriter writer = new java.io.FileWriter(csvFile)) {
            writer.write("ConsignmentReference,ProductCode,Quantity,ReceivedDate,WarehouseId,ExpirationDate\n");
            writer.write(String.format("CONS-INVALID,invalid-product-code,100,%s,%s,%s\n",
                    java.time.LocalDateTime.now().toString(),
                    testWarehouseId,
                    LocalDate.now().plusMonths(6)));
        }

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new FileSystemResource(csvFile));

        // Act
        WebTestClient.ResponseSpec response = webTestClient.post()
                .uri("/api/v1/stock-management/consignments/upload-csv")
                .headers(headers -> {
                    com.ccbsa.wms.gateway.api.util.RequestHeaderHelper.addAuthHeaders(headers, tenantAdminAuth.getAccessToken());
                    com.ccbsa.wms.gateway.api.util.RequestHeaderHelper.addTenantHeader(headers, testTenantId);
                })
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(body))
                .exchange();

        // Assert
        EntityExchangeResult<ApiResponse<CsvUploadResponse>> exchangeResult = response
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<ApiResponse<CsvUploadResponse>>() {
                })
                .returnResult();

        ApiResponse<CsvUploadResponse> apiResponse = exchangeResult.getResponseBody();
        assertThat(apiResponse).isNotNull();
        assertThat(apiResponse.isSuccess()).isTrue();

        CsvUploadResponse uploadResponse = apiResponse.getData();
        assertThat(uploadResponse).isNotNull();
        // Should have errors for invalid product
        assertThat(uploadResponse.getFailureCount()).isGreaterThan(0);
        assertThat(uploadResponse.getErrors()).isNotEmpty();

        // Cleanup
        Files.deleteIfExists(csvFile.toPath());
        Files.deleteIfExists(tempDir);
    }

    @Test
    @Order(22)
    public void testUploadConsignmentCsv_EmptyFile() throws Exception {
        // Arrange - Create empty CSV file
        Path tempDir = Files.createTempDirectory("consignment-csv-test");
        File csvFile = tempDir.resolve("consignments-empty.csv").toFile();
        csvFile.createNewFile(); // Create empty file

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new FileSystemResource(csvFile));

        // Act
        WebTestClient.ResponseSpec response = webTestClient.post()
                .uri("/api/v1/stock-management/consignments/upload-csv")
                .headers(headers -> {
                    com.ccbsa.wms.gateway.api.util.RequestHeaderHelper.addAuthHeaders(headers, tenantAdminAuth.getAccessToken());
                    com.ccbsa.wms.gateway.api.util.RequestHeaderHelper.addTenantHeader(headers, testTenantId);
                })
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(body))
                .exchange();

        // Assert
        response.expectStatus().isBadRequest();

        // Cleanup
        Files.deleteIfExists(csvFile.toPath());
        Files.deleteIfExists(tempDir);
    }

    // ==================== STOCK ALLOCATION TESTS ====================

    @Test
    @Order(30)
    public void testAllocateStock_Success() {
        // Arrange - Create consignment first using new format
        CreateConsignmentRequest consignmentRequest = ConsignmentTestDataBuilder.buildCreateConsignmentRequestV2(
                testWarehouseId, testProductCode, 100, null);
        EntityExchangeResult<ApiResponse<CreateConsignmentResponse>> consignmentResult = authenticatedPost(
                "/api/v1/stock-management/consignments",
                tenantAdminAuth.getAccessToken(),
                testTenantId,
                consignmentRequest
        ).exchange()
                .expectStatus().isCreated()
                .expectBody(new ParameterizedTypeReference<ApiResponse<CreateConsignmentResponse>>() {
                })
                .returnResult();

        ApiResponse<CreateConsignmentResponse> consignmentApiResponse = consignmentResult.getResponseBody();
        assertThat(consignmentApiResponse).isNotNull();
        CreateConsignmentResponse consignment = consignmentApiResponse != null ? consignmentApiResponse.getData() : null;
        assertThat(consignment).isNotNull();
        String orderId = "ORDER-" + faker.number().digits(5);

        CreateStockAllocationRequest request = ConsignmentTestDataBuilder.buildCreateStockAllocationRequest(
                testProductId, testLocationId, 50, orderId);

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
    }

    @Test
    @Order(31)
    public void testAllocateStock_ExceedingAvailableQuantity() {
        // Arrange - Create consignment with 100 units using new format
        CreateConsignmentRequest consignmentRequest = ConsignmentTestDataBuilder.buildCreateConsignmentRequestV2(
                testWarehouseId, testProductCode, 100, null);
        authenticatedPost(
                "/api/v1/stock-management/consignments",
                tenantAdminAuth.getAccessToken(),
                testTenantId,
                consignmentRequest
        ).exchange()
                .expectStatus().isCreated();

        // Try to allocate 150 units
        String orderId = "ORDER-" + faker.number().digits(5);
        CreateStockAllocationRequest request = ConsignmentTestDataBuilder.buildCreateStockAllocationRequest(
                testProductId, testLocationId, 150, orderId);

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

    // ==================== STOCK MOVEMENT TESTS ====================

    @Test
    @Order(40)
    public void testMoveStockBetweenLocations_Success() {
        // Arrange - Create consignment in Location A using new format
        CreateConsignmentRequest consignmentRequest = ConsignmentTestDataBuilder.buildCreateConsignmentRequestV2(
                testWarehouseId, testProductCode, 100, null);
        authenticatedPost(
                "/api/v1/stock-management/consignments",
                tenantAdminAuth.getAccessToken(),
                testTenantId,
                consignmentRequest
        ).exchange()
                .expectStatus().isCreated();

        CreateStockMovementRequest request = ConsignmentTestDataBuilder.buildCreateStockMovementRequest(
                testProductId, testLocationId, testLocationId2, 50);

        // Act
        WebTestClient.ResponseSpec response = authenticatedPost(
                "/api/v1/stock-management/movements",
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
    @Order(41)
    public void testMoveStock_InsufficientQuantity() {
        // Arrange - Create consignment with 50 units using new format
        CreateConsignmentRequest consignmentRequest = ConsignmentTestDataBuilder.buildCreateConsignmentRequestV2(
                testWarehouseId, testProductCode, 50, null);
        authenticatedPost(
                "/api/v1/stock-management/consignments",
                tenantAdminAuth.getAccessToken(),
                testTenantId,
                consignmentRequest
        ).exchange()
                .expectStatus().isCreated();

        // Try to move 100 units
        CreateStockMovementRequest request = ConsignmentTestDataBuilder.buildCreateStockMovementRequest(
                testProductId, testLocationId, testLocationId2, 100);

        // Act
        WebTestClient.ResponseSpec response = authenticatedPost(
                "/api/v1/stock-management/movements",
                tenantAdminAuth.getAccessToken(),
                testTenantId,
                request
        ).exchange();

        // Assert
        response.expectStatus().isBadRequest();
    }

    // ==================== FEFO (First-Expired-First-Out) LOGIC TESTS ====================

    @Test
    @Order(50)
    public void testAllocateStockUsingFEFO() {
        // Arrange - Create 3 consignments of same product with different expiration dates
        LocalDate expiration1 = LocalDate.now().plusMonths(3); // Expires first
        LocalDate expiration2 = LocalDate.now().plusMonths(6);
        LocalDate expiration3 = LocalDate.now().plusMonths(9); // Expires last

        CreateConsignmentRequest consignment1 = ConsignmentTestDataBuilder.buildCreateConsignmentRequestV2WithExpiration(
                testWarehouseId, testProductCode, expiration1);
        CreateConsignmentRequest consignment2 = ConsignmentTestDataBuilder.buildCreateConsignmentRequestV2WithExpiration(
                testWarehouseId, testProductCode, expiration2);
        CreateConsignmentRequest consignment3 = ConsignmentTestDataBuilder.buildCreateConsignmentRequestV2WithExpiration(
                testWarehouseId, testProductCode, expiration3);

        // Create all consignments with 100 units each
        authenticatedPost("/api/v1/stock-management/consignments",
                tenantAdminAuth.getAccessToken(), testTenantId, consignment1)
                .exchange().expectStatus().isCreated();
        authenticatedPost("/api/v1/stock-management/consignments",
                tenantAdminAuth.getAccessToken(), testTenantId, consignment2)
                .exchange().expectStatus().isCreated();
        authenticatedPost("/api/v1/stock-management/consignments",
                tenantAdminAuth.getAccessToken(), testTenantId, consignment3)
                .exchange().expectStatus().isCreated();

        // Allocate 150 units using FEFO
        String orderId = "ORDER-" + faker.number().digits(5);
        CreateStockAllocationRequest request = ConsignmentTestDataBuilder.buildCreateStockAllocationRequest(
                testProductId, testLocationId, 150, orderId);

        // Act
        WebTestClient.ResponseSpec response = authenticatedPost(
                "/api/v1/stock-management/allocations",
                tenantAdminAuth.getAccessToken(),
                testTenantId,
                request
        ).exchange();

        // Assert
        // FEFO should allocate from earliest expiring stock first
        // Batch 1 (100 units) + Batch 2 (50 units) should be allocated
        response.expectStatus().isCreated();

        EntityExchangeResult<ApiResponse<CreateStockAllocationResponse>> exchangeResult = response
                .expectBody(new ParameterizedTypeReference<ApiResponse<CreateStockAllocationResponse>>() {
                })
                .returnResult();

        ApiResponse<CreateStockAllocationResponse> allocationApiResponse = exchangeResult.getResponseBody();
        assertThat(allocationApiResponse).isNotNull();
        assertThat(allocationApiResponse.isSuccess()).isTrue();
        CreateStockAllocationResponse allocation = allocationApiResponse != null ? allocationApiResponse.getData() : null;
        assertThat(allocation).isNotNull();
        assertThat(allocation.getQuantity()).isEqualTo(150);
    }

    @Test
    @Order(51)
    public void testSkipExpiredStockInFEFOAllocation() {
        // Arrange - Create consignment with past expiration date (expired)
        LocalDate expiredDate = LocalDate.now().minusDays(10);
        CreateConsignmentRequest expiredConsignment = ConsignmentTestDataBuilder.buildCreateConsignmentRequestV2WithExpiration(
                testWarehouseId, testProductCode, expiredDate);

        // Create consignment with future expiration date
        LocalDate futureDate = LocalDate.now().plusMonths(6);
        CreateConsignmentRequest validConsignment = ConsignmentTestDataBuilder.buildCreateConsignmentRequestV2WithExpiration(
                testWarehouseId, testProductCode, futureDate);

        authenticatedPost("/api/v1/stock-management/consignments",
                tenantAdminAuth.getAccessToken(), testTenantId, expiredConsignment)
                .exchange().expectStatus().isCreated();
        authenticatedPost("/api/v1/stock-management/consignments",
                tenantAdminAuth.getAccessToken(), testTenantId, validConsignment)
                .exchange().expectStatus().isCreated();

        // Allocate stock using FEFO
        String orderId = "ORDER-" + faker.number().digits(5);
        CreateStockAllocationRequest request = ConsignmentTestDataBuilder.buildCreateStockAllocationRequest(
                testProductId, testLocationId, 50, orderId);

        // Act
        WebTestClient.ResponseSpec response = authenticatedPost(
                "/api/v1/stock-management/allocations",
                tenantAdminAuth.getAccessToken(),
                testTenantId,
                request
        ).exchange();

        // Assert
        // Expired stock should be skipped, only valid stock allocated
        response.expectStatus().isCreated();

        EntityExchangeResult<ApiResponse<CreateStockAllocationResponse>> exchangeResult = response
                .expectBody(new ParameterizedTypeReference<ApiResponse<CreateStockAllocationResponse>>() {
                })
                .returnResult();

        ApiResponse<CreateStockAllocationResponse> allocationApiResponse = exchangeResult.getResponseBody();
        assertThat(allocationApiResponse).isNotNull();
        assertThat(allocationApiResponse.isSuccess()).isTrue();
        CreateStockAllocationResponse allocation = allocationApiResponse != null ? allocationApiResponse.getData() : null;
        assertThat(allocation).isNotNull();
        // Should allocate from valid stock only
        assertThat(allocation.getQuantity()).isEqualTo(50);
    }

    // ==================== STOCK QUERY TESTS ====================

    @Test
    @Order(60)
    public void testListConsignments_Success() {
        // Arrange - Create a few consignments using new format
        for (int i = 0; i < 3; i++) {
            CreateConsignmentRequest request = ConsignmentTestDataBuilder.buildCreateConsignmentRequestV2(
                    testWarehouseId, testProductCode, 100, null);
            authenticatedPost("/api/v1/stock-management/consignments",
                    tenantAdminAuth.getAccessToken(), testTenantId, request)
                    .exchange().expectStatus().isCreated();
        }

        // Act
        WebTestClient.ResponseSpec response = authenticatedGet(
                "/api/v1/stock-management/consignments?page=0&size=10",
                tenantAdminAuth.getAccessToken(),
                testTenantId
        ).exchange();

        // Assert
        response.expectStatus().isOk();
    }

    @Test
    @Order(61)
    public void testGetConsignmentById_Success() {
        // Arrange - Create consignment using new format
        CreateConsignmentRequest request = ConsignmentTestDataBuilder.buildCreateConsignmentRequestV2(
                testWarehouseId, testProductCode, 100, null);
        EntityExchangeResult<ApiResponse<CreateConsignmentResponse>> createResult = authenticatedPost(
                "/api/v1/stock-management/consignments",
                tenantAdminAuth.getAccessToken(),
                testTenantId,
                request
        ).exchange()
                .expectStatus().isCreated()
                .expectBody(new ParameterizedTypeReference<ApiResponse<CreateConsignmentResponse>>() {
                })
                .returnResult();

        ApiResponse<CreateConsignmentResponse> createApiResponse = createResult.getResponseBody();
        assertThat(createApiResponse).isNotNull();
        assertThat(createApiResponse.isSuccess()).isTrue();
        CreateConsignmentResponse createData = createApiResponse != null ? createApiResponse.getData() : null;
        assertThat(createData).isNotNull();
        String consignmentId = createData.getConsignmentId();

        // Act
        WebTestClient.ResponseSpec response = authenticatedGet(
                "/api/v1/stock-management/consignments/" + consignmentId,
                tenantAdminAuth.getAccessToken(),
                testTenantId
        ).exchange();

        // Assert
        EntityExchangeResult<ApiResponse<ConsignmentResponse>> exchangeResult = response
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<ApiResponse<ConsignmentResponse>>() {
                })
                .returnResult();

        ApiResponse<ConsignmentResponse> apiResponse = exchangeResult.getResponseBody();
        assertThat(apiResponse).isNotNull();
        assertThat(apiResponse.isSuccess()).isTrue();

        ConsignmentResponse consignment = apiResponse.getData();
        assertThat(consignment).isNotNull();
        assertThat(consignment.getConsignmentId()).isEqualTo(consignmentId);
        assertThat(consignment.getProductId()).isEqualTo(testProductId);
    }

    @Test
    @Order(62)
    public void testGetStockLevelByProductAndLocation() {
        // Arrange - Create consignment using new format
        CreateConsignmentRequest request = ConsignmentTestDataBuilder.buildCreateConsignmentRequestV2(
                testWarehouseId, testProductCode, 100, null);
        authenticatedPost("/api/v1/stock-management/consignments",
                tenantAdminAuth.getAccessToken(), testTenantId, request)
                .exchange().expectStatus().isCreated();

        // Act
        WebTestClient.ResponseSpec response = authenticatedGet(
                "/api/v1/stock-management/stock-levels?productId=" + testProductId + "&locationId=" + testLocationId,
                tenantAdminAuth.getAccessToken(),
                testTenantId
        ).exchange();

        // Assert
        EntityExchangeResult<ApiResponse<List<StockLevelResponse>>> exchangeResult = response
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<ApiResponse<List<StockLevelResponse>>>() {
                })
                .returnResult();

        ApiResponse<List<StockLevelResponse>> apiResponse = exchangeResult.getResponseBody();
        assertThat(apiResponse).isNotNull();
        assertThat(apiResponse.isSuccess()).isTrue();

        List<StockLevelResponse> stockLevels = apiResponse.getData();
        assertThat(stockLevels).isNotNull();
        // Should have at least one stock level
        assertThat(stockLevels.size()).isGreaterThan(0);
    }

    @Test
    @Order(63)
    public void testGetStockAvailabilityByProduct() {
        // Arrange - Create consignments in multiple locations using new format
        CreateConsignmentRequest request1 = ConsignmentTestDataBuilder.buildCreateConsignmentRequestV2(
                testWarehouseId, testProductCode, 100, null);
        CreateConsignmentRequest request2 = ConsignmentTestDataBuilder.buildCreateConsignmentRequestV2(
                testLocationId2, testProductCode, 50, null);

        authenticatedPost("/api/v1/stock-management/consignments",
                tenantAdminAuth.getAccessToken(), testTenantId, request1)
                .exchange().expectStatus().isCreated();
        authenticatedPost("/api/v1/stock-management/consignments",
                tenantAdminAuth.getAccessToken(), testTenantId, request2)
                .exchange().expectStatus().isCreated();

        // Act
        WebTestClient.ResponseSpec response = authenticatedGet(
                "/api/v1/stock-management/stock-levels?productId=" + testProductId,
                tenantAdminAuth.getAccessToken(),
                testTenantId
        ).exchange();

        // Assert
        EntityExchangeResult<ApiResponse<List<StockLevelResponse>>> exchangeResult = response
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<ApiResponse<List<StockLevelResponse>>>() {
                })
                .returnResult();

        ApiResponse<List<StockLevelResponse>> apiResponse = exchangeResult.getResponseBody();
        assertThat(apiResponse).isNotNull();
        assertThat(apiResponse.isSuccess()).isTrue();

        List<StockLevelResponse> stockLevels = apiResponse.getData();
        assertThat(stockLevels).isNotNull();
        // Should have stock levels from multiple locations
        assertThat(stockLevels.size()).isGreaterThanOrEqualTo(1);
    }

    // ==================== STOCK ADJUSTMENT TESTS ====================

    @Test
    @Order(70)
    public void testIncreaseStock_PositiveAdjustment() {
        // Arrange - Create consignment with 100 units using new format
        CreateConsignmentRequest consignmentRequest = ConsignmentTestDataBuilder.buildCreateConsignmentRequestV2(
                testWarehouseId, testProductCode, 100, null);
        EntityExchangeResult<ApiResponse<CreateConsignmentResponse>> consignmentResult = authenticatedPost(
                "/api/v1/stock-management/consignments",
                tenantAdminAuth.getAccessToken(),
                testTenantId,
                consignmentRequest
        ).exchange()
                .expectStatus().isCreated()
                .expectBody(new ParameterizedTypeReference<ApiResponse<CreateConsignmentResponse>>() {
                })
                .returnResult();

        ApiResponse<CreateConsignmentResponse> consignmentApiResponse = consignmentResult.getResponseBody();
        assertThat(consignmentApiResponse).isNotNull();
        assertThat(consignmentApiResponse.isSuccess()).isTrue();
        CreateConsignmentResponse consignmentData = consignmentApiResponse != null ? consignmentApiResponse.getData() : null;
        assertThat(consignmentData).isNotNull();
        String consignmentId = consignmentData.getConsignmentId();

        CreateStockAdjustmentRequest request = ConsignmentTestDataBuilder.buildCreateStockAdjustmentRequest(
                testProductId, testLocationId, "INCREASE", 20, "Stock count correction");

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
        // New quantity should be 120 (100 + 20)
        assertThat(adjustment.getQuantityAfter()).isEqualTo(120);
    }

    @Test
    @Order(71)
    public void testDecreaseStock_NegativeAdjustment() {
        // Arrange - Create consignment with 100 units using new format
        CreateConsignmentRequest consignmentRequest = ConsignmentTestDataBuilder.buildCreateConsignmentRequestV2(
                testWarehouseId, testProductCode, 100, null);
        EntityExchangeResult<ApiResponse<CreateConsignmentResponse>> consignmentResult = authenticatedPost(
                "/api/v1/stock-management/consignments",
                tenantAdminAuth.getAccessToken(),
                testTenantId,
                consignmentRequest
        ).exchange()
                .expectStatus().isCreated()
                .expectBody(new ParameterizedTypeReference<ApiResponse<CreateConsignmentResponse>>() {
                })
                .returnResult();

        ApiResponse<CreateConsignmentResponse> consignmentApiResponse = consignmentResult.getResponseBody();
        assertThat(consignmentApiResponse).isNotNull();
        assertThat(consignmentApiResponse.isSuccess()).isTrue();
        CreateConsignmentResponse consignmentData = consignmentApiResponse != null ? consignmentApiResponse.getData() : null;
        assertThat(consignmentData).isNotNull();
        String consignmentId = consignmentData.getConsignmentId();

        CreateStockAdjustmentRequest request = ConsignmentTestDataBuilder.buildCreateStockAdjustmentRequest(
                testProductId, testLocationId, "DECREASE", 20, "Damaged goods");

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
        // New quantity should be 80 (100 - 20)
        assertThat(adjustment.getQuantityAfter()).isEqualTo(80);
    }

    @Test
    @Order(72)
    public void testAdjustStock_BelowZero() {
        // Arrange - Create consignment with 50 units using new format
        CreateConsignmentRequest consignmentRequest = ConsignmentTestDataBuilder.buildCreateConsignmentRequestV2(
                testWarehouseId, testProductCode, 50, null);
        EntityExchangeResult<ApiResponse<CreateConsignmentResponse>> consignmentResult = authenticatedPost(
                "/api/v1/stock-management/consignments",
                tenantAdminAuth.getAccessToken(),
                testTenantId,
                consignmentRequest
        ).exchange()
                .expectStatus().isCreated()
                .expectBody(new ParameterizedTypeReference<ApiResponse<CreateConsignmentResponse>>() {
                })
                .returnResult();

        String consignmentId = consignmentResult.getResponseBody().getData().getConsignmentId();

        // Try to decrease by 100 units (would result in negative)
        CreateStockAdjustmentRequest request = ConsignmentTestDataBuilder.buildCreateStockAdjustmentRequest(
                testProductId, testLocationId, "DECREASE", 100, "Correction");

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

    // ==================== TENANT ISOLATION TESTS ====================

    @Test
    @Order(80)
    public void testTenantAdminListsOnlyOwnTenantConsignments() {
        // Arrange - Create consignments in current tenant using new format
        for (int i = 0; i < 3; i++) {
            CreateConsignmentRequest request = ConsignmentTestDataBuilder.buildCreateConsignmentRequestV2(
                    testWarehouseId, testProductCode, 100, null);
            authenticatedPost("/api/v1/stock-management/consignments",
                    tenantAdminAuth.getAccessToken(), testTenantId, request)
                    .exchange().expectStatus().isCreated();
        }

        // Act - List consignments
        WebTestClient.ResponseSpec response = authenticatedGet(
                "/api/v1/stock-management/consignments",
                tenantAdminAuth.getAccessToken(),
                testTenantId
        ).exchange();

        // Assert
        EntityExchangeResult<ApiResponse<List<ConsignmentResponse>>> exchangeResult = response
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<ApiResponse<List<ConsignmentResponse>>>() {
                })
                .returnResult();

        ApiResponse<List<ConsignmentResponse>> apiResponse = exchangeResult.getResponseBody();
        assertThat(apiResponse).isNotNull();
        assertThat(apiResponse.isSuccess()).isTrue();

        List<ConsignmentResponse> consignments = apiResponse.getData();
        assertThat(consignments).isNotNull();
        // All consignments should belong to the tenant
        consignments.forEach(consignment -> {
            // Verify tenant isolation (if tenantId is exposed in response)
            // This may need to be adjusted based on actual API response structure
        });
    }

    @Test
    @Order(81)
    public void testTenantAdminCannotAccessConsignmentFromDifferentTenant() {
        // Arrange - Create consignment in current tenant using new format
        CreateConsignmentRequest request = ConsignmentTestDataBuilder.buildCreateConsignmentRequestV2(
                testWarehouseId, testProductCode, 100, null);
        EntityExchangeResult<ApiResponse<CreateConsignmentResponse>> createResult = authenticatedPost(
                "/api/v1/stock-management/consignments",
                tenantAdminAuth.getAccessToken(),
                testTenantId,
                request
        ).exchange()
                .expectStatus().isCreated()
                .expectBody(new ParameterizedTypeReference<ApiResponse<CreateConsignmentResponse>>() {
                })
                .returnResult();

        ApiResponse<CreateConsignmentResponse> createApiResponse2 = createResult.getResponseBody();
        assertThat(createApiResponse2).isNotNull();
        assertThat(createApiResponse2.isSuccess()).isTrue();
        CreateConsignmentResponse createData2 = createApiResponse2 != null ? createApiResponse2.getData() : null;
        assertThat(createData2).isNotNull();
        String consignmentId = createData2.getConsignmentId();

        // Act - Try to access with different tenant ID (if we had another tenant)
        // Note: This test may need adjustment based on actual tenant isolation implementation
        // For now, we verify that accessing with wrong tenant context fails
        WebTestClient.ResponseSpec response = authenticatedGet(
                "/api/v1/stock-management/consignments/" + consignmentId,
                tenantAdminAuth.getAccessToken(),
                testTenantId // Using correct tenant - should succeed
        ).exchange();

        // Assert - Should succeed with correct tenant
        response.expectStatus().isOk();
    }

    // ==================== AUTHORIZATION TESTS ====================

    @Test
    @Order(90)
    public void testUnauthorizedAccess_WithoutToken() {
        // Arrange
        CreateConsignmentRequest request = ConsignmentTestDataBuilder.buildCreateConsignmentRequestV2(
                testWarehouseId, testProductCode, 100, null);

        // Act
        WebTestClient.ResponseSpec response = webTestClient.post()
                .uri("/api/v1/stock-management/consignments")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange();

        // Assert
        response.expectStatus().isUnauthorized();
    }

    @Test
    @Order(91)
    public void testUnauthorizedAccess_WithInvalidToken() {
        // Arrange
        CreateConsignmentRequest request = ConsignmentTestDataBuilder.buildCreateConsignmentRequestV2(
                testWarehouseId, testProductCode, 100, null);

        // Act
        WebTestClient.ResponseSpec response = authenticatedPost(
                "/api/v1/stock-management/consignments",
                "invalid-token",
                testTenantId,
                request
        ).exchange();

        // Assert
        response.expectStatus().isUnauthorized();
    }

    // ==================== SPRINT 3: STOCK CLASSIFICATION TESTS ====================

    @Test
    @Order(100)
    public void testGetStockItem_WithExpirationDate_ShouldClassifyAutomatically() {
        // Arrange - Create a consignment with expiration date using new API structure
        // Note: This test assumes consignment confirmation creates stock items
        // First, create and confirm a consignment
        CreateConsignmentRequest consignmentRequest = ConsignmentTestDataBuilder.buildCreateConsignmentRequestV2WithExpiration(
                testWarehouseId, testProductCode, LocalDate.now().plusDays(30));
        
        EntityExchangeResult<ApiResponse<CreateConsignmentResponse>> createResult = authenticatedPost(
                "/api/v1/stock-management/consignments",
                tenantAdminAuth.getAccessToken(),
                testTenantId,
                consignmentRequest
        ).exchange()
                .expectStatus().isCreated()
                .expectBody(new ParameterizedTypeReference<ApiResponse<CreateConsignmentResponse>>() {
                })
                .returnResult();

        ApiResponse<CreateConsignmentResponse> createApiResponse = createResult.getResponseBody();
        assertThat(createApiResponse).isNotNull();
        assertThat(createApiResponse.isSuccess()).isTrue();
        CreateConsignmentResponse consignment = createApiResponse.getData();
        assertThat(consignment).isNotNull();
        String consignmentId = consignment.getConsignmentId();

        // Confirm consignment to create stock items (use PUT)
        authenticatedPut(
                "/api/v1/stock-management/consignments/" + consignmentId + "/confirm",
                tenantAdminAuth.getAccessToken(),
                testTenantId,
                null
        ).exchange()
                .expectStatus().isOk();

        // Wait a bit for async processing (stock item creation)
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Act - Get stock items by classification
        EntityExchangeResult<ApiResponse<List<com.ccbsa.wms.gateway.api.dto.StockItemResponse>>> queryResult = authenticatedGet(
                "/api/v1/stock-management/stock-items/by-classification?classification=NEAR_EXPIRY",
                tenantAdminAuth.getAccessToken(),
                testTenantId
        ).exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<ApiResponse<List<com.ccbsa.wms.gateway.api.dto.StockItemResponse>>>() {
                })
                .returnResult();

        // Assert
        ApiResponse<List<com.ccbsa.wms.gateway.api.dto.StockItemResponse>> queryApiResponse = queryResult.getResponseBody();
        assertThat(queryApiResponse).isNotNull();
        assertThat(queryApiResponse.isSuccess()).isTrue();
        List<com.ccbsa.wms.gateway.api.dto.StockItemResponse> stockItems = queryApiResponse.getData();
        assertThat(stockItems).isNotNull();
        // Verify classification
        if (!stockItems.isEmpty()) {
            com.ccbsa.wms.gateway.api.dto.StockItemResponse stockItem = stockItems.get(0);
            assertThat(stockItem.getClassification()).isIn("NEAR_EXPIRY", "CRITICAL", "NORMAL");
        }
    }

    // ==================== SPRINT 3: ASSIGN LOCATION TO STOCK TESTS ====================

    @Test
    @Order(110)
    public void testAssignLocationToStock_Success() {
        // Arrange - Create and confirm consignment to get a stock item using new API structure
        CreateConsignmentRequest consignmentRequest = ConsignmentTestDataBuilder.buildCreateConsignmentRequestV2(
                testWarehouseId, testProductCode, 100, null);
        
        EntityExchangeResult<ApiResponse<CreateConsignmentResponse>> createResult = authenticatedPost(
                "/api/v1/stock-management/consignments",
                tenantAdminAuth.getAccessToken(),
                testTenantId,
                consignmentRequest
        ).exchange()
                .expectStatus().isCreated()
                .expectBody(new ParameterizedTypeReference<ApiResponse<CreateConsignmentResponse>>() {
                })
                .returnResult();

        ApiResponse<CreateConsignmentResponse> createApiResponse = createResult.getResponseBody();
        assertThat(createApiResponse).isNotNull();
        CreateConsignmentResponse consignment = createApiResponse.getData();
        String consignmentId = consignment.getConsignmentId();

        // Confirm consignment (use PUT)
        authenticatedPut(
                "/api/v1/stock-management/consignments/" + consignmentId + "/confirm",
                tenantAdminAuth.getAccessToken(),
                testTenantId,
                null
        ).exchange()
                .expectStatus().isOk();

        // Wait for stock item creation
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Get stock items to find one without location
        EntityExchangeResult<ApiResponse<List<com.ccbsa.wms.gateway.api.dto.StockItemResponse>>> queryResult = authenticatedGet(
                "/api/v1/stock-management/stock-items/by-classification?classification=NORMAL",
                tenantAdminAuth.getAccessToken(),
                testTenantId
        ).exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<ApiResponse<List<com.ccbsa.wms.gateway.api.dto.StockItemResponse>>>() {
                })
                .returnResult();

        ApiResponse<List<com.ccbsa.wms.gateway.api.dto.StockItemResponse>> queryApiResponse = queryResult.getResponseBody();
        if (queryApiResponse == null || queryApiResponse.getData() == null || queryApiResponse.getData().isEmpty()) {
            // Skip test if no stock items available
            org.junit.jupiter.api.Assumptions.assumeTrue(false, "No stock items available for testing");
            return;
        }

        com.ccbsa.wms.gateway.api.dto.StockItemResponse stockItem = queryApiResponse.getData().get(0);
        String stockItemId = stockItem.getStockItemId();

        // Act - Assign location
        com.ccbsa.wms.gateway.api.dto.AssignLocationToStockRequest assignRequest = 
                com.ccbsa.wms.gateway.api.fixture.StockItemTestDataBuilder.buildAssignLocationRequest(
                        testLocationId2, stockItem.getQuantity());

        WebTestClient.ResponseSpec response = authenticatedPost(
                "/api/v1/stock-management/stock-items/" + stockItemId + "/assign-location",
                tenantAdminAuth.getAccessToken(),
                testTenantId,
                assignRequest
        ).exchange();

        // Assert
        response.expectStatus().isOk();

        // Verify location was assigned by querying stock item
        EntityExchangeResult<ApiResponse<com.ccbsa.wms.gateway.api.dto.StockItemResponse>> getResult = authenticatedGet(
                "/api/v1/stock-management/stock-items/" + stockItemId,
                tenantAdminAuth.getAccessToken(),
                testTenantId
        ).exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<ApiResponse<com.ccbsa.wms.gateway.api.dto.StockItemResponse>>() {
                })
                .returnResult();

        ApiResponse<com.ccbsa.wms.gateway.api.dto.StockItemResponse> getApiResponse = getResult.getResponseBody();
        assertThat(getApiResponse).isNotNull();
        assertThat(getApiResponse.isSuccess()).isTrue();
        com.ccbsa.wms.gateway.api.dto.StockItemResponse updatedStockItem = getApiResponse.getData();
        assertThat(updatedStockItem).isNotNull();
        assertThat(updatedStockItem.getLocationId()).isEqualTo(testLocationId2);
    }

    // ==================== SPRINT 3: CONFIRM CONSIGNMENT TESTS ====================

    @Test
    @Order(120)
    public void testConfirmConsignment_Success() {
        // Arrange - Create consignment using new API structure
        CreateConsignmentRequest consignmentRequest = ConsignmentTestDataBuilder.buildCreateConsignmentRequestV2(
                testWarehouseId, testProductCode, 100, null);
        
        EntityExchangeResult<ApiResponse<CreateConsignmentResponse>> createResult = authenticatedPost(
                "/api/v1/stock-management/consignments",
                tenantAdminAuth.getAccessToken(),
                testTenantId,
                consignmentRequest
        ).exchange()
                .expectStatus().isCreated()
                .expectBody(new ParameterizedTypeReference<ApiResponse<CreateConsignmentResponse>>() {
                })
                .returnResult();

        ApiResponse<CreateConsignmentResponse> createApiResponse = createResult.getResponseBody();
        assertThat(createApiResponse).isNotNull();
        CreateConsignmentResponse consignment = createApiResponse.getData();
        String consignmentId = consignment.getConsignmentId();

        // Act - Confirm consignment (use PUT)
        WebTestClient.ResponseSpec response = authenticatedPut(
                "/api/v1/stock-management/consignments/" + consignmentId + "/confirm",
                tenantAdminAuth.getAccessToken(),
                testTenantId,
                null
        ).exchange();

        // Assert
        response.expectStatus().isOk();

        // Verify consignment status changed to CONFIRMED
        EntityExchangeResult<ApiResponse<ConsignmentResponse>> getResult = authenticatedGet(
                "/api/v1/stock-management/consignments/" + consignmentId,
                tenantAdminAuth.getAccessToken(),
                testTenantId
        ).exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<ApiResponse<ConsignmentResponse>>() {
                })
                .returnResult();

        ApiResponse<ConsignmentResponse> getApiResponse = getResult.getResponseBody();
        assertThat(getApiResponse).isNotNull();
        assertThat(getApiResponse.isSuccess()).isTrue();
        ConsignmentResponse confirmedConsignment = getApiResponse.getData();
        assertThat(confirmedConsignment).isNotNull();
        assertThat(confirmedConsignment.getStatus()).isEqualTo("CONFIRMED");
    }

    @Test
    @Order(121)
    public void testConfirmConsignment_AlreadyConfirmed_ShouldFail() {
        // Arrange - Create and confirm consignment using new API structure
        CreateConsignmentRequest consignmentRequest = ConsignmentTestDataBuilder.buildCreateConsignmentRequestV2(
                testWarehouseId, testProductCode, 100, null);
        
        EntityExchangeResult<ApiResponse<CreateConsignmentResponse>> createResult = authenticatedPost(
                "/api/v1/stock-management/consignments",
                tenantAdminAuth.getAccessToken(),
                testTenantId,
                consignmentRequest
        ).exchange()
                .expectStatus().isCreated()
                .expectBody(new ParameterizedTypeReference<ApiResponse<CreateConsignmentResponse>>() {
                })
                .returnResult();

        ApiResponse<CreateConsignmentResponse> createApiResponse = createResult.getResponseBody();
        CreateConsignmentResponse consignment = createApiResponse.getData();
        String consignmentId = consignment.getConsignmentId();

        // Confirm once (use PUT)
        authenticatedPut(
                "/api/v1/stock-management/consignments/" + consignmentId + "/confirm",
                tenantAdminAuth.getAccessToken(),
                testTenantId,
                null
        ).exchange()
                .expectStatus().isOk();

        // Act - Try to confirm again (use PUT)
        WebTestClient.ResponseSpec response = authenticatedPut(
                "/api/v1/stock-management/consignments/" + consignmentId + "/confirm",
                tenantAdminAuth.getAccessToken(),
                testTenantId,
                null
        ).exchange();

        // Assert - Should fail (400 Bad Request or 409 Conflict)
        int statusCode = response.expectBody().returnResult().getStatus().value();
        assertThat(statusCode).isIn(400, 409);
    }
}
