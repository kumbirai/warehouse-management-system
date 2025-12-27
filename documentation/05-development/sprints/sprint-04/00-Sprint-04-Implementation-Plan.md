# Sprint 4 Implementation Plan

## Warehouse Management System Integration - CCBSA LDP System

**Sprint:** Sprint 4 - Stock Movement and Levels
**Duration:** 2 weeks
**Sprint Goal:** Track stock movements, monitor stock levels, allocate stock, and adjust stock levels
**Total Story Points:** 41

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

Implement stock movement tracking, stock level monitoring, stock allocation, and stock adjustment capabilities to support warehouse operations. This sprint focuses on:

1. **Stock Movement Tracking** - Track all stock movements between locations
2. **Initiate Stock Movement** - Allow operators to initiate stock movements
3. **Manage Location Status** - Update location status and capacity management
4. **Monitor Stock Levels** - Real-time stock level monitoring and reporting
5. **Enforce Min/Max Stock Levels** - Maintain stock levels within thresholds
6. **Allocate Stock for Picking** - Reserve stock for picking orders using FEFO
7. **Adjust Stock Levels** - Manual stock adjustments for corrections

### Success Criteria

- ✅ All stock movements are tracked with complete audit trail
- ✅ Operators can initiate stock movements between locations
- ✅ Location status and capacity are updated in real-time
- ✅ Stock levels are monitored in real-time across all locations
- ✅ Minimum and maximum stock levels are enforced automatically
- ✅ Stock can be allocated for picking orders with FEFO compliance
- ✅ Stock levels can be manually adjusted with proper authorization
- ✅ All data flows correctly from frontend through gateway to backend services
- ✅ Gateway API tests validate all endpoints
- ✅ All implementations follow DDD, Clean Hexagonal Architecture, CQRS, and Event-Driven Choreography principles

---

## User Stories

### Story 1: US-3.3.1 - Track Stock Movement (8 points)

**Service:** Location Management Service, Stock Management Service
**Priority:** Must Have

**Acceptance Criteria:**

- System tracks movement from receiving to storage location
- System tracks movement from storage location to picking location
- System tracks movement between storage locations
- System tracks movement from picking location to shipping
- Each movement records: timestamp, user, source location, destination location, quantity, reason
- System maintains complete audit trail
- System publishes `StockMovementCompletedEvent`

**Implementation Plan:** [01-Track-Stock-Movement-Implementation-Plan.md](01-Track-Stock-Movement-Implementation-Plan.md)

---

### Story 2: US-3.3.2 - Initiate Stock Movement (5 points)

**Service:** Location Management Service, Stock Management Service
**Priority:** Must Have

**Acceptance Criteria:**

- System allows selecting source and destination locations
- System validates source location has sufficient stock
- System validates destination location has capacity
- System requires movement reason (PICKING, RESTOCKING, REORGANIZATION, etc.)
- System publishes `StockMovementInitiatedEvent`
- System allows canceling initiated movements

**Implementation Plan:** [02-Initiate-Stock-Movement-Implementation-Plan.md](02-Initiate-Stock-Movement-Implementation-Plan.md)

---

### Story 3: US-3.4.1 - Manage Location Status (5 points)

**Service:** Location Management Service
**Priority:** Must Have

**Acceptance Criteria:**

- System tracks location status: OCCUPIED, AVAILABLE, RESERVED, BLOCKED
- System updates location status in real-time
- System tracks location capacity (current vs maximum)
- System allows blocking locations for maintenance or issues
- System prevents assignment to blocked locations
- System publishes `LocationStatusChangedEvent`

**Implementation Plan:** [03-Manage-Location-Status-Implementation-Plan.md](03-Manage-Location-Status-Implementation-Plan.md)

---

### Story 4: US-5.1.1 - Monitor Stock Levels (5 points)

**Service:** Stock Management Service
**Priority:** Must Have

**Acceptance Criteria:**

- System calculates stock levels in real-time
- Stock level visibility in dashboards
- Historical stock level tracking
- Stock level reports by product, location, warehouse
- System updates stock levels on stock movements, picking, returns

**Implementation Plan:** [04-Monitor-Stock-Levels-Implementation-Plan.md](04-Monitor-Stock-Levels-Implementation-Plan.md)

---

### Story 5: US-5.1.2 - Enforce Minimum and Maximum Stock Levels (5 points)

**Service:** Stock Management Service
**Priority:** Must Have

**Acceptance Criteria:**

