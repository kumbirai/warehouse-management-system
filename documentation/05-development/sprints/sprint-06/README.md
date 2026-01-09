# Sprint 6: Picking Execution and Expiration Management

## Overview

**Sprint Duration:** 2 weeks  
**Total Story Points:** 26  
**Sprint Goal:** Complete picking execution workflow and implement expiration tracking and restock management

---

## User Stories

| Story ID | Title | Priority | Points | Status |
|----------|-------|----------|--------|--------|
| US-6.3.1 | Execute Picking Task | Must Have | 8 | Not Started |
| US-6.3.2 | Complete Picking | Must Have | 5 | Not Started |
| US-2.1.3 | Track Expiration Dates and Generate Alerts | Must Have | 5 | Not Started |
| US-2.1.4 | Prevent Picking of Expired Stock | Must Have | 3 | Not Started |
| US-5.1.3 | Generate Restock Request | Must Have | 5 | Not Started |

---

## Implementation Plans

### 00. Sprint Master Plan
**File:** [00-Sprint-06-Implementation-Plan.md](00-Sprint-06-Implementation-Plan.md)

**Contents:**
- Sprint overview and success criteria
- Architecture compliance (DDD, Clean Hexagonal, CQRS, Event-Driven)
- Service segregation and responsibilities
- Data flow design across all user stories
- Implementation order and phases
- Dependencies and prerequisites
- Risk mitigation strategies

---

### 01. Execute Picking Task (US-6.3.1) - 8 Points
**File:** [01-Execute-Picking-Task-Implementation-Plan.md](01-Execute-Picking-Task-Implementation-Plan.md)

**Contents:**
- **Frontend:** Complete React/TypeScript implementation
  - `PickingTaskExecutionPage` with real-time validation
  - Custom hooks for picking task execution
  - Location guidance and quantity input components
  
- **Backend:** Clean Hexagonal Architecture
  - Domain Core: `PickingTask` aggregate with business logic
  - Application Service: `ExecutePickingTaskCommandHandler`
  - REST API: `POST /api/v1/picking/picking-tasks/{id}/execute`
  - Service Integration: Stock Management and Location Management
  
- **Events:**
  - `PickingTaskCompletedEvent`
  - `PartialPickingCompletedEvent`
  
- **Testing:** 50+ checkpoint implementation checklist

---

### 02. Complete Picking (US-6.3.2) - 5 Points
**File:** [02-Complete-Picking-Implementation-Plan.md](02-Complete-Picking-Implementation-Plan.md)

**Contents:**
- **Frontend:** Picking completion workflow
  - `PickingListCompletionPage` with validation
  - Task status summary and progress tracking
  - Completion confirmation workflow
  
- **Backend:** Picking list completion
  - Domain Core: `PickingList.complete()` method
  - Validation: All tasks completed or partial
  - Status management and audit trail
  
- **Events:**
  - `PickingCompletedEvent`
  
- **Integration:** Triggers returns processing preparation

---

### 03. Track Expiration Dates (US-2.1.3) - 5 Points
**File:** [03-Track-Expiration-Dates-Implementation-Plan.md](03-Track-Expiration-Dates-Implementation-Plan.md)

**Contents:**
- **Frontend:** Expiring stock dashboard
  - `ExpiringStockDashboard` with real-time alerts
  - Classification cards (CRITICAL, NEAR_EXPIRY, EXPIRED)
  - Date range filtering and reporting
  
- **Backend:** Scheduled expiration checking
  - Daily scheduled job: `ExpirationCheckScheduler`
  - Classification logic: 0-7 days (CRITICAL), 8-30 days (NEAR_EXPIRY)
  - Automated notification generation
  
- **Events:**
  - `StockExpiringAlertEvent`
  - `StockExpiredEvent`
  - `StockClassifiedEvent`
  
- **Notification Integration:** Alert warehouse managers

---

### 04. Prevent Picking Expired Stock (US-2.1.4) - 3 Points
**File:** [04-Prevent-Picking-Expired-Stock-Implementation-Plan.md](04-Prevent-Picking-Expired-Stock-Implementation-Plan.md)

**Contents:**
- **Frontend:** Expired stock warnings
  - `ExpiredStockIndicator` component
  - Disabled picking buttons for expired stock
  - Clear visual error messages
  
- **Backend:** Expiration validation
  - Query-level filtering: Exclude expired stock from FEFO queries
  - Execution validation: Check expiration before picking
  - Audit logging: Track all expired stock picking attempts
  
- **Integration:** Stock Management Service expiration checks

---

### 05. Generate Restock Request (US-5.1.3) - 5 Points
**File:** [05-Generate-Restock-Request-Implementation-Plan.md](05-Generate-Restock-Request-Implementation-Plan.md)

