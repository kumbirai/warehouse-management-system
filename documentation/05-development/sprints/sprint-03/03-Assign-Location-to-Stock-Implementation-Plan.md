# Assign Location to Stock Implementation Plan

## US-3.2.1: Assign Location to Stock

**Service:** Stock Management Service, Location Management Service  
**Priority:** Must Have  
**Story Points:** 8  
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
**I want** to assign locations to stock items  
**So that** I can track where stock is stored in the warehouse

### Business Requirements

- System allows assignment of location to stock items
- System validates location availability and capacity
- System validates stock item exists and is in valid state
- System updates location status when stock is assigned
- System publishes events for location assignment
- System supports batch location assignment

### Technical Requirements

- Follow DDD, Clean Hexagonal Architecture, CQRS, Event-Driven Choreography
- Cross-service communication (Stock Management ↔ Location Management)
- Synchronous validation calls to Location Management Service
- Event-driven updates for eventual consistency
- Multi-tenant support

---

## UI Design

### Assign Location to Stock Component

**Component:** `AssignLocationToStockForm.tsx`

**Features:**

- **Stock Item Selection** - Select stock item(s) to assign
- **Location Selection** - Select location(s) or use FEFO suggestions
- **Capacity Validation** - Show location capacity and available space
- **Batch Assignment** - Support assigning multiple stock items
- **Assignment History** - Show assignment history

**UI Flow:**

1. User navigates to "Stock Management" → "Stock Items" → "Assign Location"
2. User selects stock item(s) to assign
3. System shows available locations with:
    - Location code and barcode
    - Current capacity and available space
    - FEFO suggestions (if applicable)
4. User selects location(s) or confirms FEFO suggestions
5. System validates assignment (capacity, availability)
6. User confirms assignment
7. System displays assignment result and updates stock item list

**Location Selection Dialog:**

```typescript
<Dialog open={open} onClose={handleClose}>
  <DialogTitle>Assign Location to Stock Item</DialogTitle>
  <DialogContent>
    <StockItemInfo stockItem={selectedStockItem} />
    <LocationSelector
      availableLocations={availableLocations}
      selectedLocation={selectedLocation}
      onLocationChange={setSelectedLocation}
      showFEFOSuggestions
    />
    <CapacityIndicator
      location={selectedLocation}
      requiredQuantity={selectedStockItem.quantity}
    />
  </DialogContent>
  <DialogActions>
    <Button onClick={handleClose}>Cancel</Button>
    <Button onClick={handleAssign} variant="contained">
      Assign Location
    </Button>
  </DialogActions>
</Dialog>
```

---

## Domain Model Design

### StockItem Aggregate Updates

**Package:** `com.ccbsa.wms.stock.domain.core.entity`

**Add to StockItem:**

```java
/**
 * Business logic method: Assigns location to stock item.
 * <p>
 * Business Rules:
 * - Location must be available
 * - Location must have sufficient capacity
 * - Stock item must be in valid state (not expired, quantity > 0)
 * - Publishes LocationAssignedEvent
 */
public void assignLocation(LocationId locationId, Quantity quantity) {
    // Validate stock item can be assigned
    if (this.classification == StockClassification.EXPIRED) {
        throw new IllegalStateException("Cannot assign location to expired stock");
    }
    if (this.quantity.getValue() <= 0) {
        throw new IllegalStateException("Cannot assign location to stock with zero quantity");
    }
    
    // Validate quantity doesn't exceed stock item quantity
    if (quantity.getValue() > this.quantity.getValue()) {
        throw new IllegalArgumentException(
            String.format("Assigned quantity (%d) exceeds stock item quantity (%d)",
                quantity.getValue(), this.quantity.getValue())
        );
    }
    
    // Update location
    this.locationId = locationId;
    this.lastModifiedAt = LocalDateTime.now();
    
    // Publish domain event
    addDomainEvent(new LocationAssignedEvent(
        this.getId(),
        this.productId,
        locationId,
        quantity,
        this.expirationDate,
        this.classification
    ));
}
```

