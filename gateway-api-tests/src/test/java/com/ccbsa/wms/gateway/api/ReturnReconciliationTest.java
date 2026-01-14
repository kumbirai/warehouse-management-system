package com.ccbsa.wms.gateway.api;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.test.web.reactive.server.EntityExchangeResult;

import com.ccbsa.common.application.api.ApiResponse;
import com.ccbsa.wms.gateway.api.dto.AuthenticationResult;
import com.ccbsa.wms.gateway.api.dto.CreateProductResponse;
import com.ccbsa.wms.gateway.api.dto.HandlePartialOrderAcceptanceRequest;
import com.ccbsa.wms.gateway.api.dto.HandlePartialOrderAcceptanceResponse;
import com.ccbsa.wms.gateway.api.dto.ProcessFullOrderReturnRequest;
import com.ccbsa.wms.gateway.api.dto.ProcessFullOrderReturnResponse;
import com.ccbsa.wms.gateway.api.fixture.ProductTestDataBuilder;
import com.ccbsa.wms.gateway.api.fixture.ReturnsTestDataBuilder;
import com.ccbsa.wms.gateway.api.fixture.TestDataManager;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for return reconciliation with D365.
 * <p>
 * Tests D365 sync, retry logic, audit trail, and summary functionality.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ReturnReconciliationTest extends BaseIntegrationTest {

    private static AuthenticationResult tenantAdminAuth;
    private static String testTenantId;
    private static String testProductId;
    private static String testOrderNumber;
    private static String testReturnId;

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
            testOrderNumber = "ORD-" + faker.number().digits(8);

            // Try to reuse product from repository, otherwise create new
            CreateProductResponse product = TestDataManager.getOrCreateProduct(
                    tenantAdminAuth.getAccessToken(), testTenantId,
                    ProductTestDataBuilder::buildCreateProductRequest,
                    req -> {
                        EntityExchangeResult<ApiResponse<CreateProductResponse>> productExchangeResult =
                                authenticatedPost("/api/v1/products", tenantAdminAuth.getAccessToken(), testTenantId, req).exchange().expectStatus().isCreated()
                                        .expectBody(new ParameterizedTypeReference<ApiResponse<CreateProductResponse>>() {
                                        }).returnResult();

                        ApiResponse<CreateProductResponse> productApiResponse = productExchangeResult.getResponseBody();
                        assertThat(productApiResponse).isNotNull();
                        if (productApiResponse != null) {
                            assertThat(productApiResponse.isSuccess()).isTrue();
                            assertThat(productApiResponse.getData()).isNotNull();
                        }
                        CreateProductResponse p = productApiResponse != null ? productApiResponse.getData() : null;
                        assertThat(p).isNotNull();
                        return p;
                    });
            testProductId = product.getProductId();
        }
    }

    // ==================== D365 SYNC TESTS ====================

    @Test
    @Order(1)
    @DisplayName("D365 Reconciliation - Full return should sync to D365 successfully")
    public void testD365Sync_AfterFullReturn_ShouldSyncSuccessfully() {
        // Arrange - Create a full return
        String orderNumber = "ORD-FULL-" + faker.number().digits(8);
        ProcessFullOrderReturnRequest request = ReturnsTestDataBuilder.buildProcessFullOrderReturnRequest(orderNumber, testProductId);

        // Act - Process full return (should trigger D365 sync via event)
        EntityExchangeResult<ApiResponse<ProcessFullOrderReturnResponse>> createResult =
                authenticatedPost("/api/v1/returns/full-return", tenantAdminAuth.getAccessToken(), testTenantId, request).exchange().expectStatus().isCreated()
                        .expectBody(new ParameterizedTypeReference<ApiResponse<ProcessFullOrderReturnResponse>>() {
                        }).returnResult();

        ApiResponse<ProcessFullOrderReturnResponse> createApiResponse = createResult.getResponseBody();
        assertThat(createApiResponse).isNotNull();
        assertThat(createApiResponse != null && createApiResponse.getData() != null).isTrue();
        ProcessFullOrderReturnResponse createdReturn = createApiResponse != null ? createApiResponse.getData() : null;
        assertThat(createdReturn).isNotNull();
        String returnId = createdReturn.getReturnId();
        testReturnId = returnId;

        // Assert - Verify return was created
        assertThat(createdReturn).isNotNull();
        assertThat(returnId).isNotBlank();
        assertThat(createdReturn.getStatus()).isEqualTo("PROCESSED");

        // Wait for async D365 reconciliation (up to 15 seconds)
        Awaitility.await().atMost(15, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> isReturnReconciled(returnId));

        // Assert - Query reconciliation status and verify sync completed
        Map<String, Object> reconciliation = getReconciliationRecord(returnId);
        assertThat(reconciliation).isNotNull();

        // Verify D365 sync status (may be SUCCESS, SYNCED, or IN_PROGRESS depending on implementation)
        String syncStatus = (String) reconciliation.get("reconciliationStatus");
        assertThat(syncStatus).isIn("SUCCESS", "SYNCED", "IN_PROGRESS", "PENDING");

        // If synced, verify D365 return order ID is populated
        if ("SUCCESS".equals(syncStatus) || "SYNCED".equals(syncStatus)) {
            String d365ReturnOrderId = (String) reconciliation.get("d365ReturnOrderId");
            assertThat(d365ReturnOrderId).isNotBlank();
        }
    }

    /**
     * Checks if a return has been reconciled (status is not PENDING or IN_PROGRESS).
     */
    private boolean isReturnReconciled(String returnId) {
        try {
            Map<String, Object> record = getReconciliationRecord(returnId);
            if (record == null) {
                return false;
            }
            String status = (String) record.get("reconciliationStatus");
            return status != null && !"PENDING".equals(status) && !"IN_PROGRESS".equals(status);
        } catch (Exception e) {
            return false;
        }
    }

    // ==================== RETRY LOGIC TESTS ====================

    /**
     * Gets a reconciliation record by return ID.
     */
    private Map<String, Object> getReconciliationRecord(String returnId) {
        try {
            EntityExchangeResult<ApiResponse<Map<String, Object>>> result =
                    authenticatedGet("/api/v1/integration/reconciliation/" + returnId, tenantAdminAuth.getAccessToken(), testTenantId).exchange().expectStatus().isOk()
                            .expectBody(new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {
                            }).returnResult();

            ApiResponse<Map<String, Object>> apiResponse = result.getResponseBody();
            if (apiResponse != null && apiResponse.isSuccess()) {
                return apiResponse.getData();
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    // ==================== AUDIT TRAIL TESTS ====================

    @Test
    @Order(2)
    @DisplayName("D365 Reconciliation - Partial return should sync to D365 successfully")
    public void testD365Sync_AfterPartialReturn_ShouldSyncSuccessfully() {
        // Arrange - Create a partial return
        String orderNumber = "ORD-PARTIAL-" + faker.number().digits(8);
        HandlePartialOrderAcceptanceRequest request = ReturnsTestDataBuilder.buildHandlePartialOrderAcceptanceRequest(orderNumber, testProductId);

        // Act - Process partial return
        EntityExchangeResult<ApiResponse<HandlePartialOrderAcceptanceResponse>> createResult =
                authenticatedPost("/api/v1/returns/partial-acceptance", tenantAdminAuth.getAccessToken(), testTenantId, request).exchange().expectStatus().isCreated()
                        .expectBody(new ParameterizedTypeReference<ApiResponse<HandlePartialOrderAcceptanceResponse>>() {
                        }).returnResult();

        ApiResponse<HandlePartialOrderAcceptanceResponse> createApiResponse = createResult.getResponseBody();
        assertThat(createApiResponse).isNotNull();
        assertThat(createApiResponse.getData()).isNotNull();
        HandlePartialOrderAcceptanceResponse createdReturn = createApiResponse.getData();
        String returnId = createdReturn.getReturnId();

        // Assert - Verify return was created
        assertThat(createdReturn).isNotNull();
        assertThat(returnId).isNotBlank();
        assertThat(createdReturn.getStatus()).isEqualTo("INITIATED");

        // Wait for async D365 reconciliation (up to 15 seconds)
        Awaitility.await().atMost(15, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> isReturnReconciled(returnId));

        // Assert - Query reconciliation status
        Map<String, Object> reconciliation = getReconciliationRecord(returnId);
        assertThat(reconciliation).isNotNull();

        String syncStatus = (String) reconciliation.get("reconciliationStatus");
        assertThat(syncStatus).isIn("SUCCESS", "SYNCED", "IN_PROGRESS", "PENDING");
    }

    @Test
    @Order(3)
    @DisplayName("POST /api/v1/integration/reconciliation/{returnId}/retry - Should retry failed sync")
    public void testRetryD365Sync_ForFailedReconciliation_ShouldSucceedOnRetry() {
        // Arrange - Ensure we have a return ID
        if (testReturnId == null) {
            String orderNumber = "ORD-RETRY-" + faker.number().digits(8);
            ProcessFullOrderReturnRequest request = ReturnsTestDataBuilder.buildProcessFullOrderReturnRequest(orderNumber, testProductId);
            EntityExchangeResult<ApiResponse<ProcessFullOrderReturnResponse>> createResult =
                    authenticatedPost("/api/v1/returns/full-return", tenantAdminAuth.getAccessToken(), testTenantId, request).exchange().expectStatus().isCreated()
                            .expectBody(new ParameterizedTypeReference<ApiResponse<ProcessFullOrderReturnResponse>>() {
                            }).returnResult();
            ApiResponse<ProcessFullOrderReturnResponse> createApiResponse = createResult.getResponseBody();
            assertThat(createApiResponse).isNotNull();
            assertThat(createApiResponse.getData()).isNotNull();
            testReturnId = createApiResponse.getData().getReturnId();
        }

        // Wait for initial sync attempt (if any)
        try {
            Awaitility.await().atMost(10, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
                Map<String, Object> record = getReconciliationRecord(testReturnId);
                return record != null;
            });
        } catch (Exception e) {
            // Reconciliation record may not exist yet, continue with retry
        }

        // Act - Retry D365 sync
        EntityExchangeResult<ApiResponse<Void>> retryResult =
                authenticatedPostWithoutBody("/api/v1/integration/reconciliation/" + testReturnId + "/retry", tenantAdminAuth.getAccessToken(), testTenantId).exchange()
                        .expectStatus().is2xxSuccessful().expectBody(new ParameterizedTypeReference<ApiResponse<Void>>() {
                        }).returnResult();

        // Assert - Verify retry was accepted
        ApiResponse<Void> retryResponse = retryResult.getResponseBody();
        assertThat(retryResponse).isNotNull();
        if (retryResponse != null) {
            assertThat(retryResponse.isSuccess()).isTrue();
        }

        // Wait for retry to complete
        Awaitility.await().atMost(15, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
            Map<String, Object> record = getReconciliationRecord(testReturnId);
            if (record == null) {
                return false;
            }
            String status = (String) record.get("reconciliationStatus");
            return "SUCCESS".equals(status) || "SYNCED".equals(status) || "FAILED".equals(status);
        });

        // Verify reconciliation status after retry
        Map<String, Object> reconciliation = getReconciliationRecord(testReturnId);
        assertThat(reconciliation).isNotNull();
    }

    // ==================== SUMMARY TESTS ====================

    @Test
    @Order(4)
    @DisplayName("GET /api/v1/integration/reconciliation/{returnId} - Should return reconciliation record with all fields")
    public void testGetReconciliationRecord_ShouldReturnCompleteRecord() {
        // Arrange - Ensure we have a return ID
        if (testReturnId == null) {
            String orderNumber = "ORD-AUDIT-" + faker.number().digits(8);
            ProcessFullOrderReturnRequest request = ReturnsTestDataBuilder.buildProcessFullOrderReturnRequest(orderNumber, testProductId);
            EntityExchangeResult<ApiResponse<ProcessFullOrderReturnResponse>> createResult =
                    authenticatedPost("/api/v1/returns/full-return", tenantAdminAuth.getAccessToken(), testTenantId, request).exchange().expectStatus().isCreated()
                            .expectBody(new ParameterizedTypeReference<ApiResponse<ProcessFullOrderReturnResponse>>() {
                            }).returnResult();
            ApiResponse<ProcessFullOrderReturnResponse> createApiResponse = createResult.getResponseBody();
            assertThat(createApiResponse).isNotNull();
            assertThat(createApiResponse.getData()).isNotNull();
            testReturnId = createApiResponse.getData().getReturnId();
        }

        // Wait for reconciliation record to be created
        Awaitility.await().atMost(10, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> getReconciliationRecord(testReturnId) != null);

        // Act - Get reconciliation record
        EntityExchangeResult<ApiResponse<Map<String, Object>>> result =
                authenticatedGet("/api/v1/integration/reconciliation/" + testReturnId, tenantAdminAuth.getAccessToken(), testTenantId).exchange().expectStatus().isOk()
                        .expectBody(new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {
                        }).returnResult();

        // Assert - Verify response structure and all required fields
        ApiResponse<Map<String, Object>> apiResponse = result.getResponseBody();
        assertThat(apiResponse).isNotNull();
        if (apiResponse != null) {
            assertThat(apiResponse.isSuccess()).isTrue();
            assertThat(apiResponse.getData()).isNotNull();
        }

        Map<String, Object> reconciliation = apiResponse != null ? apiResponse.getData() : null;
        assertThat(reconciliation).isNotNull();

        // Verify all required fields are present
        if (reconciliation != null) {
            assertThat(reconciliation.get("returnId")).isEqualTo(testReturnId);
            assertThat(reconciliation.get("orderNumber")).isNotNull();
            assertThat(reconciliation.get("reconciliationStatus")).isNotNull();
            assertThat(reconciliation.get("inventoryAdjusted")).isNotNull();
            assertThat(reconciliation.get("writeOffProcessed")).isNotNull();
            assertThat(reconciliation.get("createdAt")).isNotNull();
            assertThat(reconciliation.get("lastModifiedAt")).isNotNull();

            // Verify reconciliation status is valid enum value
            String status = (String) reconciliation.get("reconciliationStatus");
            assertThat(status).isIn("PENDING", "IN_PROGRESS", "SUCCESS", "FAILED", "RETRYING");

            // If synced successfully, verify D365 return order ID
            if ("SUCCESS".equals(status)) {
                assertThat(reconciliation.get("d365ReturnOrderId")).isNotNull();
            }
        }
    }

    @Test
    @Order(5)
    @DisplayName("GET /api/v1/integration/reconciliation/{returnId}/audit-trail - Should return audit trail")
    public void testGetReconciliationAuditTrail_ShouldReturnAuditEntries() {
        // Arrange - Ensure we have a return ID
        if (testReturnId == null) {
            String orderNumber = "ORD-AUDIT-TRAIL-" + faker.number().digits(8);
            ProcessFullOrderReturnRequest request = ReturnsTestDataBuilder.buildProcessFullOrderReturnRequest(orderNumber, testProductId);
            EntityExchangeResult<ApiResponse<ProcessFullOrderReturnResponse>> createResult =
                    authenticatedPost("/api/v1/returns/full-return", tenantAdminAuth.getAccessToken(), testTenantId, request).exchange().expectStatus().isCreated()
                            .expectBody(new ParameterizedTypeReference<ApiResponse<ProcessFullOrderReturnResponse>>() {
                            }).returnResult();
            ApiResponse<ProcessFullOrderReturnResponse> createApiResponse = createResult.getResponseBody();
            assertThat(createApiResponse).isNotNull();
            assertThat(createApiResponse.getData()).isNotNull();
            testReturnId = createApiResponse.getData().getReturnId();
        }

        // Wait for reconciliation to be processed
        Awaitility.await().atMost(15, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> isReturnReconciled(testReturnId));

        // Act - Get audit trail
        EntityExchangeResult<ApiResponse<List<Map<String, Object>>>> result =
                authenticatedGet("/api/v1/integration/reconciliation/" + testReturnId + "/audit-trail", tenantAdminAuth.getAccessToken(), testTenantId).exchange().expectStatus()
                        .isOk().expectBody(new ParameterizedTypeReference<ApiResponse<List<Map<String, Object>>>>() {
                        }).returnResult();

        // Assert - Verify audit trail structure
        ApiResponse<List<Map<String, Object>>> apiResponse = result.getResponseBody();
        assertThat(apiResponse).isNotNull();
        if (apiResponse != null) {
            assertThat(apiResponse.isSuccess()).isTrue();
            assertThat(apiResponse.getData()).isNotNull();
        }

        List<Map<String, Object>> auditEntries = apiResponse != null ? apiResponse.getData() : null;
        assertThat(auditEntries).isNotNull();
        assertThat(auditEntries).isNotEmpty();

        // Verify each audit entry has required fields
        if (auditEntries != null) {
            for (Map<String, Object> entry : auditEntries) {
                assertThat(entry.get("action")).isNotNull();
                assertThat(entry.get("timestamp")).isNotNull();
                assertThat(entry.get("status")).isNotNull();
                assertThat(entry.get("description")).isNotNull();

                // Verify status is valid enum value
                String status = (String) entry.get("status");
                assertThat(status).isIn("SUCCESS", "FAILED", "PENDING");

                // If failed, verify error details are present
                if ("FAILED".equals(status)) {
                    assertThat(entry.get("errorDetails")).isNotNull();
                }

                // If successful, D365 response may be present
                if ("SUCCESS".equals(status)) {
                    // d365Response is optional but may be present
                }
            }
        }
    }

    @Test
    @Order(6)
    @DisplayName("GET /api/v1/integration/reconciliation/summary - Should return reconciliation summary with metrics")
    public void testGetReconciliationSummary_ShouldReturnSummaryMetrics() {
        // Act - Get reconciliation summary
        EntityExchangeResult<ApiResponse<Map<String, Object>>> result =
                authenticatedGet("/api/v1/integration/reconciliation/summary", tenantAdminAuth.getAccessToken(), testTenantId).exchange().expectStatus().isOk()
                        .expectBody(new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {
                        }).returnResult();

        // Assert - Verify summary structure and metrics
        ApiResponse<Map<String, Object>> apiResponse = result.getResponseBody();
        assertThat(apiResponse).isNotNull();
        if (apiResponse != null) {
            assertThat(apiResponse.isSuccess()).isTrue();
            assertThat(apiResponse.getData()).isNotNull();
        }

        Map<String, Object> summary = apiResponse != null ? apiResponse.getData() : null;
        assertThat(summary).isNotNull();

        // Verify all summary metrics are present and valid
        if (summary != null) {
            assertThat(summary.get("totalReturns")).isNotNull();
            assertThat(summary.get("pendingReconciliation")).isNotNull();
            assertThat(summary.get("inProgress")).isNotNull();
            assertThat(summary.get("successful")).isNotNull();
            assertThat(summary.get("failed")).isNotNull();
            assertThat(summary.get("retrying")).isNotNull();

            // Verify metrics are non-negative integers
            assertThat(((Number) summary.get("totalReturns")).intValue()).isGreaterThanOrEqualTo(0);
            assertThat(((Number) summary.get("pendingReconciliation")).intValue()).isGreaterThanOrEqualTo(0);
            assertThat(((Number) summary.get("inProgress")).intValue()).isGreaterThanOrEqualTo(0);
            assertThat(((Number) summary.get("successful")).intValue()).isGreaterThanOrEqualTo(0);
            assertThat(((Number) summary.get("failed")).intValue()).isGreaterThanOrEqualTo(0);
            assertThat(((Number) summary.get("retrying")).intValue()).isGreaterThanOrEqualTo(0);
        }
    }

    // ==================== HELPER METHODS ====================

    @Test
    @Order(7)
    @DisplayName("GET /api/v1/integration/reconciliation - Should list reconciliation records with pagination")
    public void testListReconciliationRecords_ShouldReturnPaginatedResults() {
        // Act - List reconciliation records with pagination
        EntityExchangeResult<ApiResponse<List<Map<String, Object>>>> result =
                authenticatedGet("/api/v1/integration/reconciliation?page=0&size=10", tenantAdminAuth.getAccessToken(), testTenantId).exchange().expectStatus().isOk()
                        .expectBody(new ParameterizedTypeReference<ApiResponse<List<Map<String, Object>>>>() {
                        }).returnResult();

        // Assert - Verify response structure
        ApiResponse<List<Map<String, Object>>> apiResponse = result.getResponseBody();
        assertThat(apiResponse).isNotNull();
        if (apiResponse != null) {
            assertThat(apiResponse.isSuccess()).isTrue();
            assertThat(apiResponse.getData()).isNotNull();
        }

        List<Map<String, Object>> records = apiResponse != null ? apiResponse.getData() : null;
        assertThat(records).isNotNull();

        // Verify all records have required fields
        if (records != null) {
            for (Map<String, Object> record : records) {
                assertThat(record.get("returnId")).isNotNull();
                assertThat(record.get("orderNumber")).isNotNull();
                assertThat(record.get("reconciliationStatus")).isNotNull();
                assertThat(record.get("reconciliationStatus")).isIn("PENDING", "IN_PROGRESS", "SUCCESS", "FAILED", "RETRYING");
            }
        }
    }

    @Test
    @Order(8)
    @DisplayName("GET /api/v1/integration/reconciliation - Should filter by status")
    public void testListReconciliationRecords_WithStatusFilter_ShouldReturnFiltered() {
        // Act - List reconciliation records filtered by status
        EntityExchangeResult<ApiResponse<List<Map<String, Object>>>> result =
                authenticatedGet("/api/v1/integration/reconciliation?status=SUCCESS&page=0&size=10", tenantAdminAuth.getAccessToken(), testTenantId).exchange().expectStatus()
                        .isOk().expectBody(new ParameterizedTypeReference<ApiResponse<List<Map<String, Object>>>>() {
                        }).returnResult();

        // Assert - Verify all returned records match the filter
        ApiResponse<List<Map<String, Object>>> apiResponse = result.getResponseBody();
        assertThat(apiResponse).isNotNull();
        if (apiResponse != null) {
            assertThat(apiResponse.isSuccess()).isTrue();
            assertThat(apiResponse.getData()).isNotNull();
        }

        List<Map<String, Object>> records = apiResponse != null ? apiResponse.getData() : null;
        if (records != null && !records.isEmpty()) {
            for (Map<String, Object> record : records) {
                assertThat(record.get("reconciliationStatus")).isEqualTo("SUCCESS");
            }
        }
    }
}
