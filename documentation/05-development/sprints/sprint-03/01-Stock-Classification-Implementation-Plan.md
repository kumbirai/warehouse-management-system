# Stock Classification Implementation Plan

## US-2.1.1: Classify Stock by Expiration Date

**Service:** Stock Management Service  
**Priority:** Must Have  
**Story Points:** 5  
**Sprint:** Sprint 3

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
**I want** stock to be automatically classified by expiration dates  
**So that** I can identify stock that needs priority handling

### Business Requirements

- System automatically assigns classification when stock item is created
- Classification categories: EXPIRED, CRITICAL (≤7 days), NEAR_EXPIRY (≤30 days), NORMAL, EXTENDED_SHELF_LIFE
- Classification is visible in all stock views and reports
- Classification updates automatically when expiration date changes
- System handles null expiration dates (non-perishable) as NORMAL

### Technical Requirements

- Follow DDD, Clean Hexagonal Architecture, CQRS, Event-Driven Choreography
- Pure Java domain core (no framework dependencies)
- Classification logic in domain aggregate
- Domain events for classification changes
- Multi-tenant support
- Move common value objects to `common-domain` (DRY principle)

---

## UI Design

### Stock Classification Display

**Component:** `StockClassificationBadge.tsx`

**Features:**

- **Visual Indicators** - Color-coded badges for each classification
- **Expiration Date Display** - Show expiration date with days remaining
- **Classification Filter** - Filter stock by classification
- **Alert Indicators** - Visual alerts for CRITICAL and EXPIRED stock

**UI Flow:**

1. User views stock items list
2. System displays classification badge for each item:
    - **EXPIRED** - Red badge with warning icon
    - **CRITICAL** - Orange badge with alert icon (≤7 days)
    - **NEAR_EXPIRY** - Yellow badge with warning icon (≤30 days)
    - **NORMAL** - Green badge (default)
    - **EXTENDED_SHELF_LIFE** - Blue badge
3. User can filter by classification
4. User can sort by expiration date
5. User can view classification details in stock item detail view

**Classification Badge Colors:**

