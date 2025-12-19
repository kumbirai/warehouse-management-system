# Stock Consignment Creation Implementation Plan

## US-1.1.5: Create Stock Consignment

**Service:** Stock Management Service  
**Priority:** Must Have  
**Story Points:** 8  
**Sprint:** Sprint 2

---

## Table of Contents

1. [Overview](#overview)
2. [Domain Model Design](#domain-model-design)
3. [Backend Implementation](#backend-implementation)
4. [Testing Strategy](#testing-strategy)
5. [Acceptance Criteria Validation](#acceptance-criteria-validation)

---

## Overview

### User Story

**As a** warehouse operator  
**I want** to create stock consignments  
**So that** I can track incoming stock consignments

### Business Requirements

- Create stock consignment aggregate with proper domain modeling
- Store consignment reference, warehouse ID, received date
- Store line items with product codes, quantities, expiration dates
- Initialize consignment status as RECEIVED
- Validate business rules before creation
- Publish `StockConsignmentReceivedEvent` after successful creation
- Support multi-tenant isolation
- Maintain audit trail (created at, created by)

### Technical Requirements

- Follow DDD, Clean Hexagonal Architecture, CQRS, Event-Driven Choreography
- Pure Java domain core (no framework dependencies)
- Builder pattern for aggregate creation
- Domain events for state changes
- Multi-tenant support

---

## Domain Model Design

### StockConsignment Aggregate Root

**Package:** `com.ccbsa.wms.stock.domain.core.entity`

```java
public class StockConsignment extends TenantAwareAggregateRoot<ConsignmentId> {
    
    private ConsignmentReference consignmentReference;
    private WarehouseId warehouseId;
    private ConsignmentStatus status;
    private LocalDateTime receivedAt;
    private LocalDateTime confirmedAt;
    private String receivedBy;
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
        
        public Builder tenantId(TenantId tenantId) {
            consignment.tenantId = tenantId;
            return this;
        }
        
        public Builder consignmentReference(ConsignmentReference reference) {
            consignment.consignmentReference = reference;
            return this;
        }
        
        public Builder warehouseId(WarehouseId warehouseId) {
            consignment.warehouseId = warehouseId;
            return this;
        }
        
        public Builder receivedAt(LocalDateTime receivedAt) {
            consignment.receivedAt = receivedAt;
            return this;
        }
        
        public Builder receivedBy(String receivedBy) {
            consignment.receivedBy = receivedBy;
            return this;
        }
        
        public Builder lineItems(List<ConsignmentLineItem> lineItems) {
            consignment.lineItems = new ArrayList<>(lineItems);
            return this;
        }
        
        public StockConsignment build() {
            validate();
            consignment.status = ConsignmentStatus.RECEIVED;
            consignment.addDomainEvent(new StockConsignmentReceivedEvent(
                consignment.id,
                consignment.consignmentReference,
                consignment.tenantId,
                consignment.warehouseId,
                consignment.lineItems
            ));
            return consignment;
        }
        
        private void validate() {
            if (consignment.id == null) {
                throw new IllegalArgumentException("ConsignmentId is required");
            }
            if (consignment.tenantId == null) {
                throw new IllegalArgumentException("TenantId is required");
            }
            if (consignment.consignmentReference == null) {
                throw new IllegalArgumentException("ConsignmentReference is required");
            }
            if (consignment.warehouseId == null) {
                throw new IllegalArgumentException("WarehouseId is required");
            }
            if (consignment.receivedAt == null) {
                throw new IllegalArgumentException("ReceivedAt is required");
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
            this.warehouseId
        ));
    }
    
    // Getters
    public ConsignmentReference getConsignmentReference() {
        return consignmentReference;
    }
    
    public WarehouseId getWarehouseId() {
        return warehouseId;
    }
    
    public ConsignmentStatus getStatus() {
        return status;
    }
    
    public List<ConsignmentLineItem> getLineItems() {
        return new ArrayList<>(lineItems);
    }
}
```

### Value Objects

**ConsignmentId:**

```java
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
        if (o == null || getClass() != o.getClass()) return false;
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

**ConsignmentReference:**

```java
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
    
    // equals, hashCode, toString
}
```

**ConsignmentLineItem:**

```java
public final class ConsignmentLineItem {
    private final ProductCode productCode;
    private final Quantity quantity;
    private final ExpirationDate expirationDate;
    private final String batchNumber;
    
    private ConsignmentLineItem(Builder builder) {
        this.productCode = builder.productCode;
        this.quantity = builder.quantity;
        this.expirationDate = builder.expirationDate;
        this.batchNumber = builder.batchNumber;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private ProductCode productCode;
        private Quantity quantity;
        private ExpirationDate expirationDate;
        private String batchNumber;
        
        public Builder productCode(ProductCode productCode) {
            this.productCode = productCode;
            return this;
        }
        
        public Builder quantity(Quantity quantity) {
            this.quantity = quantity;
            return this;
        }
        
        public Builder expirationDate(ExpirationDate expirationDate) {
            this.expirationDate = expirationDate;
            return this;
        }
        
        public Builder batchNumber(String batchNumber) {
            this.batchNumber = batchNumber;
            return this;
        }
        
        public ConsignmentLineItem build() {
            if (productCode == null) {
                throw new IllegalArgumentException("ProductCode is required");
            }
            if (quantity == null) {
                throw new IllegalArgumentException("Quantity is required");
            }
            return new ConsignmentLineItem(this);
        }
    }
    
    // Getters
}
```

**ConsignmentStatus:**

```java
public enum ConsignmentStatus {
    RECEIVED,
    CONFIRMED,
    CANCELLED
}
```

### Domain Events

**StockConsignmentReceivedEvent:**

```java
public class StockConsignmentReceivedEvent extends StockManagementEvent<StockConsignment> {
    
    private final ConsignmentReference consignmentReference;
    private final WarehouseId warehouseId;
    private final List<ConsignmentLineItem> lineItems;
    
    public StockConsignmentReceivedEvent(
            ConsignmentId aggregateId,
            ConsignmentReference consignmentReference,
            TenantId tenantId,
            WarehouseId warehouseId,
            List<ConsignmentLineItem> lineItems
    ) {
        super(aggregateId, tenantId);
        this.consignmentReference = consignmentReference;
        this.warehouseId = warehouseId;
        this.lineItems = new ArrayList<>(lineItems);
    }
    
    public ConsignmentReference getConsignmentReference() {
        return consignmentReference;
    }
    
    public WarehouseId getWarehouseId() {
        return warehouseId;
    }
    
    public List<ConsignmentLineItem> getLineItems() {
        return new ArrayList<>(lineItems);
    }
}
```

**StockManagementEvent (Base):**

```java
public abstract class StockManagementEvent<T> extends DomainEvent<T> {
    
    protected StockManagementEvent(ConsignmentId aggregateId, TenantId tenantId) {
        super(aggregateId, tenantId);
    }
}
```

---

## Backend Implementation

### Command Handler

See [02-Consignment-Manual-Entry-Implementation-Plan.md](02-Consignment-Manual-Entry-Implementation-Plan.md) for command handler implementation.

### Repository Port

```java
public interface StockConsignmentRepository {
    void save(StockConsignment consignment);
    Optional<StockConsignment> findById(ConsignmentId id);
    Optional<StockConsignment> findByConsignmentReferenceAndTenantId(
        ConsignmentReference reference, 
        TenantId tenantId
    );
    boolean existsByConsignmentReferenceAndTenantId(
        ConsignmentReference reference, 
        TenantId tenantId
    );
    List<StockConsignment> findByTenantIdAndStatus(
        TenantId tenantId, 
        ConsignmentStatus status
    );
}
```

---

## Testing Strategy

### Unit Tests

- **Domain Model** - Test business logic and validation
- **Builder Pattern** - Test aggregate creation
- **Domain Events** - Test event publishing
- **Value Objects** - Test value object validation

### Integration Tests

- **Repository** - Test persistence
- **Command Handler** - Test full creation flow
- **Event Publishing** - Test event publication

---

## Acceptance Criteria Validation

- ✅ System creates stock consignment aggregate with proper domain modeling
- ✅ System stores consignment reference, warehouse ID, received date
- ✅ System stores line items with product codes, quantities, expiration dates
- ✅ System initializes consignment status as RECEIVED
- ✅ System validates business rules before creation
- ✅ System publishes `StockConsignmentReceivedEvent` after successful creation
- ✅ System supports multi-tenant isolation
- ✅ System maintains audit trail (created at, created by)

---

**Document Control**

- **Version:** 1.0
- **Date:** 2025-01
- **Status:** Draft

