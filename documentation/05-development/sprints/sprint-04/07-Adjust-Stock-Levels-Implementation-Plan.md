# Adjust Stock Levels Implementation Plan

## US-5.2.2: Adjust Stock Levels

**Service:** Stock Management Service  
**Priority:** Must Have  
**Story Points:** 5  
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

**As a** warehouse manager  
**I want** to manually adjust stock levels  
**So that** I can correct discrepancies from stock counts, damage, or other reasons

### Business Requirements

- System allows increasing stock levels (positive adjustment)
- System allows decreasing stock levels (negative adjustment)
- System requires adjustment reason (STOCK_COUNT, DAMAGE, CORRECTION, etc.)
- System prevents negative stock levels after adjustment
- System records adjustment timestamp, user, and reason
- System publishes `StockAdjustedEvent` after successful adjustment
- System maintains audit trail of all adjustments
- System supports adjustments at consignment level or stock item level

### Technical Requirements

- Follow DDD, Clean Hexagonal Architecture, CQRS, Event-Driven Choreography
- Pure Java domain core (no framework dependencies)
- Adjustment logic in domain aggregate
- Domain events for adjustment state changes
- Multi-tenant support
- Move common value objects to `common-domain` (DRY principle)
- Authorization checks for adjustment operations
- Audit trail maintenance

---

## UI Design

### Stock Adjustment Form

**Component:** `StockAdjustmentForm.tsx`

**Fields:**

- **Adjustment Type** (required, radio buttons: INCREASE, DECREASE)
- **Product** (required, autocomplete with barcode scanning)
- **Location** (optional, autocomplete with barcode scanning)
- **Stock Item** (optional, autocomplete - if not provided, adjusts at product level)
- **Quantity** (required, number input with validation)
- **Adjustment Reason** (required, dropdown: STOCK_COUNT, DAMAGE, CORRECTION, THEFT, EXPIRATION, OTHER)
- **Notes** (optional, textarea)
- **Authorization Code** (required for large adjustments, text input)

**Validation:**

- Product must exist
- Quantity must be positive
- For DECREASE: Quantity must not exceed current stock level
- Adjustment reason is required
- Authorization code required for adjustments exceeding threshold
- Real-time validation feedback showing current stock level and resulting level

**Actions:**

- **Adjust** - Submit form to adjust stock
- **Cancel** - Navigate back to adjustment list
- **Preview Impact** - Show impact on stock levels before submission

**UI Flow:**

1. User navigates to "Stock Management" → "Adjust Stock"
2. Form displays with all fields
3. User selects adjustment type (INCREASE or DECREASE)
4. User scans or enters product barcode
5. System displays current stock level for product/location
6. User optionally scans location barcode
7. User optionally selects specific stock item
8. User enters quantity
9. System validates quantity (prevents negative for DECREASE)
10. User selects adjustment reason
11. User enters notes (optional)
12. User enters authorization code (if required)
13. User clicks "Adjust"
14. System validates and adjusts stock
15. Success message displayed with adjustment details
16. User redirected to adjustment detail page

### Stock Adjustment List View

**Component:** `StockAdjustmentList.tsx`

**Features:**

- List all adjustments with pagination
- Filter by product, location, adjustment type, reason, date range, user
- Search by product code, adjustment ID
- Sort by adjustment date, quantity
- Display adjustment impact (before/after quantities)
- Export adjustments to CSV/PDF

**UI Flow:**

1. User navigates to "Stock Management" → "Stock Adjustments"
2. System displays list of adjustments
3. User can filter by:
    - Product
    - Location
    - Adjustment type (INCREASE/DECREASE)
    - Adjustment reason
    - User who made adjustment
    - Date range
4. User can click on adjustment to view details
5. User can export adjustments

### Stock Adjustment Detail View

**Component:** `StockAdjustmentDetail.tsx`

**Features:**

- Display adjustment details
- Show product information
- Show location information (if applicable)
- Show stock item information (if applicable)
- Show before/after quantities
- Show adjustment reason and notes
- Show authorization information
- Show audit trail (created at, created by)

**UI Flow:**

