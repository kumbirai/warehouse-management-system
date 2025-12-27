# Manage Location Status Implementation Plan

## US-3.4.1: Manage Location Status

**Service:** Location Management Service
**Priority:** Must Have
**Story Points:** 5
**Sprint:** Sprint 4

---

## Overview

### User Story

**As a** warehouse operator
**I want** to manage location status
**So that** I can block locations for maintenance or control capacity

### Business Requirements

- System tracks location status: OCCUPIED, AVAILABLE, RESERVED, BLOCKED
- System updates location status in real-time
- System tracks location capacity (current vs maximum)
- System allows blocking locations for maintenance or issues
- System prevents assignment to blocked locations
- System publishes `LocationStatusChangedEvent`

---

## UI Design

### Location Status Management Panel

**Component:** `LocationStatusPanel.tsx`

```typescript
<Card>
  <CardHeader title={`Location ${location.code}`} />
  <CardContent>
    <Stack spacing={2}>
      <Box>
        <Typography variant="subtitle2">Status</Typography>
        <Chip
          label={location.status}
          color={getStatusColor(location.status)}
        />
      </Box>
      <Box>
        <Typography variant="subtitle2">Capacity</Typography>
        <LinearProgress
          variant="determinate"
          value={(location.currentQuantity / location.maxQuantity) * 100}
        />
        <Typography variant="caption">
          {location.currentQuantity} / {location.maxQuantity}
        </Typography>
      </Box>
      <ButtonGroup>
        <Button
          onClick={() => updateStatus('BLOCKED')}
          disabled={location.status === 'BLOCKED'}
        >
          Block Location
        </Button>
        <Button
          onClick={() => updateStatus('AVAILABLE')}
          disabled={location.status === 'AVAILABLE'}
        >
          Mark Available
        </Button>
      </ButtonGroup>
    </Stack>
  </CardContent>
</Card>
```

---

## Domain Model Design

### Location Aggregate Enhancement

**Package:** `com.ccbsa.wms.location.domain.core.entity`

```java
public class Location extends TenantAwareAggregateRoot<LocationId> {

    private LocationStatus status; // Already exists
    private LocationCapacity capacity; // Already exists
    private String blockReason; // NEW
    private UserId blockedBy; // NEW
    private LocalDateTime blockedAt; // NEW

    /**
     * Business logic method: Block location.
     */
    public void block(UserId blockedBy, String reason) {
        if (this.status == LocationStatus.BLOCKED) {
            throw new IllegalStateException("Location is already blocked");
        }

        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Block reason is required");
        }

        this.status = LocationStatus.BLOCKED;
        this.blockedBy = blockedBy;
        this.blockedAt = LocalDateTime.now();
        this.blockReason = reason;

        addDomainEvent(new LocationBlockedEvent(
            this.getId(),
            this.tenantId,
            blockedBy,
            reason,
            LocalDateTime.now()
        ));
    }

    /**
     * Business logic method: Unblock location.
     */
    public void unblock(UserId unblockedBy) {
        if (this.status != LocationStatus.BLOCKED) {
            throw new IllegalStateException("Location is not blocked");
        }

        // Determine new status based on current quantity
        if (capacity.getCurrentQuantity() > 0) {
            this.status = LocationStatus.OCCUPIED;
        } else {
            this.status = LocationStatus.AVAILABLE;
        }

        addDomainEvent(new LocationUnblockedEvent(
            this.getId(),
            this.tenantId,
            unblockedBy,
            this.status,
            LocalDateTime.now()
        ));

        this.blockedBy = null;
        this.blockedAt = null;
        this.blockReason = null;
    }

    /**
     * Business logic method: Update capacity and status.
     */
    public void updateCapacity(int quantityChange) {
        this.capacity = this.capacity.adjust(quantityChange);

        LocationStatus oldStatus = this.status;
        LocationStatus newStatus;

        if (this.status == LocationStatus.BLOCKED) {
            // Don't change status if blocked
            return;
        }

        if (this.capacity.getCurrentQuantity() == 0) {
            newStatus = LocationStatus.AVAILABLE;
        } else if (this.capacity.getCurrentQuantity() >= this.capacity.getMaxQuantity()) {
            newStatus = LocationStatus.OCCUPIED;
        } else if (this.status == LocationStatus.RESERVED) {
            newStatus = LocationStatus.RESERVED;
        } else {
            newStatus = LocationStatus.OCCUPIED;
        }

        if (oldStatus != newStatus) {
            this.status = newStatus;
            addDomainEvent(new LocationStatusChangedEvent(
                this.getId(),
                this.tenantId,
                oldStatus,
                newStatus,
                this.capacity.getCurrentQuantity(),
                this.capacity.getMaxQuantity()
            ));
        }
    }

    public boolean canAccommodate(Quantity quantity) {
        if (this.status == LocationStatus.BLOCKED) {
            return false;
        }
        return this.capacity.canAccommodate(quantity.getValue());
    }
}
```

### LocationCapacity Value Object Enhancement

