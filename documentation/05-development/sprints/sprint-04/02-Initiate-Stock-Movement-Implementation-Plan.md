# Initiate Stock Movement Implementation Plan

## US-3.3.2: Initiate Stock Movement

**Service:** Location Management Service, Stock Management Service
**Priority:** Must Have
**Story Points:** 5
**Sprint:** Sprint 4

---

## Overview

### User Story

**As a** warehouse operator
**I want** to initiate stock movements between locations
**So that** I can reorganize stock efficiently

### Business Requirements

- System allows selecting source and destination locations
- System validates source location has sufficient stock
- System validates destination location has capacity
- System requires movement reason (PICKING, RESTOCKING, REORGANIZATION, etc.)
- System publishes `StockMovementInitiatedEvent`
- System allows canceling initiated movements

### Technical Requirements

- Builds on US-3.3.1 Track Stock Movement
- Reuses StockMovement aggregate
- Frontend form for movement initiation
- Real-time validation

---

## UI Design

### Initiate Stock Movement Form

**Component:** `InitiateStockMovementForm.tsx`

**Features:**

- Source location selector (autocomplete)
- Destination location selector (autocomplete)
- Stock item selector with available quantity display
- Quantity input with validation
- Movement reason dropdown
- Real-time capacity validation
- Submit and cancel buttons

**UI Flow:**

1. User selects source location
2. System displays available stock items in that location
3. User selects stock item and quantity
4. User selects destination location
5. System validates destination capacity
6. User selects movement reason
7. System creates movement and displays confirmation

**Form Validation:**

- Source and destination cannot be same
- Quantity must be positive and ≤ available quantity
- Destination must have sufficient capacity
- All fields required

---

## Domain Model Design

**Note:** Reuses StockMovement aggregate from US-3.3.1.

Additional validation logic needed in command handler for source/destination validation.

---

## Backend Implementation

### Enhanced Command Handler

```java
@Component
public class CreateStockMovementCommandHandler {

    // Additional validation beyond US-3.3.1

    private void validateSourceHasStock(
        LocationId sourceLocationId,
        StockItemId stockItemId,
        Quantity quantity,
        TenantId tenantId
    ) {
        // Query stock item at source location
        StockItemLocationValidation validation =
            stockItemService.validateStockAtLocation(
                stockItemId,
                sourceLocationId,
                quantity,
                tenantId
            );

        if (!validation.hasSufficientQuantity()) {
            throw new InsufficientStockException(
                "Source location does not have sufficient stock"
            );
        }
    }
}
```

---

## Frontend Implementation

```typescript
export const InitiateStockMovementForm: React.FC = () => {
  const [sourceLocation, setSourceLocation] = useState<Location | null>(null);
  const [destinationLocation, setDestinationLocation] = useState<Location | null>(null);
  const [stockItem, setStockItem] = useState<StockItem | null>(null);
  const [quantity, setQuantity] = useState<number>(0);
  const [reason, setReason] = useState<MovementReason>('REORGANIZATION');

  const handleSubmit = async () => {
    if (!sourceLocation || !destinationLocation || !stockItem) return;

    await stockMovementService.createMovement({
      stockItemId: stockItem.id,
      sourceLocationId: sourceLocation.id,
      destinationLocationId: destinationLocation.id,
      quantity,
      movementType: determineMovementType(sourceLocation, destinationLocation),
      reason,
    }, tenantId);
  };

  const determineMovementType = (source: Location, dest: Location): MovementType => {
    if (source.type === 'RECEIVING' && dest.type === 'STORAGE') {
      return 'RECEIVING_TO_STORAGE';
    } else if (source.type === 'STORAGE' && dest.type === 'PICKING') {
      return 'STORAGE_TO_PICKING';
    } else if (source.type === 'STORAGE' && dest.type === 'STORAGE') {
      return 'INTER_STORAGE';
    } else if (source.type === 'PICKING' && dest.type === 'SHIPPING') {
      return 'PICKING_TO_SHIPPING';
    }
    return 'INTER_STORAGE';
  };

  return (
    <Form onSubmit={handleSubmit}>
      <LocationAutocomplete
        label="Source Location"
        value={sourceLocation}
        onChange={setSourceLocation}
      />
      <StockItemSelector
        locationId={sourceLocation?.id}
        value={stockItem}
        onChange={setStockItem}
      />
      <TextField
        label="Quantity"
        type="number"
        value={quantity}
        onChange={(e) => setQuantity(Number(e.target.value))}
        inputProps={{ min: 1, max: stockItem?.availableQuantity }}
      />
      <LocationAutocomplete
        label="Destination Location"
        value={destinationLocation}
        onChange={setDestinationLocation}
        exclude={sourceLocation?.id}
      />
      <Select
        label="Reason"
        value={reason}
        onChange={(e) => setReason(e.target.value as MovementReason)}
      >
        <MenuItem value="PICKING">Picking</MenuItem>
        <MenuItem value="RESTOCKING">Restocking</MenuItem>
        <MenuItem value="REORGANIZATION">Reorganization</MenuItem>
        <MenuItem value="CONSOLIDATION">Consolidation</MenuItem>
      </Select>
      <Button type="submit">Initiate Movement</Button>
    </Form>
  );
};
```

---

## Testing Strategy

### Gateway API Tests

```java
@Test
public void shouldValidateSourceLocationHasStock() {
    CreateStockMovementRequest request = CreateStockMovementRequest.builder()
        .stockItemId(stockItemId)
        .sourceLocationId(emptyLocationId)
        .destinationLocationId(destinationLocationId)
        .quantity(10)
        .movementType("INTER_STORAGE")
        .reason("REORGANIZATION")
        .build();

    authenticatedPost("/api/v1/location-management/stock-movements",
        accessToken, tenantId, request)
        .exchange()
        .expectStatus().isBadRequest()
        .expectBody()
        .jsonPath("$.success").isEqualTo(false)
        .jsonPath("$.message").value(containsString("insufficient stock"));
}

@Test
public void shouldValidateDestinationCapacity() {
    CreateStockMovementRequest request = CreateStockMovementRequest.builder()
        .stockItemId(stockItemId)
        .sourceLocationId(sourceLocationId)
        .destinationLocationId(fullLocationId)
        .quantity(100)
        .movementType("INTER_STORAGE")
        .reason("REORGANIZATION")
        .build();

    authenticatedPost("/api/v1/location-management/stock-movements",
        accessToken, tenantId, request)
        .exchange()
        .expectStatus().isBadRequest()
        .expectBody()
        .jsonPath("$.success").isEqualTo(false)
        .jsonPath("$.message").value(containsString("insufficient capacity"));
}
```

---

## Acceptance Criteria Validation

- ✅ **AC1:** System allows selecting source and destination locations (UI form)
- ✅ **AC2:** System validates source location has sufficient stock (Command handler validation)
- ✅ **AC3:** System validates destination location has capacity (Command handler validation)
- ✅ **AC4:** System requires movement reason (Form validation + domain model)
- ✅ **AC5:** System publishes StockMovementInitiatedEvent (Aggregate build() method)
- ✅ **AC6:** System allows canceling initiated movements (CancelStockMovementCommandHandler)

---

**Document Control**

- **Version:** 1.0
- **Date:** 2025-01
- **Status:** Draft
