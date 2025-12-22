# Sprint 3 Implementation Plan

## Warehouse Management System Integration - CCBSA LDP System

**Sprint:** Sprint 3 - Stock Classification  
**Duration:** 2 weeks  
**Sprint Goal:** Implement stock classification and FEFO location assignment  
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

Implement stock classification and FEFO location assignment capabilities to support warehouse operations. This sprint focuses on:

1. **Stock Classification** - Automatically classify stock by expiration dates
2. **FEFO Location Assignment** - Assign locations based on First Expiring First Out principles
3. **Location Assignment** - Assign locations to stock items
4. **Consignment Confirmation** - Confirm receipt of stock consignments

### Success Criteria

- ✅ Stock items are automatically classified by expiration dates
- ✅ Locations are assigned based on FEFO principles
- ✅ Stock items can be assigned to locations
- ✅ Consignments can be confirmed after receipt
- ✅ All data flows correctly from frontend through gateway to backend services
- ✅ Gateway API tests validate all endpoints
- ✅ All implementations follow DDD, Clean Hexagonal Architecture, CQRS, and Event-Driven Choreography principles

---

## User Stories

### Story 1: US-2.1.1 - Classify Stock by Expiration Date (5 points)

**Service:** Stock Management Service  
**Priority:** Must Have

**Acceptance Criteria:**

- System automatically assigns classification when stock item is created
- Classification categories: EXPIRED, CRITICAL (≤7 days), NEAR_EXPIRY (≤30 days), NORMAL, EXTENDED_SHELF_LIFE
- Classification is visible in all stock views and reports
- Classification updates automatically when expiration date changes
- System handles null expiration dates (non-perishable) as NORMAL

**Implementation Plan:** [01-Stock-Classification-Implementation-Plan.md](01-Stock-Classification-Implementation-Plan.md)

---

### Story 2: US-2.1.2 - Assign Locations Based on FEFO Principles (8 points)

**Service:** Location Management Service  
**Priority:** Must Have

**Acceptance Criteria:**

- System considers expiration date when assigning locations
- Locations closer to picking zones contain stock with earlier expiration dates
- System prevents picking of newer stock before older stock expires
- Visual indicators show expiration date priority in location views
- System supports multiple expiration date ranges for location assignment

**Implementation Plan:** [02-FEFO-Location-Assignment-Implementation-Plan.md](02-FEFO-Location-Assignment-Implementation-Plan.md)

---

### Story 3: US-3.2.1 - Assign Location to Stock (8 points)

**Service:** Stock Management Service, Location Management Service  
**Priority:** Must Have

**Acceptance Criteria:**

- System allows assignment of location to stock items
- System validates location availability and capacity
- System validates stock item exists and is in valid state
- System updates location status when stock is assigned
- System publishes events for location assignment
- System supports batch location assignment

**Implementation Plan:** [03-Assign-Location-to-Stock-Implementation-Plan.md](03-Assign-Location-to-Stock-Implementation-Plan.md)

---

### Story 4: US-1.1.6 - Confirm Consignment Receipt (5 points)

**Service:** Stock Management Service  
**Priority:** Must Have

**Acceptance Criteria:**

- System allows confirmation of received consignments
- System validates consignment is in RECEIVED status before confirmation
- System updates consignment status to CONFIRMED
- System records confirmation timestamp and user
- System publishes `StockConsignmentConfirmedEvent` after confirmation
- System triggers location assignment workflow after confirmation

**Implementation Plan:** [04-Confirm-Consignment-Receipt-Implementation-Plan.md](04-Confirm-Consignment-Receipt-Implementation-Plan.md)

---

## Implementation Approach

### Frontend-First Design

All implementation plans start with **production-grade UI design** to ensure:

1. **User Experience** - Intuitive, accessible, and responsive interfaces
2. **Data Validation** - Client-side validation for immediate feedback
3. **Error Handling** - Clear error messages and recovery paths
4. **Visual Indicators** - Clear display of stock classification and expiration dates
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

- **Synchronous Calls:** Location Management Service queried synchronously for location assignment
- **Circuit Breaker:** Resilience patterns for service calls
- **Event-Driven:** Stock classification triggers location assignment via events
- **Error Handling:** Graceful degradation when services unavailable

---

## Architecture Compliance

### Domain-Driven Design (DDD)

