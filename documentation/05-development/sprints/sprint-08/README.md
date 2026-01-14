# Sprint 8: Reconciliation - Implementation Summary

**Sprint Duration:** 2 weeks
**Total Story Points:** 34
**Module:** Reconciliation Service, Integration Service
**Status:** Ready for Implementation

---

## Sprint Overview

Sprint 8 introduces **comprehensive Stock Reconciliation** capabilities to the WMS, enabling warehouse operators to perform accurate stock counts, identify and investigate variances, and reconcile with Microsoft Dynamics 365.

### Business Value

- **Digital Stock Counting:** Replace paper-based counting with electronic worksheets and mobile entry
- **Offline Capability:** PWA with IndexedDB enables counting without network connectivity
- **Automatic Variance Detection:** System calculates and classifies variances by severity
- **Investigation Workflow:** Structured process with manager approval for high-value variances
- **D365 Integration:** Automatic reconciliation ensuring inventory accuracy across systems
- **Audit Compliance:** Complete audit trail for all counts, investigations, and D365 sync attempts

---

## User Stories

### Epic: Reconciliation

| ID | User Story | Story Points | Priority | Dependencies |
|----|----|----|----|-----|
| [US-8.1.1](01-Generate-Stock-Count-Worksheet-Implementation-Plan.md) | Generate Electronic Stock Count Worksheet | 8 | Must Have | - |
| [US-8.1.2](02-Perform-Stock-Count-Entry-Implementation-Plan.md) | Perform Stock Count Entry | 8 | Must Have | US-8.1.1 |
| [US-8.1.3](03-Complete-Stock-Count-Implementation-Plan.md) | Complete Stock Count | 5 | Must Have | US-8.1.2 |
| [US-8.2.1](04-Investigate-Stock-Count-Variances-Implementation-Plan.md) | Investigate Stock Count Variances | 5 | Must Have | US-8.1.3 |
| [US-8.3.1](05-Reconcile-Stock-Counts-D365-Implementation-Plan.md) | Reconcile Stock Counts with D365 | 8 | Should Have | US-8.1.3 |
| - | [Gateway API Tests](06-Gateway-API-Tests-Implementation-Plan.md) | - | Must Have | All US |

**Total Story Points:** 34

---

## Technical Architecture

### Domain-Driven Design

Sprint 8 implements the following bounded contexts:

```
┌─────────────────────────────────────────────────────────────────┐
│                  Reconciliation Service                          │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │              Domain Core (Pure Java)                     │  │
│  │  • StockCount Aggregate (DRAFT, IN_PROGRESS, COMPLETED)  │  │
│  │  • StockCountEntry Entity                                │  │
│  │  • StockCountVariance Aggregate                          │  │
│  │  • VarianceCalculationService (Domain Service)           │  │
│  │  • VarianceSeverity (LOW, MEDIUM, HIGH, CRITICAL)        │  │
│  │  • InvestigationStatus (PENDING → RESOLVED)              │  │
│  │  • Domain Events (StockCountCompletedEvent, etc.)        │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │         Application Service (Lombok allowed)             │  │
│  │  • GenerateStockCountWorksheetCommandHandler             │  │
│  │  • RecordStockCountEntryCommandHandler                   │  │
│  │  • CompleteStockCountCommandHandler                      │  │
│  │  • InvestigateVarianceCommandHandler                     │  │
│  │  • ResolveVarianceCommandHandler                         │  │
│  │  • Repository Ports                                      │  │
│  └──────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│         Integration Service (D365 Reconciliation)                │
│  • D365StockCountReconciliation Aggregate                       │
│  • D365StockCountReconciliationService                          │
│  • D365ODataApiClient with OAuth 2.0                            │
│  • Retry mechanism with exponential backoff (max 5 attempts)    │
│  • Complete audit trail for all D365 sync attempts              │
│  • Event listeners: StockCountCompletedEventListener            │
└──────────────────────────────────────────────────────────────────┘
```

### Event-Driven Choreography

Sprint 8 uses **event-driven choreography** (no SAGA orchestrator):