1. User clicks on adjustment from list
2. System displays adjustment details
3. User can view related stock item
4. User can view related product
5. User can view audit trail

---

## Domain Model Design

### StockAdjustment Aggregate Root

**File:** `stock-management-domain-core/src/main/java/com/ccbsa/wms/stock/domain/core/entity/StockAdjustment.java`

```java
package com.ccbsa.wms.stock.domain.core.entity;

import com.ccbsa.common.domain.AggregateRoot;
import com.ccbsa.common.domain.TenantId;
import com.ccbsa.wms.stock.domain.core.event.StockAdjustedEvent;
import com.ccbsa.wms.stock.domain.core.valueobject.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * StockAdjustment Aggregate Root
 * 
 * Represents a manual stock level adjustment.
 * Used for correcting discrepancies from stock counts, damage, etc.
 * 
 * Business Rules:
 * - Adjustment quantity must be positive
 * - DECREASE adjustments cannot result in negative stock
 * - Adjustment reason is required
 * - Authorization required for large adjustments
 */
public class StockAdjustment extends AggregateRoot<StockAdjustmentId> {
    
    private StockAdjustmentId id;
    private TenantId tenantId;
    private ProductId productId;
    private LocationId locationId;  // Optional - null for product-wide adjustment
    private StockItemId stockItemId; // Optional - null for product/location adjustment
    private AdjustmentType adjustmentType;
    private Quantity quantity;
    private AdjustmentReason reason;
    private String notes;
    private String adjustedBy;
    private String authorizationCode;  // For large adjustments
    private LocalDateTime adjustedAt;
    
    // Before/after quantities for audit
    private int quantityBefore;
    private int quantityAfter;
    
    // Private constructor - use builder
    private StockAdjustment() {
        super();
    }
    
    /**
     * Builder pattern for creating StockAdjustment
     */
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private StockAdjustment adjustment = new StockAdjustment();
        
        public Builder id(StockAdjustmentId id) {
            adjustment.id = id;
            return this;
        }
        
        public Builder tenantId(TenantId tenantId) {
            adjustment.tenantId = tenantId;
            return this;
        }
        
        public Builder productId(ProductId productId) {
            adjustment.productId = productId;
            return this;
        }
        
        public Builder locationId(LocationId locationId) {
            adjustment.locationId = locationId;
            return this;
        }
        
        public Builder stockItemId(StockItemId stockItemId) {
            adjustment.stockItemId = stockItemId;
            return this;
        }
        
        public Builder adjustmentType(AdjustmentType adjustmentType) {
            adjustment.adjustmentType = adjustmentType;
            return this;
        }
        
        public Builder quantity(Quantity quantity) {
            adjustment.quantity = quantity;
            return this;
        }
        
        public Builder reason(AdjustmentReason reason) {
            adjustment.reason = reason;
            return this;
        }
        
        public Builder notes(String notes) {
            adjustment.notes = notes;
            return this;
        }
        
        public Builder adjustedBy(String adjustedBy) {
            adjustment.adjustedBy = adjustedBy;
            return this;
        }
        
        public Builder authorizationCode(String authorizationCode) {
            adjustment.authorizationCode = authorizationCode;
            return this;
        }
        
        public Builder quantityBefore(int quantityBefore) {
            adjustment.quantityBefore = quantityBefore;
            return this;
        }
        
        public StockAdjustment build() {
            // Validate required fields
            if (adjustment.id == null) {
                adjustment.id = StockAdjustmentId.of(UUID.randomUUID());
            }
            if (adjustment.tenantId == null) {
                throw new IllegalArgumentException("TenantId is required");
            }
            if (adjustment.productId == null) {
                throw new IllegalArgumentException("ProductId is required");
            }
            if (adjustment.adjustmentType == null) {
                throw new IllegalArgumentException("AdjustmentType is required");
            }
            if (adjustment.quantity == null || adjustment.quantity.getValue() <= 0) {
                throw new IllegalArgumentException("Quantity must be positive");
            }
            if (adjustment.reason == null) {
                throw new IllegalArgumentException("AdjustmentReason is required");
            }
            if (adjustment.adjustedBy == null || adjustment.adjustedBy.trim().isEmpty()) {
                throw new IllegalArgumentException("AdjustedBy is required");
            }
            
            // Set defaults
            adjustment.adjustedAt = LocalDateTime.now();
            
            return adjustment;
        }
    }
    
    /**
     * Business logic method: Adjusts stock level.
     * 
     * Business Rules:
     * - Validates adjustment doesn't result in negative stock (validated at application service)
     * - Calculates before/after quantities
     * - Records adjustment timestamp and user
     * - Publishes StockAdjustedEvent
     * 
     * @param currentQuantity Current stock quantity before adjustment
     * @throws IllegalStateException if adjustment would result in negative stock
     */
    public void adjust(int currentQuantity) {
        // Calculate after quantity
        int afterQuantity;
        if (this.adjustmentType == AdjustmentType.INCREASE) {
            afterQuantity = currentQuantity + this.quantity.getValue();
        } else { // DECREASE
            afterQuantity = currentQuantity - this.quantity.getValue();
            
            // Validate doesn't result in negative
            if (afterQuantity < 0) {
                throw new IllegalStateException(
                    String.format("Adjustment would result in negative stock. Current: %d, Adjustment: %d, Result: %d",
                        currentQuantity, this.quantity.getValue(), afterQuantity)
                );
            }
        }
        
        // Set before/after quantities
        this.quantityBefore = currentQuantity;
        this.quantityAfter = afterQuantity;
        
        // Publish domain event
        addDomainEvent(new StockAdjustedEvent(
            this.id,
            this.tenantId,
            this.productId,
            this.locationId,
            this.stockItemId,
            this.adjustmentType,
            this.quantity,
            this.quantityBefore,
            this.quantityAfter,
            this.reason,
            this.notes
        ));
    }
    
    // Getters
    public StockAdjustmentId getId() { return id; }
    public TenantId getTenantId() { return tenantId; }
    public ProductId getProductId() { return productId; }
    public LocationId getLocationId() { return locationId; }
    public StockItemId getStockItemId() { return stockItemId; }
    public AdjustmentType getAdjustmentType() { return adjustmentType; }
    public Quantity getQuantity() { return quantity; }
    public AdjustmentReason getReason() { return reason; }
    public String getNotes() { return notes; }
    public String getAdjustedBy() { return adjustedBy; }
    public String getAuthorizationCode() { return authorizationCode; }
    public LocalDateTime getAdjustedAt() { return adjustedAt; }
    public int getQuantityBefore() { return quantityBefore; }
    public int getQuantityAfter() { return quantityAfter; }
}
```

