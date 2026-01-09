# Gateway API Tests Implementation Plan

## Sprint 5: Picking Operations - Gateway API Tests

**Purpose:** Mimic frontend calls to backend through gateway
**Test Coverage:** All Sprint 5 endpoints
**Sprint:** Sprint 5

---

## Table of Contents

1. [Overview](#overview)
2. [Test Infrastructure](#test-infrastructure)
3. [Test Data Builders](#test-data-builders)
4. [Test Scenarios](#test-scenarios)
5. [Implementation Examples](#implementation-examples)

---

## Overview

### Purpose

Gateway API tests validate the complete request/response flow from the gateway through to the backend services, mimicking actual frontend API calls. These tests ensure:

- Proper routing through gateway
- Authentication and authorization
- Request/response serialization
- Error handling and validation
- Multi-tenant isolation

### Test Structure

Following the established pattern from Sprint 3-4:

```
gateway-api-tests/
├── src/test/java/com/ccbsa/wms/gateway/api/
│   ├── BaseIntegrationTest.java
│   ├── PickingListGatewayTest.java
│   ├── LoadGatewayTest.java
│   ├── OrderGatewayTest.java
│   ├── dto/
│   │   ├── CreatePickingListRequest.java
│   │   ├── CreatePickingListResponse.java
│   │   ├── PickingListResponse.java
│   │   ├── UploadPickingListCsvRequest.java
│   │   ├── CsvUploadResponse.java
│   │   ├── PlanPickingLocationsRequest.java
│   │   └── PlanPickingLocationsResponse.java
│   └── fixture/
│       ├── PickingListTestDataBuilder.java
│       ├── LoadTestDataBuilder.java
│       └── OrderTestDataBuilder.java
```

---

## Test Infrastructure

### BaseIntegrationTest

```java
package com.ccbsa.wms.gateway.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import static io.restassured.RestAssured.given;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

    @LocalServerPort
    protected int port;

    @Autowired
    protected ObjectMapper objectMapper;

    protected String accessToken;
    protected String tenantId;

    @BeforeEach
    public void setUp() {
        RestAssured.port = port;
        RestAssured.basePath = "/api/v1";

        // Authenticate and get access token
        accessToken = authenticateUser();
        tenantId = getTenantId();
    }

    protected RequestSpecification authenticatedRequest() {
        return given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + accessToken)
                .header("X-Tenant-Id", tenantId);
    }

    protected String authenticateUser() {
        // Implementation depends on authentication mechanism
        // For Keycloak: get OAuth token
        // For testing: use test user credentials
        return "test-access-token";
    }

    protected String getTenantId() {
        return "550e8400-e29b-41d4-a716-446655440001";
    }

    protected <T> T parseResponse(Response response, Class<T> responseClass) {
        try {
            return objectMapper.readValue(response.asString(), responseClass);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse response", e);
        }
    }
}
```

---

## Test Data Builders

### PickingListTestDataBuilder

```java
package com.ccbsa.wms.gateway.api.fixture;

import com.ccbsa.wms.gateway.api.dto.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class PickingListTestDataBuilder {

    public static CreatePickingListRequest.CreatePickingListRequestBuilder defaultPickingListRequest() {
        return CreatePickingListRequest.builder()
                .loadNumber("LOAD-" + System.currentTimeMillis())
                .notes("Test picking list")
                .orders(defaultOrders());
    }

    public static List<CreatePickingListRequest.OrderRequest> defaultOrders() {
        List<CreatePickingListRequest.OrderRequest> orders = new ArrayList<>();

        orders.add(CreatePickingListRequest.OrderRequest.builder()
                .orderNumber("ORD-001-" + System.currentTimeMillis())
                .customerCode("CUST-001")
                .customerName("Test Customer 1")
                .priority("HIGH")
                .lineItems(defaultLineItems("PROD-001", "PROD-002"))
                .build());

        orders.add(CreatePickingListRequest.OrderRequest.builder()
                .orderNumber("ORD-002-" + System.currentTimeMillis())
                .customerCode("CUST-002")
                .customerName("Test Customer 2")
                .priority("NORMAL")
                .lineItems(defaultLineItems("PROD-003"))
                .build());

        return orders;
    }

    public static List<CreatePickingListRequest.LineItemRequest> defaultLineItems(String... productCodes) {
        List<CreatePickingListRequest.LineItemRequest> lineItems = new ArrayList<>();

        for (String productCode : productCodes) {
            lineItems.add(CreatePickingListRequest.LineItemRequest.builder()
                    .productCode(productCode)
                    .quantity(new BigDecimal("100"))
                    .notes("Test line item for " + productCode)
                    .build());
        }

        return lineItems;
    }

    public static String defaultCsvContent() {
        return """
                Load Number,Order Number,Customer Code,Customer Name,Product Code,Quantity,Priority,Notes
                LOAD-TEST-001,ORD-TEST-001,CUST-001,ABC Company,PROD-001,100,HIGH,Urgent delivery
                LOAD-TEST-001,ORD-TEST-001,CUST-001,ABC Company,PROD-002,50,HIGH,
                LOAD-TEST-001,ORD-TEST-002,CUST-002,XYZ Corp,PROD-003,200,NORMAL,
                LOAD-TEST-002,ORD-TEST-003,CUST-003,DEF Ltd,PROD-001,75,LOW,Handle with care
                """;
    }
}
```

---

## Test Scenarios

### 1. Picking List CSV Upload Tests

**PickingListCsvUploadGatewayTest.java**

```java
package com.ccbsa.wms.gateway.api;

import com.ccbsa.wms.gateway.api.dto.CsvUploadResponse;
import com.ccbsa.wms.gateway.api.fixture.PickingListTestDataBuilder;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

class PickingListCsvUploadGatewayTest extends BaseIntegrationTest {

    @Test
    void testUploadPickingListCsv_ValidCsv_ReturnsSuccess() throws Exception {
        // Arrange
        String csvContent = PickingListTestDataBuilder.defaultCsvContent();
        Path tempFile = Files.createTempFile("picking-list-", ".csv");
        Files.writeString(tempFile, csvContent);

        // Act
        var response = authenticatedRequest()
                .contentType(ContentType.MULTIPART)
                .multiPart("file", tempFile.toFile(), "text/csv")
                .post("/picking/picking-lists/upload-csv")
                .then()
                .statusCode(200)
                .extract()
                .response();

        // Assert
        CsvUploadResponse result = parseResponse(response, CsvUploadResponse.class);
        assertNotNull(result);
        assertTrue(result.getSuccessCount() > 0);
        assertEquals(0, result.getErrorCount());

        // Cleanup
        Files.deleteIfExists(tempFile);
    }

    @Test
    void testUploadPickingListCsv_InvalidCsv_ReturnsBadRequest() throws Exception {
        // Arrange - CSV with missing required columns
        String invalidCsv = "Load Number,Order Number\nLOAD-001,ORD-001";
        Path tempFile = Files.createTempFile("invalid-", ".csv");
        Files.writeString(tempFile, invalidCsv);

        // Act & Assert
        authenticatedRequest()
                .contentType(ContentType.MULTIPART)
                .multiPart("file", tempFile.toFile(), "text/csv")
                .post("/picking/picking-lists/upload-csv")
                .then()
                .statusCode(400)
                .body("message", containsString("Missing required columns"));

        // Cleanup
        Files.deleteIfExists(tempFile);
    }

    @Test
    void testUploadPickingListCsv_FileSizeExceeded_ReturnsPayloadTooLarge() throws Exception {
        // Arrange - Create file larger than 10MB
        Path tempFile = Files.createTempFile("large-", ".csv");
        byte[] largeContent = new byte[11 * 1024 * 1024]; // 11MB
        Files.write(tempFile, largeContent);

        // Act & Assert
        authenticatedRequest()
                .contentType(ContentType.MULTIPART)
                .multiPart("file", tempFile.toFile(), "text/csv")
                .post("/picking/picking-lists/upload-csv")
                .then()
                .statusCode(413); // Payload Too Large

        // Cleanup
        Files.deleteIfExists(tempFile);
    }

    @Test
    void testUploadPickingListCsv_InvalidProductCode_ReturnsValidationErrors() throws Exception {
        // Arrange - CSV with invalid product code
        String csvWithInvalidProduct = """
                Load Number,Order Number,Customer Code,Customer Name,Product Code,Quantity,Priority,Notes
                LOAD-001,ORD-001,CUST-001,Test Customer,INVALID-PROD,100,HIGH,
                """;
        Path tempFile = Files.createTempFile("invalid-product-", ".csv");
        Files.writeString(tempFile, csvWithInvalidProduct);

        // Act
        var response = authenticatedRequest()
                .contentType(ContentType.MULTIPART)
                .multiPart("file", tempFile.toFile(), "text/csv")
                .post("/picking/picking-lists/upload-csv")
                .then()
                .statusCode(200)
                .extract()
                .response();

        // Assert
        CsvUploadResponse result = parseResponse(response, CsvUploadResponse.class);
        assertNotNull(result);
        assertEquals(0, result.getSuccessCount());
        assertTrue(result.getErrorCount() > 0);
        assertTrue(result.getErrors().stream()
                .anyMatch(error -> error.getMessage().contains("Invalid product code")));

        // Cleanup
        Files.deleteIfExists(tempFile);
    }

    @Test
    void testUploadPickingListCsv_UnauthenticatedRequest_ReturnsUnauthorized() throws Exception {
        // Arrange
        String csvContent = PickingListTestDataBuilder.defaultCsvContent();
        Path tempFile = Files.createTempFile("picking-list-", ".csv");
        Files.writeString(tempFile, csvContent);

        // Act & Assert
        given()
                .contentType(ContentType.MULTIPART)
                .multiPart("file", tempFile.toFile(), "text/csv")
                .post("/api/v1/picking/picking-lists/upload-csv")
                .then()
                .statusCode(401); // Unauthorized

        // Cleanup
        Files.deleteIfExists(tempFile);
    }
}
```

### 2. Manual Picking List Creation Tests

**PickingListCreationGatewayTest.java**

```java
package com.ccbsa.wms.gateway.api;

import com.ccbsa.wms.gateway.api.dto.*;
import com.ccbsa.wms.gateway.api.fixture.PickingListTestDataBuilder;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

class PickingListCreationGatewayTest extends BaseIntegrationTest {

    @Test
    void testCreatePickingList_ValidRequest_ReturnsCreated() {
        // Arrange
        CreatePickingListRequest request = PickingListTestDataBuilder
                .defaultPickingListRequest()
                .build();

        // Act
        var response = authenticatedRequest()
                .body(request)
                .post("/picking/picking-lists")
                .then()
                .statusCode(201)
                .extract()
                .response();

        // Assert
        CreatePickingListResponse result = parseResponse(response, CreatePickingListResponse.class);
        assertNotNull(result);
        assertNotNull(result.getPickingListId());
        assertNotNull(result.getLoadNumber());
        assertEquals("Picking list created successfully", result.getMessage());
    }

    @Test
    void testCreatePickingList_MissingRequiredFields_ReturnsBadRequest() {
        // Arrange - Request with no orders
        CreatePickingListRequest request = CreatePickingListRequest.builder()
                .loadNumber("LOAD-001")
                .orders(List.of())
                .build();

        // Act & Assert
        authenticatedRequest()
                .body(request)
                .post("/picking/picking-lists")
                .then()
                .statusCode(400)
                .body("message", containsString("at least one order"));
    }

    @Test
    void testCreatePickingList_InvalidPriority_ReturnsBadRequest() {
        // Arrange
        CreatePickingListRequest request = PickingListTestDataBuilder
                .defaultPickingListRequest()
                .build();

        request.getOrders().get(0).setPriority("INVALID");

        // Act & Assert
        authenticatedRequest()
                .body(request)
                .post("/picking/picking-lists")
                .then()
                .statusCode(400)
                .body("message", containsString("Invalid priority"));
    }

    @Test
    void testCreatePickingList_DuplicateLoadNumber_ReturnsConflict() {
        // Arrange
        String duplicateLoadNumber = "LOAD-DUPLICATE-" + System.currentTimeMillis();

        CreatePickingListRequest request1 = PickingListTestDataBuilder
                .defaultPickingListRequest()
                .loadNumber(duplicateLoadNumber)
                .build();

        CreatePickingListRequest request2 = PickingListTestDataBuilder
                .defaultPickingListRequest()
                .loadNumber(duplicateLoadNumber)
                .build();

        // Act
        // Create first picking list
        authenticatedRequest()
                .body(request1)
                .post("/picking/picking-lists")
                .then()
                .statusCode(201);

        // Try to create second with same load number
        authenticatedRequest()
                .body(request2)
                .post("/picking/picking-lists")
                .then()
                .statusCode(409) // Conflict
                .body("message", containsString("Load number already exists"));
    }
}
```

### 3. Plan Picking Locations Tests

**PickingLocationPlanningGatewayTest.java**

```java
package com.ccbsa.wms.gateway.api;

import com.ccbsa.wms.gateway.api.dto.*;
import com.ccbsa.wms.gateway.api.fixture.PickingListTestDataBuilder;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

class PickingLocationPlanningGatewayTest extends BaseIntegrationTest {

    @Test
    void testPlanPickingLocations_ValidLoad_ReturnsSuccess() {
        // Arrange
        // First create a picking list
        CreatePickingListRequest createRequest = PickingListTestDataBuilder
                .defaultPickingListRequest()
                .build();

        var createResponse = authenticatedRequest()
                .body(createRequest)
                .post("/picking/picking-lists")
                .then()
                .statusCode(201)
                .extract()
                .response();

        CreatePickingListResponse pickingList = parseResponse(createResponse, CreatePickingListResponse.class);

        // Get load ID from picking list
        var pickingListResponse = authenticatedRequest()
                .get("/picking/picking-lists/" + pickingList.getPickingListId())
                .then()
                .statusCode(200)
                .extract()
                .response();

        PickingListResponse pickingListData = parseResponse(pickingListResponse, PickingListResponse.class);
        String loadId = pickingListData.getLoads().get(0).getLoadId();

        // Plan picking locations
        PlanPickingLocationsRequest planRequest = PlanPickingLocationsRequest.builder()
                .loadId(loadId)
                .build();

        // Act
        var planResponse = authenticatedRequest()
                .body(planRequest)
                .post("/picking/loads/" + loadId + "/plan")
                .then()
                .statusCode(200)
                .extract()
                .response();

        // Assert
        PlanPickingLocationsResponse result = parseResponse(planResponse, PlanPickingLocationsResponse.class);
        assertNotNull(result);
        assertTrue(result.getTotalPickingTasks() > 0);
        assertNotNull(result.getMessage());
    }

    @Test
    void testPlanPickingLocations_InsufficientStock_ReturnsBadRequest() {
        // Arrange
        // Create picking list with large quantity that exceeds available stock
        CreatePickingListRequest createRequest = PickingListTestDataBuilder
                .defaultPickingListRequest()
                .build();

        // Set extremely high quantity
        createRequest.getOrders().get(0).getLineItems().get(0)
                .setQuantity(new BigDecimal("999999"));

        var createResponse = authenticatedRequest()
                .body(createRequest)
                .post("/picking/picking-lists")
                .then()
                .statusCode(201)
                .extract()
                .response();

        CreatePickingListResponse pickingList = parseResponse(createResponse, CreatePickingListResponse.class);

        // Get load ID
        var pickingListResponse = authenticatedRequest()
                .get("/picking/picking-lists/" + pickingList.getPickingListId())
                .then()
                .statusCode(200)
                .extract()
                .response();

        PickingListResponse pickingListData = parseResponse(pickingListResponse, PickingListResponse.class);
        String loadId = pickingListData.getLoads().get(0).getLoadId();

        // Act & Assert
        authenticatedRequest()
                .post("/picking/loads/" + loadId + "/plan")
                .then()
                .statusCode(400)
                .body("message", containsString("Insufficient stock"));
    }

    @Test
    void testPlanPickingLocations_AlreadyPlanned_ReturnsConflict() {
        // Arrange
        // Create and plan a load
        CreatePickingListRequest createRequest = PickingListTestDataBuilder
                .defaultPickingListRequest()
                .build();

        var createResponse = authenticatedRequest()
                .body(createRequest)
                .post("/picking/picking-lists")
                .then()
                .statusCode(201)
                .extract()
                .response();

        CreatePickingListResponse pickingList = parseResponse(createResponse, CreatePickingListResponse.class);

        var pickingListResponse = authenticatedRequest()
                .get("/picking/picking-lists/" + pickingList.getPickingListId())
                .then()
                .statusCode(200)
                .extract()
                .response();

        PickingListResponse pickingListData = parseResponse(pickingListResponse, PickingListResponse.class);
        String loadId = pickingListData.getLoads().get(0).getLoadId();

        // Plan first time
        authenticatedRequest()
                .post("/picking/loads/" + loadId + "/plan")
                .then()
                .statusCode(200);

        // Act & Assert - Try to plan again
        authenticatedRequest()
                .post("/picking/loads/" + loadId + "/plan")
                .then()
                .statusCode(409) // Conflict
                .body("message", containsString("already planned"));
    }
}
```

### 4. Query Tests

**PickingListQueryGatewayTest.java**

```java
package com.ccbsa.wms.gateway.api;

import com.ccbsa.wms.gateway.api.dto.*;
import com.ccbsa.wms.gateway.api.fixture.PickingListTestDataBuilder;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

class PickingListQueryGatewayTest extends BaseIntegrationTest {

    @Test
    void testListPickingLists_ReturnsPagedResults() {
        // Arrange
        // Create multiple picking lists
        for (int i = 0; i < 3; i++) {
            CreatePickingListRequest request = PickingListTestDataBuilder
                    .defaultPickingListRequest()
                    .build();

            authenticatedRequest()
                    .body(request)
                    .post("/picking/picking-lists")
                    .then()
                    .statusCode(201);
        }

        // Act & Assert
        authenticatedRequest()
                .queryParam("page", 0)
                .queryParam("size", 10)
                .get("/picking/picking-lists")
                .then()
                .statusCode(200)
                .body("content", hasSize(greaterThanOrEqualTo(3)))
                .body("totalElements", greaterThanOrEqualTo(3))
                .body("totalPages", greaterThanOrEqualTo(1));
    }

    @Test
    void testGetPickingListById_ExistingId_ReturnsPickingList() {
        // Arrange
        CreatePickingListRequest createRequest = PickingListTestDataBuilder
                .defaultPickingListRequest()
                .build();

        var createResponse = authenticatedRequest()
                .body(createRequest)
                .post("/picking/picking-lists")
                .then()
                .statusCode(201)
                .extract()
                .response();

        CreatePickingListResponse created = parseResponse(createResponse, CreatePickingListResponse.class);

        // Act
        var response = authenticatedRequest()
                .get("/picking/picking-lists/" + created.getPickingListId())
                .then()
                .statusCode(200)
                .extract()
                .response();

        // Assert
        PickingListResponse result = parseResponse(response, PickingListResponse.class);
        assertNotNull(result);
        assertEquals(created.getPickingListId(), result.getPickingListId());
        assertTrue(result.getLoads().size() > 0);
    }

    @Test
    void testGetPickingListById_NonExistentId_ReturnsNotFound() {
        // Arrange
        String nonExistentId = "00000000-0000-0000-0000-000000000000";

        // Act & Assert
        authenticatedRequest()
                .get("/picking/picking-lists/" + nonExistentId)
                .then()
                .statusCode(404);
    }

    @Test
    void testGetOrdersByLoad_ReturnsOrders() {
        // Arrange
        CreatePickingListRequest createRequest = PickingListTestDataBuilder
                .defaultPickingListRequest()
                .build();

        var createResponse = authenticatedRequest()
                .body(createRequest)
                .post("/picking/picking-lists")
                .then()
                .statusCode(201)
                .extract()
                .response();

        CreatePickingListResponse created = parseResponse(createResponse, CreatePickingListResponse.class);

        var pickingListResponse = authenticatedRequest()
                .get("/picking/picking-lists/" + created.getPickingListId())
                .then()
                .statusCode(200)
                .extract()
                .response();

        PickingListResponse pickingListData = parseResponse(pickingListResponse, PickingListResponse.class);
        String loadId = pickingListData.getLoads().get(0).getLoadId();

        // Act & Assert
        authenticatedRequest()
                .get("/picking/loads/" + loadId + "/orders")
                .then()
                .statusCode(200)
                .body("orders", hasSize(greaterThan(0)))
                .body("totalOrders", greaterThan(0));
    }
}
```

---

## Acceptance Criteria Validation

| Test Category     | Coverage                                                      | Status    |
|-------------------|---------------------------------------------------------------|-----------|
| CSV Upload        | Valid CSV, Invalid CSV, File size, Product validation, Auth   | ✅ Planned |
| Manual Creation   | Valid request, Missing fields, Invalid data, Duplicates       | ✅ Planned |
| Location Planning | Valid planning, Insufficient stock, Already planned, FEFO     | ✅ Planned |
| Queries           | List picking lists, Get by ID, Get orders by load, Pagination | ✅ Planned |
| Error Handling    | 400, 401, 404, 409, 413 responses                             | ✅ Planned |
| Multi-tenant      | Tenant isolation                                              | ✅ Planned |

---

**Document Control**

- **Version:** 1.0
- **Date:** 2025-01
- **Status:** Draft
