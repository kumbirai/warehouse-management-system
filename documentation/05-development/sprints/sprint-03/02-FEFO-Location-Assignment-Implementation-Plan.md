# FEFO Location Assignment Implementation Plan

## US-2.1.2: Assign Locations Based on FEFO Principles

**Service:** Location Management Service  
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
**I want** stock to be assigned locations based on FEFO principles  
**So that** first expiring stock is positioned for first picking

### Business Requirements

- System considers expiration date when assigning locations
- Locations closer to picking zones contain stock with earlier expiration dates
- System prevents picking of newer stock before older stock expires
- Visual indicators show expiration date priority in location views
- System supports multiple expiration date ranges for location assignment

### Technical Requirements

- Follow DDD, Clean Hexagonal Architecture, CQRS, Event-Driven Choreography
- FEFO algorithm in domain service or application service
- Event-driven location assignment triggered by stock classification
- Location proximity calculation for picking zones
- Multi-tenant support

---

## UI Design

### FEFO Location Assignment Display

**Component:** `FEFOLocationAssignmentView.tsx`

**Features:**

- **Location Map** - Visual representation of warehouse locations
- **Expiration Date Indicators** - Color-coded locations by expiration date priority
- **Picking Zone Highlight** - Highlight locations closer to picking zones
- **Assignment Suggestions** - Show suggested locations for stock items
- **Assignment History** - Show assignment history and FEFO compliance

**UI Flow:**

1. User views location assignment dashboard
2. System displays warehouse location map with:
    - Locations color-coded by expiration date priority
    - Picking zones highlighted
    - Available locations marked
3. User can view stock items awaiting location assignment
4. System shows suggested locations based on FEFO principles
5. User can manually override or confirm automatic assignment
6. System displays assignment history and FEFO compliance metrics

**Location Color Coding:**

- **Red Zones** - Locations with stock expiring within 7 days (highest priority)
- **Orange Zones** - Locations with stock expiring within 30 days
- **Yellow Zones** - Locations with stock expiring within 90 days
- **Green Zones** - Locations with stock expiring beyond 90 days
- **Blue Zones** - Locations with non-perishable stock

---

## Domain Model Design

### FEFO Assignment Domain Service

**Package:** `com.ccbsa.wms.location.domain.core.service`

