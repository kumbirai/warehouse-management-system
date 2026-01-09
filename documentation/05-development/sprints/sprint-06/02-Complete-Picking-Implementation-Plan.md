# US-6.3.2: Complete Picking Implementation Plan

## User Story

**Story ID:** US-6.3.2
**Title:** Complete Picking
**Epic:** Picking List Management
**Service:** Picking Service
**Priority:** Must Have
**Story Points:** 5

**As a** warehouse operator
**I want** to complete picking operations
**So that** picked stock is ready for shipping and returns processing

---

## Acceptance Criteria

- [ ] AC1: System allows completing picking when all tasks are completed
- [ ] AC2: System validates all picking tasks are completed or partial
- [ ] AC3: System updates picking list status to "COMPLETED"
- [ ] AC4: System publishes `PickingCompletedEvent`
- [ ] AC5: System updates order status within load
- [ ] AC6: System prevents completion if tasks are still pending

---

## Table of Contents

1. [Overview](#overview)
2. [Frontend Implementation](#frontend-implementation)
3. [Backend Implementation](#backend-implementation)
4. [Domain Model](#domain-model)
5. [API Specification](#api-specification)
6. [Event Design](#event-design)
7. [Testing Strategy](#testing-strategy)
8. [Implementation Checklist](#implementation-checklist)

---

## Overview

### Business Context

Completing picking operations marks the transition from warehouse operations to shipping/returns processing. The system must:

1. **Validate Completion** - Ensure all picking tasks are executed
2. **Update Status** - Mark picking list as completed
3. **Trigger Events** - Notify downstream services (Returns, Integration)
4. **Track Completion** - Record completion timestamp and user
5. **Support Partial Loads** - Handle scenarios where some orders are partially picked

### Technical Approach

- **Frontend-First:** Build intuitive completion UI with validation
- **Status Validation:** Prevent completion if tasks still pending
- **Event-Driven:** Trigger downstream processing via events
- **CQRS:** Separate command (complete) from query (status check)
- **Audit Trail:** Complete tracking of completion details

---

## Frontend Implementation

### UI Component: PickingListCompletionPage

**Location:** `frontend-app/src/features/picking/pages/PickingListCompletionPage.tsx`

```typescript
import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { usePickingList } from '../hooks/usePickingList';
import { useCompletePickingList } from '../hooks/useCompletePickingList';
import { PickingTaskList } from '../components/PickingTaskList';
import { PickingListSummary } from '../components/PickingListSummary';
import { Alert } from '../../common/Alert';
import { Button } from '../../common/Button';
import { LoadingSpinner } from '../../common/LoadingSpinner';
import { StatusBadge } from '../../common/StatusBadge';

export const PickingListCompletionPage: React.FC = () => {
  const { pickingListId } = useParams<{ pickingListId: string }>();
  const navigate = useNavigate();

  const {
    pickingList,
    loading: listLoading,
    error: listError
  } = usePickingList(pickingListId);

  const {
    completePickingList,
    loading: completionLoading,
    error: completionError
  } = useCompletePickingList();

  const [canComplete, setCanComplete] = useState<boolean>(false);
  const [validationMessage, setValidationMessage] = useState<string>('');

  useEffect(() => {
    if (!pickingList) return;

    // Check if all tasks are completed or partially completed
    const allTasksProcessed = pickingList.pickingTasks.every(
      task => task.status === 'COMPLETED' || task.status === 'PARTIALLY_COMPLETED'
    );

    const hasPendingTasks = pickingList.pickingTasks.some(
      task => task.status === 'PENDING'
    );

    if (allTasksProcessed && !hasPendingTasks) {
      setCanComplete(true);
      setValidationMessage('');
    } else {
      setCanComplete(false);
      const pendingCount = pickingList.pickingTasks.filter(
        task => task.status === 'PENDING'
      ).length;
      setValidationMessage(`Cannot complete: ${pendingCount} task(s) still pending`);
    }
  }, [pickingList]);

  const handleCompletePicking = async () => {
    if (!pickingListId || !pickingList) return;

    try {
      await completePickingList(pickingListId);
      navigate(`/picking/picking-lists/${pickingListId}/summary`);
    } catch (error) {
      console.error('Failed to complete picking list:', error);
    }
  };

  if (listLoading) {
    return (
      <div className="flex justify-center items-center h-screen">
        <LoadingSpinner size="large" />
      </div>
    );
  }

  if (listError || !pickingList) {
    return (
      <div className="p-6">
        <Alert type="error" message={listError || 'Picking list not found'} />
        <Button onClick={() => navigate('/picking/picking-lists')} className="mt-4">
          Back to Picking Lists
        </Button>
      </div>
    );
  }

  return (
    <div className="container mx-auto p-6 max-w-6xl">
      <div className="mb-6">
        <h1 className="text-3xl font-bold text-gray-900">Complete Picking</h1>
        <p className="text-gray-600 mt-2">
          Picking List: {pickingList.pickingListReference}
        </p>
      </div>

      {completionError && (
        <Alert type="error" message={completionError} className="mb-6" />
      )}

      {!canComplete && validationMessage && (
        <Alert type="warning" message={validationMessage} className="mb-6" />
      )}

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2">
          <div className="bg-white rounded-lg shadow-md p-6">
            <h2 className="text-xl font-semibold mb-4">Picking Tasks</h2>
            <PickingTaskList tasks={pickingList.pickingTasks} showStatus={true} />
          </div>
        </div>

        <div className="lg:col-span-1">
          <div className="bg-white rounded-lg shadow-md p-6 sticky top-6">
            <h2 className="text-xl font-semibold mb-4">Summary</h2>
            <PickingListSummary pickingList={pickingList} />

            <div className="mt-6 pt-6 border-t">
              <Button
                variant="primary"
                onClick={handleCompletePicking}
                disabled={!canComplete || completionLoading}
                loading={completionLoading}
                className="w-full"
              >
                Complete Picking
              </Button>
              <Button
                variant="secondary"
                onClick={() => navigate(`/picking/picking-lists/${pickingListId}`)}
                className="w-full mt-3"
              >
                Cancel
              </Button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
```

### Custom Hook: useCompletePickingList

```typescript
import { useState } from 'react';
import { apiClient } from '../../../services/apiClient';

interface CompletePickingListResponse {
  success: boolean;
  message: string;
  pickingListId: string;
  status: string;
  completedAt: string;
}

export const useCompletePickingList = () => {
  const [loading, setLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  const completePickingList = async (pickingListId: string): Promise<void> => {
    try {
      setLoading(true);
      setError(null);

      const response = await apiClient.post<CompletePickingListResponse>(
        `/api/v1/picking/picking-lists/${pickingListId}/complete`
      );

      if (!response.data.success) {
        throw new Error(response.data.message || 'Failed to complete picking list');
      }
    } catch (err: any) {
      console.error('Failed to complete picking list:', err);
      const errorMessage = err.response?.data?.message ||
                          err.message ||
                          'Failed to complete picking list';
      setError(errorMessage);
      throw new Error(errorMessage);
    } finally {
      setLoading(false);
    }
  };

  return { completePickingList, loading, error };
};
```

### Supporting Component: PickingListSummary

```typescript
import React from 'react';
import { PickingList } from '../types/pickingList';

interface PickingListSummaryProps {
  pickingList: PickingList;
}

export const PickingListSummary: React.FC<PickingListSummaryProps> = ({
  pickingList
}) => {
  const totalTasks = pickingList.pickingTasks.length;
  const completedTasks = pickingList.pickingTasks.filter(
    t => t.status === 'COMPLETED'
  ).length;
  const partialTasks = pickingList.pickingTasks.filter(
    t => t.status === 'PARTIALLY_COMPLETED'
  ).length;
  const pendingTasks = pickingList.pickingTasks.filter(
    t => t.status === 'PENDING'
  ).length;

  const completionPercentage = Math.round(
    ((completedTasks + partialTasks) / totalTasks) * 100
  );

  return (
    <div className="space-y-4">
      <div>
        <div className="flex justify-between items-center mb-2">
          <span className="text-sm text-gray-600">Progress</span>
          <span className="text-sm font-semibold">{completionPercentage}%</span>
        </div>
        <div className="w-full bg-gray-200 rounded-full h-2">
          <div
            className="bg-blue-600 h-2 rounded-full transition-all duration-300"
            style={{ width: `${completionPercentage}%` }}
          />
        </div>
      </div>

      <div className="space-y-2">
        <div className="flex justify-between">
          <span className="text-sm text-gray-600">Total Tasks:</span>
          <span className="text-sm font-semibold">{totalTasks}</span>
        </div>
        <div className="flex justify-between">
          <span className="text-sm text-green-600">Completed:</span>
          <span className="text-sm font-semibold text-green-600">{completedTasks}</span>
        </div>
        <div className="flex justify-between">
          <span className="text-sm text-yellow-600">Partial:</span>
          <span className="text-sm font-semibold text-yellow-600">{partialTasks}</span>
        </div>
        <div className="flex justify-between">
          <span className="text-sm text-gray-600">Pending:</span>
          <span className="text-sm font-semibold text-gray-600">{pendingTasks}</span>
        </div>
      </div>

      <div className="pt-4 border-t">
        <div className="flex justify-between">
          <span className="text-sm text-gray-600">Load Number:</span>
          <span className="text-sm font-semibold">{pickingList.loadNumber}</span>
        </div>
        <div className="flex justify-between mt-2">
          <span className="text-sm text-gray-600">Orders:</span>
          <span className="text-sm font-semibold">{pickingList.orderCount}</span>
        </div>
      </div>
    </div>
  );
};
```

---

## Backend Implementation

### Command: CompletePickingListCommand

**Location:** `picking-application/src/main/java/com/ccbsa/wms/picking/application/command/dto/CompletePickingListCommand.java`

```java
package com.ccbsa.wms.picking.application.command.dto;

import lombok.Builder;
import lombok.Value;

import java.util.UUID;

@Value
@Builder
public class CompletePickingListCommand {
    UUID pickingListId;
    String completedByUserId;
}
```

### Command Handler

**Location:** `picking-application-service/src/main/java/com/ccbsa/wms/picking/application/service/command/CompletePickingListCommandHandler.java`

```java
package com.ccbsa.wms.picking.application.service.command;

import com.ccbsa.common.domain.DomainEvent;
import com.ccbsa.wms.picking.application.command.dto.CompletePickingListCommand;
import com.ccbsa.wms.picking.application.command.dto.CompletePickingListResult;
import com.ccbsa.wms.picking.application.service.port.messaging.PickingEventPublisher;
import com.ccbsa.wms.picking.application.service.port.repository.PickingListRepository;
import com.ccbsa.wms.picking.application.service.port.repository.PickingTaskRepository;
import com.ccbsa.wms.picking.domain.core.PickingList;
import com.ccbsa.wms.picking.domain.core.PickingTask;
import com.ccbsa.wms.picking.domain.core.exception.PickingListNotFoundException;
import com.ccbsa.wms.picking.domain.core.exception.PickingNotCompleteException;
import com.ccbsa.wms.picking.domain.core.valueobject.PickingListId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CompletePickingListCommandHandler {

    private final PickingListRepository pickingListRepository;
    private final PickingTaskRepository pickingTaskRepository;
    private final PickingEventPublisher eventPublisher;

    @Transactional
    public CompletePickingListResult handle(CompletePickingListCommand command) {
        log.info("Completing picking list: {}", command.getPickingListId());

        // 1. Retrieve picking list
        PickingListId pickingListId = PickingListId.of(command.getPickingListId());
        PickingList pickingList = pickingListRepository.findById(pickingListId)
                .orElseThrow(() -> new PickingListNotFoundException(
                        "Picking list not found: " + command.getPickingListId()));

        // 2. Retrieve all picking tasks
        List<PickingTask> pickingTasks = pickingTaskRepository
                .findByPickingListId(pickingListId);

        // 3. Validate all tasks are completed or partially completed
        validateAllTasksProcessed(pickingTasks);

        // 4. Complete picking list
        pickingList.complete(command.getCompletedByUserId());

        // 5. Save picking list
        pickingListRepository.save(pickingList);

        // 6. Publish domain events
        List<DomainEvent<?>> events = pickingList.getDomainEvents();
        events.forEach(eventPublisher::publish);
        pickingList.clearDomainEvents();

        log.info("Picking list completed successfully: {}", pickingListId);

        // 7. Return result
        return CompletePickingListResult.builder()
                .pickingListId(command.getPickingListId())
                .status(pickingList.getStatus().name())
                .completedAt(pickingList.getCompletedAt())
                .build();
    }

    private void validateAllTasksProcessed(List<PickingTask> pickingTasks) {
        long pendingTasks = pickingTasks.stream()
                .filter(task -> !task.isCompleted() && !task.isPartiallyCompleted())
                .count();

        if (pendingTasks > 0) {
            throw new PickingNotCompleteException(
                    String.format("Cannot complete picking: %d task(s) still pending",
                            pendingTasks));
        }

        if (pickingTasks.isEmpty()) {
            throw new PickingNotCompleteException(
                    "Cannot complete picking: No picking tasks found");
        }
    }
}
```

### REST Controller

```java
package com.ccbsa.wms.picking.application.command;

import com.ccbsa.common.application.dto.ApiResponse;
import com.ccbsa.common.application.dto.ApiResponseBuilder;
import com.ccbsa.wms.picking.application.command.dto.CompletePickingListCommand;
import com.ccbsa.wms.picking.application.command.dto.CompletePickingListResult;
import com.ccbsa.wms.picking.application.service.command.CompletePickingListCommandHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/picking/picking-lists")
@RequiredArgsConstructor
public class PickingListCommandController {

    private final CompletePickingListCommandHandler completePickingListCommandHandler;

    @PostMapping("/{pickingListId}/complete")
    public ResponseEntity<ApiResponse<CompletePickingListResult>> completePickingList(
            @PathVariable UUID pickingListId,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("Completing picking list: {} by user: {}",
                pickingListId, userDetails.getUsername());

        CompletePickingListCommand command = CompletePickingListCommand.builder()
                .pickingListId(pickingListId)
                .completedByUserId(userDetails.getUsername())
                .build();

        CompletePickingListResult result = completePickingListCommandHandler.handle(command);

        return ResponseEntity.ok(ApiResponseBuilder.ok(result));
    }
}
```

---

## Domain Model

### PickingList Aggregate - Complete Method

**Location:** `picking-domain-core/src/main/java/com/ccbsa/wms/picking/domain/core/PickingList.java`

```java
package com.ccbsa.wms.picking.domain.core;

import com.ccbsa.common.domain.AggregateRoot;
import com.ccbsa.wms.picking.domain.core.event.PickingCompletedEvent;
import com.ccbsa.wms.picking.domain.core.exception.PickingListAlreadyCompletedException;
import com.ccbsa.wms.picking.domain.core.valueobject.*;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
public class PickingList extends AggregateRoot<PickingListId> {

    private PickingListReference pickingListReference;
    private LoadNumber loadNumber;
    private List<OrderNumber> orderNumbers;
    private PickingListStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private String completedByUserId;

    /**
     * Complete the picking list
     */
    public void complete(String completedByUserId) {
        validateCompletionPreconditions();

        this.status = PickingListStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
        this.completedByUserId = completedByUserId;

        addDomainEvent(new PickingCompletedEvent(
                this.id,
                this.pickingListReference,
                this.loadNumber,
                this.orderNumbers,
                this.completedByUserId
        ));
    }

    private void validateCompletionPreconditions() {
        if (this.status == PickingListStatus.COMPLETED) {
            throw new PickingListAlreadyCompletedException(
                    "Picking list already completed: " + this.id);
        }
    }

    public boolean isCompleted() {
        return this.status == PickingListStatus.COMPLETED;
    }
}
```

### PickingListStatus Enum

```java
package com.ccbsa.wms.picking.domain.core.valueobject;

public enum PickingListStatus {
    RECEIVED("Received"),
    PLANNED("Planned"),
    IN_PROGRESS("In Progress"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled");

    private final String displayName;

    PickingListStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
```

---

## Event Design

### PickingCompletedEvent

**Location:** `picking-domain-core/src/main/java/com/ccbsa/wms/picking/domain/core/event/PickingCompletedEvent.java`

```java
package com.ccbsa.wms.picking.domain.core.event;

import com.ccbsa.wms.picking.domain.core.PickingList;
import com.ccbsa.wms.picking.domain.core.valueobject.*;
import lombok.Getter;

import java.util.List;

@Getter
public class PickingCompletedEvent extends PickingEvent<PickingList> {

    private final PickingListReference pickingListReference;
    private final LoadNumber loadNumber;
    private final List<OrderNumber> orderNumbers;
    private final String completedByUserId;

    public PickingCompletedEvent(
            PickingListId pickingListId,
            PickingListReference pickingListReference,
            LoadNumber loadNumber,
            List<OrderNumber> orderNumbers,
            String completedByUserId) {
        super(pickingListId);
        this.pickingListReference = pickingListReference;
        this.loadNumber = loadNumber;
        this.orderNumbers = orderNumbers;
        this.completedByUserId = completedByUserId;
    }
}
```

---

## API Specification

### Complete Picking List Endpoint

**Endpoint:** `POST /api/v1/picking/picking-lists/{pickingListId}/complete`

**Request:** No body required

**Response (Success):**

```json
{
  "success": true,
  "data": {
    "pickingListId": "123e4567-e89b-12d3-a456-426614174000",
    "status": "COMPLETED",
    "completedAt": "2026-01-08T14:30:00Z"
  },
  "timestamp": "2026-01-08T14:30:00Z"
}
```

**Response (Error - Pending Tasks):**

```json
{
  "success": false,
  "error": {
    "code": "PICKING_NOT_COMPLETE",
    "message": "Cannot complete picking: 3 task(s) still pending",
    "details": {
      "pickingListId": "123e4567-e89b-12d3-a456-426614174000",
      "pendingTaskCount": 3
    }
  },
  "timestamp": "2026-01-08T14:30:00Z"
}
```

---

## Testing Strategy

### Unit Tests - Domain Core

```java
@Test
void shouldCompletePickingListSuccessfully() {
    // Given
    PickingList pickingList = createPickingList();
    String userId = "user123";

    // When
    pickingList.complete(userId);

    // Then
    assertThat(pickingList.getStatus()).isEqualTo(PickingListStatus.COMPLETED);
    assertThat(pickingList.getCompletedByUserId()).isEqualTo(userId);
    assertThat(pickingList.getCompletedAt()).isNotNull();
    assertThat(pickingList.getDomainEvents()).hasSize(1);
    assertThat(pickingList.getDomainEvents().get(0))
            .isInstanceOf(PickingCompletedEvent.class);
}

@Test
void shouldRejectCompletionWhenAlreadyCompleted() {
    // Given
    PickingList pickingList = createPickingList();
    pickingList.complete("user123");

    // When / Then
    assertThatThrownBy(() -> pickingList.complete("user123"))
            .isInstanceOf(PickingListAlreadyCompletedException.class);
}
```

### Integration Tests - Gateway API

```java
@Test
void shouldCompletePickingListSuccessfully() {
    // Given: A picking list with all tasks completed
    UUID pickingListId = createPickingListWithAllTasksCompleted();

    // When: Complete picking list
    given()
            .spec(requestSpec)
            .when()
            .post("/api/v1/picking/picking-lists/{pickingListId}/complete",
                    pickingListId)
            .then()
            .statusCode(200)
            .body("success", is(true))
            .body("data.status", equalTo("COMPLETED"))
            .body("data.completedAt", notNullValue());
}

@Test
void shouldRejectCompletionWithPendingTasks() {
    // Given: A picking list with pending tasks
    UUID pickingListId = createPickingListWithPendingTasks();

    // When: Attempt to complete
    given()
            .spec(requestSpec)
            .when()
            .post("/api/v1/picking/picking-lists/{pickingListId}/complete",
                    pickingListId)
            .then()
            .statusCode(400)
            .body("success", is(false))
            .body("error.code", equalTo("PICKING_NOT_COMPLETE"));
}
```

---

## Implementation Checklist

### Frontend

- [ ] Create `PickingListCompletionPage` component
- [ ] Create `PickingListSummary` component
- [ ] Create `useCompletePickingList` hook
- [ ] Add validation for task completion status
- [ ] Add routing for completion page
- [ ] Test UI with mock data

### Backend - Domain Core

- [ ] Add `complete()` method to `PickingList` aggregate
- [ ] Create `PickingListStatus` enum
- [ ] Create `PickingCompletedEvent` domain event
- [ ] Create `PickingListAlreadyCompletedException`
- [ ] Create `PickingNotCompleteException`
- [ ] Write unit tests

### Backend - Application Service

- [ ] Create `CompletePickingListCommand`
- [ ] Create `CompletePickingListCommandHandler`
- [ ] Add task completion validation
- [ ] Write unit tests

### Backend - Application Layer

- [ ] Create/update `PickingListCommandController`
- [ ] Add endpoint: `POST /api/v1/picking/picking-lists/{id}/complete`
- [ ] Add authentication
- [ ] Add exception handling

### Backend - Data Access

- [ ] Update `PickingListEntity` with completion fields
- [ ] Update `PickingListEntityMapper`
- [ ] Add database migration script

### Event Consumers (Returns Service)

- [ ] Create listener for `PickingCompletedEvent`
- [ ] Prepare for potential returns processing

### Gateway API Tests

- [ ] Test successful completion
- [ ] Test rejection with pending tasks
- [ ] Test rejection when already completed

### Documentation

- [ ] Update API documentation
- [ ] Document completion workflow
- [ ] Create user guide

---

**Document Version:** 1.0  
**Last Updated:** 2026-01-08  
**Status:** Ready for Implementation