### AdjustmentType Enum

**File:** `common-domain/src/main/java/com/ccbsa/common/domain/valueobject/AdjustmentType.java`

```java
package com.ccbsa.common.domain.valueobject;

/**
 * Adjustment Type Enum
 * 
 * Represents the type of stock adjustment.
 * Moved to common-domain for reuse across services (DRY principle).
 */
public enum AdjustmentType {
    /**
     * Increase stock level
     */
    INCREASE,
    
    /**
     * Decrease stock level
     */
    DECREASE
}
```

### AdjustmentReason Enum

**File:** `common-domain/src/main/java/com/ccbsa/common/domain/valueobject/AdjustmentReason.java`

```java
package com.ccbsa.common.domain.valueobject;

/**
 * Adjustment Reason Enum
 * 
 * Represents the reason for stock adjustment.
 * Moved to common-domain for reuse across services (DRY principle).
 */
public enum AdjustmentReason {
    /**
     * Adjustment from stock count
     */
    STOCK_COUNT,
    
    /**
     * Adjustment due to damage
     */
    DAMAGE,
    
    /**
     * Correction adjustment
     */
    CORRECTION,
    
    /**
     * Adjustment due to theft
     */
    THEFT,
    
    /**
     * Adjustment due to expiration
     */
    EXPIRATION,
    
    /**
     * Other reason
     */
    OTHER
}
```

### StockAdjustmentId Value Object

**File:** `stock-management-domain-core/src/main/java/com/ccbsa/wms/stock/domain/core/valueobject/StockAdjustmentId.java`

