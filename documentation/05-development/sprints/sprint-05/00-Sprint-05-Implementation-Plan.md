# Sprint 5 Implementation Plan

## Warehouse Management System Integration - CCBSA LDP System

**Sprint:** Sprint 5 - Picking Operations
**Duration:** 2 weeks
**Sprint Goal:** Enable picking list management and execution
**Total Story Points:** 39

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

Implement picking list management to enable warehouse operators to efficiently process picking operations. This sprint focuses on:

1. **Upload Picking List via CSV** - Bulk import of picking lists
2. **Manual Picking List Entry** - Individual picking list creation via UI
3. **Create Picking List** - Domain model for picking lists, loads, and orders
4. **Plan Picking Locations** - Optimize picking locations based on FEFO principles
5. **Map Orders to Loads** - Manage order-to-load relationships

### Success Criteria

- ✅ Operators can upload picking lists via CSV file
- ✅ Operators can manually create picking lists through the UI
- ✅ Picking lists are created with proper validation and structure
- ✅ Picking locations are optimized based on FEFO principles
- ✅ Orders are correctly mapped to loads with proper relationships
- ✅ All data flows correctly from frontend through gateway to backend services
- ✅ Gateway API tests validate all endpoints
- ✅ All implementations follow DDD, Clean Hexagonal Architecture, CQRS, and Event-Driven Choreography principles

---

## User Stories

### Story 1: US-6.1.1 - Upload Picking List via CSV File (8 points)

**Service:** Picking Service
**Priority:** Must Have

**Acceptance Criteria:**

- System accepts CSV file uploads through web interface
- CSV format includes: load number, order numbers, customer information, products, quantities, priorities
- System validates CSV file format and required columns before processing
- System provides clear error messages for invalid CSV data
- System processes CSV file and creates picking list records
- System displays upload progress and completion status
- System supports CSV file sizes up to 10MB
- System logs all CSV upload events for audit
- System publishes `PickingListReceivedEvent`

**Implementation Plan:** [01-Upload-Picking-List-CSV-Implementation-Plan.md](01-Upload-Picking-List-CSV-Implementation-Plan.md)

---

### Story 2: US-6.1.2 - Manual Picking List Entry via UI (8 points)

**Service:** Picking Service
**Priority:** Must Have

**Acceptance Criteria:**

- System provides form-based UI for picking list data entry
- Form includes fields: load number, order numbers, customer information, products, quantities, priorities
- System validates required fields and data formats in real-time
- System supports adding multiple orders to a single load
- System supports adding multiple products per order
- System validates product codes against master data
- System provides autocomplete/suggestions for product codes and customer information
- System allows saving draft picking lists for later completion
- System provides clear validation error messages
- System publishes `PickingListReceivedEvent` after successful entry

**Implementation Plan:** [02-Manual-Picking-List-Entry-Implementation-Plan.md](02-Manual-Picking-List-Entry-Implementation-Plan.md)

---

### Story 3: US-6.1.4 - Create Picking List (5 points)

**Service:** Picking Service
**Priority:** Must Have

**Acceptance Criteria:**

- System creates `PickingList` aggregate with picking list reference
- System creates `Load` aggregate containing multiple orders
- System creates `Order` entities within load
- System sets picking list status to "RECEIVED"
- System publishes `PickingListReceivedEvent`

**Implementation Plan:** [03-Create-Picking-List-Implementation-Plan.md](03-Create-Picking-List-Implementation-Plan.md)

---

### Story 4: US-6.2.1 - Plan Picking Locations (13 points)

**Service:** Picking Service, Stock Management Service
**Priority:** Must Have

**Acceptance Criteria:**

- System optimizes picking locations based on FEFO principles
- System considers location proximity to minimize travel time
- System generates picking sequence/route suggestions
- System creates `PickingTask` entities for each location/product combination
- System publishes `LoadPlannedEvent` and `PickingTaskCreatedEvent`
- System excludes expired stock from picking locations

**Implementation Plan:** [04-Plan-Picking-Locations-Implementation-Plan.md](04-Plan-Picking-Locations-Implementation-Plan.md)

---

### Story 5: US-6.2.2 - Map Orders to Loads (5 points)