- System maintains minimum and maximum levels per product
- Levels may vary by location or warehouse
- System prevents stock levels from exceeding maximum
- System alerts when stock approaches minimum
- System validates thresholds when updating stock levels

**Implementation Plan:** [05-Enforce-Min-Max-Stock-Levels-Implementation-Plan.md](05-Enforce-Min-Max-Stock-Levels-Implementation-Plan.md)

---

### Story 6: US-5.2.1 - Allocate Stock for Picking Orders (8 points)

**Service:** Stock Management Service
**Priority:** Must Have

**Acceptance Criteria:**

- System allows allocating stock by product and location
- System validates sufficient available stock before allocation
- System supports allocation types: PICKING_ORDER, RESERVATION, OTHER
- System tracks allocated quantity separately from available quantity
- System prevents allocation exceeding available stock
- System supports FEFO-based allocation (allocates earliest expiring stock first)
- System publishes `StockAllocatedEvent` after successful allocation
- System allows querying allocations by reference ID (e.g., order ID)

**Implementation Plan:** [06-Allocate-Stock-for-Picking-Implementation-Plan.md](06-Allocate-Stock-for-Picking-Implementation-Plan.md)

---

### Story 7: US-5.2.2 - Adjust Stock Levels (5 points)

**Service:** Stock Management Service
**Priority:** Must Have

**Acceptance Criteria:**

- System allows increasing stock levels (positive adjustment)
- System allows decreasing stock levels (negative adjustment)
- System requires adjustment reason (STOCK_COUNT, DAMAGE, CORRECTION, etc.)
- System prevents negative stock levels after adjustment
- System records adjustment timestamp, user, and reason
- System publishes `StockAdjustedEvent` after successful adjustment
- System maintains audit trail of all adjustments
- System supports adjustments at consignment level or stock item level

**Implementation Plan:** [07-Adjust-Stock-Levels-Implementation-Plan.md](07-Adjust-Stock-Levels-Implementation-Plan.md)

---

## Implementation Approach

### Frontend-First Design

All implementation plans start with **production-grade UI design** to ensure:

1. **User Experience** - Intuitive, accessible, and responsive interfaces
2. **Data Validation** - Client-side validation for immediate feedback
3. **Error Handling** - Clear error messages and recovery paths
4. **Visual Indicators** - Clear display of stock levels, movements, and allocations
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

- **Synchronous Calls:** Location Management Service queried synchronously for location validation
- **Circuit Breaker:** Resilience patterns for service calls
- **Event-Driven:** Stock movements and allocations trigger events for downstream processing
- **Error Handling:** Graceful degradation when services unavailable

---

## Architecture Compliance

### Domain-Driven Design (DDD)

- **Bounded Contexts:** Clear service boundaries (Stock Management, Location Management)
- **Aggregates:** StockMovement, StockAllocation, StockAdjustment, StockLevel are aggregate roots
- **Value Objects:** Quantity, MovementReason, AdjustmentReason, AllocationType, etc.
- **Domain Events:** StockMovementCompletedEvent, StockAllocatedEvent, StockAdjustedEvent, LocationStatusChangedEvent
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

### Stock Movement Flow

```
Frontend (React)
  ↓ POST /api/v1/location-management/stock-movements
Gateway Service
  ↓ Route to Location Management Service
Location Management Service (Command Controller)
  ↓ CreateStockMovementCommand
Command Handler
  ↓ Validate Stock Item exists and has sufficient quantity
  ↓ Query Location Management for source and destination validation
  ↓ Validate source location has stock
  ↓ Validate destination location has capacity
  ↓ StockMovement.create()
  Domain Core (StockMovement Aggregate)
  ↓ StockMovementInitiatedEvent
Event Publisher
  ↓ Kafka Topic: location-management-events
Location Management Service (Event Listener)
  ↓ Update source and destination location capacity
  ↓ LocationStatusChangedEvent (if needed)
Stock Management Service (Event Listener)
  ↓ Update stock item location
  ↓ StockMovementCompletedEvent
Query Handler
  ↓ StockMovementQueryResult
Query Controller
  ↓ Response
Gateway Service
  ↓ Response
Frontend (React)
```

### Stock Level Monitoring Flow

```
Stock Level Query Request
  ↓
Stock Management Service (Query Controller)
  ↓ GetStockLevelsByProductAndLocationQuery
Query Handler
  ↓ Query from read model (data port)
  ↓ Aggregate stock levels by product/location
  ↓ Calculate available vs allocated quantities
  ↓ StockLevelQueryResult
Query Controller
  ↓ Response
```

