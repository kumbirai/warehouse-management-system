# Track Stock Movement Implementation Plan

## US-3.3.1: Track Stock Movement

**Service:** Location Management Service, Stock Management Service
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
**I want** all stock movements to be tracked
**So that** I have complete visibility of stock location

### Business Requirements

- System tracks movement from receiving to storage location
- System tracks movement from storage location to picking location
- System tracks movement between storage locations
- System tracks movement from picking location to shipping
- Each movement records: timestamp, user, source location, destination location, quantity, reason
- System maintains complete audit trail
- System publishes `StockMovementCompletedEvent`

### Technical Requirements

- Follow DDD, Clean Hexagonal Architecture, CQRS, Event-Driven Choreography
- Pure Java domain core (no framework dependencies)
- Movement tracking logic in domain aggregate
- Domain events for movement state changes
- Multi-tenant support
- Move common value objects to `common-domain` (DRY principle)

---

## UI Design

### Stock Movement History View

**Component:** `StockMovementHistory.tsx`

**Features:**

- **Movement Timeline** - Visual timeline of all movements for a stock item
- **Movement Details** - Expandable cards showing full movement details
- **Filter Options** - Filter by movement type, date range, user, location
- **Search** - Search by stock item, product code, or location
- **Export** - Export movement history to CSV/PDF

**UI Flow:**

1. User navigates to Stock Movement History
2. System displays list of recent movements
3. User can filter by:
    - Movement type (RECEIVING_TO_STORAGE, STORAGE_TO_PICKING, INTER_STORAGE, PICKING_TO_SHIPPING)
    - Date range
    - Source/destination location
    - User who performed movement
    - Product
4. User can click on movement to view detailed information
5. User can track movement path visually on warehouse map (future enhancement)

**Movement Card Display:**

```typescript
<Card>
  <CardHeader>
    <Stack direction="row" spacing={2} alignItems="center">
      <MovementTypeIcon type={movement.movementType} />
      <Typography variant="h6">
        Movement #{movement.movementNumber}
      </Typography>
      <Chip
        label={movement.status}
        color={getStatusColor(movement.status)}
        size="small"
      />
    </Stack>
  </CardHeader>
  <CardContent>
    <Grid container spacing={2}>
      <Grid item xs={12} md={6}>
        <Typography variant="subtitle2" color="textSecondary">
          From
        </Typography>
        <Typography variant="body1">
          {movement.sourceLocation.code} - {movement.sourceLocation.name}
        </Typography>
      </Grid>
      <Grid item xs={12} md={6}>
        <Typography variant="subtitle2" color="textSecondary">
          To
        </Typography>
        <Typography variant="body1">
          {movement.destinationLocation.code} - {movement.destinationLocation.name}
        </Typography>
      </Grid>
      <Grid item xs={12} md={4}>
        <Typography variant="subtitle2" color="textSecondary">
          Product
        </Typography>
        <Typography variant="body1">
          {movement.productCode}
        </Typography>
      </Grid>
      <Grid item xs={12} md={4}>
        <Typography variant="subtitle2" color="textSecondary">
          Quantity
        </Typography>
        <Typography variant="body1">
          {movement.quantity} {movement.unitOfMeasure}
        </Typography>
      </Grid>
      <Grid item xs={12} md={4}>
        <Typography variant="subtitle2" color="textSecondary">
          Reason
        </Typography>
        <Typography variant="body1">
          {getMovementReasonLabel(movement.reason)}
        </Typography>
      </Grid>
      <Grid item xs={12} md={6}>
        <Typography variant="subtitle2" color="textSecondary">
          Initiated By
        </Typography>
        <Typography variant="body1">
          {movement.initiatedBy} - {formatDateTime(movement.initiatedAt)}
        </Typography>
      </Grid>
      <Grid item xs={12} md={6}>
        <Typography variant="subtitle2" color="textSecondary">
          Completed By
        </Typography>
        <Typography variant="body1">
          {movement.completedBy} - {formatDateTime(movement.completedAt)}
        </Typography>
      </Grid>
    </Grid>
  </CardContent>
</Card>
```

**Movement Type Icons:**

- RECEIVING_TO_STORAGE: 📦 → 🏪
- STORAGE_TO_PICKING: 🏪 → 📋
- INTER_STORAGE: 🏪 → 🏪
- PICKING_TO_SHIPPING: 📋 → 🚚

---

## Domain Model Design

### StockMovement Aggregate Root

