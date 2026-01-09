# Sprint 6: Gateway API Tests Implementation Plan

## Overview

**Sprint:** Sprint 6 - Picking Execution and Expiration Management
**Purpose:** Comprehensive API testing for all Sprint 6 features through the gateway
**Test Coverage:** Execute Picking, Complete Picking, Expiration Tracking, Restock Requests

---

## Table of Contents

1. [Test Structure](#test-structure)
2. [Test Scenarios](#test-scenarios)
3. [Test Data Setup](#test-data-setup)
4. [Test Implementation](#test-implementation)
5. [Implementation Checklist](#implementation-checklist)

---

## Test Structure

### Base Test Class

**Location:** `gateway-api-tests/src/test/java/com/ccbsa/wms/gateway/api/Sprint06IntegrationTest.java`

```java
package com.ccbsa.wms.gateway.api;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class Sprint06IntegrationTest extends BaseIntegrationTest {

    private static UUID productId;
    private static UUID locationId;
    private static UUID pickingListId;
    private static UUID pickingTaskId;

    @BeforeAll
    static void setupTestData() {
        // Create test product
        productId = createTestProduct("P-TEST-SP6", "Sprint 6 Test Product");

        // Create test location
        locationId = createTestLocation("L-SP6-01", "Zone A", "Aisle 1");

        // Create stock with expiration dates
        createStockItem(productId, locationId, 100, "2026-02-15"); // 37 days - NORMAL
        createStockItem(productId, locationId, 50, "2026-01-25"); // 17 days - NEAR_EXPIRY
        createStockItem(productId, locationId, 30, "2026-01-12"); // 4 days - CRITICAL
        createStockItem(productId, locationId, 20, "2026-01-01"); // EXPIRED
    }
}
```

---

## Test Scenarios

### 1. Execute Picking Task Tests

```java
@Nested
@DisplayName("US-6.3.1: Execute Picking Task Tests")
class ExecutePickingTaskTests {

    @Test
    @Order(1)
    @DisplayName("Should create picking list with tasks")
    void shouldCreatePickingListWithTasks() {
        CreatePickingListRequest request = CreatePickingListRequest.builder()
                .loadNumber("LOAD-SP6-001")
                .orderNumber("ORD-SP6-001")
                .productCode("P-TEST-SP6")
                .quantity(50)
                .build();

        pickingListId = given()
                .spec(requestSpec)
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/v1/picking/picking-lists")
                .then()
                .statusCode(201)
                .body("success", is(true))
                .body("data.pickingListId", notNullValue())
                .extract()
                .jsonPath()
                .getUUID("data.pickingListId");

        // Get picking tasks for the list
        pickingTaskId = given()
                .spec(requestSpec)
                .when()
                .get("/api/v1/picking/picking-lists/{pickingListId}/tasks", pickingListId)
                .then()
                .statusCode(200)
                .body("data", hasSize(greaterThan(0)))
                .extract()
                .jsonPath()
                .getUUID("data[0].pickingTaskId");
    }

    @Test
    @Order(2)
    @DisplayName("Should execute picking task successfully")
    void shouldExecutePickingTaskSuccessfully() {
        ExecutePickingTaskRequest request = ExecutePickingTaskRequest.builder()
                .pickedQuantity(50)
                .isPartialPicking(false)
                .build();

        given()
                .spec(requestSpec)
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/v1/picking/picking-tasks/{pickingTaskId}/execute", pickingTaskId)
                .then()
                .statusCode(200)
                .body("success", is(true))
                .body("data.status", equalTo("COMPLETED"))
                .body("data.pickedQuantity", equalTo(50));
    }

    @Test
    @Order(3)
    @DisplayName("Should reject picking expired stock")
    void shouldRejectPickingExpiredStock() {
        // Create picking task with expired stock
        UUID expiredPickingTaskId = createPickingTaskWithExpiredStock();

        ExecutePickingTaskRequest request = ExecutePickingTaskRequest.builder()
                .pickedQuantity(10)
                .isPartialPicking(false)
                .build();

        given()
                .spec(requestSpec)
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/v1/picking/picking-tasks/{pickingTaskId}/execute",
                        expiredPickingTaskId)
                .then()
                .statusCode(400)
                .body("success", is(false))
                .body("error.code", equalTo("EXPIRED_STOCK"))
                .body("error.message", containsString("Cannot pick expired stock"));
    }

    @Test
    @Order(4)
    @DisplayName("Should handle partial picking with reason")
    void shouldHandlePartialPickingWithReason() {
        UUID partialPickingTaskId = createPickingTask(productId, 100);

        ExecutePickingTaskRequest request = ExecutePickingTaskRequest.builder()
                .pickedQuantity(60)
                .isPartialPicking(true)
                .partialReason("Stock damaged - 40 units rejected")
                .build();

        given()
                .spec(requestSpec)
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/v1/picking/picking-tasks/{pickingTaskId}/execute",
                        partialPickingTaskId)
                .then()
                .statusCode(200)
                .body("success", is(true))
                .body("data.status", equalTo("PARTIALLY_COMPLETED"))
                .body("data.pickedQuantity", equalTo(60))
                .body("data.isPartialPicking", is(true));
    }

    @Test
    @DisplayName("Should validate picked quantity constraints")
    void shouldValidatePickedQuantityConstraints() {
        UUID testPickingTaskId = createPickingTask(productId, 100);

        // Test zero quantity
        ExecutePickingTaskRequest zeroRequest = ExecutePickingTaskRequest.builder()
                .pickedQuantity(0)
                .isPartialPicking(false)
                .build();

        given()
                .spec(requestSpec)
                .contentType(ContentType.JSON)
                .body(zeroRequest)
                .when()
                .post("/api/v1/picking/picking-tasks/{pickingTaskId}/execute",
                        testPickingTaskId)
                .then()
                .statusCode(400)
                .body("error.message", containsString("must be at least 1"));

        // Test quantity exceeding required
        ExecutePickingTaskRequest excessRequest = ExecutePickingTaskRequest.builder()
                .pickedQuantity(150)
                .isPartialPicking(false)
                .build();

        given()
                .spec(requestSpec)
                .contentType(ContentType.JSON)
                .body(excessRequest)
                .when()
                .post("/api/v1/picking/picking-tasks/{pickingTaskId}/execute",
                        testPickingTaskId)
                .then()
                .statusCode(400)
                .body("error.message", containsString("cannot exceed required quantity"));
    }
}
```

### 2. Complete Picking Tests

```java
@Nested
@DisplayName("US-6.3.2: Complete Picking Tests")
class CompletePickingTests {

    @Test
    @DisplayName("Should complete picking list when all tasks completed")
    void shouldCompletePickingListSuccessfully() {
        // Setup: Create picking list with all tasks completed
        UUID completedPickingListId = createPickingListWithCompletedTasks();

        given()
                .spec(requestSpec)
                .when()
                .post("/api/v1/picking/picking-lists/{pickingListId}/complete",
                        completedPickingListId)
                .then()
                .statusCode(200)
                .body("success", is(true))
                .body("data.status", equalTo("COMPLETED"))
                .body("data.completedAt", notNullValue());
    }

    @Test
    @DisplayName("Should reject completion with pending tasks")
    void shouldRejectCompletionWithPendingTasks() {
        // Setup: Create picking list with pending tasks
        UUID incompletePicking ListId = createPickingListWithPendingTasks();

        given()
                .spec(requestSpec)
                .when()
                .post("/api/v1/picking/picking-lists/{pickingListId}/complete",
                        incompletePickingListId)
                .then()
                .statusCode(400)
                .body("success", is(false))
                .body("error.code", equalTo("PICKING_NOT_COMPLETE"))
                .body("error.message", containsString("task(s) still pending"));
    }

    @Test
    @DisplayName("Should allow completion with partial picking")
    void shouldAllowCompletionWithPartialPicking() {
        // Setup: Create picking list with all tasks completed or partially completed
        UUID partialPickingListId = createPickingListWithPartialTasks();

        given()
                .spec(requestSpec)
                .when()
                .post("/api/v1/picking/picking-lists/{pickingListId}/complete",
                        partialPickingListId)
                .then()
                .statusCode(200)
                .body("success", is(true))
                .body("data.status", equalTo("COMPLETED"));
    }
}
```

### 3. Expiration Tracking Tests

```java
@Nested
@DisplayName("US-2.1.3: Track Expiration Dates Tests")
class ExpirationTrackingTests {

    @Test
    @DisplayName("Should query expiring stock by date range")
    void shouldQueryExpiringStockByDateRange() {
        given()
                .spec(requestSpec)
                .queryParam("startDate", "2026-01-08")
                .queryParam("endDate", "2026-02-08")
                .when()
                .get("/api/v1/stock/stock-items/expiring")
                .then()
                .statusCode(200)
                .body("success", is(true))
                .body("data", hasSize(greaterThan(0)))
                .body("data[0].expirationDate", notNullValue())
                .body("data[0].classification", notNullValue());
    }

    @Test
    @DisplayName("Should filter expiring stock by classification")
    void shouldFilterExpiringStockByClassification() {
        // Query CRITICAL classification
        given()
                .spec(requestSpec)
                .queryParam("startDate", "2026-01-08")
                .queryParam("endDate", "2026-01-15")
                .queryParam("classification", "CRITICAL")
                .when()
                .get("/api/v1/stock/stock-items/expiring")
                .then()
                .statusCode(200)
                .body("success", is(true))
                .body("data", hasSize(greaterThan(0)))
                .body("data[0].classification", equalTo("CRITICAL"));
    }

    @Test
    @DisplayName("Should generate expiration alerts for critical stock")
    void shouldGenerateExpirationAlertsForCriticalStock() {
        // Trigger expiration check (simulate scheduled job)
        given()
                .spec(requestSpec)
                .when()
                .post("/api/v1/stock/expiration-check/trigger")
                .then()
                .statusCode(200)
                .body("success", is(true));

        // Verify notifications created
        given()
                .spec(requestSpec)
                .queryParam("type", "STOCK_EXPIRING")
                .when()
                .get("/api/v1/notifications")
                .then()
                .statusCode(200)
                .body("data", hasSize(greaterThan(0)))
                .body("data[0].type", equalTo("STOCK_EXPIRING"));
    }
}
```

### 4. Prevent Picking Expired Stock Tests

```java
@Nested
@DisplayName("US-2.1.4: Prevent Picking Expired Stock Tests")
class PreventPickingExpiredStockTests {

    @Test
    @DisplayName("Should exclude expired stock from FEFO query")
    void shouldExcludeExpiredStockFromFEFOQuery() {
        given()
                .spec(requestSpec)
                .queryParam("productId", productId)
                .when()
                .get("/api/v1/stock/stock-items/fefo")
                .then()
                .statusCode(200)
                .body("success", is(true))
                .body("data", hasSize(greaterThan(0)))
                .body("data.classification", everyItem(not(equalTo("EXPIRED"))));
    }

    @Test
    @DisplayName("Should check stock expiration before picking")
    void shouldCheckStockExpirationBeforePicking() {
        given()
                .spec(requestSpec)
                .queryParam("productId", productId)
                .queryParam("locationId", locationId)
                .when()
                .get("/api/v1/stock/stock-items/check-expiration")
                .then()
                .statusCode(200)
                .body("data.expired", is(boolean.class))
                .body("data.expirationDate", notNullValue())
                .body("data.classification", notNullValue());
    }

    @Test
    @DisplayName("Should audit expired stock picking attempts")
    void shouldAuditExpiredStockPickingAttempts() {
        // Attempt to pick expired stock
        UUID expiredTaskId = createPickingTaskWithExpiredStock();

        ExecutePickingTaskRequest request = ExecutePickingTaskRequest.builder()
                .pickedQuantity(10)
                .isPartialPicking(false)
                .build();

        given()
                .spec(requestSpec)
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/v1/picking/picking-tasks/{pickingTaskId}/execute", expiredTaskId)
                .then()
                .statusCode(400);

        // Verify audit log entry created
        given()
                .spec(requestSpec)
                .queryParam("eventType", "EXPIRED_STOCK_PICKING_ATTEMPT")
                .when()
                .get("/api/v1/audit/logs")
                .then()
                .statusCode(200)
                .body("data", hasSize(greaterThan(0)))
                .body("data[0].eventType", equalTo("EXPIRED_STOCK_PICKING_ATTEMPT"));
    }
}
```

### 5. Generate Restock Request Tests

```java
@Nested
@DisplayName("US-5.1.3: Generate Restock Request Tests")
class GenerateRestockRequestTests {

    @Test
    @DisplayName("Should generate restock request when below minimum")
    void shouldGenerateRestockRequestWhenBelowMinimum() {
        // Setup: Product with stock below minimum
        UUID lowStockProductId = createProductWithLowStock();

        // Simulate stock level falling below minimum
        simulateStockConsumption(lowStockProductId, 80);

        // Query restock requests
        given()
                .spec(requestSpec)
                .queryParam("productId", lowStockProductId)
                .queryParam("status", "PENDING")
                .when()
                .get("/api/v1/stock/restock-requests")
                .then()
                .statusCode(200)
                .body("success", is(true))
                .body("data", hasSize(1))
                .body("data[0].productId", equalTo(lowStockProductId.toString()))
                .body("data[0].status", equalTo("PENDING"))
                .body("data[0].priority", notNullValue());
    }

    @Test
    @DisplayName("Should prevent duplicate restock requests")
    void shouldPreventDuplicateRestockRequests() {
        UUID testProductId = createProductWithLowStock();

        // Generate first restock request
        simulateStockConsumption(testProductId, 80);

        // Attempt to generate duplicate
        simulateStockConsumption(testProductId, 5);

        // Should still have only one active request
        given()
                .spec(requestSpec)
                .queryParam("productId", testProductId)
                .queryParam("status", "PENDING")
                .when()
                .get("/api/v1/stock/restock-requests")
                .then()
                .statusCode(200)
                .body("data", hasSize(1));
    }

    @Test
    @DisplayName("Should calculate correct priority based on stock level")
    void shouldCalculateCorrectPriority() {
        // Setup products with different stock levels
        UUID highPriorityProduct = createProductWithStock(10, 50, 100); // 20% of min
        UUID mediumPriorityProduct = createProductWithStock(35, 50, 100); // 70% of min
        UUID lowPriorityProduct = createProductWithStock(45, 50, 100); // 90% of min

        // Trigger restock requests
        simulateRestockGeneration(highPriorityProduct);
        simulateRestockGeneration(mediumPriorityProduct);
        simulateRestockGeneration(lowPriorityProduct);

        // Verify priorities
        given()
                .spec(requestSpec)
                .queryParam("productId", highPriorityProduct)
                .when()
                .get("/api/v1/stock/restock-requests")
                .then()
                .body("data[0].priority", equalTo("HIGH"));

        given()
                .spec(requestSpec)
                .queryParam("productId", mediumPriorityProduct)
                .when()
                .get("/api/v1/stock/restock-requests")
                .then()
                .body("data[0].priority", equalTo("MEDIUM"));

        given()
                .spec(requestSpec)
                .queryParam("productId", lowPriorityProduct)
                .when()
                .get("/api/v1/stock/restock-requests")
                .then()
                .body("data[0].priority", equalTo("LOW"));
    }
}
```

---

## Test Data Setup

### Helper Methods

```java
/**
 * Create test product with expiration tracking enabled
 */
private static UUID createTestProduct(String productCode, String description) {
    CreateProductRequest request = CreateProductRequest.builder()
            .productCode(productCode)
            .description(description)
            .trackExpiration(true)
            .build();

    return given()
            .spec(requestSpec)
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .post("/api/v1/products")
            .then()
            .statusCode(201)
            .extract()
            .jsonPath()
            .getUUID("data.productId");
}

/**
 * Create stock item with expiration date
 */
private static void createStockItem(UUID productId, UUID locationId,
                                   int quantity, String expirationDate) {
    CreateStockItemRequest request = CreateStockItemRequest.builder()
            .productId(productId)
            .locationId(locationId)
            .quantity(quantity)
            .expirationDate(expirationDate)
            .build();

    given()
            .spec(requestSpec)
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .post("/api/v1/stock/stock-items")
            .then()
            .statusCode(201);
}

/**
 * Create picking task with expired stock
 */
private static UUID createPickingTaskWithExpiredStock() {
    // Create product with expired stock
    UUID expiredProductId = createTestProduct("P-EXPIRED", "Expired Test Product");
    createStockItem(expiredProductId, locationId, 50, "2026-01-01"); // Expired

    // Create picking list and task
    return createPickingTask(expiredProductId, 10);
}

/**
 * Simulate stock consumption to trigger restock
 */
private static void simulateStockConsumption(UUID productId, int quantityToConsume) {
    ConsumeStockRequest request = ConsumeStockRequest.builder()
            .productId(productId)
            .quantity(quantityToConsume)
            .reason("Test consumption")
            .build();

    given()
            .spec(requestSpec)
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .post("/api/v1/stock/stock-items/consume")
            .then()
            .statusCode(200);
}
```

---

## Implementation Checklist

### Test Infrastructure
- [ ] Update `BaseIntegrationTest` with Sprint 6 test utilities
- [ ] Create helper methods for test data setup
- [ ] Add test data cleanup between tests
- [ ] Configure test database with test schema

### Execute Picking Task Tests
- [ ] Test successful picking execution
- [ ] Test partial picking with reason
- [ ] Test expired stock rejection
- [ ] Test quantity validation
- [ ] Test stock level updates after picking
- [ ] Test event publishing verification

### Complete Picking Tests
- [ ] Test successful completion
- [ ] Test rejection with pending tasks
- [ ] Test completion with partial picking
- [ ] Test completion event publishing

### Expiration Tracking Tests
- [ ] Test expiring stock query
- [ ] Test filtering by classification
- [ ] Test expiration check scheduled job
- [ ] Test notification generation
- [ ] Test FEFO query excludes expired stock

### Prevent Picking Expired Stock Tests
- [ ] Test expired stock exclusion from queries
- [ ] Test expiration check before picking
- [ ] Test audit logging of picking attempts
- [ ] Test UI validation integration

### Generate Restock Request Tests
- [ ] Test restock request generation
- [ ] Test duplicate prevention
- [ ] Test priority calculation
- [ ] Test D365 integration (if enabled)
- [ ] Test restock request status tracking

### End-to-End Workflow Tests
- [ ] Test complete picking workflow with expiration checks
- [ ] Test restock triggered by picking consumption
- [ ] Test notification generation throughout workflow
- [ ] Test audit trail completeness

### Performance Tests
- [ ] Test query performance with large datasets
- [ ] Test concurrent picking execution
- [ ] Test scheduled job performance
- [ ] Verify database query optimization

### Documentation
- [ ] Document test scenarios
- [ ] Document test data requirements
- [ ] Create troubleshooting guide
- [ ] Document known limitations

---

## Test Execution

### Run All Sprint 6 Tests

```bash
mvn clean test -Dtest=Sprint06IntegrationTest
```

### Run Specific Test Category

```bash
mvn test -Dtest=Sprint06IntegrationTest\$ExecutePickingTaskTests
mvn test -Dtest=Sprint06IntegrationTest\$ExpirationTrackingTests
```

### Generate Test Report

```bash
mvn surefire-report:report
```

---

**Document Version:** 1.0  
**Last Updated:** 2026-01-08  
**Status:** Ready for Implementation
