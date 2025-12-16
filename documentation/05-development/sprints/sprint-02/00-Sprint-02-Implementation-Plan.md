# Sprint 2 Implementation Plan

## Warehouse Management System Integration - CCBSA LDP System

**Sprint:** Sprint 2 - Stock Receipt  
**Duration:** 2 weeks  
**Sprint Goal:** Enable stock consignment receipt and validation  
**Total Story Points:** 34

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

Enable stock consignment receipt and validation capabilities to support warehouse operations. This sprint focuses on:

1. **Stock Consignment Receipt** - Upload and manually enter consignment data
2. **Consignment Validation** - Validate consignment data against business rules
3. **Product Barcode Validation** - Validate product barcodes during consignment entry
4. **Stock Consignment Creation** - Create stock consignment aggregates with proper state management

### Success Criteria

- ✅ Consignment data can be uploaded via CSV file
- ✅ Consignment data can be manually entered via UI
- ✅ Consignment data is validated against business rules
- ✅ Product barcodes are validated during consignment entry
- ✅ Stock consignments are created with proper domain modeling
- ✅ All data flows correctly from frontend through gateway to backend services
- ✅ Gateway API tests validate all endpoints
- ✅ All implementations follow DDD, Clean Hexagonal Architecture, CQRS, and Event-Driven Choreography principles

---

## User Stories

### Story 1: US-1.1.1 - Upload Consignment Data via CSV File (8 points)

**Service:** Stock Management Service  
**Priority:** Must Have

**Acceptance Criteria:**

- System accepts CSV file uploads through web interface
- CSV format includes: consignment reference, product codes, quantities, expiration dates
- System validates CSV file format and required columns before processing
- System validates product codes exist in product master data
- System provides clear error messages for invalid CSV data
- System processes CSV file and creates stock consignment records
- System displays upload progress and completion status
- System supports CSV file sizes up to 10MB
- System logs all CSV upload events for audit
- System publishes `StockConsignmentReceivedEvent` for each consignment

**Implementation Plan:** [01-Consignment-CSV-Upload-Implementation-Plan.md](01-Consignment-CSV-Upload-Implementation-Plan.md)

---

### Story 2: US-1.1.2 - Manual Consignment Data Entry via UI (8 points)

**Service:** Stock Management Service  
**Priority:** Must Have

**Acceptance Criteria:**

- System provides form-based UI for consignment data entry
- Form includes fields: consignment reference, warehouse ID, line items (product, quantity, expiration date)
- System validates required fields and data formats in real-time
- System validates product codes exist in product master data
- System supports adding multiple line items per consignment
- System supports product barcode scanning for product identification
- System provides clear validation error messages
- System allows saving draft consignments for later completion
- System publishes `StockConsignmentReceivedEvent` after successful entry

**Implementation Plan:** [02-Consignment-Manual-Entry-Implementation-Plan.md](02-Consignment-Manual-Entry-Implementation-Plan.md)

---

### Story 3: US-1.1.4 - Validate Consignment Data (5 points)

**Service:** Stock Management Service  
**Priority:** Must Have

**Acceptance Criteria:**

- System validates consignment reference uniqueness
- System validates product codes exist in product master data
- System validates quantities are positive
- System validates expiration dates are in the future (if provided)
- System validates received dates are not in the future
- System validates warehouse IDs are valid
- System provides detailed validation error messages
- System supports batch validation for CSV uploads
- System returns validation results before processing

**Implementation Plan:** [03-Consignment-Validation-Implementation-Plan.md](03-Consignment-Validation-Implementation-Plan.md)

---

### Story 4: US-1.1.5 - Create Stock Consignment (8 points)

**Service:** Stock Management Service  
**Priority:** Must Have

**Acceptance Criteria:**

- System creates stock consignment aggregate with proper domain modeling
- System stores consignment reference, warehouse ID, received date
- System stores line items with product codes, quantities, expiration dates
- System initializes consignment status as RECEIVED
- System validates business rules before creation
- System publishes `StockConsignmentReceivedEvent` after successful creation
- System supports multi-tenant isolation
- System maintains audit trail (created at, created by)

**Implementation Plan:** [04-Stock-Consignment-Creation-Implementation-Plan.md](04-Stock-Consignment-Creation-Implementation-Plan.md)

---

### Story 5: US-4.1.4 - Validate Product Barcode (5 points)

**Service:** Product Service  
**Priority:** Must Have

**Acceptance Criteria:**

- System validates barcode format (EAN-13, Code 128, etc.)
- System looks up product by barcode
- System returns product information if found
- System returns clear error message if barcode not found
- System supports multiple barcode formats
- System provides synchronous API endpoint for validation
- System caches product barcode lookups for performance
- System validates barcode uniqueness

**Implementation Plan:** [05-Product-Barcode-Validation-Implementation-Plan.md](05-Product-Barcode-Validation-Implementation-Plan.md)

---

## Implementation Approach

### Frontend-First Design

