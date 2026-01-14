# Sprint 7 Implementation Plan

## Warehouse Management System Integration - CCBSA LDP System

**Sprint:** Sprint 7 - Returns Management
**Duration:** 2 weeks
**Sprint Goal:** Implement comprehensive returns management including partial acceptance, full returns, damage handling, location assignment, and D365 reconciliation
**Total Story Points:** 28

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

Implement the complete returns management workflow to enable warehouse operators to:

1. **Handle Partial Order Acceptance** - Process returns when customers accept only part of their order
2. **Process Full Order Returns** - Handle complete order returns efficiently
3. **Handle Damage-in-Transit Returns** - Classify and manage damaged stock properly
4. **Assign Return Locations** - Automatically assign locations to returned stock based on condition and expiration
5. **Reconcile with D365** - Synchronize return data with Microsoft Dynamics 365 (optional)

### Success Criteria

- ✅ Operators can record partial order acceptance with returned quantities
- ✅ Operators can process full order returns with reason codes and condition assessment
- ✅ System classifies and handles damaged stock appropriately
- ✅ System automatically assigns return locations based on FEFO principles and product condition
- ✅ System reconciles returns with D365 when integration is enabled
- ✅ All data flows correctly from frontend through gateway to backend services
- ✅ Gateway API tests validate all endpoints
- ✅ All implementations follow DDD, Clean Hexagonal Architecture, CQRS, and Event-Driven Choreography principles

---

## User Stories

### Story 1: US-7.1.1 - Handle Partial Order Acceptance (8 points)

**Service:** Returns Service
**Priority:** Must Have

**Acceptance Criteria:**

- System records accepted quantities per order line
- System identifies returned quantities
- System updates order status accordingly
- System initiates returns process for unaccepted items
- System publishes `ReturnInitiatedEvent` for partial returns
- System records customer signature and acceptance timestamp
- System supports multiple product lines per order

**Implementation Plan:** [01-Handle-Partial-Order-Acceptance-Implementation-Plan.md](01-Handle-Partial-Order-Acceptance-Implementation-Plan.md)

---

### Story 2: US-7.2.1 - Process Full Order Return (5 points)

**Service:** Returns Service
**Priority:** Must Have

**Acceptance Criteria:**

- System records return reason
- System validates returned products against original order
- System checks product condition
- System updates stock levels
- System publishes `ReturnProcessedEvent`
- System supports return reason codes (CUSTOMER_REJECTION, QUALITY_ISSUE, OVERSTOCK, etc.)

**Implementation Plan:** [02-Process-Full-Order-Return-Implementation-Plan.md](02-Process-Full-Order-Return-Implementation-Plan.md)

---

### Story 3: US-7.3.1 - Handle Damage-in-Transit Returns (5 points)

**Service:** Returns Service
**Priority:** Must Have

**Acceptance Criteria:**

- System records damage type and extent
- System classifies damage (repairable, write-off)
- System handles damaged stock appropriately (quarantine, disposal, repair)
- System generates damage reports
- System publishes `DamageRecordedEvent`
- System captures photographic evidence of damage

**Implementation Plan:** [03-Handle-Damage-in-Transit-Returns-Implementation-Plan.md](03-Handle-Damage-in-Transit-Returns-Implementation-Plan.md)

---

### Story 4: US-7.4.1 - Assign Return Location (5 points)

**Service:** Returns Service, Location Management Service
**Priority:** Must Have

**Acceptance Criteria:**

- System assigns return location based on product condition and expiration date
- System updates stock availability for re-picking
- System maintains return history
- System prioritizes returned stock in picking if appropriate
- System publishes `ReturnLocationAssignedEvent`
- Damaged stock automatically assigned to quarantine locations

**Implementation Plan:** [04-Assign-Return-Location-Implementation-Plan.md](04-Assign-Return-Location-Implementation-Plan.md)

---

### Story 5: US-7.5.1 - Reconcile Returns with D365 (5 points)

**Service:** Returns Service, Integration Service
**Priority:** Should Have

**Acceptance Criteria:**