```
Stock Count Initiated
       │
       ▼
StockCountInitiatedEvent
       │
       ├──────────────┬────────────┐
       ▼              ▼            ▼
  Notification   Location Mgmt  Stock Mgmt
  Service        Service        Service
       │
       │
Stock Count Entry Recorded
       │
       ▼
StockCountEntryRecordedEvent (per entry)
       │
       │
Stock Count Completed
       │
       ▼
StockCountCompletedEvent
       │
       ├──────────────┬────────────┬────────────┐
       ▼              ▼            ▼            ▼
  Integration    Stock Mgmt   Notification  Location Mgmt
  Service        Service      Service       Service
  (D365 Sync)    (Adjust      (Alert)       (Optional)
                 Stock)
       │
       ▼
StockCountVarianceIdentifiedEvent (for significant variances)
       │
       ├──────────────┬────────────┐
       ▼              ▼            ▼
  Notification   Stock Mgmt   Reconciliation
  Service        Service      Service
  (Alert         (Prepare     (Investigation
   Supervisor)    Adjustment)  Workflow)
```

### Key Domain Events

| Event | Publisher | Consumers | Purpose |
|-------|-----------|-----------|---------|
| `StockCountInitiatedEvent` | Reconciliation Service | Notification, Location Mgmt | Signals count started |
| `StockCountEntryRecordedEvent` | Reconciliation Service | (Internal tracking) | Tracks progress |
| `StockCountCompletedEvent` | Reconciliation Service | Integration Service, Stock Mgmt | Triggers D365 sync |
| `StockCountVarianceIdentifiedEvent` | Reconciliation Service | Notification Service | Alerts on variances |
| `VarianceInvestigatedEvent` | Reconciliation Service | Notification Service | Investigation started |
| `VarianceResolvedEvent` | Reconciliation Service | Stock Mgmt, Notification | Variance resolved |
| `D365ReconciliationCompletedEvent` | Integration Service | Notification, Reconciliation | D365 sync completed |
| `D365ReconciliationFailedEvent` | Integration Service | Notification Service | D365 sync failed |

---

## Implementation Plans

### US-8.1.1: Generate Electronic Stock Count Worksheet

**File:** [01-Generate-Stock-Count-Worksheet-Implementation-Plan.md](01-Generate-Stock-Count-Worksheet-Implementation-Plan.md)

**Key Features:**

- Digital worksheet generation based on location/product filters
- Integration with Stock Management Service for current system quantities
- Support for CYCLE_COUNT, FULL_INVENTORY, and SPOT_CHECK types
- Worksheet includes location/product barcodes and expected quantities
- Unique count reference generation (SC-YYYYMMDD-NNNN)

**Endpoints:**

- `POST /api/reconciliation/stock-counts/generate-worksheet` - Generate worksheet

**Domain Objects:**

- `StockCount` aggregate (status: DRAFT)
- `StockCountEntry` entity with system quantities
- `StockCountId` value object

---

### US-8.1.2: Perform Stock Count Entry

**File:** [02-Perform-Stock-Count-Entry-Implementation-Plan.md](02-Perform-Stock-Count-Entry-Implementation-Plan.md)

**Key Features:**

- **Offline-first PWA architecture** with IndexedDB storage
- Barcode scanning support (handheld scanner or mobile camera)
- Duplicate prevention (unique location/product combination per count)
- Auto-save functionality with background sync
- Real-time validation and progress tracking
- Resume incomplete counts from any device

**Endpoints:**

- `POST /api/reconciliation/stock-counts/{id}/entries` - Record entry
- `GET /api/reconciliation/stock-counts/{id}/entries` - List entries

**Technical Highlights:**

- Service workers for offline capability
- Background Sync API for automatic synchronization
- Conflict resolution strategy with timestamps
- Mobile-optimized touch-first interface

---

### US-8.1.3: Complete Stock Count

**File:** [03-Complete-Stock-Count-Implementation-Plan.md](03-Complete-Stock-Count-Implementation-Plan.md)

**Key Features:**

- Automatic variance calculation (counted - system quantity)
- Dual-threshold severity classification (percentage AND value):
  - **LOW**: 0-5% or R0-100
  - **MEDIUM**: 5-10% or R100-500
  - **HIGH**: 10-20% or R500-1000
  - **CRITICAL**: >20% or >R1000
- Financial impact analysis (overage vs. shortage)
- Critical variance blocking (prevents completion until resolved)
- Variance summary with color-coded dashboard

**Endpoints:**

- `POST /api/reconciliation/stock-counts/{id}/complete` - Complete count

**Domain Objects:**

- `StockCountVariance` entity with severity classification
- `VarianceCalculationService` domain service
- `VarianceSummary` value object

---

### US-8.2.1: Investigate Stock Count Variances

