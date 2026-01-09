# Sprint 6 Implementation Plan

## Warehouse Management System Integration - CCBSA LDP System

**Sprint:** Sprint 6 - Picking Execution and Expiration Management
**Duration:** 2 weeks
**Sprint Goal:** Complete picking execution workflow and implement expiration tracking and restock management
**Total Story Points:** 26

---

## Table of Contents

1. [Sprint Overview](#sprint-overview)
2. [User Stories](#user-stories)
3. [Implementation Approach](#implementation-approach)
4. [Architecture Compliance](#architecture-compliance)
5. [Data Flow Design](#data-flow-design)
6. [Service Segregation](#service-segregation)
7. [Testing Strategy](#testing-strategy)
8. [Implementation Order](#implementation-order)
9. [Dependencies and Prerequisites](#dependencies-and-prerequisites)

---

## Sprint Overview

### Sprint Goal

Complete the picking execution workflow and implement expiration tracking and restock management to enable warehouse operators to:

1. **Execute Picking Tasks** - Execute individual picking tasks with quantity validation
2. **Complete Picking Operations** - Mark picking lists as complete for shipping
3. **Track Expiration Dates** - Monitor stock expiration and generate alerts
4. **Prevent Picking Expired Stock** - Prevent expired stock from being picked
5. **Generate Restock Requests** - Automatically generate restock requests when stock falls below minimum

### Success Criteria

- ✅ Operators can execute picking tasks with real-time stock level updates
- ✅ Operators can mark picking operations as complete
- ✅ System tracks expiration dates and generates alerts for expiring stock
- ✅ System prevents picking of expired stock
- ✅ System automatically generates restock requests when stock falls below minimum thresholds
- ✅ All data flows correctly from frontend through gateway to backend services
- ✅ Gateway API tests validate all endpoints
- ✅ All implementations follow DDD, Clean Hexagonal Architecture, CQRS, and Event-Driven Choreography principles

---

## User Stories

### Story 1: US-6.3.1 - Execute Picking Task (8 points)

**Service:** Picking Service, Stock Management Service, Location Management Service
**Priority:** Must Have

**Acceptance Criteria:**

- System guides picking operations based on optimized location assignments
- System validates picked quantities against picking list
- System updates stock levels in real-time during picking
- System supports partial picking scenarios
- System publishes `PickingTaskCompletedEvent` or `PartialPickingCompletedEvent`
- System records picking timestamp and user

**Implementation Plan:** [01-Execute-Picking-Task-Implementation-Plan.md](01-Execute-Picking-Task-Implementation-Plan.md)

---

### Story 2: US-6.3.2 - Complete Picking (5 points)

**Service:** Picking Service
**Priority:** Must Have

**Acceptance Criteria:**

- System allows completing picking when all tasks are completed
- System validates all picking tasks are completed or partial
- System updates picking list status to "COMPLETED"
- System publishes `PickingCompletedEvent`
- System updates order status within load

**Implementation Plan:** [02-Complete-Picking-Implementation-Plan.md](02-Complete-Picking-Implementation-Plan.md)

---

### Story 3: US-2.1.3 - Track Expiration Dates and Generate Alerts (5 points)

**Service:** Stock Management Service, Notification Service
**Priority:** Must Have

**Acceptance Criteria:**

- System generates alert when stock is within 7 days of expiration
- System generates alert when stock is within 30 days of expiration
- System prevents picking of expired stock
- System generates reports on expiring stock
- Alerts are visible in dashboard and can be filtered by date range

**Implementation Plan:** [03-Track-Expiration-Dates-Implementation-Plan.md](03-Track-Expiration-Dates-Implementation-Plan.md)

---

### Story 4: US-2.1.4 - Prevent Picking of Expired Stock (3 points)

**Service:** Stock Management Service, Picking Service
**Priority:** Must Have

**Acceptance Criteria:**

- System checks stock expiration date before allowing picking
- System displays error message when attempting to pick expired stock
- System excludes expired stock from picking location queries
- System logs attempts to pick expired stock

**Implementation Plan:** [04-Prevent-Picking-Expired-Stock-Implementation-Plan.md](04-Prevent-Picking-Expired-Stock-Implementation-Plan.md)

---

### Story 5: US-5.1.3 - Generate Restock Request (5 points)

**Service:** Stock Management Service, Integration Service (optional)
**Priority:** Must Have

**Acceptance Criteria:**

- System automatically generates restock request when stock falls below minimum
- Restock request includes: product code, current quantity, minimum quantity, requested quantity, priority
- Request is sent to D365 for processing (if D365 integration enabled)
- System tracks restock request status
- System prevents duplicate restock requests

**Implementation Plan:** [05-Generate-Restock-Request-Implementation-Plan.md](05-Generate-Restock-Request-Implementation-Plan.md)

---

## Implementation Approach

### Frontend-First Design

All implementation plans start with **production-grade UI design** to ensure:

1. **User Experience** - Intuitive, accessible, and responsive interfaces
2. **Real-Time Feedback** - Live updates during picking execution
3. **Error Handling** - Clear error messages and recovery paths
4. **Visual Indicators** - Clear display of picking progress, expired stock, and restock alerts
5. **Accessibility** - WCAG 2.1 Level AA compliance from the start

### Backend Implementation

Following **Clean Hexagonal Architecture**:

1. **Domain Core** - Pure Java domain models with business logic
2. **Application Service** - Use case orchestration and port definitions
3. **Application Layer** - REST API with CQRS separation
4. **Data Access** - Repository adapters with JPA entities
5. **Messaging** - Event publishers and listeners for choreography

### CQRS Implementation

- **Command Side:** POST/PUT/DELETE operations modify state
- **Query Side:** GET operations read optimized views
- **Event Publishing:** Commands publish domain events for eventual consistency
- **Read Models:** Query handlers use data ports for optimized reads

### Event-Driven Choreography

- **Domain Events:** Aggregates publish events on state changes
- **Event Correlation:** Correlation IDs track business flows
- **Idempotency:** Event handlers are idempotent
- **Loose Coupling:** Services communicate only through events

### Service Integration

- **Synchronous Calls:** Stock availability queries for real-time validation
- **Circuit Breaker:** Resilience patterns for service calls
- **Event-Driven:** Picking execution triggers stock movements and level updates
- **Error Handling:** Graceful degradation when services unavailable

---

## Architecture Compliance

### Domain-Driven Design (DDD)

- **Bounded Contexts:** Clear service boundaries (Picking Service, Stock Management Service, Location Management Service)
- **Aggregates:** PickingTask, StockItem, StockLevel, Location are aggregate roots
- **Value Objects:** Quantity, ExpirationDate, StockClassification, RestockPriority, etc.
- **Domain Events:** PickingTaskCompletedEvent, StockExpiringAlertEvent, RestockRequestGeneratedEvent
- **Ubiquitous Language:** Business terminology throughout codebase

### Clean Hexagonal Architecture

- **Domain Core:** Pure Java, no framework dependencies
- **Application Service:** Port interfaces defined here
- **Infrastructure:** Adapters implement ports
- **Dependency Direction:** Domain ← Application ← Infrastructure

### CQRS

- **Command Controllers:** Separate from query controllers
- **Command Handlers:** Return command-specific results
- **Query Handlers:** Return optimized query results
- **Read Models:** Denormalized views for queries

### Event-Driven Choreography

- **Event Publishing:** After successful command execution
- **Event Consumption:** Asynchronous event listeners
- **No SAGA:** Event-driven choreography only
- **Idempotency:** All event handlers are idempotent

---

## Data Flow Design

### Execute Picking Task Flow

```
Frontend (React)
  ↓ POST /api/v1/picking/picking-tasks/{taskId}/execute
Gateway Service
  ↓ Route to Picking Service
Picking Service (Command Controller)
  ↓ ExecutePickingTaskCommand
Command Handler
  ↓ Validate picking task exists and is PENDING
  ↓ Validate picked quantity
  ↓ Query Stock Management Service for stock availability
  ↓ PickingTask.execute()
  Domain Core (PickingTask Aggregate)
  ↓ PickingTaskCompletedEvent or PartialPickingCompletedEvent
Event Publisher
  ↓ Kafka Topic: picking-events
Stock Management Service (Event Listener)
  ↓ Update stock levels
Location Management Service (Event Listener)
  ↓ Create stock movement record
Query Handler
  ↓ PickingTaskExecutionResult
Query Controller
  ↓ Response
Gateway Service
  ↓ Response
Frontend (React)
```

### Complete Picking Flow

```
Frontend (React)
  ↓ POST /api/v1/picking/picking-lists/{pickingListId}/complete
Gateway Service
  ↓ Route to Picking Service
Picking Service (Command Controller)
  ↓ CompletePickingCommand
Command Handler
  ↓ Validate all picking tasks are completed or partial
  ↓ PickingList.complete()
  Domain Core (PickingList Aggregate)
  ↓ PickingCompletedEvent
Event Publisher
  ↓ Kafka Topic: picking-events
Returns Service (Event Listener)
  ↓ Prepare for potential returns
Query Handler
  ↓ PickingCompletionResult
Query Controller
  ↓ Response
Gateway Service
  ↓ Response
Frontend (React)
```

### Track Expiration Dates Flow

```
Scheduled Job (Daily)
  ↓ Trigger CheckExpirationDatesCommand
Stock Management Service (Scheduled Job)
  ↓ Query all stock items
  ↓ For each stock item:
  ↓   StockItem.checkExpiration()
    Domain Core (StockItem Aggregate)
    ↓ StockExpiringAlertEvent (if expiring within 7 or 30 days)
    ↓ StockExpiredEvent (if expired)
  Event Publisher
    ↓ Kafka Topic: stock-management-events
Notification Service (Event Listener)
  ↓ Create notification for warehouse manager
  ↓ Display alert in dashboard
Picking Service (Event Listener)
  ↓ Update picking location queries to exclude expired stock
```

### Prevent Picking Expired Stock Flow

```
Frontend (React)
  ↓ POST /api/v1/picking/picking-tasks/{taskId}/execute
Gateway Service
  ↓ Route to Picking Service
Picking Service (Command Controller)
  ↓ ExecutePickingTaskCommand
Command Handler
  ↓ Query Stock Management Service for stock item
  ↓ StockItem.canBePicked() - checks expiration
  ↓ If expired: Reject with error
  ↓ If not expired: Proceed with execution
```

### Generate Restock Request Flow

```
Stock Management Service (Event-Driven)
  ↓ Listen to StockLevelUpdatedEvent
  ↓ StockLevel.checkMinimumThreshold()
  Domain Core (StockLevel Aggregate)
  ↓ If below minimum: RestockRequestGeneratedEvent
Event Publisher
  ↓ Kafka Topic: stock-management-events
Integration Service (Event Listener) [OPTIONAL - if D365 enabled]
  ↓ Transform to D365 format
  ↓ Send to D365 via OData API
  ↓ Handle retries and errors
Notification Service (Event Listener)
  ↓ Notify warehouse manager
```

---

## Service Segregation

### Picking Service

**Responsibilities:**

- Execute picking tasks
- Complete picking operations
- Validate picking quantities
- Track picking progress
- Publish picking events

**Aggregates:**

- `PickingTask` - Represents individual picking tasks
- `PickingList` - Represents picking list with loads and orders
- `PickingExecution` - Represents picking execution workflow

**Events Published:**

- `PickingTaskCompletedEvent`
- `PartialPickingCompletedEvent`
- `PickingCompletedEvent`

**Events Consumed:**

- `PickingListReceivedEvent` (internal)
- `LoadPlannedEvent` (internal)

---

### Stock Management Service

**Responsibilities:**

- Track expiration dates
- Generate expiration alerts
- Prevent picking expired stock
- Generate restock requests
- Update stock levels
- Manage stock availability

**Aggregates:**

- `StockItem` - Represents stock items with expiration tracking
- `StockLevel` - Represents stock levels per product/location
- `RestockRequest` - Represents restock requests

**Events Published:**

- `StockLevelUpdatedEvent`
- `StockExpiringAlertEvent`
- `StockExpiredEvent`
- `RestockRequestGeneratedEvent`
- `StockLevelBelowMinimumEvent`

**Events Consumed:**

- `PickingTaskCompletedEvent` (from Picking Service)
- `StockMovementCompletedEvent` (from Location Management Service)

---

### Location Management Service

**Responsibilities:**

- Track stock movements during picking
- Update location status
- Maintain location capacity

**Aggregates:**

- `StockMovement` - Represents stock movements
- `Location` - Represents warehouse locations

**Events Published:**

- `StockMovementInitiatedEvent`
- `StockMovementCompletedEvent`

**Events Consumed:**

- `PickingTaskCompletedEvent` (from Picking Service)

---

### Notification Service

**Responsibilities:**

- Create notifications for expiring stock alerts
- Display alerts in dashboard
- Track notification status

**Aggregates:**

- `Notification` - Represents notification

**Events Published:**

- `NotificationCreatedEvent`

**Events Consumed:**

- `StockExpiringAlertEvent` (from Stock Management Service)
- `StockExpiredEvent` (from Stock Management Service)
- `RestockRequestGeneratedEvent` (from Stock Management Service)

---

### Integration Service (Optional - D365 Integration)

**Responsibilities:**

- Send restock requests to D365 (if integration enabled)
- Handle D365 API errors with retry logic

**Events Consumed:**

- `RestockRequestGeneratedEvent` (from Stock Management Service)

---

## Testing Strategy

### Unit Testing

**Domain Core:**

- Test aggregate behavior (PickingTask.execute(), StockItem.checkExpiration(), StockLevel.checkMinimumThreshold())
- Test business rules (expired stock cannot be picked, restock requests generated below minimum)
- Test event publishing (correct events published on state changes)
- Pure Java testing (JUnit 5, AssertJ)

**Application Service:**

- Test command handlers (ExecutePickingTaskCommandHandler, CompletePickingCommandHandler)
- Test query handlers (GetPickingTaskQueryHandler, GetExpiringStockQueryHandler)
- Mock port dependencies
- Verify event publishing

**Infrastructure:**

- Test repository adapters
- Test event publishers and listeners
- Test REST controllers
- Integration tests with Testcontainers

### Integration Testing

**Gateway API Tests:**

- Test picking task execution endpoint
- Test picking completion endpoint
- Test expiring stock queries
- Test restock request generation
- Validate end-to-end flows
- Test error scenarios

**Event Flow Tests:**

- Test event publishing and consumption
- Test event correlation
- Test idempotency
- Test error handling

### Frontend Testing

**Component Tests:**

- Test picking task execution component
- Test picking completion component
- Test expiring stock alerts component
- Test restock request notifications

**E2E Tests:**

- Test full picking execution workflow
- Test expiration alert workflow
- Test restock request workflow

---

## Implementation Order

### Phase 1: Prevent Picking Expired Stock (Day 1-2)

**Rationale:** Foundation for safe picking operations

1. Implement `StockItem.canBePicked()` method (US-2.1.4)
2. Update picking location queries to filter expired stock
3. Add validation in picking task execution
4. Create frontend error handling for expired stock
5. Write unit and integration tests

### Phase 2: Execute Picking Task (Day 3-5)

**Rationale:** Core picking execution functionality

1. Implement picking task execution UI (US-6.3.1)
2. Implement `ExecutePickingTaskCommand` and handler
3. Implement `PickingTask.execute()` in domain core
4. Add stock level validation and updates
5. Create stock movement records
6. Write gateway API tests

### Phase 3: Complete Picking (Day 6-7)

**Rationale:** Complete picking workflow

1. Implement picking completion UI (US-6.3.2)
2. Implement `CompletePickingCommand` and handler
3. Implement `PickingList.complete()` in domain core
4. Validate all tasks completed or partial
5. Write gateway API tests

### Phase 4: Track Expiration Dates (Day 8-9)

**Rationale:** Proactive expiration management

1. Implement scheduled job for expiration checking (US-2.1.3)
2. Implement `StockItem.checkExpiration()` method
3. Create expiring stock alerts dashboard UI
4. Implement notification creation for alerts
5. Write unit and integration tests

### Phase 5: Generate Restock Requests (Day 10)

**Rationale:** Automated inventory replenishment

1. Implement `StockLevel.checkMinimumThreshold()` method (US-5.1.3)
2. Create restock request generation logic
3. Implement restock request notification UI
4. Optional: Integrate with D365 (if enabled)
5. Write gateway API tests

---

## Dependencies and Prerequisites

### Prerequisites from Previous Sprints

**Sprint 1-4:**

- ✅ Stock Management Service operational
- ✅ Stock items with expiration dates created
- ✅ Stock levels tracked per product/location
- ✅ Location Management Service operational

**Sprint 5:**

- ✅ Picking lists created (US-6.1.1, US-6.1.2, US-6.1.4)
- ✅ Picking locations planned (US-6.2.1)
- ✅ Picking tasks created (US-6.2.1)

### External Dependencies

- **Product Service:** Product validation
- **Stock Management Service:** Stock availability queries
- **Location Management Service:** Stock movement tracking
- **Notification Service:** Alert notifications

### Technical Dependencies

- **Kafka:** Event streaming
- **PostgreSQL:** Database storage
- **Spring Boot:** Application framework
- **React:** Frontend framework

### Common Value Objects Needed

Move to `common-domain` module if not already present:

- `Quantity` - Stock quantity
- `ExpirationDate` - Expiration date with validation
- `Priority` - Restock priority enum
- `StockClassification` - Stock classification enum

---

## Risk Mitigation

### Risk 1: Real-Time Stock Level Updates

**Risk:** Race conditions when multiple operators pick simultaneously

**Mitigation:**

- Use optimistic locking on stock level updates
- Implement retry logic for concurrent updates
- Display real-time stock availability to operators

### Risk 2: Expiration Date Accuracy

**Risk:** Incorrect expiration dates lead to picking expired stock

**Mitigation:**

- Validate expiration dates on consignment receipt
- Run scheduled jobs to check expirations
- Alert warehouse managers of critical stock
- Prevent picking expired stock at system level

### Risk 3: Duplicate Restock Requests

**Risk:** Multiple restock requests generated for same product

**Mitigation:**

- Track restock request status
- Check pending requests before generating new ones
- Use idempotent event handlers

### Risk 4: D365 Integration Failures (Optional)

**Risk:** Restock requests fail to reach D365

**Mitigation:**

- Implement retry logic with exponential backoff
- Use dead letter queue for persistent failures
- Alert on D365 integration errors
- Allow manual restock request submission

---

## Definition of Done

### Code Complete

- ✅ All user stories implemented
- ✅ Code follows Clean Hexagonal Architecture
- ✅ Domain logic in domain-core (pure Java)
- ✅ CQRS separation implemented
- ✅ Events published for all state changes

### Testing Complete

- ✅ Unit tests pass (>80% coverage)
- ✅ Integration tests pass
- ✅ Gateway API tests pass
- ✅ Frontend E2E tests pass

### Documentation Complete

- ✅ API documentation updated (OpenAPI)
- ✅ Implementation plans documented
- ✅ Architecture decisions recorded
- ✅ User guides updated

### Acceptance Criteria Met

- ✅ All acceptance criteria validated
- ✅ Product Owner approval
- ✅ Demo to stakeholders

---

## Sprint Success Metrics

### Velocity Metrics

- **Planned Points:** 26
- **Completed Points:** TBD
- **Velocity:** TBD

### Quality Metrics

- **Test Coverage:** Target >80%
- **Code Review:** All code reviewed
- **Defect Rate:** <5% post-deployment

### Business Metrics

- **Picking Accuracy:** Target >95%
- **Expired Stock Incidents:** Zero
- **Restock Request Timeliness:** <1 hour from threshold breach

---

## Appendix

### Related Documentation

- [User Story Breakdown](../../../06-project-management/User_Story_Breakdown.md)
- [Service Architecture Document](../../../01-architecture/Service_Architecture_Document.md)
- [Domain Model Design](../../../01-architecture/Domain_Model_Design.md)
- [Mandated Implementation Template Guide](../../../guide/mandated-Implementation-template-guide.md)

### Glossary

| Term                   | Definition                                           |
|------------------------|------------------------------------------------------|
| **FEFO**               | First Expiring First Out                             |
| **Picking Task**       | Individual task to pick specific product from location |
| **Partial Picking**    | Picking less quantity than requested                 |
| **Stock Classification** | Classification by expiration (CRITICAL, NEAR_EXPIRY, NORMAL) |
| **Restock Request**    | Automated request to replenish stock below minimum   |

---

**Document Control**

- **Version:** 1.0
- **Date:** 2026-01-08
- **Author:** System Architect
- **Status:** Draft
- **Review Cycle:** Sprint planning and retrospective