- System reconciles returns and updates D365 (if D365 integration enabled)
- Reconciliation includes: returned quantities, condition, location assignment, reason codes
- System handles reconciliation errors and retries
- System maintains reconciliation audit trail
- System publishes `ReturnReconciledEvent`

**Implementation Plan:** [05-Reconcile-Returns-with-D365-Implementation-Plan.md](05-Reconcile-Returns-with-D365-Implementation-Plan.md)

---

## Implementation Approach

### Frontend-First Design

All implementation plans start with **production-grade UI design** to ensure:

1. **User Experience** - Intuitive, accessible, and responsive interfaces
2. **Real-Time Feedback** - Live updates during returns processing
3. **Error Handling** - Clear error messages and recovery paths
4. **Visual Indicators** - Clear display of return status, damage assessment, and location assignment
5. **Accessibility** - WCAG 2.1 Level AA compliance from the start
6. **Mobile Support** - PWA functionality for warehouse floor operations

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

- **Synchronous Calls:** Order validation and location queries
- **Circuit Breaker:** Resilience patterns for service calls
- **Event-Driven:** Returns processing triggers stock movements and level updates
- **Error Handling:** Graceful degradation when services unavailable

---

## Architecture Compliance

### Domain-Driven Design (DDD)

- **Bounded Contexts:** Clear service boundaries (Returns Service, Stock Management Service, Location Management Service)
- **Aggregates:** Return, ReturnLineItem, DamageAssessment are aggregate roots
- **Value Objects:** ReturnReason, DamageType, ProductCondition, ReturnStatus, etc.
- **Domain Events:** ReturnInitiatedEvent, ReturnProcessedEvent, DamageRecordedEvent, ReturnLocationAssignedEvent
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

### Partial Order Acceptance Flow

```
Frontend (React)
  ↓ POST /api/v1/returns/partial-acceptance
Gateway Service
  ↓ Route to Returns Service
Returns Service (Command Controller)
  ↓ HandlePartialOrderAcceptanceCommand
Command Handler
  ↓ Validate order exists and picking is completed
  ↓ Calculate returned quantities
  ↓ Return.initiatePartialReturn()
  Domain Core (Return Aggregate)
  ↓ ReturnInitiatedEvent
Event Publisher
  ↓ Kafka Topic: returns-events
Stock Management Service (Event Listener)
  ↓ Update stock levels for returned items
Location Management Service (Event Listener)
  ↓ Prepare for return location assignment
Notification Service (Event Listener)
  ↓ Notify warehouse manager of return
Query Handler
  ↓ PartialOrderAcceptanceResult
Query Controller
  ↓ Response
Gateway Service
  ↓ Response
Frontend (React)
```

### Full Order Return Flow

```
Frontend (React)
  ↓ POST /api/v1/returns/full-return
Gateway Service
  ↓ Route to Returns Service
Returns Service (Command Controller)
  ↓ ProcessFullOrderReturnCommand
Command Handler
  ↓ Validate returned products against order
  ↓ Assess product condition
  ↓ Return.processFullReturn()
  Domain Core (Return Aggregate)
  ↓ ReturnProcessedEvent
Event Publisher
  ↓ Kafka Topic: returns-events
Stock Management Service (Event Listener)
  ↓ Update stock levels for all returned items
Location Management Service (Event Listener)
  ↓ Initiate location assignment
Query Handler
  ↓ FullOrderReturnResult
Query Controller
  ↓ Response
Gateway Service
  ↓ Response
Frontend (React)
```

### Damage-in-Transit Flow

```
Frontend (React)
  ↓ POST /api/v1/returns/damage-assessment
Gateway Service
  ↓ Route to Returns Service
Returns Service (Command Controller)
  ↓ RecordDamageCommand
Command Handler
  ↓ Validate return exists
  ↓ DamageAssessment.create()
  Domain Core (DamageAssessment Aggregate)
  ↓ DamageRecordedEvent
Event Publisher
  ↓ Kafka Topic: returns-events
Stock Management Service (Event Listener)
  ↓ Mark stock as damaged/quarantined
Location Management Service (Event Listener)
  ↓ Assign to quarantine location if write-off
Notification Service (Event Listener)
  ↓ Alert warehouse manager and quality team
Query Handler
  ↓ DamageAssessmentResult
Query Controller
  ↓ Response
Gateway Service
  ↓ Response
Frontend (React)
```

