# StockManagementTest Implementation Plan

## Overview
`StockManagementTest` validates stock management functionality through the gateway service. Tests authenticate as TENANT_ADMIN and verify consignment receipt (manual and CSV), stock allocation, stock movement, FEFO (First-Expired-First-Out) logic, expiration date tracking, and tenant-scoped access control.

---

## Objectives

1. **Consignment Receipt**: Test manual consignment entry and CSV bulk upload
2. **Stock Allocation**: Test stock allocation to locations with capacity validation
3. **Stock Movement**: Test stock transfers between locations
4. **FEFO Logic**: Test First-Expired-First-Out allocation logic
5. **Expiration Tracking**: Test expiration date management and alerts
6. **Stock Queries**: Test list consignments, stock levels, and availability
7. **Stock Adjustments**: Test stock adjustments (increase, decrease, corrections)
8. **Tenant Isolation**: Verify TENANT_ADMIN can only manage stock in own tenant
9. **Authorization Checks**: Verify role-based access control
10. **Validation Rules**: Test stock data validation (quantity, expiration dates)

---

## Test Scenarios

### 1. Consignment Receipt Tests (Manual Entry)

#### Test: Create Consignment Successfully
- **Setup**: Login as TENANT_ADMIN, create product and location
- **Action**: POST `/api/v1/stock-management/consignments`
- **Request Body**:
  ```json
  {
    "productId": "{productId}",
    "locationId": "{locationId}",
    "quantity": 100,
    "batchNumber": "BATCH-2025-001",
    "expirationDate": "2025-12-31",
    "manufactureDate": "2025-01-15",
    "supplierReference": "PO-12345",
    "receivedDate": "2025-01-20"
  }
  ```
- **Assertions**:
  - Status: 201 CREATED
  - Response contains `consignmentId`, `productId`, `locationId`, `quantity`
  - StockConsignmentReceivedEvent published
  - Stock level increased by 100 units
  - Consignment created in correct tenant schema

#### Test: Create Consignment with Invalid Product
- **Setup**: Login as TENANT_ADMIN
- **Action**: POST `/api/v1/stock-management/consignments` with non-existent productId
- **Assertions**:
  - Status: 404 NOT FOUND or 400 BAD REQUEST
  - Error message indicates product not found

#### Test: Create Consignment with Invalid Location
- **Setup**: Login as TENANT_ADMIN
- **Action**: POST `/api/v1/stock-management/consignments` with non-existent locationId
- **Assertions**:
  - Status: 404 NOT FOUND or 400 BAD REQUEST
  - Error message indicates location not found

#### Test: Create Consignment with Negative Quantity
- **Setup**: Login as TENANT_ADMIN
- **Action**: POST `/api/v1/stock-management/consignments` with quantity `-10`
- **Assertions**:
  - Status: 400 BAD REQUEST
  - Validation error for negative quantity

#### Test: Create Consignment with Past Expiration Date
- **Setup**: Login as TENANT_ADMIN
- **Action**: POST `/api/v1/stock-management/consignments` with expiration date in the past
- **Assertions**:
  - Status: 400 BAD REQUEST (if validation enforced)
  - Or 201 CREATED with warning

#### Test: Create Consignment Exceeding Location Capacity
- **Setup**: Login as TENANT_ADMIN, create location with capacity 50
- **Action**: POST `/api/v1/stock-management/consignments` with quantity 100
- **Assertions**:
  - Status: 400 BAD REQUEST (if capacity check enforced)
  - Or 201 CREATED with warning

#### Test: Create Consignment with Missing Required Fields
- **Setup**: Login as TENANT_ADMIN
- **Action**: POST `/api/v1/stock-management/consignments` with missing `quantity`
- **Assertions**:
  - Status: 400 BAD REQUEST
  - Validation errors for missing fields

#### Test: Create Consignment Without Authentication
- **Setup**: No authentication
- **Action**: POST `/api/v1/stock-management/consignments` without Bearer token
- **Assertions**:
  - Status: 401 UNAUTHORIZED

---

### 2. Consignment CSV Upload Tests

