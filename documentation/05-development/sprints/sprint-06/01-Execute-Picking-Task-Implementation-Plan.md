# US-6.3.1: Execute Picking Task Implementation Plan

## User Story

**Story ID:** US-6.3.1
**Title:** Execute Picking Task
**Epic:** Picking List Management
**Service:** Picking Service, Stock Management Service, Location Management Service
**Priority:** Must Have
**Story Points:** 8

**As a** warehouse operator
**I want** to execute picking tasks
**So that** I can pick stock according to the picking list

---

## Acceptance Criteria

- [ ] AC1: System guides picking operations based on optimized location assignments
- [ ] AC2: System validates picked quantities against picking list
- [ ] AC3: System updates stock levels in real-time during picking
- [ ] AC4: System supports partial picking scenarios
- [ ] AC5: System publishes `PickingTaskCompletedEvent` or `PartialPickingCompletedEvent`
- [ ] AC6: System records picking timestamp and user

---

## Table of Contents

1. [Overview](#overview)
2. [Frontend Implementation](#frontend-implementation)
3. [Backend Implementation](#backend-implementation)
4. [Domain Model](#domain-model)
5. [API Specification](#api-specification)
6. [Event Design](#event-design)
7. [Data Flow](#data-flow)
8. [Service Integration](#service-integration)
9. [Testing Strategy](#testing-strategy)
10. [Implementation Checklist](#implementation-checklist)

---

## Overview

### Business Context

Picking task execution is the core operation where warehouse operators physically pick products from warehouse locations. The system must:

1. **Guide operators** to correct locations with optimized sequences
2. **Validate quantities** to ensure accuracy
3. **Update stock levels** in real-time to maintain accuracy
4. **Support partial picking** when full quantity not available
5. **Track movements** for complete audit trail
6. **Prevent expired stock** from being picked

### Technical Approach

- **Frontend-First:** Build intuitive UI for picking task execution
- **Real-Time Validation:** Validate stock availability before execution
- **Event-Driven:** Trigger stock level and movement updates via events
- **CQRS:** Separate command (execute) from query (get tasks)
- **Multi-Service:** Coordinate Picking, Stock Management, and Location Management services

---

## Frontend Implementation

### UI Component: PickingTaskExecutionPage

**Location:** `frontend-app/src/features/picking/pages/PickingTaskExecutionPage.tsx`

**Purpose:** Execute individual picking tasks with real-time validation and guidance

#### Component Structure

```typescript
// PickingTaskExecutionPage.tsx
import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { usePickingTask } from '../hooks/usePickingTask';
import { usePickingTaskExecution } from '../hooks/usePickingTaskExecution';
import { PickingTaskDetail } from '../components/PickingTaskDetail';
import { QuantityInput } from '../components/QuantityInput';
import { LocationDisplay } from '../components/LocationDisplay';
import { Alert } from '../../common/Alert';
import { Button } from '../../common/Button';
import { LoadingSpinner } from '../../common/LoadingSpinner';
import { StatusBadge } from '../../common/StatusBadge';

interface PickingTaskExecutionPageProps {}

export const PickingTaskExecutionPage: React.FC<PickingTaskExecutionPageProps> = () => {
  const { taskId } = useParams<{ taskId: string }>();
  const navigate = useNavigate();

  // Fetch picking task details
  const {
    pickingTask,
    loading: taskLoading,
    error: taskError
  } = usePickingTask(taskId);

  // Execute picking task mutation
  const {
    executeTask,
    loading: executionLoading,
    error: executionError
  } = usePickingTaskExecution();

  // Local state
  const [pickedQuantity, setPickedQuantity] = useState<number>(0);
  const [isPartialPicking, setIsPartialPicking] = useState<boolean>(false);
  const [partialReason, setPartialReason] = useState<string>('');
  const [showConfirmation, setShowConfirmation] = useState<boolean>(false);

  // Initialize picked quantity when task loads
  useEffect(() => {
    if (pickingTask) {
      setPickedQuantity(pickingTask.requiredQuantity);
    }
  }, [pickingTask]);

  // Handle quantity change
  const handleQuantityChange = (quantity: number) => {
    setPickedQuantity(quantity);

    // Check if partial picking
    if (quantity < pickingTask.requiredQuantity) {
      setIsPartialPicking(true);
    } else {
      setIsPartialPicking(false);
      setPartialReason('');
    }
  };

  // Handle execute picking
  const handleExecutePicking = async () => {
    if (!taskId || !pickingTask) return;

    // Validate picked quantity
    if (pickedQuantity <= 0) {
      alert('Picked quantity must be greater than zero');
      return;
    }

    // Require reason for partial picking
    if (isPartialPicking && !partialReason.trim()) {
      alert('Please provide a reason for partial picking');
      return;
    }

    try {
      await executeTask({
        taskId,
        pickedQuantity,
        isPartialPicking,
        partialReason: isPartialPicking ? partialReason : undefined,
      });

      // Show success confirmation
      setShowConfirmation(true);

      // Navigate back to task list after delay
      setTimeout(() => {
        navigate(`/picking/picking-lists/${pickingTask.pickingListId}`);
      }, 2000);
    } catch (error) {
      console.error('Failed to execute picking task:', error);
    }
  };

  // Handle cancel
  const handleCancel = () => {
    navigate(`/picking/picking-lists/${pickingTask?.pickingListId}`);
  };

  // Loading state
  if (taskLoading) {
    return (
      <div className="flex justify-center items-center h-screen">
        <LoadingSpinner size="large" />
      </div>
    );
  }

  // Error state
  if (taskError || !pickingTask) {
    return (
      <div className="p-6">
        <Alert
          type="error"
          message={taskError || 'Picking task not found'}
        />
        <Button onClick={() => navigate('/picking/tasks')} className="mt-4">
          Back to Tasks
        </Button>
      </div>
    );
  }

  // Check if task already completed
  if (pickingTask.status === 'COMPLETED') {
    return (
      <div className="p-6">
        <Alert
          type="info"
          message="This picking task has already been completed"
        />
        <Button onClick={() => navigate('/picking/tasks')} className="mt-4">
          Back to Tasks
        </Button>
      </div>
    );
  }

  return (
    <div className="container mx-auto p-6 max-w-4xl">
      {/* Page Header */}
      <div className="mb-6">
        <h1 className="text-3xl font-bold text-gray-900">Execute Picking Task</h1>
        <p className="text-gray-600 mt-2">
          Task #{pickingTask.taskSequence} - {pickingTask.productCode}
        </p>
      </div>

      {/* Success Confirmation */}
      {showConfirmation && (
        <Alert
          type="success"
          message="Picking task executed successfully! Redirecting..."
          className="mb-6"
        />
      )}

      {/* Execution Error */}
      {executionError && (
        <Alert
          type="error"
          message={executionError}
          className="mb-6"
        />
      )}

      {/* Task Details Card */}
      <div className="bg-white rounded-lg shadow-md p-6 mb-6">
        <h2 className="text-xl font-semibold mb-4">Task Details</h2>

        <PickingTaskDetail pickingTask={pickingTask} />
      </div>

      {/* Location Guidance Card */}
      <div className="bg-blue-50 rounded-lg border border-blue-200 p-6 mb-6">
        <h2 className="text-xl font-semibold mb-4 text-blue-900">
          Location Guidance
        </h2>

        <LocationDisplay
          location={pickingTask.location}
          showBarcode={true}
          showDirections={true}
        />

        {/* Expiration Warning */}
        {pickingTask.expirationDate && (
          <div className="mt-4 p-4 bg-yellow-50 border border-yellow-200 rounded">
            <p className="text-yellow-900 font-medium">
              ⚠️ Expiration Date: {new Date(pickingTask.expirationDate).toLocaleDateString()}
            </p>
            <p className="text-yellow-800 text-sm mt-1">
              Ensure stock has not expired before picking
            </p>
          </div>
        )}
      </div>

      {/* Quantity Input Card */}
      <div className="bg-white rounded-lg shadow-md p-6 mb-6">
        <h2 className="text-xl font-semibold mb-4">Picked Quantity</h2>

        <div className="space-y-4">
          {/* Required Quantity Display */}
          <div className="flex justify-between items-center p-4 bg-gray-50 rounded">
            <span className="text-gray-700 font-medium">Required Quantity:</span>
            <span className="text-2xl font-bold text-gray-900">
              {pickingTask.requiredQuantity}
            </span>
          </div>

          {/* Quantity Input */}
          <QuantityInput
            value={pickedQuantity}
            onChange={handleQuantityChange}
            min={1}
            max={pickingTask.requiredQuantity}
            label="Picked Quantity"
            required
            autoFocus
          />

          {/* Partial Picking Warning */}
          {isPartialPicking && (
            <Alert
              type="warning"
              message="You are picking less than the required quantity"
            />
          )}

          {/* Partial Reason Input */}
          {isPartialPicking && (
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Reason for Partial Picking *
              </label>
              <textarea
                value={partialReason}
                onChange={(e) => setPartialReason(e.target.value)}
                className="w-full border border-gray-300 rounded-md p-3 focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                rows={3}
                placeholder="e.g., Stock damaged, Insufficient quantity available, etc."
                required
              />
            </div>
          )}
        </div>
      </div>

      {/* Action Buttons */}
      <div className="flex gap-4 justify-end">
        <Button
          variant="secondary"
          onClick={handleCancel}
          disabled={executionLoading}
        >
          Cancel
        </Button>

        <Button
          variant="primary"
          onClick={handleExecutePicking}
          disabled={executionLoading || pickedQuantity <= 0}
          loading={executionLoading}
        >
          {isPartialPicking ? 'Complete Partial Picking' : 'Complete Picking'}
        </Button>
      </div>
    </div>
  );
};
```

#### Supporting Components

**PickingTaskDetail Component:**

```typescript
// PickingTaskDetail.tsx
import React from 'react';
import { PickingTask } from '../types/pickingTask';
import { StatusBadge } from '../../common/StatusBadge';

interface PickingTaskDetailProps {
  pickingTask: PickingTask;
}

export const PickingTaskDetail: React.FC<PickingTaskDetailProps> = ({
  pickingTask
}) => {
  return (
    <div className="grid grid-cols-2 gap-4">
      <div>
        <p className="text-sm text-gray-600">Task Sequence</p>
        <p className="text-lg font-semibold">#{pickingTask.taskSequence}</p>
      </div>

      <div>
        <p className="text-sm text-gray-600">Status</p>
        <StatusBadge status={pickingTask.status} />
      </div>

      <div>
        <p className="text-sm text-gray-600">Product Code</p>
        <p className="text-lg font-semibold">{pickingTask.productCode}</p>
      </div>

      <div>
        <p className="text-sm text-gray-600">Product Description</p>
        <p className="text-lg">{pickingTask.productDescription}</p>
      </div>

      <div>
        <p className="text-sm text-gray-600">Required Quantity</p>
        <p className="text-lg font-semibold">{pickingTask.requiredQuantity}</p>
      </div>

      <div>
        <p className="text-sm text-gray-600">Available Quantity</p>
        <p className="text-lg font-semibold text-green-600">
          {pickingTask.availableQuantity}
        </p>
      </div>

      {pickingTask.loadNumber && (
        <div>
          <p className="text-sm text-gray-600">Load Number</p>
          <p className="text-lg font-semibold">{pickingTask.loadNumber}</p>
        </div>
      )}

      {pickingTask.orderNumber && (
        <div>
          <p className="text-sm text-gray-600">Order Number</p>
          <p className="text-lg font-semibold">{pickingTask.orderNumber}</p>
        </div>
      )}
    </div>
  );
};
```

**LocationDisplay Component:**

```typescript
// LocationDisplay.tsx
import React from 'react';
import { Location } from '../types/location';

interface LocationDisplayProps {
  location: Location;
  showBarcode?: boolean;
  showDirections?: boolean;
}

export const LocationDisplay: React.FC<LocationDisplayProps> = ({
  location,
  showBarcode = false,
  showDirections = false
}) => {
  return (
    <div className="space-y-4">
      <div className="flex items-center gap-4">
        <div className="flex-1">
          <p className="text-sm text-gray-600">Location Code</p>
          <p className="text-2xl font-bold text-blue-900">
            {location.locationCode}
          </p>
        </div>

        {showBarcode && (
          <div className="flex-1">
            <p className="text-sm text-gray-600">Barcode</p>
            <p className="text-lg font-mono bg-gray-100 p-2 rounded">
              {location.barcode}
            </p>
          </div>
        )}
      </div>

      <div className="grid grid-cols-4 gap-4">
        <div>
          <p className="text-xs text-gray-600">Zone</p>
          <p className="text-lg font-semibold">{location.zone}</p>
        </div>
        <div>
          <p className="text-xs text-gray-600">Aisle</p>
          <p className="text-lg font-semibold">{location.aisle}</p>
        </div>
        <div>
          <p className="text-xs text-gray-600">Rack</p>
          <p className="text-lg font-semibold">{location.rack}</p>
        </div>
        <div>
          <p className="text-xs text-gray-600">Level</p>
          <p className="text-lg font-semibold">{location.level}</p>
        </div>
      </div>

      {showDirections && location.description && (
        <div className="mt-4 p-4 bg-blue-100 border border-blue-300 rounded">
          <p className="text-blue-900 font-medium">📍 Directions:</p>
          <p className="text-blue-800 mt-1">{location.description}</p>
        </div>
      )}
    </div>
  );
};
```

#### Custom Hooks

**usePickingTask Hook:**

```typescript
// usePickingTask.ts
import { useState, useEffect } from 'react';
import { apiClient } from '../../../services/apiClient';
import { PickingTask } from '../types/pickingTask';

export const usePickingTask = (taskId: string | undefined) => {
  const [pickingTask, setPickingTask] = useState<PickingTask | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!taskId) {
      setLoading(false);
      return;
    }

    const fetchPickingTask = async () => {
      try {
        setLoading(true);
        setError(null);

        const response = await apiClient.get<PickingTask>(
          `/api/v1/picking/picking-tasks/${taskId}`
        );

        setPickingTask(response.data);
      } catch (err: any) {
        console.error('Failed to fetch picking task:', err);
        setError(err.response?.data?.message || 'Failed to load picking task');
      } finally {
        setLoading(false);
      }
    };

    fetchPickingTask();
  }, [taskId]);

  return { pickingTask, loading, error };
};
```

**usePickingTaskExecution Hook:**

```typescript
// usePickingTaskExecution.ts
import { useState } from 'react';
import { apiClient } from '../../../services/apiClient';

interface ExecutePickingTaskRequest {
  taskId: string;
  pickedQuantity: number;
  isPartialPicking: boolean;
  partialReason?: string;
}

interface ExecutePickingTaskResponse {
  success: boolean;
  message: string;
  pickingTaskId: string;
  status: string;
}

export const usePickingTaskExecution = () => {
  const [loading, setLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  const executeTask = async (request: ExecutePickingTaskRequest): Promise<void> => {
    try {
      setLoading(true);
      setError(null);

      const response = await apiClient.post<ExecutePickingTaskResponse>(
        `/api/v1/picking/picking-tasks/${request.taskId}/execute`,
        {
          pickedQuantity: request.pickedQuantity,
          isPartialPicking: request.isPartialPicking,
          partialReason: request.partialReason,
        }
      );

      if (!response.data.success) {
        throw new Error(response.data.message || 'Failed to execute picking task');
      }
    } catch (err: any) {
      console.error('Failed to execute picking task:', err);
      const errorMessage = err.response?.data?.message ||
                          err.message ||
                          'Failed to execute picking task';
      setError(errorMessage);
      throw new Error(errorMessage);
    } finally {
      setLoading(false);
    }
  };

  return { executeTask, loading, error };
};
```

#### TypeScript Types

```typescript
// pickingTask.ts
export interface PickingTask {
  pickingTaskId: string;
  pickingListId: string;
  loadNumber: string;
  orderNumber: string;
  taskSequence: number;
  productCode: string;
  productDescription: string;
  requiredQuantity: number;
  availableQuantity: number;
  pickedQuantity?: number;
  status: PickingTaskStatus;
  location: Location;
  expirationDate?: string;
  isPartialPicking: boolean;
  partialReason?: string;
  pickedBy?: string;
  pickedAt?: string;
  createdAt: string;
  updatedAt: string;
}

export type PickingTaskStatus =
  | 'PENDING'
  | 'IN_PROGRESS'
  | 'COMPLETED'
  | 'PARTIALLY_COMPLETED'
  | 'CANCELLED';

export interface Location {
  locationId: string;
  locationCode: string;
  barcode: string;
  zone: string;
  aisle: string;
  rack: string;
  level: string;
  description?: string;
}
```

---

## Backend Implementation

### Picking Service

#### Command: ExecutePickingTaskCommand

**Location:** `services/picking-service/picking-application/src/main/java/com/ccbsa/wms/picking/application/command/dto/ExecutePickingTaskCommand.java`

```java
package com.ccbsa.wms.picking.application.command.dto;

import lombok.Builder;
import lombok.Value;

import java.util.UUID;

@Value
@Builder
public class ExecutePickingTaskCommand {
    UUID pickingTaskId;
    int pickedQuantity;
    boolean isPartialPicking;
    String partialReason;
    String pickedByUserId;
}
```

#### Command Handler: ExecutePickingTaskCommandHandler

**Location:** `services/picking-service/picking-domain/picking-application-service/src/main/java/com/ccbsa/wms/picking/application/service/command/ExecutePickingTaskCommandHandler.java`

```java
package com.ccbsa.wms.picking.application.service.command;

import com.ccbsa.common.domain.DomainEvent;
import com.ccbsa.wms.picking.application.command.dto.ExecutePickingTaskCommand;
import com.ccbsa.wms.picking.application.command.dto.ExecutePickingTaskResult;
import com.ccbsa.wms.picking.application.service.port.messaging.PickingEventPublisher;
import com.ccbsa.wms.picking.application.service.port.repository.PickingTaskRepository;
import com.ccbsa.wms.picking.application.service.port.service.StockManagementServicePort;
import com.ccbsa.wms.picking.domain.core.PickingTask;
import com.ccbsa.wms.picking.domain.core.exception.ExpiredStockException;
import com.ccbsa.wms.picking.domain.core.exception.InsufficientStockException;
import com.ccbsa.wms.picking.domain.core.exception.PickingTaskNotFoundException;
import com.ccbsa.wms.picking.domain.core.valueobject.PickingTaskId;
import com.ccbsa.wms.picking.domain.core.valueobject.Quantity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExecutePickingTaskCommandHandler {

    private final PickingTaskRepository pickingTaskRepository;
    private final StockManagementServicePort stockManagementService;
    private final PickingEventPublisher eventPublisher;

    @Transactional
    public ExecutePickingTaskResult handle(ExecutePickingTaskCommand command) {
        log.info("Executing picking task: {}", command.getPickingTaskId());

        // 1. Retrieve picking task
        PickingTaskId taskId = PickingTaskId.of(command.getPickingTaskId());
        PickingTask pickingTask = pickingTaskRepository.findById(taskId)
                .orElseThrow(() -> new PickingTaskNotFoundException(
                        "Picking task not found: " + command.getPickingTaskId()));

        // 2. Validate stock availability and expiration
        validateStockAvailability(pickingTask, command.getPickedQuantity());

        // 3. Execute picking task
        Quantity pickedQuantity = Quantity.of(command.getPickedQuantity());

        if (command.isPartialPicking()) {
            pickingTask.executePartial(
                    pickedQuantity,
                    command.getPartialReason(),
                    command.getPickedByUserId()
            );
            log.info("Partial picking executed for task: {} with quantity: {}",
                    taskId, pickedQuantity);
        } else {
            pickingTask.execute(pickedQuantity, command.getPickedByUserId());
            log.info("Full picking executed for task: {} with quantity: {}",
                    taskId, pickedQuantity);
        }

        // 4. Save picking task
        pickingTaskRepository.save(pickingTask);

        // 5. Publish domain events
        List<DomainEvent<?>> events = pickingTask.getDomainEvents();
        events.forEach(eventPublisher::publish);
        pickingTask.clearDomainEvents();

        log.info("Picking task executed successfully: {}", taskId);

        // 6. Return result
        return ExecutePickingTaskResult.builder()
                .pickingTaskId(command.getPickingTaskId())
                .status(pickingTask.getStatus().name())
                .pickedQuantity(command.getPickedQuantity())
                .isPartialPicking(command.isPartialPicking())
                .build();
    }

    private void validateStockAvailability(PickingTask pickingTask, int pickedQuantity) {
        // Query stock management service for stock availability
        boolean stockAvailable = stockManagementService.checkStockAvailability(
                pickingTask.getProductId(),
                pickingTask.getLocationId(),
                Quantity.of(pickedQuantity)
        );

        if (!stockAvailable) {
            throw new InsufficientStockException(
                    String.format("Insufficient stock for product %s at location %s. " +
                                    "Required: %d, Attempting to pick: %d",
                            pickingTask.getProductId(),
                            pickingTask.getLocationId(),
                            pickingTask.getRequiredQuantity().getValue(),
                            pickedQuantity)
            );
        }

        // Check if stock is expired
        boolean stockExpired = stockManagementService.isStockExpired(
                pickingTask.getProductId(),
                pickingTask.getLocationId()
        );

        if (stockExpired) {
            throw new ExpiredStockException(
                    String.format("Cannot pick expired stock for product %s at location %s",
                            pickingTask.getProductId(),
                            pickingTask.getLocationId())
            );
        }
    }
}
```

#### Command Result

```java
package com.ccbsa.wms.picking.application.command.dto;

import lombok.Builder;
import lombok.Value;

import java.util.UUID;

@Value
@Builder
public class ExecutePickingTaskResult {
    UUID pickingTaskId;
    String status;
    int pickedQuantity;
    boolean isPartialPicking;
}
```

#### REST Controller

**Location:** `services/picking-service/picking-application/src/main/java/com/ccbsa/wms/picking/application/command/PickingTaskCommandController.java`

```java
package com.ccbsa.wms.picking.application.command;

import com.ccbsa.common.application.dto.ApiResponse;
import com.ccbsa.common.application.dto.ApiResponseBuilder;
import com.ccbsa.wms.picking.application.command.dto.ExecutePickingTaskCommand;
import com.ccbsa.wms.picking.application.command.dto.ExecutePickingTaskRequest;
import com.ccbsa.wms.picking.application.command.dto.ExecutePickingTaskResult;
import com.ccbsa.wms.picking.application.service.command.ExecutePickingTaskCommandHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/picking/picking-tasks")
@RequiredArgsConstructor
public class PickingTaskCommandController {

    private final ExecutePickingTaskCommandHandler executePickingTaskCommandHandler;

    @PostMapping("/{pickingTaskId}/execute")
    public ResponseEntity<ApiResponse<ExecutePickingTaskResult>> executePickingTask(
            @PathVariable UUID pickingTaskId,
            @Valid @RequestBody ExecutePickingTaskRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("Executing picking task: {} by user: {}",
                pickingTaskId, userDetails.getUsername());

        ExecutePickingTaskCommand command = ExecutePickingTaskCommand.builder()
                .pickingTaskId(pickingTaskId)
                .pickedQuantity(request.getPickedQuantity())
                .isPartialPicking(request.isPartialPicking())
                .partialReason(request.getPartialReason())
                .pickedByUserId(userDetails.getUsername())
                .build();

        ExecutePickingTaskResult result = executePickingTaskCommandHandler.handle(command);

        return ResponseEntity.ok(ApiResponseBuilder.ok(result));
    }
}
```

#### Request DTO

```java
package com.ccbsa.wms.picking.application.command.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutePickingTaskRequest {

    @NotNull(message = "Picked quantity is required")
    @Min(value = 1, message = "Picked quantity must be at least 1")
    private Integer pickedQuantity;

    private boolean isPartialPicking;

    private String partialReason;
}
```

---

## Domain Model

### PickingTask Aggregate

**Location:** `services/picking-service/picking-domain/picking-domain-core/src/main/java/com/ccbsa/wms/picking/domain/core/PickingTask.java`

```java
package com.ccbsa.wms.picking.domain.core;

import com.ccbsa.common.domain.AggregateRoot;
import com.ccbsa.wms.picking.domain.core.event.PickingTaskCompletedEvent;
import com.ccbsa.wms.picking.domain.core.event.PartialPickingCompletedEvent;
import com.ccbsa.wms.picking.domain.core.exception.InvalidPickingQuantityException;
import com.ccbsa.wms.picking.domain.core.exception.PickingTaskAlreadyCompletedException;
import com.ccbsa.wms.picking.domain.core.valueobject.*;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class PickingTask extends AggregateRoot<PickingTaskId> {

    private PickingListId pickingListId;
    private LoadNumber loadNumber;
    private OrderNumber orderNumber;
    private TaskSequence taskSequence;
    private ProductId productId;
    private LocationId locationId;
    private Quantity requiredQuantity;
    private Quantity pickedQuantity;
    private PickingTaskStatus status;
    private boolean isPartialPicking;
    private String partialReason;
    private String pickedByUserId;
    private LocalDateTime pickedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private PickingTask() {
        // Private constructor for builder
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private PickingTask pickingTask = new PickingTask();

        public Builder pickingTaskId(PickingTaskId id) {
            pickingTask.id = id;
            return this;
        }

        public Builder pickingListId(PickingListId pickingListId) {
            pickingTask.pickingListId = pickingListId;
            return this;
        }

        public Builder loadNumber(LoadNumber loadNumber) {
            pickingTask.loadNumber = loadNumber;
            return this;
        }

        public Builder orderNumber(OrderNumber orderNumber) {
            pickingTask.orderNumber = orderNumber;
            return this;
        }

        public Builder taskSequence(TaskSequence taskSequence) {
            pickingTask.taskSequence = taskSequence;
            return this;
        }

        public Builder productId(ProductId productId) {
            pickingTask.productId = productId;
            return this;
        }

        public Builder locationId(LocationId locationId) {
            pickingTask.locationId = locationId;
            return this;
        }

        public Builder requiredQuantity(Quantity requiredQuantity) {
            pickingTask.requiredQuantity = requiredQuantity;
            return this;
        }

        public PickingTask build() {
            validate();
            pickingTask.status = PickingTaskStatus.PENDING;
            pickingTask.isPartialPicking = false;
            pickingTask.createdAt = LocalDateTime.now();
            pickingTask.updatedAt = LocalDateTime.now();
            return pickingTask;
        }

        private void validate() {
            if (pickingTask.id == null) {
                throw new IllegalArgumentException("PickingTaskId is required");
            }
            if (pickingTask.pickingListId == null) {
                throw new IllegalArgumentException("PickingListId is required");
            }
            if (pickingTask.productId == null) {
                throw new IllegalArgumentException("ProductId is required");
            }
            if (pickingTask.locationId == null) {
                throw new IllegalArgumentException("LocationId is required");
            }
            if (pickingTask.requiredQuantity == null) {
                throw new IllegalArgumentException("RequiredQuantity is required");
            }
        }
    }

    /**
     * Execute picking task with full quantity
     */
    public void execute(Quantity pickedQuantity, String pickedByUserId) {
        validateExecutionPreconditions();
        validatePickedQuantity(pickedQuantity);

        this.pickedQuantity = pickedQuantity;
        this.pickedByUserId = pickedByUserId;
        this.pickedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();

        // Check if picking is partial
        if (pickedQuantity.getValue() < requiredQuantity.getValue()) {
            this.isPartialPicking = true;
            this.status = PickingTaskStatus.PARTIALLY_COMPLETED;

            addDomainEvent(new PartialPickingCompletedEvent(
                    this.id,
                    this.pickingListId,
                    this.productId,
                    this.locationId,
                    this.requiredQuantity,
                    this.pickedQuantity,
                    this.partialReason,
                    this.pickedByUserId
            ));
        } else {
            this.isPartialPicking = false;
            this.status = PickingTaskStatus.COMPLETED;

            addDomainEvent(new PickingTaskCompletedEvent(
                    this.id,
                    this.pickingListId,
                    this.productId,
                    this.locationId,
                    this.pickedQuantity,
                    this.pickedByUserId
            ));
        }
    }

    /**
     * Execute picking task with partial quantity
     */
    public void executePartial(Quantity pickedQuantity, String partialReason,
                               String pickedByUserId) {
        validateExecutionPreconditions();
        validatePickedQuantity(pickedQuantity);
        validatePartialReason(partialReason);

        this.pickedQuantity = pickedQuantity;
        this.isPartialPicking = true;
        this.partialReason = partialReason;
        this.pickedByUserId = pickedByUserId;
        this.pickedAt = LocalDateTime.now();
        this.status = PickingTaskStatus.PARTIALLY_COMPLETED;
        this.updatedAt = LocalDateTime.now();

        addDomainEvent(new PartialPickingCompletedEvent(
                this.id,
                this.pickingListId,
                this.productId,
                this.locationId,
                this.requiredQuantity,
                this.pickedQuantity,
                this.partialReason,
                this.pickedByUserId
        ));
    }

    private void validateExecutionPreconditions() {
        if (this.status == PickingTaskStatus.COMPLETED ||
            this.status == PickingTaskStatus.PARTIALLY_COMPLETED) {
            throw new PickingTaskAlreadyCompletedException(
                    "Picking task already completed: " + this.id);
        }
    }

    private void validatePickedQuantity(Quantity pickedQuantity) {
        if (pickedQuantity.getValue() <= 0) {
            throw new InvalidPickingQuantityException(
                    "Picked quantity must be greater than zero");
        }

        if (pickedQuantity.getValue() > requiredQuantity.getValue()) {
            throw new InvalidPickingQuantityException(
                    String.format("Picked quantity (%d) cannot exceed required quantity (%d)",
                            pickedQuantity.getValue(),
                            requiredQuantity.getValue()));
        }
    }

    private void validatePartialReason(String partialReason) {
        if (partialReason == null || partialReason.trim().isEmpty()) {
            throw new IllegalArgumentException("Partial reason is required for partial picking");
        }
    }

    public boolean isCompleted() {
        return this.status == PickingTaskStatus.COMPLETED;
    }

    public boolean isPartiallyCompleted() {
        return this.status == PickingTaskStatus.PARTIALLY_COMPLETED;
    }
}
```

### Value Objects

**PickingTaskStatus Enum:**

```java
package com.ccbsa.wms.picking.domain.core.valueobject;

public enum PickingTaskStatus {
    PENDING("Pending"),
    IN_PROGRESS("In Progress"),
    COMPLETED("Completed"),
    PARTIALLY_COMPLETED("Partially Completed"),
    CANCELLED("Cancelled");

    private final String displayName;

    PickingTaskStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
```

**TaskSequence Value Object:**

```java
package com.ccbsa.wms.picking.domain.core.valueobject;

import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode
public final class TaskSequence {
    private final int value;

    private TaskSequence(int value) {
        if (value <= 0) {
            throw new IllegalArgumentException("Task sequence must be positive");
        }
        this.value = value;
    }

    public static TaskSequence of(int value) {
        return new TaskSequence(value);
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
```

---

## Event Design

### PickingTaskCompletedEvent

**Location:** `services/picking-service/picking-domain/picking-domain-core/src/main/java/com/ccbsa/wms/picking/domain/core/event/PickingTaskCompletedEvent.java`

```java
package com.ccbsa.wms.picking.domain.core.event;

import com.ccbsa.wms.picking.domain.core.PickingTask;
import com.ccbsa.wms.picking.domain.core.valueobject.*;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class PickingTaskCompletedEvent extends PickingEvent<PickingTask> {

    private final PickingListId pickingListId;
    private final ProductId productId;
    private final LocationId locationId;
    private final Quantity pickedQuantity;
    private final String pickedByUserId;

    public PickingTaskCompletedEvent(
            PickingTaskId pickingTaskId,
            PickingListId pickingListId,
            ProductId productId,
            LocationId locationId,
            Quantity pickedQuantity,
            String pickedByUserId) {
        super(pickingTaskId);
        this.pickingListId = pickingListId;
        this.productId = productId;
        this.locationId = locationId;
        this.pickedQuantity = pickedQuantity;
        this.pickedByUserId = pickedByUserId;
    }
}
```

### PartialPickingCompletedEvent

```java
package com.ccbsa.wms.picking.domain.core.event;

import com.ccbsa.wms.picking.domain.core.PickingTask;
import com.ccbsa.wms.picking.domain.core.valueobject.*;
import lombok.Getter;

@Getter
public class PartialPickingCompletedEvent extends PickingEvent<PickingTask> {

    private final PickingListId pickingListId;
    private final ProductId productId;
    private final LocationId locationId;
    private final Quantity requiredQuantity;
    private final Quantity pickedQuantity;
    private final String partialReason;
    private final String pickedByUserId;

    public PartialPickingCompletedEvent(
            PickingTaskId pickingTaskId,
            PickingListId pickingListId,
            ProductId productId,
            LocationId locationId,
            Quantity requiredQuantity,
            Quantity pickedQuantity,
            String partialReason,
            String pickedByUserId) {
        super(pickingTaskId);
        this.pickingListId = pickingListId;
        this.productId = productId;
        this.locationId = locationId;
        this.requiredQuantity = requiredQuantity;
        this.pickedQuantity = pickedQuantity;
        this.partialReason = partialReason;
        this.pickedByUserId = pickedByUserId;
    }
}
```

---

## API Specification

### Execute Picking Task Endpoint

**Endpoint:** `POST /api/v1/picking/picking-tasks/{pickingTaskId}/execute`

**Request:**

```json
{
  "pickedQuantity": 50,
  "isPartialPicking": false,
  "partialReason": null
}
```

**Response (Success):**

```json
{
  "success": true,
  "data": {
    "pickingTaskId": "123e4567-e89b-12d3-a456-426614174000",
    "status": "COMPLETED",
    "pickedQuantity": 50,
    "isPartialPicking": false
  },
  "timestamp": "2026-01-08T10:30:00Z"
}
```

**Response (Partial Picking):**

```json
{
  "success": true,
  "data": {
    "pickingTaskId": "123e4567-e89b-12d3-a456-426614174000",
    "status": "PARTIALLY_COMPLETED",
    "pickedQuantity": 30,
    "isPartialPicking": true
  },
  "timestamp": "2026-01-08T10:30:00Z"
}
```

**Response (Error - Expired Stock):**

```json
{
  "success": false,
  "error": {
    "code": "EXPIRED_STOCK",
    "message": "Cannot pick expired stock for product P12345 at location L-A-01",
    "details": {
      "productId": "P12345",
      "locationId": "L-A-01",
      "expirationDate": "2026-01-05"
    }
  },
  "timestamp": "2026-01-08T10:30:00Z"
}
```

---

I'll continue with the remaining sections of this implementation plan in the next message due to length. Would you like me to continue with the Data Access Layer, Service Integration, Testing Strategy, and Implementation Checklist sections?
## Data Access Layer

### PickingTask Repository

**Location:** `services/picking-service/picking-dataaccess/src/main/java/com/ccbsa/wms/picking/dataaccess/adapter/PickingTaskRepositoryAdapter.java`

```java
package com.ccbsa.wms.picking.dataaccess.adapter;

import com.ccbsa.wms.picking.application.service.port.repository.PickingTaskRepository;
import com.ccbsa.wms.picking.dataaccess.entity.PickingTaskEntity;
import com.ccbsa.wms.picking.dataaccess.jpa.PickingTaskJpaRepository;
import com.ccbsa.wms.picking.dataaccess.mapper.PickingTaskEntityMapper;
import com.ccbsa.wms.picking.domain.core.PickingTask;
import com.ccbsa.wms.picking.domain.core.valueobject.PickingListId;
import com.ccbsa.wms.picking.domain.core.valueobject.PickingTaskId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Repository
@RequiredArgsConstructor
public class PickingTaskRepositoryAdapter implements PickingTaskRepository {

    private final PickingTaskJpaRepository jpaRepository;
    private final PickingTaskEntityMapper mapper;

    @Override
    public void save(PickingTask pickingTask) {
        log.debug("Saving picking task: {}", pickingTask.getId());
        PickingTaskEntity entity = mapper.toEntity(pickingTask);
        jpaRepository.save(entity);
    }

    @Override
    public Optional<PickingTask> findById(PickingTaskId pickingTaskId) {
        log.debug("Finding picking task by ID: {}", pickingTaskId);
        return jpaRepository.findById(pickingTaskId.getValue())
                .map(mapper::toDomain);
    }

    @Override
    public List<PickingTask> findByPickingListId(PickingListId pickingListId) {
        log.debug("Finding picking tasks by picking list ID: {}", pickingListId);
        return jpaRepository.findByPickingListId(pickingListId.getValue())
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsById(PickingTaskId pickingTaskId) {
        return jpaRepository.existsById(pickingTaskId.getValue());
    }
}
```

### PickingTask Entity

**Location:** `services/picking-service/picking-dataaccess/src/main/java/com/ccbsa/wms/picking/dataaccess/entity/PickingTaskEntity.java`

```java
package com.ccbsa.wms.picking.dataaccess.entity;

import com.ccbsa.wms.picking.domain.core.valueobject.PickingTaskStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "picking_tasks", schema = "tenant_schema")
public class PickingTaskEntity {

    @Id
    @Column(name = "picking_task_id")
    private UUID pickingTaskId;

    @Column(name = "picking_list_id", nullable = false)
    private UUID pickingListId;

    @Column(name = "load_number", nullable = false, length = 50)
    private String loadNumber;

    @Column(name = "order_number", nullable = false, length = 50)
    private String orderNumber;

    @Column(name = "task_sequence", nullable = false)
    private Integer taskSequence;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "location_id", nullable = false)
    private UUID locationId;

    @Column(name = "required_quantity", nullable = false)
    private Integer requiredQuantity;

    @Column(name = "picked_quantity")
    private Integer pickedQuantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private PickingTaskStatus status;

    @Column(name = "is_partial_picking", nullable = false)
    private Boolean isPartialPicking;

    @Column(name = "partial_reason", length = 500)
    private String partialReason;

    @Column(name = "picked_by_user_id", length = 100)
    private String pickedByUserId;

    @Column(name = "picked_at")
    private LocalDateTime pickedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
        if (isPartialPicking == null) {
            isPartialPicking = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
```

### Entity Mapper

```java
package com.ccbsa.wms.picking.dataaccess.mapper;

import com.ccbsa.wms.picking.dataaccess.entity.PickingTaskEntity;
import com.ccbsa.wms.picking.domain.core.PickingTask;
import com.ccbsa.wms.picking.domain.core.valueobject.*;
import org.springframework.stereotype.Component;

@Component
public class PickingTaskEntityMapper {

    public PickingTaskEntity toEntity(PickingTask domain) {
        if (domain == null) {
            return null;
        }

        return PickingTaskEntity.builder()
                .pickingTaskId(domain.getId().getValue())
                .pickingListId(domain.getPickingListId().getValue())
                .loadNumber(domain.getLoadNumber().getValue())
                .orderNumber(domain.getOrderNumber().getValue())
                .taskSequence(domain.getTaskSequence().getValue())
                .productId(domain.getProductId().getValue())
                .locationId(domain.getLocationId().getValue())
                .requiredQuantity(domain.getRequiredQuantity().getValue())
                .pickedQuantity(domain.getPickedQuantity() != null ?
                        domain.getPickedQuantity().getValue() : null)
                .status(domain.getStatus())
                .isPartialPicking(domain.isPartialPicking())
                .partialReason(domain.getPartialReason())
                .pickedByUserId(domain.getPickedByUserId())
                .pickedAt(domain.getPickedAt())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }

    public PickingTask toDomain(PickingTaskEntity entity) {
        if (entity == null) {
            return null;
        }

        return PickingTask.builder()
                .pickingTaskId(PickingTaskId.of(entity.getPickingTaskId()))
                .pickingListId(PickingListId.of(entity.getPickingListId()))
                .loadNumber(LoadNumber.of(entity.getLoadNumber()))
                .orderNumber(OrderNumber.of(entity.getOrderNumber()))
                .taskSequence(TaskSequence.of(entity.getTaskSequence()))
                .productId(ProductId.of(entity.getProductId()))
                .locationId(LocationId.of(entity.getLocationId()))
                .requiredQuantity(Quantity.of(entity.getRequiredQuantity()))
                .build();
        // Note: Other fields set via domain methods during reconstitution
    }
}
```

---

## Service Integration

### Stock Management Service Port

**Location:** `services/picking-service/picking-domain/picking-application-service/src/main/java/com/ccbsa/wms/picking/application/service/port/service/StockManagementServicePort.java`

```java
package com.ccbsa.wms.picking.application.service.port.service;

import com.ccbsa.wms.picking.domain.core.valueobject.LocationId;
import com.ccbsa.wms.picking.domain.core.valueobject.ProductId;
import com.ccbsa.wms.picking.domain.core.valueobject.Quantity;

/**
 * Port interface for Stock Management Service integration
 */
public interface StockManagementServicePort {

    /**
     * Check if sufficient stock is available at location
     *
     * @param productId Product identifier
     * @param locationId Location identifier
     * @param quantity Required quantity
     * @return true if stock available, false otherwise
     */
    boolean checkStockAvailability(ProductId productId, LocationId locationId, Quantity quantity);

    /**
     * Check if stock at location is expired
     *
     * @param productId Product identifier
     * @param locationId Location identifier
     * @return true if expired, false otherwise
     */
    boolean isStockExpired(ProductId productId, LocationId locationId);
}
```

### Stock Management Service Adapter

**Location:** `services/picking-service/picking-dataaccess/src/main/java/com/ccbsa/wms/picking/dataaccess/adapter/StockManagementServiceAdapter.java`

```java
package com.ccbsa.wms.picking.dataaccess.adapter;

import com.ccbsa.wms.picking.application.service.port.service.StockManagementServicePort;
import com.ccbsa.wms.picking.dataaccess.client.StockManagementServiceClient;
import com.ccbsa.wms.picking.dataaccess.dto.StockAvailabilityResponse;
import com.ccbsa.wms.picking.domain.core.valueobject.LocationId;
import com.ccbsa.wms.picking.domain.core.valueobject.ProductId;
import com.ccbsa.wms.picking.domain.core.valueobject.Quantity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockManagementServiceAdapter implements StockManagementServicePort {

    private final StockManagementServiceClient stockManagementClient;

    @Override
    public boolean checkStockAvailability(ProductId productId, LocationId locationId,
                                          Quantity quantity) {
        log.debug("Checking stock availability for product: {}, location: {}, quantity: {}",
                productId, locationId, quantity);

        try {
            StockAvailabilityResponse response = stockManagementClient.checkAvailability(
                    productId.getValue(),
                    locationId.getValue(),
                    quantity.getValue()
            );

            return response.isAvailable() && response.getAvailableQuantity() >= quantity.getValue();
        } catch (Exception e) {
            log.error("Failed to check stock availability", e);
            // Default to unavailable on error for safety
            return false;
        }
    }

    @Override
    public boolean isStockExpired(ProductId productId, LocationId locationId) {
        log.debug("Checking stock expiration for product: {}, location: {}",
                productId, locationId);

        try {
            StockAvailabilityResponse response = stockManagementClient.checkAvailability(
                    productId.getValue(),
                    locationId.getValue(),
                    1 // Minimal quantity for expiration check
            );

            return response.isExpired();
        } catch (Exception e) {
            log.error("Failed to check stock expiration", e);
            // Default to expired on error for safety
            return true;
        }
    }
}
```

### REST Client for Stock Management Service

**Location:** `services/picking-service/picking-dataaccess/src/main/java/com/ccbsa/wms/picking/dataaccess/client/StockManagementServiceClient.java`

```java
package com.ccbsa.wms.picking.dataaccess.client;

import com.ccbsa.wms.picking.dataaccess.dto.StockAvailabilityResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockManagementServiceClient {

    private final RestTemplate restTemplate;

    @Value("${services.stock-management.url:http://stock-management-service:8080}")
    private String stockManagementServiceUrl;

    @CircuitBreaker(name = "stock-management-service", fallbackMethod = "checkAvailabilityFallback")
    @Retry(name = "stock-management-service")
    public StockAvailabilityResponse checkAvailability(UUID productId, UUID locationId,
                                                       int quantity) {
        String url = String.format("%s/api/v1/stock/stock-items/availability" +
                        "?productId=%s&locationId=%s&quantity=%d",
                stockManagementServiceUrl, productId, locationId, quantity);

        log.debug("Calling stock management service: {}", url);

        return restTemplate.getForObject(url, StockAvailabilityResponse.class);
    }

    private StockAvailabilityResponse checkAvailabilityFallback(UUID productId, UUID locationId,
                                                                int quantity, Exception e) {
        log.error("Fallback triggered for stock availability check. Error: {}", e.getMessage());

        // Return unavailable by default for safety
        return StockAvailabilityResponse.builder()
                .available(false)
                .availableQuantity(0)
                .expired(true)
                .build();
    }
}
```

### Event Publisher

**Location:** `services/picking-service/picking-messaging/src/main/java/com/ccbsa/wms/picking/messaging/publisher/PickingEventPublisherImpl.java`

```java
package com.ccbsa.wms.picking.messaging.publisher;

import com.ccbsa.common.domain.DomainEvent;
import com.ccbsa.wms.picking.application.service.port.messaging.PickingEventPublisher;
import com.ccbsa.wms.picking.domain.core.event.PickingTaskCompletedEvent;
import com.ccbsa.wms.picking.domain.core.event.PartialPickingCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PickingEventPublisherImpl implements PickingEventPublisher {

    private final KafkaTemplate<String, DomainEvent<?>> kafkaTemplate;

    private static final String PICKING_EVENTS_TOPIC = "picking-events";

    @Override
    public void publish(DomainEvent<?> event) {
        log.info("Publishing event: {} to topic: {}", event.getClass().getSimpleName(),
                PICKING_EVENTS_TOPIC);

        try {
            kafkaTemplate.send(PICKING_EVENTS_TOPIC, event.getAggregateId().toString(), event);
            log.debug("Event published successfully: {}", event.getEventId());
        } catch (Exception e) {
            log.error("Failed to publish event: {}", event.getEventId(), e);
            throw new RuntimeException("Failed to publish event", e);
        }
    }
}
```

---

## Testing Strategy

### Unit Tests

#### Domain Core Tests

**Location:** `services/picking-service/picking-domain/picking-domain-core/src/test/java/com/ccbsa/wms/picking/domain/core/PickingTaskTest.java`

```java
package com.ccbsa.wms.picking.domain.core;

import com.ccbsa.wms.picking.domain.core.event.PickingTaskCompletedEvent;
import com.ccbsa.wms.picking.domain.core.event.PartialPickingCompletedEvent;
import com.ccbsa.wms.picking.domain.core.exception.InvalidPickingQuantityException;
import com.ccbsa.wms.picking.domain.core.exception.PickingTaskAlreadyCompletedException;
import com.ccbsa.wms.picking.domain.core.valueobject.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class PickingTaskTest {

    private PickingTask pickingTask;
    private PickingTaskId pickingTaskId;
    private Quantity requiredQuantity;

    @BeforeEach
    void setUp() {
        pickingTaskId = PickingTaskId.of(UUID.randomUUID());
        requiredQuantity = Quantity.of(100);

        pickingTask = PickingTask.builder()
                .pickingTaskId(pickingTaskId)
                .pickingListId(PickingListId.of(UUID.randomUUID()))
                .loadNumber(LoadNumber.of("LOAD-001"))
                .orderNumber(OrderNumber.of("ORD-001"))
                .taskSequence(TaskSequence.of(1))
                .productId(ProductId.of(UUID.randomUUID()))
                .locationId(LocationId.of(UUID.randomUUID()))
                .requiredQuantity(requiredQuantity)
                .build();
    }

    @Test
    void shouldExecutePickingTaskSuccessfully() {
        // Given
        Quantity pickedQuantity = Quantity.of(100);
        String userId = "user123";

        // When
        pickingTask.execute(pickedQuantity, userId);

        // Then
        assertThat(pickingTask.getStatus()).isEqualTo(PickingTaskStatus.COMPLETED);
        assertThat(pickingTask.getPickedQuantity()).isEqualTo(pickedQuantity);
        assertThat(pickingTask.getPickedByUserId()).isEqualTo(userId);
        assertThat(pickingTask.isPartialPicking()).isFalse();
        assertThat(pickingTask.getPickedAt()).isNotNull();
        assertThat(pickingTask.getDomainEvents()).hasSize(1);
        assertThat(pickingTask.getDomainEvents().get(0))
                .isInstanceOf(PickingTaskCompletedEvent.class);
    }

    @Test
    void shouldExecutePartialPickingSuccessfully() {
        // Given
        Quantity pickedQuantity = Quantity.of(50);
        String reason = "Insufficient stock available";
        String userId = "user123";

        // When
        pickingTask.executePartial(pickedQuantity, reason, userId);

        // Then
        assertThat(pickingTask.getStatus()).isEqualTo(PickingTaskStatus.PARTIALLY_COMPLETED);
        assertThat(pickingTask.getPickedQuantity()).isEqualTo(pickedQuantity);
        assertThat(pickingTask.isPartialPicking()).isTrue();
        assertThat(pickingTask.getPartialReason()).isEqualTo(reason);
        assertThat(pickingTask.getPickedByUserId()).isEqualTo(userId);
        assertThat(pickingTask.getDomainEvents()).hasSize(1);
        assertThat(pickingTask.getDomainEvents().get(0))
                .isInstanceOf(PartialPickingCompletedEvent.class);
    }

    @Test
    void shouldDetectPartialPickingWhenQuantityLessThanRequired() {
        // Given
        Quantity pickedQuantity = Quantity.of(80);
        String userId = "user123";

        // When
        pickingTask.execute(pickedQuantity, userId);

        // Then
        assertThat(pickingTask.getStatus()).isEqualTo(PickingTaskStatus.PARTIALLY_COMPLETED);
        assertThat(pickingTask.isPartialPicking()).isTrue();
        assertThat(pickingTask.getDomainEvents().get(0))
                .isInstanceOf(PartialPickingCompletedEvent.class);
    }

    @Test
    void shouldRejectZeroPickedQuantity() {
        // Given
        Quantity pickedQuantity = Quantity.of(0);
        String userId = "user123";

        // When / Then
        assertThatThrownBy(() -> pickingTask.execute(pickedQuantity, userId))
                .isInstanceOf(InvalidPickingQuantityException.class)
                .hasMessageContaining("must be greater than zero");
    }

    @Test
    void shouldRejectPickedQuantityExceedingRequired() {
        // Given
        Quantity pickedQuantity = Quantity.of(150);
        String userId = "user123";

        // When / Then
        assertThatThrownBy(() -> pickingTask.execute(pickedQuantity, userId))
                .isInstanceOf(InvalidPickingQuantityException.class)
                .hasMessageContaining("cannot exceed required quantity");
    }

    @Test
    void shouldRejectExecutionWhenAlreadyCompleted() {
        // Given
        Quantity pickedQuantity = Quantity.of(100);
        String userId = "user123";
        pickingTask.execute(pickedQuantity, userId);

        // When / Then
        assertThatThrownBy(() -> pickingTask.execute(pickedQuantity, userId))
                .isInstanceOf(PickingTaskAlreadyCompletedException.class)
                .hasMessageContaining("already completed");
    }

    @Test
    void shouldRequirePartialReasonForPartialPicking() {
        // Given
        Quantity pickedQuantity = Quantity.of(50);
        String userId = "user123";
        String emptyReason = "";

        // When / Then
        assertThatThrownBy(() -> pickingTask.executePartial(pickedQuantity, emptyReason, userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Partial reason is required");
    }
}
```

#### Command Handler Tests

**Location:** `services/picking-service/picking-domain/picking-application-service/src/test/java/com/ccbsa/wms/picking/application/service/command/ExecutePickingTaskCommandHandlerTest.java`

```java
package com.ccbsa.wms.picking.application.service.command;

import com.ccbsa.wms.picking.application.command.dto.ExecutePickingTaskCommand;
import com.ccbsa.wms.picking.application.command.dto.ExecutePickingTaskResult;
import com.ccbsa.wms.picking.application.service.port.messaging.PickingEventPublisher;
import com.ccbsa.wms.picking.application.service.port.repository.PickingTaskRepository;
import com.ccbsa.wms.picking.application.service.port.service.StockManagementServicePort;
import com.ccbsa.wms.picking.domain.core.PickingTask;
import com.ccbsa.wms.picking.domain.core.exception.ExpiredStockException;
import com.ccbsa.wms.picking.domain.core.exception.InsufficientStockException;
import com.ccbsa.wms.picking.domain.core.valueobject.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExecutePickingTaskCommandHandlerTest {

    @Mock
    private PickingTaskRepository pickingTaskRepository;

    @Mock
    private StockManagementServicePort stockManagementService;

    @Mock
    private PickingEventPublisher eventPublisher;

    @InjectMocks
    private ExecutePickingTaskCommandHandler commandHandler;

    private PickingTask pickingTask;
    private UUID pickingTaskId;

    @BeforeEach
    void setUp() {
        pickingTaskId = UUID.randomUUID();

        pickingTask = PickingTask.builder()
                .pickingTaskId(PickingTaskId.of(pickingTaskId))
                .pickingListId(PickingListId.of(UUID.randomUUID()))
                .loadNumber(LoadNumber.of("LOAD-001"))
                .orderNumber(OrderNumber.of("ORD-001"))
                .taskSequence(TaskSequence.of(1))
                .productId(ProductId.of(UUID.randomUUID()))
                .locationId(LocationId.of(UUID.randomUUID()))
                .requiredQuantity(Quantity.of(100))
                .build();
    }

    @Test
    void shouldExecutePickingTaskSuccessfully() {
        // Given
        ExecutePickingTaskCommand command = ExecutePickingTaskCommand.builder()
                .pickingTaskId(pickingTaskId)
                .pickedQuantity(100)
                .isPartialPicking(false)
                .pickedByUserId("user123")
                .build();

        when(pickingTaskRepository.findById(any())).thenReturn(Optional.of(pickingTask));
        when(stockManagementService.checkStockAvailability(any(), any(), any())).thenReturn(true);
        when(stockManagementService.isStockExpired(any(), any())).thenReturn(false);

        // When
        ExecutePickingTaskResult result = commandHandler.handle(command);

        // Then
        assertThat(result.getPickingTaskId()).isEqualTo(pickingTaskId);
        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        assertThat(result.getPickedQuantity()).isEqualTo(100);
        assertThat(result.isPartialPicking()).isFalse();

        verify(pickingTaskRepository).save(pickingTask);
        verify(eventPublisher, atLeastOnce()).publish(any());
    }

    @Test
    void shouldRejectPickingExpiredStock() {
        // Given
        ExecutePickingTaskCommand command = ExecutePickingTaskCommand.builder()
                .pickingTaskId(pickingTaskId)
                .pickedQuantity(100)
                .isPartialPicking(false)
                .pickedByUserId("user123")
                .build();

        when(pickingTaskRepository.findById(any())).thenReturn(Optional.of(pickingTask));
        when(stockManagementService.checkStockAvailability(any(), any(), any())).thenReturn(true);
        when(stockManagementService.isStockExpired(any(), any())).thenReturn(true);

        // When / Then
        assertThatThrownBy(() -> commandHandler.handle(command))
                .isInstanceOf(ExpiredStockException.class)
                .hasMessageContaining("Cannot pick expired stock");

        verify(pickingTaskRepository, never()).save(any());
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void shouldRejectPickingInsufficientStock() {
        // Given
        ExecutePickingTaskCommand command = ExecutePickingTaskCommand.builder()
                .pickingTaskId(pickingTaskId)
                .pickedQuantity(100)
                .isPartialPicking(false)
                .pickedByUserId("user123")
                .build();

        when(pickingTaskRepository.findById(any())).thenReturn(Optional.of(pickingTask));
        when(stockManagementService.checkStockAvailability(any(), any(), any())).thenReturn(false);

        // When / Then
        assertThatThrownBy(() -> commandHandler.handle(command))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("Insufficient stock");

        verify(pickingTaskRepository, never()).save(any());
        verify(eventPublisher, never()).publish(any());
    }
}
```

### Integration Tests

**Location:** `gateway-api-tests/src/test/java/com/ccbsa/wms/gateway/api/PickingTaskExecutionTest.java`

```java
package com.ccbsa.wms.gateway.api;

import com.ccbsa.wms.gateway.api.dto.ExecutePickingTaskRequest;
import com.ccbsa.wms.gateway.api.dto.ExecutePickingTaskResponse;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

class PickingTaskExecutionTest extends BaseIntegrationTest {

    private static final String PICKING_TASKS_ENDPOINT = "/api/v1/picking/picking-tasks";

    @Test
    void shouldExecutePickingTaskSuccessfully() {
        // Given: A picking task exists (created in setup)
        UUID pickingTaskId = createPickingTask();

        ExecutePickingTaskRequest request = ExecutePickingTaskRequest.builder()
                .pickedQuantity(100)
                .isPartialPicking(false)
                .build();

        // When: Execute picking task
        given()
                .spec(requestSpec)
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post(PICKING_TASKS_ENDPOINT + "/{pickingTaskId}/execute", pickingTaskId)
                .then()
                .statusCode(200)
                .body("success", is(true))
                .body("data.pickingTaskId", equalTo(pickingTaskId.toString()))
                .body("data.status", equalTo("COMPLETED"))
                .body("data.pickedQuantity", equalTo(100))
                .body("data.isPartialPicking", is(false));
    }

    @Test
    void shouldExecutePartialPickingSuccessfully() {
        // Given: A picking task exists
        UUID pickingTaskId = createPickingTask();

        ExecutePickingTaskRequest request = ExecutePickingTaskRequest.builder()
                .pickedQuantity(50)
                .isPartialPicking(true)
                .partialReason("Insufficient stock available")
                .build();

        // When: Execute partial picking
        given()
                .spec(requestSpec)
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post(PICKING_TASKS_ENDPOINT + "/{pickingTaskId}/execute", pickingTaskId)
                .then()
                .statusCode(200)
                .body("success", is(true))
                .body("data.status", equalTo("PARTIALLY_COMPLETED"))
                .body("data.pickedQuantity", equalTo(50))
                .body("data.isPartialPicking", is(true));
    }

    @Test
    void shouldRejectExecutingExpiredStock() {
        // Given: A picking task with expired stock
        UUID pickingTaskId = createPickingTaskWithExpiredStock();

        ExecutePickingTaskRequest request = ExecutePickingTaskRequest.builder()
                .pickedQuantity(100)
                .isPartialPicking(false)
                .build();

        // When: Attempt to execute picking
        given()
                .spec(requestSpec)
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post(PICKING_TASKS_ENDPOINT + "/{pickingTaskId}/execute", pickingTaskId)
                .then()
                .statusCode(400)
                .body("success", is(false))
                .body("error.code", equalTo("EXPIRED_STOCK"))
                .body("error.message", containsString("Cannot pick expired stock"));
    }

    @Test
    void shouldRejectInvalidPickedQuantity() {
        // Given: A picking task exists
        UUID pickingTaskId = createPickingTask();

        ExecutePickingTaskRequest request = ExecutePickingTaskRequest.builder()
                .pickedQuantity(0) // Invalid: zero quantity
                .isPartialPicking(false)
                .build();

        // When: Attempt to execute picking
        given()
                .spec(requestSpec)
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post(PICKING_TASKS_ENDPOINT + "/{pickingTaskId}/execute", pickingTaskId)
                .then()
                .statusCode(400)
                .body("success", is(false))
                .body("error.message", containsString("Picked quantity must be at least 1"));
    }

    private UUID createPickingTask() {
        // Implementation to create test picking task
        // This would call the picking list creation endpoints
        return UUID.randomUUID();
    }

    private UUID createPickingTaskWithExpiredStock() {
        // Implementation to create picking task with expired stock
        return UUID.randomUUID();
    }
}
```

---

## Implementation Checklist

### Frontend Implementation
- [ ] Create `PickingTaskExecutionPage` component
- [ ] Create `PickingTaskDetail` component
- [ ] Create `LocationDisplay` component
- [ ] Create `usePickingTask` hook
- [ ] Create `usePickingTaskExecution` hook
- [ ] Add TypeScript types for picking task and location
- [ ] Add routing for picking task execution page
- [ ] Implement client-side validation for picked quantity
- [ ] Add loading and error states
- [ ] Test UI components with mock data

### Backend - Domain Core
- [ ] Create `PickingTask` aggregate with `execute()` and `executePartial()` methods
- [ ] Create `PickingTaskStatus` enum
- [ ] Create `TaskSequence` value object
- [ ] Create `PickingTaskCompletedEvent` domain event
- [ ] Create `PartialPickingCompletedEvent` domain event
- [ ] Create domain exceptions (InvalidPickingQuantityException, etc.)
- [ ] Write unit tests for `PickingTask` aggregate
- [ ] Verify business rules enforcement
- [ ] Test event publishing

### Backend - Application Service
- [ ] Create `ExecutePickingTaskCommand`
- [ ] Create `ExecutePickingTaskResult`
- [ ] Create `ExecutePickingTaskCommandHandler`
- [ ] Add stock availability validation
- [ ] Add stock expiration validation
- [ ] Write unit tests for command handler
- [ ] Test error scenarios

### Backend - Application Layer
- [ ] Create `PickingTaskCommandController`
- [ ] Create `ExecutePickingTaskRequest` DTO
- [ ] Add endpoint: `POST /api/v1/picking/picking-tasks/{id}/execute`
- [ ] Add authentication and authorization
- [ ] Add request validation
- [ ] Add exception handling
- [ ] Document API with OpenAPI annotations

### Backend - Data Access
- [ ] Create `PickingTaskEntity` JPA entity
- [ ] Create `PickingTaskJpaRepository`
- [ ] Create `PickingTaskEntityMapper`
- [ ] Create `PickingTaskRepositoryAdapter`
- [ ] Add database migration script
- [ ] Test repository operations

### Backend - Service Integration
- [ ] Create `StockManagementServicePort` interface
- [ ] Create `StockManagementServiceAdapter`
- [ ] Create `StockManagementServiceClient` REST client
- [ ] Add circuit breaker configuration
- [ ] Add retry configuration
- [ ] Test service integration with mocks

### Backend - Messaging
- [ ] Create `PickingEventPublisher` implementation
- [ ] Configure Kafka producer for picking events
- [ ] Test event publishing to Kafka
- [ ] Verify event serialization

### Event Consumers (Stock Management Service)
- [ ] Create event listener for `PickingTaskCompletedEvent`
- [ ] Update stock levels when picking completed
- [ ] Create event listener for `PartialPickingCompletedEvent`
- [ ] Test event consumption and stock updates

### Event Consumers (Location Management Service)
- [ ] Create event listener for `PickingTaskCompletedEvent`
- [ ] Create stock movement record
- [ ] Update location status
- [ ] Test event consumption and movement tracking

### Gateway Configuration
- [ ] Add route for picking task execution endpoint
- [ ] Configure rate limiting
- [ ] Add request/response logging
- [ ] Test gateway routing

### Integration Testing
- [ ] Write gateway API test for successful execution
- [ ] Write gateway API test for partial picking
- [ ] Write gateway API test for expired stock rejection
- [ ] Write gateway API test for insufficient stock rejection
- [ ] Write gateway API test for validation errors
- [ ] Test end-to-end flow from UI to database

### Documentation
- [ ] Update API documentation (OpenAPI spec)
- [ ] Document execution workflow
- [ ] Document error codes and messages
- [ ] Create user guide for picking execution
- [ ] Update architecture documentation

### Deployment
- [ ] Update database schema (Flyway migration)
- [ ] Configure environment variables
- [ ] Deploy picking service
- [ ] Deploy frontend
- [ ] Verify health checks
- [ ] Monitor logs for errors

---

## Definition of Done

- [ ] All acceptance criteria met and verified
- [ ] Frontend components implemented and tested
- [ ] Backend services implemented with Clean Hexagonal Architecture
- [ ] Domain logic implemented in domain-core (pure Java)
- [ ] CQRS separation maintained (commands vs queries)
- [ ] Events published for all state changes
- [ ] Unit tests pass (>80% coverage)
- [ ] Integration tests pass (gateway API tests)
- [ ] Service integration tested (stock management, location management)
- [ ] API documented with OpenAPI
- [ ] Code reviewed and approved
- [ ] Manual testing completed
- [ ] Performance testing completed
- [ ] Security testing completed
- [ ] User acceptance testing passed
- [ ] Documentation updated
- [ ] Deployed to staging environment
- [ ] Product Owner approval

---

**Document Version:** 1.0  
**Last Updated:** 2026-01-08  
**Author:** System Architect  
**Status:** Ready for Implementation