**Package:** `com.ccbsa.wms.location.domain.core.entity`

**Bounded Context:** Location Management (movements are location-centric operations)

```java
package com.ccbsa.wms.location.domain.core.entity;

import com.ccbsa.common.domain.TenantAwareAggregateRoot;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.common.domain.valueobject.ProductId;
import com.ccbsa.common.domain.valueobject.UserId;
import com.ccbsa.common.domain.valueobject.Quantity;
import com.ccbsa.common.domain.valueobject.MovementReason;
import com.ccbsa.wms.location.domain.core.valueobject.StockMovementId;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;
import com.ccbsa.wms.location.domain.core.valueobject.MovementStatus;
import com.ccbsa.wms.location.domain.core.valueobject.MovementType;
import com.ccbsa.wms.location.domain.core.event.StockMovementInitiatedEvent;
import com.ccbsa.wms.location.domain.core.event.StockMovementCompletedEvent;
import com.ccbsa.wms.location.domain.core.event.StockMovementCancelledEvent;
import com.ccbsa.wms.stock.domain.core.valueobject.StockItemId;

import java.time.LocalDateTime;

/**
 * Aggregate Root: StockMovement
 * <p>
 * Represents a stock movement from one location to another.
 * Maintains complete audit trail of stock movements.
 * <p>
 * Business Rules:
 * - Movement must have valid source and destination locations
 * - Source and destination locations must be different
 * - Movement quantity must be positive
 * - Movement can only be completed if status is INITIATED
 * - Movement can only be cancelled if status is INITIATED
 * - Completed movements cannot be modified
 */
public class StockMovement extends TenantAwareAggregateRoot<StockMovementId> {

    private StockItemId stockItemId;
    private ProductId productId;
    private LocationId sourceLocationId;
    private LocationId destinationLocationId;
    private Quantity quantity;
    private MovementType movementType;
    private MovementReason reason;
    private MovementStatus status;

    // Audit fields
    private UserId initiatedBy;
    private LocalDateTime initiatedAt;
    private UserId completedBy;
    private LocalDateTime completedAt;
    private UserId cancelledBy;
    private LocalDateTime cancelledAt;
    private String cancellationReason;

    private StockMovement() {
        // Private constructor for builder
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Business logic method: Complete the movement.
     * Updates source and destination location capacities.
     */
    public void complete(UserId completedBy) {
        if (this.status != MovementStatus.INITIATED) {
            throw new IllegalStateException(
                "Can only complete initiated movements. Current status: " + this.status
            );
        }

        this.status = MovementStatus.COMPLETED;
        this.completedBy = completedBy;
        this.completedAt = LocalDateTime.now();

        // Publish completion event
        addDomainEvent(new StockMovementCompletedEvent(
            this.getId(),
            this.tenantId,
            this.stockItemId,
            this.productId,
            this.sourceLocationId,
            this.destinationLocationId,
            this.quantity,
            this.movementType,
            this.reason,
            this.initiatedBy,
            this.initiatedAt,
            this.completedBy,
            this.completedAt
        ));
    }

    /**
     * Business logic method: Cancel the movement.
     */
    public void cancel(UserId cancelledBy, String cancellationReason) {
        if (this.status != MovementStatus.INITIATED) {
            throw new IllegalStateException(
                "Can only cancel initiated movements. Current status: " + this.status
            );
        }

        if (cancellationReason == null || cancellationReason.trim().isEmpty()) {
            throw new IllegalArgumentException("Cancellation reason is required");
        }

        this.status = MovementStatus.CANCELLED;
        this.cancelledBy = cancelledBy;
        this.cancelledAt = LocalDateTime.now();
        this.cancellationReason = cancellationReason;

        // Publish cancellation event
        addDomainEvent(new StockMovementCancelledEvent(
            this.getId(),
            this.tenantId,
            this.stockItemId,
            this.sourceLocationId,
            this.destinationLocationId,
            this.cancelledBy,
            this.cancelledAt,
            this.cancellationReason
        ));
    }

    // Getters
    public StockItemId getStockItemId() {
        return stockItemId;
    }

    public ProductId getProductId() {
        return productId;
    }

    public LocationId getSourceLocationId() {
        return sourceLocationId;
    }

    public LocationId getDestinationLocationId() {
        return destinationLocationId;
    }

    public Quantity getQuantity() {
        return quantity;
    }

    public MovementType getMovementType() {
        return movementType;
    }

    public MovementReason getReason() {
        return reason;
    }

    public MovementStatus getStatus() {
        return status;
    }

    public UserId getInitiatedBy() {
        return initiatedBy;
    }

    public LocalDateTime getInitiatedAt() {
        return initiatedAt;
    }

    public UserId getCompletedBy() {
        return completedBy;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    // Builder pattern
    public static class Builder {
        private StockMovement movement = new StockMovement();

        public Builder stockMovementId(StockMovementId id) {
            movement.id = id;
            return this;
        }

        public Builder tenantId(TenantId tenantId) {
            movement.tenantId = tenantId;
            return this;
        }

        public Builder stockItemId(StockItemId stockItemId) {
            movement.stockItemId = stockItemId;
            return this;
        }

        public Builder productId(ProductId productId) {
            movement.productId = productId;
            return this;
        }

        public Builder sourceLocationId(LocationId sourceLocationId) {
            movement.sourceLocationId = sourceLocationId;
            return this;
        }

        public Builder destinationLocationId(LocationId destinationLocationId) {
            movement.destinationLocationId = destinationLocationId;
            return this;
        }

        public Builder quantity(Quantity quantity) {
            movement.quantity = quantity;
            return this;
        }

        public Builder movementType(MovementType movementType) {
            movement.movementType = movementType;
            return this;
        }

        public Builder reason(MovementReason reason) {
            movement.reason = reason;
            return this;
        }

        public Builder initiatedBy(UserId initiatedBy) {
            movement.initiatedBy = initiatedBy;
            return this;
        }

        public StockMovement build() {
            validate();
            movement.status = MovementStatus.INITIATED;
            movement.initiatedAt = LocalDateTime.now();

            // Publish initiation event
            movement.addDomainEvent(new StockMovementInitiatedEvent(
                movement.getId(),
                movement.tenantId,
                movement.stockItemId,
                movement.productId,
                movement.sourceLocationId,
                movement.destinationLocationId,
                movement.quantity,
                movement.movementType,
                movement.reason,
                movement.initiatedBy,
                movement.initiatedAt
            ));

            return movement;
        }

        /**
         * Build without events - used when reconstructing from database
         */
        public StockMovement buildWithoutEvents() {
            validate();
            if (movement.status == null) {
                movement.status = MovementStatus.INITIATED;
            }
            if (movement.initiatedAt == null) {
                movement.initiatedAt = LocalDateTime.now();
            }
            return movement;
        }

        private void validate() {
            if (movement.id == null) {
                throw new IllegalArgumentException("StockMovementId is required");
            }
            if (movement.tenantId == null) {
                throw new IllegalArgumentException("TenantId is required");
            }
            if (movement.stockItemId == null) {
                throw new IllegalArgumentException("StockItemId is required");
            }
            if (movement.productId == null) {
                throw new IllegalArgumentException("ProductId is required");
            }
            if (movement.sourceLocationId == null) {
                throw new IllegalArgumentException("Source LocationId is required");
            }
            if (movement.destinationLocationId == null) {
                throw new IllegalArgumentException("Destination LocationId is required");
            }
            if (movement.sourceLocationId.equals(movement.destinationLocationId)) {
                throw new IllegalArgumentException(
                    "Source and destination locations must be different"
                );
            }
            if (movement.quantity == null || movement.quantity.getValue() <= 0) {
                throw new IllegalArgumentException("Quantity must be positive");
            }
            if (movement.movementType == null) {
                throw new IllegalArgumentException("MovementType is required");
            }
            if (movement.reason == null) {
                throw new IllegalArgumentException("MovementReason is required");
            }
            if (movement.initiatedBy == null) {
                throw new IllegalArgumentException("InitiatedBy is required");
            }
        }
    }
}
```