**Contents:**
- **Frontend:** Restock requests dashboard
  - `RestockRequestsDashboard` with priority filtering
  - Request status tracking and management
  - High-priority alerts
  
- **Backend:** Automated restock generation
  - Event-driven trigger: `StockLevelBelowMinimumEvent`
  - Priority calculation: HIGH (<50%), MEDIUM (50-80%), LOW (80-100%)
  - Duplicate prevention logic
  - Quantity calculation: Maximum - Current
  
- **Events:**
  - `RestockRequestGeneratedEvent`
  
- **D365 Integration (Optional):** Send restock requests to D365

---

### 06. Gateway API Tests
**File:** [06-Gateway-API-Tests-Implementation-Plan.md](06-Gateway-API-Tests-Implementation-Plan.md)

**Contents:**
- Comprehensive integration test suite
- Test scenarios for all 5 user stories
- End-to-end workflow testing
- Performance and concurrency tests
- Test data setup and cleanup utilities

---

## Architecture Compliance

### Domain-Driven Design (DDD)
- **Bounded Contexts:** Clear boundaries (Picking, Stock Management, Location Management, Notification)
- **Aggregates:** PickingTask, PickingList, StockItem, RestockRequest
- **Value Objects:** Quantity, ExpirationDate, StockClassification, RestockPriority
- **Domain Events:** All state changes publish domain events
- **Ubiquitous Language:** Consistent terminology across codebase

### Clean Hexagonal Architecture
- **Domain Core:** Pure Java, no framework dependencies
- **Application Service:** Port interfaces, use case orchestration
- **Infrastructure:** Adapters implement ports (REST, JPA, Kafka)
- **Dependency Direction:** Domain ← Application ← Infrastructure

### CQRS
- **Command Side:** POST/PUT/DELETE operations modify state
- **Query Side:** GET operations read optimized views
- **Read Models:** Denormalized projections for queries
- **Separation:** Command and Query controllers separate

### Event-Driven Choreography
- **Domain Events:** Published after successful state changes
- **Asynchronous Processing:** Services react to events independently
- **Eventual Consistency:** Acceptable for cross-service operations
- **Idempotency:** All event handlers are idempotent

---

## Service Segregation

### Picking Service
**Responsibilities:**
- Execute picking tasks with validation
- Complete picking operations
- Track picking progress
- Publish picking events

**Aggregates:** PickingTask, PickingList

---

### Stock Management Service
**Responsibilities:**
- Track expiration dates (scheduled job)
- Generate expiration alerts
- Prevent picking expired stock
- Generate restock requests
- Manage stock levels

**Aggregates:** StockItem, StockLevel, RestockRequest

---

### Location Management Service
**Responsibilities:**
- Track stock movements during picking
- Update location status
- Maintain location capacity

**Aggregates:** StockMovement, Location

---

### Notification Service
**Responsibilities:**
- Create notifications for expiring stock
- Create notifications for restock requests
- Display alerts in dashboard

**Aggregates:** Notification

---

### Integration Service (Optional - D365)
**Responsibilities:**
- Send restock requests to D365
- Handle D365 API errors with retry

---

## Data Flow

### Complete Picking Execution Flow

```
1. Frontend: Operator selects picking task
   ↓
2. GET /api/v1/picking/picking-tasks/{id}
   ↓
3. Frontend: Display task details, location guidance
   ↓
4. Backend: Validate stock availability and expiration
   ↓
5. POST /api/v1/picking/picking-tasks/{id}/execute
   ↓
6. Domain: PickingTask.execute() → PickingTaskCompletedEvent
   ↓
7. Stock Management: Update stock levels
   ↓
8. Location Management: Create stock movement
   ↓
9. If stock below minimum → RestockRequestGeneratedEvent
   ↓
10. POST /api/v1/picking/picking-lists/{id}/complete
    ↓
11. Domain: PickingList.complete() → PickingCompletedEvent
    ↓
12. Returns Service: Prepare for potential returns
```

---

## Implementation Order

### Phase 1: Prevent Picking Expired Stock (Days 1-2)
**Rationale:** Foundation for safe picking operations

1. Implement `StockItem.canBePicked()` method
2. Update picking location queries to filter expired stock
3. Add validation in picking task execution
4. Create frontend error handling
5. Write unit and integration tests

---

### Phase 2: Execute Picking Task (Days 3-5)
**Rationale:** Core picking execution functionality

1. Implement picking task execution UI
2. Implement `ExecutePickingTaskCommand` and handler
3. Implement domain logic with business rules
4. Add stock level validation and updates
5. Create stock movement records
6. Write gateway API tests

---