### Location Aggregate Updates

**Package:** `com.ccbsa.wms.location.domain.core.entity`

**Add to Location:**

```java
/**
 * Business logic method: Assigns stock to location.
 * <p>
 * Business Rules:
 * - Location must be available
 * - Location must have sufficient capacity
 * - Updates location status and capacity
 * - Publishes LocationStatusChangedEvent
 */
public void assignStock(StockItemId stockItemId, Quantity quantity) {
    // Validate location is available
    if (this.status != LocationStatus.AVAILABLE) {
        throw new IllegalStateException(
            String.format("Cannot assign stock to location in status: %s", this.status)
        );
    }
    
    // Validate capacity
    if (!hasCapacity(quantity)) {
        throw new IllegalStateException(
            String.format("Location does not have sufficient capacity. Required: %s, Available: %s",
                quantity.getValue(), getAvailableCapacity().getValue())
        );
    }
    
    // Update capacity
    this.currentQuantity = this.currentQuantity.add(quantity);
    
    // Update status if location is now full
    if (isFull()) {
        this.status = LocationStatus.OCCUPIED;
    }
    
    this.lastModifiedAt = LocalDateTime.now();
    
    // Publish domain event
    addDomainEvent(new LocationStatusChangedEvent(
        this.getId(),
        LocationStatus.AVAILABLE,
        this.status
    ));
}

/**
 * Checks if location has sufficient capacity.
 */
public boolean hasCapacity(Quantity quantity) {
    if (this.maximumQuantity == null) {
        return true; // No capacity limit
    }
    
    Quantity availableCapacity = getAvailableCapacity();
    return quantity.getValue() <= availableCapacity.getValue();
}

/**
 * Gets available capacity.
 */
public Quantity getAvailableCapacity() {
    if (this.maximumQuantity == null) {
        return Quantity.of(Integer.MAX_VALUE); // Unlimited capacity
    }
    
    BigDecimal available = this.maximumQuantity.subtract(this.currentQuantity);
    return Quantity.of(available.intValue());
}
```

---

## Backend Implementation

### Phase 1: Application Service

**Module:** `stock-management-domain/stock-management-application-service`

**Command Handler:**

```java
@Component
public class AssignLocationToStockCommandHandler {
    
    private final StockItemRepository stockItemRepository;
    private final LocationServicePort locationServicePort;
    private final StockManagementEventPublisher eventPublisher;
    
    @Transactional
    public AssignLocationToStockResult handle(AssignLocationToStockCommand command) {
        // 1. Validate command
        validateCommand(command);
        
        // 2. Get stock item
        StockItem stockItem = stockItemRepository
            .findById(command.getStockItemId())
            .orElseThrow(() -> new StockItemNotFoundException(command.getStockItemId()));
        
        // 3. Validate location availability and capacity (synchronous call)
        LocationAvailability locationAvailability = locationServicePort
            .checkLocationAvailability(
                command.getLocationId(),
                command.getQuantity(),
                command.getTenantId()
            );
        
        if (!locationAvailability.isAvailable()) {
            throw new LocationNotAvailableException(
                command.getLocationId(),
                locationAvailability.getReason()
            );
        }
        
        if (!locationAvailability.hasCapacity()) {
            throw new LocationCapacityExceededException(
                command.getLocationId(),
                command.getQuantity(),
                locationAvailability.getAvailableCapacity()
            );
        }
        
        // 4. Assign location to stock item
        stockItem.assignLocation(command.getLocationId(), command.getQuantity());
        
        // 5. Get domain events BEFORE saving
        List<DomainEvent<?>> domainEvents = new ArrayList<>(stockItem.getDomainEvents());
        
        // 6. Persist stock item
        StockItem savedStockItem = stockItemRepository.save(stockItem);
        
        // 7. Publish events after transaction commit
        if (!domainEvents.isEmpty()) {
            publishEventsAfterCommit(domainEvents);
        }
        
        // 8. Return result
        return AssignLocationToStockResult.builder()
            .stockItemId(savedStockItem.getId())
            .locationId(savedStockItem.getLocationId())
            .build();
    }
}
```

