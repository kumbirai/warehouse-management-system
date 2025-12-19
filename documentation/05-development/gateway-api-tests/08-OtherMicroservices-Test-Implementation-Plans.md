# Other Microservices Test Implementation Plans

## Overview
This document provides implementation plans for testing the remaining microservices: Picking Service, Returns Service, and Reconciliation Service. All tests authenticate as TENANT_ADMIN and verify CRUD operations, lifecycle management, and tenant-scoped access control.

---

## 1. Picking Service Tests

### Overview
Validates picking task creation, allocation, execution, and completion workflows.

### Test Scenarios

#### Test: Create Picking Task
- **Setup**: Login as TENANT_ADMIN, create product with stock
- **Action**: POST `/api/v1/picking/tasks`
- **Request Body**:
  ```json
  {
    "orderId": "ORDER-12345",
    "items": [
      {
        "productId": "{productId}",
        "quantity": 50,
        "locationId": "{locationId}"
      }
    ],
    "priority": "HIGH",
    "dueDate": "2025-12-31"
  }
  ```
- **Assertions**:
  - Status: 201 CREATED
  - Response contains `taskId`, `status=PENDING`
  - PickingTaskCreatedEvent published

#### Test: Assign Picking Task to User
- **Setup**: Create picking task, create user with PICKER role
- **Action**: PUT `/api/v1/picking/tasks/{taskId}/assign`
- **Request Body**:
  ```json
  {
    "userId": "{pickerId}"
  }
  ```
- **Assertions**:
  - Status: 200 OK
  - Task status changed to ASSIGNED
  - PickingTaskAssignedEvent published

#### Test: Start Picking Task
- **Setup**: Assign task to picker, login as picker
- **Action**: PUT `/api/v1/picking/tasks/{taskId}/start`
- **Assertions**:
  - Status: 200 OK
  - Task status changed to IN_PROGRESS
  - PickingTaskStartedEvent published

#### Test: Complete Picking Task
- **Setup**: Start picking task
- **Action**: PUT `/api/v1/picking/tasks/{taskId}/complete`
- **Request Body**:
  ```json
  {
    "pickedItems": [
      {
        "productId": "{productId}",
        "quantity": 50,
        "locationId": "{locationId}",
        "batchNumber": "BATCH-001"
      }
    ]
  }
  ```
- **Assertions**:
  - Status: 200 OK
  - Task status changed to COMPLETED
  - Stock allocated/deducted
  - PickingTaskCompletedEvent published

#### Test: Cancel Picking Task
- **Setup**: Create picking task
- **Action**: PUT `/api/v1/picking/tasks/{taskId}/cancel`
- **Request Body**:
  ```json
  {
    "reason": "Order cancelled"
  }
  ```
- **Assertions**:
  - Status: 200 OK
  - Task status changed to CANCELLED
  - PickingTaskCancelledEvent published

#### Test: List Picking Tasks with Status Filter
- **Setup**: Create 3 PENDING, 2 COMPLETED picking tasks
- **Action**: GET `/api/v1/picking/tasks?status=PENDING`
- **Assertions**:
  - Status: 200 OK
  - Response contains only PENDING tasks (3 results)

#### Test: Get Picking Task by ID
- **Setup**: Create picking task
- **Action**: GET `/api/v1/picking/tasks/{taskId}`
- **Assertions**:
  - Status: 200 OK
  - Response contains task details

#### Test: Partial Pick (Short Pick)
- **Setup**: Create task for 100 units, only 80 available
- **Action**: Complete task with partial quantity
- **Assertions**:
  - Status: 200 OK
  - Task marked as PARTIALLY_COMPLETED
  - Shortfall recorded

### Authorization Tests

- **WAREHOUSE_MANAGER**: Can manage all picking tasks
- **PICKER**: Can view and execute assigned tasks only
- **STOCK_CLERK**: Cannot access picking tasks (403 FORBIDDEN)
- **VIEWER**: Can read picking tasks (read-only)

