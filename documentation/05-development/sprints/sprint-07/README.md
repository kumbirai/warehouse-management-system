# Sprint 7: Returns Management - Implementation Summary

**Sprint Duration:** 2 weeks
**Total Story Points:** 28
**Module:** Returns Management, Integration Service
**Status:** Ready for Implementation

---

## Sprint Overview

Sprint 7 introduces **comprehensive Returns Management** capabilities to the WMS, enabling seamless handling of partial returns, full returns, damage assessments, automatic
location assignment, and bidirectional integration with Microsoft Dynamics 365.

### Business Value

- **Streamlined Return Processing:** Operators can efficiently process both partial and full order returns with digital signature capture
- **Damage Documentation:** Complete damage assessment workflow with photo evidence and insurance claim integration
- **Automated Location Assignment:** Intelligent routing of returned goods based on product condition (FEFO for good stock, quarantine for damaged)
- **Financial Reconciliation:** Automatic synchronization with D365 for accurate inventory and financial records
- **Audit Compliance:** Complete audit trail for all return transactions and D365 reconciliation attempts

---

## User Stories

### Epic: Returns Management

| ID                                                                     | User Story                                                       | Story Points | Priority | Dependencies                 |
|------------------------------------------------------------------------|------------------------------------------------------------------|--------------|----------|------------------------------|
| [US-7.1.1](01-Handle-Partial-Order-Acceptance-Implementation-Plan.md)  | Handle Partial Order Acceptance                                  | 5            | High     | Picking Service              |
| [US-7.2.1](02-Process-Full-Order-Return-Implementation-Plan.md)        | Process Full Order Return                                        | 5            | High     | Picking Service              |
| [US-7.3.1](03-Handle-Damage-in-Transit-Returns-Implementation-Plan.md) | Handle Damage-in-Transit Returns                                 | 5            | High     | -                            |
| [US-7.4.1](04-Assign-Return-Location-Implementation-Plan.md)           | Assign Return Location                                           | 5            | High     | US-7.1.1, US-7.2.1, US-7.3.1 |
| [US-7.5.1](05-Reconcile-Returns-with-D365-Implementation-Plan.md)      | Reconcile Returns with D365                                      | 8            | High     | US-7.1.1, US-7.2.1           |
| -                                                                      | [Gateway API Tests](06-Gateway-API-Tests-Implementation-Plan.md) | -            | High     | All US                       |

**Total Story Points:** 28

---

## Technical Architecture

### Domain-Driven Design

Sprint 7 implements the following bounded contexts:

```
┌─────────────────────────────────────────────────────────────────┐
│                    Returns Service                               │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │              Domain Core (Pure Java)                     │  │
│  │  • Return Aggregate (PARTIAL, FULL)                      │  │
│  │  • ReturnLineItem Entity                                 │  │
│  │  • DamageAssessment Aggregate                            │  │
│  │  • DamagedProductItem Entity                             │  │
│  │  • ProductCondition (GOOD, DAMAGED, EXPIRED, WRITE_OFF)  │  │
│  │  • ReturnReason Enum                                     │  │
│  │  • Domain Events (ReturnProcessedEvent, etc.)            │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │         Application Service (Lombok allowed)             │  │
│  │  • ProcessPartialReturnCommandHandler                    │  │
│  │  • ProcessFullReturnCommandHandler                       │  │
│  │  • RecordDamageAssessmentCommandHandler                  │  │
│  │  • Repository Ports                                      │  │
│  └──────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│              Location Management Service                         │
│  • ReturnEventListener (listens to return events)               │
│  • ReturnLocationAssignmentStrategy                             │
│  • FEFO-based location assignment for good condition returns    │
│  • Quarantine zone assignment for damaged products              │
│  • Disposal area assignment for expired/write-off products      │
└──────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│              Integration Service (D365 Adapter)                  │
│  • D365ReturnReconciliationService                              │
│  • D365ODataClientAdapter                                       │
│  • OAuth Authentication Service                                 │
│  • Retry mechanism with exponential backoff                     │
│  • Audit trail for all D365 sync attempts                       │
└──────────────────────────────────────────────────────────────────┘
```

### Event-Driven Choreography

Sprint 7 uses **event-driven choreography** (no SAGA orchestrator):