### Value Objects (Move to common-domain)

#### MovementReason Enum

**Package:** `com.ccbsa.common.domain.valueobject`

```java
package com.ccbsa.common.domain.valueobject;

/**
 * Value Object: MovementReason
 * <p>
 * Enum representing reasons for stock movement.
 * Shared across services (DRY principle).
 */
public enum MovementReason {
    RECEIVING("Stock received from supplier"),
    PICKING("Stock picked for order fulfillment"),
    RESTOCKING("Restocking between locations"),
    REORGANIZATION("Warehouse reorganization"),
    CONSOLIDATION("Consolidating stock from multiple locations"),
    QUALITY_CHECK("Moving for quality inspection"),
    QUARANTINE("Moving to quarantine area"),
    DAMAGE_RELOCATION("Relocating damaged stock"),
    RETURN_TO_STORAGE("Returning unpicked stock to storage"),
    SHIPPING("Moving to shipping area"),
    CYCLE_COUNT("Movement for cycle counting"),
    OTHER("Other reason");

    private final String description;

    MovementReason(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
```

#### MovementStatus Enum

**Package:** `com.ccbsa.wms.location.domain.core.valueobject`

```java
package com.ccbsa.wms.location.domain.core.valueobject;

/**
 * Value Object: MovementStatus
 * <p>
 * Enum representing status of stock movement.
 */
public enum MovementStatus {
    INITIATED("Movement initiated"),
    IN_PROGRESS("Movement in progress"),
    COMPLETED("Movement completed"),
    CANCELLED("Movement cancelled");

    private final String description;

    MovementStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
```