### Stock Allocation Flow

```
Frontend (React)
  ↓ POST /api/v1/stock-management/allocations
Gateway Service
  ↓ Route to Stock Management Service
Stock Management Service (Command Controller)
  ↓ AllocateStockCommand
Command Handler
  ↓ Validate Stock Item exists and is available
  ↓ Query stock items by product with FEFO sorting
  ↓ Validate sufficient available stock (total - allocated)
  ↓ Create StockAllocation aggregate
  ↓ StockAllocation.allocate()
  Domain Core (StockAllocation Aggregate)
  ↓ StockAllocatedEvent
Event Publisher
  ↓ Kafka Topic: stock-management-events
Stock Management Service (Event Listener)
  ↓ Update stock item allocated quantity
Picking Service (Event Listener - future)
  ↓ Create picking task
Query Handler
  ↓ StockAllocationQueryResult
Query Controller
  ↓ Response
Gateway Service
  ↓ Response
Frontend (React)
```

### Stock Adjustment Flow

```
Frontend (React)
  ↓ POST /api/v1/stock-management/adjustments
Gateway Service
  ↓ Route to Stock Management Service
Stock Management Service (Command Controller)
  ↓ AdjustStockCommand
Command Handler
  ↓ Validate Stock Item exists
  ↓ Validate adjustment doesn't result in negative quantity
  ↓ Create StockAdjustment aggregate
  ↓ StockAdjustment.adjust()
  Domain Core (StockAdjustment Aggregate)
  ↓ StockAdjustedEvent
Event Publisher
  ↓ Kafka Topic: stock-management-events
Stock Management Service (Event Listener)
  ↓ Update stock item quantity
  ↓ Trigger reclassification if needed
Query Handler
  ↓ StockAdjustmentQueryResult
Query Controller
  ↓ Response
Gateway Service
  ↓ Response
Frontend (React)
```

---

## Service Segregation

### Stock Management Service

**Responsibilities:**

- Monitor stock levels in real-time
- Enforce minimum and maximum stock levels
- Allocate stock for picking orders
- Adjust stock levels manually
- Track stock item quantities and allocations
- Publish stock level events

**Database:** `stock_management_db`
**Events Published:**

- `StockAllocatedEvent`
- `StockAdjustedEvent`
- `StockLevelBelowMinimumEvent`
- `StockLevelAboveMaximumEvent`
- `StockMovementCompletedEvent` (when stock item location updated)

**Events Consumed:**

- `StockMovementInitiatedEvent` (from Location Management Service)
- `StockMovementCompletedEvent` (from Location Management Service)
- `LocationStatusChangedEvent` (from Location Management Service)

**Service Dependencies:**

- **Location Management Service** - Synchronous calls for location validation

---

### Location Management Service

**Responsibilities:**

- Track stock movements between locations
- Initiate stock movements
- Manage location status and capacity
- Update location availability
- Publish location and movement events

**Database:** `location_management_db`
**Events Published:**

- `StockMovementInitiatedEvent`
- `StockMovementCompletedEvent`
- `LocationStatusChangedEvent`
- `LocationCapacityExceededEvent`

**Events Consumed:**

- `StockAllocatedEvent` (from Stock Management Service - to reserve capacity)
- `StockAdjustedEvent` (from Stock Management Service - to update capacity)

**Service Dependencies:**

- **Stock Management Service** - Synchronous calls for stock validation

---

## Testing Strategy

### Unit Tests

- **Domain Core:** Business logic validation (movement, allocation, adjustment)
- **Application Service:** Command/query handler logic
- **Data Access:** Repository adapter behavior
- **Messaging:** Event publisher/listener logic
- **Validation:** Stock level rules, allocation rules, movement rules

### Integration Tests

- **Service Integration:** End-to-end service operations
- **Database Integration:** Repository operations with real database
- **Kafka Integration:** Event publishing and consumption
- **Location Management Service Integration:** Location validation calls
- **FEFO Allocation:** Test allocation based on expiration dates

### Gateway API Tests

**Purpose:** Mimic frontend calls to backend through gateway

**Test Structure:**

- Base test class with authentication setup
- Test data builders for realistic test data
- Request/response validation
- Error scenario testing

**Test Coverage:**

- Stock movement initiation through gateway
- Stock level queries through gateway
- Stock allocation through gateway
- Stock adjustment through gateway
- Location status management through gateway
- Error handling and validation
- Authentication and authorization