```
Return Initiated
       │
       ▼
ReturnProcessedEvent ────┐
       │                 │
       ├─────────────────┼──────────────┐
       ▼                 ▼              ▼
Location Mgmt      Integration    Notification
Service            Service        Service
       │                 │              │
       ▼                 ▼              │
ReturnLocation      D365 Sync           │
AssignedEvent       Success             │
       │                 │              │
       ├─────────────────┤              │
       ▼                 ▼              ▼
Stock Mgmt        ReturnReconciledEvent
Service                  │
                        ▼
                   All Services
                   Updated
```

### Key Domain Events

| Event                         | Publisher             | Consumers                       | Purpose                            |
|-------------------------------|-----------------------|---------------------------------|------------------------------------|
| `ReturnInitiatedEvent`        | Returns Service       | Location Mgmt, Notification     | Signals return started             |
| `ReturnProcessedEvent`        | Returns Service       | Integration Service, Stock Mgmt | Triggers D365 sync                 |
| `DamageRecordedEvent`         | Returns Service       | Location Mgmt, Stock Mgmt       | Assigns quarantine locations       |
| `ReturnLocationAssignedEvent` | Location Mgmt Service | Stock Mgmt, Returns Service     | Updates stock at assigned location |
| `ReturnReconciledEvent`       | Integration Service   | Notification, Returns Service   | D365 sync completed                |

---

## Implementation Plans

### US-7.1.1: Handle Partial Order Acceptance

**File:** [01-Handle-Partial-Order-Acceptance-Implementation-Plan.md](01-Handle-Partial-Order-Acceptance-Implementation-Plan.md)

**Key Features:**

- Digital signature capture for customer acceptance
- Line-by-line acceptance tracking (accepted vs. returned quantities)
- Return reason documentation per line item
- Photo evidence upload support
- Validation: Sum of accepted + returned = picked quantity

**Endpoints:**

- `POST /api/returns/partial-return` - Process partial return

**Domain Objects:**

- `Return` aggregate (returnType = PARTIAL)
- `ReturnLineItem` entity with accepted/returned quantities
- `CustomerSignature` value object

---

### US-7.2.1: Process Full Order Return

**File:** [02-Process-Full-Order-Return-Implementation-Plan.md](02-Process-Full-Order-Return-Implementation-Plan.md)

**Key Features:**

- Full order rejection with primary return reason
- Product condition assessment (GOOD, DAMAGED, EXPIRED, WRITE_OFF)
- Condition-based stock routing
- Return statistics (good condition vs. damaged count)

**Endpoints:**

- `POST /api/returns/full-return` - Process full return
- `GET /api/returns/{returnId}` - Query return details
- `GET /api/returns` - List returns with filtering

**Domain Objects:**

- `Return` aggregate (returnType = FULL)
- `ProductCondition` enum
- `ReturnReason` enum

---

### US-7.3.1: Handle Damage-in-Transit Returns

**File:** [03-Handle-Damage-in-Transit-Returns-Implementation-Plan.md](03-Handle-Damage-in-Transit-Returns-Implementation-Plan.md)

**Key Features:**

- Comprehensive damage classification (type, severity, source)
- Multiple photo evidence per damaged item
- Insurance claim integration with carrier information
- Estimated loss value calculation
- Condition breakdown (quarantine, damaged, write-off counts)

**Endpoints:**

- `POST /api/returns/damage-assessments` - Record damage assessment

**Domain Objects:**

- `DamageAssessment` aggregate
- `DamagedProductItem` entity
- `DamageType`, `DamageSeverity`, `DamageSource` enums
- `InsuranceClaimInfo` value object

---

### US-7.4.1: Assign Return Location

**File:** [04-Assign-Return-Location-Implementation-Plan.md](04-Assign-Return-Location-Implementation-Plan.md)

**Key Features:**

- Event-driven location assignment (listens to return events)
- Intelligent routing strategy:
    - GOOD condition → Available stock locations using FEFO
    - DAMAGED/QUARANTINE → Quarantine zones
    - EXPIRED → Disposal areas
    - WRITE_OFF → Scrap locations
- Capacity validation before assignment
- Manual override option for special cases

**Endpoints:**

- `POST /api/location-management/return-assignments/auto-assign` - Auto-assign locations
- `POST /api/location-management/return-assignments/manual-assign` - Manual assignment
- `GET /api/location-management/return-assignments/pending` - Pending assignments