#### MovementType Enum

**Package:** `com.ccbsa.wms.location.domain.core.valueobject`

```java
package com.ccbsa.wms.location.domain.core.valueobject;

/**
 * Value Object: MovementType
 * <p>
 * Enum representing type of stock movement based on source and destination.
 */
public enum MovementType {
    RECEIVING_TO_STORAGE("From receiving area to storage location"),
    STORAGE_TO_PICKING("From storage location to picking area"),
    INTER_STORAGE("Between storage locations"),
    PICKING_TO_SHIPPING("From picking area to shipping area"),
    STORAGE_TO_QUARANTINE("From storage to quarantine"),
    QUARANTINE_TO_STORAGE("From quarantine back to storage"),
    RETURN_TO_STORAGE("Return from picking to storage");

    private final String description;

    MovementType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
```

### Domain Events

#### StockMovementInitiatedEvent

**Package:** `com.ccbsa.wms.location.domain.core.event`

```java
package com.ccbsa.wms.location.domain.core.event;

import com.ccbsa.common.domain.DomainEvent;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.common.domain.valueobject.ProductId;
import com.ccbsa.common.domain.valueobject.UserId;
import com.ccbsa.common.domain.valueobject.Quantity;
import com.ccbsa.common.domain.valueobject.MovementReason;
import com.ccbsa.wms.location.domain.core.valueobject.StockMovementId;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;
import com.ccbsa.wms.location.domain.core.valueobject.MovementType;
import com.ccbsa.wms.stock.domain.core.valueobject.StockItemId;
import com.ccbsa.wms.location.domain.core.entity.StockMovement;

import java.time.LocalDateTime;

/**
 * Domain Event: StockMovementInitiatedEvent
 * <p>
 * Published when a stock movement is initiated.
 */
public class StockMovementInitiatedEvent extends DomainEvent<StockMovement> {
    private final StockMovementId stockMovementId;
    private final TenantId tenantId;
    private final StockItemId stockItemId;
    private final ProductId productId;
    private final LocationId sourceLocationId;
    private final LocationId destinationLocationId;
    private final Quantity quantity;
    private final MovementType movementType;
    private final MovementReason reason;
    private final UserId initiatedBy;
    private final LocalDateTime initiatedAt;

    public StockMovementInitiatedEvent(
            StockMovementId stockMovementId,
            TenantId tenantId,
            StockItemId stockItemId,
            ProductId productId,
            LocationId sourceLocationId,
            LocationId destinationLocationId,
            Quantity quantity,
            MovementType movementType,
            MovementReason reason,
            UserId initiatedBy,
            LocalDateTime initiatedAt
    ) {
        super(stockMovementId);
        this.stockMovementId = stockMovementId;
        this.tenantId = tenantId;
        this.stockItemId = stockItemId;
        this.productId = productId;
        this.sourceLocationId = sourceLocationId;
        this.destinationLocationId = destinationLocationId;
        this.quantity = quantity;
        this.movementType = movementType;
        this.reason = reason;
        this.initiatedBy = initiatedBy;
        this.initiatedAt = initiatedAt;
    }

    // Getters
    public StockMovementId getStockMovementId() {
        return stockMovementId;
    }

    public TenantId getTenantId() {
        return tenantId;
    }

    public StockItemId getStockItemId() {
        return stockItemId;
    }

    public ProductId getProductId() {
        return productId;
    }

    public LocationId getSourceLocationId() {
        return sourceLocationId;
    }

    public LocationId getDestinationLocationId() {
        return destinationLocationId;
    }

    public Quantity getQuantity() {
        return quantity;
    }

    public MovementType getMovementType() {
        return movementType;
    }

    public MovementReason getReason() {
        return reason;
    }

    public UserId getInitiatedBy() {
        return initiatedBy;
    }

    public LocalDateTime getInitiatedAt() {
        return initiatedAt;
    }
}
```