### DTOs

```java
@Data
@Builder
public class CreatePickingTaskRequest {
    private String orderId;
    private List<PickingItem> items;
    private String priority;
    private LocalDate dueDate;
}

@Data
@Builder
public class PickingItem {
    private String productId;
    private Integer quantity;
    private String locationId;
}

@Data
@Builder
public class CreatePickingTaskResponse {
    private String taskId;
    private String status;
    private String orderId;
}
```

---

## 2. Returns Service Tests

### Overview
Validates return order creation, authorization, processing, and restocking workflows.

### Test Scenarios

#### Test: Create Return Order
- **Setup**: Login as TENANT_ADMIN, create product with stock
- **Action**: POST `/api/v1/returns/orders`
- **Request Body**:
  ```json
  {
    "originalOrderId": "ORDER-12345",
    "customerId": "CUST-001",
    "items": [
      {
        "productId": "{productId}",
        "quantity": 10,
        "reason": "DAMAGED",
        "condition": "DAMAGED"
      }
    ],
    "returnDate": "2025-01-20"
  }
  ```
- **Assertions**:
  - Status: 201 CREATED
  - Response contains `returnOrderId`, `status=PENDING_AUTHORIZATION`
  - ReturnOrderCreatedEvent published

#### Test: Authorize Return Order
- **Setup**: Create return order
- **Action**: PUT `/api/v1/returns/orders/{returnOrderId}/authorize`
- **Request Body**:
  ```json
  {
    "authorizedBy": "{userId}",
    "authorizationNotes": "Approved for refund"
  }
  ```
- **Assertions**:
  - Status: 200 OK
  - Return status changed to AUTHORIZED
  - ReturnOrderAuthorizedEvent published

#### Test: Reject Return Order
- **Setup**: Create return order
- **Action**: PUT `/api/v1/returns/orders/{returnOrderId}/reject`
- **Request Body**:
  ```json
  {
    "rejectedBy": "{userId}",
    "rejectionReason": "Outside return window"
  }
  ```
- **Assertions**:
  - Status: 200 OK
  - Return status changed to REJECTED
  - ReturnOrderRejectedEvent published

#### Test: Process Return (Receive Items)
- **Setup**: Authorize return order
- **Action**: PUT `/api/v1/returns/orders/{returnOrderId}/process`
- **Request Body**:
  ```json
  {
    "receivedItems": [
      {
        "productId": "{productId}",
        "quantity": 10,
        "condition": "DAMAGED",
        "locationId": "{returnLocationId}"
      }
    ],
    "processedBy": "{userId}"
  }
  ```
- **Assertions**:
  - Status: 200 OK
  - Return status changed to PROCESSED
  - Stock updated based on condition
  - ReturnOrderProcessedEvent published

#### Test: Restock Returned Item (Good Condition)
- **Setup**: Process return with condition "GOOD"
- **Action**: PUT `/api/v1/returns/orders/{returnOrderId}/restock`
- **Assertions**:
  - Status: 200 OK
  - Item restocked to available inventory
  - Stock level increased

#### Test: Dispose Returned Item (Damaged)
- **Setup**: Process return with condition "DAMAGED"
- **Action**: PUT `/api/v1/returns/orders/{returnOrderId}/dispose`
- **Assertions**:
  - Status: 200 OK
  - Item marked for disposal
  - No stock level increase

#### Test: List Return Orders with Status Filter
- **Setup**: Create 3 PENDING, 2 AUTHORIZED return orders
- **Action**: GET `/api/v1/returns/orders?status=PENDING_AUTHORIZATION`
- **Assertions**:
  - Status: 200 OK
  - Response contains only PENDING return orders

#### Test: Get Return Order by ID
- **Setup**: Create return order
- **Action**: GET `/api/v1/returns/orders/{returnOrderId}`
- **Assertions**:
  - Status: 200 OK
  - Response contains return order details