### Return Location Assignment Flow

```
Returns Service (Event-Driven)
  ↓ Listen to ReturnProcessedEvent
  ↓ Query product condition and expiration date
  ↓ Query Location Management Service for available locations
  ↓ Return.assignLocation()
  Domain Core (Return Aggregate)
  ↓ ReturnLocationAssignedEvent
Event Publisher
  ↓ Kafka Topic: returns-events
Stock Management Service (Event Listener)
  ↓ Update stock availability for re-picking
Location Management Service (Event Listener)
  ↓ Update location status and capacity
Picking Service (Event Listener)
  ↓ Include returned stock in picking location queries (if good condition)
```

### D365 Reconciliation Flow (Optional)

```
Returns Service
  ↓ Listen to ReturnLocationAssignedEvent
  ↓ Prepare reconciliation data
  ↓ Return.reconcile()
  Domain Core (Return Aggregate)
  ↓ ReturnReconciledEvent
Event Publisher
  ↓ Kafka Topic: returns-events
Integration Service (Event Listener) [OPTIONAL - if D365 enabled]
  ↓ Transform to D365 format
  ↓ Send to D365 via OData API
  ↓ Handle retries and errors
  ↓ Update reconciliation status
Notification Service (Event Listener)
  ↓ Notify on successful/failed reconciliation
```

---

## Service Segregation

### Returns Service

**Responsibilities:**

- Handle partial order acceptance
- Process full order returns
- Record damage assessments
- Assign return locations
- Reconcile returns with D365 (optional)
- Track return history and status
- Publish return events

**Aggregates:**

- `Return` - Represents return transaction with line items
- `ReturnLineItem` - Individual product return within a return
- `DamageAssessment` - Damage details and classification

**Events Published:**

- `ReturnInitiatedEvent`
- `ReturnProcessedEvent`
- `DamageRecordedEvent`
- `ReturnLocationAssignedEvent`
- `ReturnReconciledEvent`

**Events Consumed:**

- `PickingCompletedEvent` (from Picking Service) - Enables returns processing
- `LocationAssignedEvent` (from Location Management Service) - Location confirmation

---

### Stock Management Service

**Responsibilities:**

- Update stock levels for returns
- Track stock condition changes
- Manage damaged/quarantined stock
- Re-enable stock for picking (if good condition)

**Aggregates:**

- `StockItem` - Updated with return quantities and condition
- `StockLevel` - Updated with returned stock

**Events Published:**

- `StockLevelUpdatedEvent`
- `StockConditionChangedEvent`

**Events Consumed:**

- `ReturnInitiatedEvent` (from Returns Service)
- `ReturnProcessedEvent` (from Returns Service)
- `DamageRecordedEvent` (from Returns Service)

---

### Location Management Service

**Responsibilities:**

- Assign locations to returned stock
- Manage quarantine locations for damaged stock
- Update location capacity for returns
- Track stock movements for returns

**Aggregates:**

- `Location` - Updated with returned stock
- `StockMovement` - Created for return stock movements

**Events Published:**

- `LocationAssignedEvent`
- `StockMovementCompletedEvent`

**Events Consumed:**

- `ReturnProcessedEvent` (from Returns Service)
- `ReturnLocationAssignedEvent` (from Returns Service)

---

### Picking Service

**Responsibilities:**

- Include returned stock in picking location queries (if good condition)
- Prioritize returned stock based on expiration (FEFO)

**Events Consumed:**

- `ReturnLocationAssignedEvent` (from Returns Service)

---

### Notification Service

**Responsibilities:**

- Create notifications for returns events
- Alert warehouse managers of damaged stock
- Notify quality team of write-offs
- Display return alerts in dashboard

**Aggregates:**

- `Notification` - Represents notification

**Events Published:**

- `NotificationCreatedEvent`

**Events Consumed:**

- `ReturnInitiatedEvent` (from Returns Service)
- `DamageRecordedEvent` (from Returns Service)
- `ReturnReconciledEvent` (from Returns Service)