```java
package com.ccbsa.wms.stock.domain.core.valueobject;

import java.util.UUID;

/**
 * StockAdjustmentId Value Object
 * 
 * Represents a unique identifier for stock adjustment.
 */
public final class StockAdjustmentId {
    private final UUID value;
    
    private StockAdjustmentId(UUID value) {
        if (value == null) {
            throw new IllegalArgumentException("StockAdjustmentId value cannot be null");
        }
        this.value = value;
    }
    
    public static StockAdjustmentId of(UUID value) {
        return new StockAdjustmentId(value);
    }
    
    public static StockAdjustmentId of(String value) {
        return new StockAdjustmentId(UUID.fromString(value));
    }
    
    public static StockAdjustmentId generate() {
        return new StockAdjustmentId(UUID.randomUUID());
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
        StockAdjustmentId that = (StockAdjustmentId) o;
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

### Domain Event

**File:** `stock-management-domain-core/src/main/java/com/ccbsa/wms/stock/domain/core/event/StockAdjustedEvent.java`

```java
package com.ccbsa.wms.stock.domain.core.event;

import com.ccbsa.common.domain.TenantId;
import com.ccbsa.common.domain.valueobject.AdjustmentReason;
import com.ccbsa.common.domain.valueobject.AdjustmentType;
import com.ccbsa.common.messaging.DomainEvent;
import com.ccbsa.wms.stock.domain.core.entity.StockAdjustment;
import com.ccbsa.wms.stock.domain.core.valueobject.*;
import java.time.LocalDateTime;

/**
 * StockAdjustedEvent
 * 
 * Published when stock is successfully adjusted.
 */
public class StockAdjustedEvent extends DomainEvent<StockAdjustment> {
    
    private final StockAdjustmentId adjustmentId;
    private final TenantId tenantId;
    private final ProductId productId;
    private final LocationId locationId;
    private final StockItemId stockItemId;
    private final AdjustmentType adjustmentType;
    private final Quantity quantity;
    private final int quantityBefore;
    private final int quantityAfter;
    private final AdjustmentReason reason;
    private final String notes;
    
    public StockAdjustedEvent(
        StockAdjustmentId adjustmentId,
        TenantId tenantId,
        ProductId productId,
        LocationId locationId,
        StockItemId stockItemId,
        AdjustmentType adjustmentType,
        Quantity quantity,
        int quantityBefore,
        int quantityAfter,
        AdjustmentReason reason,
        String notes
    ) {
        super(
            adjustmentId.getValueAsString(),
            "StockAdjustment",
            tenantId,
            LocalDateTime.now(),
            1
        );
        this.adjustmentId = adjustmentId;
        this.tenantId = tenantId;
        this.productId = productId;
        this.locationId = locationId;
        this.stockItemId = stockItemId;
        this.adjustmentType = adjustmentType;
        this.quantity = quantity;
        this.quantityBefore = quantityBefore;
        this.quantityAfter = quantityAfter;
        this.reason = reason;
        this.notes = notes;
    }
    
    // Getters
    public StockAdjustmentId getAdjustmentId() { return adjustmentId; }
    public TenantId getTenantId() { return tenantId; }
    public ProductId getProductId() { return productId; }
    public LocationId getLocationId() { return locationId; }
    public StockItemId getStockItemId() { return stockItemId; }
    public AdjustmentType getAdjustmentType() { return adjustmentType; }
    public Quantity getQuantity() { return quantity; }
    public int getQuantityBefore() { return quantityBefore; }
    public int getQuantityAfter() { return quantityAfter; }
    public AdjustmentReason getReason() { return reason; }
    public String getNotes() { return notes; }
}
```

---

## Backend Implementation

### Phase 1: Application Service - Command Handlers

**File:** `stock-management-application-service/src/main/java/com/ccbsa/wms/stock/application/service/command/AdjustStockCommandHandler.java`

```java
package com.ccbsa.wms.stock.application.service.command;