**File:** [04-Investigate-Stock-Count-Variances-Implementation-Plan.md](04-Investigate-Stock-Count-Variances-Implementation-Plan.md)

**Key Features:**

- Investigation workflow: PENDING → IN_PROGRESS → REQUIRES_APPROVAL → RESOLVED
- **Manager approval required** for HIGH/CRITICAL variances
- Root cause analysis with stock movement history
- Investigation notes and resolution tracking
- Approval workflow with dual approval for CRITICAL variances
- Complete audit trail of all investigation activities

**Endpoints:**

- `POST /api/reconciliation/variances/{id}/investigate` - Start investigation
- `POST /api/reconciliation/variances/{id}/resolve` - Resolve variance
- `POST /api/reconciliation/variances/{id}/approve` - Manager approval
- `GET /api/reconciliation/variances/{id}/movement-history` - Movement history

**Domain Objects:**

- `VarianceInvestigation` with status tracking
- `VarianceReason` enum (COUNTING_ERROR, SYSTEM_ERROR, DAMAGE, THEFT, etc.)
- `InvestigationStatus` enum

---

### US-8.3.1: Reconcile Stock Counts with D365

**File:** [05-Reconcile-Stock-Counts-D365-Implementation-Plan.md](05-Reconcile-Stock-Counts-D365-Implementation-Plan.md)

**Key Features:**

- **Event-driven reconciliation** (triggered by `StockCountCompletedEvent`)
- D365 OData API integration for inventory counting journals
- **Retry mechanism** with exponential backoff:
  - Base delay: 2 seconds
  - Multiplier: 2.0
  - Max delay: 30 seconds
  - Max attempts: 5
  - Jitter: 10%
- Complete audit trail with all HTTP requests/responses
- Manual retry capability via dashboard
- OAuth 2.0 authentication with token refresh

**Endpoints:**

- `GET /api/reconciliation/d365-reconciliation` - List reconciliation records
- `GET /api/reconciliation/d365-reconciliation/{id}/audit-trail` - Audit trail
- `POST /api/reconciliation/d365-reconciliation/{id}/retry` - Manual retry

**Integration Components:**

- `D365StockCountReconciliation` aggregate (Integration Service)
- `D365ODataApiClient` with OAuth 2.0
- `StockCountCompletedEventListener`
- Scheduled retry job

---

### Gateway API Tests

**File:** [06-Gateway-API-Tests-Implementation-Plan.md](06-Gateway-API-Tests-Implementation-Plan.md)

**Test Coverage:**

- Stock count worksheet generation (US-8.1.1)
- Stock count entry recording with offline scenarios (US-8.1.2)
- Stock count completion with variance calculation (US-8.1.3)
- Variance investigation and approval workflows (US-8.2.1)
- D365 reconciliation with async validation (US-8.3.1)
- End-to-end workflows
- Negative test scenarios

**Test Classes:**

- `StockCountTest` - Worksheet generation, entry, completion
- `VarianceInvestigationTest` - Investigation and resolution
- `D365ReconciliationTest` - D365 integration (conditional)
- `ReconciliationWorkflowTest` - End-to-end workflows

---

## Database Schema Updates

### Reconciliation Service

**New Tables:**

1. **stock_counts** - Main stock count records
   - `stock_count_id` (UUID, PK)
   - `count_reference` (VARCHAR, UNIQUE)
   - `count_type` (ENUM: CYCLE_COUNT, FULL_INVENTORY, SPOT_CHECK)
   - `status` (ENUM: DRAFT, IN_PROGRESS, COMPLETED, CANCELLED)
   - `total_entries` (INT)
   - `total_variances` (INT)
   - `critical_variances` (INT)
   - `tenant_id` (UUID)

2. **stock_count_entries** - Individual product entries
   - `entry_id` (UUID, PK)
   - `stock_count_id` (UUID, FK)
   - `location_id` (UUID)
   - `product_id` (UUID)
   - `system_quantity` (DECIMAL)
   - `counted_quantity` (DECIMAL)
   - `variance_quantity` (DECIMAL, GENERATED)
   - `variance_percentage` (DECIMAL, GENERATED)
   - `recorded_by` (UUID)
   - `recorded_at` (TIMESTAMP)
   - **UNIQUE(stock_count_id, location_id, product_id)** - Prevent duplicates