---

### Integration Service (Optional - D365 Integration)

**Responsibilities:**

- Send return data to D365 (if integration enabled)
- Handle D365 API errors with retry logic
- Track D365 reconciliation status

**Events Consumed:**

- `ReturnReconciledEvent` (from Returns Service)

---

## Testing Strategy

### Unit Testing

**Domain Core:**

- Test aggregate behavior (Return.initiatePartialReturn(), Return.processFullReturn(), DamageAssessment.classify())
- Test business rules (return quantities cannot exceed order quantities, damaged stock must be quarantined)
- Test event publishing (correct events published on state changes)
- Pure Java testing (JUnit 5, AssertJ)

**Application Service:**

- Test command handlers (HandlePartialOrderAcceptanceCommandHandler, ProcessFullOrderReturnCommandHandler)
- Test query handlers (GetReturnQueryHandler, ListReturnsQueryHandler)
- Mock port dependencies
- Verify event publishing

**Infrastructure:**

- Test repository adapters
- Test event publishers and listeners
- Test REST controllers
- Integration tests with Testcontainers

### Integration Testing

**Gateway API Tests:**

- Test partial order acceptance endpoint
- Test full order return endpoint
- Test damage assessment endpoint
- Test return location assignment
- Test D365 reconciliation (if enabled)
- Validate end-to-end flows
- Test error scenarios

**Event Flow Tests:**

- Test event publishing and consumption
- Test event correlation
- Test idempotency
- Test error handling

### Frontend Testing

**Component Tests:**

- Test partial order acceptance component
- Test full order return component
- Test damage assessment component
- Test return location display

**E2E Tests:**

- Test complete returns workflow
- Test damage handling workflow
- Test D365 reconciliation workflow

---

## Implementation Order

### Phase 1: Process Full Order Return (Day 1-3)

**Rationale:** Foundation for all returns operations

1. Implement full order return UI (US-7.2.1)
2. Implement `ProcessFullOrderReturnCommand` and handler
3. Implement `Return.processFullReturn()` in domain core
4. Add validation for returned products against order
5. Create event publishers
6. Write gateway API tests

### Phase 2: Handle Partial Order Acceptance (Day 4-5)

**Rationale:** Build on full return foundation

1. Implement partial order acceptance UI (US-7.1.1)
2. Implement `HandlePartialOrderAcceptanceCommand` and handler
3. Implement `Return.initiatePartialReturn()` in domain core
4. Add quantity calculations for partial returns
5. Write gateway API tests

### Phase 3: Handle Damage-in-Transit Returns (Day 6-7)

**Rationale:** Extend returns with damage handling

1. Implement damage assessment UI (US-7.3.1)
2. Implement `RecordDamageCommand` and handler
3. Implement `DamageAssessment.create()` in domain core
4. Add damage classification logic
5. Create damage report generation
6. Write gateway API tests

### Phase 4: Assign Return Location (Day 8-9)

**Rationale:** Complete returns workflow with location assignment

1. Implement return location assignment logic (US-7.4.1)
2. Integrate with Location Management Service
3. Implement FEFO principles for return location assignment
4. Add quarantine location logic for damaged stock
5. Write unit and integration tests

### Phase 5: Reconcile with D365 (Day 10)

**Rationale:** Optional D365 integration

1. Implement D365 reconciliation logic (US-7.5.1)
2. Create Integration Service event listener
3. Implement retry logic and error handling
4. Optional: Test with D365 sandbox (if available)
5. Write gateway API tests

---

## Dependencies and Prerequisites

### Prerequisites from Previous Sprints

**Sprint 1-4:**

- ✅ Stock Management Service operational
- ✅ Location Management Service operational
- ✅ Product Service operational

**Sprint 5:**

- ✅ Picking lists created
- ✅ Orders mapped to loads

**Sprint 6:**

- ✅ Picking tasks executed
- ✅ Picking lists completed

### External Dependencies

- **Picking Service:** Order validation and picking completion status
- **Stock Management Service:** Stock level updates
- **Location Management Service:** Location assignment and capacity
- **Product Service:** Product validation
- **Notification Service:** Return notifications