import com.ccbsa.common.domain.TenantId;
import com.ccbsa.common.domain.valueobject.AdjustmentReason;
import com.ccbsa.common.domain.valueobject.AdjustmentType;
import com.ccbsa.wms.stock.application.service.command.dto.AdjustStockCommand;
import com.ccbsa.wms.stock.application.service.command.dto.AdjustStockCommandResult;
import com.ccbsa.wms.stock.application.service.port.repository.StockAdjustmentRepository;
import com.ccbsa.wms.stock.application.service.port.repository.StockItemRepository;
import com.ccbsa.wms.stock.application.service.port.messaging.StockManagementEventPublisher;
import com.ccbsa.wms.stock.domain.core.entity.StockAdjustment;
import com.ccbsa.wms.stock.domain.core.entity.StockItem;
import com.ccbsa.wms.stock.domain.core.valueobject.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * AdjustStockCommandHandler
 * 
 * Handles stock adjustment commands.
 */
@Component
public class AdjustStockCommandHandler {
    
    private final StockAdjustmentRepository adjustmentRepository;
    private final StockItemRepository stockItemRepository;
    private final StockManagementEventPublisher eventPublisher;
    
    // Authorization threshold (configurable)
    private static final int AUTHORIZATION_THRESHOLD = 100;
    
    public AdjustStockCommandHandler(
        StockAdjustmentRepository adjustmentRepository,
        StockItemRepository stockItemRepository,
        StockManagementEventPublisher eventPublisher
    ) {
        this.adjustmentRepository = adjustmentRepository;
        this.stockItemRepository = stockItemRepository;
        this.eventPublisher = eventPublisher;
    }
    
    @Transactional
    public AdjustStockCommandResult handle(AdjustStockCommand command) {
        // 1. Validate command
        validateCommand(command);
        
        // 2. Find stock item or calculate current quantity
        int currentQuantity = getCurrentQuantity(command);
        
        // 3. Validate adjustment doesn't result in negative stock
        if (command.getAdjustmentType() == AdjustmentType.DECREASE) {
            if (currentQuantity < command.getQuantity().getValue()) {
                throw new IllegalStateException(
                    String.format("Insufficient stock for adjustment. Current: %d, Adjustment: %d",
                        currentQuantity, command.getQuantity().getValue())
                );
            }
        }
        
        // 4. Create adjustment aggregate
        StockAdjustment adjustment = StockAdjustment.builder()
            .id(StockAdjustmentId.generate())
            .tenantId(command.getTenantId())
            .productId(command.getProductId())
            .locationId(command.getLocationId())
            .stockItemId(command.getStockItemId())
            .adjustmentType(command.getAdjustmentType())
            .quantity(command.getQuantity())
            .reason(command.getReason())
            .notes(command.getNotes())
            .adjustedBy(command.getUserId())
            .authorizationCode(command.getAuthorizationCode())
            .quantityBefore(currentQuantity)
            .build();
        
        // 5. Adjust (publishes domain event)
        adjustment.adjust(currentQuantity);
        
        // 6. Update stock item quantity
        updateStockItemQuantity(command, adjustment);
        
        // 7. Persist adjustment
        adjustmentRepository.save(adjustment);
        
        // 8. Publish events
        eventPublisher.publish(adjustment.getDomainEvents());
        
        // 9. Return result
        return AdjustStockCommandResult.builder()
            .adjustmentId(adjustment.getId())
            .productId(adjustment.getProductId())
            .locationId(adjustment.getLocationId())
            .stockItemId(adjustment.getStockItemId())
            .adjustmentType(adjustment.getAdjustmentType())
            .quantity(adjustment.getQuantity())
            .quantityBefore(adjustment.getQuantityBefore())
            .quantityAfter(adjustment.getQuantityAfter())
            .reason(adjustment.getReason())
            .adjustedAt(adjustment.getAdjustedAt())
            .build();
    }
    