### Return Reasons and Conditions

**Reasons**: DAMAGED, DEFECTIVE, WRONG_ITEM, UNWANTED, OTHER
**Conditions**: GOOD (restockable), DAMAGED (dispose), DEFECTIVE (inspect)

### Authorization Tests

- **WAREHOUSE_MANAGER**: Can manage all return orders
- **RETURNS_MANAGER**: Can manage all return operations
- **RETURNS_CLERK**: Can process returns, cannot authorize
- **VIEWER**: Can read return orders (read-only)

### DTOs

```java
@Data
@Builder
public class CreateReturnOrderRequest {
    private String originalOrderId;
    private String customerId;
    private List<ReturnItem> items;
    private LocalDate returnDate;
}

@Data
@Builder
public class ReturnItem {
    private String productId;
    private Integer quantity;
    private String reason;
    private String condition;
}

@Data
@Builder
public class CreateReturnOrderResponse {
    private String returnOrderId;
    private String status;
    private String originalOrderId;
}
```

---

## 3. Reconciliation Service Tests

### Overview
Validates stock reconciliation (cycle counts), variance detection, and adjustment workflows.

### Test Scenarios

#### Test: Create Reconciliation Count
- **Setup**: Login as TENANT_ADMIN, create location with stock
- **Action**: POST `/api/v1/reconciliation/counts`
- **Request Body**:
  ```json
  {
    "locationId": "{locationId}",
    "countType": "CYCLE_COUNT",
    "scheduledDate": "2025-01-25",
    "assignedTo": "{userId}"
  }
  ```
- **Assertions**:
  - Status: 201 CREATED
  - Response contains `countId`, `status=SCHEDULED`
  - ReconciliationCountCreatedEvent published

#### Test: Start Reconciliation Count
- **Setup**: Create reconciliation count
- **Action**: PUT `/api/v1/reconciliation/counts/{countId}/start`
- **Assertions**:
  - Status: 200 OK
  - Count status changed to IN_PROGRESS
  - ReconciliationCountStartedEvent published

#### Test: Submit Count Results
- **Setup**: Start reconciliation count
- **Action**: PUT `/api/v1/reconciliation/counts/{countId}/submit`
- **Request Body**:
  ```json
  {
    "countedItems": [
      {
        "productId": "{productId}",
        "batchNumber": "BATCH-001",
        "countedQuantity": 95,
        "systemQuantity": 100
      }
    ],
    "countedBy": "{userId}",
    "countDate": "2025-01-25"
  }
  ```
- **Assertions**:
  - Status: 200 OK
  - Count status changed to COMPLETED
  - Variance calculated (95 vs 100 = -5)
  - ReconciliationCountCompletedEvent published

#### Test: Approve Reconciliation Adjustment
- **Setup**: Submit count with variance
- **Action**: PUT `/api/v1/reconciliation/counts/{countId}/approve`
- **Request Body**:
  ```json
  {
    "approvedBy": "{userId}",
    "adjustmentNotes": "Physical count confirmed"
  }
  ```
- **Assertions**:
  - Status: 200 OK
  - Stock adjusted to match physical count (100 → 95)
  - ReconciliationAdjustmentApprovedEvent published

#### Test: Reject Reconciliation (Recount Required)
- **Setup**: Submit count with large variance
- **Action**: PUT `/api/v1/reconciliation/counts/{countId}/reject`
- **Request Body**:
  ```json
  {
    "rejectedBy": "{userId}",
    "rejectionReason": "Variance too large, recount required"
  }
  ```
- **Assertions**:
  - Status: 200 OK
  - Count status changed to REJECTED
  - No stock adjustment
  - ReconciliationCountRejectedEvent published

#### Test: Detect Variance Exceeding Threshold
- **Setup**: Submit count with variance > 10%
- **Action**: System detects variance
- **Assertions**:
  - Status: 200 OK
  - Variance flagged for review
  - Automatic recount scheduled (optional)