3. **stock_count_variances** - Variance records with investigation
   - `variance_id` (UUID, PK)
   - `stock_count_id` (UUID, FK)
   - `entry_id` (UUID, FK)
   - `variance_quantity` (DECIMAL)
   - `variance_percentage` (DECIMAL)
   - `absolute_variance_value` (DECIMAL)
   - `severity` (ENUM: LOW, MEDIUM, HIGH, CRITICAL)
   - `investigation_status` (ENUM: PENDING, IN_PROGRESS, RESOLVED, ESCALATED)
   - `variance_reason` (VARCHAR)
   - `investigation_notes` (TEXT)
   - `resolution_notes` (TEXT)
   - `investigated_by` (UUID)
   - `resolved_by` (UUID)
   - `approved_by` (UUID)

4. **variance_investigation_history** - Audit trail for investigations
   - `history_id` (UUID, PK)
   - `variance_id` (UUID, FK)
   - `action` (VARCHAR)
   - `performed_by` (UUID)
   - `performed_at` (TIMESTAMP)
   - `notes` (TEXT)

### Integration Service

**New Tables:**

1. **d365_stock_count_reconciliations** - D365 sync tracking
   - `reconciliation_id` (UUID, PK)
   - `stock_count_id` (UUID)
   - `d365_journal_id` (VARCHAR)
   - `d365_journal_number` (VARCHAR)
   - `reconciliation_status` (ENUM: PENDING, IN_PROGRESS, SYNCED, FAILED, RETRYING)
   - `sync_attempts` (INT)
   - `max_attempts` (INT)
   - `last_sync_attempt` (TIMESTAMP)
   - `next_retry_at` (TIMESTAMP)
   - `error_message` (TEXT)
   - `d365_request_payload` (JSON)
   - `d365_response_payload` (JSON)

2. **d365_reconciliation_audit_log** - Complete sync audit trail
   - `audit_id` (UUID, PK)
   - `reconciliation_id` (UUID, FK)
   - `attempt_number` (INT)
   - `attempt_timestamp` (TIMESTAMP)
   - `http_status_code` (INT)
   - `request_payload` (JSON)
   - `response_payload` (JSON)
   - `error_details` (TEXT)

---

## Frontend Implementation

### New Pages

1. **GenerateStockCountWorksheetPage** (`/reconciliation/stock-count/new`)
   - Location and product filter selection
   - Worksheet generation form
   - Preview of worksheet entries

2. **StockCountEntryPage** (`/reconciliation/stock-count/:id/entry`)
   - **PWA with offline support (IndexedDB)**
   - Barcode scanner integration
   - Real-time entry recording
   - Progress tracking
   - Background sync indicator

3. **StockCountCompletionPage** (`/reconciliation/stock-count/:id/complete`)
   - Entry review table
   - Variance summary dashboard
   - Financial impact analysis
   - Pre-completion validation checklist

4. **VarianceInvestigationPage** (`/reconciliation/variances/:id/investigate`)
   - Variance details
   - Stock movement history timeline
   - Investigation form with root cause analysis
   - Approval workflow (for HIGH/CRITICAL)

5. **D365ReconciliationDashboard** (`/reconciliation/d365-reconciliation`)
   - Reconciliation metrics and success rate
   - Reconciliation records table
   - Manual retry capability
   - Audit trail viewer

### React Hooks

- `useGenerateStockCountWorksheet()`
- `useRecordStockCountEntry()` (with offline support)
- `useCompleteStockCount()`
- `useInvestigateVariance()`
- `useResolveVariance()`
- `useApproveVariance()`
- `useRetryD365Reconciliation()`
- `useStockMovementHistory()`

---

## Configuration Requirements

### Application Configuration

**`services/reconciliation-service/reconciliation-container/src/main/resources/application.yml`**

```yaml
reconciliation:
  variance-thresholds:
    low-percentage: 5.0
    medium-percentage: 10.0
    high-percentage: 20.0
    critical-percentage: 20.0
    low-value: 100.00
    medium-value: 500.00
    high-value: 1000.00
    critical-value: 1000.00
  approval-required:
    high-severity: true
    critical-severity: true
  offline-sync:
    batch-size: 50
    max-age-hours: 24

kafka:
  topics:
    stock-count-initiated: reconciliation.stock-count.initiated
    stock-count-entry-recorded: reconciliation.stock-count.entry-recorded
    stock-count-completed: reconciliation.stock-count.completed
    variance-identified: reconciliation.variance.identified
    variance-investigated: reconciliation.variance.investigated
    variance-resolved: reconciliation.variance.resolved
```

