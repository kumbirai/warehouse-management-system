# Service Architecture Document

## Warehouse Management System Integration - CCBSA LDP System

**Document Version:** 2.0  
**Date:** 2025-01  
**Status:** Approved  
**Related Documents:**

- [Business Requirements Document](../00-business-requiremants/business-requirements-document.md)
- [Project Roadmap](../project-management/project-roadmap.md)
- [Mandated Implementation Template Guide](../../guide/mandated-Implementation-template-guide.md)

---

## Table of Contents

1. [System Overview](#system-overview)
2. [Architecture Principles](#architecture-principles)
3. [Service Decomposition and Boundaries](#service-decomposition-and-boundaries)
4. [Domain Model and Bounded Contexts](#domain-model-and-bounded-contexts)
5. [Integration Architecture](#integration-architecture)
6. [Event-Driven Architecture Design](#event-driven-architecture-design)
7. [CQRS Implementation Strategy](#cqrs-implementation-strategy)
8. [Technology Stack](#technology-stack)
9. [Deployment Architecture](#deployment-architecture)
10. [Multi-Tenant Data Isolation](#multi-tenant-data-isolation)
11. [Common Modules](#common-modules)

---

## System Overview

### Context

The Warehouse Management System Integration serves as a bridge between Microsoft Dynamics 365 Finance and Operations (D365) and Local Distribution Partner (LDP) warehouse
operations. The system enables LDPs to manage consigned stock, optimize warehouse operations, maintain accurate inventory levels, and ensure seamless reconciliation with D365.

### System Boundaries

**In Scope:**

- Stock consignment management and confirmation
- Stock classification by expiration dates (FEFO compliance)
- Warehouse location and movement tracking
- Product master data synchronization
- Stock level monitoring and restock request generation
- Picking list management and optimization
- Returns management (partial, full, damage-in-transit)
- Daily stock counting and reconciliation
- Bidirectional integration with D365

**Out of Scope (MVP):**

- Advanced route planning and optimization
- Predictive analytics and ML-based optimization
- Native mobile applications (PWA only)
- Multi-warehouse coordination features
- Third-party logistics integration beyond D365

### High-Level Architecture

The system follows a **microservices architecture** with **Clean Hexagonal Architecture** principles. Each service is independently deployable and maintains its own database.
Services communicate through **event-driven choreography** using Apache Kafka, ensuring loose coupling and high scalability.

```
┌─────────────────────────────────────────────────────────────────┐
│                        Frontend (PWA)                           │
│              Progressive Web App with Offline Support           │
└───────────────────────────┬─────────────────────────────────────┘
                            │
                            │ REST API
                            │
┌───────────────────────────┴─────────────────────────────────────┐
│                    API Gateway (Spring Cloud Gateway)            │
└───────────┬───────────┬───────────┬───────────┬─────────────────┘
            │           │           │           │
    ┌───────┴───┐ ┌─────┴───┐ ┌────┴───┐ ┌────┴───┐
    │  Stock   │ │Location │ │ Picking│ │Returns │
    │Management│ │Management│ │ Service│ │ Service│
    └─────┬────┘ └────┬────┘ └────┬───┘ └────┬───┘
          │           │           │           │
          └───────────┴───────────┴───────────┘
                    │           │
            ┌───────┴───────────┴───────┐
            │   Apache Kafka (Events)   │
            └───────┬───────────┬───────┘
                    │           │
            ┌───────┴───────────┴───────┐
            │   Integration Service     │
            │    (D365 Adapter)         │
            └───────────┬───────────────┘
                        │
                        │ OData/REST
                        │
            ┌───────────┴───────────────┐
            │  Dynamics 365 F&O         │
            └───────────────────────────┘
```

---

## Architecture Principles

This architecture is built on the following core principles:

1. **Clean Hexagonal Architecture** - Clear separation of concerns with domain at the center
2. **Domain-Driven Design (DDD)** - Business logic drives the architecture and design decisions
3. **CQRS (Command Query Responsibility Segregation)** - Separate read and write operations for scalability and clarity
4. **Event-Driven Design** - Loose coupling through domain events and asynchronous communication
5. **Multi-Tenant Data Isolation** - Flexible tenant isolation strategies based on requirements
6. **Consistent Patterns** - Standardized patterns across all services for maintainability
7. **Port/Adapter Pattern** - Clear interfaces between layers promoting testability
8. **Dependency Inversion** - High-level modules depend on abstractions, not concrete implementations
9. **Pure Java Domain Core** - Domain-core modules contain only pure Java with no framework dependencies

### Service Module Structure

Every service follows this consistent module structure that enforces Clean Hexagonal Architecture principles:

```
{service}-service/
├── {service}-application/          # REST API layer (CQRS controllers)
├── {service}-container/            # Application bootstrap and configuration
├── {service}-dataaccess/           # Database access layer (adapters)
├── {service}-domain/               # Domain layer (aggregate root)
│   ├── {service}-application-service/  # Application services (use cases)
│   └── {service}-domain-core/          # Core domain entities and business logic
├── {service}-messaging/            # Event messaging layer (adapters)
└── pom.xml                         # Service parent POM
```

---

## Service Decomposition and Boundaries

### Service Identification Strategy

Services are decomposed based on **Domain-Driven Design bounded contexts**, ensuring each service owns a cohesive business capability with clear boundaries. Services are identified
using the following criteria:

1. **Business Capability Alignment** - Each service represents a distinct business capability
2. **Data Ownership** - Each service owns its data and is the single source of truth
3. **Independent Deployability** - Services can be deployed independently
4. **Team Ownership** - Services can be owned by different teams
5. **Scalability Requirements** - Services can scale independently based on load

### Core Services

#### 1. Stock Management Service

**Purpose:** Manages stock consignment, classification, expiration tracking, and stock level monitoring.

**Responsibilities:**

- Ingest and validate incoming stock consignment from D365
- Classify stock by expiration dates (Near Expiry, Normal, Extended Shelf Life)
- Track expiration dates and generate alerts
- Monitor stock levels against minimum/maximum thresholds
- Generate restock requests when stock falls below minimum
- Maintain stock availability for picking operations

**Key Aggregates:**

- `StockConsignment` - Represents incoming stock consignment
- `StockItem` - Represents individual stock items with expiration dates
- `StockLevel` - Represents stock levels per product/location

**Database:** `stock_management_db`

**Events Published:**

- `StockConsignmentReceivedEvent`
- `StockConsignmentConfirmedEvent`
- `StockClassifiedEvent`
- `StockExpiringAlertEvent`
- `StockLevelBelowMinimumEvent`
- `RestockRequestGeneratedEvent`

**Events Consumed:**

- `StockMovementCompletedEvent` (from Location Management Service)
- `StockPickedEvent` (from Picking Service)
- `StockReturnedEvent` (from Returns Service)
- `StockCountReconciledEvent` (from Reconciliation Service)

---

#### 2. Location Management Service

**Purpose:** Manages warehouse locations, barcodes, stock movements, and location capacity.

**Responsibilities:**

- Manage warehouse location master data
- Assign locations to stock based on FEFO principles and capacity
- Track all stock movements on warehouse floor
- Manage location status (occupied, available, reserved, blocked)
- Enforce location capacity limits
- Generate location barcodes

**Key Aggregates:**

- `Location` - Represents a warehouse location with barcode
- `StockMovement` - Represents movement of stock between locations
- `LocationCapacity` - Represents capacity constraints per location

**Database:** `location_management_db`

**Events Published:**

- `LocationAssignedEvent`
- `StockMovementInitiatedEvent`
- `StockMovementCompletedEvent`
- `LocationStatusChangedEvent`
- `LocationCapacityExceededEvent`

**Events Consumed:**

- `StockConsignmentReceivedEvent` (from Stock Management Service)
- `StockReturnedEvent` (from Returns Service)
- `PickingListCreatedEvent` (from Picking Service)

---

#### 3. Product Service

**Purpose:** Manages product master data and barcode validation.

**Responsibilities:**

- Synchronize product master data from D365
- Validate product barcodes
- Support multiple barcode formats (EAN-13, Code 128, etc.)
- Maintain product descriptions, units of measure
- Handle product master data changes

**Key Aggregates:**

- `Product` - Represents product master data
- `ProductBarcode` - Represents product barcode information

**Database:** `product_db`

**Events Published:**

- `ProductCreatedEvent`
- `ProductUpdatedEvent`
- `ProductBarcodeUpdatedEvent`

**Events Consumed:**

- None (reference data service, other services query directly)

**Note:** Product Service is primarily a **reference data service** that other services query synchronously for validation purposes.

---

#### 4. Picking Service

**Purpose:** Manages picking lists, load planning, order-to-load mapping, and picking execution.

**Responsibilities:**

- Ingest picking lists from D365
- Plan loads (multiple orders per load, multiple orders per customer)
- Optimize picking locations based on FEFO principles
- Generate picking sequences/routes
- Guide picking operations
- Validate picked quantities
- Support partial picking scenarios

**Key Aggregates:**

- `PickingList` - Represents a picking list from D365
- `Load` - Represents a load containing multiple orders
- `Order` - Represents an order within a load
- `PickingTask` - Represents a picking task for a specific location/product
- `PickingExecution` - Represents the execution of picking operations

**Database:** `picking_db`

**Events Published:**

- `PickingListReceivedEvent`
- `LoadPlannedEvent`
- `PickingTaskCreatedEvent`
- `PickingTaskCompletedEvent`
- `PickingCompletedEvent`
- `PartialPickingCompletedEvent`

**Events Consumed:**

- `StockConsignmentConfirmedEvent` (from Stock Management Service)
- `LocationAssignedEvent` (from Location Management Service)
- `StockMovementCompletedEvent` (from Location Management Service)

---

#### 5. Returns Service

**Purpose:** Manages returns processing including partial acceptance, full returns, and damage-in-transit.

**Responsibilities:**

- Handle partial order acceptance
- Process full order returns
- Handle damage-in-transit returns
- Classify returned stock condition
- Assign return locations for re-picking
- Prioritize returned stock in picking if appropriate
- Maintain return history and audit trail

**Key Aggregates:**

- `Return` - Represents a return transaction
- `ReturnItem` - Represents individual items being returned
- `ReturnReason` - Represents reason codes for returns
- `DamageAssessment` - Represents damage classification

**Database:** `returns_db`

**Events Published:**

- `ReturnInitiatedEvent`
- `ReturnProcessedEvent`
- `ReturnLocationAssignedEvent`
- `DamageRecordedEvent`
- `ReturnReconciledEvent`

**Events Consumed:**

- `PickingCompletedEvent` (from Picking Service)
- `LocationAssignedEvent` (from Location Management Service)

---

#### 6. Reconciliation Service

**Purpose:** Manages daily stock counts, variance identification, and reconciliation with D365.

**Responsibilities:**

- Generate electronic stock count worksheets
- Support cycle counting and full physical inventory counts
- Track stock count progress
- Identify variances between counted and system stock
- Support variance investigation workflow
- Reconcile stock counts with D365
- Update D365 with reconciliation results

**Key Aggregates:**

- `StockCount` - Represents a stock count session
- `StockCountWorksheet` - Represents electronic worksheet for counting
- `StockCountEntry` - Represents individual count entries (location/product/quantity)
- `StockCountVariance` - Represents variance between counted and system stock
- `Reconciliation` - Represents reconciliation transaction with D365

**Database:** `reconciliation_db`

**Events Published:**

- `StockCountInitiatedEvent`
- `StockCountCompletedEvent`
- `StockCountVarianceIdentifiedEvent`
- `ReconciliationInitiatedEvent`
- `ReconciliationCompletedEvent`
- `D365ReconciliationUpdateSentEvent`

**Events Consumed:**

- `StockConsignmentConfirmedEvent` (from Stock Management Service)
- `StockMovementCompletedEvent` (from Location Management Service)
- `StockPickedEvent` (from Picking Service)
- `StockReturnedEvent` (from Returns Service)

---

#### 7. Integration Service (D365 Adapter)

**Purpose:** Provides bidirectional integration adapter for D365 Finance and Operations.

**Responsibilities:**

- Receive consignment data from D365
- Receive picking lists from D365
- Receive product master data from D365
- Send consignment confirmations to D365
- Send stock movement updates to D365
- Send returns data to D365
- Send reconciliation data to D365
- Send restock requests to D365
- Handle D365 API authentication (OAuth 2.0 / Azure AD)
- Implement retry logic with exponential backoff
- Maintain integration error queue

**Key Aggregates:**

- `IntegrationMessage` - Represents a message to/from D365
- `IntegrationStatus` - Represents status of integration operations
- `IntegrationError` - Represents integration errors requiring manual resolution

**Database:** `integration_db`

**Events Published:**

- `ConsignmentDataReceivedEvent`
- `PickingListReceivedEvent`
- `ProductMasterDataReceivedEvent`
- `D365IntegrationErrorEvent`

**Events Consumed:**

- `StockConsignmentConfirmedEvent` (from Stock Management Service)
- `StockMovementCompletedEvent` (from Location Management Service)
- `ReturnReconciledEvent` (from Returns Service)
- `ReconciliationCompletedEvent` (from Reconciliation Service)
- `RestockRequestGeneratedEvent` (from Stock Management Service)

**Note:** Integration Service acts as an **adapter** that translates between D365 APIs and internal domain events.

---

#### 8. Tenant Service

**Purpose:** Manages tenant (LDP) lifecycle, configuration, and metadata.

**Responsibilities:**

- Manage tenant lifecycle (create, activate, deactivate, suspend, delete)
- Store tenant metadata (name, contact information, address)
- Manage tenant configuration (settings, preferences, limits)
- Validate tenant existence and status
- Trigger tenant schema creation (for schema-per-tenant isolation)
- Integrate with Keycloak for tenant realm/group management

**Key Aggregates:**

- `Tenant` - Represents a tenant (LDP) with lifecycle and configuration

**Database:** `tenant_db`

**Events Published:**

- `TenantCreatedEvent`
- `TenantActivatedEvent`
- `TenantDeactivatedEvent`
- `TenantSuspendedEvent`
- `TenantConfigurationUpdatedEvent`
- `TenantSchemaCreatedEvent`

**Events Consumed:**

- None initially (tenant service is source of truth for tenants)

**Note:** Tenant Service is a **reference data service** that other services query synchronously for tenant validation. It manages the tenant entity lifecycle and is the single
source of truth for tenant existence and status.

---

#### 9. User Service

**Purpose:** Manages user accounts, profiles, and IAM integration.

**Responsibilities:**

- User management operations
- Keycloak integration
- User profile management
- Tenant-user relationship management
- Realm determination for user creation

**Key Aggregates:**

- `User` - Represents a user account

**Database:** `user_db`

**Events Published:**

- `UserCreatedEvent`
- `UserUpdatedEvent`
- `UserDeactivatedEvent`

**Events Consumed:**

- `TenantActivatedEvent` (from Tenant Service) - To validate tenant before user creation

**Keycloak Integration:**

- Uses `KeycloakUserPort` from `common-keycloak` for user operations
- Uses `TenantServicePort` to determine correct Keycloak realm
- Realm determination strategy:
    1. Query Tenant Service for tenant-specific realm name
    2. Fall back to default realm (`wms-realm`) if tenant doesn't specify one
    3. Always validates tenant is ACTIVE before user creation
    4. Always sets `tenant_id` attribute on users for multi-tenancy enforcement

**Note:** User Service manages user accounts and integrates with Keycloak. It validates tenant existence and status via Tenant Service before creating users. The realm name is
determined dynamically based on tenant configuration.

---

### Service Communication Patterns

#### Synchronous Communication

- **REST API** - Used for frontend-to-service communication and service-to-service queries
- **Product Service Queries** - Other services query Product Service synchronously for product validation
- **Tenant Service Queries** - Other services query Tenant Service synchronously for tenant validation

#### Asynchronous Communication

- **Event-Driven Choreography** - Services communicate through domain events via Apache Kafka
- **Domain Events** - Aggregates publish domain events for service coordination
- **Eventual Consistency** - Services achieve consistency through event propagation

**Note:** This system uses **Event-Driven Choreography**, not Event Sourcing. Events are published to Kafka for service coordination but are NOT stored for aggregate
reconstruction. Database audit logs are used for compliance requirements. Event Sourcing should only be considered if aggregate reconstruction from events is explicitly required.

### Communication Pattern Decision Matrix

#### When to Use Synchronous Communication

**Use Synchronous (REST API) For:**

- **Reference Data Queries** - Product Service and Tenant Service queries for validation
- **Immediate Consistency Required** - Operations requiring immediate response
- **Low Latency Requirements** - Operations where latency is critical
- **Simple Request-Response** - Operations that don't require coordination

**Examples:**

- Product barcode validation
- Tenant existence validation
- Location availability check (for immediate decisions)
- User authentication/authorization

**Resilience Patterns for Synchronous Calls:**

- **Circuit Breaker** - Prevent cascading failures
- **Timeout Configuration** - Fail fast with appropriate timeouts
- **Retry with Exponential Backoff** - For transient failures
- **Fallback Strategies** - Default values or cached data

#### When to Use Asynchronous Communication

**Use Asynchronous (Events) For:**

- **Business Operations** - Operations that modify state
- **Cross-Service Coordination** - Operations spanning multiple services
- **High Volume Operations** - Operations with high throughput requirements
- **Eventual Consistency Acceptable** - Operations where immediate consistency not required

**Examples:**

- Stock consignment processing
- Stock movement tracking
- Picking list processing
- Reconciliation updates

**Event-Driven Benefits:**

- Loose coupling between services
- Independent scalability
- Resilience to service failures
- Natural event sourcing for audit trails

#### Hybrid Patterns

**Synchronous Query + Asynchronous Update:**

- Query Product Service synchronously for validation
- Publish event asynchronously for state changes
- Example: Validate product exists (sync), then publish stock received event (async)

**Synchronous Command + Asynchronous Notification:**

- Process command synchronously (immediate response)
- Publish events asynchronously for downstream processing
- Example: Confirm consignment (sync response), publish event for location assignment (async)

---

## Domain Model and Bounded Contexts

### Bounded Context Mapping

```
┌─────────────────────────────────────────────────────────────────┐
│                    Warehouse Management System                   │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────┐ │
│  │ Stock Management│  │ Location         │  │ Product      │ │
│  │ Context         │  │ Management       │  │ Context      │ │
│  │                 │  │ Context          │  │              │ │
│  │ - Consignment   │  │ - Locations      │  │ - Products   │ │
│  │ - Classification│  │ - Movements      │  │ - Barcodes   │ │
│  │ - Expiration    │  │ - Capacity       │  │              │ │
│  │ - Stock Levels  │  │                  │  │              │ │
│  └────────┬────────┘  └────────┬─────────┘  └──────┬───────┘ │
│           │                    │                    │          │
│           └────────────────────┴────────────────────┘          │
│                          │                                    │
│  ┌───────────────────────┴──────────────────────────────────┐  │
│  │              Order Fulfillment Context                   │  │
│  │                                                          │  │
│  │  ┌──────────────┐              ┌──────────────┐        │  │
│  │  │ Picking      │              │ Returns      │        │  │
│  │  │ Context      │              │ Context      │        │  │
│  │  │              │              │              │        │  │
│  │  │ - Picking    │              │ - Returns    │        │  │
│  │  │   Lists      │              │ - Damage     │        │  │
│  │  │ - Loads      │              │ - Re-picking │        │  │
│  │  │ - Orders     │              │              │        │  │
│  │  └──────────────┘              └──────────────┘        │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                 │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │         Inventory Control Context                       │  │
│  │                                                          │  │
│  │  - Stock Counts                                         │  │
│  │  - Variance Identification                              │  │
│  │  - Reconciliation                                       │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                 │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │         Integration Context (D365 Adapter)               │  │
│  │                                                          │  │
│  │  - D365 API Integration                                 │  │
│  │  - Message Translation                                  │  │
│  │  - Error Handling                                       │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                 │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │         Tenant Management Context                        │  │
│  │                                                          │  │
│  │  - Tenant Lifecycle                                     │  │
│  │  - Tenant Configuration                                 │  │
│  │  - Tenant Metadata                                      │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                 │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │         User Management Context                          │  │
│  │                                                          │  │
│  │  - User Accounts                                        │  │
│  │  - User Profiles                                        │  │
│  │  - IAM Integration                                      │  │
│  └──────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

### Domain Model Details

#### Stock Management Context

**Aggregate Roots:**

- `StockConsignment` - Manages incoming stock consignment lifecycle
- `StockItem` - Manages individual stock items with expiration dates
- `StockLevel` - Manages stock levels per product/location

**Value Objects:**

- `ExpirationDate` - Expiration date with classification logic
- `StockClassification` - Enum: NEAR_EXPIRY, NORMAL, EXTENDED_SHELF_LIFE
- `Quantity` - Stock quantity with validation
- `ConsignmentReference` - Unique consignment reference from D365

**Domain Events:**

- `StockConsignmentReceivedEvent`
- `StockConsignmentConfirmedEvent`
- `StockClassifiedEvent`
- `StockExpiringAlertEvent`
- `StockLevelBelowMinimumEvent`
- `RestockRequestGeneratedEvent`

**Business Rules:**

- Stock must be classified within 30 days of expiration as "Near Expiry"
- Stock within 7 days of expiration must generate alert
- Expired stock cannot be picked
- Stock levels cannot exceed maximum threshold
- Restock requests generated when stock falls below minimum

---

#### Location Management Context

**Aggregate Roots:**

- `Location` - Manages warehouse location with barcode
- `StockMovement` - Manages stock movement transactions

**Value Objects:**

- `LocationBarcode` - Barcode identifier following CCBSA standards
- `LocationStatus` - Enum: OCCUPIED, AVAILABLE, RESERVED, BLOCKED
- `LocationCoordinates` - Physical coordinates in warehouse
- `MovementReason` - Reason for stock movement

**Domain Events:**

- `LocationAssignedEvent`
- `StockMovementInitiatedEvent`
- `StockMovementCompletedEvent`
- `LocationStatusChangedEvent`
- `LocationCapacityExceededEvent`

**Business Rules:**

- All locations must have unique barcodes
- Location capacity cannot be exceeded
- Stock movements must record timestamp, user, source, destination, quantity, reason
- FEFO principles guide location assignment (earlier expiration dates closer to picking zones)

---

#### Product Context

**Aggregate Roots:**

- `Product` - Manages product master data

**Value Objects:**

- `ProductCode` - Unique product identifier
- `Barcode` - Product barcode (supports multiple formats)
- `UnitOfMeasure` - Product unit of measure
- `ProductDescription` - Product description

**Domain Events:**

- `ProductCreatedEvent`
- `ProductUpdatedEvent`
- `ProductBarcodeUpdatedEvent`

**Business Rules:**

- Product codes must be unique
- Products must have at least one barcode
- Product master data synchronized daily from D365

---

#### Order Fulfillment Context

**Picking Sub-Context:**

**Aggregate Roots:**

- `PickingList` - Manages picking list from D365
- `Load` - Manages load containing multiple orders
- `Order` - Manages order within a load
- `PickingTask` - Manages individual picking tasks
- `PickingExecution` - Manages picking execution

**Value Objects:**

- `LoadNumber` - Unique load identifier
- `OrderNumber` - Unique order identifier
- `CustomerInfo` - Customer information
- `PickingSequence` - Optimized picking sequence
- `PickingStatus` - Enum: PENDING, IN_PROGRESS, COMPLETED, PARTIAL

**Domain Events:**

- `PickingListReceivedEvent`
- `LoadPlannedEvent`
- `PickingTaskCreatedEvent`
- `PickingTaskCompletedEvent`
- `PickingCompletedEvent`
- `PartialPickingCompletedEvent`

**Business Rules:**

- Loads can contain multiple orders
- Customers can have multiple orders per load
- Picking must prioritize FEFO stock
- Picking locations optimized based on expiration dates and proximity

**Returns Sub-Context:**

**Aggregate Roots:**

- `Return` - Manages return transactions
- `ReturnItem` - Manages individual return items

**Value Objects:**

- `ReturnReason` - Reason code for return
- `ReturnCondition` - Condition of returned stock
- `DamageType` - Type of damage (if applicable)
- `ReturnStatus` - Enum: INITIATED, PROCESSED, LOCATION_ASSIGNED, RECONCILED

**Domain Events:**

- `ReturnInitiatedEvent`
- `ReturnProcessedEvent`
- `ReturnLocationAssignedEvent`
- `DamageRecordedEvent`
- `ReturnReconciledEvent`

**Business Rules:**

- Partial order acceptance must identify returned quantities
- Returned stock must be assigned location based on condition and expiration date
- Damage-in-transit must be classified (repairable, write-off)
- Returned stock available for re-picking

---

#### Inventory Control Context

**Aggregate Roots:**

- `StockCount` - Manages stock count sessions
- `StockCountWorksheet` - Manages electronic worksheets
- `StockCountEntry` - Manages individual count entries
- `StockCountVariance` - Manages variances
- `Reconciliation` - Manages reconciliation transactions

**Value Objects:**

- `CountQuantity` - Counted quantity
- `SystemQuantity` - System quantity
- `VarianceAmount` - Variance amount
- `VarianceReason` - Reason for variance
- `ReconciliationStatus` - Enum: PENDING, APPROVED, SENT_TO_D365, CONFIRMED

**Domain Events:**

- `StockCountInitiatedEvent`
- `StockCountCompletedEvent`
- `StockCountVarianceIdentifiedEvent`
- `ReconciliationInitiatedEvent`
- `ReconciliationCompletedEvent`
- `D365ReconciliationUpdateSentEvent`

**Business Rules:**

- Daily stock counts required
- Electronic worksheets eliminate paper transcription
- Variances must be flagged for review
- Reconciliation must update D365 within 1 hour
- Stock counts support cycle counting and full physical inventory

---

#### Tenant Management Context

**Aggregate Roots:**

- `Tenant` - Manages tenant lifecycle and configuration

**Value Objects:**

- `TenantId` - Unique tenant identifier (from common-domain)
- `TenantName` - Tenant name with validation
- `TenantStatus` - Enum: PENDING, ACTIVE, INACTIVE, SUSPENDED
- `ContactInformation` - Contact details (emailAddress, phone, address)
- `TenantConfiguration` - Tenant-specific settings and preferences

**Domain Events:**

- `TenantCreatedEvent`
- `TenantActivatedEvent`
- `TenantDeactivatedEvent`
- `TenantSuspendedEvent`
- `TenantConfigurationUpdatedEvent`
- `TenantSchemaCreatedEvent`

**Business Rules:**

- Tenant ID must be unique
- Tenant name is required
- Tenant status transitions must be valid (PENDING → ACTIVE → INACTIVE/SUSPENDED)
- Cannot delete active tenant (must deactivate first)
- Tenant schema must be created during activation (for schema-per-tenant)
- All tenant-aware aggregates must reference valid tenant

---

### Aggregate Consistency Boundaries

#### Transaction Boundaries

**One Transaction Per Aggregate:**

- Each aggregate modification occurs within a single transaction
- Transaction boundary = aggregate boundary
- Ensures consistency within aggregate
- Prevents distributed transactions

**Cross-Aggregate Consistency:**

- Achieved through eventual consistency via domain events
- Aggregates publish events when state changes
- Other aggregates react to events asynchronously
- No distributed transactions across aggregates

#### Consistency Rules

**Within Aggregate (Immediate Consistency):**

- All invariants enforced within aggregate boundary
- State changes are atomic
- Business rules validated before state change
- Domain events published after successful state change

**Across Aggregates (Eventual Consistency):**

- Consistency achieved through event propagation
- Events published after aggregate transaction commits
- Eventual consistency acceptable for cross-aggregate operations
- Compensating events used for rollback if needed

#### Domain Services vs Application Services

**Domain Services:**

- Pure domain logic spanning multiple aggregates within same bounded context
- No infrastructure dependencies
- Located in `{service}-domain-core/service`
- Used for complex business logic that doesn't fit in a single aggregate

**Application Services:**

- Orchestrate use cases across aggregates or bounded contexts
- Handle infrastructure concerns (transactions, event publishing)
- Located in `{service}-application-service/command` or `query`
- Coordinate domain logic and infrastructure

**Example - Domain Service:**

```java
// Domain service for logic spanning aggregates within context
@Component
public class StockAssignmentDomainService {
    public void assignStockToLocation(StockItem stock, Location location) {
        // Business logic spanning two aggregates
        stock.assignToLocation(location.getId());
        location.reserveCapacity(stock.getQuantity());
    }
}
```

**Example - Application Service:**

```java
// Application service for cross-context orchestration
@Component
public class ConfirmConsignmentCommandHandler {
    @Transactional
    public ConfirmConsignmentResult handle(ConfirmConsignmentCommand command) {
        // Orchestrate domain logic, handle transactions, publish events
        consignment.confirm();
        repository.save(consignment);
        eventPublisher.publish(consignment.getDomainEvents());
        return result;
    }
}
```

#### Handling Cross-Aggregate Invariants

**Pattern 1: Domain Service (Within Bounded Context)**

- Use domain service when invariants span aggregates in same context
- Domain service coordinates aggregate modifications
- Single transaction if aggregates in same database

**Pattern 2: Event-Driven (Across Bounded Contexts)**

- Use events when invariants span bounded contexts
- Publish event from source aggregate
- Target aggregate reacts to event asynchronously
- Accept eventual consistency

**Pattern 3: Saga Pattern (NOT USED)**

- This architecture does NOT use SAGA pattern
- Event-driven choreography preferred for loose coupling
- Compensating events used for error handling

### Context Mapping

**Customer-Supplier Relationships:**

- **Product Service → All Services** - Product Service provides reference data (Customer-Supplier)
- **Tenant Service → All Services** - Tenant Service provides tenant validation and metadata (Customer-Supplier)
- **Integration Service → All Services** - Integration Service provides D365 data (Customer-Supplier)

**Partnership Relationships:**

- **Stock Management ↔ Location Management** - Close collaboration on stock placement
- **Picking Service ↔ Location Management** - Close collaboration on picking optimization
- **Returns Service ↔ Location Management** - Close collaboration on return placement

**Shared Kernel:**

- **Common Domain Events** - Shared event definitions in `common-messaging`
- **Common Value Objects** - Shared value objects in `common-domain`

**Conformist Relationships:**

- **Integration Service → D365** - Must conform to D365 API contracts

---

## Integration Architecture

### D365 Integration Overview

The Integration Service acts as a **bidirectional adapter** between the internal event-driven architecture and D365 Finance and Operations. It translates between D365 APIs and
internal domain events.

### Integration Patterns

#### 1. Inbound Integration (D365 → System)

**Pattern:** Event-Driven with Polling Fallback

**Consignment Data:**

- D365 pushes consignment notifications via webhook (preferred) or Integration Service polls D365 API
- Integration Service receives consignment data
- Publishes `ConsignmentDataReceivedEvent` to Kafka
- Stock Management Service consumes event and processes consignment

**Picking Lists:**

- D365 pushes picking list notifications via webhook (preferred) or Integration Service polls D365 API
- Integration Service receives picking list data
- Publishes `PickingListReceivedEvent` to Kafka
- Picking Service consumes event and processes picking list

**Product Master Data:**

- Scheduled daily synchronization (pull from D365)
- Integration Service fetches product master data
- Publishes `ProductMasterDataReceivedEvent` to Kafka
- Product Service consumes event and updates product master data

**Stock Level Thresholds:**

- Scheduled daily synchronization (pull from D365)
- Integration Service fetches stock level thresholds
- Publishes `StockLevelThresholdsReceivedEvent` to Kafka
- Stock Management Service consumes event and updates thresholds

#### 2. Outbound Integration (System → D365)

**Pattern:** Event-Driven with Retry Logic

**Consignment Confirmations:**

- Stock Management Service publishes `StockConsignmentConfirmedEvent`
- Integration Service consumes event
- Transforms to D365 API format
- Sends confirmation to D365 via OData API
- Handles retries with exponential backoff

**Stock Movements:**

- Location Management Service publishes `StockMovementCompletedEvent`
- Integration Service consumes event
- Transforms to D365 API format
- Sends stock movement update to D365
- Handles retries with exponential backoff

**Returns Data:**

- Returns Service publishes `ReturnReconciledEvent`
- Integration Service consumes event
- Transforms to D365 API format
- Sends returns data to D365
- Handles retries with exponential backoff

**Reconciliation Data:**

- Reconciliation Service publishes `ReconciliationCompletedEvent`
- Integration Service consumes event
- Transforms to D365 API format
- Sends reconciliation data to D365
- Handles retries with exponential backoff

**Restock Requests:**

- Stock Management Service publishes `RestockRequestGeneratedEvent`
- Integration Service consumes event
- Transforms to D365 API format
- Sends restock request to D365
- Handles retries with exponential backoff

### Integration Technology

**API Protocol:**

- OData 4.0 (preferred) for D365 standard APIs
- RESTful APIs as fallback

**Authentication:**

- OAuth 2.0 / Azure AD authentication
- Service principal authentication for service-to-service
- Token refresh handling

**Data Format:**

- JSON for request/response payloads
- OData query syntax for filtering and pagination

**Error Handling:**

- Standard HTTP status codes
- Retry logic with exponential backoff (max 3 retries)
- Dead letter queue for persistent failures
- Integration error logging and alerting

### Integration Service Architecture

```
Integration Service
├── D365 Adapter
│   ├── D365ConsignmentAdapter      # Handles consignment data
│   ├── D365PickingListAdapter      # Handles picking lists
│   ├── D365ProductAdapter          # Handles product master data
│   └── D365ReconciliationAdapter   # Handles reconciliation data
├── Event Consumers
│   ├── ConsignmentConfirmationConsumer
│   ├── StockMovementConsumer
│   ├── ReturnsConsumer
│   ├── ReconciliationConsumer
│   └── RestockRequestConsumer
├── Event Publishers
│   ├── ConsignmentDataPublisher
│   ├── PickingListPublisher
│   └── ProductMasterDataPublisher
└── Error Handler
    ├── RetryManager
    └── DeadLetterQueueManager
```

### Integration Data Flow

```
D365 → Integration Service → Kafka Event → Domain Service
Domain Service → Kafka Event → Integration Service → D365
```

### Integration Error Handling

**Retry Strategy:**

- Transient errors: Exponential backoff (1s, 2s, 4s, 8s)
- Maximum retries: 3 attempts
- After max retries: Move to dead letter queue

**Dead Letter Queue:**

- Persistent storage of failed integration messages
- Manual review and resolution
- Retry capability after manual intervention
- Alerting for dead letter queue items

**Error Monitoring:**

- Integration error logs
- Metrics for success/failure rates
- Alerting for persistent failures
- Dashboard for integration health

---

## Event-Driven Architecture Design

### Event-Driven Choreography Pattern

The system uses **event-driven choreography** (not SAGA) for service coordination. Each service publishes domain events when business operations complete, and other services react
to these events independently.

### Event Flow Principles

1. **Decoupled Services** - Services do not know about each other directly
2. **Domain Events** - Aggregates publish domain events when state changes occur
3. **Eventual Consistency** - Services achieve consistency through event propagation
4. **Idempotency** - Event handlers must be idempotent to handle duplicate events
5. **Event Ordering** - Events within an aggregate partition are processed in order

**Important:** This architecture uses **Event-Driven Choreography**, not Event Sourcing. Events are:

- Published to Kafka for service coordination
- Consumed by other services for eventual consistency
- NOT stored for aggregate reconstruction (current state is stored in database)
- Used for audit trails via database audit logs, not event logs

### Event Categories

#### Domain Events

- Represent business events that have occurred
- Published by domain aggregates
- Consumed by other services for coordination
- Examples: `StockConsignmentReceivedEvent`, `StockMovementCompletedEvent`

#### Integration Events

- Represent integration events with external systems
- Published by Integration Service
- Examples: `D365IntegrationErrorEvent`, `D365ReconciliationUpdateSentEvent`

### Event Schema

All events follow a consistent schema:

```json
{
  "eventId": "uuid",
  "eventType": "EventClassName",
  "aggregateId": "aggregate-identifier",
  "aggregateType": "AggregateClassName",
  "tenantId": "tenant-identifier",
  "timestamp": "ISO-8601-timestamp",
  "version": "event-version",
  "payload": {
    // Event-specific data
  },
  "metadata": {
    "correlationId": "uuid",
    "causationId": "uuid",
    "userId": "user-identifier"
  }
}
```

### Event Correlation and Causation Tracking

#### Correlation ID

**Purpose:** Tracks entire business flow across services and operations.

**Usage:**

- Set at entry point (API Gateway or first service handling request)
- Propagated through all events in the business flow
- Used for distributed tracing and debugging
- Enables tracking complete request lifecycle

**Example Flow:**

```
API Request → CorrelationId: "req-123"
  → StockConsignmentReceivedEvent (correlationId: "req-123")
    → LocationAssignedEvent (correlationId: "req-123")
      → StockConsignmentConfirmedEvent (correlationId: "req-123")
```

#### Causation ID

**Purpose:** Tracks immediate cause of event (parent event).

**Usage:**

- Set to the `eventId` of the event that caused this event
- Enables event chain tracking
- Helps understand event dependencies
- Useful for debugging event flows

**Example Flow:**

```
StockConsignmentReceivedEvent (eventId: "evt-001")
  → LocationAssignedEvent (causationId: "evt-001", eventId: "evt-002")
    → StockConsignmentConfirmedEvent (causationId: "evt-002", eventId: "evt-003")
```

#### Correlation Tracking Implementation

**At API Gateway:**

- Generate correlationId for each incoming request
- Add to request headers
- Include in all downstream service calls

**In Event Publishers:**

- Extract correlationId from current context (ThreadLocal or request context)
- Include in event metadata
- Propagate to all published events

**In Event Consumers:**

- Extract correlationId from consumed event
- Set in current context for downstream operations
- Include in any events published as result of consumption

**Distributed Tracing:**

- Use correlationId for distributed tracing (e.g., OpenTelemetry)
- Enable correlation across service boundaries
- Support debugging complex event flows
- Track performance across services

#### Correlation Context Propagation

**Thread-Local Context:**

- Store correlationId in ThreadLocal for request processing
- Automatically included in all events published during request
- Cleared after request completion

**Event Metadata:**

- CorrelationId always included in event metadata
- CausationId set to previous event's eventId
- Enables complete event chain reconstruction

**Example Implementation Pattern:**

```java
// Set correlation context at entry point
CorrelationContext.setCorrelationId("req-123");

// Event publisher automatically includes correlationId
eventPublisher.publish(event); // Includes correlationId from context

// Event consumer extracts and propagates
@KafkaListener
public void handle(StockConsignmentReceivedEvent event) {
    CorrelationContext.setCorrelationId(event.getMetadata().getCorrelationId());
    // Process event and publish new events (with same correlationId)
}
```

### Key Event Flows

#### 1. Stock Consignment Flow

```
D365 → Integration Service
  → ConsignmentDataReceivedEvent
    → Stock Management Service
      → StockConsignmentReceivedEvent
        → Location Management Service (for location assignment)
      → StockConsignmentConfirmedEvent
        → Integration Service
          → D365 (confirmation)
```

#### 2. Picking Flow

```
D365 → Integration Service
  → PickingListReceivedEvent
    → Picking Service
      → LoadPlannedEvent
        → Location Management Service (for location queries)
      → PickingTaskCreatedEvent
        → Picking Service (internal)
      → PickingTaskCompletedEvent
        → Location Management Service
          → StockMovementCompletedEvent
            → Stock Management Service (update stock levels)
            → Integration Service (update D365)
      → PickingCompletedEvent
        → Returns Service (for potential returns)
```

#### 3. Returns Flow

```
Picking Service → PickingCompletedEvent
  → Returns Service
    → ReturnInitiatedEvent
      → Returns Service (internal processing)
    → ReturnProcessedEvent
      → Location Management Service
        → LocationAssignedEvent
          → Returns Service
            → ReturnLocationAssignedEvent
    → ReturnReconciledEvent
      → Integration Service
        → D365 (returns data)
      → Stock Management Service (update stock levels)
```

#### 4. Reconciliation Flow

```
Reconciliation Service → StockCountInitiatedEvent
  → Reconciliation Service (internal counting)
    → StockCountCompletedEvent
      → Reconciliation Service (variance calculation)
        → StockCountVarianceIdentifiedEvent
          → Reconciliation Service (reconciliation)
            → ReconciliationCompletedEvent
              → Integration Service
                → D365 (reconciliation data)
              → Stock Management Service (update stock levels)
```

#### 5. Stock Level Monitoring Flow

```
Stock Management Service → StockLevelBelowMinimumEvent
  → Stock Management Service (generate restock request)
    → RestockRequestGeneratedEvent
      → Integration Service
        → D365 (restock request)
```

### Event Store

**Apache Kafka** serves as the event store with the following topics:

- `stock-management-events`
- `location-management-events`
- `product-events`
- `picking-events`
- `returns-events`
- `reconciliation-events`
- `integration-events`

### Event Processing Guarantees

- **At-Least-Once Delivery** - Events are delivered at least once (idempotency required)
- **Ordering** - Events within a partition are processed in order
- **Durability** - Events are persisted to Kafka before acknowledgment

### Event Versioning Strategy

Events are versioned to support schema evolution while maintaining backward compatibility.

#### Versioning Approach

**Explicit Version Field:**

- Each event class includes explicit `eventVersion` field
- Version number increments for breaking changes
- Version number stays same for backward-compatible changes

**Backward Compatibility Rules:**

- New fields added as optional (nullable)
- Existing fields never removed (deprecated instead)
- Field types never changed (new fields added with new names)
- Consumers handle missing fields gracefully

#### Version Migration Process

**Phase 1: Add New Version**

- Create new event version with additional fields
- New fields are optional/nullable
- Both versions published during transition

**Phase 2: Consumer Migration**

- Consumers updated to handle both versions
- New consumers use latest version
- Existing consumers continue working

**Phase 3: Deprecation**

- Old version marked as deprecated
- Deprecation period: 6 months minimum
- Documentation updated with migration guide

**Phase 4: Removal**

- Old version removed after deprecation period
- All consumers must be migrated
- Breaking change requires coordination

#### Schema Registry Integration

**Confluent Schema Registry (Optional but Recommended):**

- Centralized schema management
- Schema evolution validation
- Compatibility modes: BACKWARD, FORWARD, FULL
- Schema versioning and migration tracking

**Compatibility Modes:**

- **BACKWARD** - New schema can read old data (recommended)
- **FORWARD** - Old schema can read new data
- **FULL** - Both backward and forward compatible

#### Event Versioning Example

```java
// Version 1.0 - Initial event
public class StockConsignmentReceivedEvent extends StockManagementEvent<StockConsignment> {
    private final int eventVersion = 1;
    private final ConsignmentReference consignmentReference;
    private final WarehouseId warehouseId;
    // ... other fields
}

// Version 2.0 - Added optional field (backward compatible)
public class StockConsignmentReceivedEvent extends StockManagementEvent<StockConsignment> {
    private final int eventVersion = 2;
    private final ConsignmentReference consignmentReference;
    private final WarehouseId warehouseId;
    private final String priority; // NEW: Optional field, nullable for v1 compatibility
    // ... other fields
}
```

#### Consumer Version Handling

**Consumer Implementation:**

- Check event version before processing
- Handle multiple versions during migration
- Default values for missing fields
- Log warnings for deprecated versions

**Example:**

```java
@KafkaListener(topics = "stock-management-events")
public void handle(StockConsignmentReceivedEvent event) {
    if (event.getEventVersion() == 1) {
        // Handle v1 format
        processV1(event);
    } else if (event.getEventVersion() >= 2) {
        // Handle v2+ format
        processV2(event);
    }
}
```

#### Version Compatibility Matrix

| Event Version | Consumer Version | Compatibility                   |
|---------------|------------------|---------------------------------|
| v1            | v1               | ✅ Compatible                    |
| v1            | v2               | ✅ Compatible (v2 handles v1)    |
| v2            | v1               | ⚠️ Partial (missing new fields) |
| v2            | v2               | ✅ Compatible                    |

**Migration Strategy:** Always update consumers before removing old event versions.

---

## CQRS Implementation Strategy

### CQRS Overview

Each service implements **CQRS (Command Query Responsibility Segregation)** to separate read and write operations, enabling independent optimization and scaling.

### Command Side (Write Model)

**Purpose:** Handle business operations that modify state.

**Components:**

- **Command** - Represents an intent to perform an operation
- **Command Handler** - Processes commands and executes business logic
- **Aggregate Root** - Maintains consistency boundaries
- **Domain Events** - Published after successful command execution
- **Write Database** - Optimized for writes and consistency

**Example Commands:**

- `ReceiveStockConsignmentCommand`
- `AssignLocationCommand`
- `CompleteStockMovementCommand`
- `CreatePickingTaskCommand`
- `ProcessReturnCommand`
- `InitiateStockCountCommand`

### Query Side (Read Model)

**Purpose:** Handle queries optimized for read performance.

**Components:**

- **Query** - Represents a read request
- **Query Handler** - Processes queries and returns data
- **Read Model** - Denormalized views optimized for queries
- **Data Port** - Interface for read model data access (`port/data`)
- **Read Database** - Optimized for reads and performance
- **Projection** - Builds read models from domain events

**Important:** Query handlers use **Data Ports** (`port/data`) to access read models, NOT Repository Ports. Repository Ports are for aggregate persistence (write model).

**Example Queries:**

- `GetStockLevelsQuery`
- `GetLocationAvailabilityQuery`
- `GetPickingTasksQuery`
- `GetStockCountWorksheetQuery`
- `GetReturnHistoryQuery`

### CQRS Implementation Pattern

```
┌─────────────────────────────────────────────────────────┐
│                    Command Flow                         │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  REST API → Command Controller                          │
│    ↓                                                     │
│  Command Handler                                         │
│    ↓                                                     │
│  Aggregate Root (Domain Logic)                           │
│    ↓                                                     │
│  Domain Event Published                                  │
│    ↓                                                     │
│  Write Database Updated                                  │
│    ↓                                                     │
│  Event Projection → Read Model Updated                  │
│                                                         │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│                     Query Flow                          │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  REST API → Query Controller                            │
│    ↓                                                     │
│  Query Handler                                           │
│    ↓                                                     │
│  Read Model (Denormalized)                               │
│    ↓                                                     │
│  Query Result Returned                                   │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

### Read Model Projections

Read models are built from domain events using **event projections**:

**Stock Management Service:**

- `StockLevelView` - Current stock levels per product/location
- `StockConsignmentView` - Consignment history
- `ExpiringStockView` - Stock expiring within thresholds

**Location Management Service:**

- `LocationAvailabilityView` - Available locations with capacity
- `StockMovementHistoryView` - Movement history
- `LocationStatusView` - Current location status

**Picking Service:**

- `PickingTaskView` - Current picking tasks
- `LoadStatusView` - Load and order status
- `PickingProgressView` - Picking progress tracking

**Returns Service:**

- `ReturnHistoryView` - Return history
- `ReturnStatusView` - Current return status

**Reconciliation Service:**

- `StockCountWorksheetView` - Stock count worksheets
- `VarianceReportView` - Variance reports
- `ReconciliationStatusView` - Reconciliation status

### Read Model Projection Strategy

Read models are built from domain events using **event projections**. Projections update read models asynchronously when domain events are published.

#### Projection Update Patterns

**Asynchronous Projection Updates (Recommended):**

- Event listeners consume domain events from Kafka
- Projections update read models based on event data
- Updates occur asynchronously after command completion
- Suitable for most read models with eventual consistency acceptable

**Synchronous Projection Updates (For Critical Read Models):**

- Projections updated within the same transaction as write model
- Immediate consistency guaranteed
- Use only when eventual consistency is unacceptable
- Increases transaction duration and complexity

#### Projection Implementation Pattern

Each service implements projections as Kafka event listeners:

1. **Event Listener** - Consumes domain events from Kafka topics
2. **Projection Handler** - Updates read model based on event type
3. **Idempotency Check** - Ensures events are processed only once
4. **Error Handling** - Failed projections move to dead letter queue

#### Projection Update Flow

```
Command Handler → Aggregate Modified → Domain Event Published → Kafka
                                                                   ↓
Projection Listener ← Kafka Event ← Event Published
       ↓
Idempotency Check
       ↓
Update Read Model
       ↓
Query Handler → Read Model → Query Result
```

#### Projection Failure Handling

- **Dead Letter Queue** - Failed projection updates are moved to DLQ
- **Retry Strategy** - Automatic retry with exponential backoff
- **Manual Recovery** - Failed projections can be manually retried
- **Rebuild Capability** - Projections can be rebuilt from event history (if available)

#### Projection Rebuild Strategy

When projections need to be rebuilt:

1. **Event History** - Replay events from Kafka (if retention allows) or database audit logs
2. **Snapshot Strategy** - Use current write model state as starting point
3. **Incremental Rebuild** - Process events from last known position
4. **Validation** - Compare rebuilt projection with current state

### Eventual Consistency

Read models are eventually consistent with write models:

- **Projection Lag** - Small delay (milliseconds to seconds) between write and read model update
- **Acceptable for Queries** - Most queries can tolerate eventual consistency
- **Critical Queries** - For critical queries requiring immediate consistency, query write model directly
- **Consistency Guarantees** - Define acceptable lag per read model (e.g., < 1 second for stock levels)

### CQRS Benefits

1. **Independent Scaling** - Read and write models scale independently
2. **Optimization** - Read models optimized for specific queries
3. **Performance** - Denormalized read models provide fast queries
4. **Flexibility** - Multiple read models for different query patterns

---

## Technology Stack

### Core Technologies

**Programming Language:**

- **Java 21** - LTS version with modern language features
- **Maven** - Build and dependency management

**Application Framework:**

- **Spring Boot 3.x** - Application framework
- **Spring Cloud** - Microservices framework
- **Spring Cloud Gateway** - API Gateway and routing
- **Spring Cloud Netflix Eureka** - Service discovery and registration
- **Spring Cloud LoadBalancer** - Client-side load balancing
- **Spring Data JPA** - Data access layer
- **Spring Security** - Security framework
- **Spring Cloud Stream** - Event processing

### Data & Persistence

**Database:**

- **PostgreSQL 15+** - Primary database (production and testing)
- **Flyway** - Database migration tool

**Data Access:**

- **Spring Data JPA** - JPA-based data access
- **Hibernate** - JPA implementation
- **QueryDSL** - Type-safe query building

### Messaging & Events

**Event Streaming:**

- **Apache Kafka 3.0+** - Event streaming platform
- **Spring Kafka** - Kafka integration (primary)
- **Spring Cloud Stream** - Kafka integration (optional)
- **Confluent Schema Registry** - Schema management (optional)

**Event Processing:**

- **Spring Kafka** - Event consumers and publishers (primary)
- **Spring Cloud Stream** - Event consumers and publishers (optional)
- **Kafka Streams** - Stream processing (if needed)

**Kafka Configuration Standards:**

All microservices use standardized production-grade Kafka configuration provided by `common-messaging` module:

**Producer Configuration:**

- **Idempotence:** Enabled (prevents duplicate messages)
- **Acks:** `all` (waits for all replicas for durability)
- **Retries:** 3 (with exponential backoff)
- **Compression:** `snappy` (balance of speed and compression)
- **Batch Size:** 16KB (efficient batching)
- **Linger:** 10ms (allow batching)
- **Serialization:** JSON with Jackson ObjectMapper

**Consumer Configuration:**

- **Auto Commit:** Disabled (manual acknowledgment for reliability)
- **Auto Offset Reset:** `earliest` (don't lose messages)
- **Max Poll Records:** 500 (balance throughput and processing time)
- **Max Poll Interval:** 300s (prevent rebalancing issues)
- **Session Timeout:** 30s (detect failures quickly)
- **Deserialization:** JSON with Jackson ObjectMapper

**Listener Configuration:**

- **Ack Mode:** `manual_immediate` (ack after successful processing)
- **Type:** `batch` (process multiple records efficiently)
- **Concurrency:** 3 (parallel processing per partition)

**Error Handling:**

- **Dead Letter Queue:** Failed messages published to `{topic}.dlq`
- **Retry Mechanism:** Exponential backoff (1s, 2s, 4s, 8s, 10s max)
- **Max Retries:** 3 attempts before DLQ
- **Non-Retryable Exceptions:** Serialization errors skip retries

**Health Monitoring:**

- **Kafka Health Indicator:** Available via Spring Boot Actuator `/actuator/health`
- **Checks:** Broker connectivity, cluster information, producer/consumer health

**Configuration Location:**

- Common configuration: `common/common-messaging/src/main/java/com/ccbsa/common/messaging/config/`
- Service-specific overrides: `{service}-container/src/main/resources/application.yml`
- Kubernetes ConfigMap: `infrastructure/kubernetes/configmaps/kafka-config.yaml`

### Security & Monitoring

**Security:**

- **Spring Security** - Authentication and authorization
- **OAuth 2.0 / JWT** - Token-based authentication
- **Azure AD Integration** - Enterprise authentication

**Monitoring:**

- **Spring Boot Actuator** - Application monitoring
- **Micrometer** - Metrics collection
- **Prometheus** - Metrics storage (optional)
- **Grafana** - Metrics visualization (optional)

### API & Documentation

**API:**

- **Spring Web MVC** - REST API framework
- **OpenAPI 3.0** - API specification
- **SpringDoc OpenAPI** - OpenAPI integration
- **Swagger UI** - Interactive API documentation

### Frontend

**Progressive Web App:**

- **React / Vue.js / Angular** - Frontend framework (to be determined)
- **Service Workers** - Offline support
- **IndexedDB** - Local storage
- **Workbox** - Service worker management

### Testing

**Unit Testing:**

- **JUnit 5** - Unit testing framework
- **Mockito** - Mocking framework
- **AssertJ** - Fluent assertions

**Integration Testing:**

- **Spring Boot Test** - Integration testing
- **Testcontainers** - Docker-based testing
- **WireMock** - API mocking

**End-to-End Testing:**

- **Cypress / Playwright** - E2E testing (frontend)
- **REST Assured** - API testing

---

## Deployment Architecture

### Deployment Model

**Microservices Architecture:**

- Each service deployed independently
- Containerized using Docker
- Orchestrated using Kubernetes (production) or Docker Compose (development)

### Infrastructure Components

**Container Orchestration:**

- **Kubernetes** - Production orchestration
- **Docker Compose** - Local development

**Service Discovery:**

- **Spring Cloud Netflix Eureka** - Service discovery and registration (primary)
- **Eureka Server** - Centralized service registry (port 8761)
- **Spring Cloud LoadBalancer** - Client-side load balancing integrated with Eureka
- **Service Registration** - All microservices automatically register with Eureka on startup
- **Health Monitoring** - Eureka tracks service health and removes unhealthy instances
- **Dynamic Discovery** - Gateway discovers services dynamically from Eureka registry

**API Gateway:**

- **Spring Cloud Gateway** - API gateway and routing
- **Eureka Integration** - Gateway discovers services via Eureka
- **Discovery Locator** - Automatic route creation based on registered services
- **Load Balancing** - Request distribution using Spring Cloud LoadBalancer with Eureka
- **Service Routing** - Routes use `lb://service-name` format for Eureka-based discovery

**Message Broker:**

- **Apache Kafka** - Event streaming
- **Zookeeper** - Kafka coordination (if needed)

**Database:**

- **PostgreSQL** - Primary database
- **Database per Service** - Each service has its own database

**Service Discovery Infrastructure:**

- **Eureka Server** - Centralized service registry (Docker container or standalone)
- **Eureka Dashboard** - Web UI for viewing registered services (http://localhost:8761)
- **Service Registration** - Automatic registration on service startup
- **Heartbeat Mechanism** - Services send heartbeats every 30 seconds
- **Health Checks** - Eureka monitors service health and removes unhealthy instances

**Monitoring & Logging:**

- **ELK Stack** - Log aggregation (Elasticsearch, Logstash, Kibana)
- **Prometheus** - Metrics collection
- **Grafana** - Metrics visualization

### Deployment Topology

```
┌─────────────────────────────────────────────────────────┐
│                    Kubernetes Cluster                   │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  ┌─────────────────────────────────────────────────┐   │
│  │           API Gateway (Spring Cloud Gateway)    │   │
│  │         (Eureka Client - Service Discovery)    │   │
│  └─────────────────┬───────────────────────────────┘   │
│                    │                                     │
│  ┌─────────────────┴───────────────────────────────┐   │
│  │              Service Pods                       │   │
│  │  ┌──────────┐ ┌──────────┐ ┌──────────┐        │   │
│  │  │  Stock   │ │ Location │ │ Picking  │  ...   │   │
│  │  │Management│ │Management│ │ Service  │        │   │
│  │  │(Eureka   │ │(Eureka   │ │(Eureka   │        │   │
│  │  │ Client)  │ │ Client)  │ │ Client)  │        │   │
│  │  └──────────┘ └──────────┘ └──────────┘        │   │
│  └─────────────────────────────────────────────────┘   │
│                    │                                     │
│                    │ Eureka Registration                │
│                    │                                     │
│  ┌─────────────────▼───────────────────────────────┐   │
│  │         Eureka Server (Service Registry)        │   │
│  │         - Service Registration                  │   │
│  │         - Health Monitoring                     │   │
│  │         - Service Discovery                    │   │
│  └─────────────────────────────────────────────────┘   │
│                                                         │
│  ┌─────────────────────────────────────────────────┐   │
│  │         Kafka Cluster (StatefulSet)            │   │
│  └─────────────────────────────────────────────────┘   │
│                                                         │
│  ┌─────────────────────────────────────────────────┐   │
│  │      PostgreSQL (StatefulSet per Service)      │   │
│  └─────────────────────────────────────────────────┘   │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

### Scaling Strategy

**Horizontal Scaling:**

- Services scale independently based on load
- Stateless services scale horizontally
- Database scaling handled separately

**Auto-Scaling:**

- Kubernetes Horizontal Pod Autoscaler (HPA)
- Scale based on CPU, memory, or custom metrics

**Database Scaling:**

- Read replicas for read-heavy services
- Connection pooling
- Query optimization

### High Availability

**Service Availability:**

- Multiple replicas per service
- Health checks and liveness probes
- Automatic failover

**Database Availability:**

- PostgreSQL replication (primary-replica)
- Automated backups
- Point-in-time recovery

**Kafka Availability:**

- Kafka cluster with replication
- Topic replication factor: 3

---

## Multi-Tenant Data Isolation

### Tenant Model

Each **LDP (Local Distribution Partner)** is a tenant. The system supports multiple LDPs with data isolation.

### Isolation Strategies

**Database per Tenant (Future):**

- Each tenant has its own database
- Maximum isolation and security
- Higher operational complexity

**Schema per Tenant (Recommended for MVP):**

- Shared database with schema per tenant
- Good isolation with manageable complexity
- PostgreSQL schema support

**Row-Level Security (Alternative):**

- Shared database and schema
- Tenant ID in every table
- Row-level security policies
- Simpler operations, requires careful implementation

### Recommended Approach (MVP)

**Schema per Tenant:**

- Shared PostgreSQL database
- Schema per LDP tenant
- Tenant context resolved from authentication token
- Dynamic schema switching in application layer
- Event-driven schema creation via `TenantSchemaCreatedEvent`

### Schema Creation Pattern

**Event-Driven Schema Creation:**

All backend microservices (except `tenant-service`) consume `TenantSchemaCreatedEvent` to programmatically create tenant schemas and run Flyway migrations.

**Flow:**

1. **Tenant Creation:**
    - `tenant-service` creates new tenant
    - Publishes `TenantSchemaCreatedEvent` with schema name
    - Event published to Kafka topic `tenant-events`

2. **Service Schema Setup:**
    - Each service listens to `TenantSchemaCreatedEvent`
    - Service creates tenant schema in its database
    - Service runs Flyway migrations programmatically in tenant schema
    - All tables and indexes created in tenant schema

3. **Runtime Operation:**
    - `TenantContext` set from JWT token
    - `TenantSchemaResolver` resolves schema name from tenant context
    - `TenantAwarePhysicalNamingStrategy` replaces `tenant_schema` placeholder with actual schema
    - Hibernate queries use tenant-specific schema

**Implementation:**

- See [Schema-Per-Tenant Implementation Pattern](./Schema_Per_Tenant_Implementation_Pattern.md) for detailed implementation guide
- All services must implement `TenantSchemaCreatedEventListener`
- Flyway migrations run programmatically in tenant schema context

### Tenant Context Propagation

**Authentication:**

- JWT token contains tenant ID
- Tenant ID extracted from token
- Tenant context set in thread-local or request context

**Database Access:**

- Spring Data JPA with schema switching
- Tenant-aware repository implementations
- Schema resolution based on tenant context
- Uses `TenantAwarePhysicalNamingStrategy` for dynamic schema resolution at runtime
- Entities use placeholder schema `"tenant_schema"` which is resolved dynamically by Hibernate naming strategy
- See [Multi-Tenancy Enforcement Guide](../03-security/Multi_Tenancy_Enforcement_Guide.md) for implementation details

**Event Publishing:**

- Tenant ID included in all events
- Event consumers filter by tenant ID
- Kafka topics can be partitioned by tenant (if needed)

---

## Common Modules

### Common Modules Overview

Shared functionality is extracted into common modules to ensure consistency and reduce duplication.

### Common Domain Module (`common-domain`)

**Purpose:** Shared domain value objects and base classes.

**Contents:**

- Base domain event class: `DomainEvent<T>`
- Base aggregate root class: `AggregateRoot`
- Common value objects: `TenantId`, `UserId`, `Timestamp`
- Domain exceptions

**Usage:**

- All domain-core modules depend on `common-domain`
- No framework dependencies (pure Java)

### Common Messaging Module (`common-messaging`)

**Purpose:** Shared messaging infrastructure and event definitions.

**Contents:**

- Event publisher interface: `EventPublisher`
- Event consumer interface: `EventConsumer`
- Base event classes
- Kafka configuration
- Event serialization/deserialization

**Usage:**

- All services depend on `common-messaging`
- Services publish and consume events through common interfaces

### Common Application Module (`common-application`)

**Purpose:** Shared application layer utilities.

**Contents:**

- Base command/query classes
- Command/query handler interfaces
- Validation utilities
- Exception handlers
- **Standardized API Response classes:**
    - `ApiResponse<T>` - Standardized response wrapper for all REST API responses
    - `ApiError` - Standardized error response structure
    - `ApiMeta` - Metadata for responses (pagination, etc.)
    - `ApiResponseBuilder` - Utility class for building consistent responses

**Usage:**

- Application modules depend on `common-application`
- All REST controllers MUST use `ApiResponse<T>` wrapper for responses
- All exception handlers MUST use `ApiError` for error responses
- Use `ApiResponseBuilder` utility methods for consistent response creation

**API Response Standard:**
All backend services MUST use the standardized `ApiResponse<T>` format to ensure consistent frontend consumption.
See [API Specifications](../02-api/API_Specifications.md#standardized-response-format) for detailed documentation.

**Example Usage:**

```java
// Success response
return ApiResponseBuilder.ok(data);

// Created response
return ApiResponseBuilder.created(data);

// Error response
ApiError error = ApiError.builder("ERROR_CODE", "Error message").build();
return ApiResponseBuilder.error(HttpStatus.BAD_REQUEST, error);
```

### Common Infrastructure Module (`common-infrastructure`)

**Purpose:** Shared infrastructure utilities.

**Contents:**

- Database configuration
- Security configuration
- Monitoring configuration
- Health checks

**Usage:**

- Container modules depend on `common-infrastructure`

### Common Keycloak Module (`common-keycloak`)

**Purpose:** Shared Keycloak integration infrastructure.

**Contents:**

- Keycloak client configuration (`KeycloakConfig`)
- Keycloak Admin Client setup (`KeycloakClientAdapter`)
- Port interfaces for Keycloak operations:
    - `KeycloakClientPort` - Base Keycloak client access
    - `KeycloakRealmPort` - Realm operations (for tenant-service)
    - `KeycloakGroupPort` - Group operations (for tenant-service)
    - `KeycloakUserPort` - User operations (for user-service)
    - `TenantServicePort` - Tenant service queries (for user-service realm determination)
- Base Keycloak adapter implementation
- Common Keycloak utilities (error handling, retry logic)

**Usage:**

- User Service and Tenant Service depend on `common-keycloak`
- Services implement service-specific adapters for their required ports
- Follows Port/Adapter pattern to avoid DRY violations

**Realm Determination Strategy:**

- User Service queries Tenant Service via `TenantServicePort` to get realm name
- Supports both single-realm (default `wms-realm`) and per-tenant realm strategies
- Tenant Service manages realm creation/activation when tenants are activated
- Default realm configured in `keycloak.admin.default-realm` property

**Note:** See [Keycloak Integration DRY Strategy](Keycloak_Integration_DRY_Strategy.md) for detailed design and realm determination approach.

---

## Port/Adapter Architecture

### Port Interface Hierarchy

The system follows a clear port/adapter hierarchy to maintain proper dependency direction:

#### Common Port Interfaces (`common-messaging`)

**EventPublisher Interface:**

- Base interface for event publishing
- Defined in `common-messaging` module
- Provides generic `publish()` methods
- No service-specific methods

**Usage:** All services depend on `common-messaging` for base event publishing interface.

#### Service-Specific Port Interfaces (`{service}-application-service`)

**Service Event Publisher Port:**

- Extends `EventPublisher` from `common-messaging`
- Defined in `{service}-application-service/port/messaging`
- May include service-specific methods if needed
- Represents the port that infrastructure adapts to

**Example:**

```java
// Port interface in application-service layer
public interface StockManagementEventPublisher extends EventPublisher {
    // Service-specific methods if needed
}
```

#### Port Implementations (`{service}-messaging`)

**Event Publisher Implementation:**

- Implements service-specific port interface
- Located in `{service}-messaging/publisher`
- Contains Kafka-specific implementation
- Adapts domain events to Kafka messages

**Example:**

```java
// Implementation in messaging layer
@Component
public class StockManagementEventPublisherImpl implements StockManagementEventPublisher {
    // Kafka implementation
}
```

### Port Types and Placement Rules

#### 1. Repository Ports (`port/repository`)

**Purpose:** Aggregate persistence (write model)

**Location:** `{service}-application-service/port/repository`

**Usage:** Command handlers use repository ports to persist aggregates

**Example:**

```java
// Port interface
public interface StockConsignmentRepository {
    void save(StockConsignment consignment);
    Optional<StockConsignment> findById(ConsignmentId id);
}

// Implementation in dataaccess layer
@Repository
public class StockConsignmentRepositoryAdapter implements StockConsignmentRepository {
    // JPA implementation for aggregate persistence
}
```

#### 2. Data Ports (`port/data`)

**Purpose:** Read model data access (projections/views)

**Location:** `{service}-application-service/port/data`

**Usage:** Query handlers use data ports to access read models (projections)

**Example:**

```java
// Port interface for read model
public interface StockLevelViewRepository {
    Optional<StockLevelView> findById(StockLevelViewId id);
    List<StockLevelView> findByProductId(ProductId productId);
    List<StockLevelView> findByTenantIdAndStatus(TenantId tenantId, StockLevelStatus status);
}

// Implementation in dataaccess layer
@Repository
public class StockLevelViewRepositoryAdapter implements StockLevelViewRepository {
    // JPA implementation for read model queries
}
```

**Key Distinction:**

- **Repository Ports** (`port/repository`): For aggregate persistence (write model)
- **Data Ports** (`port/data`): For read model queries (projections/views)
- **Query handlers should use data ports** for read model access (eventual consistency acceptable)
- **Query handlers may use repository ports** only when immediate consistency is required (rare)
- **Command handlers use repository ports** for aggregate persistence

**Port Usage Rules:**

**Command Handlers:**

- ✅ Use Repository Ports for aggregate persistence
- ✅ Use Event Publisher Ports for event publishing
- ❌ Do NOT use Data Ports

**Query Handlers:**

- ✅ **Primary:** Use Data Ports for read model access (eventual consistency)
- ⚠️ **Exception:** Use Repository Ports only when immediate consistency is required
- ❌ Do NOT use Repository Ports for standard queries

**When to Use Each Port:**

| Use Case                                         | Port Type       | Consistency | Performance                     |
|--------------------------------------------------|-----------------|-------------|---------------------------------|
| Aggregate persistence (commands)                 | Repository Port | Immediate   | Optimized for writes            |
| Read model queries (queries)                     | Data Port       | Eventual    | Optimized for reads             |
| Critical queries requiring immediate consistency | Repository Port | Immediate   | Slower (reads from write model) |

#### 3. Service Ports (`port/service`)

**Purpose:** External service integrations

**Location:** `{service}-application-service/port/service`

**Usage:** Application services use service ports for external system calls

**Example:**

```java
// Port interface for external service
public interface ProductServicePort {
    Optional<Product> getProduct(ProductId productId);
}

// Implementation in integration layer
@Component
public class ProductServiceAdapter implements ProductServicePort {
    // REST client implementation
}
```

#### 4. Event Publisher Ports (`port/messaging`)

**Purpose:** Event publishing

**Location:** `{service}-application-service/port/messaging`

**Usage:** Command handlers use event publisher ports to publish domain events

**Example:**

```java
// Port interface
public interface StockManagementEventPublisher extends EventPublisher {
    void publish(StockManagementEvent<?> event);
}

// Implementation in messaging layer
@Component
public class StockManagementEventPublisherImpl implements StockManagementEventPublisher {
    // Kafka implementation
}
```

### Port Placement Summary

| Port Type                 | Location          | Purpose                             | Used By              |
|---------------------------|-------------------|-------------------------------------|----------------------|
| **Repository Ports**      | `port/repository` | Aggregate persistence (write model) | Command handlers     |
| **Data Ports**            | `port/data`       | Read model queries (projections)    | Query handlers       |
| **Service Ports**         | `port/service`    | External service integration        | Application services |
| **Event Publisher Ports** | `port/messaging`  | Event publishing                    | Command handlers     |

### Dependency Direction

```
Domain Core (no dependencies)
    ↑
Application Service (depends on ports)
    ↑
Infrastructure Layers (implement ports)
    ├── Data Access (implements repository & data ports)
    ├── Messaging (implements event publisher ports)
    └── Integration (implements service ports)
```

**Key Principles:**

1. Application service layer defines ports (interfaces)
2. Infrastructure layers implement ports (adapters)
3. CQRS separation: Repository ports for writes, Data ports for reads
4. Dependency inversion: High-level depends on abstractions (ports)

---

## Appendix

### Glossary

| Term                   | Definition                                               |
|------------------------|----------------------------------------------------------|
| **Aggregate Root**     | Entity that serves as the entry point to an aggregate    |
| **Bounded Context**    | Explicit boundary within which a domain model applies    |
| **CQRS**               | Command Query Responsibility Segregation                 |
| **Domain Event**       | Event representing something that happened in the domain |
| **Event Choreography** | Coordination through events without central orchestrator |
| **FEFO**               | First Expiring First Out                                 |
| **LDP**                | Local Distribution Partner                               |
| **PWA**                | Progressive Web App                                      |
| **SAGA**               | Pattern for managing distributed transactions (not used) |

### References

- [Business Requirements Document](../00-business-requiremants/business-requirements-document.md)
- [Project Roadmap](../project-management/project-roadmap.md)
- Domain-Driven Design by Eric Evans
- Implementing Domain-Driven Design by Vaughn Vernon
- Microservices Patterns by Chris Richardson

---

**Document Control**

- **Version History:**
    - v2.0 (2025-01): Resolved critical architecture gaps - Event Sourcing clarification, Read Model Projections, Port/Adapter boundaries, Aggregate Consistency, Event Versioning
    - v1.0 (2025-11): Initial draft
- **Review Cycle:** This document will be reviewed monthly or when architectural changes occur
- **Distribution:** This document will be distributed to all technical team members

**Note:** This document contains architectural principles and patterns. For detailed code templates and implementation examples, refer to
the [Mandated Implementation Template Guide](../../guide/mandated-Implementation-template-guide.md) and related template files.

