# Confirm Consignment Receipt Implementation Plan

## US-1.1.6: Confirm Consignment Receipt

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
**I want** to confirm receipt of stock consignments  
**So that** I can complete the consignment receipt process and trigger location assignment

### Business Requirements

- System allows confirmation of received consignments
- System validates consignment is in RECEIVED status before confirmation
- System updates consignment status to CONFIRMED
- System records confirmation timestamp and user
- System publishes `StockConsignmentConfirmedEvent` after confirmation
- System triggers location assignment workflow after confirmation

### Technical Requirements

- Follow DDD, Clean Hexagonal Architecture, CQRS, Event-Driven Choreography
- Use existing `StockConsignment.confirm()` method
- Event-driven workflow triggers stock item creation and classification
- Multi-tenant support

---

## UI Design

### Confirm Consignment Component

**Component:** `ConfirmConsignmentDialog.tsx`

**Features:**

- **Consignment Summary** - Display consignment details
- **Line Items Review** - Show all line items with quantities and expiration dates
- **Confirmation Button** - Single button to confirm consignment
- **Status Display** - Show current status and confirmation status
- **Confirmation History** - Show confirmation timestamp and user

**UI Flow:**

1. User navigates to "Stock Management" → "Consignments" → Select consignment
2. System displays consignment details:
    - Consignment reference
    - Warehouse ID
    - Received date
    - Line items with quantities and expiration dates
    - Current status (RECEIVED)
3. User reviews consignment details
4. User clicks "Confirm Consignment" button
5. System validates consignment is in RECEIVED status
6. System confirms consignment and updates status to CONFIRMED
7. System displays confirmation success message
8. System shows updated consignment with CONFIRMED status
9. System triggers stock item creation and location assignment (background)

**Confirmation Dialog:**

```typescript
<Dialog open={open} onClose={handleClose} maxWidth="md" fullWidth>
  <DialogTitle>Confirm Consignment Receipt</DialogTitle>
  <DialogContent>
    <ConsignmentSummary consignment={consignment} />
    <Divider sx={{ my: 2 }} />
    <Typography variant="h6" gutterBottom>
      Line Items
    </Typography>
    <ConsignmentLineItemsTable lineItems={consignment.lineItems} />
    <Alert severity="info" sx={{ mt: 2 }}>
      Confirming this consignment will create stock items and trigger location assignment.
    </Alert>
  </DialogContent>
  <DialogActions>
    <Button onClick={handleClose}>Cancel</Button>
    <Button
      onClick={handleConfirm}
      variant="contained"
      color="primary"
      disabled={consignment.status !== 'RECEIVED' || isLoading}
    >
      Confirm Consignment
    </Button>
  </DialogActions>
</Dialog>
```

---

## Domain Model Design

### StockConsignment Aggregate

**Package:** `com.ccbsa.wms.stock.domain.core.entity`

**Note:** The `confirm()` method already exists in StockConsignment. This plan documents its usage and the event-driven workflow it triggers.

**Existing Method:**

```java
/**
 * Business logic method: Confirms the consignment.
 * <p>
 * Business Rules:
 * - Can only confirm received consignments
 * - Sets status to CONFIRMED
 * - Records confirmation timestamp
 * - Publishes StockConsignmentConfirmedEvent
 *
 * @throws IllegalStateException if consignment is not in RECEIVED status
 */
public void confirm() {
    if (this.status != ConsignmentStatus.RECEIVED) {
        throw new IllegalStateException(
            String.format("Cannot confirm consignment in status: %s. Only RECEIVED consignments can be confirmed.", 
                this.status)
        );
    }

    this.status = ConsignmentStatus.CONFIRMED;
    this.confirmedAt = LocalDateTime.now();
    this.lastModifiedAt = LocalDateTime.now();

    // Publish domain event
    addDomainEvent(new StockConsignmentConfirmedEvent(
        this.getId().getValueAsString(),
        this.consignmentReference,
        this.getTenantId(),
        this.warehouseId
    ));
}
```

### StockConsignmentConfirmedEvent

**Package:** `com.ccbsa.wms.stock.domain.core.event`

**Event Structure:**

```java
package com.ccbsa.wms.stock.domain.core.event;

import com.ccbsa.common.domain.DomainEvent;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.common.domain.valueobject.WarehouseId;
import com.ccbsa.wms.stock.domain.core.entity.StockConsignment;
import com.ccbsa.wms.stock.domain.core.valueobject.ConsignmentReference;

import java.time.LocalDateTime;

/**
 * Domain Event: StockConsignmentConfirmedEvent
 * <p>
 * Published when a stock consignment is confirmed.
 * <p>
 * This event triggers:
 * 1. Stock item creation from consignment line items
 * 2. Stock classification by expiration dates
 * 3. FEFO location assignment
 */
public class StockConsignmentConfirmedEvent extends DomainEvent<StockConsignment> {
    
    private final String consignmentId;
    private final ConsignmentReference consignmentReference;
    private final TenantId tenantId;
    private final WarehouseId warehouseId;
    private final LocalDateTime confirmedAt;
    
    // Constructor and getters
}
```

---

## Backend Implementation