**`services/integration-service/integration-container/src/main/resources/application.yml`**

```yaml
d365:
  auth:
    tenant-id: ${D365_TENANT_ID}
    client-id: ${D365_CLIENT_ID}
    client-secret: ${D365_CLIENT_SECRET}
    resource: ${D365_RESOURCE}
  api:
    base-url: ${D365_API_BASE_URL}
    version: v9.2
    timeout-seconds: 30
    counting-journal-endpoint: /api/data/v9.2/msdyn_inventorycountingjournals
  retry:
    max-attempts: 5
    initial-delay-ms: 2000
    max-delay-ms: 30000
    multiplier: 2.0
    jitter: 0.1
  reconciliation:
    enabled: ${D365_RECONCILIATION_ENABLED:false}
```

### Environment Variables

```bash
# D365 Integration (optional - only if reconciliation enabled)
export D365_RECONCILIATION_ENABLED=true
export D365_TENANT_ID=your-tenant-id
export D365_CLIENT_ID=your-client-id
export D365_CLIENT_SECRET=your-client-secret
export D365_RESOURCE=https://your-org.crm.dynamics.com
export D365_API_BASE_URL=https://your-org.api.crm.dynamics.com
```

---

## Testing Strategy

### Unit Tests

- Domain core logic (aggregates, value objects, domain services)
- Variance calculation and severity classification
- Application service command handlers
- D365 retry logic with exponential backoff

**Target Coverage:** >90%

### Integration Tests

- Repository adapters with test containers
- Event publishing and consumption
- REST API endpoints
- D365 API client

**Target Coverage:** >85%

### End-to-End Tests (Gateway API Tests)

- Complete stock count workflows
- Variance investigation and approval workflows
- D365 reconciliation with async validation
- Offline sync scenarios
- Query operations

**Target Coverage:** 100% of endpoints

---

## Implementation Order

### Phase 1: Foundation (Days 1-2)

1. Common module updates (enums, value objects)
2. Reconciliation Service domain core
3. Database schema migrations
4. Base repository implementations

### Phase 2: Core Features (Days 3-5)

5. US-8.1.1 - Generate worksheet (backend + frontend)
6. US-8.1.2 - Stock count entry with offline support (backend + frontend)
7. US-8.1.3 - Complete count with variance calculation (backend + frontend)

### Phase 3: Investigation & Integration (Days 6-9)

8. US-8.2.1 - Variance investigation and approval (backend + frontend)
9. US-8.3.1 - D365 reconciliation (Integration Service updates)
10. Event listeners and choreography
11. Retry mechanisms and scheduled jobs

### Phase 4: Testing & Validation (Day 10)

12. Gateway API Tests (all endpoints)
13. Integration testing
14. Offline sync testing
15. D365 sandbox testing
16. Bug fixes and refinements

---

## Acceptance Criteria Summary

| User Story | Total ACs | Status |
|------------|-----------|--------|
| US-8.1.1 | 7 | ✅ All validated |
| US-8.1.2 | 7 | ✅ All validated |
| US-8.1.3 | 7 | ✅ All validated |
| US-8.2.1 | 7 | ✅ All validated |
| US-8.3.1 | 6 | ✅ All validated |

**Total:** 34 acceptance criteria, all validated in implementation plans

---

## Risk Mitigation

### Technical Risks

| Risk | Mitigation |
|------|------------|
| Offline sync conflicts | Timestamp-based conflict resolution with user notification |
| Large stock count datasets | Pagination, lazy loading, batch processing |
| D365 integration failures | Retry with exponential backoff, manual retry, complete audit trail |
| Variance calculation accuracy | Comprehensive unit tests, validation against known datasets |

### Operational Risks

| Risk | Mitigation |
|------|------------|
| Incomplete stock counts | Auto-save progress, resume capability, validation before completion |
| Incorrect variance resolution | Audit trail, approval workflow for HIGH/CRITICAL variances |
| D365 sync monitoring | Dashboard alerts, daily summary reports, failed sync notifications |

---

## Success Metrics

### Sprint Goals

- ✅ All 5 user stories implemented with full test coverage
- ✅ Offline capability tested on mobile devices
- ✅ D365 integration tested in sandbox environment
- ✅ Gateway API tests passing (100% endpoint coverage)
- ✅ Frontend components integrated and functional
- ✅ Documentation complete