#### StockMovementCompletedEvent

**Package:** `com.ccbsa.wms.location.domain.core.event`

```java
package com.ccbsa.wms.location.domain.core.event;

import com.ccbsa.common.domain.DomainEvent;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.common.domain.valueobject.ProductId;
import com.ccbsa.common.domain.valueobject.UserId;
import com.ccbsa.common.domain.valueobject.Quantity;
import com.ccbsa.common.domain.valueobject.MovementReason;
import com.ccbsa.wms.location.domain.core.valueobject.StockMovementId;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;
import com.ccbsa.wms.location.domain.core.valueobject.MovementType;
import com.ccbsa.wms.stock.domain.core.valueobject.StockItemId;
import com.ccbsa.wms.location.domain.core.entity.StockMovement;

import java.time.LocalDateTime;

/**
 * Domain Event: StockMovementCompletedEvent
 * <p>
 * Published when a stock movement is completed.
 * Triggers location capacity updates in both source and destination.
 */
public class StockMovementCompletedEvent extends DomainEvent<StockMovement> {
    private final StockMovementId stockMovementId;
    private final TenantId tenantId;
    private final StockItemId stockItemId;
    private final ProductId productId;
    private final LocationId sourceLocationId;
    private final LocationId destinationLocationId;
    private final Quantity quantity;
    private final MovementType movementType;
    private final MovementReason reason;
    private final UserId initiatedBy;
    private final LocalDateTime initiatedAt;
    private final UserId completedBy;
    private final LocalDateTime completedAt;

    public StockMovementCompletedEvent(
            StockMovementId stockMovementId,
            TenantId tenantId,
            StockItemId stockItemId,
            ProductId productId,
            LocationId sourceLocationId,
            LocationId destinationLocationId,
            Quantity quantity,
            MovementType movementType,
            MovementReason reason,
            UserId initiatedBy,
            LocalDateTime initiatedAt,
            UserId completedBy,
            LocalDateTime completedAt
    ) {
        super(stockMovementId);
        this.stockMovementId = stockMovementId;
        this.tenantId = tenantId;
        this.stockItemId = stockItemId;
        this.productId = productId;
        this.sourceLocationId = sourceLocationId;
        this.destinationLocationId = destinationLocationId;
        this.quantity = quantity;
        this.movementType = movementType;
        this.reason = reason;
        this.initiatedBy = initiatedBy;
        this.initiatedAt = initiatedAt;
        this.completedBy = completedBy;
        this.completedAt = completedAt;
    }

    // Getters (similar to InitiatedEvent with additional completedBy and completedAt)
}
```

---

## Backend Implementation

### Phase 1: Common Value Objects (common-domain)

**Files to Create/Move:**

1. `MovementReason.java` - Enum for movement reasons
2. Update `common-domain/pom.xml` if needed

### Phase 2: Domain Core (location-management-domain-core)

**Files to Create:**

1. `StockMovement.java` - Aggregate root
2. `StockMovementId.java` - Value object (UUID-based)
3. `MovementStatus.java` - Enum
4. `MovementType.java` - Enum
5. `StockMovementInitiatedEvent.java` - Domain event
6. `StockMovementCompletedEvent.java` - Domain event
7. `StockMovementCancelledEvent.java` - Domain event

### Phase 3: Application Service (location-management-application-service)

**Command Handlers:**