**Domain Objects:**

- `ReturnLocationAssignment` aggregate (Location Mgmt Service)
- `ReturnLocationAssignmentStrategy` domain service
- `ReturnLocationAssignedEvent`

---

### US-7.5.1: Reconcile Returns with D365

**File:** [05-Reconcile-Returns-with-D365-Implementation-Plan.md](05-Reconcile-Returns-with-D365-Implementation-Plan.md)

**Key Features:**

- Bidirectional D365 integration via OData API
- Automatic reconciliation triggered by `ReturnProcessedEvent`
- D365 operations:
    - Create return order in D365
    - Update inventory based on product condition
    - Issue credit notes for good condition returns
    - Record write-offs for damaged/expired products
- Retry mechanism with exponential backoff (max 5 attempts)
- Complete audit trail for all sync attempts

**Endpoints:**

- `GET /api/returns/reconciliation/records` - List reconciliation records
- `GET /api/returns/reconciliation/{returnId}/audit-trail` - Audit trail
- `POST /api/returns/reconciliation/{returnId}/retry-sync` - Retry failed sync
- `GET /api/returns/reconciliation/summary` - Summary report

**Integration Components:**

- `D365ReturnReconciliationService`
- `D365ODataClientAdapter` (implements D365ClientPort)
- `D365AuthenticationService` (OAuth token management)
- `ReturnReconciliationEventListener`

---

### Gateway API Tests

**File:** [06-Gateway-API-Tests-Implementation-Plan.md](06-Gateway-API-Tests-Implementation-Plan.md)

**Test Coverage:**

- Partial return processing (US-7.1.1)
- Full return processing (US-7.2.1)
- Damage assessment (US-7.3.1)
- Location assignment scenarios
- D365 reconciliation with async validation
- Query endpoints (filtering, pagination)
- Validation scenarios (negative tests)

**Test Classes:**

- `ReturnsServiceTest` - Returns processing tests
- `DamageAssessmentTest` - Damage assessment tests
- `ReturnReconciliationTest` - D365 reconciliation tests

---

## Database Schema Updates

### Returns Service

**New Tables:**

1. **returns** - Main return records
    - `return_id` (UUID, PK)
    - `order_id` (UUID, FK)
    - `load_number` (VARCHAR)
    - `customer_id` (UUID, FK)
    - `return_type` (ENUM: PARTIAL, FULL)
    - `status` (ENUM: INITIATED, PROCESSING, LOCATION_ASSIGNED, COMPLETED, RECONCILED, CANCELLED)
    - `primary_return_reason` (VARCHAR)
    - `customer_signature_url` (TEXT)
    - `returned_at` (TIMESTAMP)
    - `tenant_id` (UUID)

2. **return_line_items** - Line items for returns
    - `line_item_id` (UUID, PK)
    - `return_id` (UUID, FK)
    - `product_id` (UUID)
    - `ordered_quantity` (DECIMAL)
    - `picked_quantity` (DECIMAL)
    - `accepted_quantity` (DECIMAL)
    - `returned_quantity` (DECIMAL)
    - `return_reason` (VARCHAR)
    - `product_condition` (ENUM)
    - `line_notes` (TEXT)

3. **damage_assessments** - Damage records
    - `damage_assessment_id` (UUID, PK)
    - `order_id` (UUID, FK)
    - `damage_type` (ENUM)
    - `damage_severity` (ENUM)
    - `damage_source` (ENUM)
    - `estimated_total_loss` (DECIMAL)
    - `status` (ENUM)
    - `claim_reference` (VARCHAR)

4. **damaged_product_items** - Damaged products
    - `item_id` (UUID, PK)
    - `damage_assessment_id` (UUID, FK)
    - `product_id` (UUID)
    - `damaged_quantity` (DECIMAL)
    - `product_condition` (ENUM)
    - `estimated_unit_loss` (DECIMAL)

### Integration Service

**New Tables:**

1. **d365_reconciliation_records** - D365 sync tracking
    - `record_id` (UUID, PK)
    - `return_id` (UUID, FK)
    - `d365_return_order_id` (VARCHAR)
    - `sync_status` (ENUM: PENDING, SYNCED, FAILED, RETRYING)
    - `sync_attempts` (INT)
    - `last_sync_attempt` (TIMESTAMP)
    - `error_message` (TEXT)
    - `d365_response` (JSON)