**Implementation Plan:** [08-Gateway-API-Tests-Implementation-Plan.md](08-Gateway-API-Tests-Implementation-Plan.md)

---

## Implementation Order

### Phase 1: Stock Movement Domain Model (Days 1-2)

1. **Location Management Service Domain Core**
    - StockMovement aggregate root
    - MovementReason enum (move to common-domain)
    - MovementStatus enum
    - StockMovementInitiatedEvent
    - StockMovementCompletedEvent
    - Business logic for movement validation

2. **Stock Management Service Domain Core**
    - Update StockItem to track location changes
    - StockMovementCompletedEvent handler

### Phase 2: Stock Level and Allocation Domain Model (Days 3-4)

1. **Stock Management Service Domain Core**
    - StockAllocation aggregate root
    - StockAdjustment aggregate root
    - AllocationType enum (move to common-domain)
    - AdjustmentReason enum (move to common-domain)
    - MinimumQuantity value object (move to common-domain)
    - MaximumQuantity value object (move to common-domain)
    - StockAllocatedEvent
    - StockAdjustedEvent
    - StockLevelBelowMinimumEvent
    - StockLevelAboveMaximumEvent

### Phase 3: Location Status Domain Model (Days 5)

1. **Location Management Service Domain Core**
    - Update Location aggregate with status management
    - LocationStatus enum (already exists, enhance if needed)
    - LocationCapacity value object enhancements
    - LocationStatusChangedEvent enhancements

### Phase 4: Backend Application Services (Days 6-9)

1. **Location Management Application Service**
    - CreateStockMovementCommandHandler
    - CompleteStockMovementCommandHandler
    - CancelStockMovementCommandHandler
    - UpdateLocationStatusCommandHandler
    - GetStockMovementQueryHandler
    - ListStockMovementsQueryHandler
    - Repository ports
    - Event publisher ports

2. **Stock Management Application Service**
    - AllocateStockCommandHandler
    - ReleaseStockAllocationCommandHandler
    - AdjustStockCommandHandler
    - GetStockLevelsByProductAndLocationQueryHandler
    - GetStockAllocationsQueryHandler
    - GetStockAdjustmentsQueryHandler
    - Repository ports
    - Event publisher ports
    - Location service port (for location validation)

### Phase 5: Backend Infrastructure (Days 10-12)

1. **Location Management Data Access**
    - StockMovementEntity (JPA)
    - StockMovementRepositoryAdapter
    - Entity mappers
    - Database migrations

2. **Stock Management Data Access**
    - StockAllocationEntity (JPA)
    - StockAdjustmentEntity (JPA)
    - StockAllocationRepositoryAdapter
    - StockAdjustmentRepositoryAdapter
    - Entity mappers
    - Database migrations
    - Update StockItemEntity to track allocated quantity

3. **Messaging**
    - Event publishers
    - Event listeners (for cross-service communication)
    - Kafka configuration

### Phase 6: Backend REST API (Days 13-14)

1. **Location Management REST API**
    - StockMovementCommandController
    - StockMovementQueryController
    - LocationCommandController (status management)
    - DTOs and mappers
    - Exception handlers

2. **Stock Management REST API**
    - StockAllocationCommandController
    - StockAllocationQueryController
    - StockAdjustmentCommandController
    - StockAdjustmentQueryController
    - StockLevelQueryController
    - DTOs and mappers
    - Exception handlers

### Phase 7: Frontend Implementation (Days 15-17)

1. **Stock Movement UI**
    - Stock movement initiation form
    - Stock movement tracking display
    - Movement history timeline
    - Movement reason selection

2. **Stock Level Monitoring UI**
    - Stock level dashboard
    - Stock level alerts
    - Min/Max threshold configuration
    - Stock level trends charts

3. **Stock Allocation UI**
    - Stock allocation form
    - Allocation status display
    - FEFO allocation suggestions
    - Available vs allocated quantity display

4. **Stock Adjustment UI**
    - Stock adjustment form
    - Adjustment reason selection
    - Adjustment history display
    - Authorization checks

5. **Location Status Management UI**
    - Location status display
    - Location capacity indicators
    - Block/unblock location controls
    - Capacity threshold warnings

### Phase 8: Gateway API Tests (Days 18-20)

1. **Stock Movement Tests**
    - Movement initiation tests
    - Movement completion tests
    - Movement cancellation tests
    - Error scenario tests

2. **Stock Level Tests**
    - Stock level query tests
    - Min/Max enforcement tests
    - Error scenario tests

