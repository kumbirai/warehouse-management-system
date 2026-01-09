# Allocate Stock for Picking Orders Implementation Plan

## US-5.2.1: Allocate Stock for Picking Orders

**Service:** Stock Management Service  
**Priority:** Must Have  
**Story Points:** 8  
**Sprint:** Sprint 4

---

## Table of Contents

1. [Overview](#overview)
2. [UI Design](#ui-design)
3. [Domain Model Design](#domain-model-design)
4. [Backend Implementation](#backend-implementation)
5. [Frontend Implementation](#frontend-implementation)
6. [Data Flow](#data-flow)
7. [Testing Strategy](#testing-strategy)
8. [Acceptance Criteria Validation](#acceptance-criteria-validation)

---

## Overview

### User Story

**As a** warehouse operator  
**I want** to allocate stock for picking orders  
**So that** I can reserve stock for specific orders and prevent double allocation

### Business Requirements

- System allows allocating stock by product and location
- System validates sufficient available stock before allocation
- System supports allocation types: PICKING_ORDER, RESERVATION, OTHER
- System tracks allocated quantity separately from available quantity
- System prevents allocation exceeding available stock
- System supports FEFO-based allocation (allocates earliest expiring stock first)
- System publishes `StockAllocatedEvent` after successful allocation
- System allows querying allocations by reference ID (e.g., order ID)

### Technical Requirements

- Follow DDD, Clean Hexagonal Architecture, CQRS, Event-Driven Choreography
- Pure Java domain core (no framework dependencies)
- Allocation logic in domain aggregate
- Domain events for allocation state changes
- Multi-tenant support
- Move common value objects to `common-domain` (DRY principle)
- FEFO allocation algorithm (earliest expiration date first)
- Optimistic locking to prevent concurrent allocation issues

---

## UI Design

### Stock Allocation Form

**Component:** `StockAllocationForm.tsx`

**Fields:**

- **Product** (required, autocomplete with barcode scanning)
- **Location** (optional, autocomplete with barcode scanning - if not provided, system suggests based on FEFO)
- **Quantity** (required, number input with validation)
- **Allocation Type** (required, dropdown: PICKING_ORDER, RESERVATION, OTHER)
- **Reference ID** (required for PICKING_ORDER, optional for others - e.g., order ID, picking list ID)
- **Notes** (optional, textarea)

**Validation:**

- Product must exist and have available stock
- Quantity must be positive and not exceed available stock
- Location must exist and have stock (if provided)
- Reference ID required for PICKING_ORDER type
- Real-time validation feedback

**Actions:**

- **Allocate** - Submit form to allocate stock
- **Cancel** - Navigate back to allocation list
- **Suggest Location** - Auto-suggest location based on FEFO principles

**UI Flow:**

1. User navigates to "Stock Management" → "Allocate Stock"
2. Form displays with all fields
3. User scans or enters product barcode
4. System displays available stock levels and locations
5. User optionally scans location barcode or clicks "Suggest Location" (FEFO-based)
6. User enters quantity and allocation type
7. User enters reference ID (if PICKING_ORDER)
8. User clicks "Allocate"
9. System validates and allocates stock
10. Success message displayed with allocation details
11. User redirected to allocation detail page

### Stock Allocation List View

**Component:** `StockAllocationList.tsx`

**Features:**

- List all allocations with pagination
- Filter by product, location, allocation type, reference ID, status
- Search by product code, reference ID
- Sort by allocation date, expiration date, quantity
- Display available vs allocated quantities
- Show allocation status (ALLOCATED, RELEASED, PICKED)

**UI Flow:**

1. User navigates to "Stock Management" → "Stock Allocations"
2. System displays list of allocations
3. User can filter by:
    - Product
    - Location
    - Allocation type
    - Reference ID
    - Status
    - Date range
4. User can click on allocation to view details
5. User can release allocation (if not yet picked)

### Stock Allocation Detail View

**Component:** `StockAllocationDetail.tsx`

**Features:**

- Display allocation details
- Show product information
- Show location information
- Show expiration date (for FEFO reference)
- Show allocation status
- Show reference ID and related order/picking list
- Action buttons: Release Allocation, View Stock Item

**UI Flow:**

1. User clicks on allocation from list
2. System displays allocation details
3. User can release allocation (if status is ALLOCATED)
4. User can view related stock item
5. User can view related order/picking list (if reference ID provided)

### FEFO Location Suggestion Component

**Component:** `FEFOLocationSuggestion.tsx`

**Features:**

- Display suggested locations based on FEFO principles
- Show expiration dates for each location
- Show available quantities per location
- Allow user to select suggested location or enter manually

**UI Flow:**

1. User enters product in allocation form
2. System queries available stock by product
3. System sorts by expiration date (earliest first)
4. System displays suggested locations with:
    - Location code
    - Expiration date
    - Available quantity
    - Distance to picking zone (if available)
5. User can select suggested location or scan/enter different location

---

## Domain Model Design

### StockAllocation Aggregate Root

**File:** `stock-management-domain-core/src/main/java/com/ccbsa/wms/stock/domain/core/entity/StockAllocation.java`

```java
package com.ccbsa.wms.stock.domain.core.entity;

import com.ccbsa.common.domain.AggregateRoot;
import com.ccbsa.common.domain.TenantId;
import com.ccbsa.wms.stock.domain.core.event.StockAllocatedEvent;
import com.ccbsa.wms.stock.domain.core.event.StockAllocationReleasedEvent;
import com.ccbsa.wms.stock.domain.core.valueobject.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * StockAllocation Aggregate Root
 * 
 * Represents a stock allocation for picking orders or reservations.
 * Tracks allocated quantity separately from available quantity.
 * 
 * Business Rules:
 * - Allocation quantity cannot exceed available stock
 * - Allocation must reference valid stock item
 * - Allocation can be released if not yet picked
 * - FEFO allocation prioritizes earliest expiring stock
 */
public class StockAllocation extends AggregateRoot<StockAllocationId> {
    
    private StockAllocationId id;
    private TenantId tenantId;
    private ProductId productId;
    private LocationId locationId;  // Optional - null for product-wide allocation
    private StockItemId stockItemId; // Reference to specific stock item
    private Quantity quantity;
    private AllocationType allocationType;
    private String referenceId;  // Order ID, picking list ID, etc.
    private AllocationStatus status;
    private LocalDateTime allocatedAt;
    private LocalDateTime releasedAt;
    private String allocatedBy;
    private String notes;
    
    // Private constructor - use builder
    private StockAllocation() {
        super();
    }
    
    /**
     * Builder pattern for creating StockAllocation
     */
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private StockAllocation allocation = new StockAllocation();
        
        public Builder id(StockAllocationId id) {
            allocation.id = id;
            return this;
        }
        
        public Builder tenantId(TenantId tenantId) {
            allocation.tenantId = tenantId;
            return this;
        }
        
        public Builder productId(ProductId productId) {
            allocation.productId = productId;
            return this;
        }
        
        public Builder locationId(LocationId locationId) {
            allocation.locationId = locationId;
            return this;
        }
        
        public Builder stockItemId(StockItemId stockItemId) {
            allocation.stockItemId = stockItemId;
            return this;
        }
        
        public Builder quantity(Quantity quantity) {
            allocation.quantity = quantity;
            return this;
        }
        
        public Builder allocationType(AllocationType allocationType) {
            allocation.allocationType = allocationType;
            return this;
        }
        
        public Builder referenceId(String referenceId) {
            allocation.referenceId = referenceId;
            return this;
        }
        
        public Builder allocatedBy(String allocatedBy) {
            allocation.allocatedBy = allocatedBy;
            return this;
        }
        
        public Builder notes(String notes) {
            allocation.notes = notes;
            return this;
        }
        
        public StockAllocation build() {
            // Validate required fields
            if (allocation.id == null) {
                allocation.id = StockAllocationId.of(UUID.randomUUID());
            }
            if (allocation.tenantId == null) {
                throw new IllegalArgumentException("TenantId is required");
            }
            if (allocation.productId == null) {
                throw new IllegalArgumentException("ProductId is required");
            }
            if (allocation.stockItemId == null) {
                throw new IllegalArgumentException("StockItemId is required");
            }
            if (allocation.quantity == null || allocation.quantity.getValue() <= 0) {
                throw new IllegalArgumentException("Quantity must be positive");
            }
            if (allocation.allocationType == null) {
                throw new IllegalArgumentException("AllocationType is required");
            }
            if (allocation.allocationType == AllocationType.PICKING_ORDER && 
                (allocation.referenceId == null || allocation.referenceId.trim().isEmpty())) {
                throw new IllegalArgumentException("ReferenceId is required for PICKING_ORDER allocation");
            }
            
            // Set defaults
            allocation.status = AllocationStatus.ALLOCATED;
            allocation.allocatedAt = LocalDateTime.now();
            
            return allocation;
        }
    }
    
    /**
     * Business logic method: Allocates stock for picking order or reservation.
     * 
     * Business Rules:
     * - Validates allocation quantity doesn't exceed available stock (validated at application service)
     * - Sets allocation status to ALLOCATED
     * - Records allocation timestamp and user
     * - Publishes StockAllocatedEvent
     * 
     * @throws IllegalStateException if allocation cannot be created
     */
    public void allocate() {
        if (this.status != null && this.status != AllocationStatus.RELEASED) {
            throw new IllegalStateException("Allocation already exists and is not released");
        }
        
        this.status = AllocationStatus.ALLOCATED;
        this.allocatedAt = LocalDateTime.now();
        
        // Publish domain event
        addDomainEvent(new StockAllocatedEvent(
            this.id,
            this.tenantId,
            this.productId,
            this.locationId,
            this.stockItemId,
            this.quantity,
            this.allocationType,
            this.referenceId
        ));
    }
    
    /**
     * Business logic method: Releases allocation.
     * 
     * Business Rules:
     * - Only ALLOCATED allocations can be released
     * - Sets status to RELEASED
     * - Records release timestamp
     * - Publishes StockAllocationReleasedEvent
     * 
     * @throws IllegalStateException if allocation cannot be released
     */
    public void release() {
        if (this.status != AllocationStatus.ALLOCATED) {
            throw new IllegalStateException(
                String.format("Cannot release allocation in status: %s", this.status)
            );
        }
        
        this.status = AllocationStatus.RELEASED;
        this.releasedAt = LocalDateTime.now();
        
        // Publish domain event
        addDomainEvent(new StockAllocationReleasedEvent(
            this.id,
            this.tenantId,
            this.productId,
            this.locationId,
            this.stockItemId,
            this.quantity
        ));
    }
    
    /**
     * Business logic method: Marks allocation as picked.
     * 
     * Business Rules:
     * - Only ALLOCATED allocations can be marked as picked
     * - Sets status to PICKED
     * 
     * @throws IllegalStateException if allocation cannot be marked as picked
     */
    public void markAsPicked() {
        if (this.status != AllocationStatus.ALLOCATED) {
            throw new IllegalStateException(
                String.format("Cannot mark allocation as picked in status: %s", this.status)
            );
        }
        
        this.status = AllocationStatus.PICKED;
    }
    
    // Getters
    public StockAllocationId getId() { return id; }
    public TenantId getTenantId() { return tenantId; }
    public ProductId getProductId() { return productId; }
    public LocationId getLocationId() { return locationId; }
    public StockItemId getStockItemId() { return stockItemId; }
    public Quantity getQuantity() { return quantity; }
    public AllocationType getAllocationType() { return allocationType; }
    public String getReferenceId() { return referenceId; }
    public AllocationStatus getStatus() { return status; }
    public LocalDateTime getAllocatedAt() { return allocatedAt; }
    public LocalDateTime getReleasedAt() { return releasedAt; }
    public String getAllocatedBy() { return allocatedBy; }
    public String getNotes() { return notes; }
}
```

### AllocationType Enum

**File:** `common-domain/src/main/java/com/ccbsa/common/domain/valueobject/AllocationType.java`

```java
package com.ccbsa.common.domain.valueobject;

/**
 * Allocation Type Enum
 * 
 * Represents the type of stock allocation.
 * Moved to common-domain for reuse across services (DRY principle).
 */
public enum AllocationType {
    /**
     * Allocation for picking orders
     */
    PICKING_ORDER,
    
    /**
     * General reservation
     */
    RESERVATION,
    
    /**
     * Other allocation types
     */
    OTHER
}
```

### AllocationStatus Enum

**File:** `stock-management-domain-core/src/main/java/com/ccbsa/wms/stock/domain/core/valueobject/AllocationStatus.java`

```java
package com.ccbsa.wms.stock.domain.core.valueobject;

/**
 * Allocation Status Enum
 * 
 * Represents the status of a stock allocation.
 */
public enum AllocationStatus {
    /**
     * Allocation is active
     */
    ALLOCATED,
    
    /**
     * Allocation has been released
     */
    RELEASED,
    
    /**
     * Allocation has been picked
     */
    PICKED
}
```

### StockAllocationId Value Object

**File:** `stock-management-domain-core/src/main/java/com/ccbsa/wms/stock/domain/core/valueobject/StockAllocationId.java`

```java
package com.ccbsa.wms.stock.domain.core.valueobject;

import java.util.UUID;

/**
 * StockAllocationId Value Object
 * 
 * Represents a unique identifier for stock allocation.
 */
public final class StockAllocationId {
    private final UUID value;
    
    private StockAllocationId(UUID value) {
        if (value == null) {
            throw new IllegalArgumentException("StockAllocationId value cannot be null");
        }
        this.value = value;
    }
    
    public static StockAllocationId of(UUID value) {
        return new StockAllocationId(value);
    }
    
    public static StockAllocationId of(String value) {
        return new StockAllocationId(UUID.fromString(value));
    }
    
    public static StockAllocationId generate() {
        return new StockAllocationId(UUID.randomUUID());
    }
    
    public UUID getValue() {
        return value;
    }
    
    public String getValueAsString() {
        return value.toString();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StockAllocationId that = (StockAllocationId) o;
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

### Domain Events

**File:** `stock-management-domain-core/src/main/java/com/ccbsa/wms/stock/domain/core/event/StockAllocatedEvent.java`

```java
package com.ccbsa.wms.stock.domain.core.event;

import com.ccbsa.common.domain.TenantId;
import com.ccbsa.common.messaging.DomainEvent;
import com.ccbsa.wms.stock.domain.core.entity.StockAllocation;
import com.ccbsa.wms.stock.domain.core.valueobject.*;
import java.time.LocalDateTime;

/**
 * StockAllocatedEvent
 * 
 * Published when stock is successfully allocated.
 */
public class StockAllocatedEvent extends DomainEvent<StockAllocation> {
    
    private final StockAllocationId allocationId;
    private final TenantId tenantId;
    private final ProductId productId;
    private final LocationId locationId;
    private final StockItemId stockItemId;
    private final Quantity quantity;
    private final AllocationType allocationType;
    private final String referenceId;
    
    public StockAllocatedEvent(
        StockAllocationId allocationId,
        TenantId tenantId,
        ProductId productId,
        LocationId locationId,
        StockItemId stockItemId,
        Quantity quantity,
        AllocationType allocationType,
        String referenceId
    ) {
        super(
            allocationId.getValueAsString(),
            "StockAllocation",
            tenantId,
            LocalDateTime.now(),
            1
        );
        this.allocationId = allocationId;
        this.tenantId = tenantId;
        this.productId = productId;
        this.locationId = locationId;
        this.stockItemId = stockItemId;
        this.quantity = quantity;
        this.allocationType = allocationType;
        this.referenceId = referenceId;
    }
    
    // Getters
    public StockAllocationId getAllocationId() { return allocationId; }
    public TenantId getTenantId() { return tenantId; }
    public ProductId getProductId() { return productId; }
    public LocationId getLocationId() { return locationId; }
    public StockItemId getStockItemId() { return stockItemId; }
    public Quantity getQuantity() { return quantity; }
    public AllocationType getAllocationType() { return allocationType; }
    public String getReferenceId() { return referenceId; }
}
```

**File:** `stock-management-domain-core/src/main/java/com/ccbsa/wms/stock/domain/core/event/StockAllocationReleasedEvent.java`

```java
package com.ccbsa.wms.stock.domain.core.event;

import com.ccbsa.common.domain.TenantId;
import com.ccbsa.common.messaging.DomainEvent;
import com.ccbsa.wms.stock.domain.core.entity.StockAllocation;
import com.ccbsa.wms.stock.domain.core.valueobject.*;
import java.time.LocalDateTime;

/**
 * StockAllocationReleasedEvent
 * 
 * Published when stock allocation is released.
 */
public class StockAllocationReleasedEvent extends DomainEvent<StockAllocation> {
    
    private final StockAllocationId allocationId;
    private final TenantId tenantId;
    private final ProductId productId;
    private final LocationId locationId;
    private final StockItemId stockItemId;
    private final Quantity quantity;
    
    public StockAllocationReleasedEvent(
        StockAllocationId allocationId,
        TenantId tenantId,
        ProductId productId,
        LocationId locationId,
        StockItemId stockItemId,
        Quantity quantity
    ) {
        super(
            allocationId.getValueAsString(),
            "StockAllocation",
            tenantId,
            LocalDateTime.now(),
            1
        );
        this.allocationId = allocationId;
        this.tenantId = tenantId;
        this.productId = productId;
        this.locationId = locationId;
        this.stockItemId = stockItemId;
        this.quantity = quantity;
    }
    
    // Getters
    public StockAllocationId getAllocationId() { return allocationId; }
    public TenantId getTenantId() { return tenantId; }
    public ProductId getProductId() { return productId; }
    public LocationId getLocationId() { return locationId; }
    public StockItemId getStockItemId() { return stockItemId; }
    public Quantity getQuantity() { return quantity; }
}
```

---

## Backend Implementation

### Phase 1: Application Service - Command Handlers

**File:** `stock-management-application-service/src/main/java/com/ccbsa/wms/stock/application/service/command/AllocateStockCommandHandler.java`

```java
package com.ccbsa.wms.stock.application.service.command;

import com.ccbsa.common.domain.TenantId;
import com.ccbsa.wms.stock.application.service.command.dto.AllocateStockCommand;
import com.ccbsa.wms.stock.application.service.command.dto.AllocateStockCommandResult;
import com.ccbsa.wms.stock.application.service.port.repository.StockAllocationRepository;
import com.ccbsa.wms.stock.application.service.port.repository.StockItemRepository;
import com.ccbsa.wms.stock.application.service.port.messaging.StockManagementEventPublisher;
import com.ccbsa.wms.stock.domain.core.entity.StockAllocation;
import com.ccbsa.wms.stock.domain.core.entity.StockItem;
import com.ccbsa.wms.stock.domain.core.valueobject.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * AllocateStockCommandHandler
 * 
 * Handles stock allocation commands with FEFO support.
 */
@Component
public class AllocateStockCommandHandler {
    
    private final StockAllocationRepository allocationRepository;
    private final StockItemRepository stockItemRepository;
    private final StockManagementEventPublisher eventPublisher;
    
    public AllocateStockCommandHandler(
        StockAllocationRepository allocationRepository,
        StockItemRepository stockItemRepository,
        StockManagementEventPublisher eventPublisher
    ) {
        this.allocationRepository = allocationRepository;
        this.stockItemRepository = stockItemRepository;
        this.eventPublisher = eventPublisher;
    }
    
    @Transactional
    public AllocateStockCommandResult handle(AllocateStockCommand command) {
        // 1. Validate command
        validateCommand(command);
        
        // 2. Find stock items for allocation (FEFO if location not specified)
        List<StockItem> stockItems = findStockItemsForAllocation(
            command.getTenantId(),
            command.getProductId(),
            command.getLocationId(),
            command.getQuantity()
        );
        
        // 3. Validate sufficient available stock
        int totalAvailable = calculateTotalAvailable(stockItems);
        if (totalAvailable < command.getQuantity().getValue()) {
            throw new IllegalStateException(
                String.format("Insufficient available stock. Required: %d, Available: %d",
                    command.getQuantity().getValue(), totalAvailable)
            );
        }
        
        // 4. Allocate stock (FEFO: earliest expiration first)
        StockItem selectedStockItem = selectStockItemForAllocation(stockItems, command.getQuantity().getValue());
        
        // 5. Create allocation aggregate
        StockAllocation allocation = StockAllocation.builder()
            .id(StockAllocationId.generate())
            .tenantId(command.getTenantId())
            .productId(command.getProductId())
            .locationId(command.getLocationId())
            .stockItemId(selectedStockItem.getId())
            .quantity(command.getQuantity())
            .allocationType(command.getAllocationType())
            .referenceId(command.getReferenceId())
            .allocatedBy(command.getUserId())
            .notes(command.getNotes())
            .build();
        
        // 6. Allocate (publishes domain event)
        allocation.allocate();
        
        // 7. Persist allocation
        allocationRepository.save(allocation);
        
        // 8. Publish events
        eventPublisher.publish(allocation.getDomainEvents());
        
        // 9. Return result
        return AllocateStockCommandResult.builder()
            .allocationId(allocation.getId())
            .productId(allocation.getProductId())
            .locationId(allocation.getLocationId())
            .stockItemId(allocation.getStockItemId())
            .quantity(allocation.getQuantity())
            .allocationType(allocation.getAllocationType())
            .referenceId(allocation.getReferenceId())
            .status(allocation.getStatus())
            .allocatedAt(allocation.getAllocatedAt())
            .build();
    }
    
    private void validateCommand(AllocateStockCommand command) {
        if (command.getTenantId() == null) {
            throw new IllegalArgumentException("TenantId is required");
        }
        if (command.getProductId() == null) {
            throw new IllegalArgumentException("ProductId is required");
        }
        if (command.getQuantity() == null || command.getQuantity().getValue() <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (command.getAllocationType() == null) {
            throw new IllegalArgumentException("AllocationType is required");
        }
        if (command.getAllocationType() == AllocationType.PICKING_ORDER &&
            (command.getReferenceId() == null || command.getReferenceId().trim().isEmpty())) {
            throw new IllegalArgumentException("ReferenceId is required for PICKING_ORDER allocation");
        }
    }
    
    /**
     * Finds stock items for allocation using FEFO principles.
     * 
     * FEFO Algorithm:
     * 1. Query stock items by product (and location if specified)
     * 2. Filter by available quantity (total - allocated)
     * 3. Sort by expiration date (earliest first)
     * 4. Return items with sufficient available stock
     */
    private List<StockItem> findStockItemsForAllocation(
        TenantId tenantId,
        ProductId productId,
        LocationId locationId,
        Quantity requiredQuantity
    ) {
        List<StockItem> stockItems;
        
        if (locationId != null) {
            // Specific location requested
            stockItems = stockItemRepository.findByTenantIdAndProductIdAndLocationId(
                tenantId, productId, locationId
            );
        } else {
            // FEFO: Find all locations with stock, sort by expiration
            stockItems = stockItemRepository.findByTenantIdAndProductId(tenantId, productId);
        }
        
        // Filter by available quantity and sort by expiration (FEFO)
        return stockItems.stream()
            .filter(item -> {
                // Calculate available quantity (total - allocated)
                int allocated = calculateAllocatedQuantity(item.getId());
                int available = item.getQuantity().getValue() - allocated;
                return available > 0;
            })
            .sorted((a, b) -> {
                // FEFO: Sort by expiration date (earliest first)
                if (a.getExpirationDate() == null && b.getExpirationDate() == null) {
                    return 0;
                }
                if (a.getExpirationDate() == null) return 1;
                if (b.getExpirationDate() == null) return -1;
                return a.getExpirationDate().compareTo(b.getExpirationDate());
            })
            .collect(java.util.stream.Collectors.toList());
    }
    
    private int calculateTotalAvailable(List<StockItem> stockItems) {
        return stockItems.stream()
            .mapToInt(item -> {
                int allocated = calculateAllocatedQuantity(item.getId());
                return Math.max(0, item.getQuantity().getValue() - allocated);
            })
            .sum();
    }
    
    private int calculateAllocatedQuantity(StockItemId stockItemId) {
        List<StockAllocation> allocations = allocationRepository.findByStockItemIdAndStatus(
            stockItemId, AllocationStatus.ALLOCATED
        );
        return allocations.stream()
            .mapToInt(a -> a.getQuantity().getValue())
            .sum();
    }
    
    /**
     * Selects stock item for allocation using FEFO.
     * Prefers earliest expiring stock that has sufficient available quantity.
     */
    private StockItem selectStockItemForAllocation(List<StockItem> stockItems, int requiredQuantity) {
        for (StockItem item : stockItems) {
            int allocated = calculateAllocatedQuantity(item.getId());
            int available = item.getQuantity().getValue() - allocated;
            
            if (available >= requiredQuantity) {
                return item;
            }
        }
        
        throw new IllegalStateException("No stock item found with sufficient available quantity");
    }
}
```

**File:** `stock-management-application-service/src/main/java/com/ccbsa/wms/stock/application/service/command/ReleaseStockAllocationCommandHandler.java`

```java
package com.ccbsa.wms.stock.application.service.command;

import com.ccbsa.wms.stock.application.service.command.dto.ReleaseStockAllocationCommand;
import com.ccbsa.wms.stock.application.service.command.dto.ReleaseStockAllocationCommandResult;
import com.ccbsa.wms.stock.application.service.port.repository.StockAllocationRepository;
import com.ccbsa.wms.stock.application.service.port.messaging.StockManagementEventPublisher;
import com.ccbsa.wms.stock.domain.core.entity.StockAllocation;
import com.ccbsa.wms.stock.domain.core.valueobject.StockAllocationId;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * ReleaseStockAllocationCommandHandler
 * 
 * Handles stock allocation release commands.
 */
@Component
public class ReleaseStockAllocationCommandHandler {
    
    private final StockAllocationRepository allocationRepository;
    private final StockManagementEventPublisher eventPublisher;
    
    public ReleaseStockAllocationCommandHandler(
        StockAllocationRepository allocationRepository,
        StockManagementEventPublisher eventPublisher
    ) {
        this.allocationRepository = allocationRepository;
        this.eventPublisher = eventPublisher;
    }
    
    @Transactional
    public ReleaseStockAllocationCommandResult handle(ReleaseStockAllocationCommand command) {
        // 1. Find allocation
        StockAllocation allocation = allocationRepository.findById(command.getAllocationId())
            .orElseThrow(() -> new IllegalArgumentException(
                "Allocation not found: " + command.getAllocationId().getValueAsString()
            ));
        
        // 2. Release allocation (publishes domain event)
        allocation.release();
        
        // 3. Persist
        allocationRepository.save(allocation);
        
        // 4. Publish events
        eventPublisher.publish(allocation.getDomainEvents());
        
        // 5. Return result
        return ReleaseStockAllocationCommandResult.builder()
            .allocationId(allocation.getId())
            .status(allocation.getStatus())
            .releasedAt(allocation.getReleasedAt())
            .build();
    }
}
```

### Phase 2: Application Service - Query Handlers

**File:** `stock-management-application-service/src/main/java/com/ccbsa/wms/stock/application/service/query/GetStockAllocationsQueryHandler.java`

```java
package com.ccbsa.wms.stock.application.service.query;

import com.ccbsa.common.domain.TenantId;
import com.ccbsa.wms.stock.application.service.port.data.StockAllocationViewRepository;
import com.ccbsa.wms.stock.application.service.query.dto.GetStockAllocationsQuery;
import com.ccbsa.wms.stock.application.service.query.dto.GetStockAllocationsQueryResult;
import com.ccbsa.wms.stock.domain.core.valueobject.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * GetStockAllocationsQueryHandler
 * 
 * Handles queries for stock allocations.
 * Uses read model (data port) for optimized queries.
 */
@Component
public class GetStockAllocationsQueryHandler {
    
    private final StockAllocationViewRepository viewRepository;
    
    public GetStockAllocationsQueryHandler(StockAllocationViewRepository viewRepository) {
        this.viewRepository = viewRepository;
    }
    
    @Transactional(readOnly = true)
    public GetStockAllocationsQueryResult handle(GetStockAllocationsQuery query) {
        // Query from read model
        List<StockAllocationView> views;
        
        if (query.getReferenceId() != null) {
            // Query by reference ID (e.g., order ID)
            views = viewRepository.findByTenantIdAndReferenceId(
                query.getTenantId(), query.getReferenceId()
            );
        } else if (query.getProductId() != null && query.getLocationId() != null) {
            views = viewRepository.findByTenantIdAndProductIdAndLocationId(
                query.getTenantId(), query.getProductId(), query.getLocationId()
            );
        } else if (query.getProductId() != null) {
            views = viewRepository.findByTenantIdAndProductId(
                query.getTenantId(), query.getProductId()
            );
        } else if (query.getLocationId() != null) {
            views = viewRepository.findByTenantIdAndLocationId(
                query.getTenantId(), query.getLocationId()
            );
        } else {
            views = viewRepository.findByTenantId(query.getTenantId());
        }
        
        // Filter by status if provided
        if (query.getStatus() != null) {
            views = views.stream()
                .filter(v -> v.getStatus() == query.getStatus())
                .collect(Collectors.toList());
        }
        
        // Map to query results
        List<StockAllocationQueryResult> results = views.stream()
            .map(this::mapToQueryResult)
            .collect(Collectors.toList());
        
        return GetStockAllocationsQueryResult.builder()
            .allocations(results)
            .totalCount(results.size())
            .build();
    }
    
    private StockAllocationQueryResult mapToQueryResult(StockAllocationView view) {
        return StockAllocationQueryResult.builder()
            .allocationId(view.getAllocationId())
            .productId(view.getProductId())
            .locationId(view.getLocationId())
            .stockItemId(view.getStockItemId())
            .quantity(view.getQuantity())
            .allocationType(view.getAllocationType())
            .referenceId(view.getReferenceId())
            .status(view.getStatus())
            .allocatedAt(view.getAllocatedAt())
            .releasedAt(view.getReleasedAt())
            .allocatedBy(view.getAllocatedBy())
            .notes(view.getNotes())
            .build();
    }
}
```

### Phase 3: Repository Ports

**File:** `stock-management-application-service/src/main/java/com/ccbsa/wms/stock/application/service/port/repository/StockAllocationRepository.java`

```java
package com.ccbsa.wms.stock.application.service.port.repository;

import com.ccbsa.common.domain.TenantId;
import com.ccbsa.wms.stock.domain.core.entity.StockAllocation;
import com.ccbsa.wms.stock.domain.core.valueobject.*;

import java.util.List;
import java.util.Optional;

/**
 * StockAllocationRepository Port
 * 
 * Repository port for StockAllocation aggregate persistence (write model).
 */
public interface StockAllocationRepository {
    
    void save(StockAllocation allocation);
    
    Optional<StockAllocation> findById(StockAllocationId id);
    
    List<StockAllocation> findByTenantId(TenantId tenantId);
    
    List<StockAllocation> findByTenantIdAndProductId(TenantId tenantId, ProductId productId);
    
    List<StockAllocation> findByTenantIdAndProductIdAndLocationId(
        TenantId tenantId, ProductId productId, LocationId locationId
    );
    
    List<StockAllocation> findByStockItemId(StockItemId stockItemId);
    
    List<StockAllocation> findByStockItemIdAndStatus(StockItemId stockItemId, AllocationStatus status);
    
    List<StockAllocation> findByTenantIdAndReferenceId(TenantId tenantId, String referenceId);
}
```

### Phase 4: Data Ports (Read Model)

**File:** `stock-management-application-service/src/main/java/com/ccbsa/wms/stock/application/service/port/data/StockAllocationViewRepository.java`

```java
package com.ccbsa.wms.stock.application.service.port.data;

import com.ccbsa.common.domain.TenantId;
import com.ccbsa.wms.stock.domain.core.valueobject.*;

import java.util.List;

/**
 * StockAllocationViewRepository Port
 * 
 * Data port for StockAllocation read model queries (projections).
 * Used by query handlers for optimized reads.
 */
public interface StockAllocationViewRepository {
    
    List<StockAllocationView> findByTenantId(TenantId tenantId);
    
    List<StockAllocationView> findByTenantIdAndProductId(TenantId tenantId, ProductId productId);
    
    List<StockAllocationView> findByTenantIdAndProductIdAndLocationId(
        TenantId tenantId, ProductId productId, LocationId locationId
    );
    
    List<StockAllocationView> findByTenantIdAndLocationId(TenantId tenantId, LocationId locationId);
    
    List<StockAllocationView> findByTenantIdAndReferenceId(TenantId tenantId, String referenceId);
}
```

---

## Frontend Implementation

### Stock Allocation Service

**File:** `frontend-app/src/features/stock-management/services/stockAllocationService.ts`

```typescript
import { apiClient } from '../../../services/apiClient';
import { ApiResponse } from '../../../types/api';

export interface CreateStockAllocationRequest {
  productId: string;
  locationId?: string;
  quantity: number;
  allocationType: 'PICKING_ORDER' | 'RESERVATION' | 'OTHER';
  referenceId?: string;
  notes?: string;
}

export interface StockAllocationResponse {
  allocationId: string;
  productId: string;
  locationId?: string;
  stockItemId: string;
  quantity: number;
  allocationType: string;
  referenceId?: string;
  status: string;
  allocatedAt: string;
  allocatedBy: string;
  notes?: string;
}

export interface StockAllocationFilters {
  productId?: string;
  locationId?: string;
  referenceId?: string;
  status?: string;
}

const BASE_PATH = '/api/v1/stock-management/allocations';

export const stockAllocationService = {
  /**
   * Allocates stock for picking order or reservation.
   */
  async allocateStock(
    request: CreateStockAllocationRequest,
    tenantId: string
  ): Promise<ApiResponse<StockAllocationResponse>> {
    const response = await apiClient.post<ApiResponse<StockAllocationResponse>>(
      BASE_PATH,
      request,
      {
        headers: {
          'X-Tenant-Id': tenantId,
        },
      }
    );
    return response.data;
  },

  /**
   * Releases stock allocation.
   */
  async releaseAllocation(
    allocationId: string,
    tenantId: string
  ): Promise<ApiResponse<StockAllocationResponse>> {
    const response = await apiClient.put<ApiResponse<StockAllocationResponse>>(
      `${BASE_PATH}/${allocationId}/release`,
      {},
      {
        headers: {
          'X-Tenant-Id': tenantId,
        },
      }
    );
    return response.data;
  },

  /**
   * Gets stock allocations with filters.
   */
  async getStockAllocations(
    filters: StockAllocationFilters,
    tenantId: string
  ): Promise<ApiResponse<StockAllocationResponse[]>> {
    const params = new URLSearchParams();
    if (filters.productId) params.append('productId', filters.productId);
    if (filters.locationId) params.append('locationId', filters.locationId);
    if (filters.referenceId) params.append('referenceId', filters.referenceId);
    if (filters.status) params.append('status', filters.status);

    const response = await apiClient.get<ApiResponse<StockAllocationResponse[]>>(
      `${BASE_PATH}?${params.toString()}`,
      {
        headers: {
          'X-Tenant-Id': tenantId,
        },
      }
    );
    return response.data;
  },

  /**
   * Gets stock allocation by ID.
   */
  async getStockAllocation(
    allocationId: string,
    tenantId: string
  ): Promise<ApiResponse<StockAllocationResponse>> {
    const response = await apiClient.get<ApiResponse<StockAllocationResponse>>(
      `${BASE_PATH}/${allocationId}`,
      {
        headers: {
          'X-Tenant-Id': tenantId,
        },
      }
    );
    return response.data;
  },
};
```

---

## Data Flow

### Allocate Stock Flow

```
Frontend: User submits allocation form
  ↓ POST /api/v1/stock-management/allocations
Gateway Service
  ↓ Route to Stock Management Service
Stock Management Service (Command Controller)
  ↓ AllocateStockCommand
AllocateStockCommandHandler
  ↓ Query stock items by product (FEFO sorted)
  ↓ Calculate available quantity (total - allocated)
  ↓ Validate sufficient available stock
  ↓ Select stock item (FEFO: earliest expiration)
  ↓ StockAllocation.builder().build()
  ↓ StockAllocation.allocate()
  Domain Core (StockAllocation Aggregate)
  ↓ StockAllocatedEvent
Repository.save()
  ↓ Persist to database
Event Publisher (after commit)
  ↓ Kafka Topic: stock-management-events
Stock Management Service (Event Listener)
  ↓ Update stock level snapshots (read model)
Picking Service (Event Listener - future)
  ↓ Create picking task
Query Handler
  ↓ StockAllocationQueryResult
Query Controller
  ↓ Response
Gateway Service
  ↓ Response
Frontend: Display allocation success
```

### Release Allocation Flow

```
Frontend: User clicks "Release Allocation"
  ↓ PUT /api/v1/stock-management/allocations/{id}/release
Gateway Service
  ↓ Route to Stock Management Service
Stock Management Service (Command Controller)
  ↓ ReleaseStockAllocationCommand
ReleaseStockAllocationCommandHandler
  ↓ Find allocation
  ↓ StockAllocation.release()
  Domain Core (StockAllocation Aggregate)
  ↓ StockAllocationReleasedEvent
Repository.save()
  ↓ Persist to database
Event Publisher (after commit)
  ↓ Kafka Topic: stock-management-events
Stock Management Service (Event Listener)
  ↓ Update stock level snapshots (read model)
Query Handler
  ↓ StockAllocationQueryResult
Query Controller
  ↓ Response
Gateway Service
  ↓ Response
Frontend: Display release success
```

---

## Testing Strategy

### Unit Tests

- **Domain Core:** Allocation business logic, FEFO algorithm
- **Application Service:** Command/query handler logic
- **Validation:** Allocation rules, quantity validation

### Integration Tests

- **Service Integration:** End-to-end allocation flow
- **Database Integration:** Repository operations
- **Kafka Integration:** Event publishing and consumption
- **FEFO Algorithm:** Test allocation prioritizes earliest expiration

### Gateway API Tests

- Allocation creation through gateway
- FEFO allocation tests
- Release allocation tests
- Error scenario tests

---

## Acceptance Criteria Validation

- ✅ System allows allocating stock by product and location
- ✅ System validates sufficient available stock before allocation
- ✅ System supports allocation types: PICKING_ORDER, RESERVATION, OTHER
- ✅ System tracks allocated quantity separately from available quantity
- ✅ System prevents allocation exceeding available stock
- ✅ System supports FEFO-based allocation (allocates earliest expiring stock first)
- ✅ System publishes `StockAllocatedEvent` after successful allocation
- ✅ System allows querying allocations by reference ID (e.g., order ID)

---

**Document Control**

- **Version:** 1.0
- **Date:** 2025-01
- **Status:** Draft
- **Next Review:** Sprint planning meeting