- **EXPIRED:** Red (#d32f2f)
- **CRITICAL:** Orange (#f57c00)
- **NEAR_EXPIRY:** Yellow (#fbc02d)
- **NORMAL:** Green (#388e3c)
- **EXTENDED_SHELF_LIFE:** Blue (#1976d2)

**Stock Item List View:**

```typescript
<TableRow>
  <TableCell>{stockItem.productCode}</TableCell>
  <TableCell>{stockItem.quantity}</TableCell>
  <TableCell>
    <StockClassificationBadge classification={stockItem.classification} />
  </TableCell>
  <TableCell>
    {stockItem.expirationDate ? (
      <ExpirationDateDisplay date={stockItem.expirationDate} />
    ) : (
      <Typography variant="body2" color="textSecondary">
        Non-perishable
      </Typography>
    )}
  </TableCell>
</TableRow>
```

---

## Domain Model Design

### StockItem Aggregate Root

**Package:** `com.ccbsa.wms.stock.domain.core.entity`

**Note:** StockItem aggregate may need to be created if it doesn't exist. This plan assumes it needs to be created.

```java
package com.ccbsa.wms.stock.domain.core.entity;

import com.ccbsa.common.domain.TenantAwareAggregateRoot;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.common.domain.valueobject.ProductId;
import com.ccbsa.wms.stock.domain.core.valueobject.StockItemId;
import com.ccbsa.wms.stock.domain.core.valueobject.Quantity;
import com.ccbsa.wms.stock.domain.core.valueobject.ExpirationDate;
import com.ccbsa.wms.stock.domain.core.valueobject.StockClassification;
import com.ccbsa.wms.stock.domain.core.event.StockClassifiedEvent;
import com.ccbsa.wms.stock.domain.core.event.StockExpiringAlertEvent;
import com.ccbsa.wms.stock.domain.core.event.StockExpiredEvent;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Aggregate Root: StockItem
 * <p>
 * Represents individual stock item with expiration tracking and classification.
 * <p>
 * Business Rules:
 * - Stock items must have valid expiration dates (or null for non-perishable)
 * - Stock classification automatically assigned based on expiration date
 * - Expired stock cannot be picked
 * - Stock within 7 days of expiration generates alert
 * - Stock within 30 days of expiration classified as "Near Expiry"
 */
public class StockItem extends TenantAwareAggregateRoot<StockItemId> {
    
    private ProductId productId;
    private LocationId locationId; // May be null initially
    private Quantity quantity;
    private ExpirationDate expirationDate; // May be null for non-perishable
    private StockClassification classification;
    private LocalDateTime createdAt;
    private LocalDateTime lastModifiedAt;
    
    private StockItem() {
        // Private constructor for builder
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Business logic method: Classifies stock based on expiration date.
     * <p>
     * Classification Rules:
     * - EXPIRED: Expiration date is in the past
     * - CRITICAL: Expiration date is within 7 days
     * - NEAR_EXPIRY: Expiration date is within 30 days (but > 7 days)
     * - NORMAL: Expiration date is more than 30 days away (or null)
     * - EXTENDED_SHELF_LIFE: Expiration date is more than 1 year away
     */
    public void classify() {
        if (expirationDate == null) {
            // Non-perishable items are always NORMAL
            if (this.classification != StockClassification.NORMAL) {
                this.classification = StockClassification.NORMAL;
                this.lastModifiedAt = LocalDateTime.now();
            }
            return;
        }
        
        LocalDate today = LocalDate.now();
        LocalDate expiryDate = expirationDate.getValue();
        long daysUntilExpiry = java.time.temporal.ChronoUnit.DAYS.between(today, expiryDate);
        
        StockClassification newClassification;
        
        if (daysUntilExpiry < 0) {
            newClassification = StockClassification.EXPIRED;
            if (this.classification != StockClassification.EXPIRED) {
                addDomainEvent(new StockExpiredEvent(
                    this.getId(),
                    this.productId,
                    expiryDate
                ));
            }
        } else if (daysUntilExpiry <= 7) {
            newClassification = StockClassification.CRITICAL;
            if (this.classification != StockClassification.CRITICAL) {
                addDomainEvent(new StockExpiringAlertEvent(
                    this.getId(),
                    this.productId,
                    expiryDate,
                    7
                ));
            }
        } else if (daysUntilExpiry <= 30) {
            newClassification = StockClassification.NEAR_EXPIRY;
            if (this.classification != StockClassification.NEAR_EXPIRY) {
                addDomainEvent(new StockExpiringAlertEvent(
                    this.getId(),
                    this.productId,
                    expiryDate,
                    30
                ));
            }
        } else if (daysUntilExpiry > 365) {
            newClassification = StockClassification.EXTENDED_SHELF_LIFE;
        } else {
            newClassification = StockClassification.NORMAL;
        }
        
        if (this.classification != newClassification) {
            StockClassification oldClassification = this.classification;
            this.classification = newClassification;
            this.lastModifiedAt = LocalDateTime.now();
            
            // Publish classification change event
            addDomainEvent(new StockClassifiedEvent(
                this.getId(),
                this.productId,
                oldClassification,
                newClassification,
                expiryDate
            ));
        }
    }
    
    /**
     * Business logic method: Updates expiration date and reclassifies.
     */
    public void updateExpirationDate(ExpirationDate newExpirationDate) {
        this.expirationDate = newExpirationDate;
        this.lastModifiedAt = LocalDateTime.now();
        classify(); // Reclassify after expiration date change
    }
    
    /**
     * Business logic method: Checks if stock can be picked.
     */
    public boolean canBePicked() {
        return classification != StockClassification.EXPIRED 
            && quantity.getValue() > 0;
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
    
    // Builder pattern
    public static class Builder {
        private StockItem stockItem = new StockItem();
        
        public Builder stockItemId(StockItemId id) {
            stockItem.id = id;
            return this;
        }
        
        public Builder tenantId(TenantId tenantId) {
            stockItem.tenantId = tenantId;
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
            return this;
        }
        
        public StockItem build() {
            validate();
            stockItem.createdAt = LocalDateTime.now();
            stockItem.lastModifiedAt = LocalDateTime.now();
            stockItem.classify(); // Classify on creation
            return stockItem;
        }
        
        private void validate() {
            if (stockItem.id == null) {
                throw new IllegalArgumentException("StockItemId is required");
            }
            if (stockItem.tenantId == null) {
                throw new IllegalArgumentException("TenantId is required");
            }
            if (stockItem.productId == null) {
                throw new IllegalArgumentException("ProductId is required");
            }
            if (stockItem.quantity == null) {
                throw new IllegalArgumentException("Quantity is required");
            }
        }
    }
}
```

### Value Objects

#### ExpirationDate (Move to common-domain)

**Package:** `com.ccbsa.common.domain.valueobject`

```java
package com.ccbsa.common.domain.valueobject;

import java.time.LocalDate;

/**
 * Value Object: ExpirationDate
 * <p>
 * Represents product expiration date with validation and utility methods.
 * <p>
 * This value object is shared across services (DRY principle).
 */
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
        if (o == null || getClass() != o.getClass()) return false;
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

#### StockClassification (Move to common-domain)

**Package:** `com.ccbsa.common.domain.valueobject`

```java
package com.ccbsa.common.domain.valueobject;

/**
 * Value Object: StockClassification
 * <p>
 * Enum representing stock classification based on expiration dates.
 * <p>
 * This enum is shared across services (DRY principle).
 */
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

### Domain Events

#### StockClassifiedEvent

```java
package com.ccbsa.wms.stock.domain.core.event;

import com.ccbsa.common.domain.DomainEvent;
import com.ccbsa.common.domain.valueobject.StockClassification;
import com.ccbsa.common.domain.valueobject.ProductId;
import com.ccbsa.wms.stock.domain.core.valueobject.StockItemId;

import java.time.LocalDate;

/**
 * Domain Event: StockClassifiedEvent
 * <p>
 * Published when stock item classification changes.
 */
public class StockClassifiedEvent extends DomainEvent<StockItem> {
    private final StockItemId stockItemId;
    private final ProductId productId;
    private final StockClassification oldClassification;
    private final StockClassification newClassification;
    private final LocalDate expirationDate;
    
    // Constructor and getters
}
```

---

## Backend Implementation

### Phase 1: Move Common Value Objects to common-domain

**Module:** `common/common-domain`

**Files to Create/Update:**

1. `ExpirationDate.java` - Move from stock-management-domain-core
2. `StockClassification.java` - Move from stock-management-domain-core
3. Update `common-domain/pom.xml` if needed

### Phase 2: StockItem Domain Core

**Module:** `stock-management-domain/stock-management-domain-core`

**Files to Create:**

1. `StockItem.java` - Aggregate root
2. `StockItemId.java` - Value object
3. `StockClassifiedEvent.java` - Domain event
4. `StockExpiringAlertEvent.java` - Domain event
5. `StockExpiredEvent.java` - Domain event

### Phase 3: Application Service

**Module:** `stock-management-domain/stock-management-application-service`

**Command Handler:**

```java
@Component
public class CreateStockItemCommandHandler {
    
    private final StockItemRepository repository;
    private final StockManagementEventPublisher eventPublisher;
    
    @Transactional
    public CreateStockItemResult handle(CreateStockItemCommand command) {
        // 1. Validate command
        validateCommand(command);
        
        // 2. Create aggregate using builder
        StockItem stockItem = StockItem.builder()
            .stockItemId(StockItemId.generate())
            .tenantId(command.getTenantId())
            .productId(command.getProductId())
            .locationId(command.getLocationId())
            .quantity(command.getQuantity())
            .expirationDate(command.getExpirationDate())
            .build();
        
        // 3. Get domain events BEFORE saving
        List<DomainEvent<?>> domainEvents = new ArrayList<>(stockItem.getDomainEvents());
        
        // 4. Persist aggregate
        StockItem savedStockItem = repository.save(stockItem);
        
        // 5. Publish events after transaction commit
        if (!domainEvents.isEmpty()) {
            publishEventsAfterCommit(domainEvents);
        }
        
        // 6. Return result
        return CreateStockItemResult.builder()
            .stockItemId(savedStockItem.getId())
            .classification(savedStockItem.getClassification())
            .build();
    }
}
```

### Phase 4: Data Access

**Module:** `stock-management-dataaccess`

**Files to Create:**

1. `StockItemEntity.java` - JPA entity
2. `StockItemJpaRepository.java` - JPA repository
3. `StockItemRepositoryAdapter.java` - Repository adapter
4. `StockItemEntityMapper.java` - Entity mapper
5. Database migration: `V4__Create_stock_items_table.sql`

### Phase 5: REST API

**Module:** `stock-management-application`

**Files to Create/Update:**

1. `StockItemCommandController.java` - Command endpoints
2. `StockItemQueryController.java` - Query endpoints
3. DTOs for stock items
4. DTO mappers

---

## Frontend Implementation

### Stock Classification Badge Component

**File:** `frontend-app/src/components/stock/StockClassificationBadge.tsx`

```typescript
import React from 'react';
import { Chip } from '@mui/material';
import { StockClassification } from '../../features/stock-management/types/stockManagement';

interface StockClassificationBadgeProps {
  classification: StockClassification;
}

export const StockClassificationBadge: React.FC<StockClassificationBadgeProps> = ({
  classification,
}) => {
  const getColor = (): 'default' | 'primary' | 'secondary' | 'error' | 'info' | 'success' | 'warning' => {
    switch (classification) {
      case 'EXPIRED':
        return 'error';
      case 'CRITICAL':
        return 'warning';
      case 'NEAR_EXPIRY':
        return 'warning';
      case 'NORMAL':
        return 'success';
      case 'EXTENDED_SHELF_LIFE':
        return 'info';
      default:
        return 'default';
    }
  };

  const getLabel = (): string => {
    switch (classification) {
      case 'EXPIRED':
        return 'Expired';
      case 'CRITICAL':
        return 'Critical';
      case 'NEAR_EXPIRY':
        return 'Near Expiry';
      case 'NORMAL':
        return 'Normal';
      case 'EXTENDED_SHELF_LIFE':
        return 'Extended Shelf Life';
      default:
        return classification;
    }
  };

  return (
    <Chip
      label={getLabel()}
      color={getColor()}
      size="small"
      variant="outlined"
    />
  );
};
```

---

## Data Flow

### Stock Item Creation with Classification

```
CreateStockItemCommand
  ↓
CreateStockItemCommandHandler
  ↓
StockItem.builder().build()
  ↓
StockItem.classify() (automatic in build())
  ↓
StockClassifiedEvent (if classification assigned)
  ↓
Repository.save()
  ↓
Event Publisher (after transaction commit)
  ↓
Kafka Topic: stock-management-events
  ↓
Event Listeners (Location Management Service, etc.)
```

---

## Testing Strategy

### Unit Tests

- **StockItem.classify()** - Test all classification scenarios
- **ExpirationDate** - Test validation and utility methods
- **StockClassification** - Test enum values

### Integration Tests

- **CreateStockItemCommandHandler** - End-to-end creation with classification
- **Event Publishing** - Verify events are published correctly

### Gateway API Tests

- **Create Stock Item** - Test classification is returned
- **Update Expiration Date** - Test reclassification

---

## Acceptance Criteria Validation

- ✅ **AC1:** Classification assigned automatically in `StockItem.build()`
- ✅ **AC2:** All classification categories implemented in enum
- ✅ **AC3:** Classification visible in query results and DTOs
- ✅ **AC4:** `updateExpirationDate()` triggers reclassification
- ✅ **AC5:** Null expiration dates handled as NORMAL in `classify()` method

---

**Document Control**

- **Version:** 1.0
- **Date:** 2025-01
- **Status:** Draft