3. **Stock Allocation Tests**
    - Allocation creation tests
    - FEFO allocation tests
    - Release allocation tests
    - Error scenario tests

4. **Stock Adjustment Tests**
    - Adjustment creation tests
    - Negative quantity prevention tests
    - Error scenario tests

5. **Location Status Tests**
    - Status update tests
    - Capacity management tests
    - Error scenario tests

---

## Dependencies and Prerequisites

### Infrastructure Dependencies

- ✅ **Eureka Server** - Service discovery (already running)
- ✅ **Gateway Service** - API routing (already running)
- ✅ **Kafka** - Event streaming (must be running)
- ✅ **PostgreSQL** - Database (must be running)
- ✅ **Keycloak** - Authentication (already configured)

### Service Dependencies

- ✅ **Tenant Service** - Tenant validation (already running)
- ✅ **User Service** - User authentication (already running)
- ✅ **Product Service** - Product master data (already running)
- ✅ **Location Management Service** - Location master data (already running from Sprint 1)
- ✅ **Stock Management Service** - Stock consignment (already running from Sprints 2-3)

### Common Module Dependencies

- ✅ **common-domain** - Base classes (already available)
- ✅ **common-messaging** - Event infrastructure (already available)
- ✅ **common-application** - API response utilities (already available)
- ✅ **common-infrastructure** - Database configuration (already available)

### Development Dependencies

- ✅ **Java 21** - Development environment
- ✅ **Maven 3.8+** - Build tool
- ✅ **Node.js 18+** - Frontend development
- ✅ **Docker** - Local infrastructure

---

## Risk Mitigation

### Technical Risks

1. **Stock Level Calculation Performance**
    - **Risk:** Real-time stock level calculations may cause performance issues
    - **Mitigation:** Implement caching and read models for stock levels
    - **Contingency:** Use background jobs for level recalculation

2. **Stock Allocation Concurrency**
    - **Risk:** Concurrent allocations may cause over-allocation
    - **Mitigation:** Use optimistic locking and transaction isolation
    - **Contingency:** Implement allocation queue with sequential processing

3. **Location Service Integration**
    - **Risk:** Synchronous calls to Location Service may cause latency
    - **Mitigation:** Implement caching and circuit breaker patterns
    - **Contingency:** Use async validation with eventual consistency

### Integration Risks

1. **Service-to-Service Communication**
    - **Risk:** Location Service calls may fail
    - **Mitigation:** Implement resilience patterns (circuit breaker, retry)
    - **Contingency:** Graceful degradation with movement warnings

2. **Event-Driven Coordination**
    - **Risk:** Events may not be processed in correct order
    - **Mitigation:** Use event correlation IDs and idempotency
    - **Contingency:** Implement event replay mechanism

---

## Definition of Done

### Backend

- [ ] All domain models implemented with business logic
- [ ] All command/query handlers implemented
- [ ] All REST API endpoints implemented
- [ ] All database migrations created and tested
- [ ] All events published correctly
- [ ] Location Service integration implemented with resilience patterns
- [ ] Stock allocation with FEFO implemented and tested
- [ ] Optimistic locking implemented for allocations
- [ ] Unit tests written (80%+ coverage)
- [ ] Integration tests written
- [ ] Code reviewed and approved

### Frontend

- [ ] All UI components implemented
- [ ] All API integrations implemented
- [ ] Client-side validation implemented
- [ ] Error handling implemented
- [ ] Stock movement UI implemented
- [ ] Stock level dashboard implemented
- [ ] Stock allocation UI implemented
- [ ] Stock adjustment UI implemented
- [ ] Location status management UI implemented
- [ ] Responsive design verified
- [ ] Accessibility compliance verified (WCAG 2.1 Level AA)
- [ ] Unit tests written
- [ ] Code reviewed and approved

### Gateway API Tests

- [ ] All endpoint tests written
- [ ] All error scenario tests written
- [ ] All tests passing
- [ ] Test coverage documented

### Documentation

- [ ] API documentation updated (OpenAPI/Swagger)
- [ ] Service READMEs updated
- [ ] Architecture diagrams updated
- [ ] Implementation plans completed

---

## Next Steps

1. Review this plan with the development team
2. Assign stories to developers
3. Set up development branches
4. Begin Phase 1 implementation
5. Daily standups to track progress
6. Sprint review at completion

---

**Document Control**

- **Version:** 1.0
- **Date:** 2025-01
- **Status:** Draft
- **Next Review:** Sprint planning meeting