**Service:** Picking Service
**Priority:** Must Have

**Acceptance Criteria:**

- System supports multiple orders per load
- System supports multiple orders per customer per load
- System maintains order-to-load relationships
- System tracks order status within load
- System allows querying orders by load

**Implementation Plan:** [05-Map-Orders-to-Loads-Implementation-Plan.md](05-Map-Orders-to-Loads-Implementation-Plan.md)

---

## Implementation Approach

### Frontend-First Design

All implementation plans start with **production-grade UI design** to ensure:

1. **User Experience** - Intuitive, accessible, and responsive interfaces
2. **Data Validation** - Client-side validation for immediate feedback
3. **Error Handling** - Clear error messages and recovery paths
4. **Visual Indicators** - Clear display of picking lists, loads, and orders
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

- **Synchronous Calls:** Product Service and Stock Management Service queried synchronously
- **Circuit Breaker:** Resilience patterns for service calls
- **Event-Driven:** Picking list creation triggers events for downstream processing
- **Error Handling:** Graceful degradation when services unavailable

---

## Architecture Compliance

### Domain-Driven Design (DDD)

- **Bounded Contexts:** Clear service boundaries (Picking Service, Stock Management Service)
- **Aggregates:** PickingList, Load, Order, PickingTask are aggregate roots
- **Value Objects:** LoadNumber, OrderNumber, CustomerInfo, Priority, etc.
- **Domain Events:** PickingListReceivedEvent, LoadPlannedEvent, PickingTaskCreatedEvent
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

### Upload Picking List CSV Flow

```
Frontend (React)
  ↓ POST /api/v1/picking/picking-lists/upload-csv (multipart/form-data)
Gateway Service
  ↓ Route to Picking Service
Picking Service (Command Controller)
  ↓ UploadPickingListCsvCommand
Command Handler
  ↓ Parse CSV file
  ↓ Validate CSV structure and data
  ↓ Validate products against Product Service
  ↓ Create PickingList aggregates for each row
  ↓ PickingList.create()
  Domain Core (PickingList Aggregate)
  ↓ PickingListReceivedEvent
Event Publisher
  ↓ Kafka Topic: picking-events
Stock Management Service (Event Listener)
  ↓ Allocate stock for picking list
Query Handler
  ↓ CsvUploadResult
Query Controller
  ↓ Response
Gateway Service
  ↓ Response
Frontend (React)
```

### Manual Picking List Entry Flow

```
Frontend (React)
  ↓ POST /api/v1/picking/picking-lists
Gateway Service
  ↓ Route to Picking Service
Picking Service (Command Controller)
  ↓ CreatePickingListCommand
Command Handler
  ↓ Validate order and product data
  ↓ Validate products against Product Service
  ↓ Create PickingList aggregate with Load and Orders
  ↓ PickingList.create()
  Domain Core (PickingList Aggregate)
  ↓ PickingListReceivedEvent
Event Publisher
  ↓ Kafka Topic: picking-events
Query Handler
  ↓ PickingListQueryResult
Query Controller
  ↓ Response
Gateway Service
  ↓ Response
Frontend (React)
```

### Plan Picking Locations Flow

```
PickingList Created
  ↓
Picking Service (Event Listener)
  ↓ Listen to PickingListReceivedEvent
Command Handler (PlanPickingLocationsCommandHandler)
  ↓ Query Stock Management Service for available stock
  ↓ Filter by product and FEFO (expiration date)
  ↓ Sort by expiration date (earliest first)
  ↓ Optimize picking sequence by location proximity
  ↓ Create PickingTask entities for each location/product
  ↓ Load.plan()
  Domain Core (Load Aggregate)
  ↓ LoadPlannedEvent, PickingTaskCreatedEvent
Event Publisher
  ↓ Kafka Topic: picking-events
Stock Management Service (Event Listener)
  ↓ Allocate stock for picking tasks
```

### Query Picking Lists Flow

```
Frontend (React)
  ↓ GET /api/v1/picking/picking-lists?status=RECEIVED&page=0&size=20
Gateway Service
  ↓ Route to Picking Service
Picking Service (Query Controller)
  ↓ ListPickingListsQuery
Query Handler
  ↓ Query from read model (data port)
  ↓ Filter by status, pagination
  ↓ PickingListQueryResult
Query Controller
  ↓ Response
Gateway Service
  ↓ Response
Frontend (React)
```

