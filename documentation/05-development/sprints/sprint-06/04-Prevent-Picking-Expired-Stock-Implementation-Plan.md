# US-2.1.4: Prevent Picking of Expired Stock Implementation Plan

## User Story

**Story ID:** US-2.1.4
**Title:** Prevent Picking of Expired Stock
**Epic:** Stock Classification and Expiration Management
**Service:** Stock Management Service, Picking Service
**Priority:** Must Have
**Story Points:** 3

**As a** warehouse operator
**I want** the system to prevent picking of expired stock
**So that** expired products are never shipped to customers

---

## Acceptance Criteria

- [ ] AC1: System checks stock expiration date before allowing picking
- [ ] AC2: System displays error message when attempting to pick expired stock
- [ ] AC3: System excludes expired stock from picking location queries
- [ ] AC4: System logs attempts to pick expired stock for audit
- [ ] AC5: Frontend displays clear indication when stock is expired
- [ ] AC6: Expired stock cannot be included in picking tasks

---

## Table of Contents

1. [Overview](#overview)
2. [Frontend Implementation](#frontend-implementation)
3. [Backend Implementation](#backend-implementation)
4. [Integration Points](#integration-points)
5. [Testing Strategy](#testing-strategy)
6. [Implementation Checklist](#implementation-checklist)

---

## Overview

### Business Context

Preventing expired stock from being picked is critical for:

1. **Food Safety** - Fundamental requirement to prevent health hazards
2. **Regulatory Compliance** - Legal obligation to prevent expired products
3. **Brand Protection** - Avoid reputation damage from expired products
4. **Customer Safety** - Ensure customer wellbeing

### Implementation Strategy

1. **Query-Level Filtering** - Exclude expired stock from picking location queries
2. **Validation at Execution** - Double-check expiration before executing picking
3. **UI Indicators** - Clear visual warnings for expired stock
4. **Audit Logging** - Log any attempts to pick expired stock

---

## Frontend Implementation

### UI Enhancement: Expired Stock Warning

**Location:** Update `PickingTaskExecutionPage.tsx`

```typescript
// Add expiration validation
const validateStockExpiration = (task: PickingTask): boolean => {
  if (!task.expirationDate) {
    return true; // No expiration date, allow picking
  }

  const expDate = new Date(task.expirationDate);
  const today = new Date();

  return expDate >= today;
};

// In the component render
{!validateStockExpiration(pickingTask) && (
  <Alert
    type="error"
    message="⚠️ EXPIRED STOCK - This stock has expired and cannot be picked"
    className="mb-6"
  />
)}

<Button
  variant="primary"
  onClick={handleExecutePicking}
  disabled={
    !validateStockExpiration(pickingTask) ||
    executionLoading ||
    pickedQuantity <= 0
  }
  loading={executionLoading}
>
  {validateStockExpiration(pickingTask)
    ? 'Complete Picking'
    : 'Cannot Pick Expired Stock'}
</Button>
```

### UI Component: ExpiredStockIndicator

```typescript
import React from 'react';

interface ExpiredStockIndicatorProps {
  expirationDate: string;
  showDetails?: boolean;
}

export const ExpiredStockIndicator: React.FC<ExpiredStockIndicatorProps> = ({
  expirationDate,
  showDetails = true
}) => {
  const expDate = new Date(expirationDate);
  const today = new Date();
  const isExpired = expDate < today;

  if (!isExpired) {
    return null;
  }

  const daysExpired = Math.floor(
    (today.getTime() - expDate.getTime()) / (1000 * 60 * 60 * 24)
  );

  return (
    <div className="bg-red-100 border-2 border-red-600 rounded-lg p-4">
      <div className="flex items-center gap-3">
        <span className="text-4xl">❌</span>
        <div>
          <p className="text-red-900 font-bold text-lg">EXPIRED STOCK</p>
          {showDetails && (
            <>
              <p className="text-red-800 text-sm mt-1">
                Expired: {expDate.toLocaleDateString()}
              </p>
              <p className="text-red-800 text-sm">
                {daysExpired} day{daysExpired !== 1 ? 's' : ''} past expiration
              </p>
              <p className="text-red-900 font-medium text-sm mt-2">
                ⛔ This stock cannot be picked or shipped
              </p>
            </>
          )}
        </div>
      </div>
    </div>
  );
};
```

---

## Backend Implementation

### Validation in Picking Service

**Enhanced ExecutePickingTaskCommandHandler:**

```java
private void validateStockExpiration(PickingTask pickingTask, int pickedQuantity) {
    // Query stock management service for stock expiration status
    boolean stockExpired = stockManagementService.isStockExpired(
            pickingTask.getProductId(),
            pickingTask.getLocationId()
    );

    if (stockExpired) {
        // Log the attempt for audit
        auditLogger.logExpiredStockPickingAttempt(
                pickingTask.getId(),
                pickingTask.getProductId(),
                pickingTask.getLocationId(),
                SecurityContextHolder.getContext().getAuthentication().getName()
        );

        throw new ExpiredStockException(
                String.format("Cannot pick expired stock for product %s at location %s",
                        pickingTask.getProductId(),
                        pickingTask.getLocationId())
        );
    }
}
```

### Query Filtering in Stock Management Service

**Enhanced GetFEFOStockItemsQueryHandler:**

```java
@Component
@RequiredArgsConstructor
public class GetFEFOStockItemsQueryHandler {

    private final StockItemViewRepository stockItemViewRepository;

    public List<StockItemView> handle(GetFEFOStockItemsQuery query) {
        // Query stock items sorted by expiration date (FEFO)
        List<StockItemView> stockItems = stockItemViewRepository
                .findByProductIdOrderByExpirationDateAsc(query.getProductId());

        // Filter out expired stock
        LocalDate today = LocalDate.now();

        return stockItems.stream()
                .filter(item -> item.getExpirationDate() == null ||
                               item.getExpirationDate().isAfter(today))
                .filter(item -> item.getClassification() != StockClassification.EXPIRED)
                .collect(Collectors.toList());
    }
}
```

### Audit Logging

**Location:** `stock-management-application-service/src/main/java/com/ccbsa/wms/stock/application/service/audit/ExpiredStockAuditLogger.java`

```java
package com.ccbsa.wms.stock.application.service.audit;

import com.ccbsa.wms.stock.domain.core.valueobject.LocationId;
import com.ccbsa.wms.stock.domain.core.valueobject.PickingTaskId;
import com.ccbsa.wms.stock.domain.core.valueobject.ProductId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExpiredStockAuditLogger {

    private final AuditLogRepository auditLogRepository;

    public void logExpiredStockPickingAttempt(
            PickingTaskId pickingTaskId,
            ProductId productId,
            LocationId locationId,
            String username) {

        log.warn("AUDIT: Attempt to pick expired stock - User: {}, Product: {}, Location: {}, Task: {}",
                username, productId, locationId, pickingTaskId);

        AuditLog auditLog = AuditLog.builder()
                .eventType("EXPIRED_STOCK_PICKING_ATTEMPT")
                .eventDescription(String.format(
                        "User %s attempted to pick expired stock for product %s at location %s",
                        username, productId, locationId))
                .userId(username)
                .pickingTaskId(pickingTaskId.getValue())
                .productId(productId.getValue())
                .locationId(locationId.getValue())
                .timestamp(LocalDateTime.now())
                .severity("WARNING")
                .build();

        auditLogRepository.save(auditLog);
    }
}
```

---

## Integration Points

### Stock Management Service API

**Endpoint:** `GET /api/v1/stock/stock-items/check-expiration`

```java
@GetMapping("/stock-items/check-expiration")
public ResponseEntity<ApiResponse<StockExpirationCheckResponse>> checkStockExpiration(
        @RequestParam UUID productId,
        @RequestParam UUID locationId) {

    log.debug("Checking stock expiration for product: {}, location: {}",
            productId, locationId);

    StockExpirationCheckQuery query = StockExpirationCheckQuery.builder()
            .productId(ProductId.of(productId))
            .locationId(LocationId.of(locationId))
            .build();

    StockExpirationCheckResponse result = checkStockExpirationQueryHandler.handle(query);

    return ResponseEntity.ok(ApiResponseBuilder.ok(result));
}
```

**Response DTO:**

```java
@Value
@Builder
public class StockExpirationCheckResponse {
    boolean expired;
    LocalDate expirationDate;
    StockClassification classification;
    int daysUntilExpiration;
    String message;
}
```

### Database Index for Performance

**Migration Script:**

```sql
-- Add index for expiration date queries
CREATE INDEX idx_stock_items_expiration_classification 
ON stock_items(expiration_date, classification) 
WHERE classification != 'EXPIRED';

-- Add index for product/location lookups
CREATE INDEX idx_stock_items_product_location_expiration
ON stock_items(product_id, location_id, expiration_date);
```

---

## Testing Strategy

### Unit Tests - Domain Core

```java
@Test
void shouldPreventPickingExpiredStock() {
    // Given
    LocalDate expiredDate = LocalDate.now().minusDays(1);
    StockItem stockItem = createStockItemWithExpiration(expiredDate);
    stockItem.checkExpiration();

    // When/Then
    assertThat(stockItem.canBePicked()).isFalse();
    assertThat(stockItem.getClassification()).isEqualTo(StockClassification.EXPIRED);
}

@Test
void shouldAllowPickingNonExpiredStock() {
    // Given
    LocalDate futureDate = LocalDate.now().plusDays(10);
    StockItem stockItem = createStockItemWithExpiration(futureDate);
    stockItem.checkExpiration();

    // When/Then
    assertThat(stockItem.canBePicked()).isTrue();
    assertThat(stockItem.getClassification()).isNotEqualTo(StockClassification.EXPIRED);
}
```

### Integration Tests - Picking Service

```java
@Test
void shouldRejectPickingExpiredStock() {
    // Given: Stock item that has expired
    UUID productId = createProductWithExpiredStock();
    UUID pickingTaskId = createPickingTaskForProduct(productId);

    ExecutePickingTaskRequest request = ExecutePickingTaskRequest.builder()
            .pickedQuantity(10)
            .isPartialPicking(false)
            .build();

    // When/Then: Attempt to pick
    given()
            .spec(requestSpec)
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .post("/api/v1/picking/picking-tasks/{pickingTaskId}/execute", pickingTaskId)
            .then()
            .statusCode(400)
            .body("success", is(false))
            .body("error.code", equalTo("EXPIRED_STOCK"))
            .body("error.message", containsString("Cannot pick expired stock"));
}

@Test
void shouldExcludeExpiredStockFromFEFOQuery() {
    // Given: Mix of expired and non-expired stock
    UUID productId = UUID.randomUUID();
    createStockItem(productId, LocalDate.now().minusDays(1)); // Expired
    createStockItem(productId, LocalDate.now().plusDays(10)); // Valid
    createStockItem(productId, LocalDate.now().plusDays(20)); // Valid

    // When: Query FEFO stock
    List<StockItemView> result = given()
            .spec(requestSpec)
            .queryParam("productId", productId)
            .when()
            .get("/api/v1/stock/stock-items/fefo")
            .then()
            .statusCode(200)
            .extract()
            .jsonPath()
            .getList("data", StockItemView.class);

    // Then: Should only return non-expired stock
    assertThat(result).hasSize(2);
    assertThat(result).noneMatch(item ->
            item.getClassification() == StockClassification.EXPIRED);
}
```

### Frontend Tests

```typescript
describe('Expired Stock Prevention', () => {
  it('should disable picking button for expired stock', () => {
    const expiredTask = createMockPickingTask({
      expirationDate: new Date(Date.now() - 86400000).toISOString() // Yesterday
    });

    render(<PickingTaskExecutionPage task={expiredTask} />);

    const pickButton = screen.getByRole('button', { name: /pick/i });
    expect(pickButton).toBeDisabled();
  });

  it('should show expired stock warning', () => {
    const expiredTask = createMockPickingTask({
      expirationDate: new Date(Date.now() - 86400000).toISOString()
    });

    render(<PickingTaskExecutionPage task={expiredTask} />);

    expect(screen.getByText(/expired stock/i)).toBeInTheDocument();
    expect(screen.getByText(/cannot be picked/i)).toBeInTheDocument();
  });
});
```

---

## Implementation Checklist

### Frontend

- [ ] Add expired stock validation to `PickingTaskExecutionPage`
- [ ] Create `ExpiredStockIndicator` component
- [ ] Disable picking button for expired stock
- [ ] Show clear error messages for expired stock
- [ ] Add visual indicators (red borders, warning icons)
- [ ] Test UI with expired stock scenarios

### Backend - Stock Management Service

- [ ] Add `canBePicked()` method to `StockItem` domain model
- [ ] Filter expired stock in FEFO queries
- [ ] Create `StockExpirationCheckQuery` and handler
- [ ] Add expiration check API endpoint
- [ ] Write unit tests for expiration validation

### Backend - Picking Service

- [ ] Add expiration validation in `ExecutePickingTaskCommandHandler`
- [ ] Create `ExpiredStockException`
- [ ] Query Stock Management Service for expiration status
- [ ] Write unit tests for picking validation

### Audit Logging

- [ ] Create `ExpiredStockAuditLogger`
- [ ] Create `AuditLog` entity and repository
- [ ] Log all attempts to pick expired stock
- [ ] Include user, timestamp, product, and location
- [ ] Test audit log creation

### Database

- [ ] Add indexes for expiration queries
- [ ] Add audit_logs table migration
- [ ] Optimize query performance
- [ ] Test query execution plans

### Integration Tests

- [ ] Test picking rejection for expired stock
- [ ] Test FEFO query excludes expired stock
- [ ] Test audit logging
- [ ] Test end-to-end workflow

### Documentation

- [ ] Document expiration validation logic
- [ ] Document audit logging process
- [ ] Update API documentation
- [ ] Create troubleshooting guide

---

## Error Handling

### Error Codes

| Code                | Message                     | HTTP Status   | Action                         |
|---------------------|-----------------------------|---------------|--------------------------------|
| EXPIRED_STOCK       | Cannot pick expired stock   | 400           | Display error, prevent picking |
| STOCK_EXPIRING_SOON | Stock expires within 7 days | 200 (Warning) | Allow picking, show warning    |

### Error Response Format

```json
{
  "success": false,
  "error": {
    "code": "EXPIRED_STOCK",
    "message": "Cannot pick expired stock for product P12345 at location L-A-01",
    "details": {
      "productId": "P12345",
      "locationId": "L-A-01",
      "expirationDate": "2026-01-05",
      "daysExpired": 3
    }
  },
  "timestamp": "2026-01-08T10:30:00Z"
}
```

---

## Performance Considerations

### Query Optimization

1. **Database Indexes** - Index on (expiration_date, classification)
2. **Query Filtering** - Filter expired stock at database level
3. **Caching** - Cache expiration check results for 5 minutes
4. **Batch Processing** - Process expiration checks in batches

### Monitoring

- **Metrics:**
    - Count of expired stock picking attempts
    - Average expiration check query time
    - Number of expired stock items in system

- **Alerts:**
    - Alert when expired stock picking attempts exceed threshold
    - Alert when query performance degrades

---

## Definition of Done

- [ ] All acceptance criteria met
- [ ] Frontend prevents picking expired stock
- [ ] Backend validates expiration before picking
- [ ] Queries exclude expired stock automatically
- [ ] Audit logging captures all expired stock attempts
- [ ] Unit tests pass (>80% coverage)
- [ ] Integration tests pass
- [ ] Performance tests pass
- [ ] Documentation updated
- [ ] Code reviewed and approved

---

**Document Version:** 1.0  
**Last Updated:** 2026-01-08  
**Status:** Ready for Implementation