- **Bounded Contexts:** Clear service boundaries (Stock Management, Location Management)
- **Aggregates:** StockItem, StockConsignment, Location are aggregate roots
- **Value Objects:** ExpirationDate, StockClassification, LocationId, etc.
- **Domain Events:** StockClassifiedEvent, LocationAssignedEvent, StockConsignmentConfirmedEvent
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

### Stock Classification Flow

```
Stock Consignment Confirmed Event
  ↓
Stock Management Service (Event Listener)
  ↓
Create StockItem for each line item
  ↓
Classify Stock by Expiration Date (Domain Logic)
  ↓
StockClassifiedEvent Published
  ↓
Location Management Service (Event Listener)
  ↓
FEFO Location Assignment
  ↓
LocationAssignedEvent Published
  ↓
Stock Management Service (Event Listener)
  ↓
Update StockItem with Location
```

### FEFO Location Assignment Flow

```
StockClassifiedEvent
  ↓
Location Management Service (Event Listener)
  ↓
Query Available Locations (sorted by proximity to picking zone)
  ↓
Query Stock Items by Expiration Date (sorted by expiration date)
  ↓
Match Stock Items to Locations (FEFO algorithm)
  ↓
Assign Location to Stock Item
  ↓
LocationAssignedEvent Published
  ↓
Stock Management Service (Event Listener)
  ↓
Update StockItem with Location
```

### Assign Location to Stock Flow

```
Frontend (React)
  ↓ POST /api/v1/stock-management/stock-items/{id}/assign-location
Gateway Service
  ↓ Route to Stock Management Service
Stock Management Service (Command Controller)
  ↓ AssignLocationToStockCommand
Command Handler
  ↓ Validate Stock Item exists and is in valid state
  ↓ Query Location Management Service (synchronous)
  ↓ Validate Location availability and capacity
  ↓ StockItem.assignLocation()
  Domain Core (StockItem Aggregate)
  ↓ LocationAssignedEvent
Event Publisher
  ↓ Kafka Topic: stock-management-events
Location Management Service (Event Listener)
  ↓ Update Location status and capacity
  ↓ LocationStatusChangedEvent
Query Handler
  ↓ StockItemQueryResult
Query Controller
  ↓ Response
Gateway Service
  ↓ Response
Frontend (React)
```

### Confirm Consignment Receipt Flow

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
  ↓ Classify Stock by Expiration Date
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

## Service Segregation

### Stock Management Service

**Responsibilities:**

- Classify stock by expiration dates
- Create stock items from consignment line items
- Assign locations to stock items
- Confirm consignment receipts
- Track stock classification and expiration dates
- Publish stock classification events

**Database:** `stock_management_db`  
**Events Published:**

- `StockConsignmentConfirmedEvent`
- `StockClassifiedEvent`
- `LocationAssignedEvent` (for stock items)
- `StockExpiringAlertEvent`

**Events Consumed:**

- `StockConsignmentReceivedEvent` (from self, when consignment is created)
- `LocationAssignedEvent` (from Location Management Service, when location is assigned)

**Service Dependencies:**

- **Location Management Service** - Synchronous calls for location assignment validation

---

### Location Management Service

**Responsibilities:**

- Assign locations based on FEFO principles
- Update location status and capacity
- Track location availability
- Publish location assignment events

**Database:** `location_management_db`  
**Events Published:**

- `LocationAssignedEvent`
- `LocationStatusChangedEvent`
- `LocationCapacityExceededEvent`

**Events Consumed:**

- `StockConsignmentConfirmedEvent` (from Stock Management Service)
- `StockClassifiedEvent` (from Stock Management Service)

**Service Dependencies:**

- None (provides location assignment services)

---

## Testing Strategy

### Unit Tests

- **Domain Core:** Business logic validation (classification, FEFO algorithm)
- **Application Service:** Command/query handler logic
- **Data Access:** Repository adapter behavior
- **Messaging:** Event publisher/listener logic
- **Validation:** Location assignment rules, FEFO algorithm

### Integration Tests

- **Service Integration:** End-to-end service operations
- **Database Integration:** Repository operations with real database
- **Kafka Integration:** Event publishing and consumption
- **Location Management Service Integration:** Location assignment calls
- **FEFO Algorithm:** Test location assignment based on expiration dates

### Gateway API Tests

**Purpose:** Mimic frontend calls to backend through gateway

**Test Structure:**

- Base test class with authentication setup
- Test data builders for realistic test data
- Request/response validation
- Error scenario testing

**Test Coverage:**