```java
package com.ccbsa.wms.location.application.service.command;

@Component
public class CreateStockMovementCommandHandler {

    private final StockMovementRepository repository;
    private final LocationManagementEventPublisher eventPublisher;
    private final LocationRepository locationRepository;
    private final StockItemServicePort stockItemService;

    @Transactional
    public CreateStockMovementResult handle(CreateStockMovementCommand command) {
        // 1. Validate command
        validateCommand(command);

        // 2. Validate source location has stock
        Location sourceLocation = locationRepository.findById(command.getSourceLocationId())
            .orElseThrow(() -> new LocationNotFoundException(
                command.getSourceLocationId().getValueAsString(),
                "Source location not found"
            ));

        // 3. Validate destination location has capacity
        Location destinationLocation = locationRepository.findById(command.getDestinationLocationId())
            .orElseThrow(() -> new LocationNotFoundException(
                command.getDestinationLocationId().getValueAsString(),
                "Destination location not found"
            ));

        if (!destinationLocation.canAccommodate(command.getQuantity())) {
            throw new InsufficientCapacityException(
                "Destination location does not have sufficient capacity"
            );
        }

        // 4. Validate stock item exists via synchronous call
        StockItemValidationResult stockItemValidation =
            stockItemService.validateStockItem(
                command.getStockItemId(),
                command.getQuantity(),
                command.getTenantId()
            );

        if (!stockItemValidation.isValid()) {
            throw new InvalidStockItemException(stockItemValidation.getErrorMessage());
        }

        // 5. Create aggregate using builder
        StockMovement movement = StockMovement.builder()
            .stockMovementId(StockMovementId.generate())
            .tenantId(command.getTenantId())
            .stockItemId(command.getStockItemId())
            .productId(stockItemValidation.getProductId())
            .sourceLocationId(command.getSourceLocationId())
            .destinationLocationId(command.getDestinationLocationId())
            .quantity(command.getQuantity())
            .movementType(command.getMovementType())
            .reason(command.getReason())
            .initiatedBy(command.getInitiatedBy())
            .build();

        // 6. Get domain events BEFORE saving
        List<DomainEvent<?>> domainEvents = new ArrayList<>(movement.getDomainEvents());

        // 7. Persist aggregate
        repository.save(movement);

        // 8. Publish events after transaction commit
        if (!domainEvents.isEmpty()) {
            publishEventsAfterCommit(domainEvents);
            movement.clearDomainEvents();
        }

        // 9. Return result
        return CreateStockMovementResult.builder()
            .stockMovementId(movement.getId())
            .status(movement.getStatus())
            .initiatedAt(movement.getInitiatedAt())
            .build();
    }

    private void publishEventsAfterCommit(List<DomainEvent<?>> domainEvents) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            eventPublisher.publish(domainEvents);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    eventPublisher.publish(domainEvents);
                } catch (Exception e) {
                    logger.error("Failed to publish domain events after transaction commit", e);
                }
            }
        });
    }
}
```

**Complete Movement Command Handler:**

```java
package com.ccbsa.wms.location.application.service.command;

@Component
public class CompleteStockMovementCommandHandler {

    private final StockMovementRepository repository;
    private final LocationManagementEventPublisher eventPublisher;

    @Transactional
    public CompleteStockMovementResult handle(CompleteStockMovementCommand command) {
        // 1. Validate command
        validateCommand(command);

        // 2. Load aggregate
        StockMovement movement = repository.findById(command.getStockMovementId())
            .orElseThrow(() -> new StockMovementNotFoundException(
                command.getStockMovementId().getValueAsString(),
                "Stock movement not found"
            ));

        // 3. Execute business logic
        movement.complete(command.getCompletedBy());

        // 4. Get domain events BEFORE saving
        List<DomainEvent<?>> domainEvents = new ArrayList<>(movement.getDomainEvents());

        // 5. Persist aggregate
        repository.save(movement);

        // 6. Publish events after transaction commit
        if (!domainEvents.isEmpty()) {
            publishEventsAfterCommit(domainEvents);
            movement.clearDomainEvents();
        }

        // 7. Return result
        return CompleteStockMovementResult.builder()
            .stockMovementId(movement.getId())
            .status(movement.getStatus())
            .completedAt(movement.getCompletedAt())
            .build();
    }
}
```

**Query Handlers:**

```java
package com.ccbsa.wms.location.application.service.query;

@Component
public class GetStockMovementQueryHandler {

    private final StockMovementRepository repository;

    @Transactional(readOnly = true)
    public StockMovementQueryResult handle(GetStockMovementQuery query) {
        StockMovement movement = repository.findById(query.getStockMovementId())
            .orElseThrow(() -> new StockMovementNotFoundException(
                query.getStockMovementId().getValueAsString(),
                "Stock movement not found"
            ));

        return mapToQueryResult(movement);
    }
}
```