#### Test: Upload Valid CSV with Consignments
- **Setup**: Login as TENANT_ADMIN, create products and locations
- **CSV Content**:
  ```csv
  productId,locationId,quantity,batchNumber,expirationDate,manufactureDate,supplierReference
  {productId1},{locationId1},100,BATCH-001,2025-12-31,2025-01-15,PO-12345
  {productId2},{locationId1},50,BATCH-002,2025-11-30,2025-01-10,PO-12346
  {productId3},{locationId2},200,BATCH-003,2026-01-31,2025-01-20,PO-12347
  ```
- **Action**: POST `/api/v1/stock-management/consignments/upload/csv` with multipart/form-data
- **Assertions**:
  - Status: 200 OK or 201 CREATED
  - Response contains upload summary:
    - `totalRows: 3`
    - `successCount: 3`
    - `failureCount: 0`
    - `errors: []`
  - All 3 consignments created in database
  - Stock levels updated for all products

#### Test: Upload CSV with Invalid Product IDs
- **Setup**: Login as TENANT_ADMIN, prepare CSV with invalid productId
- **CSV Content**:
  ```csv
  productId,locationId,quantity,batchNumber
  invalid-product-id,{locationId},100,BATCH-004
  ```
- **Action**: POST `/api/v1/stock-management/consignments/upload/csv`
- **Assertions**:
  - Status: 200 OK (partial success) or 400 BAD REQUEST
  - Response contains errors:
    - `failureCount: 1`
    - `errors: [{row: 1, message: "Product not found"}]`

#### Test: Upload CSV with Invalid Data
- **Setup**: Login as TENANT_ADMIN, prepare CSV with negative quantities
- **Action**: POST `/api/v1/stock-management/consignments/upload/csv`
- **Assertions**:
  - Status: 200 OK (partial success) or 400 BAD REQUEST
  - Response contains validation errors for each row

#### Test: Upload Empty CSV
- **Setup**: Login as TENANT_ADMIN, prepare empty CSV
- **Action**: POST `/api/v1/stock-management/consignments/upload/csv` with empty file
- **Assertions**:
  - Status: 400 BAD REQUEST
  - Error message indicates empty file

---

### 3. Stock Allocation Tests

#### Test: Allocate Stock to Location
- **Setup**: Login as TENANT_ADMIN, create consignment
- **Action**: POST `/api/v1/stock-management/allocations`
- **Request Body**:
  ```json
  {
    "productId": "{productId}",
    "sourceLocationId": "{locationId}",
    "quantity": 50,
    "allocationType": "PICKING_ORDER",
    "referenceId": "{orderId}"
  }
  ```
- **Assertions**:
  - Status: 201 CREATED
  - Stock allocated successfully
  - Available quantity reduced by 50
  - StockAllocatedEvent published

#### Test: Allocate Stock Exceeding Available Quantity
- **Setup**: Login as TENANT_ADMIN, create consignment with 100 units
- **Action**: Allocate 150 units
- **Assertions**:
  - Status: 400 BAD REQUEST
  - Error message indicates insufficient stock

---

### 4. Stock Movement Tests

#### Test: Move Stock Between Locations
- **Setup**: Login as TENANT_ADMIN, create consignment in Location A
- **Action**: POST `/api/v1/stock-management/movements`
- **Request Body**:
  ```json
  {
    "productId": "{productId}",
    "sourceLocationId": "{locationAId}",
    "targetLocationId": "{locationBId}",
    "quantity": 50,
    "movementType": "RELOCATION",
    "reason": "Optimization"
  }
  ```
- **Assertions**:
  - Status: 201 CREATED
  - Stock reduced in Location A by 50
  - Stock increased in Location B by 50
  - StockMovedEvent published

#### Test: Move Stock with Insufficient Quantity
- **Setup**: Login as TENANT_ADMIN, create consignment with 50 units
- **Action**: Move 100 units
- **Assertions**:
  - Status: 400 BAD REQUEST
  - Error message indicates insufficient stock

#### Test: Move Stock to Inactive Location
- **Setup**: Login as TENANT_ADMIN, create consignment, deactivate target location
- **Action**: Move stock to inactive location
- **Assertions**:
  - Status: 400 BAD REQUEST
  - Error message indicates location inactive

---

### 5. FEFO (First-Expired-First-Out) Logic Tests

#### Test: Allocate Stock Using FEFO
- **Setup**:
  - Login as TENANT_ADMIN
  - Create 3 consignments of same product with different expiration dates:
    - Batch A: Expires 2025-10-31 (100 units)
    - Batch B: Expires 2025-12-31 (100 units)
    - Batch C: Expires 2026-01-31 (100 units)