```java
package com.ccbsa.wms.location.domain.core.service;

import com.ccbsa.common.domain.valueobject.ExpirationDate;
import com.ccbsa.common.domain.valueobject.StockClassification;
import com.ccbsa.wms.location.domain.core.entity.Location;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;
import com.ccbsa.wms.stock.domain.core.valueobject.StockItemId;

import java.util.List;
import java.util.Map;

/**
 * Domain Service: FEFOAssignmentService
 * <p>
 * Business logic for assigning locations based on FEFO (First Expiring First Out) principles.
 * <p>
 * This is a domain service because it coordinates logic across multiple aggregates
 * within the Location Management bounded context.
 */
public class FEFOAssignmentService {
    
    /**
     * Assigns locations to stock items based on FEFO principles.
     * <p>
     * Algorithm:
     * 1. Sort stock items by expiration date (earliest first)
     * 2. Sort available locations by proximity to picking zones (closest first)
     * 3. Match stock items to locations:
     *    - Stock expiring soonest → Locations closest to picking zones
     *    - Stock expiring later → Locations further from picking zones
     * 4. Consider location capacity and current stock
     * 
     * @param stockItems Stock items to assign (sorted by expiration date)
     * @param availableLocations Available locations (sorted by proximity to picking zone)
     * @return Map of StockItemId to LocationId assignments
     */
    public Map<StockItemId, LocationId> assignLocationsFEFO(
            List<StockItemAssignmentRequest> stockItems,
            List<Location> availableLocations) {
        
        // Validate inputs
        if (stockItems == null || stockItems.isEmpty()) {
            throw new IllegalArgumentException("Stock items list cannot be empty");
        }
        if (availableLocations == null || availableLocations.isEmpty()) {
            throw new IllegalArgumentException("Available locations list cannot be empty");
        }
        
        // Sort stock items by expiration date (earliest first)
        List<StockItemAssignmentRequest> sortedStockItems = stockItems.stream()
            .sorted((a, b) -> {
                ExpirationDate expA = a.getExpirationDate();
                ExpirationDate expB = b.getExpirationDate();
                
                // Null expiration dates (non-perishable) go to end
                if (expA == null && expB == null) return 0;
                if (expA == null) return 1;
                if (expB == null) return -1;
                
                return expA.getValue().compareTo(expB.getValue());
            })
            .toList();
        
        // Sort locations by proximity to picking zones (closest first)
        List<Location> sortedLocations = availableLocations.stream()
            .sorted((a, b) -> {
                int proximityA = calculateProximityToPickingZone(a);
                int proximityB = calculateProximityToPickingZone(b);
                return Integer.compare(proximityA, proximityB);
            })
            .toList();
        
        // Match stock items to locations
        Map<StockItemId, LocationId> assignments = new HashMap<>();
        int locationIndex = 0;
        
        for (StockItemAssignmentRequest stockItem : sortedStockItems) {
            // Find next available location with sufficient capacity
            Location assignedLocation = findAvailableLocation(
                sortedLocations,
                stockItem.getQuantity(),
                assignments.values()
            );
            
            if (assignedLocation != null) {
                assignments.put(stockItem.getStockItemId(), assignedLocation.getId());
            } else {
                // No available location - log warning
                // In production, this would trigger an alert
                throw new IllegalStateException(
                    String.format("No available location for stock item: %s", stockItem.getStockItemId())
                );
            }
        }
        
        return assignments;
    }
    
    /**
     * Calculates proximity to picking zones.
     * <p>
     * Lower number = closer to picking zones (higher priority for FEFO).
     * <p>
     * Algorithm:
     * - Zone "A" (picking zone) = 0
     * - Zone "B" = 1
     * - Zone "C" = 2
     * - etc.
     * 
     * @param location Location to calculate proximity for
     * @return Proximity score (lower = closer to picking zones)
     */
    private int calculateProximityToPickingZone(Location location) {
        String zone = location.getCoordinates().getZone();
        
        // Zone "A" is picking zone (closest)
        if (zone.equalsIgnoreCase("A")) {
            return 0;
        }
        
        // Calculate distance from picking zone
        // For now, simple alphabetical comparison
        // In production, this would use actual warehouse layout data
        char zoneChar = zone.charAt(0);
        char pickingZoneChar = 'A';
        
        return Math.abs(zoneChar - pickingZoneChar);
    }
    
    /**
     * Finds available location with sufficient capacity.
     */
    private Location findAvailableLocation(
            List<Location> locations,
            Quantity requiredQuantity,
            Collection<LocationId> alreadyAssigned) {
        
        for (Location location : locations) {
            // Skip if already assigned in this batch
            if (alreadyAssigned.contains(location.getId())) {
                continue;
            }
            
            // Check if location is available
            if (location.getStatus() != LocationStatus.AVAILABLE) {
                continue;
            }
            
            // Check capacity
            if (location.hasCapacity(requiredQuantity)) {
                return location;
            }
        }
        
        return null;
    }
}
```

### StockItemAssignmentRequest Value Object

```java
package com.ccbsa.wms.location.domain.core.valueobject;

import com.ccbsa.common.domain.valueobject.ExpirationDate;
import com.ccbsa.wms.stock.domain.core.valueobject.StockItemId;
import com.ccbsa.wms.stock.domain.core.valueobject.Quantity;

/**
 * Value Object: StockItemAssignmentRequest
 * <p>
 * Represents a request to assign a location to a stock item.
 */
public final class StockItemAssignmentRequest {
    private final StockItemId stockItemId;
    private final Quantity quantity;
    private final ExpirationDate expirationDate;
    private final StockClassification classification;
    
    // Constructor, getters, equals, hashCode
}
```

---

## Backend Implementation

### Phase 1: Domain Service

**Module:** `location-management-domain/location-management-domain-core`

**Files to Create:**

1. `FEFOAssignmentService.java` - Domain service
2. `StockItemAssignmentRequest.java` - Value object

### Phase 2: Application Service

**Module:** `location-management-domain/location-management-application-service`

**Command Handler:**

```java
@Component
public class AssignLocationsFEFOCommandHandler {
    
    private final LocationRepository locationRepository;
    private final FEFOAssignmentService fefoAssignmentService;
    private final LocationEventPublisher eventPublisher;
    
    @Transactional
    public AssignLocationsFEResult handle(AssignLocationsFEFOCommand command) {
        // 1. Get stock items to assign (from event or query)
        List<StockItemAssignmentRequest> stockItems = command.getStockItems();
        
        // 2. Get available locations
        List<Location> availableLocations = locationRepository
            .findAvailableLocations(command.getTenantId());
        
        // 3. Assign locations using FEFO algorithm
        Map<StockItemId, LocationId> assignments = fefoAssignmentService
            .assignLocationsFEFO(stockItems, availableLocations);
        
        // 4. Update locations and publish events
        List<LocationAssignedEvent> events = new ArrayList<>();
        
        for (Map.Entry<StockItemId, LocationId> assignment : assignments.entrySet()) {
            Location location = locationRepository.findById(assignment.getValue())
                .orElseThrow(() -> new LocationNotFoundException(assignment.getValue()));
            
            // Update location status and capacity
            location.assignStock(assignment.getKey(), stockItems.stream()
                .filter(si -> si.getStockItemId().equals(assignment.getKey()))
                .findFirst()
                .map(StockItemAssignmentRequest::getQuantity)
                .orElseThrow());
            
            locationRepository.save(location);
            
            // Create event
            events.add(new LocationAssignedEvent(
                location.getId(),
                assignment.getKey(),
                location.getTenantId()
            ));
        }
        
        // 5. Publish events after transaction commit
        publishEventsAfterCommit(events);
        
        // 6. Return result
        return AssignLocationsFEResult.builder()
            .assignments(assignments)
            .build();
    }
}
```