### Phase 2: Location Service Port

**Module:** `stock-management-domain/stock-management-application-service/port/service`

**Port Interface:**

```java
package com.ccbsa.wms.stock.application.service.port.service;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;
import com.ccbsa.wms.stock.domain.core.valueobject.Quantity;

/**
 * Port: LocationServicePort
 * <p>
 * Interface for Location Management Service integration.
 * <p>
 * This port is used for synchronous calls to Location Management Service
 * for location availability and capacity validation.
 */
public interface LocationServicePort {
    
    /**
     * Checks location availability and capacity.
     * 
     * @param locationId Location ID to check
     * @param requiredQuantity Required quantity
     * @param tenantId Tenant ID
     * @return LocationAvailability result
     */
    LocationAvailability checkLocationAvailability(
        LocationId locationId,
        Quantity requiredQuantity,
        TenantId tenantId
    );
}
```

**Port Implementation:**

**Module:** `stock-management-integration` (or create integration module)

```java
@Component
public class LocationServiceAdapter implements LocationServicePort {
    
    private final RestTemplate restTemplate;
    private final CircuitBreaker circuitBreaker;
    
    @Override
    public LocationAvailability checkLocationAvailability(
            LocationId locationId,
            Quantity requiredQuantity,
            TenantId tenantId) {
        
        return circuitBreaker.executeSupplier(() -> {
            String url = String.format(
                "http://location-management-service/api/v1/locations/%s/check-availability",
                locationId.getValueAsString()
            );
            
            CheckLocationAvailabilityRequest request = CheckLocationAvailabilityRequest.builder()
                .requiredQuantity(requiredQuantity.getValue())
                .tenantId(tenantId.getValueAsString())
                .build();
            
            ResponseEntity<LocationAvailabilityResponse> response = restTemplate.postForEntity(
                url,
                request,
                LocationAvailabilityResponse.class
            );
            
            return mapToLocationAvailability(response.getBody());
        });
    }
}
```

### Phase 3: Event Listener in Location Management Service

**Module:** `location-management-messaging`

**Event Listener:**

```java
@Component
public class LocationAssignedEventListener {
    
    private final LocationRepository locationRepository;
    private final LocationEventPublisher eventPublisher;
    
    @KafkaListener(topics = "stock-management-events")
    public void handleLocationAssignedEvent(LocationAssignedEvent event) {
        // Update location when stock item is assigned
        Location location = locationRepository
            .findById(event.getLocationId())
            .orElseThrow(() -> new LocationNotFoundException(event.getLocationId()));
        
        // Assign stock to location
        location.assignStock(
            event.getStockItemId(),
            event.getQuantity()
        );
        
        // Persist location
        locationRepository.save(location);
        
        // Publish location status changed event
        eventPublisher.publish(location.getDomainEvents());
    }
}
```

---

## Frontend Implementation

### Assign Location to Stock Service

**File:** `frontend-app/src/features/stock-management/services/stockManagementService.ts`

```typescript
async assignLocationToStock(
  stockItemId: string,
  locationId: string,
  quantity: number,
  tenantId: string
): Promise<AssignLocationToStockResponse> {
  const response = await apiClient.post<AssignLocationToStockResponse>(
    `${STOCK_MANAGEMENT_BASE_PATH}/stock-items/${stockItemId}/assign-location`,
    {
      locationId,
      quantity,
    },
    {
      headers: {
        'X-Tenant-Id': tenantId,
      },
    }
  );
  return response.data;
}
```

### Assign Location Form Component

**File:** `frontend-app/src/features/stock-management/components/AssignLocationToStockForm.tsx`

