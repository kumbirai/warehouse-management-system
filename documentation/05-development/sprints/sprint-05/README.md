# Sprint 5 Implementation Plans

## Warehouse Management System Integration - CCBSA LDP System

**Sprint:** Sprint 5 - Picking Operations
**Duration:** 2 weeks
**Sprint Goal:** Enable picking list management and execution
**Total Story Points:** 39

---

## Overview

Sprint 5 focuses on implementing picking list management to enable warehouse operators to efficiently process picking operations. This sprint builds on the foundation laid in
Sprints 1-4, adding critical picking management capabilities.

### Key Features

1. **Upload Picking List via CSV** - Bulk import of picking lists from CSV files
2. **Manual Picking List Entry** - Individual picking list creation through UI
3. **Create Picking List** - Domain model for picking lists, loads, and orders
4. **Plan Picking Locations** - Optimize picking locations based on FEFO principles
5. **Map Orders to Loads** - Manage order-to-load relationships

---

## Implementation Plans

### Main Sprint Plan

- **[00-Sprint-05-Implementation-Plan.md](00-Sprint-05-Implementation-Plan.md)** - Overall sprint plan with architecture, data flows, and implementation order

### User Story Implementation Plans

1. **[01-Upload-Picking-List-CSV-Implementation-Plan.md](01-Upload-Picking-List-CSV-Implementation-Plan.md)** - US-6.1.1: Upload Picking List via CSV File (8 points)
2. **[02-Manual-Picking-List-Entry-Implementation-Plan.md](02-Manual-Picking-List-Entry-Implementation-Plan.md)** - US-6.1.2: Manual Picking List Entry via UI (8 points)
3. **[03-Create-Picking-List-Implementation-Plan.md](03-Create-Picking-List-Implementation-Plan.md)** - US-6.1.4: Create Picking List (5 points)
4. **[04-Plan-Picking-Locations-Implementation-Plan.md](04-Plan-Picking-Locations-Implementation-Plan.md)** - US-6.2.1: Plan Picking Locations (13 points)
5. **[05-Map-Orders-to-Loads-Implementation-Plan.md](05-Map-Orders-to-Loads-Implementation-Plan.md)** - US-6.2.2: Map Orders to Loads (5 points)

### Testing Plan

- **[06-Gateway-API-Tests-Implementation-Plan.md](06-Gateway-API-Tests-Implementation-Plan.md)** - Gateway API tests for all Sprint 5 endpoints

---

## Architecture Principles

All implementations follow:

- **Domain-Driven Design (DDD)** - Business logic drives architecture
- **Clean Hexagonal Architecture** - Clear separation of concerns
- **CQRS** - Command Query Responsibility Segregation
- **Event-Driven Choreography** - Loose coupling through events
- **DRY Principle** - Common value objects moved to `common-domain`

---

## Service Segregation

### Picking Service

- Upload picking lists via CSV
- Create picking lists manually
- Plan picking locations with FEFO optimization
- Map orders to loads
- Generate picking tasks
- Track picking list and order status

---

## Dependencies

### Prerequisites from Previous Sprints

- ✅ Sprint 1: Location Management, Product Management
- ✅ Sprint 2: Stock Consignment Creation, Product Barcode Validation
- ✅ Sprint 3: Stock Classification, FEFO Location Assignment
- ✅ Sprint 4: Stock Movement, Stock Levels, Stock Allocation

### Infrastructure

- ✅ Eureka Server
- ✅ Gateway Service
- ✅ Kafka
- ✅ PostgreSQL
- ✅ Keycloak

---

## Implementation Order

1. **Phase 1:** Picking List Domain Model (Days 1-3)
2. **Phase 2:** Backend Application Services (Days 4-7)
3. **Phase 3:** Backend Infrastructure (Days 8-11)
4. **Phase 4:** Backend REST API (Days 12-13)
5. **Phase 5:** Frontend Implementation (Days 14-17)
6. **Phase 6:** Gateway API Tests (Days 18-20)

---

## Common Value Objects

The following value objects are moved to `common-domain` (DRY principle):

- `LoadNumber` - Load identifier (shared across services)
- `OrderNumber` - Order identifier (shared across services)
- `CustomerInfo` - Customer information (shared across services)
- `Priority` - Picking priority enum (shared across services)

---

## Event Flow

```
Picking List CSV Upload
  ↓
Parse and Validate CSV
  ↓
Create PickingList Aggregates
  ↓
PickingListReceivedEvent
  ↓
Plan Picking Locations (FEFO)
  ↓
LoadPlannedEvent
  ↓
Create PickingTasks
  ↓
PickingTaskCreatedEvent
  ↓
Allocate Stock
  ↓
StockAllocatedEvent
```

---

## Testing Strategy

- **Unit Tests** - Domain logic, CSV parsing, business rules
- **Integration Tests** - Service integration, database, Kafka
- **Gateway API Tests** - End-to-end through gateway service
- **Event-Driven Tests** - Verify event publishing and consumption

---

## CSV Upload Features

### CSV Format

- **Required Columns:** Load Number, Order Number, Customer Code, Product Code, Quantity
- **Optional Columns:** Customer Name, Priority, Notes
- **Max File Size:** 10MB
- **Encoding:** UTF-8

### Validation

- File format and size validation
- Column header validation
- Data type validation
- Product existence validation
- Business rule validation

### Error Handling

- Detailed error messages with row numbers
- Downloadable error report
- Partial success handling (create valid rows, report errors)

---

## Next Steps

1. Review all implementation plans
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