### Performance Targets

- Stock count creation: < 2 seconds (P95)
- Entry recording: < 500ms (P95)
- Variance calculation: < 3 seconds for 1000 entries (P95)
- D365 sync latency: < 10 seconds (P95)
- Offline sync: < 5 seconds for 100 entries (P95)
- List query with filters: < 1 second (P95)

---

## Dependencies

### External Services

- Microsoft Dynamics 365 (OData API v9.2) - Optional
- PostgreSQL database
- Kafka for event streaming

### Internal Services

- Stock Management Service (stock levels, movement history)
- Location Management Service (location data)
- Product Service (product information)
- Integration Service (D365 synchronization)
- Notification Service (alerts for variances)

---

## Documentation

### Implementation Plans

1. [00-Sprint-08-Implementation-Plan.md](00-Sprint-08-Implementation-Plan.md) - Master plan
2. [01-Generate-Stock-Count-Worksheet-Implementation-Plan.md](01-Generate-Stock-Count-Worksheet-Implementation-Plan.md)
3. [02-Perform-Stock-Count-Entry-Implementation-Plan.md](02-Perform-Stock-Count-Entry-Implementation-Plan.md)
4. [03-Complete-Stock-Count-Implementation-Plan.md](03-Complete-Stock-Count-Implementation-Plan.md)
5. [04-Investigate-Stock-Count-Variances-Implementation-Plan.md](04-Investigate-Stock-Count-Variances-Implementation-Plan.md)
6. [05-Reconcile-Stock-Counts-D365-Implementation-Plan.md](05-Reconcile-Stock-Counts-D365-Implementation-Plan.md)
7. [06-Gateway-API-Tests-Implementation-Plan.md](06-Gateway-API-Tests-Implementation-Plan.md)

### Architecture Documents

- [Service Architecture Document](../../01-architecture/Service_Architecture_Document.md)
- [Frontend Architecture Document](../../01-architecture/Frontend_Architecture_Document.md)
- [Clean Code Guidelines](../../guide/clean-code-guidelines-per-module.md)

---

## Next Steps (Post-Sprint 8)

### Sprint 9: Advanced Reconciliation

- Cycle count scheduling and automation
- Variance trend analysis and reporting
- Predictive analytics for high-risk locations/products

### Sprint 10: Reporting & Analytics

- Stock accuracy metrics
- Reconciliation performance dashboards
- Management reports with KPIs

---

## Team Allocation

**Backend Developers (3):**

- Developer 1: Reconciliation Service domain core and application service
- Developer 2: Integration Service (D365 adapter, retry logic)
- Developer 3: Event listeners, scheduled jobs, database migrations

**Frontend Developers (2):**

- Developer 1: Stock count pages (worksheet, entry with offline support)
- Developer 2: Variance investigation, D365 reconciliation dashboard

**QA Engineers (1):**

- Gateway API tests, integration testing, offline sync testing, UAT coordination

**DevOps (0.5):**

- D365 sandbox configuration, IndexedDB testing, monitoring setup

---

**Sprint 8 Status:** ✅ Ready for Implementation
**Last Updated:** 2026-01-11
**Next Review:** Sprint Planning Meeting

---

## Quick Reference

### Key Endpoints

```
Stock Count Workflow:
POST   /api/reconciliation/stock-counts/generate-worksheet
POST   /api/reconciliation/stock-counts/{id}/entries
POST   /api/reconciliation/stock-counts/{id}/complete
GET    /api/reconciliation/stock-counts
GET    /api/reconciliation/stock-counts/{id}

Variance Investigation:
POST   /api/reconciliation/variances/{id}/investigate
POST   /api/reconciliation/variances/{id}/resolve
POST   /api/reconciliation/variances/{id}/approve
GET    /api/reconciliation/variances/{id}/movement-history

D365 Reconciliation:
GET    /api/reconciliation/d365-reconciliation
POST   /api/reconciliation/d365-reconciliation/{id}/retry
GET    /api/reconciliation/d365-reconciliation/{id}/audit-trail
```

### Event Topics

```
reconciliation.stock-count.initiated
reconciliation.stock-count.entry-recorded
reconciliation.stock-count.completed
reconciliation.variance.identified
reconciliation.variance.investigated
reconciliation.variance.resolved
d365.reconciliation.completed
d365.reconciliation.failed
```

---

**End of Sprint 8 README**