---

## Service Segregation

### Picking Service

**Responsibilities:**

- Ingest picking lists from CSV upload or manual entry
- Create picking list, load, and order aggregates
- Plan picking locations based on FEFO principles
- Map orders to loads
- Generate picking tasks
- Track picking list and order status
- Publish picking events

**Database:** `picking_db`
**Events Published:**

- `PickingListReceivedEvent`
- `LoadPlannedEvent`
- `PickingTaskCreatedEvent`
- `OrderMappedToLoadEvent`

**Events Consumed:**

- `StockAllocatedEvent` (from Stock Management Service)
- `StockMovementCompletedEvent` (from Location Management Service)

**Service Dependencies:**

- **Product Service** - Synchronous calls for product validation
- **Stock Management Service** - Synchronous calls for stock availability queries

---

### Stock Management Service

**Responsibilities (for Sprint 5):**

- Respond to stock availability queries for picking planning
- Allocate stock for picking tasks
- Provide FEFO-sorted stock items
- Update stock allocations

**Events Published:**

- `StockAllocatedEvent` (when stock is allocated for picking)

**Events Consumed:**

- `PickingListReceivedEvent` (from Picking Service)
- `LoadPlannedEvent` (from Picking Service)
- `PickingTaskCreatedEvent` (from Picking Service)

---

## Testing Strategy

### Unit Tests

- **Domain Core:** Business logic validation (picking list creation, load planning)
- **Application Service:** Command/query handler logic
- **Data Access:** Repository adapter behavior
- **Messaging:** Event publisher/listener logic
- **Validation:** CSV parsing, product validation, FEFO logic

### Integration Tests

- **Service Integration:** End-to-end service operations
- **Database Integration:** Repository operations with real database
- **Kafka Integration:** Event publishing and consumption
- **Product Service Integration:** Product validation calls
- **Stock Management Service Integration:** Stock availability queries
- **FEFO Allocation:** Test picking planning based on expiration dates

### Gateway API Tests

**Purpose:** Mimic frontend calls to backend through gateway

**Test Structure:**

- Base test class with authentication setup
- Test data builders for realistic test data
- Request/response validation
- Error scenario testing

**Test Coverage:**

- Picking list CSV upload through gateway
- Manual picking list creation through gateway
- Picking list queries through gateway
- Error handling and validation
- Authentication and authorization

**Implementation Plan:** [06-Gateway-API-Tests-Implementation-Plan.md](06-Gateway-API-Tests-Implementation-Plan.md)

---

## Implementation Order

### Phase 1: Picking List Domain Model (Days 1-3)

1. **Picking Service Domain Core**
    - PickingList aggregate root
    - Load aggregate root
    - Order entity
    - PickingTask entity
    - LoadNumber value object (move to common-domain)
    - OrderNumber value object (move to common-domain)
    - CustomerInfo value object (move to common-domain)
    - Priority enum (move to common-domain)
    - PickingListStatus enum
    - LoadStatus enum
    - OrderStatus enum
    - PickingListReceivedEvent
    - LoadPlannedEvent
    - PickingTaskCreatedEvent
    - OrderMappedToLoadEvent
    - Business logic for picking list validation

2. **Common Domain Enhancements**
    - Move LoadNumber to common-domain (DRY)
    - Move OrderNumber to common-domain (DRY)
    - Move CustomerInfo to common-domain (DRY)
    - Move Priority enum to common-domain (DRY)

### Phase 2: Backend Application Services (Days 4-7)

1. **Picking Application Service**
    - UploadPickingListCsvCommandHandler
    - CreatePickingListCommandHandler
    - PlanPickingLocationsCommandHandler
    - MapOrdersToLoadCommandHandler
    - GetPickingListQueryHandler
    - ListPickingListsQueryHandler
    - GetLoadQueryHandler
    - ListOrdersByLoadQueryHandler
    - Repository ports
    - Event publisher ports
    - Product service port (for product validation)
    - Stock management service port (for stock availability)