- **Action**: Allocate 150 units using FEFO
- **Assertions**:
  - Batch A allocated first (100 units)
  - Batch B allocated next (50 units)
  - Batch C not allocated
  - FEFO logic enforced

#### Test: Skip Expired Stock in FEFO Allocation
- **Setup**:
  - Create consignment with past expiration date (expired)
  - Create consignment with future expiration date
- **Action**: Allocate stock using FEFO
- **Assertions**:
  - Expired stock skipped
  - Only valid stock allocated

---

### 6. Stock Query Tests

#### Test: List All Consignments with Pagination
- **Setup**: Login as TENANT_ADMIN, create 15 consignments
- **Action**: GET `/api/v1/stock-management/consignments?page=0&size=10`
- **Assertions**:
  - Status: 200 OK
  - Response contains:
    - `consignments: [...]` (10 items)
    - Pagination metadata

#### Test: Get Consignment by ID
- **Setup**: Login as TENANT_ADMIN, create consignment
- **Action**: GET `/api/v1/stock-management/consignments/{consignmentId}`
- **Assertions**:
  - Status: 200 OK
  - Response contains consignment details

#### Test: Get Stock Level by Product and Location
- **Setup**: Login as TENANT_ADMIN, create consignment
- **Action**: GET `/api/v1/stock-management/stock-levels?productId={productId}&locationId={locationId}`
- **Assertions**:
  - Status: 200 OK
  - Response contains available quantity, allocated quantity, total quantity

#### Test: Get Stock Availability by Product
- **Setup**: Login as TENANT_ADMIN, create consignments in multiple locations
- **Action**: GET `/api/v1/stock-management/stock-levels?productId={productId}`
- **Assertions**:
  - Status: 200 OK
  - Response contains aggregated stock levels across all locations

#### Test: List Consignments Expiring Soon
- **Setup**: Login as TENANT_ADMIN, create consignments with various expiration dates
- **Action**: GET `/api/v1/stock-management/consignments?expiringWithinDays=30`
- **Assertions**:
  - Status: 200 OK
  - Response contains only consignments expiring within 30 days

---

### 7. Stock Adjustment Tests

#### Test: Increase Stock (Positive Adjustment)
- **Setup**: Login as TENANT_ADMIN, create consignment with 100 units
- **Action**: POST `/api/v1/stock-management/adjustments`
- **Request Body**:
  ```json
  {
    "consignmentId": "{consignmentId}",
    "adjustmentType": "INCREASE",
    "quantity": 20,
    "reason": "Stock count correction"
  }
  ```
- **Assertions**:
  - Status: 201 CREATED
  - Stock increased to 120 units
  - StockAdjustedEvent published

#### Test: Decrease Stock (Negative Adjustment)
- **Setup**: Login as TENANT_ADMIN, create consignment with 100 units
- **Action**: POST `/api/v1/stock-management/adjustments`
- **Request Body**:
  ```json
  {
    "consignmentId": "{consignmentId}",
    "adjustmentType": "DECREASE",
    "quantity": 20,
    "reason": "Damaged goods"
  }
  ```
- **Assertions**:
  - Status: 201 CREATED
  - Stock decreased to 80 units

#### Test: Adjust Stock Below Zero
- **Setup**: Login as TENANT_ADMIN, create consignment with 50 units
- **Action**: Decrease stock by 100 units
- **Assertions**:
  - Status: 400 BAD REQUEST
  - Error message indicates insufficient stock

---

### 8. Tenant Isolation Tests

#### Test: TENANT_ADMIN Lists Only Own Tenant Consignments
- **Setup**:
  - Login as SYSTEM_ADMIN, create Tenant A and Tenant B
  - Create 3 consignments in Tenant A
  - Create 2 consignments in Tenant B
  - Login as TENANT_ADMIN (Tenant A)
- **Action**: GET `/api/v1/stock-management/consignments`
- **Assertions**:
  - Status: 200 OK
  - Response contains only Tenant A consignments (3 consignments)
  - Tenant B consignments not visible