---

## Frontend Implementation

### New Pages

1. **PartialReturnPage** (`/returns/partial-return`)
    - Order selection
    - Line-by-line acceptance form
    - Customer signature canvas
    - Evidence upload

2. **FullReturnPage** (`/returns/full-return`)
    - Order selection
    - Return reason selection
    - Product condition assessment
    - Return summary

3. **DamageAssessmentPage** (`/returns/damage-assessment`)
    - Damage classification
    - Product-specific damage recording
    - Photo evidence upload
    - Insurance claim information

4. **ReturnLocationAssignmentDashboard** (`/location-management/return-assignments`)
    - Pending returns summary
    - Auto-assignment actions
    - Manual location override

5. **ReturnReconciliationDashboard** (`/returns/reconciliation`)
    - Reconciliation summary metrics
    - D365 sync status tracking
    - Retry failed syncs
    - Audit trail viewer

### React Hooks

- `useProcessPartialReturn()`
- `useProcessFullReturn()`
- `useRecordDamageAssessment()`
- `useAutoAssignReturnLocations()`
- `useReconciliationRecords()`
- `useRetryD365Sync()`

---

## Configuration Requirements

### Application Configuration

**`services/returns-service/returns-container/src/main/resources/application.yml`**

```yaml
returns:
  signature:
    max-size-mb: 5
    allowed-formats: [png, jpg, jpeg]
  evidence:
    max-files: 10
    max-size-mb: 10
  damage-photos:
    max-per-product: 10
    max-size-mb: 5

kafka:
  topics:
    return-initiated: return.initiated
    return-processed: return.processed
    damage-recorded: damage.recorded
    return-location-assigned: return.location.assigned
    return-reconciled: return.reconciled
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
  retry:
    max-attempts: 5
    initial-delay-ms: 2000
    max-delay-ms: 30000
    multiplier: 2.0
```

### Environment Variables

```bash
# D365 Integration (required for US-7.5.1)
export D365_TENANT_ID=your-tenant-id
export D365_CLIENT_ID=your-client-id
export D365_CLIENT_SECRET=your-client-secret
export D365_RESOURCE=https://your-org.crm.dynamics.com
export D365_API_BASE_URL=https://your-org.api.crm.dynamics.com

# File Storage (for signatures and photos)
export AWS_S3_BUCKET=wms-evidence-storage
export AWS_S3_REGION=us-east-1
export AWS_ACCESS_KEY_ID=your-access-key
export AWS_SECRET_ACCESS_KEY=your-secret-key
```

---

## Testing Strategy

### Unit Tests

- Domain core logic (aggregates, value objects, domain services)
- Application service command handlers
- D365 adapter logic
- Location assignment strategy

**Target Coverage:** >90%

### Integration Tests

- Repository adapters with test containers
- Event publishing and consumption
- REST API endpoints

**Target Coverage:** >85%

### End-to-End Tests (Gateway API Tests)

- Complete return workflows (partial, full, damage)
- D365 reconciliation with async validation
- Location assignment scenarios
- Query operations

**Target Coverage:** 100% of endpoints

---

## Implementation Order

### Phase 1: Foundation (Days 1-3)

1. Common module updates (enums, value objects)
2. Returns Service - Domain Core
3. Database schema migration
4. Base repository implementations

### Phase 2: Core Features (Days 4-6)

5. US-7.1.1 - Partial returns (backend + frontend)
6. US-7.2.1 - Full returns (backend + frontend)
7. US-7.3.1 - Damage assessment (backend + frontend)

### Phase 3: Integration (Days 7-9)

8. US-7.4.1 - Location assignment (event-driven)
9. US-7.5.1 - D365 reconciliation (Integration Service)
10. Event listeners and choreography

### Phase 4: Testing & Validation (Days 10)

11. Gateway API Tests
12. Integration testing
13. Bug fixes and refinements

---

## Acceptance Criteria Summary

| User Story | Total ACs | Status          |
|------------|-----------|-----------------|
| US-7.1.1   | 6         | ✅ All validated |
| US-7.2.1   | 6         | ✅ All validated |
| US-7.3.1   | 6         | ✅ All validated |
| US-7.4.1   | 6         | ✅ All validated |
| US-7.5.1   | 6         | ✅ All validated |