### Phase 1: Command Handler

**Module:** `stock-management-domain/stock-management-application-service`

**Command Handler:**

```java
@Component
public class ConfirmConsignmentCommandHandler {
    
    private final StockConsignmentRepository consignmentRepository;
    private final StockManagementEventPublisher eventPublisher;
    
    @Transactional
    public ConfirmConsignmentResult handle(ConfirmConsignmentCommand command) {
        // 1. Validate command
        validateCommand(command);
        
        // 2. Get consignment
        StockConsignment consignment = consignmentRepository
            .findById(command.getConsignmentId())
            .orElseThrow(() -> new StockConsignmentNotFoundException(command.getConsignmentId()));
        
        // 3. Validate tenant
        if (!consignment.getTenantId().equals(command.getTenantId())) {
            throw new UnauthorizedException("Consignment does not belong to tenant");
        }
        
        // 4. Confirm consignment (business logic in aggregate)
        consignment.confirm();
        
        // 5. Get domain events BEFORE saving
        List<DomainEvent<?>> domainEvents = new ArrayList<>(consignment.getDomainEvents());
        
        // 6. Persist consignment
        StockConsignment savedConsignment = consignmentRepository.save(consignment);
        
        // 7. Publish events after transaction commit
        if (!domainEvents.isEmpty()) {
            publishEventsAfterCommit(domainEvents);
        }
        
        // 8. Return result
        return ConfirmConsignmentResult.builder()
            .consignmentId(savedConsignment.getId())
            .status(savedConsignment.getStatus())
            .confirmedAt(savedConsignment.getConfirmedAt())
            .build();
    }
}
```

### Phase 2: Event Listener for Stock Item Creation

**Module:** `stock-management-messaging`

**Event Listener:**

```java
@Component
public class StockConsignmentConfirmedEventListener {
    
    private final CreateStockItemCommandHandler createStockItemHandler;
    
    @KafkaListener(topics = "stock-management-events")
    public void handleStockConsignmentConfirmedEvent(StockConsignmentConfirmedEvent event) {
        // When consignment is confirmed, create stock items from line items
        // This is event-driven choreography
        
        // Get consignment with line items (query)
        StockConsignment consignment = consignmentRepository
            .findById(ConsignmentId.of(event.getConsignmentId()))
            .orElseThrow();
        
        // Create stock items for each line item
        for (ConsignmentLineItem lineItem : consignment.getLineItems()) {
            CreateStockItemCommand command = CreateStockItemCommand.builder()
                .tenantId(event.getTenantId())
                .productId(ProductId.of(lineItem.getProductCode().getValue()))
                .quantity(lineItem.getQuantity())
                .expirationDate(lineItem.getExpirationDate())
                .consignmentId(consignment.getId())
                .build();
            
            // Create stock item (this will trigger classification)
            createStockItemHandler.handle(command);
        }
    }
}
```

### Phase 3: REST API

**Module:** `stock-management-application`

**Command Controller:**

```java
@RestController
@RequestMapping("/api/v1/stock-management/consignments")
public class StockConsignmentCommandController {
    
    private final ConfirmConsignmentCommandHandler confirmHandler;
    private final StockConsignmentDTOMapper dtoMapper;
    
    @PutMapping("/{consignmentId}/confirm")
    public ResponseEntity<ApiResponse<ConfirmConsignmentResponseDTO>> confirmConsignment(
            @PathVariable String consignmentId,
            @RequestHeader("X-Tenant-Id") String tenantId) {
        
        ConfirmConsignmentCommand command = ConfirmConsignmentCommand.builder()
            .consignmentId(consignmentId)
            .tenantId(TenantId.of(tenantId))
            .build();
        
        ConfirmConsignmentResult result = confirmHandler.handle(command);
        
        ConfirmConsignmentResponseDTO responseDTO = dtoMapper.toConfirmResponseDTO(result);
        
        return ApiResponseBuilder.ok(responseDTO);
    }
}
```

---

## Frontend Implementation

### Confirm Consignment Service

**File:** `frontend-app/src/features/stock-management/services/stockManagementService.ts`

```typescript
async confirmConsignment(
  consignmentId: string,
  tenantId: string
): Promise<ConfirmConsignmentResponse> {
  const response = await apiClient.put<ConfirmConsignmentResponse>(
    `${STOCK_MANAGEMENT_BASE_PATH}/consignments/${consignmentId}/confirm`,
    {},
    {
      headers: {
        'X-Tenant-Id': tenantId,
      },
    }
  );
  return response.data;
}
```

### Confirm Consignment Hook

**File:** `frontend-app/src/features/stock-management/hooks/useConfirmConsignment.ts`

```typescript
import { useState } from 'react';
import { stockManagementService } from '../services/stockManagementService';
import { useNotification } from '../../../hooks/useNotification';

export const useConfirmConsignment = () => {
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<Error | null>(null);
  const { showSuccess, showError } = useNotification();

  const confirmConsignment = async (
    consignmentId: string,
    tenantId: string
  ) => {
    setIsLoading(true);
    setError(null);

    try {
      const result = await stockManagementService.confirmConsignment(
        consignmentId,
        tenantId
      );
      showSuccess('Consignment confirmed successfully');
      return result;
    } catch (err) {
      const error = err as Error;
      setError(error);
      showError(error.message || 'Failed to confirm consignment');
      throw error;
    } finally {
      setIsLoading(false);
    }
  };

  return {
    confirmConsignment,
    isLoading,
    error,
  };
};
```