```typescript
import React, { useState } from 'react';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  TextField,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Alert,
} from '@mui/material';
import { useAssignLocationToStock } from '../hooks/useAssignLocationToStock';

interface AssignLocationToStockFormProps {
  stockItem: StockItem;
  open: boolean;
  onClose: () => void;
  onSuccess: () => void;
}

export const AssignLocationToStockForm: React.FC<AssignLocationToStockFormProps> = ({
  stockItem,
  open,
  onClose,
  onSuccess,
}) => {
  const [locationId, setLocationId] = useState<string>('');
  const [quantity, setQuantity] = useState<number>(stockItem.quantity);
  const { assignLocation, isLoading, error } = useAssignLocationToStock();

  const handleAssign = async () => {
    try {
      await assignLocation(stockItem.id, locationId, quantity);
      onSuccess();
      onClose();
    } catch (err) {
      // Error handled by hook
    }
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>Assign Location to Stock Item</DialogTitle>
      <DialogContent>
        {error && <Alert severity="error">{error.message}</Alert>}
        
        <TextField
          label="Stock Item"
          value={stockItem.productCode}
          disabled
          fullWidth
          margin="normal"
        />
        
        <TextField
          label="Quantity"
          type="number"
          value={quantity}
          onChange={(e) => setQuantity(Number(e.target.value))}
          inputProps={{ min: 1, max: stockItem.quantity }}
          fullWidth
          margin="normal"
        />
        
        <FormControl fullWidth margin="normal">
          <InputLabel>Location</InputLabel>
          <Select
            value={locationId}
            onChange={(e) => setLocationId(e.target.value)}
            label="Location"
          >
            {/* Location options loaded from Location Management Service */}
          </Select>
        </FormControl>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Cancel</Button>
        <Button
          onClick={handleAssign}
          variant="contained"
          disabled={!locationId || isLoading}
        >
          Assign Location
        </Button>
      </DialogActions>
    </Dialog>
  );
};
```

---

## Data Flow

### Assign Location to Stock Flow

```
Frontend (React)
  ↓ POST /api/v1/stock-management/stock-items/{id}/assign-location
Gateway Service
  ↓ Route to Stock Management Service
Stock Management Service (Command Controller)
  ↓ AssignLocationToStockCommand
Command Handler
  ↓ Validate Stock Item exists
  ↓ Query Location Management Service (synchronous)
  ↓ Validate Location availability and capacity
  ↓ StockItem.assignLocation()
  Domain Core (StockItem Aggregate)
  ↓ LocationAssignedEvent
Event Publisher
  ↓ Kafka Topic: stock-management-events
Location Management Service (Event Listener)
  ↓ Location.assignStock()
  ↓ LocationStatusChangedEvent
Query Handler
  ↓ StockItemQueryResult
Query Controller
  ↓ Response
Gateway Service
  ↓ Response
Frontend (React)
```

---

## Testing Strategy

### Unit Tests

- **StockItem.assignLocation()** - Test location assignment logic
- **Location.assignStock()** - Test stock assignment to location
- **Capacity Validation** - Test capacity checks

### Integration Tests

- **AssignLocationToStockCommandHandler** - End-to-end assignment
- **Location Service Integration** - Test synchronous calls
- **Event-Driven Flow** - Test event publishing and consumption

### Gateway API Tests

- **Assign Location to Stock** - Test assignment endpoint
- **Location Availability Check** - Test validation endpoint
- **Error Scenarios** - Test capacity exceeded, location unavailable

---

## Acceptance Criteria Validation

- ✅ **AC1:** `AssignLocationToStockCommand` allows location assignment
- ✅ **AC2:** Location availability and capacity validated via `LocationServicePort`
- ✅ **AC3:** Stock item validation in `StockItem.assignLocation()`
- ✅ **AC4:** Location status updated in `Location.assignStock()`
- ✅ **AC5:** `LocationAssignedEvent` published after assignment
- ✅ **AC6:** Batch assignment supported via multiple command calls

---

**Document Control**

- **Version:** 1.0
- **Date:** 2025-01
- **Status:** Draft

