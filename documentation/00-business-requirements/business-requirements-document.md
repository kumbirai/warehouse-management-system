# Business Requirements Document

## Warehouse Management System Integration

### Coca Cola Beverages South Africa - Local Distribution Partner (LDP) System

**Document Version:** 1.0  
**Date:** 2025-11
**Status:** Approved  
**Author:** Business Analysis Team

---

## Table of Contents

1. [Executive Summary](#executive-summary)
2. [Background and Context](#background-and-context)
3. [Problem Statement](#problem-statement)
4. [Objectives](#objectives)
5. [Scope](#scope)
6. [Functional Requirements](#functional-requirements)
7. [Non-Functional Requirements](#non-functional-requirements)
8. [Integration Requirements](#integration-requirements)
9. [Assumptions and Constraints](#assumptions-and-constraints)
10. [Success Criteria](#success-criteria)
11. [Glossary](#glossary)

---

## Executive Summary

Coca Cola Beverages South Africa (CCBSA) utilizes Microsoft Dynamics 365 Finance and Operations (D365) as its core ERP system, which includes comprehensive Warehouse Management
System (WMS) capabilities. However, there exists a critical gap requiring integration between D365 Finance and Operations and third-party services, specifically for Local
Distribution Partners (LDPs) who manage last-mile delivery operations.

This document outlines the business requirements for a Warehouse Management System integration that will bridge this gap, enabling LDPs to effectively manage consigned stock,
optimize warehouse operations, and maintain accurate inventory levels while ensuring seamless reconciliation with D365.

---

## Background and Context

### Business Context

CCBSA operates a distribution model where stock is consigned to Local Distribution Partners (LDPs). These LDPs are responsible for:

- Receiving and managing consigned inventory
- Maintaining stock within mandated minimum and maximum levels
- Executing last-mile delivery to end customers
- Ensuring First Expiring First Out (FEFO) stock rotation
- Performing daily stock counts and reconciliation

### Current State

- D365 provides core WMS functionality but lacks integration with LDP-specific operations
- Manual processes exist for stock classification, location management, and reconciliation
- Limited visibility into real-time stock levels at LDP warehouses
- Challenges in tracking stock movements and expiration dates
- Inefficient picking processes without proper location-based optimization

### Target State

A fully integrated system that:

- Automatically ingests stock consignment data from D365
- Classifies and manages stock by expiration dates
- Tracks all stock movements within the warehouse
- Optimizes picking operations based on FEFO principles
- Manages returns and reconciliation automatically
- Generates restock requests when stock falls below minimum levels

---

## Problem Statement

CCBSA requires seamless integration between D365 Finance and Operations and third-party warehouse management services to support LDP operations. The current system lacks the
capability to:

1. **Automate Consignment Confirmation**: Incoming stock from D365 must be automatically ingested and confirmed, eliminating manual data entry and reducing errors.

2. **Manage Expiration-Based Stock Classification**: Stock must be classified and organized by expiration dates to ensure FEFO (First Expiring First Out) compliance, preventing
   stock obsolescence and waste.

3. **Track Warehouse Locations and Movements**: All stock locations must be barcoded, and every movement on the warehouse floor must be tracked to maintain accurate inventory
   visibility.

4. **Optimize Picking Operations**: The system must ingest picking lists from D365 and intelligently plan picking locations based on expiration dates and stock availability.

5. **Handle Returns Management**: The system must accommodate partial order acceptance, full returns, and damage-in-transit scenarios, with the ability to return stock to the
   warehouse floor for re-picking.

6. **Automate Reconciliation**: Daily stock counts and reconciliation processes must be automated with updates flowing back to D365.

7. **Maintain Stock Levels**: The system must monitor stock levels against minimum and maximum thresholds and automatically generate restock requests when stock falls below minimum
   levels.

---

## Objectives

### Primary Objectives

1. **Integration Excellence**: Establish seamless bidirectional integration between D365 and the LDP warehouse management system.

2. **Inventory Accuracy**: Achieve 99%+ inventory accuracy through automated tracking and reconciliation.

3. **FEFO Compliance**: Ensure 100% compliance with First Expiring First Out stock rotation principles.

4. **Operational Efficiency**: Reduce manual data entry by 90% and improve picking efficiency by 25%.

5. **Real-time Visibility**: Provide real-time visibility into stock levels, locations, and movements.

### Secondary Objectives

1. **Scalability**: Design the system to support multiple LDPs and warehouse locations.

2. **Audit Trail**: Maintain comprehensive audit trails for all stock movements and transactions.

3. **Reporting**: Provide actionable insights through reporting and analytics.

---

## Scope

### In Scope

#### 1. Stock Consignment Management

- Automated ingestion of incoming stock from D365
- Consignment confirmation and acknowledgment
- Stock receipt validation

#### 2. Stock Classification and Organization

- Classification of stock by expiration dates
- FEFO (First Expiring First Out) compliance
- Stock packing and location assignment based on expiration dates

#### 3. Warehouse Location Management

- Barcode-based location identification
- Location assignment and tracking
- Stock movement tracking within warehouse floor
- Location capacity management

#### 4. Product Identification

- Product barcode scanning and validation
- Product master data synchronization with D365

#### 5. Stock Level Management

- Minimum and maximum stock level enforcement
- Real-time stock level monitoring
- Automated restock request generation when stock falls below minimum

#### 6. Picking List Management

- Ingestion of picking lists from D365
- Load planning (loads can contain multiple orders)
- Order-to-load mapping (customers can have multiple orders per load)
- Location-based picking optimization

#### 7. Returns Management

- Partial order acceptance handling
- Full order returns processing
- Damage-in-transit returns
- Stock return to warehouse floor for re-picking
- Returns reconciliation

#### 8. Reconciliation

- Daily stock count execution
- Stock count variance identification
- Reconciliation with D365
- Automatic D365 updates

#### 9. Integration

- D365 Finance and Operations integration (bidirectional)
- Real-time data synchronization
- Error handling and retry mechanisms

### Out of Scope (MVP)

1. **Route Planning**: Advanced route planning and optimization is not required for the Minimum Viable Product (MVP). This may be considered for future phases.

2. **Advanced Analytics**: Complex predictive analytics and machine learning-based optimization are out of scope for MVP.

3. **Mobile Applications**: Native mobile applications are out of scope. However, the system MUST be implemented as a Progressive Web App (PWA) with offline support and data
   synchronization when online.

4. **Multi-warehouse Management**: While the system should be designed to support multiple warehouses, multi-warehouse coordination features are out of scope for MVP.

5. **Third-party Logistics Integration**: Integration with external logistics providers beyond D365 is out of scope.

---

## Functional Requirements

### FR-1: Stock Consignment Management

#### FR-1.1: Incoming Stock Ingestion

- **Requirement**: The system MUST automatically ingest incoming stock consignment data from D365 Finance and Operations.
- **Details**:
    - Stock consignment data includes: product codes, quantities, expiration dates, consignment reference numbers, timestamps
    - Ingestion must occur in near real-time (within 5 minutes of D365 transaction)
    - System must validate data completeness and format before processing

#### FR-1.2: Consignment Confirmation

- **Requirement**: The system MUST confirm receipt of consigned stock and send acknowledgment back to D365.
- **Details**:
    - Confirmation must include: received quantities, received date/time, location assignments
    - System must handle partial receipts
    - Confirmation must be sent within 2 minutes of stock receipt

#### FR-1.3: Stock Receipt Validation

- **Requirement**: The system MUST validate received stock against expected consignment data.
- **Details**:
    - Validate product codes against master data
    - Validate quantities against expected quantities
    - Flag discrepancies for manual review
    - Generate exception reports for mismatches

### FR-2: Stock Classification and Expiration Management

#### FR-2.1: Expiration Date Classification

- **Requirement**: The system MUST classify all incoming stock according to expiration dates.
- **Details**:
    - Classification categories: Near Expiry (within 30 days), Normal, Extended Shelf Life
    - System must automatically assign classification based on expiration date
    - Classification must be visible in all stock views and reports

#### FR-2.2: FEFO Stock Packing

- **Requirement**: Stock MUST be packed in the warehouse such that First Expiring stock is positioned for first picking.
- **Details**:
    - System must assign locations based on expiration date priority
    - Locations closer to picking zones must contain stock with earlier expiration dates
    - System must prevent picking of newer stock before older stock expires
    - Visual indicators must show expiration date priority in location views

#### FR-2.3: Expiration Date Tracking

- **Requirement**: The system MUST track expiration dates for all stock items and generate alerts.
- **Details**:
    - Alert when stock is within 7 days of expiration
    - Alert when stock is within 30 days of expiration
    - Prevent picking of expired stock
    - Generate reports on expiring stock

### FR-3: Warehouse Location Management

#### FR-3.1: Location Barcoding

- **Requirement**: ALL warehouse locations MUST be barcoded and scannable.
- **Details**:
    - Each location must have a unique barcode identifier
    - Barcode format must follow CCBSA standards
    - System must support barcode scanning for location identification
    - Location barcodes must be printable and replaceable

#### FR-3.2: Location Assignment

- **Requirement**: The system MUST assign stock to designated locations based on FEFO principles and location capacity.
- **Details**:
    - System must consider expiration date when assigning locations
    - System must respect location capacity limits
    - System must optimize location assignments to minimize movement
    - System must support manual location override with authorization

#### FR-3.3: Stock Movement Tracking

- **Requirement**: ALL movements of stock on the warehouse floor MUST be tracked.
- **Details**:
    - Track movement from receiving to storage location
    - Track movement from storage location to picking location
    - Track movement between storage locations
    - Track movement from picking location to shipping
    - Each movement must record: timestamp, user, source location, destination location, quantity, reason
    - System must maintain complete audit trail

#### FR-3.4: Location Status Management

- **Requirement**: The system MUST track location status (occupied, available, reserved, blocked).
- **Details**:
    - Real-time location status updates
    - Location capacity tracking
    - Location blocking for maintenance or issues
    - Location availability for new stock assignment

### FR-4: Product Identification

#### FR-4.1: Product Barcode Support

- **Requirement**: ALL stock items MUST have product barcodes for easy identification.
- **Details**:
    - System must support scanning product barcodes for identification
    - Product barcodes must be validated against master data
    - System must handle multiple barcode formats (EAN-13, Code 128, etc.)
    - Barcode scanning must work in all relevant processes (receiving, picking, returns)

#### FR-4.2: Product Master Data Synchronization

- **Requirement**: Product master data MUST be synchronized with D365.
- **Details**:
    - Product codes, descriptions, barcodes, unit of measure
    - Synchronization must occur daily or on-demand
    - System must handle product master data changes
    - System must validate product data before use

### FR-5: Stock Level Management

#### FR-5.1: Minimum and Maximum Level Enforcement

- **Requirement**: The system MUST maintain stock within mandated minimum and maximum levels.
- **Details**:
    - Minimum and maximum levels defined per product
    - Levels may vary by location or warehouse
    - System must prevent stock levels from exceeding maximum
    - System must alert when stock approaches minimum

#### FR-5.2: Stock Level Monitoring

- **Requirement**: The system MUST monitor stock levels in real-time.
- **Details**:
    - Real-time stock level calculations
    - Stock level visibility in dashboards
    - Historical stock level tracking
    - Stock level reports by product, location, warehouse

#### FR-5.3: Restock Request Generation

- **Requirement**: When stock levels fall below minimum, the system MUST automatically generate a restock request.
- **Details**:
    - Restock request must include: product code, current quantity, minimum quantity, requested quantity, priority
    - Request must be sent to D365 for processing
    - System must track restock request status
    - System must prevent duplicate restock requests

### FR-6: Picking List Management

#### FR-6.1: Picking List Ingestion

- **Requirement**: The system MUST ingest picking lists generated by D365 for outgoing loads.
- **Details**:
    - Picking list includes: load number, order numbers, customer information, products, quantities, priorities
    - Ingestion must occur in near real-time
    - System must validate picking list data
    - System must handle picking list updates and cancellations

#### FR-6.2: Load Planning

- **Requirement**: The system MUST plan where to pick stock from based on the picking list.
- **Details**:
    - Load can contain multiple order numbers
    - Customer can have one or more orders in a load
    - System must optimize picking locations based on FEFO principles
    - System must consider location proximity to minimize travel time
    - System must generate picking sequence/route suggestions

#### FR-6.3: Order-to-Load Mapping

- **Requirement**: The system MUST correctly map orders to loads and customers to orders.
- **Details**:
    - Multiple orders per load support
    - Multiple orders per customer per load support
    - System must maintain order-to-load relationships
    - System must track order status within load

#### FR-6.4: Picking Execution

- **Requirement**: The system MUST guide picking operations based on optimized location assignments.
- **Details**:
    - Picking instructions must prioritize FEFO stock
    - System must validate picked quantities against picking list
    - System must update stock levels in real-time during picking
    - System must support partial picking scenarios

### FR-7: Returns Management

#### FR-7.1: Partial Order Acceptance

- **Requirement**: The system MUST handle scenarios where a customer accepts only part of their order.
- **Details**:
    - System must record accepted quantities per order line
    - System must identify returned quantities
    - System must update order status accordingly
    - System must initiate returns process for unaccepted items

#### FR-7.2: Full Order Returns

- **Requirement**: The system MUST handle full order returns.
- **Details**:
    - System must record return reason
    - System must validate returned products
    - System must check product condition
    - System must update stock levels

#### FR-7.3: Damage-in-Transit Returns

- **Requirement**: The system MUST handle returns due to damage in transit.
- **Details**:
    - System must record damage type and extent
    - System must classify damage (repairable, write-off)
    - System must handle damaged stock appropriately
    - System must generate damage reports

#### FR-7.4: Stock Return to Warehouse Floor

- **Requirement**: Returned stock MUST be put back on the warehouse floor to be re-picked for another order.
- **Details**:
    - System must assign return location based on product condition and expiration date
    - System must update stock availability for re-picking
    - System must maintain return history
    - System must prioritize returned stock in picking if appropriate

#### FR-7.5: Returns Reconciliation

- **Requirement**: The system MUST reconcile returns and update D365.
- **Details**:
    - Returns reconciliation must include: returned quantities, condition, location assignment, reason codes
    - Reconciliation data must be sent to D365
    - System must handle reconciliation errors and retries
    - System must maintain reconciliation audit trail

### FR-8: Reconciliation

#### FR-8.1: Daily Stock Count

- **Requirement**: The LDP MUST perform a daily stock count, and the system MUST support this process through electronic stock count worksheets that eliminate manual transcription
  from paper to system.
- **Details**:
    - System must generate **electronic stock count worksheets** (not paper-based)
    - System must support cycle counting (counting subsets of locations daily)
    - System must support full physical inventory counts
    - System must provide a digital workflow where users can:
        - Scan location barcodes to identify the location being counted
        - Scan product barcodes to identify products at that location
        - Enter product quantities directly into the electronic worksheet
        - Complete counts entirely within the system without paper transcription
    - System must validate scanned barcodes against master data (locations and products)
    - System must prevent duplicate counting of the same location/product combination
    - System must allow manual count entry for products without barcodes or when scanning is unavailable
    - System must support barcode scanning using handheld scanners and mobile device cameras
    - System must save count progress automatically to prevent data loss
    - System must allow users to resume incomplete stock counts

#### FR-8.2: Stock Count Variance Identification

- **Requirement**: The system MUST identify variances between counted stock and system stock.
- **Details**:
    - System must calculate variances by product and location
    - System must flag significant variances for review
    - System must generate variance reports
    - System must support variance investigation workflow

#### FR-8.3: Reconciliation with D365

- **Requirement**: The system MUST reconcile stock counts with D365 and update D365 accordingly.
- **Details**:
    - Reconciliation must include: product codes, quantities, locations, variance reasons
    - System must send reconciliation data to D365
    - System must handle reconciliation approval workflow if required
    - System must update local stock levels based on reconciliation

#### FR-8.4: D365 Updates

- **Requirement**: The system MUST automatically update D365 with reconciliation results.
- **Details**:
    - Updates must occur within 1 hour of reconciliation completion
    - System must handle update failures and retries
    - System must maintain update status tracking
    - System must generate update confirmation reports

### FR-9: Integration Requirements

#### FR-9.1: D365 Integration

- **Requirement**: The system MUST integrate bidirectionally with D365 Finance and Operations.
- **Details**:
    - Integration must use D365 standard APIs (OData, REST)
    - System must authenticate securely with D365
    - System must handle D365 API rate limits
    - System must support both push and pull data exchange patterns

#### FR-9.2: Data Synchronization

- **Requirement**: The system MUST synchronize data with D365 in near real-time.
- **Details**:
    - Critical transactions (consignment, picking lists) must sync within 5 minutes
    - Less critical data (master data) can sync daily
    - System must handle synchronization failures gracefully
    - System must maintain data consistency

#### FR-9.3: Error Handling

- **Requirement**: The system MUST handle integration errors robustly.
- **Details**:
    - System must retry failed transactions with exponential backoff
    - System must log all integration errors
    - System must alert operators of persistent failures
    - System must maintain error queue for manual resolution

---

## Non-Functional Requirements

### NFR-1: Performance

#### NFR-1.1: Response Time

- **Requirement**: System response time for critical operations MUST be less than 2 seconds.
- **Details**:
    - Stock receipt processing: < 2 seconds
    - Picking list ingestion: < 5 seconds
    - Barcode scanning response: < 1 second
    - Stock level queries: < 1 second

#### NFR-1.2: Throughput

- **Requirement**: System MUST support processing of 10,000 stock movements per day per warehouse.
- **Details**:
    - Peak load handling: 500 movements per hour
    - Concurrent user support: 50+ users per warehouse
    - Batch processing: Support overnight batch jobs

#### NFR-1.3: Scalability

- **Requirement**: System architecture MUST support scaling to 10+ warehouses and 100+ concurrent users.
- **Details**:
    - Horizontal scaling capability
    - Database performance optimization
    - Caching strategies for frequently accessed data

### NFR-2: Reliability and Availability

#### NFR-2.1: System Availability

- **Requirement**: System MUST be available 99.5% of the time during business hours (6 AM - 10 PM, 7 days a week).
- **Details**:
    - Planned maintenance windows: Outside business hours
    - Unplanned downtime: < 4 hours per month
    - Disaster recovery: RTO < 4 hours, RPO < 1 hour

#### NFR-2.2: Data Integrity

- **Requirement**: System MUST maintain data integrity and prevent data loss.
- **Details**:
    - Transaction atomicity
    - Database backups: Daily full backups, hourly incremental
    - Data validation at all entry points
    - Audit trail for all critical operations

### NFR-3: Security

#### NFR-3.1: Authentication and Authorization

- **Requirement**: System MUST implement robust authentication and authorization.
- **Details**:
    - Integration with Active Directory or similar
    - Role-based access control (RBAC)
    - Multi-factor authentication support
    - Session management and timeout

#### NFR-3.2: Data Security

- **Requirement**: System MUST protect sensitive data.
- **Details**:
    - Encryption at rest and in transit (TLS 1.2+)
    - Secure API communication
    - PII data protection compliance
    - Regular security audits

### NFR-4: Usability

#### NFR-4.1: User Interface

- **Requirement**: System MUST provide an intuitive, user-friendly interface.
- **Details**:
    - Responsive design for desktop and tablet
    - Progressive Web App (PWA) implementation for mobile devices
    - PWA must support installation on mobile devices (home screen icon, app-like experience)
    - Mobile browser compatibility
    - Accessibility compliance (WCAG 2.1 Level AA)
    - Multi-language support (English, Afrikaans, Zulu)

#### NFR-4.2: Barcode Scanning

- **Requirement**: System MUST support efficient barcode scanning workflows.
- **Details**:
    - Support for handheld scanners
    - Mobile device camera scanning
    - Batch scanning capabilities
    - Offline scanning with sync when online

#### NFR-4.3: Offline Support and Data Synchronization

- **Requirement**: The PWA MUST support offline operation with automatic data synchronization when connectivity is restored.
- **Details**:
    - System must function offline for critical operations (stock counting, barcode scanning, data entry)
    - Offline data must be stored locally using browser storage (IndexedDB, LocalStorage)
    - System must automatically detect when connectivity is restored
    - System must automatically synchronize offline data with server when online
    - System must handle synchronization conflicts (last-write-wins or conflict resolution workflow)
    - System must provide visual indicators for offline/online status
    - System must queue failed operations for retry when online
    - System must maintain data integrity during offline-to-online transitions
    - Critical operations (consignment confirmation, reconciliation updates) must be queued and synchronized with D365 when online
    - System must support partial synchronization (sync only changed data)
    - System must handle large data sets efficiently during synchronization

### NFR-5: Maintainability

#### NFR-5.1: Logging and Monitoring

- **Requirement**: System MUST provide comprehensive logging and monitoring.
- **Details**:
    - Application logs for all critical operations
    - Integration logs for D365 communication
    - Performance monitoring and alerting
    - Error tracking and notification

#### NFR-5.2: Documentation

- **Requirement**: System MUST be well-documented.
- **Details**:
    - Technical documentation
    - User manuals and training materials
    - API documentation
    - Operational runbooks

---

## Integration Requirements

### INT-1: D365 Finance and Operations Integration

#### INT-1.1: Inbound Data Flows (D365 → System)

- **Consignment Data**: Stock consignment notifications with product details, quantities, expiration dates
- **Picking Lists**: Outgoing load picking lists with orders, customers, products, quantities
- **Product Master Data**: Product codes, descriptions, barcodes, units of measure
- **Stock Level Thresholds**: Minimum and maximum stock levels per product

#### INT-1.2: Outbound Data Flows (System → D365)

- **Consignment Confirmations**: Receipt confirmations with received quantities and locations
- **Stock Movements**: Real-time stock movement updates
- **Returns Data**: Returns information including quantities, reasons, conditions
- **Reconciliation Data**: Stock count results and variance adjustments
- **Restock Requests**: Automated restock requests when stock falls below minimum

#### INT-1.3: Integration Technology

- **API Protocol**: RESTful APIs using OData protocol
- **Authentication**: OAuth 2.0 / Azure AD authentication
- **Data Format**: JSON
- **Error Handling**: Standard HTTP status codes and error response format
- **Retry Logic**: Exponential backoff for transient failures

#### INT-1.4: Integration Patterns

- **Event-Driven**: Real-time event notifications for critical transactions
- **Scheduled Sync**: Daily synchronization for master data
- **On-Demand**: Ad-hoc data retrieval when needed
- **Batch Processing**: Bulk data transfer for large datasets

---

## Assumptions and Constraints

### Assumptions

1. **D365 Availability**: D365 Finance and Operations will be available and accessible for integration during business hours.

2. **Network Connectivity**: Stable internet connectivity will be available at all warehouse locations.

3. **Barcode Infrastructure**: All products and locations will have barcodes that comply with CCBSA standards.

4. **User Training**: Warehouse staff will be trained on system usage and barcode scanning procedures.

5. **D365 Configuration**: D365 will be configured to generate picking lists and consignment data in the required format.

6. **Hardware Availability**: Warehouse locations will have necessary hardware (scanners, tablets, printers) for system operation.

### Constraints

1. **Route Planning**: Advanced route planning functionality is explicitly excluded from MVP scope.

2. **D365 Customization**: Limited ability to customize D365 Finance and Operations; integration must work with standard D365 APIs.

3. **Budget**: Project must be completed within approved budget constraints.

4. **Timeline**: MVP must be delivered within agreed timeline.

5. **Regulatory Compliance**: System must comply with South African data protection and business regulations.

6. **Legacy Systems**: Integration may need to work alongside existing legacy systems during transition period.

---

## Success Criteria

### Quantitative Success Criteria

1. **Integration Performance**
    - 99%+ successful data synchronization with D365
    - < 5 minute latency for critical transactions
    - < 0.1% data loss or corruption rate

2. **Inventory Accuracy**
    - 99%+ inventory accuracy (measured by stock count variances)
    - < 1% variance rate in daily stock counts

3. **FEFO Compliance**
    - 100% compliance with FEFO picking principles
    - Zero instances of expired stock being shipped

4. **Operational Efficiency**
    - 90% reduction in manual data entry
    - 25% improvement in picking efficiency
    - 50% reduction in reconciliation time

5. **System Availability**
    - 99.5% uptime during business hours
    - < 4 hours unplanned downtime per month

### Qualitative Success Criteria

1. **User Satisfaction**: Positive feedback from warehouse staff and LDP management on system usability and effectiveness.

2. **Business Value**: Demonstrated improvement in stock visibility, reduced waste, and improved customer service levels.

3. **Scalability**: System architecture proven capable of supporting additional warehouses and increased transaction volumes.

4. **Maintainability**: System is maintainable with clear documentation and operational procedures.

---

## Glossary

| Term                | Definition                                                                                                     |
|---------------------|----------------------------------------------------------------------------------------------------------------|
| **CCBSA**           | Coca Cola Beverages South Africa                                                                               |
| **D365**            | Microsoft Dynamics 365 Finance and Operations                                                                  |
| **FEFO**            | First Expiring First Out - stock rotation principle where stock with earliest expiration dates is picked first |
| **LDP**             | Local Distribution Partner - third-party partner responsible for last-mile delivery                            |
| **Consignment**     | Stock that is transferred to LDP but remains owned by CCBSA until sold/delivered                               |
| **Load**            | A collection of orders grouped together for delivery, can contain multiple orders and customers                |
| **Picking List**    | Document generated by D365 specifying which products and quantities to pick for a load                         |
| **Reconciliation**  | Process of comparing physical stock counts with system records and updating D365                               |
| **Stock Count**     | Physical counting of inventory in warehouse locations                                                          |
| **Restock Request** | Automated request generated when stock falls below minimum level                                               |
| **MVP**             | Minimum Viable Product - initial version with core functionality                                               |

---

## Document Approval

| Role                   | Name | Signature | Date |
|------------------------|------|-----------|------|
| Business Sponsor       |      |           |      |
| LDP Operations Manager |      |           |      |
| IT Director            |      |           |      |
| Project Manager        |      |           |      |

---

**Document Control**

- **Version History**: This document will be version controlled with change tracking
- **Review Cycle**: This document will be reviewed quarterly or upon significant business changes
- **Distribution**: This document will be distributed to all stakeholders and project team members