#### Test: TENANT_ADMIN Cannot Access Consignment from Different Tenant
- **Setup**:
  - Login as SYSTEM_ADMIN, create Tenant A and Tenant B
  - Create consignment in Tenant B
  - Login as TENANT_ADMIN (Tenant A)
- **Action**: GET `/api/v1/stock-management/consignments/{tenantBConsignmentId}`
- **Assertions**:
  - Status: 403 FORBIDDEN or 404 NOT FOUND

---

### 9. Authorization Tests

#### Test: WAREHOUSE_MANAGER Can Manage Stock
- **Setup**: Create user with WAREHOUSE_MANAGER role, login
- **Action**: POST `/api/v1/stock-management/consignments` with valid data
- **Assertions**:
  - Status: 201 CREATED

#### Test: STOCK_MANAGER Can Manage Stock
- **Setup**: Create user with STOCK_MANAGER role, login
- **Action**: POST `/api/v1/stock-management/consignments` with valid data
- **Assertions**:
  - Status: 201 CREATED

#### Test: STOCK_CLERK Can Receive Stock
- **Setup**: Create user with STOCK_CLERK role, login
- **Action**: POST `/api/v1/stock-management/consignments` with valid data
- **Assertions**:
  - Status: 201 CREATED

#### Test: PICKER Can Read Stock Levels
- **Setup**: Create user with PICKER role, login
- **Action**: GET `/api/v1/stock-management/stock-levels`
- **Assertions**:
  - Status: 200 OK

#### Test: PICKER Cannot Create Consignments
- **Setup**: Login as PICKER
- **Action**: POST `/api/v1/stock-management/consignments` with valid data
- **Assertions**:
  - Status: 403 FORBIDDEN

#### Test: VIEWER Can Read Stock Levels
- **Setup**: Create user with VIEWER role, login
- **Action**: GET `/api/v1/stock-management/stock-levels`
- **Assertions**:
  - Status: 200 OK

#### Test: VIEWER Cannot Modify Stock
- **Setup**: Login as VIEWER
- **Action**: POST `/api/v1/stock-management/consignments` with valid data
- **Assertions**:
  - Status: 403 FORBIDDEN

---

### 10. Edge Case Tests

#### Test: Create Consignment with Very Large Quantity
- **Setup**: Login as TENANT_ADMIN
- **Action**: POST `/api/v1/stock-management/consignments` with quantity 1,000,000
- **Assertions**:
  - Status: 201 CREATED or 400 BAD REQUEST (depending on limits)

#### Test: Create Consignment with Duplicate Batch Number
- **Setup**: Login as TENANT_ADMIN, create consignment with batch "BATCH-001"
- **Action**: Create another consignment with same batch number
- **Assertions**:
  - Status: 201 CREATED (batch numbers may not be unique globally)
  - Or 409 CONFLICT (if uniqueness enforced)

#### Test: Concurrent Consignment Creation
- **Setup**: Login as TENANT_ADMIN
- **Action**: Send 5 concurrent POST requests to create consignments
- **Assertions**:
  - All requests succeed (201 CREATED)
  - Each consignment has unique ID

---

## Test Data Strategy

### Faker Data Generation

```java
private CreateConsignmentRequest createRandomConsignmentRequest(String productId, String locationId) {
    return CreateConsignmentRequest.builder()
            .productId(productId)
            .locationId(locationId)
            .quantity(faker.number().numberBetween(10, 500))
            .batchNumber("BATCH-" + faker.number().digits(6))
            .expirationDate(faker.date().future(365, TimeUnit.DAYS))
            .manufactureDate(faker.date().past(30, TimeUnit.DAYS))
            .supplierReference("PO-" + faker.number().digits(5))
            .receivedDate(LocalDate.now())
            .build();
}
```

---

## Test Class Structure