**Total:** 30 acceptance criteria, all validated in implementation plans

---

## Risk Mitigation

### Technical Risks

| Risk                      | Mitigation                                                                          |
|---------------------------|-------------------------------------------------------------------------------------|
| D365 integration failures | Retry mechanism with exponential backoff, complete audit trail, manual retry option |
| Large photo uploads       | File size limits (5MB), compression on frontend, S3 direct upload                   |
| Event processing delays   | Asynchronous processing with status tracking, timeout configurations                |
| Location capacity issues  | Real-time capacity validation, fallback to manual assignment                        |

### Operational Risks

| Risk                            | Mitigation                                                                |
|---------------------------------|---------------------------------------------------------------------------|
| Incomplete damage documentation | Required photo evidence validation, minimum photo count enforcement       |
| Incorrect condition assessment  | Clear UI guidelines, training documentation, audit trail                  |
| D365 sync monitoring            | Reconciliation dashboard, alerting on failed syncs, daily summary reports |

---

## Success Metrics

### Sprint Goals

- ✅ All 5 user stories implemented with full test coverage
- ✅ D365 integration tested in sandbox environment
- ✅ Gateway API tests passing (100% endpoint coverage)
- ✅ Frontend components integrated and functional
- ✅ Documentation complete

### Performance Targets

- Return processing: < 2 seconds (P95)
- D365 sync latency: < 10 seconds (P95)
- Signature upload: < 1 second (P95)
- Photo upload: < 2 seconds per photo (P95)
- Reconciliation success rate: > 99.5%

---

## Dependencies

### External Services

- Microsoft Dynamics 365 (OData API v9.2)
- AWS S3 (or equivalent) for signature and photo storage
- Kafka for event streaming

### Internal Services

- Picking Service (order and picking list data)
- Location Management Service (location queries and assignments)
- Stock Management Service (stock level updates)
- Product Service (product information)
- Notification Service (alerts for managers)

---

## Documentation

### Implementation Plans

1. [00-Sprint-07-Implementation-Plan.md](00-Sprint-07-Implementation-Plan.md) - Master plan
2. [01-Handle-Partial-Order-Acceptance-Implementation-Plan.md](01-Handle-Partial-Order-Acceptance-Implementation-Plan.md)
3. [02-Process-Full-Order-Return-Implementation-Plan.md](02-Process-Full-Order-Return-Implementation-Plan.md)
4. [03-Handle-Damage-in-Transit-Returns-Implementation-Plan.md](03-Handle-Damage-in-Transit-Returns-Implementation-Plan.md)
5. [04-Assign-Return-Location-Implementation-Plan.md](04-Assign-Return-Location-Implementation-Plan.md)
6. [05-Reconcile-Returns-with-D365-Implementation-Plan.md](05-Reconcile-Returns-with-D365-Implementation-Plan.md)
7. [06-Gateway-API-Tests-Implementation-Plan.md](06-Gateway-API-Tests-Implementation-Plan.md)

### Architecture Documents

- [Service Architecture Document](../../01-architecture/Service_Architecture_Document.md)
- [Frontend Architecture Document](../../01-architecture/Frontend_Architecture_Document.md)
- [Clean Code Guidelines](../../guide/clean-code-guidelines-per-module.md)

---

## Next Steps (Post-Sprint 7)

### Sprint 8: Reconciliation & Reporting

- Stock reconciliation (cycle counting)
- Discrepancy investigation workflow
- Reconciliation reports for management

### Sprint 9: Advanced Returns

- Return Authorization workflow
- Credit note generation
- Return analytics and dashboards

---

## Team Allocation

**Backend Developers (3):**

- Developer 1: Returns Service domain core and application service
- Developer 2: Integration Service (D365 adapter)
- Developer 3: Location Management Service updates, event listeners

**Frontend Developers (2):**

- Developer 1: Partial/Full return pages, signature capture
- Developer 2: Damage assessment, reconciliation dashboard

**QA Engineers (1):**

- Gateway API tests, integration testing, UAT coordination

**DevOps (0.5):**

- D365 sandbox configuration, S3 bucket setup, monitoring setup

---

**Sprint 7 Status:** ✅ Ready for Implementation
**Last Updated:** 2026-01-09
**Next Review:** Sprint Planning Meeting