- Stock classification through gateway
- FEFO location assignment through gateway
- Assign location to stock through gateway
- Confirm consignment receipt through gateway
- Error handling and validation
- Authentication and authorization

**Implementation Plan:** [05-Gateway-API-Tests-Implementation-Plan.md](05-Gateway-API-Tests-Implementation-Plan.md)

---

## Implementation Order

### Phase 1: Stock Classification Domain Model (Days 1-2)

1. **Stock Management Service Domain Core**
    - StockItem aggregate root (if not exists)
    - ExpirationDate value object (move to common-domain if shared)
    - StockClassification enum (move to common-domain if shared)
    - StockClassifiedEvent
    - Business logic for classification

### Phase 2: FEFO Location Assignment Domain Model (Days 3-4)

1. **Location Management Service Domain Core**
    - FEFO assignment algorithm
    - Location assignment business logic
    - LocationAssignedEvent updates
    - Location capacity validation

### Phase 3: Backend Application Services (Days 5-7)

1. **Stock Management Application Service**
    - ClassifyStockCommandHandler
    - AssignLocationToStockCommandHandler
    - ConfirmConsignmentCommandHandler
    - GetStockItemQueryHandler
    - Repository ports
    - Event publisher ports
    - Location service port (for location assignment)

2. **Location Management Application Service**
    - AssignLocationFEFOCommandHandler
    - GetAvailableLocationsQueryHandler
    - Location assignment algorithm
    - Event publisher ports

### Phase 4: Backend Infrastructure (Days 8-10)

1. **Stock Management Data Access**
    - StockItemEntity (JPA) - if not exists
    - StockItemRepositoryAdapter
    - Entity mappers
    - Database migrations

2. **Location Management Service Integration**
    - LocationServicePort interface
    - LocationServiceAdapter (REST client)
    - Circuit breaker configuration

3. **Messaging**
    - Event publishers
    - Event listeners (for cross-service communication)
    - Kafka configuration

### Phase 5: Backend REST API (Days 11-12)

1. **Stock Management REST API**
    - StockItemCommandController
    - StockItemQueryController
    - ConsignmentCommandController (confirm endpoint)
    - DTOs and mappers
    - Exception handlers

2. **Location Management REST API**
    - LocationAssignmentController
    - DTOs and mappers

### Phase 6: Frontend Implementation (Days 13-15)

1. **Stock Classification UI**
    - Stock classification display
    - Expiration date indicators
    - Classification filters

2. **Location Assignment UI**
    - Location assignment form
    - FEFO location suggestions
    - Location availability display

3. **Consignment Confirmation UI**
    - Consignment confirmation button
    - Confirmation status display
    - Stock item list after confirmation

### Phase 7: Gateway API Tests (Day 16)

1. **Stock Classification Tests**
    - Classification endpoint tests
    - Error scenario tests

2. **Location Assignment Tests**
    - FEFO assignment tests
    - Location assignment tests
    - Error scenario tests

3. **Consignment Confirmation Tests**
    - Confirmation endpoint tests
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
- ✅ **Location Management Service** - Location master data (already running)
- ✅ **Stock Management Service** - Stock consignment (already running from Sprint 2)

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

1. **FEFO Algorithm Complexity**
    - **Risk:** FEFO algorithm may be complex to implement correctly
    - **Mitigation:** Start with simple algorithm, iterate based on feedback
    - **Contingency:** Use rule-based assignment with configurable thresholds

2. **Location Service Integration**
    - **Risk:** Synchronous calls to Location Service may cause latency
    - **Mitigation:** Implement caching and circuit breaker patterns
    - **Contingency:** Use async assignment with eventual consistency

3. **Stock Classification Performance**
    - **Risk:** Classification of large batches may cause performance issues
    - **Mitigation:** Implement batch processing and async classification
    - **Contingency:** Use background job for classification

### Integration Risks

1. **Service-to-Service Communication**
    - **Risk:** Location Service calls may fail
    - **Mitigation:** Implement resilience patterns (circuit breaker, retry)
    - **Contingency:** Graceful degradation with assignment warnings

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
- [ ] FEFO algorithm implemented and tested
- [ ] Unit tests written (80%+ coverage)
- [ ] Integration tests written
- [ ] Code reviewed and approved

### Frontend

- [ ] All UI components implemented
- [ ] All API integrations implemented
- [ ] Client-side validation implemented
- [ ] Error handling implemented
- [ ] Stock classification display implemented
- [ ] Location assignment UI implemented
- [ ] Consignment confirmation UI implemented
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

