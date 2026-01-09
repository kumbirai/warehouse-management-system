# US-5.1.3: Generate Restock Request Implementation Plan

## User Story

**Story ID:** US-5.1.3
**Title:** Generate Restock Request
**Epic:** Stock Level Management
**Service:** Stock Management Service, Integration Service (optional)
**Priority:** Must Have
**Story Points:** 5

**As a** warehouse manager
**I want** the system to automatically generate restock requests when stock falls below minimum
**So that** inventory levels are maintained and stockouts are prevented

---

## Acceptance Criteria

- [ ] AC1: System automatically generates restock request when stock falls below minimum
- [ ] AC2: Restock request includes: product code, current quantity, minimum quantity, requested quantity, priority
- [ ] AC3: Request is sent to D365 for processing (if D365 integration enabled)
- [ ] AC4: System tracks restock request status
- [ ] AC5: System prevents duplicate restock requests for same product
- [ ] AC6: Warehouse managers can view and manage restock requests
- [ ] AC7: System calculates requested quantity intelligently (max - current)

---

## Table of Contents

1. [Overview](#overview)
2. [Frontend Implementation](#frontend-implementation)
3. [Backend Implementation](#backend-implementation)
4. [Domain Model](#domain-model)
5. [Event Design](#event-design)
6. [D365 Integration](#d365-integration)
7. [Testing Strategy](#testing-strategy)
8. [Implementation Checklist](#implementation-checklist)

---

## Overview

### Business Context

Automated restock requests ensure:

1. **Prevent Stockouts** - Maintain adequate inventory levels
2. **Optimize Inventory** - Balance minimum/maximum stock levels
3. **Reduce Manual Work** - Eliminate manual monitoring and requests
4. **Timely Replenishment** - Proactive rather than reactive restocking
5. **D365 Integration** - Seamless integration with ERP system

### Restock Logic

- **Trigger:** Stock level falls below minimum threshold
- **Calculation:** Requested quantity = Maximum - Current quantity
- **Priority:**
  - HIGH: Stock below 50% of minimum
  - MEDIUM: Stock between 50-80% of minimum
  - LOW: Stock between 80-100% of minimum
- **Deduplication:** Only one active restock request per product

---

## Frontend Implementation

### UI Component: RestockRequestsDashboard

**Location:** `frontend-app/src/features/stock-management/pages/RestockRequestsDashboard.tsx`

```typescript
import React, { useState } from 'react';
import { useRestockRequests } from '../hooks/useRestockRequests';
import { RestockRequestList } from '../components/RestockRequestList';
import { RestockRequestFilters } from '../components/RestockRequestFilters';
import { Alert } from '../../common/Alert';
import { Button } from '../../common/Button';
import { LoadingSpinner } from '../../common/LoadingSpinner';

export const RestockRequestsDashboard: React.FC = () => {
  const [filters, setFilters] = useState({
    status: 'PENDING',
    priority: 'ALL'
  });

  const {
    restockRequests,
    loading,
    error,
    refetch
  } = useRestockRequests(filters);

  if (loading) {
    return (
      <div className="flex justify-center items-center h-screen">
        <LoadingSpinner size="large" />
      </div>
    );
  }

  if (error) {
    return (
      <div className="p-6">
        <Alert type="error" message={error} />
      </div>
    );
  }

  const highPriorityCount = restockRequests?.filter(
    r => r.priority === 'HIGH' && r.status === 'PENDING'
  ).length || 0;

  return (
    <div className="container mx-auto p-6">
      <div className="mb-6">
        <h1 className="text-3xl font-bold text-gray-900">Restock Requests</h1>
        <p className="text-gray-600 mt-2">
          Monitor and manage automatic inventory replenishment requests
        </p>
      </div>

      {highPriorityCount > 0 && (
        <Alert
          type="warning"
          message={`${highPriorityCount} high-priority restock request(s) require attention`}
          className="mb-6"
        />
      )}

      <div className="bg-white rounded-lg shadow-md p-6 mb-6">
        <RestockRequestFilters
          filters={filters}
          onChange={setFilters}
          onRefresh={refetch}
        />
      </div>

      <div className="bg-white rounded-lg shadow-md p-6">
        <h2 className="text-xl font-semibold mb-4">Restock Requests</h2>

        {restockRequests && restockRequests.length > 0 ? (
          <RestockRequestList requests={restockRequests} />
        ) : (
          <div className="text-center py-12">
            <p className="text-gray-500 text-lg">
              No restock requests found for the selected criteria
            </p>
          </div>
        )}
      </div>
    </div>
  );
};
```

### Supporting Component: RestockRequestList

```typescript
import React from 'react';
import { RestockRequest } from '../types/restockRequest';

interface RestockRequestListProps {
  requests: RestockRequest[];
}

export const RestockRequestList: React.FC<RestockRequestListProps> = ({
  requests
}) => {
  const getPriorityBadge = (priority: string) => {
    const styles = {
      HIGH: 'bg-red-100 text-red-800 border-red-300',
      MEDIUM: 'bg-yellow-100 text-yellow-800 border-yellow-300',
      LOW: 'bg-blue-100 text-blue-800 border-blue-300'
    };

    return (
      <span className={`px-3 py-1 rounded-full text-xs font-medium border ${styles[priority]}`}>
        {priority}
      </span>
    );
  };

  const getStatusBadge = (status: string) => {
    const styles = {
      PENDING: 'bg-yellow-100 text-yellow-800',
      SENT_TO_D365: 'bg-blue-100 text-blue-800',
      APPROVED: 'bg-green-100 text-green-800',
      REJECTED: 'bg-red-100 text-red-800',
      FULFILLED: 'bg-gray-100 text-gray-800'
    };

    return (
      <span className={`px-3 py-1 rounded-full text-xs font-medium ${styles[status]}`}>
        {status.replace('_', ' ')}
      </span>
    );
  };

  return (
    <div className="overflow-x-auto">
      <table className="min-w-full divide-y divide-gray-200">
        <thead className="bg-gray-50">
          <tr>
            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
              Request ID
            </th>
            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
              Product
            </th>
            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
              Current Qty
            </th>
            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
              Minimum Qty
            </th>
            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
              Requested Qty
            </th>
            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
              Priority
            </th>
            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
              Status
            </th>
            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
              Created
            </th>
          </tr>
        </thead>
        <tbody className="bg-white divide-y divide-gray-200">
          {requests.map((request) => (
            <tr key={request.restockRequestId} className="hover:bg-gray-50">
              <td className="px-6 py-4 whitespace-nowrap text-sm font-mono text-gray-900">
                {request.restockRequestId.slice(0, 8)}
              </td>
              <td className="px-6 py-4 text-sm text-gray-900">
                <div>
                  <p className="font-medium">{request.productCode}</p>
                  <p className="text-gray-500 text-xs">{request.productDescription}</p>
                </div>
              </td>
              <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                <span className="font-semibold text-red-600">{request.currentQuantity}</span>
              </td>
              <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                {request.minimumQuantity}
              </td>
              <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                <span className="font-semibold text-green-600">{request.requestedQuantity}</span>
              </td>
              <td className="px-6 py-4 whitespace-nowrap">
                {getPriorityBadge(request.priority)}
              </td>
              <td className="px-6 py-4 whitespace-nowrap">
                {getStatusBadge(request.status)}
              </td>
              <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                {new Date(request.createdAt).toLocaleDateString()}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
};
```

---

## Backend Implementation

### Event-Driven Restock Generation

**Location:** `stock-management-messaging/src/main/java/com/ccbsa/wms/stock/messaging/listener/StockLevelEventListener.java`

```java
package com.ccbsa.wms.stock.messaging.listener;

import com.ccbsa.wms.stock.application.service.command.GenerateRestockRequestCommandHandler;
import com.ccbsa.wms.stock.application.command.dto.GenerateRestockRequestCommand;
import com.ccbsa.wms.stock.domain.core.event.StockLevelBelowMinimumEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockLevelEventListener {

    private final GenerateRestockRequestCommandHandler generateRestockRequestCommandHandler;

    @KafkaListener(
            topics = "stock-management-events",
            groupId = "stock-management-restock-generation"
    )
    public void handleStockLevelBelowMinimum(StockLevelBelowMinimumEvent event) {
        log.info("Received StockLevelBelowMinimumEvent for product: {}",
                event.getProductId());

        try {
            GenerateRestockRequestCommand command = GenerateRestockRequestCommand.builder()
                    .productId(event.getProductId())
                    .currentQuantity(event.getCurrentQuantity())
                    .minimumQuantity(event.getMinimumQuantity())
                    .maximumQuantity(event.getMaximumQuantity())
                    .build();

            generateRestockRequestCommandHandler.handle(command);

            log.info("Restock request generated for product: {}", event.getProductId());
        } catch (Exception e) {
            log.error("Failed to generate restock request for product: {}",
                    event.getProductId(), e);
        }
    }
}
```

### Command Handler: GenerateRestockRequestCommandHandler

**Location:** `stock-management-application-service/src/main/java/com/ccbsa/wms/stock/application/service/command/GenerateRestockRequestCommandHandler.java`

```java
package com.ccbsa.wms.stock.application.service.command;

import com.ccbsa.common.domain.DomainEvent;
import com.ccbsa.wms.stock.application.command.dto.GenerateRestockRequestCommand;
import com.ccbsa.wms.stock.application.command.dto.GenerateRestockRequestResult;
import com.ccbsa.wms.stock.application.service.port.messaging.StockManagementEventPublisher;
import com.ccbsa.wms.stock.application.service.port.repository.RestockRequestRepository;
import com.ccbsa.wms.stock.domain.core.RestockRequest;
import com.ccbsa.wms.stock.domain.core.exception.DuplicateRestockRequestException;
import com.ccbsa.wms.stock.domain.core.valueobject.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class GenerateRestockRequestCommandHandler {

    private final RestockRequestRepository restockRequestRepository;
    private final StockManagementEventPublisher eventPublisher;

    @Transactional
    public GenerateRestockRequestResult handle(GenerateRestockRequestCommand command) {
        log.info("Generating restock request for product: {}", command.getProductId());

        // 1. Check for existing active restock request
        Optional<RestockRequest> existingRequest = restockRequestRepository
                .findActiveRequestByProductId(command.getProductId());

        if (existingRequest.isPresent()) {
            log.info("Active restock request already exists for product: {}",
                    command.getProductId());
            throw new DuplicateRestockRequestException(
                    "Active restock request already exists for product: " +
                            command.getProductId());
        }

        // 2. Calculate requested quantity (max - current)
        int requestedQuantity = command.getMaximumQuantity().getValue() -
                               command.getCurrentQuantity().getValue();

        // 3. Calculate priority based on current vs minimum
        RestockPriority priority = calculatePriority(
                command.getCurrentQuantity(),
                command.getMinimumQuantity()
        );

        // 4. Create restock request
        RestockRequest restockRequest = RestockRequest.builder()
                .restockRequestId(RestockRequestId.of(UUID.randomUUID()))
                .productId(command.getProductId())
                .currentQuantity(command.getCurrentQuantity())
                .minimumQuantity(command.getMinimumQuantity())
                .maximumQuantity(command.getMaximumQuantity())
                .requestedQuantity(Quantity.of(requestedQuantity))
                .priority(priority)
                .build();

        // 5. Save restock request
        restockRequestRepository.save(restockRequest);

        // 6. Publish domain events
        List<DomainEvent<?>> events = restockRequest.getDomainEvents();
        events.forEach(eventPublisher::publish);
        restockRequest.clearDomainEvents();

        log.info("Restock request generated successfully: {}", restockRequest.getId());

        // 7. Return result
        return GenerateRestockRequestResult.builder()
                .restockRequestId(restockRequest.getId().getValue())
                .productId(command.getProductId().getValue())
                .requestedQuantity(requestedQuantity)
                .priority(priority.name())
                .build();
    }

    private RestockPriority calculatePriority(Quantity currentQuantity,
                                             Quantity minimumQuantity) {
        double percentageOfMinimum = (double) currentQuantity.getValue() /
                                    minimumQuantity.getValue();

        if (percentageOfMinimum < 0.5) {
            return RestockPriority.HIGH;
        } else if (percentageOfMinimum < 0.8) {
            return RestockPriority.MEDIUM;
        } else {
            return RestockPriority.LOW;
        }
    }
}
```

---

## Domain Model

### RestockRequest Aggregate

**Location:** `stock-management-domain-core/src/main/java/com/ccbsa/wms/stock/domain/core/RestockRequest.java`

```java
package com.ccbsa.wms.stock.domain.core;

import com.ccbsa.common.domain.AggregateRoot;
import com.ccbsa.wms.stock.domain.core.event.RestockRequestGeneratedEvent;
import com.ccbsa.wms.stock.domain.core.valueobject.*;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class RestockRequest extends AggregateRoot<RestockRequestId> {

    private ProductId productId;
    private Quantity currentQuantity;
    private Quantity minimumQuantity;
    private Quantity maximumQuantity;
    private Quantity requestedQuantity;
    private RestockPriority priority;
    private RestockRequestStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime sentToD365At;
    private String d365OrderReference;

    private RestockRequest() {
        // Private constructor for builder
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private RestockRequest restockRequest = new RestockRequest();

        public Builder restockRequestId(RestockRequestId id) {
            restockRequest.id = id;
            return this;
        }

        public Builder productId(ProductId productId) {
            restockRequest.productId = productId;
            return this;
        }

        public Builder currentQuantity(Quantity currentQuantity) {
            restockRequest.currentQuantity = currentQuantity;
            return this;
        }

        public Builder minimumQuantity(Quantity minimumQuantity) {
            restockRequest.minimumQuantity = minimumQuantity;
            return this;
        }

        public Builder maximumQuantity(Quantity maximumQuantity) {
            restockRequest.maximumQuantity = maximumQuantity;
            return this;
        }

        public Builder requestedQuantity(Quantity requestedQuantity) {
            restockRequest.requestedQuantity = requestedQuantity;
            return this;
        }

        public Builder priority(RestockPriority priority) {
            restockRequest.priority = priority;
            return this;
        }

        public RestockRequest build() {
            validate();
            restockRequest.status = RestockRequestStatus.PENDING;
            restockRequest.createdAt = LocalDateTime.now();

            // Publish domain event
            restockRequest.addDomainEvent(new RestockRequestGeneratedEvent(
                    restockRequest.id,
                    restockRequest.productId,
                    restockRequest.currentQuantity,
                    restockRequest.minimumQuantity,
                    restockRequest.requestedQuantity,
                    restockRequest.priority
            ));

            return restockRequest;
        }

        private void validate() {
            if (restockRequest.id == null) {
                throw new IllegalArgumentException("RestockRequestId is required");
            }
            if (restockRequest.productId == null) {
                throw new IllegalArgumentException("ProductId is required");
            }
            if (restockRequest.requestedQuantity == null ||
                restockRequest.requestedQuantity.getValue() <= 0) {
                throw new IllegalArgumentException("Requested quantity must be positive");
            }
        }
    }

    /**
     * Mark as sent to D365
     */
    public void markAsSentToD365(String d365OrderReference) {
        this.status = RestockRequestStatus.SENT_TO_D365;
        this.sentToD365At = LocalDateTime.now();
        this.d365OrderReference = d365OrderReference;
    }

    /**
     * Mark as fulfilled
     */
    public void markAsFulfilled() {
        this.status = RestockRequestStatus.FULFILLED;
    }

    public boolean isActive() {
        return this.status == RestockRequestStatus.PENDING ||
               this.status == RestockRequestStatus.SENT_TO_D365;
    }
}
```

### Value Objects

**RestockPriority Enum:**

```java
package com.ccbsa.wms.stock.domain.core.valueobject;

public enum RestockPriority {
    LOW("Low", "Stock between 80-100% of minimum"),
    MEDIUM("Medium", "Stock between 50-80% of minimum"),
    HIGH("High", "Stock below 50% of minimum - urgent");

    private final String displayName;
    private final String description;

    RestockPriority(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}
```

**RestockRequestStatus Enum:**

```java
package com.ccbsa.wms.stock.domain.core.valueobject;

public enum RestockRequestStatus {
    PENDING("Pending", "Restock request created, awaiting processing"),
    SENT_TO_D365("Sent to D365", "Restock request sent to D365 for approval"),
    APPROVED("Approved", "Restock request approved in D365"),
    REJECTED("Rejected", "Restock request rejected in D365"),
    FULFILLED("Fulfilled", "Restock received and inventory updated");

    private final String displayName;
    private final String description;

    RestockRequestStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}
```

---

## Event Design

### RestockRequestGeneratedEvent

```java
package com.ccbsa.wms.stock.domain.core.event;

import com.ccbsa.wms.stock.domain.core.RestockRequest;
import com.ccbsa.wms.stock.domain.core.valueobject.*;
import lombok.Getter;

@Getter
public class RestockRequestGeneratedEvent extends StockManagementEvent<RestockRequest> {

    private final ProductId productId;
    private final Quantity currentQuantity;
    private final Quantity minimumQuantity;
    private final Quantity requestedQuantity;
    private final RestockPriority priority;

    public RestockRequestGeneratedEvent(
            RestockRequestId restockRequestId,
            ProductId productId,
            Quantity currentQuantity,
            Quantity minimumQuantity,
            Quantity requestedQuantity,
            RestockPriority priority) {
        super(restockRequestId);
        this.productId = productId;
        this.currentQuantity = currentQuantity;
        this.minimumQuantity = minimumQuantity;
        this.requestedQuantity = requestedQuantity;
        this.priority = priority;
    }
}
```

---

## D365 Integration

### Integration Service Event Listener

**Location:** `integration-service/integration-messaging/src/main/java/com/ccbsa/wms/integration/messaging/listener/RestockRequestEventListener.java`

```java
package com.ccbsa.wms.integration.messaging.listener;

import com.ccbsa.wms.integration.application.service.D365RestockRequestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RestockRequestEventListener {

    private final D365RestockRequestService d365RestockRequestService;

    @KafkaListener(
            topics = "stock-management-events",
            groupId = "integration-service-restock-request"
    )
    public void handleRestockRequestGenerated(RestockRequestGeneratedEvent event) {
        log.info("Received RestockRequestGeneratedEvent: {}", event.getAggregateId());

        try {
            // Send restock request to D365
            d365RestockRequestService.sendRestockRequestToD365(event);
            log.info("Restock request sent to D365 successfully: {}",
                    event.getAggregateId());
        } catch (Exception e) {
            log.error("Failed to send restock request to D365: {}",
                    event.getAggregateId(), e);
            // Retry will be handled by Kafka consumer configuration
        }
    }
}
```

---

## Testing Strategy

### Unit Tests

```java
@Test
void shouldGenerateRestockRequestWhenBelowMinimum() {
    // Given
    Quantity currentQuantity = Quantity.of(20);
    Quantity minimumQuantity = Quantity.of(50);
    Quantity maximumQuantity = Quantity.of(100);

    GenerateRestockRequestCommand command = GenerateRestockRequestCommand.builder()
            .productId(ProductId.of(UUID.randomUUID()))
            .currentQuantity(currentQuantity)
            .minimumQuantity(minimumQuantity)
            .maximumQuantity(maximumQuantity)
            .build();

    // When
    GenerateRestockRequestResult result = handler.handle(command);

    // Then
    assertThat(result.getRequestedQuantity()).isEqualTo(80); // 100 - 20
    assertThat(result.getPriority()).isEqualTo("HIGH"); // 20 < 50% of 50
}

@Test
void shouldPreventDuplicateRestockRequests() {
    // Given: Active restock request exists
    ProductId productId = ProductId.of(UUID.randomUUID());
    createActiveRestockRequest(productId);

    // When/Then
    assertThatThrownBy(() -> handler.handle(createCommand(productId)))
            .isInstanceOf(DuplicateRestockRequestException.class);
}
```

---

## Implementation Checklist

### Frontend
- [ ] Create `RestockRequestsDashboard` page
- [ ] Create `RestockRequestList` component
- [ ] Create `RestockRequestFilters` component
- [ ] Create `useRestockRequests` hook
- [ ] Add routing for restock requests
- [ ] Test UI with mock data

### Backend - Domain Core
- [ ] Create `RestockRequest` aggregate
- [ ] Create `RestockPriority` enum
- [ ] Create `RestockRequestStatus` enum
- [ ] Create `RestockRequestGeneratedEvent`
- [ ] Write unit tests

### Backend - Application Service
- [ ] Create `GenerateRestockRequestCommand`
- [ ] Create `GenerateRestockRequestCommandHandler`
- [ ] Add deduplication logic
- [ ] Add priority calculation
- [ ] Write unit tests

### Backend - Event Listener
- [ ] Create `StockLevelEventListener`
- [ ] Handle `StockLevelBelowMinimumEvent`
- [ ] Trigger restock request generation
- [ ] Test event consumption

### D365 Integration (Optional)
- [ ] Create event listener in Integration Service
- [ ] Implement D365 restock request API call
- [ ] Add retry logic
- [ ] Test D365 integration

### Gateway API Tests
- [ ] Test restock request generation
- [ ] Test duplicate prevention
- [ ] Test priority calculation
- [ ] Test D365 integration

### Documentation
- [ ] Document restock logic
- [ ] Document priority calculation
- [ ] Update API documentation
- [ ] Create user guide

---

**Document Version:** 1.0  
**Last Updated:** 2026-01-08  
**Status:** Ready for Implementation