```java
package com.ccbsa.wms.location.application.service.query;

@Component
public class ListStockMovementsQueryHandler {

    private final StockMovementRepository repository;

    @Transactional(readOnly = true)
    public ListStockMovementsQueryResult handle(ListStockMovementsQuery query) {
        List<StockMovement> movements;

        if (query.getStockItemId() != null) {
            movements = repository.findByStockItemId(
                query.getTenantId(),
                query.getStockItemId()
            );
        } else if (query.getSourceLocationId() != null) {
            movements = repository.findBySourceLocationId(
                query.getTenantId(),
                query.getSourceLocationId()
            );
        } else {
            movements = repository.findByTenantId(query.getTenantId());
        }

        List<StockMovementQueryResult> results = movements.stream()
            .map(this::mapToQueryResult)
            .collect(Collectors.toList());

        return ListStockMovementsQueryResult.builder()
            .movements(results)
            .totalCount(results.size())
            .build();
    }
}
```

### Phase 4: Data Access (location-management-dataaccess)

**Files to Create:**

1. `StockMovementEntity.java` - JPA entity
2. `StockMovementJpaRepository.java` - JPA repository
3. `StockMovementRepositoryAdapter.java` - Repository adapter
4. `StockMovementEntityMapper.java` - Entity mapper
5. Database migration: `V5__Create_stock_movements_table.sql`

**Database Migration:**

```sql
-- V5__Create_stock_movements_table.sql

CREATE TABLE stock_movements (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    stock_item_id UUID NOT NULL,
    product_id UUID NOT NULL,
    source_location_id UUID NOT NULL,
    destination_location_id UUID NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    movement_type VARCHAR(50) NOT NULL,
    reason VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL,
    initiated_by UUID NOT NULL,
    initiated_at TIMESTAMP NOT NULL,
    completed_by UUID,
    completed_at TIMESTAMP,
    cancelled_by UUID,
    cancelled_at TIMESTAMP,
    cancellation_reason VARCHAR(500),
    CONSTRAINT fk_stock_movement_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT chk_different_locations CHECK (source_location_id != destination_location_id),
    CONSTRAINT chk_completed_state CHECK (
        (status = 'COMPLETED' AND completed_by IS NOT NULL AND completed_at IS NOT NULL) OR
        (status != 'COMPLETED' AND completed_by IS NULL AND completed_at IS NULL)
    ),
    CONSTRAINT chk_cancelled_state CHECK (
        (status = 'CANCELLED' AND cancelled_by IS NOT NULL AND cancelled_at IS NOT NULL AND cancellation_reason IS NOT NULL) OR
        (status != 'CANCELLED' AND cancelled_by IS NULL AND cancelled_at IS NULL AND cancellation_reason IS NULL)
    )
);

-- Indexes for performance
CREATE INDEX idx_stock_movements_tenant ON stock_movements(tenant_id);
CREATE INDEX idx_stock_movements_stock_item ON stock_movements(stock_item_id);
CREATE INDEX idx_stock_movements_source_location ON stock_movements(source_location_id);
CREATE INDEX idx_stock_movements_destination_location ON stock_movements(destination_location_id);
CREATE INDEX idx_stock_movements_status ON stock_movements(status);
CREATE INDEX idx_stock_movements_initiated_at ON stock_movements(initiated_at DESC);
```

---

## Frontend Implementation

### Stock Movement Service

**File:** `frontend-app/src/features/location-management/services/stockMovementService.ts`

```typescript
const BASE_PATH = '/api/v1/location-management/stock-movements';

export const stockMovementService = {
  createMovement: async (
    request: CreateStockMovementRequest,
    tenantId: string
  ): Promise<ApiResponse<StockMovementResponse>> => {
    const response = await apiClient.post(BASE_PATH, request, {
      headers: { 'X-Tenant-Id': tenantId },
    });
    return response.data;
  },

  completeMovement: async (
    movementId: string,
    tenantId: string
  ): Promise<ApiResponse<StockMovementResponse>> => {
    const response = await apiClient.put(
      `${BASE_PATH}/${movementId}/complete`,
      {},
      { headers: { 'X-Tenant-Id': tenantId } }
    );
    return response.data;
  },

  listMovements: async (
    filters: { stockItemId?: string; sourceLocationId?: string },
    tenantId: string
  ): Promise<ApiResponse<StockMovementResponse[]>> => {
    const params = new URLSearchParams();
    if (filters.stockItemId) params.append('stockItemId', filters.stockItemId);
    if (filters.sourceLocationId) params.append('sourceLocationId', filters.sourceLocationId);

    const response = await apiClient.get(`${BASE_PATH}?${params.toString()}`, {
      headers: { 'X-Tenant-Id': tenantId },
    });
    return response.data;
  },
};
```