### Phase 3: Complete Picking (Days 6-7)
**Rationale:** Complete picking workflow

1. Implement picking completion UI
2. Implement `CompletePickingCommand` and handler
3. Validate all tasks completed or partial
4. Write gateway API tests

---

### Phase 4: Track Expiration Dates (Days 8-9)
**Rationale:** Proactive expiration management

1. Implement scheduled job for expiration checking
2. Implement `StockItem.checkExpiration()` method
3. Create expiring stock alerts dashboard UI
4. Implement notification creation
5. Write unit and integration tests

---

### Phase 5: Generate Restock Requests (Day 10)
**Rationale:** Automated inventory replenishment

1. Implement `StockLevel.checkMinimumThreshold()` method
2. Create restock request generation logic
3. Implement restock request notification UI
4. Optional: Integrate with D365
5. Write gateway API tests

---

## Common Value Objects (DRY Principle)

Move these to `common-domain` module:

- **Quantity** - Stock quantity with validation
- **ExpirationDate** - Expiration date with classification logic
- **Priority** - Restock priority enum (HIGH, MEDIUM, LOW)
- **StockClassification** - Enum (CRITICAL, NEAR_EXPIRY, NORMAL, EXPIRED)
- **LoadNumber** - Load identifier
- **OrderNumber** - Order identifier
- **CustomerInfo** - Customer information

---

## Testing Strategy

### Unit Tests
- **Domain Core:** Pure Java testing with JUnit 5
- **Coverage Target:** >80%
- **Focus:** Business rules, validation, event publishing

### Integration Tests
- **Gateway API Tests:** End-to-end testing through gateway
- **Event Flow Tests:** Verify event publishing and consumption
- **Service Integration:** Test service-to-service communication

### Performance Tests
- **Query Optimization:** Expiration queries with large datasets
- **Concurrent Picking:** Multiple operators picking simultaneously
- **Scheduled Jobs:** Expiration check performance

---

## Dependencies

### Prerequisites from Previous Sprints
- ✅ Stock Management Service operational (Sprint 1-3)
- ✅ Stock items with expiration dates created (Sprint 3-4)
- ✅ Stock levels tracked per product/location (Sprint 3-4)
- ✅ Location Management Service operational (Sprint 2)
- ✅ Picking lists created (Sprint 5)
- ✅ Picking tasks created (Sprint 5)

### External Dependencies
- **Kafka:** Event streaming
- **PostgreSQL:** Database storage
- **Spring Boot:** Application framework
- **React:** Frontend framework
- **D365 (Optional):** ERP integration for restock requests

---

## Risk Mitigation

### Risk 1: Real-Time Stock Level Updates
**Mitigation:** Optimistic locking, retry logic, real-time stock display

### Risk 2: Expiration Date Accuracy
**Mitigation:** Validation on receipt, scheduled checks, UI validation, system-level prevention

### Risk 3: Duplicate Restock Requests
**Mitigation:** Status tracking, pending request checks, idempotent event handlers

### Risk 4: D365 Integration Failures (Optional)
**Mitigation:** Retry logic, dead letter queue, error alerts, manual submission fallback

---

## Success Metrics

### Velocity Metrics
- **Planned Points:** 26
- **Target Completion:** 100%

### Quality Metrics
- **Test Coverage:** >80%
- **Code Review:** All code reviewed
- **Defect Rate:** <5% post-deployment

### Business Metrics
- **Picking Accuracy:** Target >95%
- **Expired Stock Incidents:** Zero
- **Restock Request Timeliness:** <1 hour from threshold breach

---

## Documentation

### User Guides
- Picking task execution workflow
- Expiring stock dashboard usage
- Restock request management

### Technical Documentation
- API specifications (OpenAPI)
- Event schemas
- Database migrations
- Architecture decisions

---

## Deployment Checklist

- [ ] Database migrations applied
- [ ] Kafka topics created
- [ ] Environment variables configured
- [ ] Services deployed
- [ ] Frontend deployed
- [ ] Health checks verified
- [ ] Scheduled jobs configured
- [ ] Monitoring alerts configured
- [ ] Documentation updated
- [ ] User training completed

---

## Related Documentation

- [Service Architecture Document](../../../01-architecture/Service_Architecture_Document.md)
- [Domain Model Design](../../../01-architecture/Domain_Model_Design.md)
- [User Story Breakdown](../../../06-project-management/User_Story_Breakdown.md)
- [Mandated Implementation Template Guide](../../../guide/mandated-Implementation-template-guide.md)

---

**Document Version:** 1.0  
**Date:** 2026-01-08  
**Status:** Ready for Implementation  
**Sprint Start:** TBD  
**Sprint End:** TBD