### Phase 3: Backend Infrastructure (Days 8-11)

1. **Picking Data Access**
    - PickingListEntity (JPA)
    - LoadEntity (JPA)
    - OrderEntity (JPA)
    - PickingTaskEntity (JPA)
    - PickingListRepositoryAdapter
    - LoadRepositoryAdapter
    - OrderRepositoryAdapter
    - PickingTaskRepositoryAdapter
    - Entity mappers
    - Database migrations

2. **Service Adapters**
    - ProductServiceAdapter (synchronous HTTP calls)
    - StockManagementServiceAdapter (synchronous HTTP calls)
    - Circuit breaker configuration

3. **Messaging**
    - Event publishers
    - Event listeners (for stock allocation events)
    - Kafka configuration

### Phase 4: Backend REST API (Days 12-13)

1. **Picking REST API**
    - PickingListCommandController
    - PickingListQueryController
    - LoadQueryController
    - CSV upload endpoint with multipart support
    - DTOs and mappers
    - Exception handlers

### Phase 5: Frontend Implementation (Days 14-17)

1. **Picking List Upload UI**
    - CSV file upload component
    - Upload progress indicator
    - Validation error display
    - Upload success confirmation

2. **Manual Picking List Entry UI**
    - Picking list form component
    - Load management (add/remove orders)
    - Order line items (add/remove products)
    - Product autocomplete
    - Customer information input
    - Priority selection
    - Real-time validation
    - Draft saving

3. **Picking List Management UI**
    - Picking list list view
    - Picking list detail view
    - Load detail view
    - Order detail view
    - Status indicators
    - Filtering and sorting

4. **Picking Location Planning UI**
    - Picking task display
    - FEFO indicators
    - Location sequence display
    - Route visualization

### Phase 6: Gateway API Tests (Days 18-20)

1. **Picking List CSV Upload Tests**
    - Valid CSV upload tests
    - Invalid CSV format tests
    - Product validation error tests
    - Error scenario tests

2. **Manual Picking List Entry Tests**
    - Valid picking list creation tests
    - Validation error tests
    - Product validation error tests
    - Error scenario tests

3. **Picking List Query Tests**
    - List picking lists tests
    - Get picking list detail tests
    - Filter and pagination tests
    - Error scenario tests

4. **Load Planning Tests**
    - FEFO allocation tests
    - Location optimization tests
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
- ✅ **Product Service** - Product master data (already running from Sprint 1)
- ✅ **Stock Management Service** - Stock availability queries (already running from Sprints 2-4)
- ✅ **Location Management Service** - Location data (already running from Sprints 1, 3)

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

1. **CSV Parsing Performance**
    - **Risk:** Large CSV files may cause performance issues
    - **Mitigation:** Implement streaming CSV parsing and batch processing
    - **Contingency:** Add file size limits and async processing

2. **FEFO Allocation Complexity**
    - **Risk:** FEFO allocation algorithm may be complex and slow
    - **Mitigation:** Optimize database queries and implement caching
    - **Contingency:** Simplify algorithm or make it async

3. **Product Service Integration**
    - **Risk:** Synchronous calls to Product Service may cause latency
    - **Mitigation:** Implement caching and circuit breaker patterns
    - **Contingency:** Cache product data locally or use async validation

### Integration Risks

1. **Service-to-Service Communication**
    - **Risk:** Product Service or Stock Management Service calls may fail
    - **Mitigation:** Implement resilience patterns (circuit breaker, retry)
    - **Contingency:** Graceful degradation with picking list warnings

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
- [ ] CSV upload endpoint with streaming parsing implemented
- [ ] All database migrations created and tested
- [ ] All events published correctly
- [ ] Product Service integration implemented with resilience patterns
- [ ] Stock Management Service integration implemented with caching
- [ ] FEFO allocation algorithm implemented and tested
- [ ] Unit tests written (80%+ coverage)
- [ ] Integration tests written
- [ ] Code reviewed and approved

### Frontend

- [ ] CSV upload component implemented
- [ ] Manual picking list entry form implemented
- [ ] Picking list management UI implemented
- [ ] All API integrations implemented
- [ ] Client-side validation implemented
- [ ] Error handling implemented
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
