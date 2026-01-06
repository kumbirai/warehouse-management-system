# Create Picking List Implementation Plan

## US-6.1.4: Create Picking List

**Service:** Picking Service
**Priority:** Must Have
**Story Points:** 5
**Sprint:** Sprint 5

---

## Table of Contents

1. [Overview](#overview)
2. [Domain Model Design](#domain-model-design)
3. [Backend Implementation](#backend-implementation)
4. [Database Schema](#database-schema)
5. [Event Design](#event-design)
6. [Testing Strategy](#testing-strategy)
7. [Acceptance Criteria Validation](#acceptance-criteria-validation)

---

## Overview

### User Story

**As a** warehouse operator
**I want** to create picking list records
**So that** I can track picking operations

### Business Requirements

- System creates `PickingList` aggregate with picking list reference
- System creates `Load` aggregate containing multiple orders
- System creates `Order` entities within load
- System sets picking list status to "RECEIVED"
- System publishes `PickingListReceivedEvent`

### Technical Requirements

- Follow DDD, Clean Hexagonal Architecture, CQRS, Event-Driven Choreography
- Pure Java domain core (no framework dependencies)
- Proper aggregate boundaries
- Multi-tenant support
- Optimistic locking for concurrency control
- Complete audit trail

---

## Domain Model Design

### Complete Entity Model

```
PickingList (Aggregate Root)
  ├── PickingListId (UUID)
  ├── TenantId
  ├── List<Load>
  ├── PickingListStatus
  ├── receivedAt (ZonedDateTime)
  ├── processedAt (ZonedDateTime)
  └── notes

Load (Aggregate Root)
  ├── LoadId (UUID)
  ├── LoadNumber (Value Object)
  ├── List<Order>
  ├── LoadStatus
  ├── createdAt (ZonedDateTime)
  └── plannedAt (ZonedDateTime)

Order (Entity)
  ├── OrderId (UUID)
  ├── OrderNumber (Value Object)
  ├── CustomerInfo (Value Object)
  ├── Priority (Enum)
  ├── List<OrderLineItem>
  ├── OrderStatus
  └── createdAt (ZonedDateTime)

OrderLineItem (Entity)
  ├── OrderLineItemId (UUID)
  ├── ProductCode (Value Object)
  ├── Quantity (Value Object)
  └── notes

PickingTask (Entity - created during planning)
  ├── PickingTaskId (UUID)
  ├── LoadId
  ├── OrderId
  ├── ProductCode
  ├── LocationId (assigned during planning)
  ├── Quantity
  ├── PickingTaskStatus
  └── sequence (picking order)
```

### Domain Core Implementation

#### Order Entity

```java
package com.ccbsa.wms.picking.domain.core.entity;

import com.ccbsa.common.domain.entity.BaseEntity;
import com.ccbsa.common.domain.valueobject.*;
import com.ccbsa.wms.picking.domain.core.valueobject.OrderId;
import com.ccbsa.wms.picking.domain.core.valueobject.OrderStatus;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Order extends BaseEntity<OrderId> {
    private final OrderNumber orderNumber;
    private final CustomerInfo customerInfo;
    private final Priority priority;
    private final List<OrderLineItem> lineItems;
    private OrderStatus status;
    private final ZonedDateTime createdAt;
    private ZonedDateTime completedAt;

    private Order(Builder builder) {
        super.setId(builder.orderId);
        this.orderNumber = builder.orderNumber;
        this.customerInfo = builder.customerInfo;
        this.priority = builder.priority;
        this.lineItems = builder.lineItems;
        this.status = builder.status;
        this.createdAt = builder.createdAt;
        this.completedAt = builder.completedAt;
    }

    public void validateOrder() {
        if (orderNumber == null) {
            throw new IllegalStateException("Order must have an order number");
        }
        if (customerInfo == null) {
            throw new IllegalStateException("Order must have customer information");
        }
        if (lineItems == null || lineItems.isEmpty()) {
            throw new IllegalStateException("Order must have at least one line item");
        }
        lineItems.forEach(OrderLineItem::validateLineItem);
    }

    public void updateStatus(OrderStatus newStatus) {
        if (this.status == OrderStatus.COMPLETED) {
            throw new IllegalStateException("Cannot update status of completed order");
        }
        this.status = newStatus;
        if (newStatus == OrderStatus.COMPLETED) {
            this.completedAt = ZonedDateTime.now();
        }
    }

    public Quantity getTotalQuantity() {
        return lineItems.stream()
                .map(OrderLineItem::getQuantity)
                .reduce(Quantity::add)
                .orElse(new Quantity(java.math.BigDecimal.ZERO));
    }

    public int getLineItemCount() {
        return lineItems.size();
    }

    // Getters
    public OrderNumber getOrderNumber() { return orderNumber; }
    public CustomerInfo getCustomerInfo() { return customerInfo; }
    public Priority getPriority() { return priority; }
    public List<OrderLineItem> getLineItems() { return Collections.unmodifiableList(lineItems); }
    public OrderStatus getStatus() { return status; }
    public ZonedDateTime getCreatedAt() { return createdAt; }
    public ZonedDateTime getCompletedAt() { return completedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Order order = (Order) o;
        return Objects.equals(getId(), order.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private OrderId orderId;
        private OrderNumber orderNumber;
        private CustomerInfo customerInfo;
        private Priority priority;
        private List<OrderLineItem> lineItems;
        private OrderStatus status;
        private ZonedDateTime createdAt;
        private ZonedDateTime completedAt;

        private Builder() {}

        public Builder orderId(OrderId val) {
            orderId = val;
            return this;
        }

        public Builder orderNumber(OrderNumber val) {
            orderNumber = val;
            return this;
        }

        public Builder customerInfo(CustomerInfo val) {
            customerInfo = val;
            return this;
        }

        public Builder priority(Priority val) {
            priority = val;
            return this;
        }

        public Builder lineItems(List<OrderLineItem> val) {
            lineItems = val;
            return this;
        }

        public Builder status(OrderStatus val) {
            status = val;
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

        public Order build() {
            return new Order(this);
        }
    }
}
```

#### OrderLineItem Entity

```java
package com.ccbsa.wms.picking.domain.core.entity;

import com.ccbsa.common.domain.entity.BaseEntity;
import com.ccbsa.common.domain.valueobject.ProductCode;
import com.ccbsa.common.domain.valueobject.Quantity;
import com.ccbsa.wms.picking.domain.core.valueobject.OrderLineItemId;

import java.util.Objects;

public class OrderLineItem extends BaseEntity<OrderLineItemId> {
    private final ProductCode productCode;
    private final Quantity quantity;
    private Quantity pickedQuantity;
    private final String notes;

    private OrderLineItem(Builder builder) {
        super.setId(builder.orderLineItemId);
        this.productCode = builder.productCode;
        this.quantity = builder.quantity;
        this.pickedQuantity = builder.pickedQuantity;
        this.notes = builder.notes;
    }

    public void validateLineItem() {
        if (productCode == null) {
            throw new IllegalStateException("Line item must have a product code");
        }
        if (quantity == null || !quantity.isGreaterThanZero()) {
            throw new IllegalStateException("Line item quantity must be greater than zero");
        }
    }

    public void updatePickedQuantity(Quantity picked) {
        if (picked.isGreaterThan(quantity)) {
            throw new IllegalStateException("Picked quantity cannot exceed ordered quantity");
        }
        this.pickedQuantity = picked;
    }

    public boolean isFullyPicked() {
        return pickedQuantity != null && pickedQuantity.equals(quantity);
    }

    public boolean isPartiallyPicked() {
        return pickedQuantity != null &&
               pickedQuantity.isGreaterThanZero() &&
               !pickedQuantity.equals(quantity);
    }

    public Quantity getRemainingQuantity() {
        if (pickedQuantity == null) {
            return quantity;
        }
        return quantity.subtract(pickedQuantity);
    }

    // Getters
    public ProductCode getProductCode() { return productCode; }
    public Quantity getQuantity() { return quantity; }
    public Quantity getPickedQuantity() { return pickedQuantity; }
    public String getNotes() { return notes; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrderLineItem that = (OrderLineItem) o;
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
        private OrderLineItemId orderLineItemId;
        private ProductCode productCode;
        private Quantity quantity;
        private Quantity pickedQuantity;
        private String notes;

        private Builder() {}

        public Builder orderLineItemId(OrderLineItemId val) {
            orderLineItemId = val;
            return this;
        }

        public Builder productCode(ProductCode val) {
            productCode = val;
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

        public Builder notes(String val) {
            notes = val;
            return this;
        }

        public OrderLineItem build() {
            return new OrderLineItem(this);
        }
    }
}
```

#### Value Object IDs

**OrderId**

```java
package com.ccbsa.wms.picking.domain.core.valueobject;

import com.ccbsa.common.domain.valueobject.BaseId;

import java.util.UUID;

public class OrderId extends BaseId<UUID> {
    public OrderId(UUID value) {
        super(value);
    }

    public static OrderId of(UUID value) {
        return new OrderId(value);
    }

    public static OrderId newId() {
        return new OrderId(UUID.randomUUID());
    }
}
```

**OrderLineItemId**

```java
package com.ccbsa.wms.picking.domain.core.valueobject;

import com.ccbsa.common.domain.valueobject.BaseId;

import java.util.UUID;

public class OrderLineItemId extends BaseId<UUID> {
    public OrderLineItemId(UUID value) {
        super(value);
    }

    public static OrderLineItemId of(UUID value) {
        return new OrderLineItemId(value);
    }

    public static OrderLineItemId newId() {
        return new OrderLineItemId(UUID.randomUUID());
    }
}
```

#### Status Enums

**OrderStatus**

```java
package com.ccbsa.wms.picking.domain.core.valueobject;

public enum OrderStatus {
    PENDING("Pending"),
    PLANNED("Planned"),
    IN_PROGRESS("In Progress"),
    PARTIALLY_PICKED("Partially Picked"),
    PICKED("Picked"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled");

    private final String displayName;

    OrderStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isTerminal() {
        return this == COMPLETED || this == CANCELLED;
    }

    public boolean canTransitionTo(OrderStatus newStatus) {
        if (this.isTerminal()) {
            return false; // Cannot transition from terminal states
        }

        switch (this) {
            case PENDING:
                return newStatus == PLANNED || newStatus == CANCELLED;
            case PLANNED:
                return newStatus == IN_PROGRESS || newStatus == CANCELLED;
            case IN_PROGRESS:
                return newStatus == PARTIALLY_PICKED || newStatus == PICKED || newStatus == CANCELLED;
            case PARTIALLY_PICKED:
                return newStatus == IN_PROGRESS || newStatus == PICKED || newStatus == CANCELLED;
            case PICKED:
                return newStatus == COMPLETED;
            default:
                return false;
        }
    }
}
```

---

## Backend Implementation

### Repository Interfaces (Application Service Layer)

**PickingListRepository**

```java
package com.ccbsa.wms.picking.application.service.port.data;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.picking.domain.core.entity.PickingList;
import com.ccbsa.wms.picking.domain.core.valueobject.PickingListId;
import com.ccbsa.wms.picking.domain.core.valueobject.PickingListStatus;

import java.util.List;
import java.util.Optional;

public interface PickingListRepository {
    /**
     * Save picking list aggregate
     */
    PickingList save(PickingList pickingList);

    /**
     * Find picking list by ID and tenant
     */
    Optional<PickingList> findById(PickingListId pickingListId, TenantId tenantId);

    /**
     * Find all picking lists for tenant with pagination
     */
    List<PickingList> findByTenantId(TenantId tenantId, int page, int size);

    /**
     * Find picking lists by status
     */
    List<PickingList> findByTenantIdAndStatus(TenantId tenantId, PickingListStatus status, int page, int size);

    /**
     * Delete picking list (soft delete)
     */
    void delete(PickingListId pickingListId, TenantId tenantId);
}
```

**LoadRepository**

```java
package com.ccbsa.wms.picking.application.service.port.data;

import com.ccbsa.common.domain.valueobject.LoadNumber;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.picking.domain.core.entity.Load;
import com.ccbsa.wms.picking.domain.core.valueobject.LoadId;

import java.util.Optional;

public interface LoadRepository {
    /**
     * Save load aggregate
     */
    Load save(Load load);

    /**
     * Find load by ID
     */
    Optional<Load> findById(LoadId loadId, TenantId tenantId);

    /**
     * Find load by load number
     */
    Optional<Load> findByLoadNumber(LoadNumber loadNumber, TenantId tenantId);
}
```

---

## Database Schema

### Tables

**picking_lists**

```sql
CREATE TABLE picking_lists (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    status VARCHAR(50) NOT NULL,
    received_at TIMESTAMP WITH TIME ZONE NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE,
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version INT NOT NULL DEFAULT 0,

    CONSTRAINT fk_picking_lists_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenants(id)
);

CREATE INDEX idx_picking_lists_tenant_id ON picking_lists(tenant_id);
CREATE INDEX idx_picking_lists_status ON picking_lists(status);
CREATE INDEX idx_picking_lists_tenant_status ON picking_lists(tenant_id, status);
```

**loads**

```sql
CREATE TABLE loads (
    id UUID PRIMARY KEY,
    picking_list_id UUID NOT NULL,
    load_number VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    planned_at TIMESTAMP WITH TIME ZONE,
    version INT NOT NULL DEFAULT 0,

    CONSTRAINT fk_loads_picking_list
        FOREIGN KEY (picking_list_id) REFERENCES picking_lists(id),
    CONSTRAINT uk_load_number UNIQUE (load_number)
);

CREATE INDEX idx_loads_picking_list_id ON loads(picking_list_id);
CREATE INDEX idx_loads_load_number ON loads(load_number);
CREATE INDEX idx_loads_status ON loads(status);
```

**orders**

```sql
CREATE TABLE orders (
    id UUID PRIMARY KEY,
    load_id UUID NOT NULL,
    order_number VARCHAR(50) NOT NULL,
    customer_code VARCHAR(50) NOT NULL,
    customer_name VARCHAR(200),
    priority VARCHAR(20) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE,
    version INT NOT NULL DEFAULT 0,

    CONSTRAINT fk_orders_load
        FOREIGN KEY (load_id) REFERENCES loads(id),
    CONSTRAINT uk_order_number UNIQUE (order_number)
);

CREATE INDEX idx_orders_load_id ON orders(load_id);
CREATE INDEX idx_orders_order_number ON orders(order_number);
CREATE INDEX idx_orders_customer_code ON orders(customer_code);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_orders_priority ON orders(priority);
```

**order_line_items**

```sql
CREATE TABLE order_line_items (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL,
    product_code VARCHAR(50) NOT NULL,
    quantity DECIMAL(10,2) NOT NULL,
    picked_quantity DECIMAL(10,2),
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_order_line_items_order
        FOREIGN KEY (order_id) REFERENCES orders(id),
    CONSTRAINT chk_quantity_positive CHECK (quantity > 0),
    CONSTRAINT chk_picked_quantity_valid
        CHECK (picked_quantity IS NULL OR (picked_quantity >= 0 AND picked_quantity <= quantity))
);

CREATE INDEX idx_order_line_items_order_id ON order_line_items(order_id);
CREATE INDEX idx_order_line_items_product_code ON order_line_items(product_code);
```

**picking_tasks** (created during planning - US-6.2.1)

```sql
CREATE TABLE picking_tasks (
    id UUID PRIMARY KEY,
    load_id UUID NOT NULL,
    order_id UUID NOT NULL,
    order_line_item_id UUID NOT NULL,
    product_code VARCHAR(50) NOT NULL,
    location_id UUID,
    quantity DECIMAL(10,2) NOT NULL,
    picked_quantity DECIMAL(10,2),
    status VARCHAR(50) NOT NULL,
    sequence INT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP WITH TIME ZONE,

    CONSTRAINT fk_picking_tasks_load
        FOREIGN KEY (load_id) REFERENCES loads(id),
    CONSTRAINT fk_picking_tasks_order
        FOREIGN KEY (order_id) REFERENCES orders(id),
    CONSTRAINT fk_picking_tasks_order_line_item
        FOREIGN KEY (order_line_item_id) REFERENCES order_line_items(id)
);

CREATE INDEX idx_picking_tasks_load_id ON picking_tasks(load_id);
CREATE INDEX idx_picking_tasks_order_id ON picking_tasks(order_id);
CREATE INDEX idx_picking_tasks_status ON picking_tasks(status);
CREATE INDEX idx_picking_tasks_sequence ON picking_tasks(load_id, sequence);
```

### Flyway Migration

```sql
-- V5_1__Create_picking_service_schema.sql
-- Create picking service schema for Sprint 5

-- Picking Lists table
CREATE TABLE picking_lists (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    status VARCHAR(50) NOT NULL,
    received_at TIMESTAMP WITH TIME ZONE NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE,
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version INT NOT NULL DEFAULT 0
);

CREATE INDEX idx_picking_lists_tenant_id ON picking_lists(tenant_id);
CREATE INDEX idx_picking_lists_status ON picking_lists(status);
CREATE INDEX idx_picking_lists_tenant_status ON picking_lists(tenant_id, status);

-- Loads table
CREATE TABLE loads (
    id UUID PRIMARY KEY,
    picking_list_id UUID NOT NULL,
    load_number VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    planned_at TIMESTAMP WITH TIME ZONE,
    version INT NOT NULL DEFAULT 0,

    CONSTRAINT fk_loads_picking_list
        FOREIGN KEY (picking_list_id) REFERENCES picking_lists(id) ON DELETE CASCADE,
    CONSTRAINT uk_load_number UNIQUE (load_number)
);

CREATE INDEX idx_loads_picking_list_id ON loads(picking_list_id);
CREATE INDEX idx_loads_load_number ON loads(load_number);
CREATE INDEX idx_loads_status ON loads(status);

-- Orders table
CREATE TABLE orders (
    id UUID PRIMARY KEY,
    load_id UUID NOT NULL,
    order_number VARCHAR(50) NOT NULL,
    customer_code VARCHAR(50) NOT NULL,
    customer_name VARCHAR(200),
    priority VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE,
    version INT NOT NULL DEFAULT 0,

    CONSTRAINT fk_orders_load
        FOREIGN KEY (load_id) REFERENCES loads(id) ON DELETE CASCADE,
    CONSTRAINT uk_order_number UNIQUE (order_number)
);

CREATE INDEX idx_orders_load_id ON orders(load_id);
CREATE INDEX idx_orders_order_number ON orders(order_number);
CREATE INDEX idx_orders_customer_code ON orders(customer_code);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_orders_priority ON orders(priority);

-- Order Line Items table
CREATE TABLE order_line_items (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL,
    product_code VARCHAR(50) NOT NULL,
    quantity DECIMAL(10,2) NOT NULL,
    picked_quantity DECIMAL(10,2),
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_order_line_items_order
        FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    CONSTRAINT chk_quantity_positive CHECK (quantity > 0),
    CONSTRAINT chk_picked_quantity_valid
        CHECK (picked_quantity IS NULL OR (picked_quantity >= 0 AND picked_quantity <= quantity))
);

CREATE INDEX idx_order_line_items_order_id ON order_line_items(order_id);
CREATE INDEX idx_order_line_items_product_code ON order_line_items(product_code);

-- Picking Tasks table (for Sprint 5 US-6.2.1)
CREATE TABLE picking_tasks (
    id UUID PRIMARY KEY,
    load_id UUID NOT NULL,
    order_id UUID NOT NULL,
    order_line_item_id UUID NOT NULL,
    product_code VARCHAR(50) NOT NULL,
    location_id UUID,
    quantity DECIMAL(10,2) NOT NULL,
    picked_quantity DECIMAL(10,2),
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    sequence INT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP WITH TIME ZONE,

    CONSTRAINT fk_picking_tasks_load
        FOREIGN KEY (load_id) REFERENCES loads(id) ON DELETE CASCADE,
    CONSTRAINT fk_picking_tasks_order
        FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    CONSTRAINT fk_picking_tasks_order_line_item
        FOREIGN KEY (order_line_item_id) REFERENCES order_line_items(id) ON DELETE CASCADE,
    CONSTRAINT chk_picking_task_quantity_positive CHECK (quantity > 0),
    CONSTRAINT chk_picking_task_picked_quantity_valid
        CHECK (picked_quantity IS NULL OR (picked_quantity >= 0 AND picked_quantity <= quantity))
);

CREATE INDEX idx_picking_tasks_load_id ON picking_tasks(load_id);
CREATE INDEX idx_picking_tasks_order_id ON picking_tasks(order_id);
CREATE INDEX idx_picking_tasks_status ON picking_tasks(status);
CREATE INDEX idx_picking_tasks_sequence ON picking_tasks(load_id, sequence);
CREATE INDEX idx_picking_tasks_location_id ON picking_tasks(location_id);

COMMENT ON TABLE picking_lists IS 'Stores picking lists received from CSV upload or manual entry';
COMMENT ON TABLE loads IS 'Stores loads containing multiple orders';
COMMENT ON TABLE orders IS 'Stores orders within loads';
COMMENT ON TABLE order_line_items IS 'Stores line items (products) within orders';
COMMENT ON TABLE picking_tasks IS 'Stores picking tasks generated during load planning';
```

---

## Event Design

### PickingListReceivedEvent

```java
package com.ccbsa.wms.picking.domain.core.event;

import com.ccbsa.common.domain.event.DomainEvent;
import com.ccbsa.wms.picking.domain.core.entity.PickingList;
import lombok.Getter;

import java.time.ZonedDateTime;

@Getter
public class PickingListReceivedEvent extends DomainEvent<PickingList> {
    private final PickingList pickingList;

    public PickingListReceivedEvent(PickingList pickingList) {
        super(pickingList, ZonedDateTime.now());
        this.pickingList = pickingList;
    }
}
```

### Event Publisher Interface

```java
package com.ccbsa.wms.picking.application.service.port.event;

import com.ccbsa.wms.picking.domain.core.entity.PickingList;

public interface PickingEventPublisher {
    void publishPickingListReceivedEvent(PickingList pickingList);
}
```

### Event Publisher Implementation

```java
package com.ccbsa.wms.picking.messaging.publisher;

import com.ccbsa.common.messaging.EventEnricher;
import com.ccbsa.wms.picking.application.service.port.event.PickingEventPublisher;
import com.ccbsa.wms.picking.domain.core.entity.PickingList;
import com.ccbsa.wms.picking.domain.core.event.PickingListReceivedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PickingEventPublisherImpl implements PickingEventPublisher {
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final EventEnricher eventEnricher;
    private static final String TOPIC = "picking-events";

    @Override
    public void publishPickingListReceivedEvent(PickingList pickingList) {
        PickingListReceivedEvent event = new PickingListReceivedEvent(pickingList);

        // Enrich event with correlation context
        eventEnricher.enrichEvent(event);

        log.info("Publishing PickingListReceivedEvent for picking list: {}",
                pickingList.getId().getValue());

        kafkaTemplate.send(TOPIC, pickingList.getId().getValue().toString(), event);
    }
}
```

---

## Testing Strategy

### Unit Tests

**Domain Core Tests:**

```java
@Test
void testCreatePickingList_Success() {
    // Arrange
    Load load = createTestLoad();

    // Act
    PickingList pickingList = PickingList.builder()
            .pickingListId(PickingListId.newId())
            .tenantId(TenantId.of(UUID.randomUUID()))
            .loads(List.of(load))
            .status(PickingListStatus.RECEIVED)
            .receivedAt(ZonedDateTime.now())
            .build();

    pickingList.initializePickingList();

    // Assert
    assertNotNull(pickingList.getId());
    assertEquals(PickingListStatus.RECEIVED, pickingList.getStatus());
    assertEquals(1, pickingList.getLoads().size());
    verify(pickingList.getDomainEvents()).contains(
        instanceOf(PickingListReceivedEvent.class)
    );
}

@Test
void testValidateOrder_WithoutLineItems_ThrowsException() {
    // Arrange
    Order order = Order.builder()
            .orderId(OrderId.newId())
            .orderNumber(new OrderNumber("ORD-001"))
            .customerInfo(new CustomerInfo("CUST-001", "Test Customer"))
            .priority(Priority.NORMAL)
            .lineItems(Collections.emptyList())
            .status(OrderStatus.PENDING)
            .createdAt(ZonedDateTime.now())
            .build();

    // Act & Assert
    assertThrows(IllegalStateException.class, order::validateOrder);
}

@Test
void testOrderLineItem_PickedQuantityValidation() {
    // Arrange
    OrderLineItem lineItem = createTestLineItem(BigDecimal.valueOf(100));

    // Act & Assert - valid picked quantity
    assertDoesNotThrow(() ->
        lineItem.updatePickedQuantity(new Quantity(BigDecimal.valueOf(50)))
    );

    // Act & Assert - invalid picked quantity (exceeds ordered)
    assertThrows(IllegalStateException.class, () ->
        lineItem.updatePickedQuantity(new Quantity(BigDecimal.valueOf(150)))
    );
}
```

### Integration Tests

```java
@SpringBootTest
@Transactional
class PickingListRepositoryIntegrationTest {

    @Autowired
    private PickingListRepository pickingListRepository;

    @Test
    void testSaveAndRetrievePickingList() {
        // Arrange
        PickingList pickingList = createTestPickingList();

        // Act
        PickingList saved = pickingListRepository.save(pickingList);
        Optional<PickingList> retrieved = pickingListRepository.findById(
                saved.getId(), saved.getTenantId()
        );

        // Assert
        assertTrue(retrieved.isPresent());
        assertEquals(saved.getId(), retrieved.get().getId());
        assertEquals(1, retrieved.get().getLoads().size());
    }
}
```

---

## Acceptance Criteria Validation

| Acceptance Criteria | Implementation | Status |
|---------------------|----------------|--------|
| AC1: Create PickingList aggregate | PickingList.builder() pattern | ✅ Planned |
| AC2: Create Load aggregate | Load.builder() pattern | ✅ Planned |
| AC3: Create Order entities | Order.builder() pattern | ✅ Planned |
| AC4: Set status to RECEIVED | PickingList.initializePickingList() | ✅ Planned |
| AC5: Publish PickingListReceivedEvent | Event publisher implementation | ✅ Planned |

---

**Document Control**

- **Version:** 1.0
- **Date:** 2025-01
- **Status:** Draft