#### Test: List Reconciliation Counts with Status Filter
- **Setup**: Create 3 SCHEDULED, 2 COMPLETED counts
- **Action**: GET `/api/v1/reconciliation/counts?status=SCHEDULED`
- **Assertions**:
  - Status: 200 OK
  - Response contains only SCHEDULED counts

#### Test: Get Reconciliation Count by ID
- **Setup**: Create reconciliation count
- **Action**: GET `/api/v1/reconciliation/counts/{countId}`
- **Assertions**:
  - Status: 200 OK
  - Response contains count details and variance

#### Test: Get Reconciliation History by Location
- **Setup**: Create multiple counts for same location
- **Action**: GET `/api/v1/reconciliation/counts?locationId={locationId}`
- **Assertions**:
  - Status: 200 OK
  - Response contains all counts for location

### Count Types

- **CYCLE_COUNT**: Regular scheduled count
- **FULL_COUNT**: Complete warehouse inventory count
- **SPOT_COUNT**: Random location verification
- **VARIANCE_RECOUNT**: Recount due to discrepancy

### Authorization Tests

- **WAREHOUSE_MANAGER**: Can manage all reconciliation operations
- **RECONCILIATION_MANAGER**: Can manage reconciliation counts
- **RECONCILIATION_CLERK**: Can perform counts, cannot approve adjustments
- **VIEWER**: Can read reconciliation data (read-only)

### DTOs

```java
@Data
@Builder
public class CreateReconciliationCountRequest {
    private String locationId;
    private String countType;
    private LocalDate scheduledDate;
    private String assignedTo;
}

@Data
@Builder
public class CreateReconciliationCountResponse {
    private String countId;
    private String status;
    private String locationId;
}

@Data
@Builder
public class CountedItem {
    private String productId;
    private String batchNumber;
    private Integer countedQuantity;
    private Integer systemQuantity;
    private Integer variance;
}
```

---

## Common Test Patterns

### Setup Requirements for All Services

```java
@BeforeAll
public static void setupTestData() {
    // Login as TENANT_ADMIN
    tenantAdminAuth = loginAsTenantAdmin();
    testTenantId = tenantAdminAuth.getTenantId();

    // Create test dependencies
    testProductId = createTestProduct(tenantAdminAuth);
    testLocationId = createTestLocation(tenantAdminAuth);
    testConsignmentId = createTestConsignment(tenantAdminAuth, testProductId, testLocationId);
}
```

### Tenant Isolation Tests (All Services)

#### Test: TENANT_ADMIN Lists Only Own Tenant Data
- **Setup**: Create data in Tenant A and Tenant B
- **Action**: GET endpoint as TENANT_ADMIN (Tenant A)
- **Assertions**: Only Tenant A data visible

#### Test: TENANT_ADMIN Cannot Access Other Tenant Data
- **Setup**: Create data in Tenant B
- **Action**: GET endpoint as TENANT_ADMIN (Tenant A)
- **Assertions**: 403 FORBIDDEN or 404 NOT FOUND

### Common Query Tests (All Services)

- List with pagination: `?page=0&size=10`
- Filter by status: `?status=PENDING`
- Filter by date range: `?startDate=2025-01-01&endDate=2025-12-31`
- Search: `?search=keyword`

---

## Test Data Builders

### PickingTestDataBuilder

```java
public class PickingTestDataBuilder {
    public static CreatePickingTaskRequest buildCreatePickingTaskRequest(String productId, String locationId, Faker faker) {
        return CreatePickingTaskRequest.builder()
                .orderId("ORDER-" + faker.number().digits(5))
                .items(List.of(
                    PickingItem.builder()
                        .productId(productId)
                        .quantity(faker.number().numberBetween(10, 100))
                        .locationId(locationId)
                        .build()
                ))
                .priority("HIGH")
                .dueDate(LocalDate.now().plusDays(7))
                .build();
    }
}
```

