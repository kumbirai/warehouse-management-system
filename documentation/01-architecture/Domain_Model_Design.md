# Domain Model Design

## Warehouse Management System Integration - CCBSA LDP System

**Document Version:** 1.0  
**Date:** 2025-11  
**Status:** Draft  
**Related Documents:**

- [Business Requirements Document](../00-business-requiremants/business-requirements-document.md)
- [Service Architecture Document](Service_Architecture_Document.md)
- [Project Roadmap](../project-management/project-roadmap.md)

---

## Table of Contents

1. [Overview](#overview)
2. [Domain Model Principles](#domain-model-principles)
3. [Bounded Contexts](#bounded-contexts)
4. [Stock Management Context](#stock-management-context)
5. [Location Management Context](#location-management-context)
5. [Product Context](#product-context)
6. [Order Fulfillment Context](#order-fulfillment-context)
7. [Inventory Control Context](#inventory-control-context)
8. [Shared Kernel](#shared-kernel)
9. [Domain Events Catalog](#domain-events-catalog)

---

## Overview

### Purpose

This document defines the detailed domain model for the Warehouse Management System Integration. The domain model follows **Domain-Driven Design (DDD)** principles with clear
bounded contexts, aggregates, entities, value objects, and domain events.

### Domain Model Principles

1. **Ubiquitous Language** - Domain terms used consistently across code and documentation
2. **Aggregate Roots** - Entities that maintain consistency boundaries
3. **Value Objects** - Immutable objects defined by their attributes
4. **Domain Events** - Events representing something that happened in the domain
5. **Bounded Contexts** - Explicit boundaries where domain model applies
6. **Pure Domain Logic** - Domain-core modules contain only pure Java, no framework dependencies

---

## Domain Model Principles

### Aggregate Design Rules

1. **One Aggregate Root per Aggregate** - Each aggregate has exactly one aggregate root
2. **Consistency Boundary** - Aggregates maintain consistency within their boundary
3. **Reference by Identity** - Aggregates reference other aggregates by ID only
4. **Eventual Consistency** - Consistency between aggregates achieved through events
5. **Transaction Boundary** - One transaction per aggregate modification

### Value Object Rules

1. **Immutability** - Value objects are immutable
2. **Equality by Value** - Two value objects are equal if all attributes are equal
3. **No Identity** - Value objects have no identity
4. **Validation** - Value objects validate themselves on construction

### Entity Rules

1. **Identity** - Entities have unique identity
2. **Mutable** - Entities can change state
3. **Lifecycle** - Entities have lifecycle (created, modified, deleted)

### Domain Event Rules

1. **Past Tense** - Events represent something that happened
2. **Immutable** - Events are immutable
3. **Rich Domain** - Events contain domain data, not just IDs
4. **Versioned** - Events are versioned for schema evolution

---

## Bounded Contexts

### Context Map

```
┌─────────────────────────────────────────────────────────┐
│              Warehouse Management System                 │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  ┌──────────────────┐  ┌──────────────────┐          │
│  │ Stock Management│  │ Location          │          │
│  │ Context         │  │ Management       │          │
│  │                 │  │ Context          │          │
│  │ Owns:           │  │ Owns:            │          │
│  │ - Stock         │  │ - Locations      │          │
│  │ - Expiration    │  │ - Movements      │          │
│  │ - Stock Levels  │  │ - Capacity       │          │
│  └────────┬────────┘  └────────┬─────────┘          │
│           │                    │                      │
│           └────────────────────┘                      │
│                  │                                     │
│  ┌───────────────┴─────────────────────────────────┐  │
│  │         Order Fulfillment Context               │  │
│  │                                                  │  │
│  │  ┌──────────────┐      ┌──────────────┐       │  │
│  │  │ Picking      │      │ Returns      │       │  │
│  │  │ Sub-Context  │      │ Sub-Context  │       │  │
│  │  └──────────────┘      └──────────────┘       │  │
│  └──────────────────────────────────────────────────┘  │
│                                                         │
│  ┌──────────────────────────────────────────────────┐  │
│  │      Inventory Control Context                   │  │
│  │      - Stock Counts                              │  │
│  │      - Reconciliation                            │  │
│  └──────────────────────────────────────────────────┘  │
│                                                         │
│  ┌──────────────────────────────────────────────────┐  │
│  │      Product Context (Reference Data)            │  │
│  │      - Products                                   │  │
│  │      - Barcodes                                   │  │
│  └──────────────────────────────────────────────────┘  │
│                                                         │
│  ┌──────────────────────────────────────────────────┐  │
│  │      Tenant Management Context                   │  │
│  │      - Tenants                                    │  │
│  │      - Tenant Configuration                       │  │
│  │      - Tenant Lifecycle                           │  │
│  └──────────────────────────────────────────────────┘  │
│                                                         │
│  ┌──────────────────────────────────────────────────┐  │
│  │      User Management Context                     │  │
│  │      - Users                                      │  │
│  │      - User Profiles                              │  │
│  │      - IAM Integration                            │  │
│  └──────────────────────────────────────────────────┘  │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

---

## Stock Management Context

### Context Overview

**Purpose:** Manages stock consignment, classification by expiration dates, stock level monitoring, and restock request generation.

**Key Concepts:**

- Stock Consignment - Incoming stock from D365
- Stock Classification - Classification by expiration dates
- Stock Levels - Current stock levels per product/location
- Restock Requests - Automated requests when stock falls below minimum

### Aggregate: StockConsignment

**Aggregate Root:** `StockConsignment`

**Identity:** `ConsignmentId` (Value Object)

**Responsibilities:**

- Receive and validate consignment data from D365
- Track consignment status
- Confirm consignment receipt
- Handle partial receipts

**Entities:**

- `StockConsignment` (Aggregate Root)
- `ConsignmentLineItem` (Entity)

**Value Objects:**

- `ConsignmentId`
- `ConsignmentReference` (from D365)
- `Quantity`
- `ExpirationDate`
- `ReceivedTimestamp`

**Domain Events:**

- `StockConsignmentReceivedEvent`
- `StockConsignmentValidatedEvent`
- `StockConsignmentConfirmedEvent`
- `PartialConsignmentReceivedEvent`

**Business Rules:**

- Consignment must have at least one line item
- Consignment reference must be unique
- Quantities must be positive
- Expiration dates must be in the future (or null for non-perishable)

**Java Implementation:**

```java
package com.ccbsa.wms.stockmanagement.domain.stockmanagement.domaincore;

import com.ccbsa.common.domain.DomainEvent;
import com.ccbsa.common.domain.AggregateRoot;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Aggregate Root: StockConsignment
 * Represents incoming stock consignment from D365
 */
public class StockConsignment extends AggregateRoot<ConsignmentId> {

    private ConsignmentReference consignmentReference;
    private TenantId tenantId;
    private WarehouseId warehouseId;
    private ConsignmentStatus status;
    private LocalDateTime receivedAt;
    private LocalDateTime confirmedAt;
    private List<ConsignmentLineItem> lineItems;

    private StockConsignment() {
        // Private constructor for builder
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private StockConsignment consignment = new StockConsignment();

        public Builder consignmentId(ConsignmentId id) {
            consignment.id = id;
            return this;
        }

        public Builder consignmentReference(ConsignmentReference reference) {
            consignment.consignmentReference = reference;
            return this;
        }

        public Builder tenantId(TenantId tenantId) {
            consignment.tenantId = tenantId;
            return this;
        }

        public Builder warehouseId(WarehouseId warehouseId) {
            consignment.warehouseId = warehouseId;
            return this;
        }

        public Builder lineItem(ConsignmentLineItem lineItem) {
            if (consignment.lineItems == null) {
                consignment.lineItems = new ArrayList<>();
            }
            consignment.lineItems.add(lineItem);
            return this;
        }

        public StockConsignment build() {
            validate();
            consignment.status = ConsignmentStatus.RECEIVED;
            consignment.receivedAt = LocalDateTime.now();
            return consignment;
        }

        private void validate() {
            if (consignment.id == null) {
                throw new IllegalArgumentException("ConsignmentId is required");
            }
            if (consignment.consignmentReference == null) {
                throw new IllegalArgumentException("ConsignmentReference is required");
            }
            if (consignment.tenantId == null) {
                throw new IllegalArgumentException("TenantId is required");
            }
            if (consignment.warehouseId == null) {
                throw new IllegalArgumentException("WarehouseId is required");
            }
            if (consignment.lineItems == null || consignment.lineItems.isEmpty()) {
                throw new IllegalArgumentException("At least one line item is required");
            }
        }
    }

    public void confirm() {
        if (this.status != ConsignmentStatus.RECEIVED) {
            throw new IllegalStateException("Can only confirm received consignments");
        }
        this.status = ConsignmentStatus.CONFIRMED;
        this.confirmedAt = LocalDateTime.now();
        
        addDomainEvent(new StockConsignmentConfirmedEvent(
            this.id,
            this.consignmentReference,
            this.tenantId,
            this.warehouseId,
            this.lineItems
        ));
    }

    public void handlePartialReceipt(List<ConsignmentLineItem> receivedItems) {
        // Business logic for partial receipt
        addDomainEvent(new PartialConsignmentReceivedEvent(
            this.id,
            this.consignmentReference,
            receivedItems
        ));
    }

    // Getters
    public ConsignmentReference getConsignmentReference() {
        return consignmentReference;
    }

    public ConsignmentStatus getStatus() {
        return status;
    }

    public List<ConsignmentLineItem> getLineItems() {
        return new ArrayList<>(lineItems);
    }
}
```

### Aggregate: StockItem

**Aggregate Root:** `StockItem`

**Identity:** `StockItemId` (Value Object)

**Responsibilities:**

- Track individual stock items with expiration dates
- Classify stock by expiration dates
- Generate expiration alerts
- Prevent picking of expired stock

**Value Objects:**

- `StockItemId`
- `ProductId`
- `LocationId`
- `Quantity`
- `ExpirationDate`
- `StockClassification`

**Domain Events:**

- `StockItemCreatedEvent`
- `StockItemClassifiedEvent`
- `StockExpiringAlertEvent`
- `StockExpiredEvent`

**Business Rules:**

- Stock items must have valid expiration dates (or null for non-perishable)
- Stock classification automatically assigned based on expiration date
- Expired stock cannot be picked
- Stock within 7 days of expiration generates alert
- Stock within 30 days of expiration classified as "Near Expiry"

**Java Implementation:**

```java
package com.ccbsa.wms.stockmanagement.domain.stockmanagement.domaincore;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Aggregate Root: StockItem
 * Represents individual stock item with expiration tracking
 */
public class StockItem extends AggregateRoot<StockItemId> {

    private ProductId productId;
    private LocationId locationId;
    private Quantity quantity;
    private ExpirationDate expirationDate;
    private StockClassification classification;
    private LocalDateTime createdAt;
    private LocalDateTime lastModifiedAt;

    private StockItem() {
        // Private constructor for builder
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private StockItem stockItem = new StockItem();

        public Builder stockItemId(StockItemId id) {
            stockItem.id = id;
            return this;
        }

        public Builder productId(ProductId productId) {
            stockItem.productId = productId;
            return this;
        }

        public Builder locationId(LocationId locationId) {
            stockItem.locationId = locationId;
            return this;
        }

        public Builder quantity(Quantity quantity) {
            stockItem.quantity = quantity;
            return this;
        }

        public Builder expirationDate(ExpirationDate expirationDate) {
            stockItem.expirationDate = expirationDate;
            stockItem.classification = classifyStock(expirationDate);
            return this;
        }

        public StockItem build() {
            validate();
            stockItem.createdAt = LocalDateTime.now();
            stockItem.lastModifiedAt = LocalDateTime.now();
            return stockItem;
        }

        private void validate() {
            if (stockItem.id == null) {
                throw new IllegalArgumentException("StockItemId is required");
            }
            if (stockItem.productId == null) {
                throw new IllegalArgumentException("ProductId is required");
            }
            if (stockItem.locationId == null) {
                throw new IllegalArgumentException("LocationId is required");
            }
            if (stockItem.quantity == null) {
                throw new IllegalArgumentException("Quantity is required");
            }
        }

        private StockClassification classifyStock(ExpirationDate expirationDate) {
            if (expirationDate == null) {
                return StockClassification.NORMAL;
            }
            LocalDate today = LocalDate.now();
            LocalDate expiryDate = expirationDate.getValue();
            long daysUntilExpiry = java.time.temporal.ChronoUnit.DAYS.between(today, expiryDate);
            
            if (daysUntilExpiry < 0) {
                return StockClassification.EXPIRED;
            } else if (daysUntilExpiry <= 7) {
                return StockClassification.CRITICAL;
            } else if (daysUntilExpiry <= 30) {
                return StockClassification.NEAR_EXPIRY;
            } else {
                return StockClassification.NORMAL;
            }
        }
    }

    public void checkExpiration() {
        if (expirationDate == null) {
            return;
        }

        LocalDate today = LocalDate.now();
        LocalDate expiryDate = expirationDate.getValue();
        
        if (expiryDate.isBefore(today)) {
            this.classification = StockClassification.EXPIRED;
            addDomainEvent(new StockExpiredEvent(this.id, this.productId, expiryDate));
        } else {
            long daysUntilExpiry = java.time.temporal.ChronoUnit.DAYS.between(today, expiryDate);
            
            if (daysUntilExpiry <= 7 && classification != StockClassification.CRITICAL) {
                this.classification = StockClassification.CRITICAL;
                addDomainEvent(new StockExpiringAlertEvent(
                    this.id,
                    this.productId,
                    expiryDate,
                    7
                ));
            } else if (daysUntilExpiry <= 30 && classification == StockClassification.NORMAL) {
                this.classification = StockClassification.NEAR_EXPIRY;
                addDomainEvent(new StockExpiringAlertEvent(
                    this.id,
                    this.productId,
                    expiryDate,
                    30
                ));
            }
        }
        
        this.lastModifiedAt = LocalDateTime.now();
    }

    public boolean canBePicked() {
        return classification != StockClassification.EXPIRED && quantity.getValue() > 0;
    }

    // Getters
    public ProductId getProductId() {
        return productId;
    }

    public LocationId getLocationId() {
        return locationId;
    }

    public Quantity getQuantity() {
        return quantity;
    }

    public ExpirationDate getExpirationDate() {
        return expirationDate;
    }

    public StockClassification getClassification() {
        return classification;
    }
}
```

### Aggregate: StockLevel

**Aggregate Root:** `StockLevel`

**Identity:** `StockLevelId` (Value Object)

**Responsibilities:**

- Monitor stock levels per product/location
- Enforce minimum and maximum thresholds
- Generate restock requests when stock falls below minimum

**Value Objects:**

- `StockLevelId`
- `ProductId`
- `LocationId` (optional, null for warehouse-level)
- `CurrentQuantity`
- `MinimumQuantity`
- `MaximumQuantity`

**Domain Events:**

- `StockLevelUpdatedEvent`
- `StockLevelBelowMinimumEvent`
- `StockLevelAboveMaximumEvent`
- `RestockRequestGeneratedEvent`

**Business Rules:**

- Stock levels cannot exceed maximum threshold
- Stock levels below minimum trigger restock request
- Restock requests are generated only once per threshold breach
- Stock levels updated on stock movements, picking, returns

**Java Implementation:**

```java
package com.ccbsa.wms.stockmanagement.domain.stockmanagement.domaincore;

import java.time.LocalDateTime;

/**
 * Aggregate Root: StockLevel
 * Monitors stock levels and generates restock requests
 */
public class StockLevel extends AggregateRoot<StockLevelId> {

    private ProductId productId;
    private LocationId locationId; // null for warehouse-level
    private WarehouseId warehouseId;
    private CurrentQuantity currentQuantity;
    private MinimumQuantity minimumQuantity;
    private MaximumQuantity maximumQuantity;
    private boolean restockRequestPending;
    private LocalDateTime lastUpdatedAt;

    private StockLevel() {
        // Private constructor for builder
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private StockLevel stockLevel = new StockLevel();

        public Builder stockLevelId(StockLevelId id) {
            stockLevel.id = id;
            return this;
        }

        public Builder productId(ProductId productId) {
            stockLevel.productId = productId;
            return this;
        }

        public Builder locationId(LocationId locationId) {
            stockLevel.locationId = locationId;
            return this;
        }

        public Builder warehouseId(WarehouseId warehouseId) {
            stockLevel.warehouseId = warehouseId;
            return this;
        }

        public Builder currentQuantity(CurrentQuantity quantity) {
            stockLevel.currentQuantity = quantity;
            return this;
        }

        public Builder minimumQuantity(MinimumQuantity minimum) {
            stockLevel.minimumQuantity = minimum;
            return this;
        }

        public Builder maximumQuantity(MaximumQuantity maximum) {
            stockLevel.maximumQuantity = maximum;
            return this;
        }

        public StockLevel build() {
            validate();
            stockLevel.lastUpdatedAt = LocalDateTime.now();
            return stockLevel;
        }

        private void validate() {
            if (stockLevel.id == null) {
                throw new IllegalArgumentException("StockLevelId is required");
            }
            if (stockLevel.productId == null) {
                throw new IllegalArgumentException("ProductId is required");
            }
            if (stockLevel.warehouseId == null) {
                throw new IllegalArgumentException("WarehouseId is required");
            }
            if (stockLevel.currentQuantity == null) {
                throw new IllegalArgumentException("CurrentQuantity is required");
            }
            if (stockLevel.minimumQuantity == null) {
                throw new IllegalArgumentException("MinimumQuantity is required");
            }
            if (stockLevel.maximumQuantity == null) {
                throw new IllegalArgumentException("MaximumQuantity is required");
            }
        }
    }

    public void updateQuantity(Quantity newQuantity) {
        CurrentQuantity updatedQuantity = CurrentQuantity.of(newQuantity.getValue());
        
        // Check maximum threshold
        if (updatedQuantity.getValue() > maximumQuantity.getValue()) {
            addDomainEvent(new StockLevelAboveMaximumEvent(
                this.id,
                this.productId,
                updatedQuantity.getValue(),
                maximumQuantity.getValue()
            ));
            throw new IllegalArgumentException("Stock level exceeds maximum threshold");
        }
        
        this.currentQuantity = updatedQuantity;
        this.lastUpdatedAt = LocalDateTime.now();
        
        addDomainEvent(new StockLevelUpdatedEvent(
            this.id,
            this.productId,
            this.locationId,
            updatedQuantity.getValue()
        ));
        
        // Check minimum threshold
        checkMinimumThreshold();
    }

    public void decreaseQuantity(Quantity amount) {
        if (amount.getValue() > currentQuantity.getValue()) {
            throw new IllegalArgumentException("Cannot decrease quantity below zero");
        }
        
        CurrentQuantity newQuantity = CurrentQuantity.of(
            currentQuantity.getValue() - amount.getValue()
        );
        updateQuantity(Quantity.of(newQuantity.getValue()));
    }

    public void increaseQuantity(Quantity amount) {
        CurrentQuantity newQuantity = CurrentQuantity.of(
            currentQuantity.getValue() + amount.getValue()
        );
        updateQuantity(Quantity.of(newQuantity.getValue()));
    }

    private void checkMinimumThreshold() {
        if (currentQuantity.getValue() < minimumQuantity.getValue() && !restockRequestPending) {
            this.restockRequestPending = true;
            
            Quantity requestedQuantity = Quantity.of(
                maximumQuantity.getValue() - currentQuantity.getValue()
            );
            
            addDomainEvent(new StockLevelBelowMinimumEvent(
                this.id,
                this.productId,
                this.locationId,
                currentQuantity.getValue(),
                minimumQuantity.getValue()
            ));
            
            addDomainEvent(new RestockRequestGeneratedEvent(
                RestockRequestId.generate(),
                this.productId,
                this.warehouseId,
                this.locationId,
                currentQuantity.getValue(),
                minimumQuantity.getValue(),
                requestedQuantity.getValue(),
                RestockPriority.NORMAL
            ));
        }
    }

    public void markRestockRequestFulfilled() {
        this.restockRequestPending = false;
    }

    // Getters
    public ProductId getProductId() {
        return productId;
    }

    public CurrentQuantity getCurrentQuantity() {
        return currentQuantity;
    }
}
```

### Value Objects

#### ConsignmentId

```java
package com.ccbsa.wms.stockmanagement.domain.stockmanagement.domaincore;

import java.util.UUID;

public final class ConsignmentId {
    private final UUID value;

    private ConsignmentId(UUID value) {
        if (value == null) {
            throw new IllegalArgumentException("ConsignmentId value cannot be null");
        }
        this.value = value;
    }

    public static ConsignmentId of(UUID value) {
        return new ConsignmentId(value);
    }

    public static ConsignmentId generate() {
        return new ConsignmentId(UUID.randomUUID());
    }

    public UUID getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getValue()) return false;
        ConsignmentId that = (ConsignmentId) o;
        return value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
```

#### ConsignmentReference

```java
package com.ccbsa.wms.stockmanagement.domain.stockmanagement.domaincore;

public final class ConsignmentReference {
    private final String value;

    private ConsignmentReference(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("ConsignmentReference cannot be null or empty");
        }
        if (value.length() > 50) {
            throw new IllegalArgumentException("ConsignmentReference cannot exceed 50 characters");
        }
        this.value = value.trim();
    }

    public static ConsignmentReference of(String value) {
        return new ConsignmentReference(value);
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getValue()) return false;
        ConsignmentReference that = (ConsignmentReference) o;
        return value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return value;
    }
}
```

#### Quantity

```java
package com.ccbsa.wms.stockmanagement.domain.stockmanagement.domaincore;

public final class Quantity {
    private final int value;

    private Quantity(int value) {
        if (value < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative");
        }
        this.value = value;
    }

    public static Quantity of(int value) {
        return new Quantity(value);
    }

    public int getValue() {
        return value;
    }

    public Quantity add(Quantity other) {
        return new Quantity(this.value + other.value);
    }

    public Quantity subtract(Quantity other) {
        if (other.value > this.value) {
            throw new IllegalArgumentException("Cannot subtract larger quantity");
        }
        return new Quantity(this.value - other.value);
    }

    public boolean isGreaterThan(Quantity other) {
        return this.value > other.value;
    }

    public boolean isLessThan(Quantity other) {
        return this.value < other.value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getValue()) return false;
        Quantity quantity = (Quantity) o;
        return value == quantity.value;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(value);
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
```

#### ExpirationDate

```java
package com.ccbsa.wms.stockmanagement.domain.stockmanagement.domaincore;

import java.time.LocalDate;

public final class ExpirationDate {
    private final LocalDate value;

    private ExpirationDate(LocalDate value) {
        if (value == null) {
            throw new IllegalArgumentException("ExpirationDate cannot be null");
        }
        this.value = value;
    }

    public static ExpirationDate of(LocalDate value) {
        return new ExpirationDate(value);
    }

    public LocalDate getValue() {
        return value;
    }

    public boolean isExpired() {
        return value.isBefore(LocalDate.now());
    }

    public boolean isExpiringWithinDays(int days) {
        LocalDate threshold = LocalDate.now().plusDays(days);
        return !value.isAfter(threshold) && !isExpired();
    }

    public long daysUntilExpiration() {
        return java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getValue()) return false;
        ExpirationDate that = (ExpirationDate) o;
        return value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
```

#### StockClassification

```java
package com.ccbsa.wms.stockmanagement.domain.stockmanagement.domaincore;

public enum StockClassification {
    EXPIRED("Expired"),
    CRITICAL("Critical - Expiring within 7 days"),
    NEAR_EXPIRY("Near Expiry - Expiring within 30 days"),
    NORMAL("Normal"),
    EXTENDED_SHELF_LIFE("Extended Shelf Life");

    private final String description;

    StockClassification(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
```

---

## Location Management Context

### Aggregate: Location

**Aggregate Root:** `Location`

**Identity:** `LocationId` (Value Object)

**Responsibilities:**

- Manage warehouse location master data
- Track location status and capacity
- Assign locations to stock based on FEFO principles

**Value Objects:**

- `LocationId`
- `LocationBarcode`
- `LocationCode`
- `LocationStatus`
- `LocationCapacity`
- `LocationCoordinates`

**Domain Events:**

- `LocationCreatedEvent`
- `LocationAssignedEvent`
- `LocationStatusChangedEvent`
- `LocationCapacityExceededEvent`
- `LocationBlockedEvent`

**Business Rules:**

- All locations must have unique barcodes
- Location capacity cannot be exceeded
- Blocked locations cannot be assigned
- Location status must be valid

### Aggregate: StockMovement

**Aggregate Root:** `StockMovement`

**Identity:** `StockMovementId` (Value Object)

**Responsibilities:**

- Track all stock movements on warehouse floor
- Record movement details (source, destination, quantity, reason)
- Maintain complete audit trail

**Value Objects:**

- `StockMovementId`
- `SourceLocationId`
- `DestinationLocationId`
- `ProductId`
- `Quantity`
- `MovementReason`
- `UserId`

**Domain Events:**

- `StockMovementInitiatedEvent`
- `StockMovementCompletedEvent`
- `StockMovementCancelledEvent`

**Business Rules:**

- Source and destination locations must be different
- Quantity must be positive
- Movement reason must be provided
- User must be authenticated

---

## Product Context

### Aggregate: Product

**Aggregate Root:** `Product`

**Identity:** `ProductId` (Value Object)

**Responsibilities:**

- Manage product master data
- Validate product barcodes
- Support multiple barcode formats

**Value Objects:**

- `ProductId`
- `ProductCode`
- `ProductBarcode`
- `ProductDescription`
- `UnitOfMeasure`

**Domain Events:**

- `ProductCreatedEvent`
- `ProductUpdatedEvent`
- `ProductBarcodeUpdatedEvent`

**Business Rules:**

- Product codes must be unique
- Products must have at least one barcode
- Barcode formats must be valid (EAN-13, Code 128, etc.)

---

## Order Fulfillment Context

### Picking Sub-Context

#### Aggregate: PickingList

**Aggregate Root:** `PickingList`

**Identity:** `PickingListId` (Value Object)

**Responsibilities:**

- Manage picking lists from D365
- Plan loads and orders
- Optimize picking locations

**Value Objects:**

- `PickingListId`
- `LoadNumber`
- `OrderNumber`
- `CustomerInfo`
- `PickingStatus`

**Domain Events:**

- `PickingListReceivedEvent`
- `LoadPlannedEvent`
- `PickingListCompletedEvent`

#### Aggregate: PickingTask

**Aggregate Root:** `PickingTask`

**Identity:** `PickingTaskId` (Value Object)

**Responsibilities:**

- Manage individual picking tasks
- Guide picking operations
- Validate picked quantities

**Domain Events:**

- `PickingTaskCreatedEvent`
- `PickingTaskCompletedEvent`
- `PartialPickingCompletedEvent`

### Returns Sub-Context

#### Aggregate: Return

**Aggregate Root:** `Return`

**Identity:** `ReturnId` (Value Object)

**Responsibilities:**

- Process returns (partial, full, damage-in-transit)
- Assign return locations
- Maintain return history

**Value Objects:**

- `ReturnId`
- `OrderId`
- `ReturnReason`
- `ReturnCondition`
- `DamageType`

**Domain Events:**

- `ReturnInitiatedEvent`
- `ReturnProcessedEvent`
- `ReturnLocationAssignedEvent`
- `DamageRecordedEvent`
- `ReturnReconciledEvent`

---

## Inventory Control Context

### Aggregate: StockCount

**Aggregate Root:** `StockCount`

**Identity:** `StockCountId` (Value Object)

**Responsibilities:**

- Manage stock count sessions
- Generate electronic worksheets
- Track count progress

**Value Objects:**

- `StockCountId`
- `WorksheetId`
- `CountType` (CYCLE, FULL_PHYSICAL)
- `StockCountStatus`

**Domain Events:**

- `StockCountInitiatedEvent`
- `StockCountEntryAddedEvent`
- `StockCountCompletedEvent`

### Aggregate: StockCountVariance

**Aggregate Root:** `StockCountVariance`

**Identity:** `VarianceId` (Value Object)

**Responsibilities:**

- Identify variances between counted and system stock
- Support variance investigation
- Generate variance reports

**Value Objects:**

- `VarianceId`
- `CountQuantity`
- `SystemQuantity`
- `VarianceAmount`
- `VarianceReason`

**Domain Events:**

- `StockCountVarianceIdentifiedEvent`
- `VarianceInvestigatedEvent`
- `VarianceResolvedEvent`

### Aggregate: Reconciliation

**Aggregate Root:** `Reconciliation`

**Identity:** `ReconciliationId` (Value Object)

**Responsibilities:**

- Reconcile stock counts with D365
- Update D365 with reconciliation results
- Track reconciliation status

**Value Objects:**

- `ReconciliationId`
- `ReconciliationStatus`
- `D365UpdateStatus`

**Domain Events:**

- `ReconciliationInitiatedEvent`
- `ReconciliationCompletedEvent`
- `D365ReconciliationUpdateSentEvent`
- `D365ReconciliationUpdateConfirmedEvent`

---

## Tenant Management Context

### Context Overview

**Purpose:** Manages tenant (LDP) lifecycle, configuration, and metadata.

**Key Concepts:**

- Tenant - Represents an LDP (Local Distribution Partner)
- Tenant Status - Active, Inactive, Suspended, Pending
- Tenant Configuration - Settings, preferences, limits
- Tenant Onboarding - Process of adding new tenants

### Aggregate: Tenant

**Aggregate Root:** `Tenant`

**Identity:** `TenantId` (Value Object from common-domain)

**Responsibilities:**

- Manage tenant lifecycle (create, activate, deactivate, suspend)
- Store tenant metadata (name, contact information, address)
- Manage tenant configuration (settings, preferences, limits)
- Validate tenant status
- Track tenant onboarding progress
- Trigger tenant schema creation (for schema-per-tenant isolation)

**Entities:**

- `Tenant` (Aggregate Root)

**Value Objects:**

- `TenantId` - Unique tenant identifier (from common-domain)
- `TenantName` - Tenant name with validation
- `TenantStatus` - Enum: PENDING, ACTIVE, INACTIVE, SUSPENDED
- `ContactInformation` - Contact details (emailAddress, phone, address)
- `TenantConfiguration` - Tenant-specific settings and preferences

**Domain Events:**

- `TenantCreatedEvent` - Published when new tenant is created
- `TenantActivatedEvent` - Published when tenant is activated
- `TenantDeactivatedEvent` - Published when tenant is deactivated
- `TenantSuspendedEvent` - Published when tenant is suspended
- `TenantConfigurationUpdatedEvent` - Published when tenant configuration changes
- `TenantSchemaCreatedEvent` - Published when tenant schema is created (for schema-per-tenant)

**Business Rules:**

- Tenant ID must be unique
- Tenant name is required
- Tenant status transitions must be valid:
    - PENDING → ACTIVE
    - ACTIVE → INACTIVE or SUSPENDED
    - SUSPENDED → ACTIVE or INACTIVE
    - INACTIVE → ACTIVE
- Cannot delete active tenant (must deactivate first)
- Tenant schema must be created during activation (for schema-per-tenant)
- All tenant-aware aggregates must reference valid tenant

---

## Shared Kernel

### Common Value Objects

**TenantId:**

```java
package com.ccbsa.common.domain;

public final class TenantId {
    private final String value;
    // Implementation similar to other value objects
}
```

**UserId:**

```java
package com.ccbsa.common.domain;

public final class UserId {
    private final String value;
    // Implementation similar to other value objects
}
```

**WarehouseId:**

```java
package com.ccbsa.common.domain;

public final class WarehouseId {
    private final String value;
    // Implementation similar to other value objects
}
```

### Base Classes

**AggregateRoot:**

```java
package com.ccbsa.common.domain;

import java.util.ArrayList;
import java.util.List;

public abstract class AggregateRoot<ID> {
    protected ID id;
    private List<DomainEvent<?>> domainEvents = new ArrayList<>();
    private int version = 0;

    protected void addDomainEvent(DomainEvent<?> event) {
        domainEvents.add(event);
    }

    public List<DomainEvent<?>> getDomainEvents() {
        return new ArrayList<>(domainEvents);
    }

    public void clearDomainEvents() {
        domainEvents.clear();
    }

    public ID getId() {
        return id;
    }

    public int getVersion() {
        return version;
    }

    public void incrementVersion() {
        this.version++;
    }
}
```

**DomainEvent:**

```java
package com.ccbsa.common.domain;

import java.time.LocalDateTime;
import java.util.UUID;

public abstract class DomainEvent<T extends AggregateRoot<?>> {
    private final UUID eventId;
    private final LocalDateTime occurredAt;
    private final String aggregateType;
    private final Object aggregateId;
    private final int eventVersion;

    protected DomainEvent(Object aggregateId) {
        this.eventId = UUID.randomUUID();
        this.occurredAt = LocalDateTime.now();
        this.aggregateType = getAggregateType();
        this.aggregateId = aggregateId;
        this.eventVersion = 1;
    }

    public UUID getEventId() {
        return eventId;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    public String getAggregateType() {
        return this.getClass().getSimpleName().replace("Event", "");
    }

    public Object getAggregateId() {
        return aggregateId;
    }

    public int getEventVersion() {
        return eventVersion;
    }
}
```

---

## Domain Events Catalog

### Stock Management Events

- `StockConsignmentReceivedEvent`
- `StockConsignmentValidatedEvent`
- `StockConsignmentConfirmedEvent`
- `PartialConsignmentReceivedEvent`
- `StockItemCreatedEvent`
- `StockItemClassifiedEvent`
- `StockExpiringAlertEvent`
- `StockExpiredEvent`
- `StockLevelUpdatedEvent`
- `StockLevelBelowMinimumEvent`
- `StockLevelAboveMaximumEvent`
- `RestockRequestGeneratedEvent`

### Location Management Events

- `LocationCreatedEvent`
- `LocationAssignedEvent`
- `LocationStatusChangedEvent`
- `LocationCapacityExceededEvent`
- `LocationBlockedEvent`
- `StockMovementInitiatedEvent`
- `StockMovementCompletedEvent`
- `StockMovementCancelledEvent`

### Product Events

- `ProductCreatedEvent`
- `ProductUpdatedEvent`
- `ProductBarcodeUpdatedEvent`

### Picking Events

- `PickingListReceivedEvent`
- `LoadPlannedEvent`
- `PickingTaskCreatedEvent`
- `PickingTaskCompletedEvent`
- `PartialPickingCompletedEvent`
- `PickingCompletedEvent`

### Returns Events

- `ReturnInitiatedEvent`
- `ReturnProcessedEvent`
- `ReturnLocationAssignedEvent`
- `DamageRecordedEvent`
- `ReturnReconciledEvent`

### Reconciliation Events

- `StockCountInitiatedEvent`
- `StockCountEntryAddedEvent`
- `StockCountCompletedEvent`
- `StockCountVarianceIdentifiedEvent`
- `VarianceInvestigatedEvent`
- `VarianceResolvedEvent`
- `ReconciliationInitiatedEvent`
- `ReconciliationCompletedEvent`
- `D365ReconciliationUpdateSentEvent`
- `D365ReconciliationUpdateConfirmedEvent`

---

## Appendix

### Domain Model Diagrams

*Note: UML diagrams would be included here in production documentation*

### Glossary

| Term                    | Definition                                           |
|-------------------------|------------------------------------------------------|
| **Aggregate**           | Cluster of domain objects treated as a single unit   |
| **Aggregate Root**      | Entity that serves as entry point to aggregate       |
| **Bounded Context**     | Explicit boundary where domain model applies         |
| **Domain Event**        | Event representing something that happened in domain |
| **Entity**              | Object with unique identity                          |
| **Value Object**        | Immutable object defined by attributes               |
| **Ubiquitous Language** | Domain terms used consistently                       |

---

**Document Control**

- **Version History:** This document will be version controlled with change tracking
- **Review Cycle:** This document will be reviewed weekly during architecture phase
- **Distribution:** This document will be distributed to all domain modeling team members