```java
package com.ccbsa.wms.gateway.api;

import com.ccbsa.wms.gateway.api.dto.AuthenticationResult;
import com.ccbsa.wms.gateway.api.fixture.ConsignmentTestDataBuilder;
import org.junit.jupiter.api.*;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class StockManagementTest extends BaseIntegrationTest {

    private static AuthenticationResult tenantAdminAuth;
    private static String testTenantId;
    private static String testProductId;
    private static String testLocationId;

    @BeforeAll
    public static void setupTestData() {
        // Login as TENANT_ADMIN
        tenantAdminAuth = loginAsTenantAdmin();
        testTenantId = tenantAdminAuth.getTenantId();

        // Create product and location for tests
        testProductId = createTestProduct(tenantAdminAuth);
        testLocationId = createTestLocation(tenantAdminAuth);
    }

    // ==================== CONSIGNMENT RECEIPT TESTS ====================

    @Test
    @Order(1)
    public void testCreateConsignment_Success() {
        // Arrange
        CreateConsignmentRequest request = ConsignmentTestDataBuilder.buildCreateConsignmentRequest(
                testProductId, testLocationId, faker);

        // Act
        WebTestClient.ResponseSpec response = authenticatedPost(
                "/api/v1/stock-management/consignments",
                tenantAdminAuth.getAccessToken(),
                testTenantId,
                request
        ).exchange();

        // Assert
        response.expectStatus().isCreated();

        CreateConsignmentResponse consignment = response.expectBody(CreateConsignmentResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(consignment).isNotNull();
        assertThat(consignment.getConsignmentId()).isNotBlank();
        assertThat(consignment.getQuantity()).isEqualTo(request.getQuantity());
    }

    // ... Additional test methods
}
```

---

## Test Fixtures

### ConsignmentTestDataBuilder

Create `gateway-api-tests/src/test/java/com/ccbsa/wms/gateway/api/fixture/ConsignmentTestDataBuilder.java`:

```java
package com.ccbsa.wms.gateway.api.fixture;

import net.datafaker.Faker;

import java.time.LocalDate;

public class ConsignmentTestDataBuilder {

    public static CreateConsignmentRequest buildCreateConsignmentRequest(String productId, String locationId, Faker faker) {
        return CreateConsignmentRequest.builder()
                .productId(productId)
                .locationId(locationId)
                .quantity(faker.number().numberBetween(10, 500))
                .batchNumber("BATCH-" + faker.number().digits(6))
                .expirationDate(LocalDate.now().plusMonths(6))
                .manufactureDate(LocalDate.now().minusDays(15))
                .supplierReference("PO-" + faker.number().digits(5))
                .receivedDate(LocalDate.now())
                .build();
    }
}
```

---

## DTOs Required

### CreateConsignmentRequest

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateConsignmentRequest {
    private String productId;
    private String locationId;
    private Integer quantity;
    private String batchNumber;
    private LocalDate expirationDate;
    private LocalDate manufactureDate;
    private String supplierReference;
    private LocalDate receivedDate;
}
```

### CreateConsignmentResponse

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateConsignmentResponse {
    private String consignmentId;
    private String productId;
    private String locationId;
    private Integer quantity;
    private String batchNumber;
}
```

---

## Environment Variables

```bash
TEST_TENANT_ADMIN_USERNAME=<user-input>
TEST_TENANT_ADMIN_PASSWORD=Password123@
```

---

## Testing Checklist

- [ ] Consignment created successfully with valid data
- [ ] Consignment creation fails with invalid product/location
- [ ] Consignment creation fails with negative quantity
- [ ] CSV upload succeeds with valid data
- [ ] CSV upload handles invalid data
- [ ] Stock allocated successfully
- [ ] Stock moved between locations successfully
- [ ] FEFO logic allocates oldest expiring stock first
- [ ] List consignments with pagination works
- [ ] Get stock level by product and location works
- [ ] Stock adjustment (increase/decrease) works
- [ ] Tenant isolation verified
- [ ] Authorization checks prevent unauthorized access
- [ ] StockConsignmentReceivedEvent published on creation

---

## Next Steps

1. **Implement StockManagementTest** with all test scenarios
2. **Create ConsignmentTestDataBuilder** for test data generation
3. **Create DTO classes** for consignment requests/responses
4. **Prepare test CSV files** for bulk upload
5. **Test FEFO logic** with multiple batches
6. **Validate event publishing** (StockConsignmentReceivedEvent, StockMovedEvent)
7. **Document test results** and edge cases discovered

---

## Notes

- **FEFO Logic**: Allocates stock based on expiration date (oldest first)
- **Capacity Validation**: May be enforced at consignment creation or allocation
- **Batch Numbers**: May not be globally unique across tenants
- **Rate Limiting**: Stock service has 100 req/min limit
- **Tenant Isolation**: Stock scoped to tenant schema via X-Tenant-Id header
