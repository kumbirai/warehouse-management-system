# User Story Breakdown

## Warehouse Management System Integration - CCBSA LDP System

**Document Version:** 1.0  
**Date:** 2025-11  
**Status:** Draft  
**Related Documents:**

- [Business Requirements Document](../00-business-requiremants/business-requirements-document.md)
- [Service Architecture Document](../architecture/Service_Architecture_Document.md)
- [Domain Model Design](../architecture/Domain_Model_Design.md)
- [API Specifications](../api/API_Specifications.md)
- [Project Roadmap](project-roadmap.md)

---

## Table of Contents

1. [Overview](#overview)
2. [User Story Format](#user-story-format)
3. [Epic 1: Stock Consignment Management](#epic-1-stock-consignment-management)
4. [Epic 2: Stock Classification and Expiration Management](#epic-2-stock-classification-and-expiration-management)
5. [Epic 3: Warehouse Location Management](#epic-3-warehouse-location-management)
6. [Epic 4: Product Identification](#epic-4-product-identification)
7. [Epic 5: Stock Level Management](#epic-5-stock-level-management)
8. [Epic 6: Picking List Management](#epic-6-picking-list-management)
7. [Epic 7: Returns Management](#epic-7-returns-management)
8. [Epic 8: Reconciliation](#epic-8-reconciliation)
9. [Epic 9: Integration Requirements](#epic-9-integration-requirements)
10. [Non-Functional Requirements](#non-functional-requirements)

---

## Overview

### Purpose

This document breaks down the Business Requirements Document (BRD) functional requirements into implementable user stories. Each user story follows the standard format: "As
a [role], I want [goal] so that [benefit]."

### User Story Structure

Each user story includes:

- **Story ID** - Unique identifier (e.g., US-1.1.1)
- **Title** - Brief descriptive title
- **User Story** - Standard format statement
- **Acceptance Criteria** - Specific, testable conditions
- **Technical Notes** - Implementation considerations
- **Dependencies** - Related stories or prerequisites
- **Priority** - Must Have, Should Have, Could Have, Won't Have (MoSCoW)
- **Story Points** - Effort estimation (Fibonacci scale)
- **Service** - Owning service

### Epic Mapping

Epics align with BRD Functional Requirements:

- **Epic 1:** FR-1 - Stock Consignment Management
- **Epic 2:** FR-2 - Stock Classification and Expiration Management
- **Epic 3:** FR-3 - Warehouse Location Management
- **Epic 4:** FR-4 - Product Identification
- **Epic 5:** FR-5 - Stock Level Management
- **Epic 6:** FR-6 - Picking List Management
- **Epic 7:** FR-7 - Returns Management
- **Epic 8:** FR-8 - Reconciliation
- **Epic 9:** FR-9 - Integration Requirements

---

## User Story Format

### Standard Format

```
**Story ID:** US-X.Y.Z
**Title:** [Brief Title]
**Epic:** [Epic Name]
**Service:** [Service Name]
**Priority:** [Must Have / Should Have / Could Have]
**Story Points:** [Estimate]

**As a** [role]
**I want** [goal]
**So that** [benefit]

**Acceptance Criteria:**
- [ ] AC1: [Specific testable condition]
- [ ] AC2: [Specific testable condition]
- [ ] AC3: [Specific testable condition]

**Technical Notes:**
- [Implementation considerations]

**Dependencies:**
- [Related stories or prerequisites]

**Related BRD Requirements:**
- FR-X.Y: [Requirement reference]
```

---

## Epic 1: Stock Consignment Management

**Epic ID:** EPIC-1  
**BRD Reference:** FR-1  
**Service:** Stock Management Service  
**Description:** Manage incoming stock consignment from D365, validate receipt, and confirm consignment.

### US-1.1.1: Ingest Consignment Data from D365

**Story ID:** US-1.1.1  
**Title:** Ingest Consignment Data from D365  
**Epic:** Stock Consignment Management  
**Service:** Stock Management Service (Integration Service)  
**Priority:** Must Have  
**Story Points:** 8

**As a** warehouse operator  
**I want** the system to automatically ingest stock consignment data from D365  
**So that** I don't have to manually enter consignment information

**Acceptance Criteria:**

- [ ] AC1: System receives consignment data from D365 within 5 minutes of D365 transaction
- [ ] AC2: Consignment data includes: product codes, quantities, expiration dates, consignment reference numbers, timestamps
- [ ] AC3: System validates data completeness and format before processing
- [ ] AC4: System handles D365 API errors gracefully with retry logic
- [ ] AC5: System logs all consignment ingestion events for audit

**Technical Notes:**

- Integration Service consumes D365 webhook or polls D365 API
- Publishes `ConsignmentDataReceivedEvent` to Kafka
- Stock Management Service consumes event and creates `StockConsignment` aggregate
- Validate against Product Service for product existence

**Dependencies:**

- US-4.1.1: Product Master Data Synchronization (for product validation)

**Related BRD Requirements:**

- FR-1.1: Incoming Stock Ingestion

---

### US-1.1.2: Validate Consignment Data

**Story ID:** US-1.1.2  
**Title:** Validate Consignment Data  
**Epic:** Stock Consignment Management  
**Service:** Stock Management Service  
**Priority:** Must Have  
**Story Points:** 5

**As a** warehouse operator  
**I want** the system to validate received consignment data  
**So that** I can identify discrepancies before processing

**Acceptance Criteria:**

- [ ] AC1: System validates product codes against master data
- [ ] AC2: System validates quantities are positive numbers
- [ ] AC3: System validates expiration dates are in the future (or null for non-perishable)
- [ ] AC4: System validates consignment reference is unique
- [ ] AC5: System flags discrepancies for manual review
- [ ] AC6: System generates exception reports for mismatches

**Technical Notes:**

- Query Product Service to validate product codes
- Validate expiration dates using `ExpirationDate` value object
- Check consignment reference uniqueness in database
- Create exception record for manual review workflow

**Dependencies:**

- US-1.1.1: Ingest Consignment Data from D365
- US-4.1.1: Product Master Data Synchronization

**Related BRD Requirements:**

- FR-1.3: Stock Receipt Validation

---

### US-1.1.3: Create Stock Consignment

**Story ID:** US-1.1.3  
**Title:** Create Stock Consignment  
**Epic:** Stock Consignment Management  
**Service:** Stock Management Service  
**Priority:** Must Have  
**Story Points:** 8

**As a** warehouse operator  
**I want** to create a stock consignment record  
**So that** I can track incoming stock

**Acceptance Criteria:**

- [ ] AC1: System creates `StockConsignment` aggregate with consignment reference
- [ ] AC2: System creates `ConsignmentLineItem` entities for each product
- [ ] AC3: System sets consignment status to "RECEIVED"
- [ ] AC4: System records received timestamp
- [ ] AC5: System publishes `StockConsignmentReceivedEvent`
- [ ] AC6: System assigns initial location based on FEFO principles (delegates to Location Service)

**Technical Notes:**

- Use `StockConsignment.builder()` pattern
- Validate all required fields before creation
- Publish domain event after successful creation
- Trigger location assignment workflow

**Dependencies:**

- US-1.1.2: Validate Consignment Data
- US-3.2.1: Assign Location to Stock

**Related BRD Requirements:**

- FR-1.1: Incoming Stock Ingestion

---

### US-1.1.4: Confirm Consignment Receipt

**Story ID:** US-1.1.4  
**Title:** Confirm Consignment Receipt  
**Epic:** Stock Consignment Management  
**Service:** Stock Management Service  
**Priority:** Must Have  
**Story Points:** 5

**As a** warehouse operator  
**I want** to confirm receipt of consigned stock  
**So that** D365 is notified of successful receipt

**Acceptance Criteria:**

- [ ] AC1: System allows confirmation of received consignments
- [ ] AC2: System updates consignment status to "CONFIRMED"
- [ ] AC3: System records confirmation timestamp
- [ ] AC4: System includes received quantities, received date/time, location assignments in confirmation
- [ ] AC5: System publishes `StockConsignmentConfirmedEvent`
- [ ] AC6: System sends confirmation to D365 within 2 minutes of confirmation
- [ ] AC7: System handles partial receipts

**Technical Notes:**

- Call `stockConsignment.confirm()` method
- Integration Service consumes `StockConsignmentConfirmedEvent`
- Send confirmation to D365 via OData API
- Handle retry logic for D365 API failures

**Dependencies:**

- US-1.1.3: Create Stock Consignment
- US-9.1.1: Send Consignment Confirmation to D365

**Related BRD Requirements:**

- FR-1.2: Consignment Confirmation

---

### US-1.1.5: Handle Partial Consignment Receipt

**Story ID:** US-1.1.5  
**Title:** Handle Partial Consignment Receipt  
**Epic:** Stock Consignment Management  
**Service:** Stock Management Service  
**Priority:** Should Have  
**Story Points:** 5

**As a** warehouse operator  
**I want** to handle partial consignment receipts  
**So that** I can process consignments when not all items are received

**Acceptance Criteria:**

- [ ] AC1: System allows marking individual line items as received
- [ ] AC2: System tracks received vs expected quantities per line item
- [ ] AC3: System publishes `PartialConsignmentReceivedEvent` for partial receipts
- [ ] AC4: System allows completing remaining items later
- [ ] AC5: System generates reports for partial receipts

**Technical Notes:**

- Extend `ConsignmentLineItem` to track received quantity
- Allow multiple confirmations for same consignment
- Update consignment status based on completion percentage

**Dependencies:**

- US-1.1.4: Confirm Consignment Receipt

**Related BRD Requirements:**

- FR-1.2: Consignment Confirmation (partial receipts)

---

## Epic 2: Stock Classification and Expiration Management

**Epic ID:** EPIC-2  
**BRD Reference:** FR-2  
**Service:** Stock Management Service  
**Description:** Classify stock by expiration dates, ensure FEFO compliance, and track expiration alerts.

### US-2.1.1: Classify Stock by Expiration Date

**Story ID:** US-2.1.1  
**Title:** Classify Stock by Expiration Date  
**Epic:** Stock Classification and Expiration Management  
**Service:** Stock Management Service  
**Priority:** Must Have  
**Story Points:** 5

**As a** warehouse operator  
**I want** stock to be automatically classified by expiration dates  
**So that** I can identify stock that needs priority handling

**Acceptance Criteria:**

- [ ] AC1: System automatically assigns classification when stock item is created
- [ ] AC2: Classification categories: EXPIRED, CRITICAL (≤7 days), NEAR_EXPIRY (≤30 days), NORMAL, EXTENDED_SHELF_LIFE
- [ ] AC3: Classification is visible in all stock views and reports
- [ ] AC4: Classification updates automatically when expiration date changes
- [ ] AC5: System handles null expiration dates (non-perishable) as NORMAL

**Technical Notes:**

- Use `StockItem.checkExpiration()` method
- Classification logic in `StockClassification` enum
- Update classification on stock item creation and expiration date changes
- Store classification in `StockItem` aggregate

**Dependencies:**

- US-1.1.3: Create Stock Consignment

**Related BRD Requirements:**

- FR-2.1: Expiration Date Classification

---

### US-2.1.2: Assign Locations Based on FEFO Principles

**Story ID:** US-2.1.2  
**Title:** Assign Locations Based on FEFO Principles  
**Epic:** Stock Classification and Expiration Management  
**Service:** Location Management Service  
**Priority:** Must Have  
**Story Points:** 8

**As a** warehouse operator  
**I want** stock to be assigned locations based on FEFO principles  
**So that** first expiring stock is positioned for first picking

**Acceptance Criteria:**

- [ ] AC1: System considers expiration date when assigning locations
- [ ] AC2: Locations closer to picking zones contain stock with earlier expiration dates
- [ ] AC3: System prevents picking of newer stock before older stock expires
- [ ] AC4: Visual indicators show expiration date priority in location views
- [ ] AC5: System optimizes location assignments to minimize movement

**Technical Notes:**

- Location assignment algorithm considers expiration date and proximity to picking zones
- Query available locations sorted by distance to picking zone
- Filter locations by expiration date priority
- Assign stock with earliest expiration to closest available location

**Dependencies:**

- US-3.2.1: Assign Location to Stock
- US-2.1.1: Classify Stock by Expiration Date

**Related BRD Requirements:**

- FR-2.2: FEFO Stock Packing

---

### US-2.1.3: Track Expiration Dates and Generate Alerts

**Story ID:** US-2.1.3  
**Title:** Track Expiration Dates and Generate Alerts  
**Epic:** Stock Classification and Expiration Management  
**Service:** Stock Management Service  
**Priority:** Must Have  
**Story Points:** 5

**As a** warehouse manager  
**I want** to receive alerts when stock is expiring  
**So that** I can take action to prevent waste

**Acceptance Criteria:**

- [ ] AC1: System generates alert when stock is within 7 days of expiration
- [ ] AC2: System generates alert when stock is within 30 days of expiration
- [ ] AC3: System prevents picking of expired stock
- [ ] AC4: System generates reports on expiring stock
- [ ] AC5: Alerts are visible in dashboard and can be filtered by date range

**Technical Notes:**

- Scheduled job runs daily to check expiration dates
- Publish `StockExpiringAlertEvent` for alerts
- Update `StockItem` classification when alerts trigger
- Create read model for expiring stock dashboard

**Dependencies:**

- US-2.1.1: Classify Stock by Expiration Date

**Related BRD Requirements:**

- FR-2.3: Expiration Date Tracking

---

### US-2.1.4: Prevent Picking of Expired Stock

**Story ID:** US-2.1.4  
**Title:** Prevent Picking of Expired Stock  
**Epic:** Stock Classification and Expiration Management  
**Service:** Stock Management Service, Picking Service  
**Priority:** Must Have  
**Story Points:** 3

**As a** warehouse operator  
**I want** the system to prevent picking expired stock  
**So that** expired products are not shipped to customers

**Acceptance Criteria:**

- [ ] AC1: System checks stock expiration date before allowing picking
- [ ] AC2: System displays error message when attempting to pick expired stock
- [ ] AC3: System excludes expired stock from picking location queries
- [ ] AC4: System logs attempts to pick expired stock

**Technical Notes:**

- `StockItem.canBePicked()` method checks expiration
- Picking Service queries stock items filtered by `canBePicked() = true`
- Return appropriate error when expired stock is selected

**Dependencies:**

- US-2.1.1: Classify Stock by Expiration Date
- US-6.2.1: Plan Picking Locations

**Related BRD Requirements:**

- FR-2.3: Expiration Date Tracking

---

## Epic 3: Warehouse Location Management

**Epic ID:** EPIC-3  
**BRD Reference:** FR-3  
**Service:** Location Management Service  
**Description:** Manage warehouse locations, barcodes, stock movements, and location capacity.

### US-3.1.1: Create Warehouse Location with Barcode

**Story ID:** US-3.1.1  
**Title:** Create Warehouse Location with Barcode  
**Epic:** Warehouse Location Management  
**Service:** Location Management Service  
**Priority:** Must Have  
**Story Points:** 5

**As a** warehouse administrator  
**I want** to create warehouse locations with barcodes  
**So that** all locations can be scanned and tracked

**Acceptance Criteria:**

- [ ] AC1: System allows creation of location with unique barcode identifier
- [ ] AC2: Barcode format follows CCBSA standards
- [ ] AC3: System validates barcode uniqueness
- [ ] AC4: System supports barcode scanning for location identification
- [ ] AC5: Location barcodes are printable and replaceable
- [ ] AC6: System stores location coordinates (zone, aisle, rack, level)

**Technical Notes:**

- Use `Location.builder()` pattern
- Validate barcode format against CCBSA standards
- Check barcode uniqueness in database
- Generate barcode image for printing

**Dependencies:**

- None (foundational)

**Related BRD Requirements:**

- FR-3.1: Location Barcoding

---

### US-3.1.2: Scan Location Barcode

**Story ID:** US-3.1.2  
**Title:** Scan Location Barcode  
**Epic:** Warehouse Location Management  
**Service:** Location Management Service  
**Priority:** Must Have  
**Story Points:** 5

**As a** warehouse operator  
**I want** to scan location barcodes  
**So that** I can quickly identify locations

**Acceptance Criteria:**

- [ ] AC1: System supports scanning location barcodes using handheld scanners
- [ ] AC2: System supports scanning location barcodes using mobile device camera
- [ ] AC3: System validates scanned barcode against master data
- [ ] AC4: System displays location details after successful scan
- [ ] AC5: System handles invalid barcode scans with error message

**Technical Notes:**

- Frontend barcode scanner component (ZXing library)
- API endpoint: `GET /locations/by-barcode/{barcode}`
- Validate barcode format before querying database
- Return location details or 404 if not found

**Dependencies:**

- US-3.1.1: Create Warehouse Location with Barcode
- Frontend barcode scanning implementation

**Related BRD Requirements:**

- FR-3.1: Location Barcoding

---

### US-3.2.1: Assign Location to Stock

**Story ID:** US-3.2.1  
**Title:** Assign Location to Stock  
**Epic:** Warehouse Location Management  
**Service:** Location Management Service  
**Priority:** Must Have  
**Story Points:** 8

**As a** warehouse operator  
**I want** stock to be assigned to locations based on FEFO principles and capacity  
**So that** stock is optimally placed for picking

**Acceptance Criteria:**

- [ ] AC1: System considers expiration date when assigning locations (FEFO)
- [ ] AC2: System respects location capacity limits
- [ ] AC3: System optimizes location assignments to minimize movement
- [ ] AC4: System supports manual location override with authorization
- [ ] AC5: System updates location status to OCCUPIED when assigned
- [ ] AC6: System publishes `LocationAssignedEvent`

**Technical Notes:**

- Location assignment algorithm considers expiration date, capacity, and proximity
- Query available locations filtered by capacity and status
- Sort by expiration date priority and distance to picking zone
- Assign first matching location
- Update location status and capacity

**Dependencies:**

- US-3.1.1: Create Warehouse Location with Barcode
- US-2.1.2: Assign Locations Based on FEFO Principles

**Related BRD Requirements:**

- FR-3.2: Location Assignment

---

### US-3.3.1: Track Stock Movement

**Story ID:** US-3.3.1  
**Title:** Track Stock Movement  
**Epic:** Warehouse Location Management  
**Service:** Location Management Service  
**Priority:** Must Have  
**Story Points:** 8

**As a** warehouse operator  
**I want** all stock movements to be tracked  
**So that** I have complete visibility of stock location

**Acceptance Criteria:**

- [ ] AC1: System tracks movement from receiving to storage location
- [ ] AC2: System tracks movement from storage location to picking location
- [ ] AC3: System tracks movement between storage locations
- [ ] AC4: System tracks movement from picking location to shipping
- [ ] AC5: Each movement records: timestamp, user, source location, destination location, quantity, reason
- [ ] AC6: System maintains complete audit trail
- [ ] AC7: System publishes `StockMovementCompletedEvent`

**Technical Notes:**

- Create `StockMovement` aggregate for each movement
- Use `StockMovement.builder()` pattern
- Validate source and destination locations are different
- Update source and destination location status and capacity
- Publish domain event after successful movement

**Dependencies:**

- US-3.2.1: Assign Location to Stock

**Related BRD Requirements:**

- FR-3.3: Stock Movement Tracking

---

### US-3.3.2: Initiate Stock Movement

**Story ID:** US-3.3.2  
**Title:** Initiate Stock Movement  
**Epic:** Warehouse Location Management  
**Service:** Location Management Service  
**Priority:** Must Have  
**Story Points:** 5

**As a** warehouse operator  
**I want** to initiate stock movements  
**So that** I can move stock between locations

**Acceptance Criteria:**

- [ ] AC1: System allows selecting source and destination locations
- [ ] AC2: System validates source location has sufficient stock
- [ ] AC3: System validates destination location has capacity
- [ ] AC4: System requires movement reason (PICKING, RESTOCKING, REORGANIZATION, etc.)
- [ ] AC5: System publishes `StockMovementInitiatedEvent`
- [ ] AC6: System allows canceling initiated movements

**Technical Notes:**

- API endpoint: `POST /stock-movements`
- Validate stock availability in source location
- Validate capacity in destination location
- Create `StockMovement` with status INITIATED
- Allow completion or cancellation

**Dependencies:**

- US-3.3.1: Track Stock Movement

**Related BRD Requirements:**

- FR-3.3: Stock Movement Tracking

---

### US-3.4.1: Manage Location Status

**Story ID:** US-3.4.1  
**Title:** Manage Location Status  
**Epic:** Warehouse Location Management  
**Service:** Location Management Service  
**Priority:** Must Have  
**Story Points:** 5

**As a** warehouse administrator  
**I want** to manage location status  
**So that** I can block locations for maintenance or mark them as available

**Acceptance Criteria:**

- [ ] AC1: System tracks location status: OCCUPIED, AVAILABLE, RESERVED, BLOCKED
- [ ] AC2: System updates location status in real-time
- [ ] AC3: System tracks location capacity (current vs maximum)
- [ ] AC4: System allows blocking locations for maintenance or issues
- [ ] AC5: System prevents assignment to blocked locations
- [ ] AC6: System publishes `LocationStatusChangedEvent`

**Technical Notes:**

- Update `Location` aggregate status
- Validate status transitions (e.g., cannot assign to BLOCKED)
- Track capacity in `LocationCapacity` value object
- Publish domain event on status change

**Dependencies:**

- US-3.1.1: Create Warehouse Location with Barcode

**Related BRD Requirements:**

- FR-3.4: Location Status Management

---

## Epic 4: Product Identification

**Epic ID:** EPIC-4  
**BRD Reference:** FR-4  
**Service:** Product Service  
**Description:** Manage product master data and barcode validation.

### US-4.1.1: Synchronize Product Master Data from D365

**Story ID:** US-4.1.1  
**Title:** Synchronize Product Master Data from D365  
**Epic:** Product Identification  
**Service:** Product Service (Integration Service)  
**Priority:** Must Have  
**Story Points:** 8

**As a** system administrator  
**I want** product master data to be synchronized from D365  
**So that** product information is always up to date

**Acceptance Criteria:**

- [ ] AC1: System synchronizes product master data daily or on-demand
- [ ] AC2: Product data includes: product codes, descriptions, barcodes, unit of measure
- [ ] AC3: System handles product master data changes (create, update, delete)
- [ ] AC4: System validates product data before saving
- [ ] AC5: System publishes `ProductCreatedEvent`, `ProductUpdatedEvent` for changes
- [ ] AC6: System handles synchronization errors gracefully

**Technical Notes:**

- Scheduled job runs daily to sync products
- Integration Service fetches products from D365 OData API
- Publishes events to Kafka
- Product Service consumes events and updates product data
- Handle product deletions (soft delete or archive)

**Dependencies:**

- US-9.1.1: D365 Integration Setup

**Related BRD Requirements:**

- FR-4.2: Product Master Data Synchronization

---

### US-4.1.2: Validate Product Barcode

**Story ID:** US-4.1.2  
**Title:** Validate Product Barcode  
**Epic:** Product Identification  
**Service:** Product Service  
**Priority:** Must Have  
**Story Points:** 5

**As a** warehouse operator  
**I want** to validate product barcodes  
**So that** I can ensure products are correctly identified

**Acceptance Criteria:**

- [ ] AC1: System validates product barcodes against master data
- [ ] AC2: System supports multiple barcode formats (EAN-13, Code 128, QR Code, etc.)
- [ ] AC3: System returns product details when barcode is valid
- [ ] AC4: System returns error when barcode is invalid or not found
- [ ] AC5: System handles multiple barcodes per product (primary and secondary)

**Technical Notes:**

- API endpoint: `GET /products/by-barcode/{barcode}`
- Validate barcode format before querying database
- Query product by primary or secondary barcode
- Return product details or 404 if not found
- Cache frequently accessed products

**Dependencies:**

- US-4.1.1: Synchronize Product Master Data from D365

**Related BRD Requirements:**

- FR-4.1: Product Barcode Support

---

### US-4.1.3: Scan Product Barcode

**Story ID:** US-4.1.3  
**Title:** Scan Product Barcode  
**Epic:** Product Identification  
**Service:** Product Service (Frontend)  
**Priority:** Must Have  
**Story Points:** 5

**As a** warehouse operator  
**I want** to scan product barcodes  
**So that** I can quickly identify products

**Acceptance Criteria:**

- [ ] AC1: System supports scanning product barcodes using handheld scanners
- [ ] AC2: System supports scanning product barcodes using mobile device camera
- [ ] AC3: System validates scanned barcode against master data
- [ ] AC4: System displays product details after successful scan
- [ ] AC5: System handles invalid barcode scans with error message
- [ ] AC6: System works in all relevant processes (receiving, picking, returns, stock counting)

**Technical Notes:**

- Frontend barcode scanner component (ZXing library)
- Call Product Service API to validate barcode
- Display product details in UI
- Handle errors gracefully

**Dependencies:**

- US-4.1.2: Validate Product Barcode
- Frontend barcode scanning implementation

**Related BRD Requirements:**

- FR-4.1: Product Barcode Support

---

## Epic 5: Stock Level Management

**Epic ID:** EPIC-5  
**BRD Reference:** FR-5  
**Service:** Stock Management Service  
**Description:** Monitor stock levels, enforce minimum/maximum thresholds, and generate restock requests.

### US-5.1.1: Monitor Stock Levels

**Story ID:** US-5.1.1  
**Title:** Monitor Stock Levels  
**Epic:** Stock Level Management  
**Service:** Stock Management Service  
**Priority:** Must Have  
**Story Points:** 5

**As a** warehouse manager  
**I want** to monitor stock levels in real-time  
**So that** I can ensure adequate stock availability

**Acceptance Criteria:**

- [ ] AC1: System calculates stock levels in real-time
- [ ] AC2: Stock level visibility in dashboards
- [ ] AC3: Historical stock level tracking
- [ ] AC4: Stock level reports by product, location, warehouse
- [ ] AC5: System updates stock levels on stock movements, picking, returns

**Technical Notes:**

- Create `StockLevel` aggregate per product/location
- Update stock levels via domain events (StockMovementCompletedEvent, StockPickedEvent, StockReturnedEvent)
- Create read model for dashboard queries
- Support filtering and aggregation

**Dependencies:**

- US-1.1.3: Create Stock Consignment
- US-3.3.1: Track Stock Movement

**Related BRD Requirements:**

- FR-5.2: Stock Level Monitoring

---

### US-5.1.2: Enforce Minimum and Maximum Stock Levels

**Story ID:** US-5.1.2  
**Title:** Enforce Minimum and Maximum Stock Levels  
**Epic:** Stock Level Management  
**Service:** Stock Management Service  
**Priority:** Must Have  
**Story Points:** 5

**As a** warehouse manager  
**I want** stock levels to be maintained within minimum and maximum thresholds  
**So that** I can prevent stockouts and overstocking

**Acceptance Criteria:**

- [ ] AC1: System maintains minimum and maximum levels per product
- [ ] AC2: Levels may vary by location or warehouse
- [ ] AC3: System prevents stock levels from exceeding maximum
- [ ] AC4: System alerts when stock approaches minimum
- [ ] AC5: System validates thresholds when updating stock levels

**Technical Notes:**

- Store `MinimumQuantity` and `MaximumQuantity` in `StockLevel` aggregate
- Validate on stock level updates
- Publish `StockLevelAboveMaximumEvent` when exceeded
- Alert on approach to minimum (e.g., within 10% of minimum)

**Dependencies:**

- US-5.1.1: Monitor Stock Levels

**Related BRD Requirements:**

- FR-5.1: Minimum and Maximum Level Enforcement

---

### US-5.1.3: Generate Restock Request

**Story ID:** US-5.1.3  
**Title:** Generate Restock Request  
**Epic:** Stock Level Management  
**Service:** Stock Management Service  
**Priority:** Must Have  
**Story Points:** 5

**As a** warehouse manager  
**I want** restock requests to be automatically generated when stock falls below minimum  
**So that** I can maintain adequate stock levels

**Acceptance Criteria:**

- [ ] AC1: System automatically generates restock request when stock falls below minimum
- [ ] AC2: Restock request includes: product code, current quantity, minimum quantity, requested quantity, priority
- [ ] AC3: Request is sent to D365 for processing
- [ ] AC4: System tracks restock request status
- [ ] AC5: System prevents duplicate restock requests

**Technical Notes:**

- `StockLevel.checkMinimumThreshold()` method
- Generate `RestockRequestGeneratedEvent` when threshold breached
- Calculate requested quantity (maximum - current)
- Integration Service consumes event and sends to D365
- Track request status to prevent duplicates

**Dependencies:**

- US-5.1.2: Enforce Minimum and Maximum Stock Levels
- US-9.1.5: Send Restock Request to D365

**Related BRD Requirements:**

- FR-5.3: Restock Request Generation

---

## Epic 6: Picking List Management

**Epic ID:** EPIC-6  
**BRD Reference:** FR-6  
**Service:** Picking Service  
**Description:** Manage picking lists, load planning, order-to-load mapping, and picking execution.

### US-6.1.1: Ingest Picking List from D365

**Story ID:** US-6.1.1  
**Title:** Ingest Picking List from D365  
**Epic:** Picking List Management  
**Service:** Picking Service (Integration Service)  
**Priority:** Must Have  
**Story Points:** 8

**As a** warehouse operator  
**I want** picking lists to be automatically ingested from D365  
**So that** I can start picking operations immediately

**Acceptance Criteria:**

- [ ] AC1: System receives picking lists from D365 in near real-time
- [ ] AC2: Picking list includes: load number, order numbers, customer information, products, quantities, priorities
- [ ] AC3: System validates picking list data
- [ ] AC4: System handles picking list updates and cancellations
- [ ] AC5: System publishes `PickingListReceivedEvent`

**Technical Notes:**

- Integration Service receives picking list from D365 (webhook or polling)
- Publishes `PickingListReceivedEvent` to Kafka
- Picking Service consumes event and creates `PickingList` aggregate
- Validate data completeness and format

**Dependencies:**

- US-9.1.1: D365 Integration Setup

**Related BRD Requirements:**

- FR-6.1: Picking List Ingestion

---

### US-6.1.2: Create Picking List

**Story ID:** US-6.1.2  
**Title:** Create Picking List  
**Epic:** Picking List Management  
**Service:** Picking Service  
**Priority:** Must Have  
**Story Points:** 5

**As a** warehouse operator  
**I want** to create picking list records  
**So that** I can track picking operations

**Acceptance Criteria:**

- [ ] AC1: System creates `PickingList` aggregate with picking list reference
- [ ] AC2: System creates `Load` aggregate containing multiple orders
- [ ] AC3: System creates `Order` entities within load
- [ ] AC4: System sets picking list status to "RECEIVED"
- [ ] AC5: System publishes `PickingListReceivedEvent`

**Technical Notes:**

- Use `PickingList.builder()` pattern
- Create load and order aggregates
- Validate all required fields
- Publish domain event after creation

**Dependencies:**

- US-6.1.1: Ingest Picking List from D365

**Related BRD Requirements:**

- FR-6.1: Picking List Ingestion

---

### US-6.2.1: Plan Picking Locations

**Story ID:** US-6.2.1  
**Title:** Plan Picking Locations  
**Epic:** Picking List Management  
**Service:** Picking Service  
**Priority:** Must Have  
**Story Points:** 13

**As a** warehouse operator  
**I want** picking locations to be optimized based on FEFO principles  
**So that** I can efficiently pick stock with earliest expiration dates

**Acceptance Criteria:**

- [ ] AC1: System optimizes picking locations based on FEFO principles
- [ ] AC2: System considers location proximity to minimize travel time
- [ ] AC3: System generates picking sequence/route suggestions
- [ ] AC4: System creates `PickingTask` entities for each location/product combination
- [ ] AC5: System publishes `LoadPlannedEvent` and `PickingTaskCreatedEvent`
- [ ] AC6: System excludes expired stock from picking locations

**Technical Notes:**

- Query stock items filtered by product, available quantity, and `canBePicked() = true`
- Sort by expiration date (earliest first) and location proximity
- Create picking tasks with optimized sequence
- Consider multiple orders per load and multiple orders per customer

**Dependencies:**

- US-6.1.2: Create Picking List
- US-2.1.4: Prevent Picking of Expired Stock
- US-3.2.1: Assign Location to Stock

**Related BRD Requirements:**

- FR-6.2: Load Planning

---

### US-6.2.2: Map Orders to Loads

**Story ID:** US-6.2.2  
**Title:** Map Orders to Loads  
**Epic:** Picking List Management  
**Service:** Picking Service  
**Priority:** Must Have  
**Story Points:** 5

**As a** warehouse operator  
**I want** orders to be correctly mapped to loads  
**So that** I can track which orders belong to which load

**Acceptance Criteria:**

- [ ] AC1: System supports multiple orders per load
- [ ] AC2: System supports multiple orders per customer per load
- [ ] AC3: System maintains order-to-load relationships
- [ ] AC4: System tracks order status within load
- [ ] AC5: System allows querying orders by load

**Technical Notes:**

- `Load` aggregate contains list of `Order` entities
- `Order` entity references `CustomerId`
- Maintain relationships in database
- Support queries: orders by load, loads by customer

**Dependencies:**

- US-6.1.2: Create Picking List

**Related BRD Requirements:**

- FR-6.3: Order-to-Load Mapping

---

### US-6.3.1: Execute Picking Task

**Story ID:** US-6.3.1  
**Title:** Execute Picking Task  
**Epic:** Picking List Management  
**Service:** Picking Service  
**Priority:** Must Have  
**Story Points:** 8

**As a** warehouse operator  
**I want** to execute picking tasks  
**So that** I can pick stock according to the picking list

**Acceptance Criteria:**

- [ ] AC1: System guides picking operations based on optimized location assignments
- [ ] AC2: System validates picked quantities against picking list
- [ ] AC3: System updates stock levels in real-time during picking
- [ ] AC4: System supports partial picking scenarios
- [ ] AC5: System publishes `PickingTaskCompletedEvent` or `PartialPickingCompletedEvent`
- [ ] AC6: System records picking timestamp and user

**Technical Notes:**

- API endpoint: `POST /picking-tasks/{taskId}/complete`
- Validate picked quantity against task quantity
- Update stock levels via domain event
- Create stock movement from storage to picking location
- Handle partial picking with reason

**Dependencies:**

- US-6.2.1: Plan Picking Locations
- US-3.3.1: Track Stock Movement

**Related BRD Requirements:**

- FR-6.4: Picking Execution

---

### US-6.3.2: Complete Picking

**Story ID:** US-6.3.2  
**Title:** Complete Picking  
**Epic:** Picking List Management  
**Service:** Picking Service  
**Priority:** Must Have  
**Story Points:** 5

**As a** warehouse operator  
**I want** to mark picking as complete  
**So that** I can proceed to shipping

**Acceptance Criteria:**

- [ ] AC1: System allows completing picking when all tasks are completed
- [ ] AC2: System validates all picking tasks are completed or partial
- [ ] AC3: System updates picking list status to "COMPLETED"
- [ ] AC4: System publishes `PickingCompletedEvent`
- [ ] AC5: System updates order status within load

**Technical Notes:**

- Check all picking tasks are completed
- Update picking list and load status
- Publish domain event for downstream processing (returns, shipping)

**Dependencies:**

- US-6.3.1: Execute Picking Task

**Related BRD Requirements:**

- FR-6.4: Picking Execution

---

## Epic 7: Returns Management

**Epic ID:** EPIC-7  
**BRD Reference:** FR-7  
**Service:** Returns Service  
**Description:** Handle returns processing including partial acceptance, full returns, and damage-in-transit.

### US-7.1.1: Handle Partial Order Acceptance

**Story ID:** US-7.1.1  
**Title:** Handle Partial Order Acceptance  
**Epic:** Returns Management  
**Service:** Returns Service  
**Priority:** Must Have  
**Story Points:** 8

**As a** warehouse operator  
**I want** to handle partial order acceptance  
**So that** I can process returns when customers accept only part of their order

**Acceptance Criteria:**

- [ ] AC1: System records accepted quantities per order line
- [ ] AC2: System identifies returned quantities
- [ ] AC3: System updates order status accordingly
- [ ] AC4: System initiates returns process for unaccepted items
- [ ] AC5: System publishes `ReturnInitiatedEvent` for partial returns

**Technical Notes:**

- Create `Return` aggregate for partial returns
- Record accepted vs returned quantities per line item
- Update order status to PARTIAL_ACCEPTED
- Trigger returns processing workflow

**Dependencies:**

- US-6.3.2: Complete Picking

**Related BRD Requirements:**

- FR-7.1: Partial Order Acceptance

---

### US-7.2.1: Process Full Order Return

**Story ID:** US-7.2.1  
**Title:** Process Full Order Return  
**Epic:** Returns Management  
**Service:** Returns Service  
**Priority:** Must Have  
**Story Points:** 5

**As a** warehouse operator  
**I want** to process full order returns  
**So that** I can handle customer returns efficiently

**Acceptance Criteria:**

- [ ] AC1: System records return reason
- [ ] AC2: System validates returned products
- [ ] AC3: System checks product condition
- [ ] AC4: System updates stock levels
- [ ] AC5: System publishes `ReturnProcessedEvent`

**Technical Notes:**

- Create `Return` aggregate with return type FULL
- Validate returned products against order
- Record return reason and condition
- Update stock levels via domain event

**Dependencies:**

- US-6.3.2: Complete Picking

**Related BRD Requirements:**

- FR-7.2: Full Order Returns

---

### US-7.3.1: Handle Damage-in-Transit Returns

**Story ID:** US-7.3.1  
**Title:** Handle Damage-in-Transit Returns  
**Epic:** Returns Management  
**Service:** Returns Service  
**Priority:** Must Have  
**Story Points:** 5

**As a** warehouse operator  
**I want** to handle damage-in-transit returns  
**So that** I can properly classify and handle damaged stock

**Acceptance Criteria:**

- [ ] AC1: System records damage type and extent
- [ ] AC2: System classifies damage (repairable, write-off)
- [ ] AC3: System handles damaged stock appropriately
- [ ] AC4: System generates damage reports
- [ ] AC5: System publishes `DamageRecordedEvent`

**Technical Notes:**

- Extend `Return` aggregate with damage information
- Classify damage type (CRUSHED, BROKEN, EXPIRED, etc.)
- Determine if stock is repairable or write-off
- Generate damage report for management

**Dependencies:**

- US-7.2.1: Process Full Order Return

**Related BRD Requirements:**

- FR-7.3: Damage-in-Transit Returns

---

### US-7.4.1: Assign Return Location

**Story ID:** US-7.4.1  
**Title:** Assign Return Location  
**Epic:** Returns Management  
**Service:** Returns Service, Location Management Service  
**Priority:** Must Have  
**Story Points:** 5

**As a** warehouse operator  
**I want** returned stock to be assigned locations  
**So that** returned stock can be re-picked for another order

**Acceptance Criteria:**

- [ ] AC1: System assigns return location based on product condition and expiration date
- [ ] AC2: System updates stock availability for re-picking
- [ ] AC3: System maintains return history
- [ ] AC4: System prioritizes returned stock in picking if appropriate
- [ ] AC5: System publishes `ReturnLocationAssignedEvent`

**Technical Notes:**

- Query Location Service for available locations
- Consider product condition (damaged stock may go to quarantine)
- Consider expiration date (FEFO principles)
- Update stock levels and location status

**Dependencies:**

- US-7.2.1: Process Full Order Return
- US-3.2.1: Assign Location to Stock

**Related BRD Requirements:**

- FR-7.4: Stock Return to Warehouse Floor

---

### US-7.5.1: Reconcile Returns with D365

**Story ID:** US-7.5.1  
**Title:** Reconcile Returns with D365  
**Epic:** Returns Management  
**Service:** Returns Service, Integration Service  
**Priority:** Must Have  
**Story Points:** 5

**As a** warehouse operator  
**I want** returns to be reconciled with D365  
**So that** D365 has accurate return information

**Acceptance Criteria:**

- [ ] AC1: System reconciles returns and updates D365
- [ ] AC2: Reconciliation includes: returned quantities, condition, location assignment, reason codes
- [ ] AC3: System handles reconciliation errors and retries
- [ ] AC4: System maintains reconciliation audit trail
- [ ] AC5: System publishes `ReturnReconciledEvent`

**Technical Notes:**

- Integration Service consumes `ReturnReconciledEvent`
- Send returns data to D365 via OData API
- Handle retry logic for failures
- Maintain reconciliation status

**Dependencies:**

- US-7.4.1: Assign Return Location
- US-9.1.3: Send Returns Data to D365

**Related BRD Requirements:**

- FR-7.5: Returns Reconciliation

---

## Epic 8: Reconciliation

**Epic ID:** EPIC-8  
**BRD Reference:** FR-8  
**Service:** Reconciliation Service  
**Description:** Manage daily stock counts, variance identification, and reconciliation with D365.

### US-8.1.1: Generate Electronic Stock Count Worksheet

**Story ID:** US-8.1.1  
**Title:** Generate Electronic Stock Count Worksheet  
**Epic:** Reconciliation  
**Service:** Reconciliation Service  
**Priority:** Must Have  
**Story Points:** 8

**As a** warehouse operator  
**I want** to generate electronic stock count worksheets  
**So that** I can perform stock counts without paper transcription

**Acceptance Criteria:**

- [ ] AC1: System generates electronic stock count worksheets (not paper-based)
- [ ] AC2: System supports cycle counting (counting subsets of locations daily)
- [ ] AC3: System supports full physical inventory counts
- [ ] AC4: System provides digital workflow for counting
- [ ] AC5: System saves count progress automatically
- [ ] AC6: System allows resuming incomplete stock counts

**Technical Notes:**

- Create `StockCount` aggregate with `StockCountWorksheet`
- Generate worksheet entries for locations and products
- Store worksheet in database (not paper)
- Support offline counting with sync when online

**Dependencies:**

- None (foundational)

**Related BRD Requirements:**

- FR-8.1: Daily Stock Count

---

### US-8.1.2: Perform Stock Count Entry

**Story ID:** US-8.1.2  
**Title:** Perform Stock Count Entry  
**Epic:** Reconciliation  
**Service:** Reconciliation Service  
**Priority:** Must Have  
**Story Points:** 8

**As a** warehouse operator  
**I want** to perform stock count entries using barcode scanning  
**So that** I can count stock efficiently and accurately

**Acceptance Criteria:**

- [ ] AC1: System allows scanning location barcodes to identify location being counted
- [ ] AC2: System allows scanning product barcodes to identify products at that location
- [ ] AC3: System allows entering product quantities directly into electronic worksheet
- [ ] AC4: System validates scanned barcodes against master data
- [ ] AC5: System prevents duplicate counting of same location/product combination
- [ ] AC6: System allows manual count entry for products without barcodes
- [ ] AC7: System supports barcode scanning using handheld scanners and mobile device cameras
- [ ] AC8: System saves count progress automatically

**Technical Notes:**

- Frontend: Stock count worksheet component with barcode scanning
- API endpoint: `POST /stock-counts/{stockCountId}/entries`
- Validate location and product barcodes
- Prevent duplicate entries for same location/product
- Support offline counting with IndexedDB storage

**Dependencies:**

- US-8.1.1: Generate Electronic Stock Count Worksheet
- US-3.1.2: Scan Location Barcode
- US-4.1.3: Scan Product Barcode

**Related BRD Requirements:**

- FR-8.1: Daily Stock Count

---

### US-8.1.3: Complete Stock Count

**Story ID:** US-8.1.3  
**Title:** Complete Stock Count  
**Epic:** Reconciliation  
**Service:** Reconciliation Service  
**Priority:** Must Have  
**Story Points:** 5

**As a** warehouse operator  
**I want** to complete stock counts  
**So that** I can identify variances and reconcile stock

**Acceptance Criteria:**

- [ ] AC1: System allows completing stock count when all entries are made
- [ ] AC2: System calculates variances between counted and system stock
- [ ] AC3: System flags significant variances for review
- [ ] AC4: System updates stock count status to "COMPLETED"
- [ ] AC5: System publishes `StockCountCompletedEvent` and `StockCountVarianceIdentifiedEvent`

**Technical Notes:**

- Calculate variances: countedQuantity - systemQuantity
- Flag variances exceeding threshold (e.g., >5% or >10 units)
- Create `StockCountVariance` aggregates for each variance
- Publish domain events for downstream processing

**Dependencies:**

- US-8.1.2: Perform Stock Count Entry

**Related BRD Requirements:**

- FR-8.1: Daily Stock Count
- FR-8.2: Stock Count Variance Identification

---

### US-8.2.1: Investigate Stock Count Variances

**Story ID:** US-8.2.1  
**Title:** Investigate Stock Count Variances  
**Epic:** Reconciliation  
**Service:** Reconciliation Service  
**Priority:** Must Have  
**Story Points:** 5

**As a** warehouse manager  
**I want** to investigate stock count variances  
**So that** I can identify root causes and take corrective action

**Acceptance Criteria:**

- [ ] AC1: System supports variance investigation workflow
- [ ] AC2: System allows recording variance reasons
- [ ] AC3: System generates variance reports
- [ ] AC4: System tracks variance investigation status
- [ ] AC5: System publishes `VarianceInvestigatedEvent`

**Technical Notes:**

- Update `StockCountVariance` aggregate with investigation details
- Record variance reason (DAMAGE, THEFT, COUNTING_ERROR, etc.)
- Generate variance report for management
- Support workflow: IDENTIFIED → INVESTIGATING → RESOLVED

**Dependencies:**

- US-8.1.3: Complete Stock Count

**Related BRD Requirements:**

- FR-8.2: Stock Count Variance Identification

---

### US-8.3.1: Reconcile Stock Counts with D365

**Story ID:** US-8.3.1  
**Title:** Reconcile Stock Counts with D365  
**Epic:** Reconciliation  
**Service:** Reconciliation Service, Integration Service  
**Priority:** Must Have  
**Story Points:** 8

**As a** warehouse manager  
**I want** stock counts to be reconciled with D365  
**So that** D365 has accurate inventory information

**Acceptance Criteria:**

- [ ] AC1: System reconciles stock counts with D365
- [ ] AC2: Reconciliation includes: product codes, quantities, locations, variance reasons
- [ ] AC3: System sends reconciliation data to D365
- [ ] AC4: System handles reconciliation approval workflow if required
- [ ] AC5: System updates local stock levels based on reconciliation
- [ ] AC6: System publishes `ReconciliationCompletedEvent`
- [ ] AC7: System automatically updates D365 within 1 hour of reconciliation completion

**Technical Notes:**

- Create `Reconciliation` aggregate
- Integration Service consumes `ReconciliationCompletedEvent`
- Send reconciliation data to D365 via OData API
- Handle approval workflow if required by D365
- Update local stock levels after D365 confirmation

**Dependencies:**

- US-8.2.1: Investigate Stock Count Variances
- US-9.1.4: Send Reconciliation Data to D365

**Related BRD Requirements:**

- FR-8.3: Reconciliation with D365
- FR-8.4: D365 Updates

---

## Epic 9: Integration Requirements

**Epic ID:** EPIC-9  
**BRD Reference:** FR-9  
**Service:** Integration Service  
**Description:** Provide bidirectional integration with D365 Finance and Operations.

### US-9.1.1: D365 Integration Setup

**Story ID:** US-9.1.1  
**Title:** D365 Integration Setup  
**Epic:** Integration Requirements  
**Service:** Integration Service  
**Priority:** Must Have  
**Story Points:** 13

**As a** system administrator  
**I want** to set up D365 integration  
**So that** the system can communicate with D365

**Acceptance Criteria:**

- [ ] AC1: System authenticates securely with D365 using OAuth 2.0 / Azure AD
- [ ] AC2: System handles D365 API rate limits
- [ ] AC3: System supports both push and pull data exchange patterns
- [ ] AC4: System implements retry logic with exponential backoff
- [ ] AC5: System maintains integration error queue
- [ ] AC6: System logs all D365 communication

**Technical Notes:**

- Configure OAuth 2.0 / Azure AD authentication
- Implement D365 API client with rate limiting
- Set up webhook endpoints for push notifications
- Implement polling for pull patterns
- Create error queue for failed operations

**Dependencies:**

- None (foundational)

**Related BRD Requirements:**

- FR-9.1: D365 Integration

---

### US-9.1.2: Send Consignment Confirmation to D365

**Story ID:** US-9.1.2  
**Title:** Send Consignment Confirmation to D365  
**Epic:** Integration Requirements  
**Service:** Integration Service  
**Priority:** Must Have  
**Story Points:** 5

**As a** system  
**I want** to send consignment confirmations to D365  
**So that** D365 knows stock has been received

**Acceptance Criteria:**

- [ ] AC1: System sends consignment confirmation to D365 within 2 minutes of confirmation
- [ ] AC2: Confirmation includes: received quantities, received date/time, location assignments
- [ ] AC3: System handles D365 API errors with retry logic
- [ ] AC4: System tracks confirmation status
- [ ] AC5: System handles partial receipts

**Technical Notes:**

- Consume `StockConsignmentConfirmedEvent` from Kafka
- Transform to D365 OData API format
- Send via D365 API client
- Handle retries and errors

**Dependencies:**

- US-9.1.1: D365 Integration Setup
- US-1.1.4: Confirm Consignment Receipt

**Related BRD Requirements:**

- FR-9.2: Data Synchronization

---

### US-9.1.3: Send Returns Data to D365

**Story ID:** US-9.1.3  
**Title:** Send Returns Data to D365  
**Epic:** Integration Requirements  
**Service:** Integration Service  
**Priority:** Must Have  
**Story Points:** 5

**As a** system  
**I want** to send returns data to D365  
**So that** D365 has accurate return information

**Acceptance Criteria:**

- [ ] AC1: System sends returns data to D365 after reconciliation
- [ ] AC2: Returns data includes: returned quantities, condition, location assignment, reason codes
- [ ] AC3: System handles reconciliation errors and retries
- [ ] AC4: System maintains reconciliation audit trail

**Technical Notes:**

- Consume `ReturnReconciledEvent` from Kafka
- Transform to D365 OData API format
- Send via D365 API client
- Handle retries and errors

**Dependencies:**

- US-9.1.1: D365 Integration Setup
- US-7.5.1: Reconcile Returns with D365

**Related BRD Requirements:**

- FR-9.2: Data Synchronization

---

### US-9.1.4: Send Reconciliation Data to D365

**Story ID:** US-9.1.4  
**Title:** Send Reconciliation Data to D365  
**Epic:** Integration Requirements  
**Service:** Integration Service  
**Priority:** Must Have  
**Story Points:** 5

**As a** system  
**I want** to send reconciliation data to D365  
**So that** D365 has accurate inventory counts

**Acceptance Criteria:**

- [ ] AC1: System sends reconciliation data to D365 within 1 hour of reconciliation completion
- [ ] AC2: Reconciliation data includes: product codes, quantities, locations, variance reasons
- [ ] AC3: System handles update failures and retries
- [ ] AC4: System maintains update status tracking
- [ ] AC5: System generates update confirmation reports

**Technical Notes:**

- Consume `ReconciliationCompletedEvent` from Kafka
- Transform to D365 OData API format
- Send via D365 API client
- Handle retries and errors
- Track update status

**Dependencies:**

- US-9.1.1: D365 Integration Setup
- US-8.3.1: Reconcile Stock Counts with D365

**Related BRD Requirements:**

- FR-9.2: Data Synchronization
- FR-9.3: Error Handling

---

### US-9.1.5: Send Restock Request to D365

**Story ID:** US-9.1.5  
**Title:** Send Restock Request to D365  
**Epic:** Integration Requirements  
**Service:** Integration Service  
**Priority:** Must Have  
**Story Points:** 3

**As a** system  
**I want** to send restock requests to D365  
**So that** D365 can process restock orders

**Acceptance Criteria:**

- [ ] AC1: System sends restock request to D365 when stock falls below minimum
- [ ] AC2: Restock request includes: product code, current quantity, minimum quantity, requested quantity, priority
- [ ] AC3: System handles D365 API errors with retry logic
- [ ] AC4: System tracks restock request status

**Technical Notes:**

- Consume `RestockRequestGeneratedEvent` from Kafka
- Transform to D365 OData API format
- Send via D365 API client
- Handle retries and errors

**Dependencies:**

- US-9.1.1: D365 Integration Setup
- US-5.1.3: Generate Restock Request

**Related BRD Requirements:**

- FR-9.2: Data Synchronization

---

## Non-Functional Requirements

### NFR-1: Performance

#### US-NFR-1.1: Response Time Requirements

**Story ID:** US-NFR-1.1  
**Title:** Meet Response Time Requirements  
**Epic:** Non-Functional Requirements  
**Service:** All Services  
**Priority:** Must Have  
**Story Points:** 8

**As a** warehouse operator  
**I want** the system to respond quickly  
**So that** I can work efficiently

**Acceptance Criteria:**

- [ ] AC1: Stock receipt processing: < 2 seconds
- [ ] AC2: Picking list ingestion: < 5 seconds
- [ ] AC3: Barcode scanning response: < 1 second
- [ ] AC4: Stock level queries: < 1 second
- [ ] AC5: System supports 10,000 stock movements per day per warehouse
- [ ] AC6: System supports 50+ concurrent users per warehouse

**Technical Notes:**

- Optimize database queries with indexes
- Implement caching for frequently accessed data
- Use read models for queries
- Load testing required

**Related BRD Requirements:**

- NFR-1: Performance

---

### NFR-2: Reliability and Availability

#### US-NFR-2.1: System Availability

**Story ID:** US-NFR-2.1  
**Title:** Ensure System Availability  
**Epic:** Non-Functional Requirements  
**Service:** All Services  
**Priority:** Must Have  
**Story Points:** 8

**As a** warehouse operator  
**I want** the system to be available when I need it  
**So that** I can perform my work

**Acceptance Criteria:**

- [ ] AC1: System available 99.5% of the time during business hours (6 AM - 10 PM, 7 days a week)
- [ ] AC2: Planned maintenance windows outside business hours
- [ ] AC3: Unplanned downtime < 4 hours per month
- [ ] AC4: Disaster recovery: RTO < 4 hours, RPO < 1 hour
- [ ] AC5: Database backups: Daily full backups, hourly incremental

**Technical Notes:**

- Implement health checks and monitoring
- Set up automated backups
- Implement disaster recovery procedures
- Load balancing and high availability

**Related BRD Requirements:**

- NFR-2: Reliability and Availability

---

### NFR-3: Security

#### US-NFR-3.1: Authentication and Authorization

**Story ID:** US-NFR-3.1  
**Title:** Implement Authentication and Authorization  
**Epic:** Non-Functional Requirements  
**Service:** All Services  
**Priority:** Must Have  
**Story Points:** 8

**As a** system administrator  
**I want** the system to have robust security  
**So that** only authorized users can access the system

**Acceptance Criteria:**

- [ ] AC1: Integration with Active Directory or similar
- [ ] AC2: Role-based access control (RBAC)
- [ ] AC3: Multi-factor authentication support
- [ ] AC4: Session management and timeout
- [ ] AC5: Encryption at rest and in transit (TLS 1.2+)

**Technical Notes:**

- OAuth 2.0 / JWT authentication
- Spring Security for authorization
- RBAC implementation
- TLS/SSL configuration

**Related BRD Requirements:**

- NFR-3: Security

---

### NFR-4: Usability

#### US-NFR-4.1: PWA Implementation

**Story ID:** US-NFR-4.1  
**Title:** Implement Progressive Web App  
**Epic:** Non-Functional Requirements  
**Service:** Frontend  
**Priority:** Must Have  
**Story Points:** 13

**As a** warehouse operator  
**I want** the system to work as a Progressive Web App  
**So that** I can use it on mobile devices like a native app

**Acceptance Criteria:**

- [ ] AC1: PWA supports installation on mobile devices (home screen icon, app-like experience)
- [ ] AC2: PWA works offline for critical operations
- [ ] AC3: PWA automatically synchronizes data when online
- [ ] AC4: PWA provides visual indicators for offline/online status
- [ ] AC5: PWA supports barcode scanning using mobile device camera

**Technical Notes:**

- Service worker implementation
- IndexedDB for offline storage
- Background sync for data synchronization
- Web app manifest

**Related BRD Requirements:**

- NFR-4.1: User Interface
- NFR-4.3: Offline Support and Data Synchronization

---

#### US-NFR-4.2: Accessibility Compliance

**Story ID:** US-NFR-4.2  
**Title:** Ensure WCAG 2.1 Level AA Compliance  
**Epic:** Non-Functional Requirements  
**Service:** Frontend  
**Priority:** Must Have  
**Story Points:** 8

**As a** user with disabilities  
**I want** the system to be accessible  
**So that** I can use it effectively

**Acceptance Criteria:**

- [ ] AC1: System complies with WCAG 2.1 Level AA standards
- [ ] AC2: System supports keyboard navigation
- [ ] AC3: System supports screen readers
- [ ] AC4: System has adequate color contrast
- [ ] AC5: System provides text alternatives for images

**Technical Notes:**

- ARIA attributes
- Semantic HTML
- Keyboard navigation support
- Screen reader testing
- Color contrast validation

**Related BRD Requirements:**

- NFR-4.1: User Interface

---

#### US-NFR-4.3: Multi-Language Support

**Story ID:** US-NFR-4.3  
**Title:** Implement Multi-Language Support  
**Epic:** Non-Functional Requirements  
**Service:** Frontend  
**Priority:** Must Have  
**Story Points:** 5

**As a** warehouse operator  
**I want** the system to support multiple languages  
**So that** I can use it in my preferred language

**Acceptance Criteria:**

- [ ] AC1: System supports English, Afrikaans, and Zulu
- [ ] AC2: System allows switching languages
- [ ] AC3: System stores language preference
- [ ] AC4: All UI text is translatable

**Technical Notes:**

- react-i18next for internationalization
- Translation files for each language
- Language switcher component
- Store preference in localStorage

**Related BRD Requirements:**

- NFR-4.1: User Interface

---

## Summary

### Story Count by Epic

- **Epic 1:** Stock Consignment Management - 5 stories
- **Epic 2:** Stock Classification and Expiration Management - 4 stories
- **Epic 3:** Warehouse Location Management - 6 stories
- **Epic 4:** Product Identification - 3 stories
- **Epic 5:** Stock Level Management - 3 stories
- **Epic 6:** Picking List Management - 6 stories
- **Epic 7:** Returns Management - 5 stories
- **Epic 8:** Reconciliation - 5 stories
- **Epic 9:** Integration Requirements - 5 stories
- **Non-Functional Requirements:** 6 stories

**Total:** 52 user stories

### Priority Distribution

- **Must Have:** 48 stories
- **Should Have:** 1 story
- **Could Have:** 0 stories
- **Won't Have:** 0 stories (MVP scope)

### Estimated Story Points

**Total Story Points:** ~350 points (estimated)

---

**Document Control**

- **Version History:** This document will be version controlled with change tracking
- **Review Cycle:** This document will be reviewed weekly during planning phase
- **Distribution:** This document will be distributed to all project team members