### Phase 3: Event Listener

**Module:** `location-management-messaging`

**Event Listener:**

```java
@Component
public class StockClassifiedEventListener {
    
    private final AssignLocationsFEFOCommandHandler assignmentHandler;
    
    @KafkaListener(topics = "stock-management-events")
    public void handleStockClassifiedEvent(StockClassifiedEvent event) {
        // When stock is classified, trigger FEFO location assignment
        // This is event-driven choreography
        
        // Create assignment request
        StockItemAssignmentRequest request = StockItemAssignmentRequest.builder()
            .stockItemId(event.getStockItemId())
            .quantity(event.getQuantity())
            .expirationDate(event.getExpirationDate())
            .classification(event.getNewClassification())
            .build();
        
        // Trigger FEFO assignment
        AssignLocationsFEFOCommand command = AssignLocationsFEFOCommand.builder()
            .tenantId(event.getTenantId())
            .stockItems(List.of(request))
            .build();
        
        assignmentHandler.handle(command);
    }
}
```

---

## Frontend Implementation

### FEFO Location Assignment Component

**File:** `frontend-app/src/features/location-management/components/FEFOLocationAssignmentView.tsx`

```typescript
import React, { useEffect, useState } from 'react';
import { Grid, Paper, Typography } from '@mui/material';
import { useFEFOLocationAssignment } from '../hooks/useFEFOLocationAssignment';
import { LocationMap } from './LocationMap';
import { StockItemAssignmentList } from './StockItemAssignmentList';

export const FEFOLocationAssignmentView: React.FC = () => {
  const {
    stockItems,
    availableLocations,
    assignments,
    isLoading,
    error,
    assignLocations,
  } = useFEFOLocationAssignment();

  const handleAssign = async () => {
    await assignLocations();
  };

  return (
    <Grid container spacing={3}>
      <Grid item xs={12} md={8}>
        <Paper>
          <Typography variant="h6">Warehouse Location Map</Typography>
          <LocationMap
            locations={availableLocations}
            assignments={assignments}
            showExpirationPriority
          />
        </Paper>
      </Grid>
      <Grid item xs={12} md={4}>
        <Paper>
          <Typography variant="h6">Stock Items Awaiting Assignment</Typography>
          <StockItemAssignmentList
            stockItems={stockItems}
            assignments={assignments}
            onAssign={handleAssign}
          />
        </Paper>
      </Grid>
    </Grid>
  );
};
```

---

## Data Flow

### FEFO Location Assignment Flow

```
StockClassifiedEvent (from Stock Management Service)
  ↓
Location Management Service (Event Listener)
  ↓
AssignLocationsFEFOCommand
  ↓
FEFOAssignmentService.assignLocationsFEFO()
  ↓
Match Stock Items to Locations (FEFO algorithm)
  ↓
Update Location status and capacity
  ↓
LocationAssignedEvent Published
  ↓
Kafka Topic: location-management-events
  ↓
Stock Management Service (Event Listener)
  ↓
Update StockItem with Location
```

---

## Testing Strategy

### Unit Tests

- **FEFOAssignmentService** - Test FEFO algorithm with various scenarios
- **Proximity Calculation** - Test location proximity to picking zones
- **Capacity Validation** - Test location capacity checks

### Integration Tests

- **AssignLocationsFEFOCommandHandler** - End-to-end FEFO assignment
- **Event-Driven Flow** - Test event-driven location assignment

### Gateway API Tests

- **FEFO Location Assignment** - Test assignment endpoint
- **Location Query** - Test location availability queries

---

## Acceptance Criteria Validation

- ✅ **AC1:** Expiration date considered in `FEFOAssignmentService.assignLocationsFEFO()`
- ✅ **AC2:** Locations sorted by proximity, stock sorted by expiration date
- ✅ **AC3:** FEFO algorithm ensures earlier expiring stock gets closer locations
- ✅ **AC4:** Visual indicators in LocationMap component
- ✅ **AC5:** Multiple expiration ranges supported in classification enum

---

**Document Control**

- **Version:** 1.0
- **Date:** 2025-01
- **Status:** Draft