### ReturnsTestDataBuilder

```java
public class ReturnsTestDataBuilder {
    public static CreateReturnOrderRequest buildCreateReturnOrderRequest(String productId, Faker faker) {
        return CreateReturnOrderRequest.builder()
                .originalOrderId("ORDER-" + faker.number().digits(5))
                .customerId("CUST-" + faker.number().digits(3))
                .items(List.of(
                    ReturnItem.builder()
                        .productId(productId)
                        .quantity(faker.number().numberBetween(1, 20))
                        .reason("DAMAGED")
                        .condition("DAMAGED")
                        .build()
                ))
                .returnDate(LocalDate.now())
                .build();
    }
}
```

### ReconciliationTestDataBuilder

```java
public class ReconciliationTestDataBuilder {
    public static CreateReconciliationCountRequest buildCreateReconciliationCountRequest(String locationId, String userId, Faker faker) {
        return CreateReconciliationCountRequest.builder()
                .locationId(locationId)
                .countType("CYCLE_COUNT")
                .scheduledDate(LocalDate.now().plusDays(1))
                .assignedTo(userId)
                .build();
    }
}
```

---

## Rate Limiting

All microservices have rate limits (100 req/min, 200 burst). Tests should:
- Avoid exceeding rate limits in concurrent tests
- Add delays if necessary: `Thread.sleep(100)`
- Test rate limit behavior: Send 150 requests, expect 429 after 100

---

## Event Publishing

All operations publish domain events:
- **Picking**: PickingTaskCreatedEvent, PickingTaskCompletedEvent
- **Returns**: ReturnOrderCreatedEvent, ReturnOrderProcessedEvent
- **Reconciliation**: ReconciliationCountCompletedEvent, ReconciliationAdjustmentApprovedEvent

Consider using:
- Embedded Kafka for event validation
- Mock Kafka consumer to verify events
- Event store query to validate published events

---

## Testing Checklist

### Picking Service
- [ ] Create picking task successfully
- [ ] Assign task to picker
- [ ] Start task changes status to IN_PROGRESS
- [ ] Complete task updates stock
- [ ] Cancel task succeeds
- [ ] List tasks with filters works
- [ ] Partial pick recorded correctly
- [ ] Authorization checks enforced
- [ ] Tenant isolation verified

### Returns Service
- [ ] Create return order successfully
- [ ] Authorize return changes status
- [ ] Reject return changes status
- [ ] Process return updates stock
- [ ] Restock good condition items
- [ ] Dispose damaged items
- [ ] List returns with filters works
- [ ] Authorization checks enforced
- [ ] Tenant isolation verified

### Reconciliation Service
- [ ] Create reconciliation count successfully
- [ ] Start count changes status
- [ ] Submit count calculates variance
- [ ] Approve adjustment updates stock
- [ ] Reject count prevents adjustment
- [ ] Large variance flagged for review
- [ ] List counts with filters works
- [ ] Authorization checks enforced
- [ ] Tenant isolation verified

---

## Next Steps

1. **Implement test classes**: PickingServiceTest, ReturnsServiceTest, ReconciliationServiceTest
2. **Create test data builders** for each service
3. **Create DTO classes** for requests/responses
4. **Test end-to-end workflows** (e.g., pick order → return → reconcile)
5. **Validate event publishing** for all operations
6. **Document test results** and edge cases

---

## Notes

- **Test Execution Order**: Tests should create dependencies (products, locations, stock) in @BeforeAll
- **Cleanup**: Consider cleanup strategy for test data (@AfterEach or @AfterAll)
- **Test Isolation**: Each test should be independent and not rely on previous test state
- **Realistic Scenarios**: Test workflows match real-world warehouse operations
- **Edge Cases**: Test boundary conditions (zero quantity, negative values, etc.)
