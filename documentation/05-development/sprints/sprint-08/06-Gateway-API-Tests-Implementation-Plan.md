# Gateway API Tests Implementation Plan

## Sprint 8: Reconciliation - Gateway API Tests

**Module:** gateway-api-tests
**Sprint:** Sprint 8
**Coverage:** All 5 user stories + complete workflows

---

## Table of Contents

1. [Overview](#overview)
2. [Test Structure](#test-structure)
3. [Test Classes](#test-classes)
4. [Test Data Builders](#test-data-builders)
5. [Test Scenarios](#test-scenarios)
6. [Implementation Checklist](#implementation-checklist)

---

## Overview

### Purpose

Validate all Sprint 8 reconciliation functionality through comprehensive end-to-end gateway API tests, ensuring:

- All endpoints function correctly with authentication and multi-tenant isolation
- Domain events are published correctly
- Business validation rules are enforced
- Error scenarios are handled properly
- Async operations (D365 reconciliation) complete successfully

### Test Coverage

- **US-8.1.1**: Generate Electronic Stock Count Worksheet
- **US-8.1.2**: Perform Stock Count Entry
- **US-8.1.3**: Complete Stock Count
- **US-8.2.1**: Investigate Stock Count Variances
- **US-8.3.1**: Reconcile Stock Counts with D365

### Test Infrastructure

- **Base Class**: `BaseIntegrationTest`
- **Authentication**: Bearer token with `X-Tenant-ID` header
- **Test Containers**: PostgreSQL, Kafka
- **Async Validation**: Polling mechanisms for event-driven operations
- **Test Data**: Builders for repeatable test data creation

---

## Test Structure

### Directory Organization

```
gateway-api-tests/src/test/java/com/ccbsa/wms/gateway/api/
├── StockCountTest.java
├── VarianceInvestigationTest.java
├── D365ReconciliationTest.java
├── ReconciliationWorkflowTest.java (End-to-end)
├── fixture/
│   ├── StockCountTestDataBuilder.java
│   ├── VarianceTestDataBuilder.java
│   ├── TestDataManager.java
│   └── repository/
│       ├── StockCountTestRepository.java
│       └── VarianceTestRepository.java
└── util/
    ├── AsyncValidator.java
    └── EventValidator.java
```

---

## Test Classes

### 1. StockCountTest.java

**Responsibility**: Test stock count creation, entry recording, and completion

```java
package com.ccbsa.wms.gateway.api;

import com.ccbsa.wms.gateway.api.dto.*;
import com.ccbsa.wms.gateway.api.fixture.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class StockCountTest extends BaseIntegrationTest {

    @Autowired
    private StockCountTestDataBuilder stockCountBuilder;

    @Autowired
    private TestDataManager testDataManager;

    private String testTenantId;
    private String authToken;

    @BeforeEach
    void setUp() {
        testTenantId = testDataManager.createTestTenant();
        authToken = testDataManager.getAuthToken("warehouse_manager");
        testDataManager.createTestProducts(testTenantId);
        testDataManager.createTestLocations(testTenantId);
    }

    @Test
    @Order(1)
    @DisplayName("US-8.1.1: Generate Stock Count Worksheet - Success")
    void generateStockCountWorksheet_WithValidRequest_ShouldReturn201() {
        // Arrange
        GenerateStockCountWorksheetRequest request = stockCountBuilder
            .buildGenerateWorksheetRequest(
                "CYCLE_COUNT",
                List.of("ZONE-A", "ZONE-B"),
                null // No product filter
            );

        // Act & Assert
        webTestClient.post()
            .uri("/api/reconciliation/stock-counts/generate-worksheet")
            .header("X-Tenant-ID", testTenantId)
            .header("Authorization", "Bearer " + authToken)
            .bodyValue(request)
            .exchange()
            .expectStatus().isCreated()
            .expectBody(GenerateStockCountWorksheetResponse.class)
            .value(response -> {
                assertThat(response.getStockCountId()).isNotBlank();
                assertThat(response.getCountReference()).startsWith("SC-");
                assertThat(response.getStatus()).isEqualTo("DRAFT");
                assertThat(response.getTotalWorksheetEntries()).isGreaterThan(0);
                assertThat(response.getWorksheetEntries()).isNotEmpty();

                // Validate worksheet entries
                response.getWorksheetEntries().forEach(entry -> {
                    assertThat(entry.getLocationCode()).isNotBlank();
                    assertThat(entry.getProductCode()).isNotBlank();
                    assertThat(entry.getSystemQuantity()).isGreaterThanOrEqualTo(BigDecimal.ZERO);
                });
            });
    }

    @Test
    @Order(2)
    @DisplayName("US-8.1.2: Record Stock Count Entry - Success")
    void recordStockCountEntry_WithValidData_ShouldReturn201() {
        // Arrange
        String stockCountId = stockCountBuilder.createInProgressStockCount(testTenantId);
        RecordStockCountEntryRequest request = stockCountBuilder
            .buildRecordEntryRequest("LOC-001", "PROD-001", BigDecimal.valueOf(50));

        // Act & Assert
        webTestClient.post()
            .uri("/api/reconciliation/stock-counts/{id}/entries", stockCountId)
            .header("X-Tenant-ID", testTenantId)
            .header("Authorization", "Bearer " + authToken)
            .bodyValue(request)
            .exchange()
            .expectStatus().isCreated()
            .expectBody(RecordStockCountEntryResponse.class)
            .value(response -> {
                assertThat(response.getEntryId()).isNotBlank();
                assertThat(response.getStockCountId()).isEqualTo(stockCountId);
                assertThat(response.getLocationCode()).isEqualTo("LOC-001");
                assertThat(response.getProductCode()).isEqualTo("PROD-001");
                assertThat(response.getCountedQuantity()).isEqualByComparingTo(BigDecimal.valueOf(50));
                assertThat(response.getRecordedAt()).isNotNull();
            });

        // Validate event published
        eventValidator.assertEventPublished(
            "reconciliation.stock-count.entry-recorded",
            event -> event.get("stockCountId").equals(stockCountId)
        );
    }

    @Test
    @Order(3)
    @DisplayName("US-8.1.2: Record Duplicate Entry - Should Return 409")
    void recordStockCountEntry_DuplicateLocationProduct_ShouldReturn409() {
        // Arrange
        String stockCountId = stockCountBuilder.createInProgressStockCount(testTenantId);
        RecordStockCountEntryRequest request = stockCountBuilder
            .buildRecordEntryRequest("LOC-001", "PROD-001", BigDecimal.valueOf(50));

        // Record first entry
        webTestClient.post()
            .uri("/api/reconciliation/stock-counts/{id}/entries", stockCountId)
            .header("X-Tenant-ID", testTenantId)
            .header("Authorization", "Bearer " + authToken)
            .bodyValue(request)
            .exchange()
            .expectStatus().isCreated();

        // Act & Assert - Attempt duplicate
        webTestClient.post()
            .uri("/api/reconciliation/stock-counts/{id}/entries", stockCountId)
            .header("X-Tenant-ID", testTenantId)
            .header("Authorization", "Bearer " + authToken)
            .bodyValue(request)
            .exchange()
            .expectStatus().isEqualTo(409)
            .expectBody()
            .jsonPath("$.message").value(msg ->
                assertThat(msg.toString()).contains("duplicate")
            );
    }

    @Test
    @Order(4)
    @DisplayName("US-8.1.3: Complete Stock Count - Success with Variances")
    void completeStockCount_WithVariances_ShouldReturn200AndCalculateVariances() {
        // Arrange
        String stockCountId = stockCountBuilder.createStockCountWithEntries(
            testTenantId,
            List.of(
                new EntryData("LOC-001", "PROD-001", BigDecimal.valueOf(100), BigDecimal.valueOf(95)), // -5 variance
                new EntryData("LOC-002", "PROD-002", BigDecimal.valueOf(50), BigDecimal.valueOf(55))  // +5 variance
            )
        );

        CompleteStockCountRequest request = CompleteStockCountRequest.builder()
            .acknowledgeVariances(true)
            .completionNotes("Test completion")
            .build();

        // Act & Assert
        webTestClient.post()
            .uri("/api/reconciliation/stock-counts/{id}/complete", stockCountId)
            .header("X-Tenant-ID", testTenantId)
            .header("Authorization", "Bearer " + authToken)
            .bodyValue(request)
            .exchange()
            .expectStatus().isOk()
            .expectBody(CompleteStockCountResponse.class)
            .value(response -> {
                assertThat(response.getStockCountId()).isEqualTo(stockCountId);
                assertThat(response.getStatus()).isEqualTo("COMPLETED");
                assertThat(response.getTotalVariances()).isEqualTo(2);

                VarianceSummary summary = response.getVarianceSummary();
                assertThat(summary.getTotalVariances()).isEqualTo(2);
                assertThat(summary.getCriticalCount()).isEqualTo(0);
                assertThat(summary.getHighCount()).isEqualTo(0);
                assertThat(summary.getMediumCount()).isGreaterThanOrEqualTo(0);
                assertThat(summary.getLowCount()).isGreaterThanOrEqualTo(0);
            });

        // Validate events
        eventValidator.assertEventPublished(
            "reconciliation.stock-count.completed",
            event -> event.get("stockCountId").equals(stockCountId)
        );
    }

    @Test
    @Order(5)
    @DisplayName("US-8.1.3: Complete with Critical Variance - Should Return 400")
    void completeStockCount_WithUnresolvedCriticalVariance_ShouldReturn400() {
        // Arrange - Create count with critical variance (>20% or >R1000)
        String stockCountId = stockCountBuilder.createStockCountWithEntries(
            testTenantId,
            List.of(
                new EntryData("LOC-001", "PROD-001", BigDecimal.valueOf(1000), BigDecimal.valueOf(500)) // 50% variance
            )
        );

        CompleteStockCountRequest request = CompleteStockCountRequest.builder()
            .acknowledgeVariances(true)
            .build();

        // Act & Assert
        webTestClient.post()
            .uri("/api/reconciliation/stock-counts/{id}/complete", stockCountId)
            .header("X-Tenant-ID", testTenantId)
            .header("Authorization", "Bearer " + authToken)
            .bodyValue(request)
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody()
            .jsonPath("$.message").value(msg ->
                assertThat(msg.toString()).contains("critical variance")
            );
    }

    @Test
    @Order(6)
    @DisplayName("Query Stock Counts - With Filters")
    void listStockCounts_WithFilters_ShouldReturnFilteredResults() {
        // Arrange
        stockCountBuilder.createMultipleStockCounts(testTenantId, 5);

        // Act & Assert
        webTestClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/api/reconciliation/stock-counts")
                .queryParam("status", "COMPLETED")
                .queryParam("page", 0)
                .queryParam("size", 10)
                .build())
            .header("X-Tenant-ID", testTenantId)
            .header("Authorization", "Bearer " + authToken)
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.content").isArray()
            .jsonPath("$.totalElements").isNumber()
            .jsonPath("$.content[0].status").isEqualTo("COMPLETED");
    }
}
```

---

### 2. VarianceInvestigationTest.java

**Responsibility**: Test variance investigation and resolution workflows

```java
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class VarianceInvestigationTest extends BaseIntegrationTest {

    @Autowired
    private VarianceTestDataBuilder varianceBuilder;

    @Test
    @Order(1)
    @DisplayName("US-8.2.1: Investigate Variance - Success")
    void investigateVariance_WithValidData_ShouldReturn200() {
        // Arrange
        String varianceId = varianceBuilder.createPendingVariance(testTenantId, "MEDIUM");
        InvestigateVarianceRequest request = InvestigateVarianceRequest.builder()
            .varianceReason("COUNTING_ERROR")
            .investigationNotes("Operator miscounted items during busy period")
            .rootCauseCategory("Process Breakdown")
            .contributingFactors(List.of("Time Pressure", "Inadequate Training"))
            .preventativeActions("Provide additional training on counting procedures")
            .build();

        // Act & Assert
        webTestClient.post()
            .uri("/api/reconciliation/variances/{id}/investigate", varianceId)
            .header("X-Tenant-ID", testTenantId)
            .header("Authorization", "Bearer " + authToken)
            .bodyValue(request)
            .exchange()
            .expectStatus().isOk()
            .expectBody(InvestigateVarianceResponse.class)
            .value(response -> {
                assertThat(response.getVarianceId()).isEqualTo(varianceId);
                assertThat(response.getInvestigationStatus()).isEqualTo("IN_PROGRESS");
                assertThat(response.getVarianceReason()).isEqualTo("COUNTING_ERROR");
                assertThat(response.getInvestigatedBy()).isNotBlank();
                assertThat(response.getInvestigatedAt()).isNotNull();
            });

        // Validate event
        eventValidator.assertEventPublished(
            "reconciliation.variance.investigated",
            event -> event.get("varianceId").equals(varianceId)
        );
    }

    @Test
    @Order(2)
    @DisplayName("US-8.2.1: Resolve Low Severity Variance - Auto Approve")
    void resolveVariance_LowSeverity_ShouldAutoApprove() {
        // Arrange
        String varianceId = varianceBuilder.createInvestigatedVariance(testTenantId, "LOW");
        ResolveVarianceRequest request = ResolveVarianceRequest.builder()
            .resolutionNotes("Counting error confirmed. System quantity is correct.")
            .build();

        // Act & Assert
        webTestClient.post()
            .uri("/api/reconciliation/variances/{id}/resolve", varianceId)
            .header("X-Tenant-ID", testTenantId)
            .header("Authorization", "Bearer " + authToken)
            .bodyValue(request)
            .exchange()
            .expectStatus().isOk()
            .expectBody(ResolveVarianceResponse.class)
            .value(response -> {
                assertThat(response.getVarianceId()).isEqualTo(varianceId);
                assertThat(response.getInvestigationStatus()).isEqualTo("RESOLVED");
                assertThat(response.getResolvedAt()).isNotNull();
            });

        // Validate event
        eventValidator.assertEventPublished(
            "reconciliation.variance.resolved",
            event -> event.get("varianceId").equals(varianceId)
        );
    }

    @Test
    @Order(3)
    @DisplayName("US-8.2.1: Request Approval for High Severity Variance")
    void resolveVariance_HighSeverity_ShouldRequireApproval() {
        // Arrange
        String varianceId = varianceBuilder.createInvestigatedVariance(testTenantId, "HIGH");
        ResolveVarianceRequest request = ResolveVarianceRequest.builder()
            .resolutionNotes("Significant variance due to theft")
            .requestApproval(true)
            .build();

        // Act & Assert
        webTestClient.post()
            .uri("/api/reconciliation/variances/{id}/resolve", varianceId)
            .header("X-Tenant-ID", testTenantId)
            .header("Authorization", "Bearer " + authToken)
            .bodyValue(request)
            .exchange()
            .expectStatus().isOk()
            .expectBody(ResolveVarianceResponse.class)
            .value(response -> {
                assertThat(response.getVarianceId()).isEqualTo(varianceId);
                assertThat(response.getInvestigationStatus()).isEqualTo("REQUIRES_APPROVAL");
                assertThat(response.isRequiresApproval()).isTrue();
            });

        // Validate approval request event
        eventValidator.assertEventPublished(
            "reconciliation.variance.approval-requested",
            event -> event.get("varianceId").equals(varianceId)
        );
    }

    @Test
    @Order(4)
    @DisplayName("US-8.2.1: Manager Approves Variance")
    void approveVariance_AsManager_ShouldApprove() {
        // Arrange
        String varianceId = varianceBuilder.createVarianceAwaitingApproval(testTenantId);
        String managerToken = testDataManager.getAuthToken("warehouse_manager");
        ApproveVarianceRequest request = ApproveVarianceRequest.builder()
            .approvalDecision("APPROVED")
            .approverComments("Approved after reviewing investigation details")
            .build();

        // Act & Assert
        webTestClient.post()
            .uri("/api/reconciliation/variances/{id}/approve", varianceId)
            .header("X-Tenant-ID", testTenantId)
            .header("Authorization", "Bearer " + managerToken)
            .bodyValue(request)
            .exchange()
            .expectStatus().isOk()
            .expectBody(ApproveVarianceResponse.class)
            .value(response -> {
                assertThat(response.getVarianceId()).isEqualTo(varianceId);
                assertThat(response.getApprovalStatus()).isEqualTo("APPROVED");
                assertThat(response.getApprovedBy()).isNotBlank();
                assertThat(response.getApprovedAt()).isNotNull();
            });

        // Validate event
        eventValidator.assertEventPublished(
            "reconciliation.variance.approved",
            event -> event.get("varianceId").equals(varianceId)
        );
    }

    @Test
    @Order(5)
    @DisplayName("US-8.2.1: Get Stock Movement History for Variance")
    void getStockMovementHistory_ForVariance_ShouldReturnHistory() {
        // Arrange
        String varianceId = varianceBuilder.createVarianceWithMovementHistory(testTenantId);

        // Act & Assert
        webTestClient.get()
            .uri("/api/reconciliation/variances/{id}/movement-history", varianceId)
            .header("X-Tenant-ID", testTenantId)
            .header("Authorization", "Bearer " + authToken)
            .exchange()
            .expectStatus().isOk()
            .expectBody(StockMovementHistoryResponse.class)
            .value(response -> {
                assertThat(response.getProductId()).isNotBlank();
                assertThat(response.getLocationId()).isNotBlank();
                assertThat(response.getMovements()).isNotEmpty();

                // Validate movements have required fields
                response.getMovements().forEach(movement -> {
                    assertThat(movement.getMovementType()).isNotBlank();
                    assertThat(movement.getQuantity()).isNotNull();
                    assertThat(movement.getTimestamp()).isNotNull();
                    assertThat(movement.getPerformedBy()).isNotBlank();
                });
            });
    }
}
```

---

### 3. D365ReconciliationTest.java

**Responsibility**: Test D365 reconciliation workflows (optional integration)

```java
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@EnabledIfEnvironmentVariable(named = "D365_RECONCILIATION_ENABLED", matches = "true")
public class D365ReconciliationTest extends BaseIntegrationTest {

    @Autowired
    private AsyncValidator asyncValidator;

    @Test
    @Order(1)
    @DisplayName("US-8.3.1: D365 Reconciliation - Success")
    void reconcileStockCount_WithD365_ShouldSyncSuccessfully() {
        // Arrange
        String stockCountId = stockCountBuilder.createCompletedStockCount(testTenantId);

        // Wait for async reconciliation
        asyncValidator.waitForCondition(
            () -> d365ReconciliationExists(stockCountId),
            30000, // 30 seconds timeout
            "D365 reconciliation should be initiated"
        );

        // Act & Assert - Query reconciliation status
        webTestClient.get()
            .uri("/api/reconciliation/d365-reconciliation/by-stock-count/{stockCountId}", stockCountId)
            .header("X-Tenant-ID", testTenantId)
            .header("Authorization", "Bearer " + authToken)
            .exchange()
            .expectStatus().isOk()
            .expectBody(D365ReconciliationRecordResponse.class)
            .value(response -> {
                assertThat(response.getStockCountId()).isEqualTo(stockCountId);
                assertThat(response.getReconciliationStatus()).isIn("SYNCED", "IN_PROGRESS");

                if (response.getReconciliationStatus().equals("SYNCED")) {
                    assertThat(response.getD365JournalId()).isNotBlank();
                    assertThat(response.getD365JournalNumber()).isNotBlank();
                }
            });
    }

    @Test
    @Order(2)
    @DisplayName("US-8.3.1: D365 Reconciliation - Retry Failed")
    void retryFailedReconciliation_ShouldInitiateRetry() {
        // Arrange
        String reconciliationId = d365Builder.createFailedReconciliation(testTenantId);

        // Act & Assert
        webTestClient.post()
            .uri("/api/reconciliation/d365-reconciliation/{id}/retry", reconciliationId)
            .header("X-Tenant-ID", testTenantId)
            .header("Authorization", "Bearer " + authToken)
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.message").value(msg ->
                assertThat(msg.toString()).contains("retry initiated")
            );

        // Validate reconciliation status updated
        asyncValidator.waitForCondition(
            () -> {
                D365ReconciliationRecordResponse record = getReconciliationRecord(reconciliationId);
                return record.getReconciliationStatus().equals("PENDING") ||
                       record.getReconciliationStatus().equals("IN_PROGRESS");
            },
            10000,
            "Reconciliation should be reset to pending"
        );
    }

    @Test
    @Order(3)
    @DisplayName("US-8.3.1: List D365 Reconciliation Records - With Filters")
    void listD365ReconciliationRecords_WithFilters_ShouldReturnResults() {
        // Arrange
        d365Builder.createMultipleReconciliations(testTenantId, 10);

        // Act & Assert
        webTestClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/api/reconciliation/d365-reconciliation")
                .queryParam("status", "SYNCED")
                .queryParam("page", 0)
                .queryParam("size", 20)
                .build())
            .header("X-Tenant-ID", testTenantId)
            .header("Authorization", "Bearer " + authToken)
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.content").isArray()
            .jsonPath("$.content[0].reconciliationStatus").isEqualTo("SYNCED");
    }

    @Test
    @Order(4)
    @DisplayName("US-8.3.1: Get D365 Audit Trail")
    void getD365AuditTrail_ForReconciliation_ShouldReturnAllAttempts() {
        // Arrange
        String reconciliationId = d365Builder.createReconciliationWithRetries(testTenantId, 3);

        // Act & Assert
        webTestClient.get()
            .uri("/api/reconciliation/d365-reconciliation/{id}/audit-trail", reconciliationId)
            .header("X-Tenant-ID", testTenantId)
            .header("Authorization", "Bearer " + authToken)
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.auditEntries").isArray()
            .jsonPath("$.auditEntries.length()").isEqualTo(3)
            .jsonPath("$.auditEntries[0].attemptNumber").isEqualTo(1)
            .jsonPath("$.auditEntries[0].requestPayload").exists()
            .jsonPath("$.auditEntries[0].attemptTimestamp").exists();
    }
}
```

---

### 4. ReconciliationWorkflowTest.java

**End-to-end workflow tests**

```java
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ReconciliationWorkflowTest extends BaseIntegrationTest {

    @Test
    @DisplayName("Complete Stock Count Workflow - Generate → Entry → Complete → Investigate")
    void completeStockCountWorkflow_EndToEnd_ShouldExecuteSuccessfully() {
        // Step 1: Generate Worksheet
        String stockCountId = generateWorksheet();
        assertThat(stockCountId).isNotBlank();

        // Step 2: Record Entries
        recordEntries(stockCountId, 5);

        // Step 3: Complete Count
        CompleteStockCountResponse completionResponse = completeStockCount(stockCountId);
        assertThat(completionResponse.getStatus()).isEqualTo("COMPLETED");

        // Step 4: Investigate Variances (if any)
        if (completionResponse.getTotalVariances() > 0) {
            String varianceId = getFirstVarianceId(stockCountId);
            investigateVariance(varianceId);
            resolveVariance(varianceId);
        }

        // Validate all events published
        eventValidator.assertEventsPublishedInOrder(
            "reconciliation.stock-count.initiated",
            "reconciliation.stock-count.entry-recorded",
            "reconciliation.stock-count.completed"
        );
    }
}
```

---

## Test Data Builders

### StockCountTestDataBuilder.java

```java
@Component
public class StockCountTestDataBuilder {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private StockCountTestRepository stockCountRepository;

    public String createInProgressStockCount(String tenantId) {
        // Create stock count via API
        GenerateStockCountWorksheetRequest request = buildGenerateWorksheetRequest(
            "CYCLE_COUNT",
            List.of("ZONE-A"),
            null
        );

        GenerateStockCountWorksheetResponse response = webTestClient.post()
            .uri("/api/reconciliation/stock-counts/generate-worksheet")
            .header("X-Tenant-ID", tenantId)
            .header("Authorization", "Bearer " + getAuthToken())
            .bodyValue(request)
            .exchange()
            .expectStatus().isCreated()
            .expectBody(GenerateStockCountWorksheetResponse.class)
            .returnResult()
            .getResponseBody();

        return response.getStockCountId();
    }

    public String createStockCountWithEntries(
            String tenantId,
            List<EntryData> entries) {

        String stockCountId = createInProgressStockCount(tenantId);

        entries.forEach(entry -> {
            RecordStockCountEntryRequest request = buildRecordEntryRequest(
                entry.getLocationCode(),
                entry.getProductCode(),
                entry.getCountedQuantity()
            );

            webTestClient.post()
                .uri("/api/reconciliation/stock-counts/{id}/entries", stockCountId)
                .header("X-Tenant-ID", tenantId)
                .header("Authorization", "Bearer " + getAuthToken())
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated();
        });

        return stockCountId;
    }

    public GenerateStockCountWorksheetRequest buildGenerateWorksheetRequest(
            String countType,
            List<String> locationCodes,
            List<String> productCodes) {

        return GenerateStockCountWorksheetRequest.builder()
            .countType(countType)
            .locationFilter(LocationFilterDTO.builder()
                .locationCodes(locationCodes)
                .build())
            .productFilter(productCodes != null
                ? ProductFilterDTO.builder().productCodes(productCodes).build()
                : null)
            .build();
    }

    public RecordStockCountEntryRequest buildRecordEntryRequest(
            String locationCode,
            String productCode,
            BigDecimal countedQuantity) {

        return RecordStockCountEntryRequest.builder()
            .locationCode(locationCode)
            .productCode(productCode)
            .countedQuantity(countedQuantity)
            .entryNotes("Test entry")
            .build();
    }
}
```

---

## Implementation Checklist

### Test Infrastructure
- [ ] Update `BaseIntegrationTest` with reconciliation support
- [ ] Create `AsyncValidator` utility
- [ ] Create `EventValidator` utility
- [ ] Configure test containers for Reconciliation Service

### Test Classes
- [ ] Implement `StockCountTest`
- [ ] Implement `VarianceInvestigationTest`
- [ ] Implement `D365ReconciliationTest`
- [ ] Implement `ReconciliationWorkflowTest`

### Test Data Builders
- [ ] Implement `StockCountTestDataBuilder`
- [ ] Implement `VarianceTestDataBuilder`
- [ ] Implement `D365TestDataBuilder`
- [ ] Implement `TestDataManager` updates

### Test Scenarios
- [ ] US-8.1.1: Generate worksheet (success, validation errors)
- [ ] US-8.1.2: Record entries (success, duplicates, offline sync)
- [ ] US-8.1.3: Complete count (success, critical variance blocking)
- [ ] US-8.2.1: Investigate variance (success, approval workflow)
- [ ] US-8.3.1: D365 reconciliation (success, retry, audit trail)
- [ ] End-to-end workflows

### Documentation
- [ ] Document test data setup
- [ ] Document async validation patterns
- [ ] Document D365 test environment setup

---

**End of Implementation Plan**