```java
public final class LocationCapacity {
    private final int currentQuantity;
    private final int maxQuantity;

    public boolean canAccommodate(int quantity) {
        return (currentQuantity + quantity) <= maxQuantity;
    }

    public LocationCapacity adjust(int quantityChange) {
        int newQuantity = currentQuantity + quantityChange;
        if (newQuantity < 0) {
            throw new IllegalArgumentException("Capacity cannot be negative");
        }
        return new LocationCapacity(newQuantity, maxQuantity);
    }

    public boolean isFull() {
        return currentQuantity >= maxQuantity;
    }

    public boolean isEmpty() {
        return currentQuantity == 0;
    }

    public int getAvailableCapacity() {
        return maxQuantity - currentQuantity;
    }
}
```

---

## Backend Implementation

### Command Handlers

```java
@Component
public class BlockLocationCommandHandler {

    private final LocationRepository repository;
    private final LocationManagementEventPublisher eventPublisher;

    @Transactional
    public BlockLocationResult handle(BlockLocationCommand command) {
        Location location = repository.findById(command.getLocationId())
            .orElseThrow(() -> new LocationNotFoundException(
                command.getLocationId().getValueAsString(),
                "Location not found"
            ));

        location.block(command.getBlockedBy(), command.getReason());

        List<DomainEvent<?>> domainEvents = new ArrayList<>(location.getDomainEvents());
        repository.save(location);

        if (!domainEvents.isEmpty()) {
            publishEventsAfterCommit(domainEvents);
            location.clearDomainEvents();
        }

        return BlockLocationResult.builder()
            .locationId(location.getId())
            .status(location.getStatus())
            .blockedAt(location.getBlockedAt())
            .build();
    }
}
```

```java
@Component
public class UpdateLocationCapacityCommandHandler {

    private final LocationRepository repository;
    private final LocationManagementEventPublisher eventPublisher;

    @Transactional
    public UpdateLocationCapacityResult handle(UpdateLocationCapacityCommand command) {
        Location location = repository.findById(command.getLocationId())
            .orElseThrow(() -> new LocationNotFoundException(
                command.getLocationId().getValueAsString(),
                "Location not found"
            ));

        location.updateCapacity(command.getQuantityChange());

        List<DomainEvent<?>> domainEvents = new ArrayList<>(location.getDomainEvents());
        repository.save(location);

        if (!domainEvents.isEmpty()) {
            publishEventsAfterCommit(domainEvents);
            location.clearDomainEvents();
        }

        return UpdateLocationCapacityResult.builder()
            .locationId(location.getId())
            .status(location.getStatus())
            .currentQuantity(location.getCapacity().getCurrentQuantity())
            .maxQuantity(location.getCapacity().getMaxQuantity())
            .build();
    }
}
```

---

## Frontend Implementation

```typescript
export const locationManagementService = {
  blockLocation: async (
    locationId: string,
    reason: string,
    tenantId: string
  ): Promise<ApiResponse<LocationResponse>> => {
    const response = await apiClient.put(
      `/api/v1/location-management/locations/${locationId}/block`,
      { reason },
      { headers: { 'X-Tenant-Id': tenantId } }
    );
    return response.data;
  },

  unblockLocation: async (
    locationId: string,
    tenantId: string
  ): Promise<ApiResponse<LocationResponse>> => {
    const response = await apiClient.put(
      `/api/v1/location-management/locations/${locationId}/unblock`,
      {},
      { headers: { 'X-Tenant-Id': tenantId } }
    );
    return response.data;
  },
};
```

---

## Database Migration

```sql
-- V6__Add_location_blocking_fields.sql

ALTER TABLE locations
ADD COLUMN block_reason VARCHAR(500),
ADD COLUMN blocked_by UUID,
ADD COLUMN blocked_at TIMESTAMP;

-- Index for blocked locations
CREATE INDEX idx_locations_blocked ON locations(status) WHERE status = 'BLOCKED';
```

---

## Testing Strategy

### Gateway API Tests

```java
@Test
public void shouldBlockLocation() {
    BlockLocationRequest request = BlockLocationRequest.builder()
        .reason("Maintenance required")
        .build();

    authenticatedPut("/api/v1/location-management/locations/" + locationId + "/block",
        accessToken, tenantId, request)
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.success").isEqualTo(true)
        .jsonPath("$.data.status").isEqualTo("BLOCKED")
        .jsonPath("$.data.blockedAt").exists();
}

@Test
public void shouldPreventAssignmentToBlockedLocation() {
    // First block the location
    blockLocation(locationId);

    // Attempt to assign stock
    CreateStockMovementRequest request = CreateStockMovementRequest.builder()
        .destinationLocationId(locationId)
        .quantity(10)
        .build();

    authenticatedPost("/api/v1/location-management/stock-movements",
        accessToken, tenantId, request)
        .exchange()
        .expectStatus().isBadRequest()
        .expectBody()
        .jsonPath("$.message").value(containsString("blocked"));
}
```

---

## Acceptance Criteria Validation

- ✅ **AC1:** System tracks location status (LocationStatus enum)
- ✅ **AC2:** System updates location status in real-time (updateCapacity() method)
- ✅ **AC3:** System tracks location capacity (LocationCapacity value object)
- ✅ **AC4:** System allows blocking locations (block() method)
- ✅ **AC5:** System prevents assignment to blocked locations (canAccommodate() check)
- ✅ **AC6:** System publishes LocationStatusChangedEvent (In domain methods)

---

**Document Control**

- **Version:** 1.0
- **Date:** 2025-01
- **Status:** Draft
