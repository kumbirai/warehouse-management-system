# Sprint 8 Implementation Plan

## Warehouse Management System Integration - CCBSA LDP System

**Sprint:** Sprint 8 - Reconciliation
**Duration:** 2 weeks
**Sprint Goal:** Implement comprehensive stock counting and reconciliation workflows including worksheet generation, stock count entry, variance investigation, and D365 reconciliation
**Total Story Points:** 34

---

## Table of Contents

1. [Sprint Overview](#sprint-overview)
2. [User Stories](#user-stories)
3. [Implementation Approach](#implementation-approach)
4. [Architecture Compliance](#architecture-compliance)
5. [Data Flow Design](#data-flow-design)
6. [Service Segregation](#service-segregation)
7. [Database Schema](#database-schema)
8. [Frontend Structure](#frontend-structure)
9. [Configuration Requirements](#configuration-requirements)
10. [Testing Strategy](#testing-strategy)
11. [Implementation Order](#implementation-order)
12. [Dependencies and Prerequisites](#dependencies-and-prerequisites)
13. [Risk Mitigation](#risk-mitigation)
14. [Definition of Done](#definition-of-done)
15. [Sprint Success Metrics](#sprint-success-metrics)

---

## Sprint Overview

### Sprint Goal

Implement the complete stock reconciliation workflow to enable warehouse operators to:

1. **Generate Electronic Stock Count Worksheet** - Create digital worksheets for stock counting activities
2. **Perform Stock Count Entry** - Record counted quantities via mobile devices with offline support
3. **Complete Stock Count** - Finalize counts and identify variances
4. **Investigate Stock Count Variances** - Research and resolve stock discrepancies
5. **Reconcile Stock Counts with D365** - Synchronize stock count data with Microsoft Dynamics 365 (optional)

### Business Value

- **Digital Transformation:** Replace paper-based stock counting with electronic worksheets
- **Accuracy Improvement:** Automated variance calculation and highlighting
- **Investigation Workflow:** Structured process for variance resolution with approval gates
- **Financial Accuracy:** Accurate inventory valuation through regular stock reconciliation
- **D365 Integration:** Bidirectional synchronization ensuring system accuracy
- **Audit Trail:** Complete record of all stock counts and investigations

### Success Criteria

- ✅ Warehouse managers can generate stock count worksheets for specific locations/products
- ✅ Operators can record counted quantities via mobile devices with offline support
- ✅ System automatically calculates variances and severity classifications
- ✅ Supervisors can investigate and resolve variances with notes and approvals
- ✅ System reconciles stock counts with D365 when integration is enabled
- ✅ All data flows correctly from frontend through gateway to backend services
- ✅ Gateway API tests validate all endpoints
- ✅ All implementations follow DDD, Clean Hexagonal Architecture, CQRS, and Event-Driven Choreography principles

---

## User Stories

### Story 1: US-8.1.1 - Generate Electronic Stock Count Worksheet (8 points)

**Service:** Reconciliation Service
**Priority:** Must Have

**Acceptance Criteria:**

- System generates stock count worksheets based on location selection
- System includes current system quantities from Stock Management Service
- System supports filtering by product category, classification, or expiry date
- System assigns unique worksheet identifier
- System tracks worksheet status (DRAFT, IN_PROGRESS, COMPLETED, CANCELLED)
- System publishes `StockCountInitiatedEvent`
- Worksheet includes location barcode, product barcode, expected quantity columns

**Implementation Plan:** [01-Generate-Stock-Count-Worksheet-Implementation-Plan.md](01-Generate-Stock-Count-Worksheet-Implementation-Plan.md)

---

### Story 2: US-8.1.2 - Perform Stock Count Entry (8 points)

**Service:** Reconciliation Service
**Priority:** Must Have

**Acceptance Criteria:**

- System supports barcode scanning for location and product identification
- System displays expected quantity for reference
- System allows manual quantity entry
- System validates entries (no duplicates, valid products/locations)
- System supports offline entry with IndexedDB storage
- System publishes `StockCountEntryRecordedEvent` for each entry
- System provides running total and progress indicator

**Implementation Plan:** [02-Perform-Stock-Count-Entry-Implementation-Plan.md](02-Perform-Stock-Count-Entry-Implementation-Plan.md)

---

### Story 3: US-8.1.3 - Complete Stock Count (5 points)

**Service:** Reconciliation Service
**Priority:** Must Have

**Acceptance Criteria:**

- System calculates variances (counted - system quantity)
- System classifies variance severity (LOW, MEDIUM, HIGH, CRITICAL) based on thresholds
- System displays variance summary with color coding
- System requires review confirmation before completion
- System publishes `StockCountCompletedEvent`
- System publishes `StockCountVarianceIdentifiedEvent` for significant variances
- System prevents completion if critical variances are unresolved

**Implementation Plan:** [03-Complete-Stock-Count-Implementation-Plan.md](03-Complete-Stock-Count-Implementation-Plan.md)

---

### Story 4: US-8.2.1 - Investigate Stock Count Variances (5 points)

**Service:** Reconciliation Service
**Priority:** Must Have

**Acceptance Criteria:**

- System displays variance details with historical movement data
- System allows investigation notes and reason code assignment
- System tracks investigation status (PENDING, IN_PROGRESS, RESOLVED, ESCALATED)
- System requires manager approval for HIGH/CRITICAL variances
- System publishes `VarianceInvestigatedEvent`
- System publishes `VarianceResolvedEvent` upon resolution
- System maintains complete audit trail of investigation activities

**Implementation Plan:** [04-Investigate-Stock-Count-Variances-Implementation-Plan.md](04-Investigate-Stock-Count-Variances-Implementation-Plan.md)

---

### Story 5: US-8.3.1 - Reconcile Stock Counts with D365 (8 points)

**Service:** Reconciliation Service, Integration Service
**Priority:** Should Have

**Acceptance Criteria:**

- System reconciles completed stock counts with D365 (if integration enabled)
- Reconciliation includes: stock count journal ID, variances, adjusted quantities
- System handles reconciliation errors with retry mechanism (exponential backoff)
- System maintains reconciliation audit trail with all sync attempts
- System publishes `ReconciliationInitiatedEvent`, `ReconciliationCompletedEvent`, `ReconciliationFailedEvent`
- Dashboard displays reconciliation status and allows manual retry

**Implementation Plan:** [05-Reconcile-Stock-Counts-D365-Implementation-Plan.md](05-Reconcile-Stock-Counts-D365-Implementation-Plan.md)

---

### Gateway API Tests

**File:** [06-Gateway-API-Tests-Implementation-Plan.md](06-Gateway-API-Tests-Implementation-Plan.md)

**Test Coverage:**

- Stock count worksheet generation (US-8.1.1)
- Stock count entry recording (US-8.1.2)
- Stock count completion with variance calculation (US-8.1.3)
- Variance investigation and resolution (US-8.2.1)
- D365 reconciliation with async validation (US-8.3.1)
- Query endpoints (filtering, pagination)
- Validation scenarios (negative tests)

**Test Classes:**

- `StockCountTest` - Stock count workflow tests
- `VarianceInvestigationTest` - Variance investigation tests
- `D365ReconciliationTest` - D365 reconciliation tests

---

## Implementation Approach

### Frontend-First Design

All implementation plans start with **production-grade UI design** to ensure:

1. **User Experience** - Intuitive mobile-first interfaces for warehouse floor operations
2. **Offline Capability** - IndexedDB storage with background sync for stock count entry
3. **Real-Time Feedback** - Live variance calculation and progress tracking
4. **Error Handling** - Clear error messages and recovery paths
5. **Visual Indicators** - Color-coded variance severity, status badges, progress bars
6. **Accessibility** - WCAG 2.1 Level AA compliance from the start
7. **Mobile Support** - PWA functionality with barcode scanner integration

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

- **Synchronous Calls:** Stock level queries and location validation
- **Circuit Breaker:** Resilience patterns for service calls
- **Event-Driven:** Stock count completion triggers stock adjustments
- **Error Handling:** Graceful degradation when services unavailable

---

## Architecture Compliance

### Domain-Driven Design (DDD)

- **Bounded Contexts:** Clear service boundaries (Reconciliation Service, Integration Service, Stock Management Service)
- **Aggregates:** StockCount, StockCountVariance, D365Reconciliation are aggregate roots
- **Value Objects:** StockCountId, VarianceReason, StockCountStatus, InvestigationStatus, etc.
- **Domain Events:** StockCountInitiatedEvent, StockCountCompletedEvent, VarianceResolvedEvent, ReconciliationCompletedEvent
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

### Generate Stock Count Worksheet Flow

```
Frontend (React)
  ↓ POST /api/v1/reconciliation/stock-counts/generate-worksheet
Gateway Service
  ↓ Route to Reconciliation Service
Reconciliation Service (Command Controller)
  ↓ GenerateStockCountWorksheetCommand
Command Handler
  ↓ Query Stock Management Service for current quantities
  ↓ Query Location Management Service for location details
  ↓ StockCount.initiate()
  Domain Core (StockCount Aggregate)
  ↓ StockCountInitiatedEvent
Event Publisher
  ↓ Kafka Topic: reconciliation-events
Notification Service (Event Listener)
  ↓ Notify assigned operators
Query Handler
  ↓ StockCountWorksheetResult
Query Controller
  ↓ Response with worksheet ID and entries
Gateway Service
  ↓ Response
Frontend (React)
  ↓ Navigate to stock count entry page
```

### Stock Count Entry Flow (Mobile)

```
Frontend (PWA/React)
  ↓ Scan location barcode
  ↓ Scan product barcode
  ↓ Enter counted quantity
  ↓ POST /api/v1/reconciliation/stock-counts/{id}/entries
Gateway Service
  ↓ Route to Reconciliation Service
Reconciliation Service (Command Controller)
  ↓ RecordStockCountEntryCommand
Command Handler
  ↓ Validate no duplicate entry exists
  ↓ Validate product exists in location
  ↓ StockCount.recordEntry()
  Domain Core (StockCount Aggregate)
  ↓ StockCountEntryRecordedEvent
Event Publisher
  ↓ Kafka Topic: reconciliation-events
Repository Adapter
  ↓ Save to PostgreSQL
Query Handler
  ↓ StockCountEntryResult
Query Controller
  ↓ Response with entry confirmation
Gateway Service
  ↓ Response
Frontend (React)
  ↓ Update UI with running total
  ↓ Store in IndexedDB (offline support)
```

### Complete Stock Count Flow

```
Frontend (React)
  ↓ POST /api/v1/reconciliation/stock-counts/{id}/complete
Gateway Service
  ↓ Route to Reconciliation Service
Reconciliation Service (Command Controller)
  ↓ CompleteStockCountCommand
Command Handler
  ↓ Calculate variances for all entries
  ↓ Classify variance severity
  ↓ StockCount.complete()
  Domain Core (StockCount Aggregate)
  ↓ StockCountCompletedEvent
  ↓ StockCountVarianceIdentifiedEvent (for each significant variance)
Event Publisher
  ↓ Kafka Topic: reconciliation-events
Stock Management Service (Event Listener)
  ↓ Update system quantities based on count results
Integration Service (Event Listener) [OPTIONAL]
  ↓ Trigger D365 reconciliation
Notification Service (Event Listener)
  ↓ Alert supervisors of critical variances
Query Handler
  ↓ StockCountCompletionResult with variance summary
Query Controller
  ↓ Response
Gateway Service
  ↓ Response
Frontend (React)
  ↓ Display variance summary with color coding
```

### Variance Investigation Flow

```
Frontend (React)
  ↓ POST /api/v1/reconciliation/variances/{id}/investigate
Gateway Service
  ↓ Route to Reconciliation Service
Reconciliation Service (Command Controller)
  ↓ InvestigateVarianceCommand
Command Handler
  ↓ Query Stock Management Service for movement history
  ↓ StockCountVariance.investigate()
  Domain Core (StockCountVariance Aggregate)
  ↓ Validate investigation notes provided
  ↓ VarianceInvestigatedEvent
Event Publisher
  ↓ Kafka Topic: reconciliation-events
Repository Adapter
  ↓ Save investigation record
---
Frontend (React) - Resolution
  ↓ POST /api/v1/reconciliation/variances/{id}/resolve
Gateway Service
  ↓ Route to Reconciliation Service
Reconciliation Service (Command Controller)
  ↓ ResolveVarianceCommand
Command Handler
  ↓ Validate manager approval for HIGH/CRITICAL
  ↓ StockCountVariance.resolve()
  Domain Core (StockCountVariance Aggregate)
  ↓ VarianceResolvedEvent
Event Publisher
  ↓ Kafka Topic: reconciliation-events
Stock Management Service (Event Listener)
  ↓ Finalize stock adjustments
Notification Service (Event Listener)
  ↓ Notify stakeholders of resolution
```

### D365 Reconciliation Flow (Optional)

```
Reconciliation Service
  ↓ Listen to StockCountCompletedEvent
  ↓ Prepare reconciliation data
Integration Service (Event Listener) [OPTIONAL - if D365 enabled]
  ↓ ReconciliationInitiatedEvent
  ↓ Transform to D365 counting journal format
  ↓ Authenticate with OAuth 2.0
  ↓ POST to D365 OData API: /api/data/v9.2/msdyn_inventorycountingjournals
  ↓ Handle response
---
If Success:
  ↓ ReconciliationCompletedEvent
  ↓ Update reconciliation status to SUCCESS
  ↓ Kafka Topic: reconciliation-events
---
If Failure:
  ↓ ReconciliationFailedEvent
  ↓ Retry with exponential backoff (max 5 attempts)
  ↓ Dead letter queue after max retries
  ↓ Kafka Topic: reconciliation-events
---
Notification Service (Event Listener)
  ↓ Alert on failure requiring manual intervention
```

---

## Service Segregation

### Reconciliation Service

**Responsibilities:**

- Generate stock count worksheets
- Record stock count entries
- Calculate variances and classify severity
- Manage variance investigation workflow
- Track stock count status and history
- Publish reconciliation events

**Aggregates:**

- `StockCount` - Represents stock count with entries
- `StockCountEntry` - Individual product count within a stock count
- `StockCountVariance` - Variance details and investigation status

**Events Published:**

- `StockCountInitiatedEvent`
- `StockCountEntryRecordedEvent`
- `StockCountCompletedEvent`
- `StockCountVarianceIdentifiedEvent`
- `VarianceInvestigatedEvent`
- `VarianceResolvedEvent`
- `StockCountCancelledEvent`

**Events Consumed:**

- `TenantSchemaCreatedEvent` (from Tenant Service) - Create reconciliation tables

---

### Stock Management Service

**Responsibilities:**

- Provide current system quantities for worksheets
- Update stock levels based on completed counts
- Track stock adjustment history
- Maintain stock accuracy metrics

**Aggregates:**

- `StockItem` - Updated with count results
- `StockAdjustment` - Created for variance corrections

**Events Published:**

- `StockLevelAdjustedEvent`
- `StockAccuracyUpdatedEvent`

**Events Consumed:**

- `StockCountCompletedEvent` (from Reconciliation Service)
- `VarianceResolvedEvent` (from Reconciliation Service)

---

### Location Management Service

**Responsibilities:**

- Provide location details for worksheet generation
- Validate location barcodes during entry
- Track stock count activity by location

**Events Consumed:**

- `StockCountInitiatedEvent` (from Reconciliation Service)

---

### Integration Service (Optional - D365 Integration)

**Responsibilities:**

- Send stock count data to D365 (if integration enabled)
- Create counting journals in D365
- Handle D365 API errors with retry logic
- Track D365 reconciliation status

**Aggregates:**

- `D365Reconciliation` - Tracks sync status and attempts

**Events Published:**

- `ReconciliationInitiatedEvent`
- `ReconciliationCompletedEvent`
- `ReconciliationFailedEvent`

**Events Consumed:**

- `StockCountCompletedEvent` (from Reconciliation Service)

---

### Notification Service

**Responsibilities:**

- Create notifications for stock count events
- Alert supervisors of critical variances
- Notify managers of pending approvals
- Display reconciliation alerts in dashboard

**Events Consumed:**

- `StockCountInitiatedEvent` (from Reconciliation Service)
- `StockCountCompletedEvent` (from Reconciliation Service)
- `StockCountVarianceIdentifiedEvent` (from Reconciliation Service)
- `ReconciliationFailedEvent` (from Integration Service)

---

## Database Schema

### Reconciliation Service Tables

#### 1. stock_counts

Main table for stock count records.

```sql
CREATE TABLE stock_counts (
    stock_count_id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    count_reference VARCHAR(50) NOT NULL UNIQUE,
    count_type VARCHAR(20) NOT NULL, -- CYCLE_COUNT, FULL_INVENTORY, SPOT_CHECK
    status VARCHAR(20) NOT NULL, -- DRAFT, IN_PROGRESS, COMPLETED, CANCELLED
    location_filter JSON, -- Stores location selection criteria
    product_filter JSON, -- Stores product filter criteria
    initiated_by UUID NOT NULL,
    initiated_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP,
    completion_notes TEXT,
    total_entries INT DEFAULT 0,
    total_variances INT DEFAULT 0,
    critical_variances INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    version INT DEFAULT 0,
    CONSTRAINT fk_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(tenant_id)
);

CREATE INDEX idx_stock_counts_tenant ON stock_counts(tenant_id);
CREATE INDEX idx_stock_counts_status ON stock_counts(status);
CREATE INDEX idx_stock_counts_initiated_at ON stock_counts(initiated_at);
CREATE INDEX idx_stock_counts_reference ON stock_counts(count_reference);
```

#### 2. stock_count_entries

Individual product entries within a stock count.

```sql
CREATE TABLE stock_count_entries (
    entry_id UUID PRIMARY KEY,
    stock_count_id UUID NOT NULL,
    location_id UUID NOT NULL,
    product_id UUID NOT NULL,
    location_barcode VARCHAR(100),
    product_barcode VARCHAR(100),
    system_quantity DECIMAL(10,2) NOT NULL,
    counted_quantity DECIMAL(10,2) NOT NULL,
    variance_quantity DECIMAL(10,2) GENERATED ALWAYS AS (counted_quantity - system_quantity) STORED,
    variance_percentage DECIMAL(5,2) GENERATED ALWAYS AS (
        CASE
            WHEN system_quantity = 0 THEN 100.00
            ELSE ((counted_quantity - system_quantity) / system_quantity * 100)
        END
    ) STORED,
    recorded_by UUID NOT NULL,
    recorded_at TIMESTAMP NOT NULL,
    entry_notes TEXT,
    created_at TIMESTAMP DEFAULT NOW(),
    CONSTRAINT fk_stock_count FOREIGN KEY (stock_count_id) REFERENCES stock_counts(stock_count_id) ON DELETE CASCADE,
    CONSTRAINT uq_count_location_product UNIQUE(stock_count_id, location_id, product_id)
);

CREATE INDEX idx_entries_stock_count ON stock_count_entries(stock_count_id);
CREATE INDEX idx_entries_location ON stock_count_entries(location_id);
CREATE INDEX idx_entries_product ON stock_count_entries(product_id);
CREATE INDEX idx_entries_recorded_at ON stock_count_entries(recorded_at);
```

#### 3. stock_count_variances

Variance records with investigation tracking.

```sql
CREATE TABLE stock_count_variances (
    variance_id UUID PRIMARY KEY,
    stock_count_id UUID NOT NULL,
    entry_id UUID NOT NULL,
    location_id UUID NOT NULL,
    product_id UUID NOT NULL,
    variance_quantity DECIMAL(10,2) NOT NULL,
    variance_percentage DECIMAL(5,2) NOT NULL,
    absolute_variance_value DECIMAL(12,2), -- Monetary value of variance
    severity VARCHAR(20) NOT NULL, -- LOW, MEDIUM, HIGH, CRITICAL
    investigation_status VARCHAR(20) NOT NULL, -- PENDING, IN_PROGRESS, RESOLVED, ESCALATED
    variance_reason VARCHAR(50), -- DAMAGE, THEFT, COUNTING_ERROR, SYSTEM_ERROR, EXPIRY, OTHER
    investigation_notes TEXT,
    resolution_notes TEXT,
    investigated_by UUID,
    investigated_at TIMESTAMP,
    resolved_by UUID,
    resolved_at TIMESTAMP,
    approved_by UUID, -- Required for HIGH/CRITICAL variances
    approved_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    CONSTRAINT fk_variance_stock_count FOREIGN KEY (stock_count_id) REFERENCES stock_counts(stock_count_id) ON DELETE CASCADE,
    CONSTRAINT fk_variance_entry FOREIGN KEY (entry_id) REFERENCES stock_count_entries(entry_id) ON DELETE CASCADE
);

CREATE INDEX idx_variances_stock_count ON stock_count_variances(stock_count_id);
CREATE INDEX idx_variances_entry ON stock_count_variances(entry_id);
CREATE INDEX idx_variances_severity ON stock_count_variances(severity);
CREATE INDEX idx_variances_status ON stock_count_variances(investigation_status);
CREATE INDEX idx_variances_created_at ON stock_count_variances(created_at);
```

#### 4. variance_investigation_history

Audit trail for variance investigations.

```sql
CREATE TABLE variance_investigation_history (
    history_id UUID PRIMARY KEY,
    variance_id UUID NOT NULL,
    action VARCHAR(50) NOT NULL, -- INVESTIGATION_STARTED, NOTE_ADDED, STATUS_CHANGED, RESOLVED, ESCALATED
    performed_by UUID NOT NULL,
    performed_at TIMESTAMP NOT NULL,
    previous_status VARCHAR(20),
    new_status VARCHAR(20),
    notes TEXT,
    metadata JSON,
    created_at TIMESTAMP DEFAULT NOW(),
    CONSTRAINT fk_history_variance FOREIGN KEY (variance_id) REFERENCES stock_count_variances(variance_id) ON DELETE CASCADE
);

CREATE INDEX idx_history_variance ON variance_investigation_history(variance_id);
CREATE INDEX idx_history_performed_at ON variance_investigation_history(performed_at);
```

### Integration Service Tables (D365 Reconciliation)

#### 5. d365_stock_count_reconciliations

Tracks D365 synchronization status.

```sql
CREATE TABLE d365_stock_count_reconciliations (
    reconciliation_id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    stock_count_id UUID NOT NULL,
    d365_journal_id VARCHAR(255), -- D365 counting journal ID
    d365_journal_number VARCHAR(100), -- Human-readable journal number
    reconciliation_status VARCHAR(20) NOT NULL, -- PENDING, IN_PROGRESS, SUCCESS, FAILED, RETRYING
    sync_attempts INT DEFAULT 0,
    max_attempts INT DEFAULT 5,
    last_sync_attempt TIMESTAMP,
    next_retry_at TIMESTAMP,
    error_message TEXT,
    error_code VARCHAR(50),
    d365_request_payload JSON,
    d365_response_payload JSON,
    sync_duration_ms BIGINT,
    initiated_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    CONSTRAINT fk_reconciliation_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(tenant_id)
);

CREATE INDEX idx_reconciliations_tenant ON d365_stock_count_reconciliations(tenant_id);
CREATE INDEX idx_reconciliations_stock_count ON d365_stock_count_reconciliations(stock_count_id);
CREATE INDEX idx_reconciliations_status ON d365_stock_count_reconciliations(reconciliation_status);
CREATE INDEX idx_reconciliations_d365_journal ON d365_stock_count_reconciliations(d365_journal_id);
CREATE INDEX idx_reconciliations_next_retry ON d365_stock_count_reconciliations(next_retry_at);
```

#### 6. d365_reconciliation_audit_log

Complete audit trail for all D365 sync attempts.

```sql
CREATE TABLE d365_reconciliation_audit_log (
    audit_id UUID PRIMARY KEY,
    reconciliation_id UUID NOT NULL,
    attempt_number INT NOT NULL,
    attempt_timestamp TIMESTAMP NOT NULL,
    http_status_code INT,
    request_payload JSON,
    response_payload JSON,
    error_details TEXT,
    retry_after_seconds INT,
    created_at TIMESTAMP DEFAULT NOW(),
    CONSTRAINT fk_audit_reconciliation FOREIGN KEY (reconciliation_id) REFERENCES d365_stock_count_reconciliations(reconciliation_id) ON DELETE CASCADE
);

CREATE INDEX idx_audit_reconciliation ON d365_reconciliation_audit_log(reconciliation_id);
CREATE INDEX idx_audit_timestamp ON d365_reconciliation_audit_log(attempt_timestamp);
```

### Stock Management Service Updates

#### 7. stock_adjustments

Tracks stock adjustments from completed counts.

```sql
CREATE TABLE stock_adjustments (
    adjustment_id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    stock_item_id UUID NOT NULL,
    adjustment_type VARCHAR(50) NOT NULL, -- STOCK_COUNT_VARIANCE, DAMAGE, EXPIRY, etc.
    adjustment_quantity DECIMAL(10,2) NOT NULL, -- Can be positive or negative
    previous_quantity DECIMAL(10,2) NOT NULL,
    new_quantity DECIMAL(10,2) NOT NULL,
    reason_code VARCHAR(50),
    reference_id UUID, -- Links to stock_count_id or variance_id
    reference_type VARCHAR(50), -- STOCK_COUNT, VARIANCE_RESOLUTION
    adjusted_by UUID NOT NULL,
    adjusted_at TIMESTAMP NOT NULL,
    notes TEXT,
    created_at TIMESTAMP DEFAULT NOW(),
    CONSTRAINT fk_adjustment_stock_item FOREIGN KEY (stock_item_id) REFERENCES stock_items(stock_item_id)
);

CREATE INDEX idx_adjustments_stock_item ON stock_adjustments(stock_item_id);
CREATE INDEX idx_adjustments_reference ON stock_adjustments(reference_id);
CREATE INDEX idx_adjustments_adjusted_at ON stock_adjustments(adjusted_at);
```

---

## Frontend Structure

### Directory Organization

```
frontend-app/src/features/reconciliation/
├── pages/
│   ├── GenerateStockCountWorksheetPage.tsx
│   ├── StockCountEntryPage.tsx
│   ├── StockCountReviewPage.tsx
│   ├── StockCountCompletionPage.tsx
│   ├── VarianceInvestigationPage.tsx
│   ├── VarianceInvestigationDashboard.tsx
│   ├── D365ReconciliationDashboard.tsx
│   └── StockCountListPage.tsx
├── components/
│   ├── WorksheetGenerationForm.tsx
│   ├── LocationSelector.tsx
│   ├── ProductFilter.tsx
│   ├── BarcodeScanner.tsx
│   ├── StockCountEntryForm.tsx
│   ├── VarianceSummaryTable.tsx
│   ├── VarianceCard.tsx
│   ├── InvestigationForm.tsx
│   ├── ApprovalDialog.tsx
│   ├── ReconciliationStatusCard.tsx
│   └── StockCountProgressBar.tsx
├── hooks/
│   ├── useGenerateStockCountWorksheet.ts
│   ├── useRecordStockCountEntry.ts
│   ├── useCompleteStockCount.ts
│   ├── useInvestigateVariance.ts
│   ├── useResolveVariance.ts
│   ├── useApproveVariance.ts
│   ├── useStockCounts.ts (list query)
│   ├── useStockCount.ts (single query)
│   ├── useVariances.ts (list query)
│   ├── useVariance.ts (single query)
│   ├── useRetryD365Reconciliation.ts
│   └── useOfflineSync.ts
├── services/
│   ├── stockCountService.ts
│   ├── varianceService.ts
│   ├── d365ReconciliationService.ts
│   └── offlineStorageService.ts
├── types/
│   ├── stockCount.ts
│   ├── variance.ts
│   ├── reconciliation.ts
│   └── offline.ts
└── utils/
    ├── varianceCalculations.ts
    ├── severityClassification.ts
    ├── offlineSyncUtils.ts
    └── barcodeValidator.ts
```

---

## Configuration Requirements

### Application Configuration

#### services/reconciliation-service/reconciliation-container/src/main/resources/application.yml

```yaml
server:
  port: 8086

spring:
  application:
    name: reconciliation-service
  datasource:
    url: jdbc:postgresql://${POSTGRES_HOST:localhost}:${POSTGRES_PORT:5432}/${POSTGRES_DB:wms_db}
    username: ${POSTGRES_USER:wms_user}
    password: ${POSTGRES_PASSWORD:wms_password}
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        default_schema: public
  flyway:
    enabled: true
    baseline-on-migrate: true
    locations: classpath:db/migration

reconciliation:
  variance-thresholds:
    low-percentage: 5.0  # 0-5%
    medium-percentage: 10.0  # 5-10%
    high-percentage: 20.0  # 10-20%
    critical-percentage: 20.0  # >20%
    low-value: 100.00  # Under $100
    medium-value: 500.00  # $100-$500
    high-value: 1000.00  # $500-$1000
    critical-value: 1000.00  # Over $1000
  approval-required:
    high-severity: true
    critical-severity: true
  offline-sync:
    batch-size: 50
    max-age-hours: 24

kafka:
  bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
  topics:
    stock-count-initiated: reconciliation.stock-count.initiated
    stock-count-entry-recorded: reconciliation.stock-count.entry-recorded
    stock-count-completed: reconciliation.stock-count.completed
    variance-identified: reconciliation.variance.identified
    variance-investigated: reconciliation.variance.investigated
    variance-resolved: reconciliation.variance.resolved
    stock-count-cancelled: reconciliation.stock-count.cancelled
  consumer:
    group-id: reconciliation-service
    auto-offset-reset: earliest
  producer:
    acks: all
    retries: 3
```

#### services/integration-service/integration-container/src/main/resources/application.yml

```yaml
d365:
  auth:
    tenant-id: ${D365_TENANT_ID}
    client-id: ${D365_CLIENT_ID}
    client-secret: ${D365_CLIENT_SECRET}
    resource: ${D365_RESOURCE}
    authority-url: https://login.microsoftonline.com/${D365_TENANT_ID}/oauth2/v2.0/token
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
    async: true
    batch-size: 20

kafka:
  topics:
    reconciliation-initiated: d365.reconciliation.initiated
    reconciliation-completed: d365.reconciliation.completed
    reconciliation-failed: d365.reconciliation.failed
```

### Environment Variables

```bash
# Reconciliation Service
export POSTGRES_HOST=localhost
export POSTGRES_PORT=5432
export POSTGRES_DB=wms_db
export POSTGRES_USER=wms_user
export POSTGRES_PASSWORD=wms_password
export KAFKA_BOOTSTRAP_SERVERS=localhost:9092

# D365 Integration (Optional - only if reconciliation enabled)
export D365_RECONCILIATION_ENABLED=true
export D365_TENANT_ID=your-tenant-id
export D365_CLIENT_ID=your-client-id
export D365_CLIENT_SECRET=your-client-secret
export D365_RESOURCE=https://your-org.crm.dynamics.com
export D365_API_BASE_URL=https://your-org.api.crm.dynamics.com
```

---

## Testing Strategy

### Unit Testing

**Domain Core:**

- Test aggregate behavior (StockCount.initiate(), StockCount.recordEntry(), StockCount.complete())
- Test variance calculation logic (quantity and percentage calculations)
- Test severity classification (LOW, MEDIUM, HIGH, CRITICAL based on thresholds)
- Test business rules (no duplicate entries, variance approval requirements)
- Test event publishing (correct events published on state changes)
- Pure Java testing (JUnit 5, AssertJ)

**Application Service:**

- Test command handlers (GenerateStockCountWorksheetCommandHandler, RecordStockCountEntryCommandHandler, CompleteStockCountCommandHandler)
- Test query handlers (GetStockCountQueryHandler, ListStockCountsQueryHandler, ListVariancesQueryHandler)
- Mock port dependencies (StockManagementServicePort, LocationManagementServicePort)
- Verify event publishing
- Test validation logic

**Infrastructure:**

- Test repository adapters with test data
- Test event publishers and listeners
- Test REST controllers
- Integration tests with Testcontainers

**Target Coverage:** >90%

---

### Integration Testing

**Gateway API Tests:**

- Test stock count worksheet generation endpoint
- Test stock count entry recording endpoint
- Test stock count completion with variance calculation
- Test variance investigation and resolution endpoints
- Test D365 reconciliation endpoints (if enabled)
- Validate end-to-end flows
- Test error scenarios and validation

**Event Flow Tests:**

- Test event publishing and consumption across services
- Test event correlation with correlation IDs
- Test idempotency of event handlers
- Test error handling and retry mechanisms

**Target Coverage:** >85%

---

### Frontend Testing

**Component Tests:**

- Test worksheet generation form
- Test stock count entry form
- Test barcode scanner integration
- Test variance display components
- Test investigation form
- Test approval dialogs

**Hook Tests:**

- Test useGenerateStockCountWorksheet
- Test useRecordStockCountEntry with online/offline scenarios
- Test useCompleteStockCount
- Test useInvestigateVariance
- Test useResolveVariance
- Mock API responses

**E2E Tests:**

- Test complete stock count workflow (generate → entry → complete → investigate)
- Test offline sync functionality
- Test D365 reconciliation workflow
- Test variance approval workflow

**Target Coverage:** >80%

---

## Implementation Order

### Phase 1: Foundation and Common Objects (Days 1-2)

**Rationale:** Establish foundational domain objects and infrastructure

1. Create common domain value objects:
   - `StockCountId` (common-domain)
   - `StockCountVarianceId` (common-domain)
   - `VarianceReason` enum (common-domain)
   - `StockCountStatus` enum (common-domain)
   - `InvestigationStatus` enum (common-domain)
   
2. Set up Reconciliation Service domain core:
   - `StockCount` aggregate
   - `StockCountEntry` entity
   - `StockCountVariance` aggregate
   - Domain events
   
3. Create database migrations:
   - stock_counts table
   - stock_count_entries table
   - stock_count_variances table
   - variance_investigation_history table
   
4. Implement base repository interfaces and adapters

---

### Phase 2: Generate Stock Count Worksheet (Days 3-4)

**Rationale:** Core functionality for initiating stock counts

**Backend:**

1. Implement GenerateStockCountWorksheetCommand and handler (US-8.1.1)
2. Implement StockCount.initiate() domain logic
3. Integrate with Stock Management Service for system quantities
4. Integrate with Location Management Service for location details
5. Implement event publishing (StockCountInitiatedEvent)
6. Write unit tests

**Frontend:**

1. Create GenerateStockCountWorksheetPage
2. Implement WorksheetGenerationForm component
3. Create LocationSelector and ProductFilter components
4. Implement useGenerateStockCountWorksheet hook
5. Implement stockCountService.generateWorksheet()
6. Write component tests

**Gateway API Tests:**

1. Test worksheet generation with various filters
2. Test validation scenarios
3. Test event publishing

---

### Phase 3: Perform Stock Count Entry (Days 5-6)

**Rationale:** Mobile-first entry with offline support

**Backend:**

1. Implement RecordStockCountEntryCommand and handler (US-8.1.2)
2. Implement StockCount.recordEntry() domain logic
3. Add duplicate entry validation
4. Implement event publishing (StockCountEntryRecordedEvent)
5. Write unit tests

**Frontend:**

1. Create StockCountEntryPage (PWA)
2. Implement BarcodeScanner component
3. Implement StockCountEntryForm
4. Implement useRecordStockCountEntry hook with offline support
5. Implement offlineStorageService using IndexedDB
6. Implement background sync for offline entries
7. Write component and hook tests

**Gateway API Tests:**

1. Test entry recording with valid data
2. Test duplicate entry prevention
3. Test barcode validation
4. Test offline sync scenarios

---

### Phase 4: Complete Stock Count (Days 7-8)

**Rationale:** Variance calculation and completion workflow

**Backend:**

1. Implement CompleteStockCountCommand and handler (US-8.1.3)
2. Implement variance calculation service (domain service)
3. Implement severity classification logic
4. Implement StockCount.complete() domain logic
5. Implement event publishing (StockCountCompletedEvent, StockCountVarianceIdentifiedEvent)
6. Write unit tests for variance calculation

**Frontend:**

1. Create StockCountCompletionPage
2. Implement VarianceSummaryTable component
3. Implement VarianceCard with color coding
4. Implement useCompleteStockCount hook
5. Create variance visualization utilities
6. Write component tests

**Gateway API Tests:**

1. Test stock count completion
2. Test variance calculation accuracy
3. Test severity classification
4. Test event publishing for variances

---

### Phase 5: Investigate Stock Count Variances (Day 9)

**Rationale:** Variance resolution workflow with approval gates

**Backend:**

1. Implement InvestigateVarianceCommand and handler (US-8.2.1)
2. Implement ResolveVarianceCommand and handler
3. Implement ApproveVarianceCommand and handler (for HIGH/CRITICAL)
4. Integrate with Stock Management Service for movement history
5. Implement event publishing (VarianceInvestigatedEvent, VarianceResolvedEvent)
6. Write unit tests

**Frontend:**

1. Create VarianceInvestigationPage
2. Implement InvestigationForm component
3. Implement ApprovalDialog component
4. Implement useInvestigateVariance, useResolveVariance, useApproveVariance hooks
5. Implement varianceService
6. Write component tests

**Gateway API Tests:**

1. Test variance investigation
2. Test variance resolution
3. Test approval workflow for HIGH/CRITICAL
4. Test investigation history tracking

---

### Phase 6: D365 Reconciliation (Optional - Day 10)

**Rationale:** Optional integration with D365 for stock count journals

**Backend (Integration Service):**

1. Implement StockCountReconciliationEventListener (US-8.3.1)
2. Create D365StockCountReconciliationService
3. Implement D365 counting journal creation via OData API
4. Implement retry mechanism with exponential backoff
5. Create database tables for reconciliation tracking
6. Implement event publishing (ReconciliationCompletedEvent, ReconciliationFailedEvent)
7. Write integration tests

**Frontend:**

1. Create D365ReconciliationDashboard
2. Implement ReconciliationStatusCard component
3. Implement useRetryD365Reconciliation hook
4. Implement d365ReconciliationService
5. Write component tests

**Gateway API Tests:**

1. Test D365 reconciliation success flow
2. Test retry mechanism
3. Test failed reconciliation handling
4. Test audit trail creation

---

### Phase 7: Testing and Validation (Day 11-12)

**Rationale:** Comprehensive testing and bug fixes

1. Complete all gateway API tests
2. End-to-end integration testing
3. Offline sync testing with various scenarios
4. Performance testing (variance calculation for large datasets)
5. Security testing (authorization for variance approvals)
6. Bug fixes and refinements
7. Documentation updates

---

## Dependencies and Prerequisites

### Prerequisites from Previous Sprints

**Sprint 1-4:**

- ✅ Stock Management Service operational with stock level queries
- ✅ Location Management Service operational with location queries
- ✅ Product Service operational with product information
- ✅ Tenant Service with multi-tenant schema support

**Sprint 6:**

- ✅ Stock item tracking with quantities and locations

### External Dependencies

- **Stock Management Service:** System quantity queries, stock adjustment updates
- **Location Management Service:** Location details, barcode validation
- **Product Service:** Product information, barcode validation
- **Notification Service:** Alerts for critical variances and approvals

### Technical Dependencies

- **Kafka:** Event streaming for reconciliation events
- **PostgreSQL:** Database storage with multi-tenant schemas
- **Spring Boot:** Application framework
- **React:** Frontend framework with PWA capabilities
- **IndexedDB:** Offline storage for mobile entry
- **D365 (Optional):** ERP integration for stock count reconciliation

### Common Value Objects Needed

Add to `common-domain` module:

- `StockCountId` - UUID-based identifier for stock counts
- `StockCountVarianceId` - UUID-based identifier for variances
- `VarianceReason` - Enum (DAMAGE, THEFT, COUNTING_ERROR, SYSTEM_ERROR, EXPIRY, OTHER)
- `StockCountStatus` - Enum (DRAFT, IN_PROGRESS, COMPLETED, CANCELLED)
- `InvestigationStatus` - Enum (PENDING, IN_PROGRESS, RESOLVED, ESCALATED)

### New Enums in Reconciliation Service Domain

- `CountType` - CYCLE_COUNT, FULL_INVENTORY, SPOT_CHECK
- `VarianceSeverity` - LOW, MEDIUM, HIGH, CRITICAL

---

## Risk Mitigation

### Risk 1: Offline Sync Conflicts

**Risk:** Multiple users counting same location/product offline, causing conflicts on sync

**Mitigation:**

- Implement conflict resolution with timestamp priority
- Display conflict resolution UI for user decision
- Alert supervisors of sync conflicts
- Maintain audit trail of conflict resolutions

---

### Risk 2: Large Stock Count Datasets

**Risk:** Performance degradation with thousands of entries

**Mitigation:**

- Implement pagination for entry lists
- Use lazy loading for variance displays
- Batch process variance calculations
- Index database tables appropriately
- Target: < 3 seconds for 1000 entries (P95)

---

### Risk 3: D365 Integration Failures

**Risk:** Stock count reconciliation fails to reach D365

**Mitigation:**

- Implement retry logic with exponential backoff (max 5 attempts)
- Use dead letter queue for persistent failures
- Provide manual retry option in dashboard
- Alert on critical reconciliation failures
- Maintain complete audit trail of all sync attempts
- Support manual reconciliation submission

---

### Risk 4: Variance Calculation Accuracy

**Risk:** Incorrect variance calculations lead to stock inaccuracies

**Mitigation:**

- Comprehensive unit tests with known datasets
- Validation against manual calculations
- Peer review of variance logic
- Audit trail for all calculations
- Support for variance recalculation if errors found

---

### Risk 5: Incomplete Stock Counts

**Risk:** Operators abandon counts before completion

**Mitigation:**

- Auto-save progress with every entry
- Resume capability for in-progress counts
- Validation before allowing completion
- Alert supervisors of stale counts (>24 hours)
- Support for count cancellation with reason

---

### Risk 6: Unauthorized Variance Approvals

**Risk:** High-value variances approved without proper authorization

**Mitigation:**

- Role-based access control (RBAC) for approvals
- Manager approval required for HIGH/CRITICAL variances
- Audit trail for all approvals
- Alert on approval anomalies
- Support for approval revocation

---

## Definition of Done

### Code Complete

- ✅ All 5 user stories implemented with acceptance criteria validated
- ✅ Code follows Clean Hexagonal Architecture
- ✅ Domain logic in domain-core (pure Java, no framework dependencies)
- ✅ CQRS separation implemented (separate command and query handlers)
- ✅ Events published for all state changes
- ✅ Multi-tenant support with schema isolation

### Testing Complete

- ✅ Unit tests pass (>90% coverage for domain core)
- ✅ Integration tests pass (>85% coverage)
- ✅ Gateway API tests pass (100% endpoint coverage)
- ✅ Frontend E2E tests pass
- ✅ Offline sync tests pass
- ✅ Performance tests meet targets

### Documentation Complete

- ✅ API documentation updated (OpenAPI/Swagger)
- ✅ Implementation plans documented
- ✅ Architecture decisions recorded (ADRs)
- ✅ User guides updated
- ✅ Database schema documented

### Acceptance Criteria Met

- ✅ All acceptance criteria validated with gateway API tests
- ✅ Product Owner approval obtained
- ✅ Demo to stakeholders completed
- ✅ User Acceptance Testing (UAT) passed

### Production Ready

- ✅ Database migrations tested and reviewed
- ✅ Event schemas validated
- ✅ Error handling comprehensive
- ✅ Logging and monitoring configured
- ✅ Security review completed
- ✅ Performance benchmarks met

---

## Sprint Success Metrics

### Velocity Metrics

- **Planned Story Points:** 34
- **Sprint Duration:** 2 weeks (10 working days)
- **Team Capacity:** 6 developers × 10 days = 60 dev-days

### Quality Metrics

- **Test Coverage:** Target >90% for domain core, >85% for application service
- **Code Review:** All code reviewed before merge
- **Defect Rate:** <5% post-deployment
- **Technical Debt:** Zero critical technical debt introduced

### Business Metrics

- **Stock Count Creation Time:** Target <2 seconds (P95)
- **Entry Recording Time:** Target <500ms per entry (P95)
- **Variance Calculation Time:** Target <3 seconds for 1000 entries (P95)
- **D365 Sync Latency:** Target <10 seconds (P95)
- **Offline Sync Time:** Target <5 seconds for 100 entries (P95)
- **List Query Performance:** Target <1 second with filters (P95)

### Acceptance Criteria Summary

| User Story | Total ACs | Status          |
|------------|-----------|-----------------|
| US-8.1.1   | 7         | ✅ Planned       |
| US-8.1.2   | 7         | ✅ Planned       |
| US-8.1.3   | 7         | ✅ Planned       |
| US-8.2.1   | 7         | ✅ Planned       |
| US-8.3.1   | 6         | ✅ Planned       |

**Total:** 34 acceptance criteria across 5 user stories

---

## Appendix

### Related Documentation

- [User Story Breakdown](../../../06-project-management/User_Story_Breakdown.md)
- [Service Architecture Document](../../../01-architecture/Service_Architecture_Document.md)
- [Domain Model Design](../../../01-architecture/Domain_Model_Design.md)
- [Mandated Implementation Template Guide](../../../guide/mandated-Implementation-template-guide.md)
- [Sprint 7 Implementation Plans](../sprint-07/) - Reference for structure and patterns

### Glossary

| Term                          | Definition                                                                      |
|-------------------------------|---------------------------------------------------------------------------------|
| **Stock Count**               | Physical counting of inventory to verify system quantities                      |
| **Cycle Count**               | Regular counting of specific locations/products on a rotating schedule          |
| **Full Inventory**            | Complete physical count of all warehouse stock                                  |
| **Variance**                  | Difference between counted quantity and system quantity                         |
| **Variance Severity**         | Classification of variance impact (LOW, MEDIUM, HIGH, CRITICAL)                 |
| **Investigation**             | Process of researching and resolving stock count variances                      |
| **Stock Count Worksheet**     | Electronic form listing products and locations to be counted                    |
| **FEFO**                      | First Expiring First Out - inventory rotation strategy                          |
| **D365 Counting Journal**     | Microsoft Dynamics 365 record for stock count adjustments                       |
| **Offline Sync**              | Background synchronization of data collected while device was offline           |
| **IndexedDB**                 | Browser-based database for offline data storage                                 |
| **Barcode Scanner**           | Device or software for reading product and location barcodes                    |
| **Approval Gate**             | Required authorization for high-impact actions (HIGH/CRITICAL variance approval) |

### Domain Events Summary

| Event                              | Publisher                | Consumers                                    | Purpose                                 |
|------------------------------------|--------------------------|----------------------------------------------|-----------------------------------------|
| `StockCountInitiatedEvent`         | Reconciliation Service   | Notification Service, Location Mgmt Service  | Signals stock count started             |
| `StockCountEntryRecordedEvent`     | Reconciliation Service   | (Internal to service)                        | Tracks progress                         |
| `StockCountCompletedEvent`         | Reconciliation Service   | Stock Mgmt Service, Integration Service      | Triggers stock adjustments and D365 sync |
| `StockCountVarianceIdentifiedEvent`| Reconciliation Service   | Notification Service                         | Alerts on significant variances         |
| `VarianceInvestigatedEvent`        | Reconciliation Service   | Notification Service                         | Tracks investigation progress           |
| `VarianceResolvedEvent`            | Reconciliation Service   | Stock Mgmt Service, Notification Service     | Finalizes stock adjustments             |
| `ReconciliationInitiatedEvent`     | Integration Service      | Reconciliation Service                       | D365 sync started                       |
| `ReconciliationCompletedEvent`     | Integration Service      | Reconciliation Service, Notification Service | D365 sync successful                    |
| `ReconciliationFailedEvent`        | Integration Service      | Reconciliation Service, Notification Service | D365 sync failed, manual intervention   |

### Common Module Updates

**New Files to Create:**

```
common/common-domain/src/main/java/com/ccbsa/common/domain/valueobject/
├── StockCountId.java
├── StockCountVarianceId.java
├── VarianceReason.java
├── StockCountStatus.java
└── InvestigationStatus.java
```

**Example: StockCountId.java**

```java
package com.ccbsa.common.domain.valueobject;

import java.util.UUID;

public class StockCountId extends BaseId<UUID> {
    
    public StockCountId(UUID value) {
        super(value);
    }
    
    public static StockCountId of(UUID value) {
        return new StockCountId(value);
    }
}
```

---

**Document Control**

- **Version:** 1.0
- **Date:** 2026-01-10
- **Author:** System Architect
- **Status:** Ready for Implementation
- **Review Cycle:** Sprint planning and retrospective
- **Last Updated:** 2026-01-10

---

**Sprint 8 Status:** ✅ Ready for Implementation
**Next Review:** Sprint Planning Meeting
**Estimated Delivery:** 2 weeks from sprint start