---

## Data Flow

### Complete Movement Flow

```
Frontend: User clicks "Create Movement"
  ↓ POST /api/v1/location-management/stock-movements
Gateway Service
  ↓ Route to Location Management Service
Location Management Service (Command Controller)
  ↓ CreateStockMovementCommand
CreateStockMovementCommandHandler
  ↓ Validate source/destination locations
  ↓ Call Stock Management Service (synchronous) to validate stock item
  ↓ StockMovement.builder().build()
  ↓ StockMovementInitiatedEvent
Repository.save()
  ↓ Persist to database
Event Publisher (after commit)
  ↓ Kafka Topic: location-management-events

(Later) Frontend: User clicks "Complete Movement"
  ↓ PUT /api/v1/location-management/stock-movements/{id}/complete
CompleteStockMovementCommandHandler
  ↓ movement.complete(userId)
  ↓ StockMovementCompletedEvent
Repository.save()
  ↓ Update database
Event Publisher (after commit)
  ↓ Kafka Topic: location-management-events

Event Listeners:
  ↓ StockMovementCompletedEventListener (Location Service)
  ↓ Update source location capacity (-quantity)
  ↓ Update destination location capacity (+quantity)
  ↓ LocationStatusChangedEvent (if status changes)

  ↓ StockMovementCompletedEventListener (Stock Service)
  ↓ Update stock item location
```

---

## Testing Strategy

### Unit Tests

1. **StockMovement Aggregate Tests**
    - Test movement creation with builder
    - Test complete() method with valid status
    - Test complete() method with invalid status (should throw)
    - Test cancel() method with valid status
    - Test cancel() method without reason (should throw)
    - Test source/destination validation (cannot be same)

2. **Command Handler Tests**
    - CreateStockMovementCommandHandler
    - CompleteStockMovementCommandHandler
    - CancelStockMovementCommandHandler

### Integration Tests

1. **Repository Tests**
    - Save and retrieve movement
    - Find by stock item ID
    - Find by source/destination location

2. **Event Publishing Tests**
    - Verify events published after commit
    - Verify event correlation IDs

### Gateway API Tests

```java
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class StockMovementTest extends BaseIntegrationTest {

    private static String movementId;

    @Test
    @Order(1)
    public void shouldCreateStockMovement() {
        CreateStockMovementRequest request = CreateStockMovementRequest.builder()
            .stockItemId(stockItemId)
            .sourceLocationId(sourceLocationId)
            .destinationLocationId(destinationLocationId)
            .quantity(10)
            .movementType("INTER_STORAGE")
            .reason("REORGANIZATION")
            .build();

        EntityExchangeResult<ApiResponse<CreateStockMovementResponse>> result =
            authenticatedPost("/api/v1/location-management/stock-movements",
                accessToken, tenantId, request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(new ParameterizedTypeReference<ApiResponse<CreateStockMovementResponse>>() {})
                .returnResult();

        ApiResponse<CreateStockMovementResponse> apiResponse = result.getResponseBody();
        assertThat(apiResponse.isSuccess()).isTrue();
        assertThat(apiResponse.getData().getStatus()).isEqualTo("INITIATED");

        movementId = apiResponse.getData().getStockMovementId();
    }

    @Test
    @Order(2)
    public void shouldCompleteStockMovement() {
        authenticatedPut("/api/v1/location-management/stock-movements/" + movementId + "/complete",
            accessToken, tenantId, null)
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.success").isEqualTo(true)
            .jsonPath("$.data.status").isEqualTo("COMPLETED");
    }
}
```

---

## Acceptance Criteria Validation

- ✅ **AC1:** System tracks movement from receiving to storage (MovementType.RECEIVING_TO_STORAGE)
- ✅ **AC2:** System tracks movement from storage to picking (MovementType.STORAGE_TO_PICKING)
- ✅ **AC3:** System tracks movement between storage (MovementType.INTER_STORAGE)
- ✅ **AC4:** System tracks movement from picking to shipping (MovementType.PICKING_TO_SHIPPING)
- ✅ **AC5:** Each movement records timestamp, user, locations, quantity, reason (StockMovement aggregate)
- ✅ **AC6:** System maintains complete audit trail (Database with all audit fields)
- ✅ **AC7:** System publishes StockMovementCompletedEvent (In complete() method)

---

**Document Control**

- **Version:** 1.0
- **Date:** 2025-01
- **Status:** Draft