### Confirm Consignment Dialog Component

**File:** `frontend-app/src/features/stock-management/components/ConfirmConsignmentDialog.tsx`

```typescript
import React from 'react';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  Alert,
  Divider,
  Typography,
} from '@mui/material';
import { useConfirmConsignment } from '../hooks/useConfirmConsignment';
import { Consignment } from '../types/stockManagement';
import { ConsignmentSummary } from './ConsignmentSummary';
import { ConsignmentLineItemsTable } from './ConsignmentLineItemsTable';

interface ConfirmConsignmentDialogProps {
  consignment: Consignment;
  open: boolean;
  onClose: () => void;
  onSuccess: () => void;
  tenantId: string;
}

export const ConfirmConsignmentDialog: React.FC<ConfirmConsignmentDialogProps> = ({
  consignment,
  open,
  onClose,
  onSuccess,
  tenantId,
}) => {
  const { confirmConsignment, isLoading, error } = useConfirmConsignment();

  const handleConfirm = async () => {
    try {
      await confirmConsignment(consignment.id, tenantId);
      onSuccess();
      onClose();
    } catch (err) {
      // Error handled by hook
    }
  };

  const canConfirm = consignment.status === 'RECEIVED';

  return (
    <Dialog open={open} onClose={onClose} maxWidth="md" fullWidth>
      <DialogTitle>Confirm Consignment Receipt</DialogTitle>
      <DialogContent>
        {error && <Alert severity="error">{error.message}</Alert>}
        
        {!canConfirm && (
          <Alert severity="warning">
            Only RECEIVED consignments can be confirmed. Current status: {consignment.status}
          </Alert>
        )}

        <ConsignmentSummary consignment={consignment} />
        
        <Divider sx={{ my: 2 }} />
        
        <Typography variant="h6" gutterBottom>
          Line Items
        </Typography>
        <ConsignmentLineItemsTable lineItems={consignment.lineItems} />
        
        <Alert severity="info" sx={{ mt: 2 }}>
          Confirming this consignment will:
          <ul>
            <li>Create stock items from line items</li>
            <li>Classify stock by expiration dates</li>
            <li>Trigger FEFO location assignment</li>
          </ul>
        </Alert>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Cancel</Button>
        <Button
          onClick={handleConfirm}
          variant="contained"
          color="primary"
          disabled={!canConfirm || isLoading}
        >
          {isLoading ? 'Confirming...' : 'Confirm Consignment'}
        </Button>
      </DialogActions>
    </Dialog>
  );
};
```

---

## Data Flow

### Confirm Consignment Flow

```
Frontend (React)
  ↓ PUT /api/v1/stock-management/consignments/{id}/confirm
Gateway Service
  ↓ Route to Stock Management Service
Stock Management Service (Command Controller)
  ↓ ConfirmConsignmentCommand
Command Handler
  ↓ StockConsignment.confirm()
  Domain Core (StockConsignment Aggregate)
  ↓ StockConsignmentConfirmedEvent
Event Publisher
  ↓ Kafka Topic: stock-management-events
Stock Management Service (Event Listener)
  ↓ Create StockItems for each line item
  ↓ StockItem.classify() (automatic)
  ↓ StockClassifiedEvent
Location Management Service (Event Listener)
  ↓ FEFO Location Assignment
  ↓ LocationAssignedEvent
Query Handler
  ↓ ConsignmentQueryResult
Query Controller
  ↓ Response
Gateway Service
  ↓ Response
Frontend (React)
```

---

## Testing Strategy

### Unit Tests

- **StockConsignment.confirm()** - Test confirmation logic and validation
- **Status Validation** - Test that only RECEIVED consignments can be confirmed

### Integration Tests

- **ConfirmConsignmentCommandHandler** - End-to-end confirmation
- **Event-Driven Flow** - Test event publishing and stock item creation
- **Complete Workflow** - Test confirmation → stock item creation → classification → location assignment

### Gateway API Tests

- **Confirm Consignment** - Test confirmation endpoint
- **Status Validation** - Test that non-RECEIVED consignments cannot be confirmed
- **Error Scenarios** - Test invalid consignment ID, unauthorized tenant

---

## Acceptance Criteria Validation

- ✅ **AC1:** `ConfirmConsignmentCommand` allows confirmation
- ✅ **AC2:** Status validation in `StockConsignment.confirm()`
- ✅ **AC3:** Status updated to CONFIRMED in `confirm()` method
- ✅ **AC4:** Confirmation timestamp recorded in `confirmedAt` field
- ✅ **AC5:** `StockConsignmentConfirmedEvent` published after confirmation
- ✅ **AC6:** Event listener triggers stock item creation and location assignment workflow

---

**Document Control**

- **Version:** 1.0
- **Date:** 2025-01
- **Status:** Draft

