# Map Orders to Loads Implementation Plan

## US-6.2.2: Map Orders to Loads

**Service:** Picking Service
**Priority:** Must Have
**Story Points:** 5
**Sprint:** Sprint 5

---

## Table of Contents

1. [Overview](#overview)
2. [Domain Model Design](#domain-model-design)
3. [Backend Implementation](#backend-implementation)
4. [Frontend Implementation](#frontend-implementation)
5. [Testing Strategy](#testing-strategy)
6. [Acceptance Criteria Validation](#acceptance-criteria-validation)

---

## Overview

### User Story

**As a** warehouse operator
**I want** orders to be correctly mapped to loads
**So that** I can track which orders belong to which load

### Business Requirements

- System supports multiple orders per load
- System supports multiple orders per customer per load
- System maintains order-to-load relationships
- System tracks order status within load
- System allows querying orders by load

### Technical Requirements

- Follow DDD, Clean Hexagonal Architecture, CQRS, Event-Driven Choreography
- Efficient database queries for order-load relationships
- Support for complex filtering and pagination
- Multi-tenant support

---

## Domain Model Design

### Aggregate Relationships

```
Load (Aggregate Root)
  └── List<Order> (Child Entities)
        └── List<OrderLineItem> (Child Entities)

Relationships:
- One Load has many Orders (1:N)
- One Order belongs to one Load (N:1)
- One Customer can have multiple Orders in same Load
- Order-to-Load relationship is immutable once created
```

### Enhanced Load Aggregate

```java
package com.ccbsa.wms.picking.domain.core.entity;

import com.ccbsa.common.domain.entity.AggregateRoot;
import com.ccbsa.common.domain.valueobject.*;
import com.ccbsa.wms.picking.domain.core.event.OrderMappedToLoadEvent;
import com.ccbsa.wms.picking.domain.core.valueobject.*;

import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class Load extends AggregateRoot<LoadId> {
    private final LoadNumber loadNumber;
    private final List<Order> orders;
    private LoadStatus status;
    private final ZonedDateTime createdAt;
    private ZonedDateTime plannedAt;

    private Load(Builder builder) {
        super.setId(builder.loadId);
        this.loadNumber = builder.loadNumber;
        this.orders = builder.orders;
        this.status = builder.status;
        this.createdAt = builder.createdAt;
        this.plannedAt = builder.plannedAt;
    }

    public void validateLoad() {
        if (orders == null || orders.isEmpty()) {
            throw new IllegalStateException("Load must have at least one order");
        }
        orders.forEach(Order::validateOrder);
    }

    public void addOrder(Order order) {
        if (this.status != LoadStatus.PENDING) {
            throw new IllegalStateException("Cannot add orders to non-pending load");
        }
        this.orders.add(order);
        this.registerEvent(new OrderMappedToLoadEvent(this, order));
    }

    public void removeOrder(OrderId orderId) {
        if (this.status != LoadStatus.PENDING) {
            throw new IllegalStateException("Cannot remove orders from non-pending load");
        }
        orders.removeIf(order -> order.getId().equals(orderId));
    }

    public Optional<Order> findOrderById(OrderId orderId) {
        return orders.stream()
                .filter(order -> order.getId().equals(orderId))
                .findFirst();
    }

    public List<Order> getOrdersByCustomer(String customerCode) {
        return orders.stream()
                .filter(order -> order.getCustomerInfo().getCustomerCode().equals(customerCode))
                .collect(Collectors.toList());
    }

    public List<Order> getOrdersByPriority(Priority priority) {
        return orders.stream()
                .filter(order -> order.getPriority() == priority)
                .collect(Collectors.toList());
    }

    public List<Order> getOrdersByStatus(OrderStatus status) {
        return orders.stream()
                .filter(order -> order.getStatus() == status)
                .collect(Collectors.toList());
    }

    public int getOrderCount() {
        return orders.size();
    }

    public int getUniqueCustomerCount() {
        return (int) orders.stream()
                .map(order -> order.getCustomerInfo().getCustomerCode())
                .distinct()
                .count();
    }

    public Quantity getTotalQuantity() {
        return orders.stream()
                .map(Order::getTotalQuantity)
                .reduce(Quantity::add)
                .orElse(new Quantity(java.math.BigDecimal.ZERO));
    }

    public boolean isFullyPicked() {
        return orders.stream().allMatch(order -> order.getStatus() == OrderStatus.PICKED);
    }

    public boolean isPartiallyPicked() {
        return orders.stream().anyMatch(order ->
                order.getStatus() == OrderStatus.PARTIALLY_PICKED ||
                order.getStatus() == OrderStatus.IN_PROGRESS
        );
    }

    public void updateStatus(LoadStatus newStatus) {
        this.status = newStatus;
    }

    public void setPlannedAt(ZonedDateTime plannedAt) {
        this.plannedAt = plannedAt;
    }

    // Getters
    public LoadNumber getLoadNumber() { return loadNumber; }
    public List<Order> getOrders() { return Collections.unmodifiableList(orders); }
    public LoadStatus getStatus() { return status; }
    public ZonedDateTime getCreatedAt() { return createdAt; }
    public ZonedDateTime getPlannedAt() { return plannedAt; }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private LoadId loadId;
        private LoadNumber loadNumber;
        private List<Order> orders = new ArrayList<>();
        private LoadStatus status;
        private ZonedDateTime createdAt;
        private ZonedDateTime plannedAt;

        private Builder() {}

        public Builder loadId(LoadId val) {
            loadId = val;
            return this;
        }

        public Builder loadNumber(LoadNumber val) {
            loadNumber = val;
            return this;
        }

        public Builder orders(List<Order> val) {
            orders = new ArrayList<>(val);
            return this;
        }

        public Builder status(LoadStatus val) {
            status = val;
            return this;
        }

        public Builder createdAt(ZonedDateTime val) {
            createdAt = val;
            return this;
        }

        public Builder plannedAt(ZonedDateTime val) {
            plannedAt = val;
            return this;
        }

        public Load build() {
            return new Load(this);
        }
    }
}
```

### OrderMappedToLoadEvent

```java
package com.ccbsa.wms.picking.domain.core.event;

import com.ccbsa.common.domain.event.DomainEvent;
import com.ccbsa.wms.picking.domain.core.entity.Load;
import com.ccbsa.wms.picking.domain.core.entity.Order;
import lombok.Getter;

import java.time.ZonedDateTime;

@Getter
public class OrderMappedToLoadEvent extends DomainEvent<Load> {
    private final Load load;
    private final Order order;

    public OrderMappedToLoadEvent(Load load, Order order) {
        super(load, ZonedDateTime.now());
        this.load = load;
        this.order = order;
    }
}
```

---

## Backend Implementation

### Query Handlers

**GetOrdersByLoadQueryHandler**

```java
package com.ccbsa.wms.picking.application.service.query;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.picking.application.service.port.data.OrderViewRepository;
import com.ccbsa.wms.picking.application.service.query.dto.*;
import com.ccbsa.wms.picking.domain.core.valueobject.LoadId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class GetOrdersByLoadQueryHandler {
    private final OrderViewRepository orderViewRepository;

    @Transactional(readOnly = true)
    public GetOrdersByLoadQueryResult handle(GetOrdersByLoadQuery query) {
        log.info("Getting orders for load: {}", query.getLoadId());

        List<OrderView> orderViews = orderViewRepository.findByLoadId(
                query.getLoadId(),
                query.getTenantId()
        );

        List<OrderQueryDTO> orders = orderViews.stream()
                .map(this::toOrderDTO)
                .collect(Collectors.toList());

        return GetOrdersByLoadQueryResult.builder()
                .loadId(query.getLoadId())
                .orders(orders)
                .totalOrders(orders.size())
                .build();
    }

    private OrderQueryDTO toOrderDTO(OrderView view) {
        return OrderQueryDTO.builder()
                .orderId(view.getOrderId())
                .orderNumber(view.getOrderNumber())
                .customerCode(view.getCustomerCode())
                .customerName(view.getCustomerName())
                .priority(view.getPriority())
                .status(view.getStatus())
                .totalQuantity(view.getTotalQuantity())
                .lineItemCount(view.getLineItemCount())
                .createdAt(view.getCreatedAt())
                .build();
    }
}
```

**ListOrdersByCustomerQueryHandler**

```java
package com.ccbsa.wms.picking.application.service.query;

import com.ccbsa.wms.picking.application.service.port.data.OrderViewRepository;
import com.ccbsa.wms.picking.application.service.query.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class ListOrdersByCustomerQueryHandler {
    private final OrderViewRepository orderViewRepository;

    @Transactional(readOnly = true)
    public ListOrdersByCustomerQueryResult handle(ListOrdersByCustomerQuery query) {
        log.info("Listing orders for customer: {}", query.getCustomerCode());

        List<OrderView> orderViews = orderViewRepository.findByCustomerCode(
                query.getCustomerCode(),
                query.getTenantId(),
                query.getPage(),
                query.getSize()
        );

        List<OrderQueryDTO> orders = orderViews.stream()
                .map(this::toOrderDTO)
                .collect(Collectors.toList());

        long totalCount = orderViewRepository.countByCustomerCode(
                query.getCustomerCode(),
                query.getTenantId()
        );

        return ListOrdersByCustomerQueryResult.builder()
                .customerCode(query.getCustomerCode())
                .orders(orders)
                .totalCount(totalCount)
                .page(query.getPage())
                .pageSize(query.getSize())
                .build();
    }

    private OrderQueryDTO toOrderDTO(OrderView view) {
        return OrderQueryDTO.builder()
                .orderId(view.getOrderId())
                .orderNumber(view.getOrderNumber())
                .loadId(view.getLoadId())
                .loadNumber(view.getLoadNumber())
                .customerCode(view.getCustomerCode())
                .customerName(view.getCustomerName())
                .priority(view.getPriority())
                .status(view.getStatus())
                .totalQuantity(view.getTotalQuantity())
                .lineItemCount(view.getLineItemCount())
                .createdAt(view.getCreatedAt())
                .build();
    }
}
```

### Repository Interfaces

**OrderViewRepository**

```java
package com.ccbsa.wms.picking.application.service.port.data;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.picking.application.service.port.data.dto.OrderView;
import com.ccbsa.wms.picking.domain.core.valueobject.*;

import java.util.List;
import java.util.UUID;

public interface OrderViewRepository {
    /**
     * Find all orders in a load
     */
    List<OrderView> findByLoadId(UUID loadId, TenantId tenantId);

    /**
     * Find orders by customer code
     */
    List<OrderView> findByCustomerCode(String customerCode, TenantId tenantId, int page, int size);

    /**
     * Count orders by customer code
     */
    long countByCustomerCode(String customerCode, TenantId tenantId);

    /**
     * Find orders by priority
     */
    List<OrderView> findByPriority(String priority, TenantId tenantId, int page, int size);

    /**
     * Find orders by status
     */
    List<OrderView> findByStatus(String status, TenantId tenantId, int page, int size);
}
```

**OrderView DTO**

```java
package com.ccbsa.wms.picking.application.service.port.data.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class OrderView {
    private final UUID orderId;
    private final String orderNumber;
    private final UUID loadId;
    private final String loadNumber;
    private final String customerCode;
    private final String customerName;
    private final String priority;
    private final String status;
    private final BigDecimal totalQuantity;
    private final int lineItemCount;
    private final ZonedDateTime createdAt;
    private final ZonedDateTime completedAt;
}
```

---

## Frontend Implementation

### Load Detail View with Orders

```typescript
<Card>
  <CardHeader
    title={`Load: ${load.loadNumber}`}
    subheader={`${load.orderCount} orders, ${load.uniqueCustomerCount} customers`}
  />
  <CardContent>
    {/* Load Summary */}
    <Grid container spacing={2}>
      <Grid item xs={12} md={3}>
        <Typography variant="body2" color="textSecondary">Status</Typography>
        <StatusBadge status={load.status} />
      </Grid>
      <Grid item xs={12} md={3}>
        <Typography variant="body2" color="textSecondary">Total Quantity</Typography>
        <Typography variant="h6">{load.totalQuantity}</Typography>
      </Grid>
      <Grid item xs={12} md={3}>
        <Typography variant="body2" color="textSecondary">Created</Typography>
        <Typography variant="body1">{formatDateTime(load.createdAt)}</Typography>
      </Grid>
      <Grid item xs={12} md={3}>
        <Typography variant="body2" color="textSecondary">Planned</Typography>
        <Typography variant="body1">
          {load.plannedAt ? formatDateTime(load.plannedAt) : 'Not planned'}
        </Typography>
      </Grid>
    </Grid>

    <Divider sx={{ my: 3 }} />

    {/* Orders in Load */}
    <Typography variant="h6" gutterBottom>
      Orders ({load.orders.length})
    </Typography>

    {/* Group by Customer */}
    {Object.entries(groupOrdersByCustomer(load.orders)).map(([customerCode, orders]) => (
      <Card key={customerCode} variant="outlined" sx={{ mb: 2 }}>
        <CardHeader
          title={`Customer: ${customerCode}`}
          subheader={`${orders[0].customerName} - ${orders.length} order(s)`}
        />
        <CardContent>
          <TableContainer>
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>Order Number</TableCell>
                  <TableCell>Priority</TableCell>
                  <TableCell>Status</TableCell>
                  <TableCell align="right">Line Items</TableCell>
                  <TableCell align="right">Total Qty</TableCell>
                  <TableCell>Actions</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {orders.map((order) => (
                  <TableRow key={order.orderId}>
                    <TableCell>
                      <Link to={`/orders/${order.orderId}`}>
                        {order.orderNumber}
                      </Link>
                    </TableCell>
                    <TableCell>
                      <Chip
                        label={order.priority}
                        color={getPriorityColor(order.priority)}
                        size="small"
                      />
                    </TableCell>
                    <TableCell>
                      <StatusBadge status={order.status} />
                    </TableCell>
                    <TableCell align="right">{order.lineItemCount}</TableCell>
                    <TableCell align="right">{order.totalQuantity}</TableCell>
                    <TableCell>
                      <IconButton
                        size="small"
                        onClick={() => handleViewOrder(order.orderId)}
                      >
                        <VisibilityIcon />
                      </IconButton>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        </CardContent>
      </Card>
    ))}
  </CardContent>
</Card>
```

### Order List with Load Information

```typescript
<TableContainer>
  <Table>
    <TableHead>
      <TableRow>
        <TableCell>Order Number</TableCell>
        <TableCell>Load</TableCell>
        <TableCell>Customer</TableCell>
        <TableCell>Priority</TableCell>
        <TableCell>Status</TableCell>
        <TableCell align="right">Items</TableCell>
        <TableCell align="right">Quantity</TableCell>
        <TableCell>Actions</TableCell>
      </TableRow>
    </TableHead>
    <TableBody>
      {orders.map((order) => (
        <TableRow key={order.orderId}>
          <TableCell>
            <Link to={`/orders/${order.orderId}`}>
              {order.orderNumber}
            </Link>
          </TableCell>
          <TableCell>
            <Link to={`/loads/${order.loadId}`}>
              {order.loadNumber}
            </Link>
          </TableCell>
          <TableCell>
            <Box>
              <Typography variant="body2">{order.customerCode}</Typography>
              <Typography variant="caption" color="textSecondary">
                {order.customerName}
              </Typography>
            </Box>
          </TableCell>
          <TableCell>
            <Chip
              label={order.priority}
              color={getPriorityColor(order.priority)}
              size="small"
            />
          </TableCell>
          <TableCell>
            <StatusBadge status={order.status} />
          </TableCell>
          <TableCell align="right">{order.lineItemCount}</TableCell>
          <TableCell align="right">{order.totalQuantity}</TableCell>
          <TableCell>
            <IconButton
              size="small"
              onClick={() => handleViewOrder(order.orderId)}
            >
              <VisibilityIcon />
            </IconButton>
          </TableCell>
        </TableRow>
      ))}
    </TableBody>
  </Table>
</TableContainer>
```

---

## Testing Strategy

### Unit Tests

```java
@Test
void testLoad_AddOrder_Success() {
    // Arrange
    Load load = createTestLoad();
    Order newOrder = createTestOrder();

    // Act
    load.addOrder(newOrder);

    // Assert
    assertEquals(2, load.getOrderCount());
    assertTrue(load.findOrderById(newOrder.getId()).isPresent());
    verify(load.getDomainEvents()).contains(
        instanceOf(OrderMappedToLoadEvent.class)
    );
}

@Test
void testLoad_GetOrdersByCustomer_ReturnsCorrectOrders() {
    // Arrange
    Load load = createLoadWithMultipleCustomers();
    String customerCode = "CUST-001";

    // Act
    List<Order> customerOrders = load.getOrdersByCustomer(customerCode);

    // Assert
    assertTrue(customerOrders.size() > 0);
    assertTrue(customerOrders.stream()
        .allMatch(order -> order.getCustomerInfo().getCustomerCode().equals(customerCode))
    );
}

@Test
void testLoad_GetOrdersByPriority_FiltersCorrectly() {
    // Arrange
    Load load = createLoadWithDifferentPriorities();

    // Act
    List<Order> highPriorityOrders = load.getOrdersByPriority(Priority.HIGH);

    // Assert
    assertTrue(highPriorityOrders.size() > 0);
    assertTrue(highPriorityOrders.stream()
        .allMatch(order -> order.getPriority() == Priority.HIGH)
    );
}
```

### Integration Tests

```java
@SpringBootTest
@Transactional
class OrderViewRepositoryIntegrationTest {

    @Test
    void testFindOrdersByLoadId() {
        // Arrange
        Load load = createAndSaveTestLoad();

        // Act
        List<OrderView> orders = orderViewRepository.findByLoadId(
            load.getId().getValue(),
            load.getTenantId()
        );

        // Assert
        assertEquals(load.getOrderCount(), orders.size());
    }

    @Test
    void testFindOrdersByCustomerCode() {
        // Arrange
        String customerCode = "CUST-001";
        createOrdersForCustomer(customerCode, 3);

        // Act
        List<OrderView> orders = orderViewRepository.findByCustomerCode(
            customerCode,
            tenantId,
            0,
            10
        );

        // Assert
        assertTrue(orders.size() >= 3);
        assertTrue(orders.stream()
            .allMatch(order -> order.getCustomerCode().equals(customerCode))
        );
    }
}
```

---

## Acceptance Criteria Validation

| Acceptance Criteria                        | Implementation              | Status    |
|--------------------------------------------|-----------------------------|-----------|
| AC1: Multiple orders per load              | Load.orders List            | ✅ Planned |
| AC2: Multiple orders per customer per load | CustomerInfo grouping       | ✅ Planned |
| AC3: Maintain order-to-load relationships  | Load-Order association      | ✅ Planned |
| AC4: Track order status within load        | Order.status field          | ✅ Planned |
| AC5: Query orders by load                  | OrderViewRepository methods | ✅ Planned |

---

**Document Control**

- **Version:** 1.0
- **Date:** 2025-01
- **Status:** Draft