### Technical Dependencies

- **Kafka:** Event streaming
- **PostgreSQL:** Database storage
- **Spring Boot:** Application framework
- **React:** Frontend framework
- **D365 (Optional):** ERP integration for returns reconciliation

### Common Value Objects Needed

Move to `common-domain` module if not already present:

- `ReturnReason` - Return reason enum (CUSTOMER_REJECTION, QUALITY_ISSUE, OVERSTOCK, DAMAGE, OTHER)
- `DamageType` - Damage type enum (CRUSHED, BROKEN, EXPIRED, CONTAMINATED, PACKAGING_DAMAGE, OTHER)
- `ProductCondition` - Product condition enum (GOOD, DAMAGED, EXPIRED, QUARANTINE, WRITE_OFF)
- `ReturnStatus` - Return status enum (INITIATED, PROCESSING, LOCATION_ASSIGNED, COMPLETED, RECONCILED)
- `CustomerInfo` - Customer information value object
- `OrderNumber` - Order identifier value object
- `LoadNumber` - Load identifier value object

---

## Risk Mitigation

### Risk 1: Return Quantity Validation

**Risk:** Returned quantities exceed original order quantities

**Mitigation:**

- Validate returned quantities against original order at domain level
- Display order quantities in UI for reference
- Alert on discrepancies
- Require manager approval for over-returns

### Risk 2: Damaged Stock Handling

**Risk:** Damaged stock incorrectly returned to picking locations

**Mitigation:**

- Mandatory damage assessment before location assignment
- Automatic quarantine for damaged stock
- Require quality team sign-off for damaged stock
- Visual indicators for damaged stock in UI

### Risk 3: Location Capacity

**Risk:** Return locations at capacity

**Mitigation:**

- Real-time capacity checking before assignment
- Overflow location strategy
- Alert warehouse managers when capacity low
- Support manual location override

### Risk 4: D365 Integration Failures (Optional)

**Risk:** Return reconciliation fails to reach D365

**Mitigation:**

- Implement retry logic with exponential backoff
- Use dead letter queue for persistent failures
- Alert on D365 integration errors
- Allow manual reconciliation submission

### Risk 5: Stock Re-availability

**Risk:** Returned stock not immediately available for re-picking

**Mitigation:**

- Event-driven stock availability updates
- Real-time notification to picking service
- Clear status indicators in stock queries
- Support for immediate re-picking after return

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

- **Planned Points:** 28
- **Completed Points:** TBD
- **Velocity:** TBD

### Quality Metrics

- **Test Coverage:** Target >80%
- **Code Review:** All code reviewed
- **Defect Rate:** <5% post-deployment

### Business Metrics

- **Returns Processing Time:** Target <5 minutes per return
- **Damage Assessment Accuracy:** Target >95%
- **Location Assignment Success Rate:** Target >98%
- **D365 Reconciliation Success:** Target >99% (if enabled)

---

## Appendix

### Related Documentation

- [User Story Breakdown](../../../06-project-management/User_Story_Breakdown.md)
- [Service Architecture Document](../../../01-architecture/Service_Architecture_Document.md)
- [Domain Model Design](../../../01-architecture/Domain_Model_Design.md)
- [Mandated Implementation Template Guide](../../../guide/mandated-Implementation-template-guide.md)

### Glossary

| Term                      | Definition                                                             |
|---------------------------|------------------------------------------------------------------------|
| **Partial Return**        | Return of some (not all) items from an order                           |
| **Full Return**           | Return of all items from an order                                      |
| **Damage Assessment**     | Evaluation of damaged stock to determine repairability and disposition |
| **Quarantine Location**   | Designated location for damaged or questionable stock                  |
| **Return Reconciliation** | Synchronization of return data with D365 ERP system                    |
| **FEFO**                  | First Expiring First Out - inventory rotation strategy                 |
| **Write-Off**             | Stock classified as unsalvageable and removed from inventory           |

---

**Document Control**

- **Version:** 1.0
- **Date:** 2026-01-09
- **Author:** System Architect
- **Status:** Draft
- **Review Cycle:** Sprint planning and retrospective