    private void validateCommand(AdjustStockCommand command) {
        if (command.getTenantId() == null) {
            throw new IllegalArgumentException("TenantId is required");
        }
        if (command.getProductId() == null) {
            throw new IllegalArgumentException("ProductId is required");
        }
        if (command.getQuantity() == null || command.getQuantity().getValue() <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (command.getAdjustmentType() == null) {
            throw new IllegalArgumentException("AdjustmentType is required");
        }
        if (command.getReason() == null) {
            throw new IllegalArgumentException("AdjustmentReason is required");
        }
        
        // Validate authorization for large adjustments
        if (command.getQuantity().getValue() >= AUTHORIZATION_THRESHOLD) {
            if (command.getAuthorizationCode() == null || command.getAuthorizationCode().trim().isEmpty()) {
                throw new IllegalArgumentException(
                    String.format("Authorization code required for adjustments >= %d", AUTHORIZATION_THRESHOLD)
                );
            }
        }
    }
    
    private int getCurrentQuantity(AdjustStockCommand command) {
        if (command.getStockItemId() != null) {
            // Adjust specific stock item
            StockItem stockItem = stockItemRepository.findById(command.getStockItemId())
                .orElseThrow(() -> new IllegalArgumentException(
                    "Stock item not found: " + command.getStockItemId().getValueAsString()
                ));
            return stockItem.getQuantity().getValue();
        } else if (command.getLocationId() != null) {
            // Adjust at product/location level
            // Query all stock items for product/location and sum quantities
            return stockItemRepository.findByTenantIdAndProductIdAndLocationId(
                command.getTenantId(),
                command.getProductId(),
                command.getLocationId()
            ).stream()
                .mapToInt(item -> item.getQuantity().getValue())
                .sum();
        } else {
            // Adjust at product level (all locations)
            return stockItemRepository.findByTenantIdAndProductId(
                command.getTenantId(),
                command.getProductId()
            ).stream()
                .mapToInt(item -> item.getQuantity().getValue())
                .sum();
        }
    }
    
    private void updateStockItemQuantity(AdjustStockCommand command, StockAdjustment adjustment) {
        if (command.getStockItemId() != null) {
            // Update specific stock item
            StockItem stockItem = stockItemRepository.findById(command.getStockItemId())
                .orElseThrow(() -> new IllegalArgumentException(
                    "Stock item not found: " + command.getStockItemId().getValueAsString()
                ));
            
            // Update quantity
            int newQuantity = adjustment.getQuantityAfter();
            stockItem.updateQuantity(Quantity.of(newQuantity));
            
            stockItemRepository.save(stockItem);
        } else {
            // Update multiple stock items (product/location or product-wide)
            // For simplicity, adjust proportionally or adjust first stock item
            // In production, this might require more sophisticated logic
            // For now, we'll adjust the first stock item found
            Optional<StockItem> stockItem;
            if (command.getLocationId() != null) {
                stockItem = stockItemRepository.findByTenantIdAndProductIdAndLocationId(
                    command.getTenantId(),
                    command.getProductId(),
                    command.getLocationId()
                ).stream().findFirst();
            } else {
                stockItem = stockItemRepository.findByTenantIdAndProductId(
                    command.getTenantId(),
                    command.getProductId()
                ).stream().findFirst();
            }
            
            if (stockItem.isPresent()) {
                StockItem item = stockItem.get();
                int newQuantity = adjustment.getQuantityAfter();
                item.updateQuantity(Quantity.of(newQuantity));
                stockItemRepository.save(item);
            }
        }
    }
}
```

### Phase 2: Application Service - Query Handlers

**File:** `stock-management-application-service/src/main/java/com/ccbsa/wms/stock/application/service/query/GetStockAdjustmentsQueryHandler.java`

```java
package com.ccbsa.wms.stock.application.service.query;

import com.ccbsa.common.domain.TenantId;
import com.ccbsa.wms.stock.application.service.port.data.StockAdjustmentViewRepository;
import com.ccbsa.wms.stock.application.service.query.dto.GetStockAdjustmentsQuery;
import com.ccbsa.wms.stock.application.service.query.dto.GetStockAdjustmentsQueryResult;
import com.ccbsa.wms.stock.domain.core.valueobject.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * GetStockAdjustmentsQueryHandler
 * 
 * Handles queries for stock adjustments.
 * Uses read model (data port) for optimized queries.
 */
@Component
public class GetStockAdjustmentsQueryHandler {
    
    private final StockAdjustmentViewRepository viewRepository;
    
