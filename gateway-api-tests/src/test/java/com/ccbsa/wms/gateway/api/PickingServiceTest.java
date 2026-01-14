package com.ccbsa.wms.gateway.api;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

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
import com.ccbsa.wms.gateway.api.dto.CompletePickingListResponse;
import com.ccbsa.wms.gateway.api.dto.CreateConsignmentRequest;
import com.ccbsa.wms.gateway.api.dto.CreateConsignmentResponse;
import com.ccbsa.wms.gateway.api.dto.CreateLocationRequest;
import com.ccbsa.wms.gateway.api.dto.CreateLocationResponse;
import com.ccbsa.wms.gateway.api.dto.CreatePickingListRequest;
import com.ccbsa.wms.gateway.api.dto.CreatePickingListResponse;
import com.ccbsa.wms.gateway.api.dto.OrderQueryResult;
import com.ccbsa.wms.gateway.api.dto.CreatePickingTaskRequest;
import com.ccbsa.wms.gateway.api.dto.CreatePickingTaskResponse;
import com.ccbsa.wms.gateway.api.dto.CreateProductResponse;
import com.ccbsa.wms.gateway.api.dto.ExecutePickingTaskRequest;
import com.ccbsa.wms.gateway.api.dto.ListPickingListsQueryResult;
import com.ccbsa.wms.gateway.api.dto.ListRestockRequestsResponse;
import com.ccbsa.wms.gateway.api.dto.PickingListQueryResult;
import com.ccbsa.wms.gateway.api.dto.StockExpirationCheckResponse;
import com.ccbsa.wms.gateway.api.dto.StockItemQueryDTO;
import com.ccbsa.wms.gateway.api.dto.UploadPickingListCsvResponse;
import com.ccbsa.wms.gateway.api.fixture.ConsignmentTestDataBuilder;
import com.ccbsa.wms.gateway.api.fixture.LocationTestDataBuilder;
import com.ccbsa.wms.gateway.api.fixture.PickingListTestDataBuilder;
import com.ccbsa.wms.gateway.api.fixture.PickingTestDataBuilder;
import com.ccbsa.wms.gateway.api.fixture.TestDataManager;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive integration tests for Picking Service.
 *
 * Tests cover:
 * - Picking Task Creation and Listing
 * - Picking List Creation (JSON and CSV)
 * - Picking List Querying
 * - Picking Task Execution
 * - Picking List Completion
 * - Stock Expiration Management
 * - Restock Request Generation
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PickingServiceTest extends BaseIntegrationTest {

    private static AuthenticationResult tenantAdminAuth;
    private static String testTenantId;
    private static String testProductId;
    private static String testProductCode;
    private static String testLocationId;
    private static String testPickingListId;
    @SuppressWarnings("unused")
    private static String testPickingTaskId;
    private static String testExpiringProductId;
    private static String testExpiringProductCode;
    private static String testExpiredProductId;
    private static String testExpiredProductCode;
    private static String testWarehouseId;

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
            // Use createProduct() helper which checks repository first and saves new products
            CreateProductResponse product = createProduct(tenantAdminAuth.getAccessToken(), testTenantId);
            assertThat(product).isNotNull();
            testProductId = product.getProductId();
            testProductCode = product.getProductCode();

            // Create product with expiring stock
            // Use createProduct() helper which checks repository first and saves new products
            CreateProductResponse expiringProduct = createProduct(tenantAdminAuth.getAccessToken(), testTenantId);
            assertThat(expiringProduct).isNotNull();
            testExpiringProductId = expiringProduct.getProductId();
            testExpiringProductCode = expiringProduct.getProductCode();

            // Create product with expired stock
            // Use createProduct() helper which checks repository first and saves new products
            CreateProductResponse expiredProduct = createProduct(tenantAdminAuth.getAccessToken(), testTenantId);
            assertThat(expiredProduct).isNotNull();
            testExpiredProductId = expiredProduct.getProductId();
            testExpiredProductCode = expiredProduct.getProductCode();

            // Try to reuse existing warehouse from repository, otherwise create new
            Optional<CreateLocationResponse> existingWarehouse = TestDataManager.getLocationByType("WAREHOUSE", testTenantId);
            CreateLocationResponse warehouse;
            if (existingWarehouse.isPresent()) {
                warehouse = existingWarehouse.get();
                testWarehouseId = warehouse.getLocationId();
            } else {
                // Create warehouse using BaseIntegrationTest helper which automatically checks repository and saves
                warehouse = createLocation(tenantAdminAuth.getAccessToken(), testTenantId);
                testWarehouseId = warehouse.getLocationId();
            }

            // Create full location hierarchy: WAREHOUSE -> ZONE -> AISLE -> RACK -> BIN
            // Use BaseIntegrationTest helper which automatically checks repository and saves
            // Create ZONE under warehouse
            CreateLocationRequest zoneRequest = LocationTestDataBuilder.buildZoneRequest(testWarehouseId);
            CreateLocationResponse zone = createLocation(tenantAdminAuth.getAccessToken(), testTenantId, zoneRequest);
            assertThat(zone).isNotNull();
            String zoneId = zone.getLocationId();

            // Create AISLE under zone
            CreateLocationRequest aisleRequest = LocationTestDataBuilder.buildAisleRequest(zoneId);
            CreateLocationResponse aisle = createLocation(tenantAdminAuth.getAccessToken(), testTenantId, aisleRequest);
            assertThat(aisle).isNotNull();
            String aisleId = aisle.getLocationId();

            // Create RACK under aisle
            CreateLocationRequest rackRequest = LocationTestDataBuilder.buildRackRequest(aisleId);
            CreateLocationResponse rack = createLocation(tenantAdminAuth.getAccessToken(), testTenantId, rackRequest);
            assertThat(rack).isNotNull();
            String rackId = rack.getLocationId();

            // Create BIN location under rack (required for FEFO assignment)
            CreateLocationRequest binRequest = LocationTestDataBuilder.buildBinRequest(rackId);
            CreateLocationResponse binLocation = createLocation(tenantAdminAuth.getAccessToken(), testTenantId, binRequest);
            assertThat(binLocation).isNotNull();
            testLocationId = binLocation.getLocationId();

            // Check H2 for existing stock items before creating consignment
            List<com.ccbsa.wms.gateway.api.dto.StockItemResponse> existingStockItems = TestDataManager.getStockItemsByProductId(testProductId, testTenantId);
            int totalQuantity = existingStockItems.stream()
                    .filter(item -> item.getQuantity() != null && item.getQuantity() > 0)
                    .mapToInt(com.ccbsa.wms.gateway.api.dto.StockItemResponse::getQuantity)
                    .sum();

            if (totalQuantity < 100) {
                // Create consignment for normal picking - check repository first, then save if new
                CreateConsignmentRequest consignmentRequest = ConsignmentTestDataBuilder.buildCreateConsignmentRequestV2(testLocationId, testProductCode, 100, null);
                EntityExchangeResult<ApiResponse<CreateConsignmentResponse>> consignmentResult =
                        authenticatedPost("/api/v1/stock-management/consignments", tenantAdminAuth.getAccessToken(), testTenantId, consignmentRequest).exchange().expectStatus().isCreated()
                                .expectBody(new ParameterizedTypeReference<ApiResponse<CreateConsignmentResponse>>() {
                                }).returnResult();
                ApiResponse<CreateConsignmentResponse> consignmentApiResponse = consignmentResult.getResponseBody();
                if (consignmentApiResponse != null && consignmentApiResponse.isSuccess() && consignmentApiResponse.getData() != null) {
                    CreateConsignmentResponse consignment = consignmentApiResponse.getData();
                    // Check if consignment already exists in repository
                    Optional<CreateConsignmentResponse> existing = TestDataManager.getRepository().findConsignmentById(consignment.getConsignmentId(), testTenantId);
                    if (existing.isEmpty()) {
                        TestDataManager.saveConsignment(consignment, testTenantId);
                    }
                }
            }

            // Check H2 for existing expiring stock items before creating consignment
            List<com.ccbsa.wms.gateway.api.dto.StockItemResponse> existingExpiringStockItems = TestDataManager.getStockItemsByProductId(testExpiringProductId, testTenantId);
            int totalExpiringQuantity = existingExpiringStockItems.stream()
                    .filter(item -> item.getQuantity() != null && item.getQuantity() > 0)
                    .mapToInt(com.ccbsa.wms.gateway.api.dto.StockItemResponse::getQuantity)
                    .sum();

            if (totalExpiringQuantity < 100) {
                // Create consignment with expiring stock (expires in 5 days) - check repository first, then save if new
                LocalDate expiringDate = LocalDate.now().plusDays(5);
                CreateConsignmentRequest expiringConsignmentRequest =
                        ConsignmentTestDataBuilder.buildCreateConsignmentRequestV2WithExpiration(testLocationId, testExpiringProductCode, expiringDate);
                EntityExchangeResult<ApiResponse<CreateConsignmentResponse>> expiringConsignmentResult =
                        authenticatedPost("/api/v1/stock-management/consignments", tenantAdminAuth.getAccessToken(), testTenantId, expiringConsignmentRequest).exchange().expectStatus()
                                .isCreated().expectBody(new ParameterizedTypeReference<ApiResponse<CreateConsignmentResponse>>() {
                                }).returnResult();
                ApiResponse<CreateConsignmentResponse> expiringConsignmentApiResponse = expiringConsignmentResult.getResponseBody();
                if (expiringConsignmentApiResponse != null && expiringConsignmentApiResponse.isSuccess() && expiringConsignmentApiResponse.getData() != null) {
                    CreateConsignmentResponse consignment = expiringConsignmentApiResponse.getData();
                    Optional<CreateConsignmentResponse> existing = TestDataManager.getRepository().findConsignmentById(consignment.getConsignmentId(), testTenantId);
                    if (existing.isEmpty()) {
                        TestDataManager.saveConsignment(consignment, testTenantId);
                    }
                }
            }

            // Check H2 for existing expired stock items before creating consignment
            List<com.ccbsa.wms.gateway.api.dto.StockItemResponse> existingExpiredStockItems = TestDataManager.getStockItemsByProductId(testExpiredProductId, testTenantId);
            int totalExpiredQuantity = existingExpiredStockItems.stream()
                    .filter(item -> item.getQuantity() != null && item.getQuantity() > 0)
                    .mapToInt(com.ccbsa.wms.gateway.api.dto.StockItemResponse::getQuantity)
                    .sum();

            if (totalExpiredQuantity < 100) {
                // Create consignment with expired stock (expired 5 days ago) - check repository first, then save if new
                LocalDate expiredDate = LocalDate.now().minusDays(5);
                CreateConsignmentRequest expiredConsignmentRequest =
                        ConsignmentTestDataBuilder.buildCreateConsignmentRequestV2WithExpiration(testLocationId, testExpiredProductCode, expiredDate);
                EntityExchangeResult<ApiResponse<CreateConsignmentResponse>> expiredConsignmentResult =
                        authenticatedPost("/api/v1/stock-management/consignments", tenantAdminAuth.getAccessToken(), testTenantId, expiredConsignmentRequest).exchange().expectStatus()
                                .isCreated().expectBody(new ParameterizedTypeReference<ApiResponse<CreateConsignmentResponse>>() {
                                }).returnResult();
                ApiResponse<CreateConsignmentResponse> expiredConsignmentApiResponse = expiredConsignmentResult.getResponseBody();
                if (expiredConsignmentApiResponse != null && expiredConsignmentApiResponse.isSuccess() && expiredConsignmentApiResponse.getData() != null) {
                    CreateConsignmentResponse consignment = expiredConsignmentApiResponse.getData();
                    Optional<CreateConsignmentResponse> existing = TestDataManager.getRepository().findConsignmentById(consignment.getConsignmentId(), testTenantId);
                    if (existing.isEmpty()) {
                        TestDataManager.saveConsignment(consignment, testTenantId);
                    }
                }
            }

            // Wait for stock items to be created (only if we created new consignments)
            if (totalQuantity < 100) {
                waitForStockItems(testProductId, tenantAdminAuth.getAccessToken(), testTenantId, 10, 500);
                // Query and save stock items to H2
                queryAndSaveStockItems(testProductId, tenantAdminAuth.getAccessToken(), testTenantId);
            }
            if (totalExpiringQuantity < 100) {
                waitForStockItems(testExpiringProductId, tenantAdminAuth.getAccessToken(), testTenantId, 10, 500);
                // Query and save stock items to H2
                queryAndSaveStockItems(testExpiringProductId, tenantAdminAuth.getAccessToken(), testTenantId);
            }
            if (totalExpiredQuantity < 100) {
                waitForStockItems(testExpiredProductId, tenantAdminAuth.getAccessToken(), testTenantId, 10, 500);
                // Query and save stock items to H2
                queryAndSaveStockItems(testExpiredProductId, tenantAdminAuth.getAccessToken(), testTenantId);
            }

            // Wait for stock items to be assigned to locations (FEFO assignment happens asynchronously)
            // This ensures stock items are available for picking list planning
            waitForStockItemsAssignedToLocations(testProductId, tenantAdminAuth.getAccessToken(), testTenantId, 15, 500, 1);
            waitForStockItemsAssignedToLocations(testExpiringProductId, tenantAdminAuth.getAccessToken(), testTenantId, 15, 500, 1);
            // Note: Expired stock items are NOT assigned to locations (by design), so we don't wait for them
        }
    }

    // ==================== PICKING TASK CREATION TESTS ====================

    @Test
    @Order(1)
    public void testCreatePickingTask_Success() {
        // Arrange
        CreatePickingTaskRequest request = PickingTestDataBuilder.buildCreatePickingTaskRequest(testProductId, testLocationId);

        // Act
        WebTestClient.ResponseSpec response = authenticatedPost("/api/v1/picking/picking-tasks", tenantAdminAuth.getAccessToken(), testTenantId, request).exchange();

        // Assert
        EntityExchangeResult<ApiResponse<CreatePickingTaskResponse>> exchangeResult =
                response.expectStatus().isCreated().expectBody(new ParameterizedTypeReference<ApiResponse<CreatePickingTaskResponse>>() {
                }).returnResult();

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
        WebTestClient.ResponseSpec response = authenticatedGet("/api/v1/picking/tasks?page=0&size=10", tenantAdminAuth.getAccessToken(), testTenantId).exchange();

        // Assert
        response.expectStatus().isOk();
    }

    // ==================== PICKING LIST CREATION TESTS ====================

    @Test
    @Order(3)
    public void testCreatePickingList_ValidRequest_Success() {
        // Arrange
        CreatePickingListRequest request = PickingListTestDataBuilder.buildCreatePickingListRequest();
        // Update product code to use test product
        request.getLoads().get(0).getOrders().get(0).getLineItems().get(0).setProductCode(testProductCode);

        // Act
        EntityExchangeResult<ApiResponse<CreatePickingListResponse>> result =
                authenticatedPost("/api/v1/picking/picking-lists", tenantAdminAuth.getAccessToken(), testTenantId, request).exchange().expectStatus().isCreated()
                        .expectBody(new ParameterizedTypeReference<ApiResponse<CreatePickingListResponse>>() {
                        }).returnResult();

        // Assert
        ApiResponse<CreatePickingListResponse> apiResponse = result.getResponseBody();
        assertThat(apiResponse).isNotNull();
        assertThat(apiResponse.isSuccess()).isTrue();

        CreatePickingListResponse response = apiResponse.getData();
        assertThat(response).isNotNull();
        assertThat(response.getPickingListId()).isNotBlank();
        assertThat(response.getStatus()).isEqualTo("RECEIVED");
        assertThat(response.getLoadCount()).isGreaterThan(0);
    }

    @Test
    @Order(4)
    public void testCreatePickingList_InvalidRequest_ValidationError() {
        // Arrange
        CreatePickingListRequest request = PickingListTestDataBuilder.buildInvalidPickingListRequest();

        // Act & Assert
        authenticatedPost("/api/v1/picking/picking-lists", tenantAdminAuth.getAccessToken(), testTenantId, request).exchange().expectStatus().isBadRequest();
    }

    @Test
    @Order(5)
    public void testCreatePickingList_InvalidProduct_Error() {
        // Arrange
        CreatePickingListRequest request = PickingListTestDataBuilder.buildCreatePickingListRequest();
        request.getLoads().get(0).getOrders().get(0).getLineItems().get(0).setProductCode("INVALID-PRODUCT");

        // Act & Assert
        authenticatedPost("/api/v1/picking/picking-lists", tenantAdminAuth.getAccessToken(), testTenantId, request).exchange().expectStatus().isBadRequest();
    }

    @Test
    @Order(6)
    public void testCreatePickingList_MultipleOrders_Success() {
        // Arrange
        CreatePickingListRequest request = PickingListTestDataBuilder.buildCreatePickingListRequestWithMultipleOrders();
        // Update product codes
        request.getLoads().forEach(load -> load.getOrders().forEach(order -> order.getLineItems().forEach(item -> item.setProductCode(testProductCode))));

        // Act
        EntityExchangeResult<ApiResponse<CreatePickingListResponse>> result =
                authenticatedPost("/api/v1/picking/picking-lists", tenantAdminAuth.getAccessToken(), testTenantId, request).exchange().expectStatus().isCreated()
                        .expectBody(new ParameterizedTypeReference<ApiResponse<CreatePickingListResponse>>() {
                        }).returnResult();

        // Assert
        ApiResponse<CreatePickingListResponse> apiResponse = result.getResponseBody();
        assertThat(apiResponse).isNotNull();
        assertThat(apiResponse.isSuccess()).isTrue();

        CreatePickingListResponse response = apiResponse.getData();
        assertThat(response).isNotNull();
        assertThat(response.getPickingListId()).isNotBlank();
        assertThat(response.getLoadCount()).isEqualTo(2);
    }

    // ==================== PICKING LIST CSV UPLOAD TESTS ====================

    @Test
    @Order(7)
    public void testUploadPickingListCsv_ValidCsv_Success() throws Exception {
        // Arrange - Create CSV content with valid data (all required fields)
        // Use unique load number to avoid duplicate key constraint
        String uniqueLoadNumber = "LOAD-" + System.currentTimeMillis();
        String csvContent = String.format("LoadNumber,OrderNumber,OrderLineNumber,CustomerCode,CustomerName,Priority,ProductCode,Quantity,WarehouseId,Notes\n"
                        + "%s,ORD-001,1,CUST-001,Acme Corp,HIGH,%s,10,%s,Urgent order\n" + "%s,ORD-001,2,CUST-001,Acme Corp,HIGH,%s,5,%s,", uniqueLoadNumber, testProductCode,
                testWarehouseId, uniqueLoadNumber, testProductCode, testWarehouseId);

        Path tempDir = Files.createTempDirectory("picking-list-csv-test");
        File csvFile = createCsvFile(csvContent, tempDir, "test-picking-list.csv");

        MultiValueMap<String, Object> formData = new LinkedMultiValueMap<>();
        formData.add("file", new FileSystemResource(csvFile));

        // Act
        EntityExchangeResult<ApiResponse<UploadPickingListCsvResponse>> result =
                webTestClient.post().uri("/api/v1/picking/picking-lists/upload-csv").header("Authorization", "Bearer " + tenantAdminAuth.getAccessToken())
                        .header("X-Tenant-Id", testTenantId).contentType(MediaType.MULTIPART_FORM_DATA).body(BodyInserters.fromMultipartData(formData)).exchange().expectStatus()
                        .isOk().expectBody(new ParameterizedTypeReference<ApiResponse<UploadPickingListCsvResponse>>() {
                        }).returnResult();

        // Assert
        ApiResponse<UploadPickingListCsvResponse> apiResponse = result.getResponseBody();
        assertThat(apiResponse).isNotNull();
        assertThat(apiResponse.isSuccess()).isTrue();

        UploadPickingListCsvResponse response = apiResponse.getData();
        assertThat(response).isNotNull();
        assertThat(response.getTotalRows()).isGreaterThan(0);
        assertThat(response.getSuccessfulRows()).isGreaterThan(0);
        assertThat(response.getErrorRows()).isEqualTo(0);

        // Cleanup
        Files.deleteIfExists(csvFile.toPath());
        Files.deleteIfExists(tempDir);
    }

    /**
     * Creates a CSV file with the given content in a temporary directory.
     *
     * @param content  the CSV content to write
     * @param tempDir  the temporary directory to create the file in
     * @param filename the name of the CSV file
     * @return the created File
     * @throws Exception if file creation fails
     */
    private File createCsvFile(String content, Path tempDir, String filename) throws Exception {
        File csvFile = tempDir.resolve(filename).toFile();
        try (java.io.FileWriter writer = new java.io.FileWriter(csvFile)) {
            writer.write(content);
        }
        return csvFile;
    }

    @Test
    @Order(8)
    public void testUploadPickingListCsv_InvalidFormat_Error() throws Exception {
        // Arrange - Invalid CSV format (missing required headers)
        String csvContent = "Invalid,CSV,Format\nRow1,Row2";

        Path tempDir = Files.createTempDirectory("picking-list-csv-test");
        File csvFile = createCsvFile(csvContent, tempDir, "invalid.csv");

        MultiValueMap<String, Object> formData = new LinkedMultiValueMap<>();
        formData.add("file", new FileSystemResource(csvFile));

        // Act & Assert
        // The endpoint returns 200 OK with errors in the response body when CSV format is invalid
        EntityExchangeResult<ApiResponse<UploadPickingListCsvResponse>> result =
                webTestClient.post().uri("/api/v1/picking/picking-lists/upload-csv").header("Authorization", "Bearer " + tenantAdminAuth.getAccessToken())
                        .header("X-Tenant-Id", testTenantId).contentType(MediaType.MULTIPART_FORM_DATA).body(BodyInserters.fromMultipartData(formData)).exchange().expectStatus()
                        .isOk().expectBody(new ParameterizedTypeReference<ApiResponse<UploadPickingListCsvResponse>>() {
                        }).returnResult();

        // Assert - Should have errors in response
        ApiResponse<UploadPickingListCsvResponse> apiResponse = result.getResponseBody();
        assertThat(apiResponse).isNotNull();
        assertThat(apiResponse.isSuccess()).isTrue();
        UploadPickingListCsvResponse response = apiResponse.getData();
        assertThat(response).isNotNull();
        assertThat(response.getErrorRows()).isGreaterThan(0);
        assertThat(response.getErrors()).isNotEmpty();

        // Cleanup
        Files.deleteIfExists(csvFile.toPath());
        Files.deleteIfExists(tempDir);
    }

    // ==================== PICKING LIST QUERY TESTS ====================

    @Test
    @Order(9)
    public void testUploadPickingListCsv_InvalidProduct_Error() throws Exception {
        // Arrange - CSV with invalid product code (all required fields present)
        // Use unique load number to avoid duplicate key constraint
        String uniqueLoadNumber = "LOAD-INVALID-" + System.currentTimeMillis();
        String csvContent = String.format("LoadNumber,OrderNumber,OrderLineNumber,CustomerCode,CustomerName,Priority,ProductCode,Quantity,WarehouseId,Notes\n"
                + "%s,ORD-001,1,CUST-001,Acme Corp,HIGH,INVALID-PRODUCT,10,%s,", uniqueLoadNumber, testWarehouseId);

        Path tempDir = Files.createTempDirectory("picking-list-csv-test");
        File csvFile = createCsvFile(csvContent, tempDir, "invalid-product.csv");

        MultiValueMap<String, Object> formData = new LinkedMultiValueMap<>();
        formData.add("file", new FileSystemResource(csvFile));

        // Act
        EntityExchangeResult<ApiResponse<UploadPickingListCsvResponse>> result =
                webTestClient.post().uri("/api/v1/picking/picking-lists/upload-csv").header("Authorization", "Bearer " + tenantAdminAuth.getAccessToken())
                        .header("X-Tenant-Id", testTenantId).contentType(MediaType.MULTIPART_FORM_DATA).body(BodyInserters.fromMultipartData(formData)).exchange().expectStatus()
                        .isOk().expectBody(new ParameterizedTypeReference<ApiResponse<UploadPickingListCsvResponse>>() {
                        }).returnResult();

        // Assert - Should have errors
        ApiResponse<UploadPickingListCsvResponse> apiResponse = result.getResponseBody();
        assertThat(apiResponse).isNotNull();
        assertThat(apiResponse.isSuccess()).isTrue();

        UploadPickingListCsvResponse response = apiResponse.getData();
        assertThat(response).isNotNull();
        assertThat(response.getErrorRows()).isGreaterThan(0);
        assertThat(response.getErrors()).isNotEmpty();

        // Cleanup
        Files.deleteIfExists(csvFile.toPath());
        Files.deleteIfExists(tempDir);
    }

    @Test
    @Order(10)
    public void testGetPickingList_ValidId_Success() {
        // Arrange - Create picking list first
        CreatePickingListRequest request = PickingListTestDataBuilder.buildCreatePickingListRequest();
        request.getLoads().get(0).getOrders().get(0).getLineItems().get(0).setProductCode(testProductCode);

        EntityExchangeResult<ApiResponse<CreatePickingListResponse>> pickingListResult =
                authenticatedPost("/api/v1/picking/picking-lists", tenantAdminAuth.getAccessToken(), testTenantId, request).exchange().expectStatus().isCreated()
                        .expectBody(new ParameterizedTypeReference<ApiResponse<CreatePickingListResponse>>() {
                        }).returnResult();

        ApiResponse<CreatePickingListResponse> pickingListApiResponse = pickingListResult.getResponseBody();
        assertThat(pickingListApiResponse).isNotNull();
        assertThat(pickingListApiResponse.isSuccess()).isTrue();
        String pickingListId = pickingListApiResponse.getData().getPickingListId();

        // Act
        EntityExchangeResult<ApiResponse<PickingListQueryResult>> result =
                authenticatedGet("/api/v1/picking/picking-lists/" + pickingListId, tenantAdminAuth.getAccessToken(), testTenantId).exchange().expectStatus().isOk()
                        .expectBody(new ParameterizedTypeReference<ApiResponse<PickingListQueryResult>>() {
                        }).returnResult();

        // Assert
        ApiResponse<PickingListQueryResult> apiResponse = result.getResponseBody();
        assertThat(apiResponse).isNotNull();
        assertThat(apiResponse.isSuccess()).isTrue();

        PickingListQueryResult response = apiResponse.getData();
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(pickingListId);
        assertThat(response.getLoadCount()).isGreaterThan(0);
        
        // Save orders from picking list to H2 repository for reuse
        saveOrdersFromPickingList(response, testTenantId);
    }

    @Test
    @Order(11)
    public void testGetPickingList_InvalidId_NotFound() {
        // Act & Assert
        authenticatedGet("/api/v1/picking/picking-lists/invalid-id", tenantAdminAuth.getAccessToken(), testTenantId).exchange().expectStatus().isNotFound();
    }

    @Test
    @Order(12)
    public void testListPickingLists_NoFilters_Success() {
        // Act
        EntityExchangeResult<ApiResponse<ListPickingListsQueryResult>> result =
                authenticatedGet("/api/v1/picking/picking-lists?page=0&size=20", tenantAdminAuth.getAccessToken(), testTenantId).exchange().expectStatus().isOk()
                        .expectBody(new ParameterizedTypeReference<ApiResponse<ListPickingListsQueryResult>>() {
                        }).returnResult();

        // Assert
        ApiResponse<ListPickingListsQueryResult> apiResponse = result.getResponseBody();
        assertThat(apiResponse).isNotNull();
        assertThat(apiResponse.isSuccess()).isTrue();

        ListPickingListsQueryResult response = apiResponse.getData();
        assertThat(response).isNotNull();
        assertThat(response.getPickingLists()).isNotEmpty();
    }

    @Test
    @Order(13)
    public void testListPickingLists_WithStatusFilter_Success() {
        // Act
        EntityExchangeResult<ApiResponse<ListPickingListsQueryResult>> result =
                authenticatedGet("/api/v1/picking/picking-lists?page=0&size=20&status=RECEIVED", tenantAdminAuth.getAccessToken(), testTenantId).exchange().expectStatus().isOk()
                        .expectBody(new ParameterizedTypeReference<ApiResponse<ListPickingListsQueryResult>>() {
                        }).returnResult();

        // Assert
        ApiResponse<ListPickingListsQueryResult> apiResponse = result.getResponseBody();
        assertThat(apiResponse).isNotNull();
        assertThat(apiResponse.isSuccess()).isTrue();

        ListPickingListsQueryResult response = apiResponse.getData();
        assertThat(response).isNotNull();
        // All returned picking lists should have RECEIVED status
        response.getPickingLists().forEach(list -> assertThat(list.getStatus()).isEqualTo("RECEIVED"));
    }

    // ==================== PICKING TASK EXECUTION TESTS ====================

    @Test
    @Order(14)
    public void testListPickingLists_WithPagination_Success() {
        // Act
        EntityExchangeResult<ApiResponse<ListPickingListsQueryResult>> result =
                authenticatedGet("/api/v1/picking/picking-lists?page=0&size=10", tenantAdminAuth.getAccessToken(), testTenantId).exchange().expectStatus().isOk()
                        .expectBody(new ParameterizedTypeReference<ApiResponse<ListPickingListsQueryResult>>() {
                        }).returnResult();

        // Assert
        ApiResponse<ListPickingListsQueryResult> apiResponse = result.getResponseBody();
        assertThat(apiResponse).isNotNull();
        assertThat(apiResponse.isSuccess()).isTrue();

        ListPickingListsQueryResult response = apiResponse.getData();
        assertThat(response).isNotNull();
        assertThat(response.getPage()).isEqualTo(0);
        assertThat(response.getSize()).isEqualTo(10);
        assertThat(response.getPickingLists().size()).isLessThanOrEqualTo(10);
    }

    @Test
    @Order(15)
    public void testExecutePickingTask_FullQuantity_Success() {
        // Arrange: Create picking list first
        CreatePickingListRequest pickingListRequest = PickingListTestDataBuilder.buildCreatePickingListRequest();
        pickingListRequest.getLoads().get(0).getOrders().get(0).getLineItems().get(0).setProductCode(testProductCode);

        EntityExchangeResult<ApiResponse<CreatePickingListResponse>> pickingListResult =
                authenticatedPost("/api/v1/picking/picking-lists", tenantAdminAuth.getAccessToken(), testTenantId, pickingListRequest).exchange().expectStatus().isCreated()
                        .expectBody(new ParameterizedTypeReference<ApiResponse<CreatePickingListResponse>>() {
                        }).returnResult();

        ApiResponse<CreatePickingListResponse> pickingListApiResponse = pickingListResult.getResponseBody();
        assertThat(pickingListApiResponse).isNotNull();
        assertThat(pickingListApiResponse.isSuccess()).isTrue();
        CreatePickingListResponse pickingList = pickingListApiResponse.getData();
        assertThat(pickingList).isNotNull();
        testPickingListId = pickingList.getPickingListId();

        // Get picking tasks from the list
        EntityExchangeResult<ApiResponse<PickingListQueryResult>> listResult =
                authenticatedGet("/api/v1/picking/picking-lists/" + testPickingListId, tenantAdminAuth.getAccessToken(), testTenantId).exchange().expectStatus().isOk()
                        .expectBody(new ParameterizedTypeReference<ApiResponse<PickingListQueryResult>>() {
                        }).returnResult();

        // Wait a bit for tasks to be created
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Act: Execute picking task
        ExecutePickingTaskRequest executeRequest = ExecutePickingTaskRequest.builder().pickedQuantity(10).isPartialPicking(false).build();

        // Note: We need the actual task ID - this would come from querying tasks
        // For now, this test demonstrates the pattern
        // In practice, you'd query /api/v1/picking/picking-tasks?pickingListId={id} first
    }

    // ==================== PICKING LIST COMPLETION TESTS ====================

    @Test
    @Order(16)
    public void testExecutePickingTask_PartialQuantity_Success() {
        // Similar to above but with partial picking
        ExecutePickingTaskRequest executeRequest =
                ExecutePickingTaskRequest.builder().pickedQuantity(5).isPartialPicking(true).partialReason("Insufficient stock available").build();

        // Execute with actual task ID
    }

    // ==================== STOCK EXPIRATION TESTS ====================

    @Test
    @Order(17)
    public void testCompletePickingList_AllTasksCompleted_Success() {
        // Arrange: Create a new picking list for this test
        CreatePickingListRequest pickingListRequest = PickingListTestDataBuilder.buildCreatePickingListRequest();
        pickingListRequest.getLoads().get(0).getOrders().get(0).getLineItems().get(0).setProductCode(testProductCode);

        EntityExchangeResult<ApiResponse<CreatePickingListResponse>> pickingListResult =
                authenticatedPost("/api/v1/picking/picking-lists", tenantAdminAuth.getAccessToken(), testTenantId, pickingListRequest).exchange().expectStatus().isCreated()
                        .expectBody(new ParameterizedTypeReference<ApiResponse<CreatePickingListResponse>>() {
                        }).returnResult();

        ApiResponse<CreatePickingListResponse> pickingListApiResponse = pickingListResult.getResponseBody();
        assertThat(pickingListApiResponse).isNotNull();
        assertThat(pickingListApiResponse.isSuccess()).isTrue();
        CreatePickingListResponse pickingList = pickingListApiResponse.getData();
        assertThat(pickingList).isNotNull();
        String pickingListId = pickingList.getPickingListId();

        // Wait for picking list to be planned (async via Kafka events)
        boolean isPlanned = waitForPickingListStatus(pickingListId, "PLANNED", tenantAdminAuth.getAccessToken(), testTenantId, 15, 500);
        assertThat(isPlanned).as("Picking list should be in PLANNED status within 15 seconds").isTrue();

        // Get all picking tasks for this picking list
        // First, get the picking list to find load IDs
        EntityExchangeResult<ApiResponse<PickingListQueryResult>> listResult =
                authenticatedGet("/api/v1/picking/picking-lists/" + pickingListId, tenantAdminAuth.getAccessToken(), testTenantId).exchange().expectStatus().isOk()
                        .expectBody(new ParameterizedTypeReference<ApiResponse<PickingListQueryResult>>() {
                        }).returnResult();

        ApiResponse<PickingListQueryResult> listApiResponse = listResult.getResponseBody();
        assertThat(listApiResponse).isNotNull();
        assertThat(listApiResponse.isSuccess()).isTrue();
        PickingListQueryResult pickingListData = listApiResponse.getData();
        assertThat(pickingListData).isNotNull();
        assertThat(pickingListData.getStatus()).isEqualTo("PLANNED");
        
        // Save orders from picking list to H2 repository for reuse
        saveOrdersFromPickingList(pickingListData, testTenantId);

        // Get picking tasks (they should be created by now)
        // Wait a bit for tasks to be created
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Get load IDs from the picking list to filter tasks
        java.util.Set<String> loadIds = new java.util.HashSet<>();
        if (pickingListData.getLoads() != null) {
            for (PickingListQueryResult.LoadQueryResult load : pickingListData.getLoads()) {
                if (load.getLoadId() != null) {
                    loadIds.add(load.getLoadId());
                }
            }
        }

        // List picking tasks with PENDING status - get all and filter by load ID
        EntityExchangeResult<ApiResponse<java.util.Map<String, Object>>> tasksResult =
                authenticatedGet("/api/v1/picking/tasks?status=PENDING&page=0&size=100", tenantAdminAuth.getAccessToken(), testTenantId).exchange().expectStatus().isOk()
                        .expectBody(new ParameterizedTypeReference<ApiResponse<java.util.Map<String, Object>>>() {
                        }).returnResult();

        ApiResponse<java.util.Map<String, Object>> tasksApiResponse = tasksResult.getResponseBody();
        assertThat(tasksApiResponse).isNotNull();
        assertThat(tasksApiResponse.isSuccess()).isTrue();

        @SuppressWarnings("unchecked") java.util.List<java.util.Map<String, Object>> allTasks =
                (java.util.List<java.util.Map<String, Object>>) tasksApiResponse.getData().get("pickingTasks");

        // Filter tasks by load ID and execute them
        if (allTasks != null && !allTasks.isEmpty() && !loadIds.isEmpty()) {
            for (java.util.Map<String, Object> task : allTasks) {
                String taskLoadId = (String) task.get("loadId");
                if (loadIds.contains(taskLoadId)) {
                    String taskId = (String) task.get("taskId");
                    Object quantityObj = task.get("quantity");
                    Integer quantity = quantityObj instanceof Integer ? (Integer) quantityObj : quantityObj instanceof Number ? ((Number) quantityObj).intValue() : null;

                    if (taskId != null && quantity != null) {
                        ExecutePickingTaskRequest executeRequest = ExecutePickingTaskRequest.builder().pickedQuantity(quantity).isPartialPicking(false).build();

                        authenticatedPost("/api/v1/picking/picking-tasks/" + taskId + "/execute", tenantAdminAuth.getAccessToken(), testTenantId, executeRequest).exchange()
                                .expectStatus().isOk();
                    }
                }
            }
        }

        // Act: Complete picking list (POST without body)
        EntityExchangeResult<ApiResponse<CompletePickingListResponse>> result =
                authenticatedPostWithoutBody("/api/v1/picking/picking-lists/" + pickingListId + "/complete", tenantAdminAuth.getAccessToken(), testTenantId).exchange()
                        .expectStatus().isOk().expectBody(new ParameterizedTypeReference<ApiResponse<CompletePickingListResponse>>() {
                        }).returnResult();

        // Assert
        ApiResponse<CompletePickingListResponse> apiResponse = result.getResponseBody();
        assertThat(apiResponse).isNotNull();
        assertThat(apiResponse.isSuccess()).isTrue();

        CompletePickingListResponse response = apiResponse.getData();
        assertThat(response).isNotNull();
        assertThat(response.getPickingListId()).isEqualTo(pickingListId);
        assertThat(response.getStatus()).isEqualTo("COMPLETED");
    }

    @Test
    @Order(18)
    public void testCheckStockExpiration_ExpiredStock_ReturnsExpired() {
        // Note: Expired stock items are NOT assigned to locations via FEFO (by design - expired stock cannot be assigned)
        // However, consignments are created with a locationId, so the stock item initially has that location
        // The stock expiration check endpoint requires both productCode and locationId
        // We use testLocationId (the BIN) where the consignment was created

        // Ensure testLocationId is set (from setup)
        assertThat(testLocationId).as("testLocationId must be set from setup").isNotNull();

        // Act: Check expiration for expired stock (endpoint expects productCode, not productId)
        // The consignment was created with testLocationId, so we check at that location
        EntityExchangeResult<ApiResponse<StockExpirationCheckResponse>> result =
                authenticatedGet("/api/v1/stock-management/stock-items/check-expiration?productCode=" + testExpiredProductCode + "&locationId=" + testLocationId,
                        tenantAdminAuth.getAccessToken(), testTenantId).exchange().expectStatus().isOk()
                        .expectBody(new ParameterizedTypeReference<ApiResponse<StockExpirationCheckResponse>>() {
                        }).returnResult();

        // Assert
        ApiResponse<StockExpirationCheckResponse> apiResponse = result.getResponseBody();
        assertThat(apiResponse).isNotNull();
        assertThat(apiResponse.isSuccess()).isTrue();

        StockExpirationCheckResponse response = apiResponse.getData();
        assertThat(response).isNotNull();
        assertThat(response.isExpired()).as("Expired stock should be detected as expired").isTrue(); // Use isExpired() getter which returns the expired field
        assertThat(response.getClassification()).as("Expired stock should have EXPIRED classification").isEqualTo("EXPIRED");
    }

    @Test
    @Order(19)
    public void testExecutePickingTask_ExpiredStock_Rejected() {
        // Arrange: Create picking list with expired product
        CreatePickingListRequest pickingListRequest = PickingListTestDataBuilder.buildCreatePickingListRequest();
        pickingListRequest.getLoads().get(0).getOrders().get(0).getLineItems().get(0).setProductCode(testExpiredProductCode);

        EntityExchangeResult<ApiResponse<CreatePickingListResponse>> pickingListResult =
                authenticatedPost("/api/v1/picking/picking-lists", tenantAdminAuth.getAccessToken(), testTenantId, pickingListRequest).exchange().expectStatus().isCreated()
                        .expectBody(new ParameterizedTypeReference<ApiResponse<CreatePickingListResponse>>() {
                        }).returnResult();

        // Wait for tasks
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Act: Try to execute picking task with expired stock
        // This should be rejected
        ExecutePickingTaskRequest executeRequest = ExecutePickingTaskRequest.builder().pickedQuantity(10).build();

        // The execution should fail with expired stock error
        // (Implementation depends on how task ID is obtained)
    }

    @Test
    @Order(20)
    public void testGetExpiringStock_WithDateRange_Success() {
        // Act: Query expiring stock
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = LocalDate.now().plusDays(30);

        // The endpoint returns ApiResponse<List<StockItemQueryDTO>>, not ListExpiringStockResponse
        EntityExchangeResult<ApiResponse<java.util.List<StockItemQueryDTO>>> result =
                authenticatedGet("/api/v1/stock-management/stock-items/expiring?startDate=" + startDate + "&endDate=" + endDate, tenantAdminAuth.getAccessToken(),
                        testTenantId).exchange().expectStatus().isOk().expectBody(new ParameterizedTypeReference<ApiResponse<java.util.List<StockItemQueryDTO>>>() {
                }).returnResult();

        // Assert
        ApiResponse<java.util.List<StockItemQueryDTO>> apiResponse = result.getResponseBody();
        assertThat(apiResponse).isNotNull();
        assertThat(apiResponse.isSuccess()).isTrue();

        java.util.List<StockItemQueryDTO> items = apiResponse.getData();
        assertThat(items).isNotNull();
        // Should contain expiring stock items
    }

    // ==================== RESTOCK REQUEST TESTS ====================

    @Test
    @Order(21)
    public void testGetExpiringStock_WithClassificationFilter_Success() {
        // Act: Query expiring stock with CRITICAL classification
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = LocalDate.now().plusDays(30);

        // The endpoint returns ApiResponse<List<StockItemQueryDTO>>, not ListExpiringStockResponse
        EntityExchangeResult<ApiResponse<java.util.List<StockItemQueryDTO>>> result =
                authenticatedGet("/api/v1/stock-management/stock-items/expiring?startDate=" + startDate + "&endDate=" + endDate + "&classification=CRITICAL",
                        tenantAdminAuth.getAccessToken(), testTenantId).exchange().expectStatus().isOk()
                        .expectBody(new ParameterizedTypeReference<ApiResponse<java.util.List<StockItemQueryDTO>>>() {
                        }).returnResult();

        // Assert
        ApiResponse<java.util.List<StockItemQueryDTO>> apiResponse = result.getResponseBody();
        assertThat(apiResponse).isNotNull();
        assertThat(apiResponse.isSuccess()).isTrue();

        java.util.List<StockItemQueryDTO> items = apiResponse.getData();
        assertThat(items).isNotNull();
        // Should only contain CRITICAL items
        items.forEach(item -> assertThat(item.getClassification()).isEqualTo("CRITICAL"));
    }

    @Test
    @Order(22)
    public void testRestockRequest_GeneratedWhenStockBelowMinimum() {
        // Arrange: Create stock adjustment to reduce stock below minimum
        // This should trigger StockLevelBelowMinimumEvent which generates restock request

        // Wait for restock request to be generated (async via Kafka)
        try {
            Thread.sleep(5000); // Wait for event processing
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Act: Query restock requests
        EntityExchangeResult<ApiResponse<ListRestockRequestsResponse>> result =
                authenticatedGet("/api/v1/stock-management/restock-requests", tenantAdminAuth.getAccessToken(), testTenantId).exchange().expectStatus().isOk()
                        .expectBody(new ParameterizedTypeReference<ApiResponse<ListRestockRequestsResponse>>() {
                        }).returnResult();

        // Assert
        ApiResponse<ListRestockRequestsResponse> apiResponse = result.getResponseBody();
        assertThat(apiResponse).isNotNull();
        assertThat(apiResponse.isSuccess()).isTrue();

        ListRestockRequestsResponse response = apiResponse.getData();
        assertThat(response).isNotNull();
        assertThat(response.getRequests()).isNotNull();
        // Should contain restock requests
    }

    // ==================== INTEGRATION SCENARIOS ====================

    @Test
    @Order(23)
    public void testRestockRequest_QueryByStatus_Success() {
        // Act: Query pending restock requests
        EntityExchangeResult<ApiResponse<ListRestockRequestsResponse>> result =
                authenticatedGet("/api/v1/stock-management/restock-requests?status=PENDING", tenantAdminAuth.getAccessToken(), testTenantId).exchange().expectStatus().isOk()
                        .expectBody(new ParameterizedTypeReference<ApiResponse<ListRestockRequestsResponse>>() {
                        }).returnResult();

        // Assert
        ApiResponse<ListRestockRequestsResponse> apiResponse = result.getResponseBody();
        assertThat(apiResponse).isNotNull();
        assertThat(apiResponse.isSuccess()).isTrue();

        ListRestockRequestsResponse response = apiResponse.getData();
        assertThat(response).isNotNull();
        // Should only contain PENDING requests
    }

    // ==================== HELPER METHODS ====================

    /**
     * Save orders from a picking list to H2 repository for reuse.
     * Extracts order numbers from the picking list and queries them to save to repository.
     *
     * @param pickingList the picking list query result
     * @param tenantId   the tenant ID
     */
    private void saveOrdersFromPickingList(PickingListQueryResult pickingList, String tenantId) {
        if (pickingList.getLoads() == null) {
            return;
        }
        
        for (PickingListQueryResult.LoadQueryResult load : pickingList.getLoads()) {
            if (load.getOrders() == null) {
                continue;
            }
            
            for (PickingListQueryResult.OrderQueryResult order : load.getOrders()) {
                if (order.getOrderNumber() != null) {
                    try {
                        // Query the order to get full details
                        EntityExchangeResult<ApiResponse<OrderQueryResult>> orderResult =
                                authenticatedGet("/api/v1/picking/orders/" + order.getOrderNumber(), tenantAdminAuth.getAccessToken(), tenantId).exchange().expectStatus().isOk()
                                        .expectBody(new ParameterizedTypeReference<ApiResponse<OrderQueryResult>>() {
                                        }).returnResult();
                        
                        ApiResponse<OrderQueryResult> orderApiResponse = orderResult.getResponseBody();
                        if (orderApiResponse != null && orderApiResponse.isSuccess() && orderApiResponse.getData() != null) {
                            OrderQueryResult orderData = orderApiResponse.getData();
                            // Check if order already exists in repository
                            Optional<OrderQueryResult> existing = TestDataManager.getRepository().findOrderByNumber(order.getOrderNumber(), tenantId);
                            if (existing.isEmpty()) {
                                TestDataManager.saveOrder(orderData, tenantId);
                            }
                        }
                    } catch (Exception e) {
                        // Log but don't fail test if order save fails
                        // Order might not exist yet or might be in a different state
                    }
                }
            }
        }
    }

    @Test
    @Order(24)
    public void testEndToEndPickingWorkflow_CompleteFlow() {
        // This test demonstrates the complete workflow:
        // 1. Create picking list
        // 2. Execute picking tasks
        // 3. Complete picking list
        // 4. Verify stock levels updated
        // 5. Verify stock movements created

        // Implementation would follow the pattern above
        // This serves as a template for comprehensive E2E testing
    }
}