All implementation plans start with **production-grade UI design** to ensure:

1. **User Experience** - Intuitive, accessible, and responsive interfaces
2. **Data Validation** - Client-side validation for immediate feedback
3. **Error Handling** - Clear error messages and recovery paths
4. **Barcode Scanning** - Support for barcode scanning in manual entry
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

- **Synchronous Calls:** Product Service queried synchronously for barcode validation
- **Circuit Breaker:** Resilience patterns for service calls
- **Caching:** Product barcode lookups cached for performance
- **Error Handling:** Graceful degradation when Product Service unavailable

---

## Architecture Compliance

### Domain-Driven Design (DDD)

- **Bounded Contexts:** Clear service boundaries (Stock Management, Product)
- **Aggregates:** StockConsignment is aggregate root
- **Value Objects:** ConsignmentReference, ConsignmentId, ConsignmentLineItem, etc.
- **Domain Events:** StockConsignmentReceivedEvent
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

### Consignment CSV Upload Flow

```
Frontend (React)
  ↓ POST /api/v1/stock-management/consignments/upload-csv (multipart/form-data)
Gateway Service
  ↓ Route to Stock Management Service
Stock Management Service (Command Controller)
  ↓ UploadConsignmentCsvCommand
Command Handler
  ↓ CSV Parser
  ↓ For each row:
    Validate Product Code (synchronous call to Product Service)
    Create ConsignmentLineItem
    ↓ StockConsignment.builder()
    Domain Core (StockConsignment Aggregate)
    ↓ StockConsignmentReceivedEvent
Event Publisher
  ↓ Kafka Topic: stock-management-events
Query Handler
  ↓ ConsignmentQueryResult
Query Controller
  ↓ Response with upload summary
Gateway Service
  ↓ Response
Frontend (React)
```

### Consignment Manual Entry Flow

```
Frontend (React)
  ↓ POST /api/v1/stock-management/consignments
Gateway Service
  ↓ Route to Stock Management Service
Stock Management Service (Command Controller)
  ↓ CreateConsignmentCommand
Command Handler
  ↓ Validate Product Codes (synchronous calls to Product Service)
  ↓ StockConsignment.builder()
  Domain Core (StockConsignment Aggregate)
  ↓ StockConsignmentReceivedEvent
Event Publisher
  ↓ Kafka Topic: stock-management-events
Query Handler
  ↓ ConsignmentQueryResult
Query Controller
  ↓ Response
Gateway Service
  ↓ Response
Frontend (React)
```

### Product Barcode Validation Flow

```
Frontend (React)
  ↓ GET /api/v1/product-service/products/validate-barcode?barcode={barcode}
Gateway Service
  ↓ Route to Product Service
Product Service (Query Controller)
  ↓ ValidateProductBarcodeQuery
Query Handler
  ↓ Product lookup by barcode
  ↓ Barcode format validation
Query Controller
  ↓ Response (product info or error)
Gateway Service
  ↓ Response
Frontend (React)
```

### Consignment Validation Flow

```
Frontend (React)
  ↓ POST /api/v1/stock-management/consignments/validate
Gateway Service
  ↓ Route to Stock Management Service
Stock Management Service (Command Controller)
  ↓ ValidateConsignmentCommand
Command Handler
  ↓ Validate Consignment Reference (uniqueness)
  ↓ Validate Product Codes (synchronous calls to Product Service)
  ↓ Validate Quantities (positive)
  ↓ Validate Dates (expiration in future, received not in future)
  ↓ Validate Warehouse ID
Validation Service
  ↓ ValidationResult
Query Controller
  ↓ Response with validation results
Gateway Service
  ↓ Response
Frontend (React)
```

---

## Service Segregation

### Stock Management Service

**Responsibilities:**

- Manage stock consignment lifecycle
- Validate consignment data
- Process CSV uploads
- Store consignment records
- Publish consignment events

**Database:** `stock_management_db`  
**Events Published:**

- `StockConsignmentReceivedEvent`

**Events Consumed:**

- None (foundational service for consignment receipt)

**Service Dependencies:**

- **Product Service** - Synchronous calls for product barcode validation

---

### Product Service

**Responsibilities:**

- Validate product barcodes
- Look up products by barcode
- Return product information
- Cache barcode lookups

**Database:** `product_db`  
**Events Published:**

- None (query-only operations)

**Events Consumed:**

- None (reference data service)

**Note:** Product Service provides **synchronous query endpoints** for barcode validation. Other services query Product Service directly via REST API for immediate validation.

---

## Testing Strategy

### Unit Tests

- **Domain Core:** Business logic validation
- **Application Service:** Command/query handler logic
- **Data Access:** Repository adapter behavior
- **Messaging:** Event publisher/listener logic
- **Validation:** Consignment validation rules

### Integration Tests

- **Service Integration:** End-to-end service operations
- **Database Integration:** Repository operations with real database
- **Kafka Integration:** Event publishing and consumption
- **Product Service Integration:** Barcode validation calls