    public GetStockAdjustmentsQueryHandler(StockAdjustmentViewRepository viewRepository) {
        this.viewRepository = viewRepository;
    }
    
    @Transactional(readOnly = true)
    public GetStockAdjustmentsQueryResult handle(GetStockAdjustmentsQuery query) {
        // Query from read model
        List<StockAdjustmentView> views;
        
        if (query.getProductId() != null && query.getLocationId() != null) {
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
        
        // Filter by adjustment type if provided
        if (query.getAdjustmentType() != null) {
            views = views.stream()
                .filter(v -> v.getAdjustmentType() == query.getAdjustmentType())
                .collect(Collectors.toList());
        }
        
        // Filter by reason if provided
        if (query.getReason() != null) {
            views = views.stream()
                .filter(v -> v.getReason() == query.getReason())
                .collect(Collectors.toList());
        }
        
        // Map to query results
        List<StockAdjustmentQueryResult> results = views.stream()
            .map(this::mapToQueryResult)
            .collect(Collectors.toList());
        
        return GetStockAdjustmentsQueryResult.builder()
            .adjustments(results)
            .totalCount(results.size())
            .build();
    }
    
    private StockAdjustmentQueryResult mapToQueryResult(StockAdjustmentView view) {
        return StockAdjustmentQueryResult.builder()
            .adjustmentId(view.getAdjustmentId())
            .productId(view.getProductId())
            .locationId(view.getLocationId())
            .stockItemId(view.getStockItemId())
            .adjustmentType(view.getAdjustmentType())
            .quantity(view.getQuantity())
            .quantityBefore(view.getQuantityBefore())
            .quantityAfter(view.getQuantityAfter())
            .reason(view.getReason())
            .notes(view.getNotes())
            .adjustedBy(view.getAdjustedBy())
            .adjustedAt(view.getAdjustedAt())
            .build();
    }
}
```

### Phase 3: Repository Ports

**File:** `stock-management-application-service/src/main/java/com/ccbsa/wms/stock/application/service/port/repository/StockAdjustmentRepository.java`

```java
package com.ccbsa.wms.stock.application.service.port.repository;

import com.ccbsa.common.domain.TenantId;
import com.ccbsa.wms.stock.domain.core.entity.StockAdjustment;
import com.ccbsa.wms.stock.domain.core.valueobject.*;

import java.util.List;
import java.util.Optional;

/**
 * StockAdjustmentRepository Port
 * 
 * Repository port for StockAdjustment aggregate persistence (write model).
 */
public interface StockAdjustmentRepository {
    
    void save(StockAdjustment adjustment);
    
    Optional<StockAdjustment> findById(StockAdjustmentId id);
    
    List<StockAdjustment> findByTenantId(TenantId tenantId);
    
    List<StockAdjustment> findByTenantIdAndProductId(TenantId tenantId, ProductId productId);
    
    List<StockAdjustment> findByTenantIdAndProductIdAndLocationId(
        TenantId tenantId, ProductId productId, LocationId locationId
    );
    
