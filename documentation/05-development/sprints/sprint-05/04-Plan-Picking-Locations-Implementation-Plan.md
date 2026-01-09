# Plan Picking Locations Implementation Plan

## US-6.2.1: Plan Picking Locations

**Service:** Picking Service, Stock Management Service
**Priority:** Must Have
**Story Points:** 13
**Sprint:** Sprint 5

---

## Table of Contents

1. [Overview](#overview)
2. [FEFO Algorithm Design](#fefo-algorithm-design)
3. [Domain Model Design](#domain-model-design)
4. [Backend Implementation](#backend-implementation)
5. [Service Integration](#service-integration)
6. [UI Design](#ui-design)
7. [Testing Strategy](#testing-strategy)
8. [Acceptance Criteria Validation](#acceptance-criteria-validation)

---

## Overview

### User Story

**As a** warehouse operator
**I want** picking locations to be optimized based on FEFO principles
**So that** I can efficiently pick stock with earliest expiration dates

### Business Requirements

- System optimizes picking locations based on FEFO (First Expiring First Out) principles
- System considers location proximity to minimize travel time
- System generates picking sequence/route suggestions
- System creates `PickingTask` entities for each location/product combination
- System publishes `LoadPlannedEvent` and `PickingTaskCreatedEvent`
- System excludes expired stock from picking locations

### Technical Requirements

- Follow DDD, Clean Hexagonal Architecture, CQRS, Event-Driven Choreography
- Optimize database queries for FEFO sorting
- Implement efficient location proximity algorithm
- Handle concurrent planning requests
- Multi-tenant support
- Complete audit trail of picking tasks

---

## FEFO Algorithm Design

### FEFO Planning Algorithm

**Objectives:**

1. Pick stock with earliest expiration date first (FEFO)
2. Minimize warehouse travel distance
3. Maximize picking efficiency
4. Prevent picking of expired stock

**Algorithm Steps:**

```
For each Order in Load:
  For each LineItem in Order:
    1. Query Stock Management Service for available stock:
       - Product = LineItem.productCode
       - Status = AVAILABLE
       - Classification != EXPIRED
       - AvailableQuantity > 0 (total - allocated)
       - Sort by: expirationDate ASC, location proximity

    2. Allocate stock using FEFO:
       remainingQuantity = LineItem.quantity
       selectedStockItems = []

       For each StockItem (sorted by expiration):
         if remainingQuantity == 0:
           break

         allocateQuantity = min(StockItem.availableQuantity, remainingQuantity)
         selectedStockItems.add({
           stockItemId: StockItem.id,
           locationId: StockItem.locationId,
           quantity: allocateQuantity,
           expirationDate: StockItem.expirationDate
         })
         remainingQuantity -= allocateQuantity

       if remainingQuantity > 0:
         raise InsufficientStockException

    3. Create PickingTasks from selected stock items

    4. Optimize picking sequence by location proximity

5. Update Load status to PLANNED
6. Publish LoadPlannedEvent
7. Publish PickingTaskCreatedEvent for each task
8. Trigger Stock Allocation (StockAllocatedEvent)
```

### Location Proximity Algorithm

**Simple Distance Calculation:**

```java
/**
 * Calculate proximity score based on warehouse zones
 * Lower score = closer to picking zone
 */
public int calculateProximityScore(Location location) {
    // Zone priority (closer to shipping = higher priority)
    Map<String, Integer> zonePriority = Map.of(
        "PICKING_ZONE", 1,
        "BULK_STORAGE", 2,
        "OVERFLOW", 3,
        "RECEIVING", 4
    );

    int baseScore = zonePriority.getOrDefault(location.getZone(), 5) * 1000;

    // Add aisle distance (assuming linear warehouse layout)
    int aisleDistance = Math.abs(location.getAisle() - PICKING_AISLE) * 10;

    // Add rack/level distance
    int rackDistance = Math.abs(location.getRack()) + Math.abs(location.getLevel());

    return baseScore + aisleDistance + rackDistance;
}
```

**Advanced: Traveling Salesman Problem (TSP) for Route Optimization**

For future enhancement, implement TSP for optimal picking route:

- Nearest Neighbor algorithm
- 2-Opt optimization
- Genetic algorithm for larger loads

---

## Domain Model Design

### PickingTask Entity

```java
package com.ccbsa.wms.picking.domain.core.entity;

import com.ccbsa.common.domain.entity.BaseEntity;
import com.ccbsa.common.domain.valueobject.*;
import com.ccbsa.wms.picking.domain.core.valueobject.*;

import java.time.ZonedDateTime;
import java.util.Objects;

public class PickingTask extends BaseEntity<PickingTaskId> {
    private final LoadId loadId;
    private final OrderId orderId;
    private final OrderLineItemId orderLineItemId;
    private final ProductCode productCode;
    private final LocationId locationId;
    private final Quantity quantity;
    private Quantity pickedQuantity;
    private PickingTaskStatus status;
    private final int sequence; // Picking order based on location proximity
    private final ZonedDateTime createdAt;
    private ZonedDateTime completedAt;

    private PickingTask(Builder builder) {
        super.setId(builder.pickingTaskId);
        this.loadId = builder.loadId;
        this.orderId = builder.orderId;
        this.orderLineItemId = builder.orderLineItemId;
        this.productCode = builder.productCode;
        this.locationId = builder.locationId;
        this.quantity = builder.quantity;
        this.pickedQuantity = builder.pickedQuantity;
        this.status = builder.status;
        this.sequence = builder.sequence;
        this.createdAt = builder.createdAt;
        this.completedAt = builder.completedAt;
    }

    public void validatePickingTask() {
        if (productCode == null) {
            throw new IllegalStateException("Picking task must have a product code");
        }
        if (locationId == null) {
            throw new IllegalStateException("Picking task must have a location");
        }
        if (quantity == null || !quantity.isGreaterThanZero()) {
            throw new IllegalStateException("Picking task quantity must be greater than zero");
        }
    }

    public void startPicking() {
        if (status != PickingTaskStatus.PENDING) {
            throw new IllegalStateException("Can only start picking from PENDING status");
        }
        this.status = PickingTaskStatus.IN_PROGRESS;
    }

    public void completePicking(Quantity picked) {
        if (status != PickingTaskStatus.IN_PROGRESS) {
            throw new IllegalStateException("Can only complete picking from IN_PROGRESS status");
        }
        if (picked.isGreaterThan(quantity)) {
            throw new IllegalStateException("Picked quantity cannot exceed task quantity");
        }

        this.pickedQuantity = picked;

        if (picked.equals(quantity)) {
            this.status = PickingTaskStatus.COMPLETED;
        } else {
            this.status = PickingTaskStatus.PARTIALLY_COMPLETED;
        }

        this.completedAt = ZonedDateTime.now();
    }

    public boolean isCompleted() {
        return status == PickingTaskStatus.COMPLETED;
    }

    public Quantity getRemainingQuantity() {
        if (pickedQuantity == null) {
            return quantity;
        }
        return quantity.subtract(pickedQuantity);
    }

    // Getters
    public LoadId getLoadId() { return loadId; }
    public OrderId getOrderId() { return orderId; }
    public OrderLineItemId getOrderLineItemId() { return orderLineItemId; }
    public ProductCode getProductCode() { return productCode; }
    public LocationId getLocationId() { return locationId; }
    public Quantity getQuantity() { return quantity; }
    public Quantity getPickedQuantity() { return pickedQuantity; }
    public PickingTaskStatus getStatus() { return status; }
    public int getSequence() { return sequence; }
    public ZonedDateTime getCreatedAt() { return createdAt; }
    public ZonedDateTime getCompletedAt() { return completedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PickingTask that = (PickingTask) o;
        return Objects.equals(getId(), that.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private PickingTaskId pickingTaskId;
        private LoadId loadId;
        private OrderId orderId;
        private OrderLineItemId orderLineItemId;
        private ProductCode productCode;
        private LocationId locationId;
        private Quantity quantity;
        private Quantity pickedQuantity;
        private PickingTaskStatus status;
        private int sequence;
        private ZonedDateTime createdAt;
        private ZonedDateTime completedAt;

        private Builder() {}

        public Builder pickingTaskId(PickingTaskId val) {
            pickingTaskId = val;
            return this;
        }

        public Builder loadId(LoadId val) {
            loadId = val;
            return this;
        }

        public Builder orderId(OrderId val) {
            orderId = val;
            return this;
        }

        public Builder orderLineItemId(OrderLineItemId val) {
            orderLineItemId = val;
            return this;
        }

        public Builder productCode(ProductCode val) {
            productCode = val;
            return this;
        }

        public Builder locationId(LocationId val) {
            locationId = val;
            return this;
        }

        public Builder quantity(Quantity val) {
            quantity = val;
            return this;
        }

        public Builder pickedQuantity(Quantity val) {
            pickedQuantity = val;
            return this;
        }

        public Builder status(PickingTaskStatus val) {
            status = val;
            return this;
        }

        public Builder sequence(int val) {
            sequence = val;
            return this;
        }

        public Builder createdAt(ZonedDateTime val) {
            createdAt = val;
            return this;
        }

        public Builder completedAt(ZonedDateTime val) {
            completedAt = val;
            return this;
        }

        public PickingTask build() {
            return new PickingTask(this);
        }
    }
}
```

### PickingTaskStatus Enum

```java
package com.ccbsa.wms.picking.domain.core.valueobject;

public enum PickingTaskStatus {
    PENDING("Pending"),
    IN_PROGRESS("In Progress"),
    PARTIALLY_COMPLETED("Partially Completed"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled");

    private final String displayName;

    PickingTaskStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isTerminal() {
        return this == COMPLETED || this == CANCELLED;
    }
}
```

### Domain Events

**LoadPlannedEvent**

```java
package com.ccbsa.wms.picking.domain.core.event;

import com.ccbsa.common.domain.event.DomainEvent;
import com.ccbsa.wms.picking.domain.core.entity.Load;
import lombok.Getter;

import java.time.ZonedDateTime;

@Getter
public class LoadPlannedEvent extends DomainEvent<Load> {
    private final Load load;

    public LoadPlannedEvent(Load load) {
        super(load, ZonedDateTime.now());
        this.load = load;
    }
}
```

**PickingTaskCreatedEvent**

```java
package com.ccbsa.wms.picking.domain.core.event;

import com.ccbsa.common.domain.event.DomainEvent;
import com.ccbsa.wms.picking.domain.core.entity.PickingTask;
import lombok.Getter;

import java.time.ZonedDateTime;
import java.util.List;

@Getter
public class PickingTaskCreatedEvent extends DomainEvent<List<PickingTask>> {
    private final List<PickingTask> pickingTasks;

    public PickingTaskCreatedEvent(List<PickingTask> pickingTasks) {
        super(pickingTasks, ZonedDateTime.now());
        this.pickingTasks = pickingTasks;
    }
}
```

---

## Backend Implementation

### Application Service Layer

**PlanPickingLocationsCommandHandler**

```java
package com.ccbsa.wms.picking.application.service.command;

import com.ccbsa.common.domain.valueobject.*;
import com.ccbsa.wms.picking.application.service.port.data.*;
import com.ccbsa.wms.picking.application.service.port.event.PickingEventPublisher;
import com.ccbsa.wms.picking.application.service.port.service.*;
import com.ccbsa.wms.picking.domain.core.entity.*;
import com.ccbsa.wms.picking.domain.core.event.*;
import com.ccbsa.wms.picking.domain.core.exception.InsufficientStockException;
import com.ccbsa.wms.picking.domain.core.valueobject.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class PlanPickingLocationsCommandHandler {
    private final LoadRepository loadRepository;
    private final PickingTaskRepository pickingTaskRepository;
    private final StockManagementServicePort stockManagementServicePort;
    private final LocationManagementServicePort locationManagementServicePort;
    private final PickingEventPublisher pickingEventPublisher;

    @Transactional
    public PlanPickingLocationsResponse handle(PlanPickingLocationsCommand command) {
        log.info("Planning picking locations for load: {}", command.getLoadId());

        // Load the load aggregate
        Load load = loadRepository.findById(command.getLoadId(), command.getTenantId())
                .orElseThrow(() -> new IllegalArgumentException("Load not found"));

        // Validate load status
        if (load.getStatus() != LoadStatus.PENDING) {
            throw new IllegalStateException("Can only plan pending loads");
        }

        // Plan picking tasks for all orders
        List<PickingTask> allPickingTasks = new ArrayList<>();
        int sequenceNumber = 1;

        for (Order order : load.getOrders()) {
            for (OrderLineItem lineItem : order.getLineItems()) {
                // Query available stock items sorted by FEFO
                List<AvailableStockItem> availableStock = queryAvailableStockFEFO(
                        command.getTenantId(),
                        lineItem.getProductCode(),
                        lineItem.getQuantity()
                );

                // Allocate stock and create picking tasks
                List<PickingTask> lineItemTasks = allocateStockAndCreateTasks(
                        load.getId(),
                        order.getId(),
                        lineItem,
                        availableStock,
                        sequenceNumber
                );

                allPickingTasks.addAll(lineItemTasks);
                sequenceNumber += lineItemTasks.size();
            }

            // Update order status to PLANNED
            order.updateStatus(OrderStatus.PLANNED);
        }

        // Optimize picking sequence by location proximity
        optimizePickingSequence(allPickingTasks);

        // Save picking tasks
        pickingTaskRepository.saveAll(allPickingTasks);

        // Update load status to PLANNED
        load.updateStatus(LoadStatus.PLANNED);
        load.setPlannedAt(ZonedDateTime.now());
        loadRepository.save(load);

        // Publish domain events
        pickingEventPublisher.publishLoadPlannedEvent(load);
        pickingEventPublisher.publishPickingTaskCreatedEvent(allPickingTasks);

        log.info("Successfully planned {} picking tasks for load: {}",
                allPickingTasks.size(), load.getLoadNumber());

        return PlanPickingLocationsResponse.builder()
                .loadId(load.getId().getValue())
                .loadNumber(load.getLoadNumber().getValue())
                .totalPickingTasks(allPickingTasks.size())
                .message("Load planned successfully with optimized picking sequence")
                .build();
    }

    private List<AvailableStockItem> queryAvailableStockFEFO(
            TenantId tenantId,
            ProductCode productCode,
            Quantity requiredQuantity
    ) {
        log.debug("Querying available stock for product: {}, quantity: {}",
                productCode.getValue(), requiredQuantity.getValue());

        // Query Stock Management Service for available stock
        // Sorted by: expirationDate ASC (FEFO), then location proximity
        List<AvailableStockItem> availableStock =
                stockManagementServicePort.queryAvailableStockFEFO(
                        tenantId,
                        productCode,
                        requiredQuantity
                );

        if (availableStock.isEmpty()) {
            throw new InsufficientStockException(
                    "No available stock found for product: " + productCode.getValue()
            );
        }

        // Calculate total available quantity
        BigDecimal totalAvailable = availableStock.stream()
                .map(item -> item.getAvailableQuantity().getValue())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalAvailable.compareTo(requiredQuantity.getValue()) < 0) {
            throw new InsufficientStockException(
                    String.format("Insufficient stock for product: %s. Required: %s, Available: %s",
                            productCode.getValue(),
                            requiredQuantity.getValue(),
                            totalAvailable)
            );
        }

        return availableStock;
    }

    private List<PickingTask> allocateStockAndCreateTasks(
            LoadId loadId,
            OrderId orderId,
            OrderLineItem lineItem,
            List<AvailableStockItem> availableStock,
            int startingSequence
    ) {
        List<PickingTask> pickingTasks = new ArrayList<>();
        BigDecimal remainingQuantity = lineItem.getQuantity().getValue();
        int sequence = startingSequence;

        for (AvailableStockItem stockItem : availableStock) {
            if (remainingQuantity.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }

            // Allocate quantity: min of available and remaining
            BigDecimal allocateQuantity = stockItem.getAvailableQuantity().getValue()
                    .min(remainingQuantity);

            // Create picking task
            PickingTask pickingTask = PickingTask.builder()
                    .pickingTaskId(PickingTaskId.newId())
                    .loadId(loadId)
                    .orderId(orderId)
                    .orderLineItemId(lineItem.getId())
                    .productCode(lineItem.getProductCode())
                    .locationId(stockItem.getLocationId())
                    .quantity(new Quantity(allocateQuantity))
                    .status(PickingTaskStatus.PENDING)
                    .sequence(sequence)
                    .createdAt(ZonedDateTime.now())
                    .build();

            pickingTask.validatePickingTask();
            pickingTasks.add(pickingTask);

            remainingQuantity = remainingQuantity.subtract(allocateQuantity);
            sequence++;
        }

        return pickingTasks;
    }

    private void optimizePickingSequence(List<PickingTask> pickingTasks) {
        log.debug("Optimizing picking sequence for {} tasks", pickingTasks.size());

        // Sort picking tasks by location proximity
        // This is a simple greedy algorithm - can be improved with TSP
        pickingTasks.sort((task1, task2) -> {
            int proximity1 = getLocationProximityScore(task1.getLocationId());
            int proximity2 = getLocationProximityScore(task2.getLocationId());
            return Integer.compare(proximity1, proximity2);
        });

        // Update sequence numbers after sorting
        for (int i = 0; i < pickingTasks.size(); i++) {
            pickingTasks.get(i).setSequence(i + 1);
        }
    }

    private int getLocationProximityScore(LocationId locationId) {
        // Query Location Management Service for location details
        try {
            return locationManagementServicePort.calculateLocationProximity(locationId);
        } catch (Exception e) {
            log.warn("Failed to calculate proximity for location: {}, using default",
                    locationId.getValue());
            return Integer.MAX_VALUE; // Put failed lookups at end
        }
    }
}
```

---

## Service Integration

### Stock Management Service Port

**StockManagementServicePort**

```java
package com.ccbsa.wms.picking.application.service.port.service;

import com.ccbsa.common.domain.valueobject.*;

import java.util.List;

public interface StockManagementServicePort {
    /**
     * Query available stock items sorted by FEFO (First Expiring First Out)
     * Excludes expired stock and allocates only available quantity
     *
     * @param tenantId Tenant identifier
     * @param productCode Product code to query
     * @param requiredQuantity Minimum required quantity
     * @return List of available stock items sorted by expiration date (earliest first)
     */
    List<AvailableStockItem> queryAvailableStockFEFO(
            TenantId tenantId,
            ProductCode productCode,
            Quantity requiredQuantity
    );
}
```

**AvailableStockItem DTO**

```java
package com.ccbsa.wms.picking.application.service.port.service;

import com.ccbsa.common.domain.valueobject.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class AvailableStockItem {
    private final UUID stockItemId;
    private final ProductCode productCode;
    private final LocationId locationId;
    private final Quantity totalQuantity;
    private final Quantity allocatedQuantity;
    private final Quantity availableQuantity; // total - allocated
    private final LocalDate expirationDate;
    private final String classification; // NORMAL, NEAR_EXPIRY, CRITICAL
}
```

**StockManagementServiceAdapter** (Infrastructure)

```java
package com.ccbsa.wms.picking.dataaccess.adapter;

import com.ccbsa.common.domain.valueobject.*;
import com.ccbsa.wms.picking.application.service.port.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockManagementServiceAdapter implements StockManagementServicePort {
    private final RestTemplate restTemplate;
    private static final String STOCK_SERVICE_URL = "http://stock-management-service";

    @Override
    public List<AvailableStockItem> queryAvailableStockFEFO(
            TenantId tenantId,
            ProductCode productCode,
            Quantity requiredQuantity
    ) {
        log.debug("Querying stock service for product: {}, tenant: {}",
                productCode.getValue(), tenantId.getValue());

        try {
            String url = String.format(
                    "%s/api/v1/stock-management/stock-items/available-fefo?productCode=%s&requiredQuantity=%s",
                    STOCK_SERVICE_URL,
                    productCode.getValue(),
                    requiredQuantity.getValue()
            );

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Tenant-Id", tenantId.getValue().toString());

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<List<AvailableStockItem>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<List<AvailableStockItem>>() {}
            );

            List<AvailableStockItem> stockItems = response.getBody();

            log.debug("Retrieved {} available stock items for product: {}",
                    stockItems != null ? stockItems.size() : 0, productCode.getValue());

            return stockItems != null ? stockItems : List.of();

        } catch (Exception e) {
            log.error("Failed to query available stock from stock service", e);
            throw new RuntimeException("Stock service unavailable: " + e.getMessage(), e);
        }
    }
}
```

### Location Management Service Port

**LocationManagementServicePort**

```java
package com.ccbsa.wms.picking.application.service.port.service;

import com.ccbsa.common.domain.valueobject.LocationId;

public interface LocationManagementServicePort {
    /**
     * Calculate location proximity score
     * Lower score means closer to picking zone
     *
     * @param locationId Location identifier
     * @return Proximity score (lower is better)
     */
    int calculateLocationProximity(LocationId locationId);
}
```

---

## UI Design

### Picking Task List View

```typescript
<Card>
  <CardHeader title={`Picking Tasks (${pickingTasks.length})`} />
  <CardContent>
    <TableContainer>
      <Table>
        <TableHead>
          <TableRow>
            <TableCell>Sequence</TableCell>
            <TableCell>Product</TableCell>
            <TableCell>Location</TableCell>
            <TableCell align="right">Quantity</TableCell>
            <TableCell>Expiration</TableCell>
            <TableCell>Status</TableCell>
            <TableCell>Actions</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {pickingTasks.map((task) => (
            <TableRow
              key={task.id}
              sx={{
                backgroundColor: task.sequence <= currentTask ? 'action.hover' : 'inherit'
              }}
            >
              <TableCell>
                <Chip label={task.sequence} color="primary" size="small" />
              </TableCell>
              <TableCell>
                <Typography variant="body2">{task.productCode}</Typography>
                <Typography variant="caption" color="textSecondary">
                  {task.productDescription}
                </Typography>
              </TableCell>
              <TableCell>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                  <LocationOnIcon fontSize="small" />
                  <Typography variant="body2">{task.locationCode}</Typography>
                </Box>
                <Typography variant="caption" color="textSecondary">
                  {task.locationDescription}
                </Typography>
              </TableCell>
              <TableCell align="right">
                <Typography variant="body2">{task.quantity}</Typography>
              </TableCell>
              <TableCell>
                {task.expirationDate ? (
                  <Box>
                    <Typography variant="body2">
                      {formatDate(task.expirationDate)}
                    </Typography>
                    <Chip
                      label={task.classification}
                      color={getClassificationColor(task.classification)}
                      size="small"
                    />
                  </Box>
                ) : (
                  <Typography variant="caption" color="textSecondary">
                    Non-perishable
                  </Typography>
                )}
              </TableCell>
              <TableCell>
                <StatusBadge status={task.status} />
              </TableCell>
              <TableCell>
                <IconButton
                  size="small"
                  onClick={() => handleStartPicking(task)}
                  disabled={task.status !== 'PENDING'}
                >
                  <PlayArrowIcon />
                </IconButton>
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </TableContainer>
  </CardContent>
</Card>
```

---

## Testing Strategy

### Unit Tests

**FEFO Algorithm Tests:**

```java
@Test
void testQueryAvailableStockFEFO_SortsCorrectly() {
    // Arrange
    List<AvailableStockItem> stockItems = Arrays.asList(
        createStockItem(100, LocalDate.now().plusDays(30)), // Normal
        createStockItem(50, LocalDate.now().plusDays(5)),   // Critical
        createStockItem(200, LocalDate.now().plusDays(15))  // Near Expiry
    );

    // Mock stock service to return unsorted items
    when(stockManagementServicePort.queryAvailableStockFEFO(any(), any(), any()))
        .thenReturn(stockItems);

    // Act
    List<AvailableStockItem> result = queryAvailableStockFEFO(
        tenantId, productCode, new Quantity(BigDecimal.valueOf(350))
    );

    // Assert
    assertEquals(3, result.size());
    assertEquals(LocalDate.now().plusDays(5), result.get(0).getExpirationDate());
    assertEquals(LocalDate.now().plusDays(15), result.get(1).getExpirationDate());
    assertEquals(LocalDate.now().plusDays(30), result.get(2).getExpirationDate());
}

@Test
void testAllocateStock_InsufficientQuantity_ThrowsException() {
    // Arrange
    List<AvailableStockItem> stockItems = Arrays.asList(
        createStockItem(50, LocalDate.now().plusDays(10))
    );

    // Act & Assert
    assertThrows(InsufficientStockException.class, () ->
        allocateStockAndCreateTasks(
            loadId,
            orderId,
            lineItem,
            stockItems,
            1
        )
    );
}

@Test
void testOptimizePickingSequence_SortsByProximity() {
    // Arrange
    List<PickingTask> tasks = createPickingTasksWithDifferentLocations();

    // Act
    optimizePickingSequence(tasks);

    // Assert
    for (int i = 1; i < tasks.size(); i++) {
        int prevProximity = getLocationProximityScore(tasks.get(i - 1).getLocationId());
        int currProximity = getLocationProximityScore(tasks.get(i).getLocationId());
        assertTrue(prevProximity <= currProximity);
    }
}
```

### Integration Tests

```java
@SpringBootTest
@Transactional
class PlanPickingLocationsIntegrationTest {

    @Test
    void testPlanPickingLocations_EndToEnd() {
        // Arrange
        PickingList pickingList = createTestPickingList();
        mockStockServiceResponse();

        // Act
        PlanPickingLocationsResponse response =
            commandHandler.handle(createPlanCommand(pickingList.getLoads().get(0).getId()));

        // Assert
        assertNotNull(response);
        assertTrue(response.getTotalPickingTasks() > 0);

        // Verify picking tasks were created
        List<PickingTask> tasks = pickingTaskRepository.findByLoadId(
            pickingList.getLoads().get(0).getId()
        );
        assertTrue(tasks.size() > 0);

        // Verify FEFO ordering
        for (int i = 1; i < tasks.size(); i++) {
            assertTrue(tasks.get(i - 1).getSequence() <= tasks.get(i).getSequence());
        }
    }
}
```

---

## Acceptance Criteria Validation

| Acceptance Criteria                  | Implementation                              | Status    |
|--------------------------------------|---------------------------------------------|-----------|
| AC1: Optimize based on FEFO          | FEFO algorithm with expiration date sorting | ✅ Planned |
| AC2: Consider location proximity     | Proximity scoring algorithm                 | ✅ Planned |
| AC3: Generate picking sequence       | Sequence optimization algorithm             | ✅ Planned |
| AC4: Create PickingTask entities     | PickingTask domain model                    | ✅ Planned |
| AC5: Publish LoadPlannedEvent        | Event publishing                            | ✅ Planned |
| AC6: Publish PickingTaskCreatedEvent | Event publishing                            | ✅ Planned |
| AC7: Exclude expired stock           | Stock query filters                         | ✅ Planned |

---

**Document Control**

- **Version:** 1.0
- **Date:** 2025-01
- **Status:** Draft