### Gateway API Tests

**Purpose:** Mimic frontend calls to backend through gateway

**Test Structure:**

- Base test class with authentication setup
- Test data builders for realistic test data
- Request/response validation
- Error scenario testing

**Test Coverage:**

- Consignment CSV upload through gateway
- Consignment manual entry through gateway
- Consignment validation through gateway
- Product barcode validation through gateway
- Error handling and validation
- Authentication and authorization

**Implementation Plan:** [06-Gateway-API-Tests-Implementation-Plan.md](06-Gateway-API-Tests-Implementation-Plan.md)

---

## Implementation Order

### Phase 1: Product Barcode Validation (Days 1-2)

1. **Product Service Barcode Validation API**
    - ValidateProductBarcodeQueryHandler
    - Product lookup by barcode
    - Barcode format validation
    - Product barcode validation endpoint

### Phase 2: Backend Domain Models (Days 3-5)

1. **Stock Management Service Domain Core**
    - StockConsignment aggregate root
    - ConsignmentId value object
    - ConsignmentReference value object
    - ConsignmentLineItem value object
    - ConsignmentStatus enum
    - StockConsignmentReceivedEvent
    - Business logic validation

### Phase 3: Backend Application Services (Days 6-8)

1. **Stock Management Application Service**
    - CreateConsignmentCommandHandler
    - UploadConsignmentCsvCommandHandler
    - ValidateConsignmentCommandHandler
    - GetConsignmentQueryHandler
    - Repository ports
    - Event publisher ports
    - Product service port (for barcode validation)

### Phase 4: Backend Infrastructure (Days 9-11)

1. **Stock Management Data Access**
    - StockConsignmentEntity (JPA)
    - ConsignmentLineItemEntity (JPA)
    - StockConsignmentRepositoryAdapter
    - Entity mappers
    - Database migrations

2. **Product Service Integration**
    - ProductServicePort interface
    - ProductServiceAdapter (REST client)
    - Circuit breaker configuration
    - Caching configuration

3. **Messaging**
    - Event publishers
    - Event listeners (if needed)
    - Kafka configuration

### Phase 5: Backend REST API (Days 12-13)

1. **Stock Management REST API**
    - StockConsignmentCommandController
    - StockConsignmentQueryController
    - DTOs and mappers
    - Exception handlers
    - CSV upload endpoint

2. **Product Service REST API**
    - ProductBarcodeValidationController
    - DTOs and mappers

### Phase 6: Frontend Implementation (Days 14-16)

1. **Consignment CSV Upload UI**
    - CSV upload component
    - Progress tracking
    - Error display
    - Upload summary

2. **Consignment Manual Entry UI**
    - Consignment form
    - Line item entry
    - Product barcode scanning
    - Real-time validation
    - Draft saving

3. **Product Barcode Validation Integration**
    - Barcode scanner component
    - Product lookup by barcode
    - Validation feedback

### Phase 7: Gateway API Tests (Day 17)

1. **Consignment Endpoint Tests**
    - CSV upload tests
    - Manual entry tests
    - Validation tests
    - Error scenario tests

2. **Product Barcode Validation Tests**
    - Barcode validation tests
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
- ✅ **Product Service** - Product master data (already running) - **NEW dependency for barcode validation**

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

1. **Product Service Integration**
    - **Risk:** Synchronous calls to Product Service may cause latency
    - **Mitigation:** Implement caching and circuit breaker patterns
    - **Contingency:** Use async validation with eventual consistency

2. **CSV Parsing Performance**
    - **Risk:** Large CSV files may cause performance issues
    - **Mitigation:** Implement streaming CSV parser
    - **Contingency:** Add file size limits and batch processing

3. **Product Barcode Validation**
    - **Risk:** Product Service may be unavailable
    - **Mitigation:** Implement circuit breaker and fallback
    - **Contingency:** Allow consignment creation with validation warnings

### Integration Risks

1. **Service-to-Service Communication**
    - **Risk:** Product Service calls may fail
    - **Mitigation:** Implement resilience patterns (circuit breaker, retry)
    - **Contingency:** Graceful degradation with validation warnings

2. **Frontend-Backend Integration**
    - **Risk:** API contracts may not match
    - **Mitigation:** Use OpenAPI/Swagger for contract definition
    - **Contingency:** Update DTOs to match frontend needs

---

## Definition of Done

### Backend

- [ ] All domain models implemented with business logic
- [ ] All command/query handlers implemented
- [ ] All REST API endpoints implemented
- [ ] All database migrations created and tested
- [ ] All events published correctly
- [ ] Product Service integration implemented with resilience patterns
- [ ] Unit tests written (80%+ coverage)
- [ ] Integration tests written
- [ ] Code reviewed and approved

### Frontend

- [ ] All UI components implemented
- [ ] All API integrations implemented
- [ ] Client-side validation implemented
- [ ] Error handling implemented
- [ ] Barcode scanning integrated
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