    List<StockAdjustment> findByTenantIdAndStockItemId(TenantId tenantId, StockItemId stockItemId);
}
```

---

## Frontend Implementation

### Stock Adjustment Service

**File:** `frontend-app/src/features/stock-management/services/stockAdjustmentService.ts`

```typescript
import { apiClient } from '../../../services/apiClient';
import { ApiResponse } from '../../../types/api';

export interface CreateStockAdjustmentRequest {
  productId: string;
  locationId?: string;
  stockItemId?: string;
  adjustmentType: 'INCREASE' | 'DECREASE';
  quantity: number;
  reason: 'STOCK_COUNT' | 'DAMAGE' | 'CORRECTION' | 'THEFT' | 'EXPIRATION' | 'OTHER';
  notes?: string;
  authorizationCode?: string;
}

export interface StockAdjustmentResponse {
  adjustmentId: string;
  productId: string;
  locationId?: string;
  stockItemId?: string;
  adjustmentType: string;
  quantity: number;
  quantityBefore: number;
  quantityAfter: number;
  reason: string;
  notes?: string;
  adjustedBy: string;
  adjustedAt: string;
}

export interface StockAdjustmentFilters {
  productId?: string;
  locationId?: string;
  adjustmentType?: string;
  reason?: string;
  dateFrom?: string;
  dateTo?: string;
}

const BASE_PATH = '/api/v1/stock-management/adjustments';

export const stockAdjustmentService = {
  /**
   * Adjusts stock level.
   */
  async adjustStock(
    request: CreateStockAdjustmentRequest,
    tenantId: string
  ): Promise<ApiResponse<StockAdjustmentResponse>> {
    const response = await apiClient.post<ApiResponse<StockAdjustmentResponse>>(
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
   * Gets stock adjustments with filters.
   */
  async getStockAdjustments(
    filters: StockAdjustmentFilters,
    tenantId: string
  ): Promise<ApiResponse<StockAdjustmentResponse[]>> {
    const params = new URLSearchParams();
    if (filters.productId) params.append('productId', filters.productId);
    if (filters.locationId) params.append('locationId', filters.locationId);
    if (filters.adjustmentType) params.append('adjustmentType', filters.adjustmentType);
    if (filters.reason) params.append('reason', filters.reason);
    if (filters.dateFrom) params.append('dateFrom', filters.dateFrom);
    if (filters.dateTo) params.append('dateTo', filters.dateTo);

    const response = await apiClient.get<ApiResponse<StockAdjustmentResponse[]>>(
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
   * Gets stock adjustment by ID.
   */
  async getStockAdjustment(
    adjustmentId: string,
    tenantId: string
  ): Promise<ApiResponse<StockAdjustmentResponse>> {
    const response = await apiClient.get<ApiResponse<StockAdjustmentResponse>>(
      `${BASE_PATH}/${adjustmentId}`,
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

### Adjust Stock Flow

```
Frontend: User submits adjustment form
  ↓ POST /api/v1/stock-management/adjustments
Gateway Service
  ↓ Route to Stock Management Service
Stock Management Service (Command Controller)
  ↓ AdjustStockCommand
AdjustStockCommandHandler
  ↓ Get current quantity (from stock item or aggregate)
  ↓ Validate adjustment doesn't result in negative
  ↓ StockAdjustment.builder().build()
  ↓ StockAdjustment.adjust(currentQuantity)
  Domain Core (StockAdjustment Aggregate)
  ↓ StockAdjustedEvent
  ↓ Update stock item quantity
Repository.save()
  ↓ Persist to database
Event Publisher (after commit)
  ↓ Kafka Topic: stock-management-events
Stock Management Service (Event Listener)
  ↓ Update stock level snapshots (read model)
  ↓ Trigger reclassification if needed
Query Handler
  ↓ StockAdjustmentQueryResult
Query Controller
  ↓ Response
Gateway Service
  ↓ Response
Frontend: Display adjustment success
```

---

## Testing Strategy

### Unit Tests

- **Domain Core:** Adjustment business logic, negative stock prevention
- **Application Service:** Command/query handler logic
- **Validation:** Adjustment rules, authorization checks

### Integration Tests

- **Service Integration:** End-to-end adjustment flow
- **Database Integration:** Repository operations
- **Kafka Integration:** Event publishing and consumption
- **Negative Stock Prevention:** Test DECREASE adjustments

### Gateway API Tests

- Adjustment creation through gateway
- Negative stock prevention tests
- Authorization code validation tests
- Error scenario tests

---

## Acceptance Criteria Validation

- ✅ System allows increasing stock levels (positive adjustment)
- ✅ System allows decreasing stock levels (negative adjustment)
- ✅ System requires adjustment reason (STOCK_COUNT, DAMAGE, CORRECTION, etc.)
- ✅ System prevents negative stock levels after adjustment
- ✅ System records adjustment timestamp, user, and reason
- ✅ System publishes `StockAdjustedEvent` after successful adjustment
- ✅ System maintains audit trail of all adjustments
- ✅ System supports adjustments at consignment level or stock item level

---

**Document Control**

- **Version:** 1.0
- **Date:** 2025-01
- **Status:** Draft
- **Next Review:** Sprint planning meeting

