# User Story Tracking

## Warehouse Management System Integration - CCBSA LDP System

**Document Version:** 2.0  
**Date:** 2025-11  
**Status:** Draft  
**Related Documents:**

- [User Story Breakdown](User_Story_Breakdown.md)
- [Project Roadmap](project-roadmap.md)

---

## Overview

This document tracks the progress and implementation status of all user stories defined in the User Story Breakdown document.

### Status Definitions

- **Not Started** - Story not yet started
- **In Progress** - Story currently being worked on
- **In Review** - Story completed, awaiting review
- **Done** - Story completed and approved
- **Blocked** - Story blocked by dependency or issue
- **Deferred** - Story deferred to future sprint/release

### Story Point Totals

| Status      | Story Count | Story Points |
|-------------|-------------|--------------|
| Not Started | 58          | ~380         |
| In Progress | 0           | 0            |
| In Review   | 0           | 0            |
| Done        | 0           | 0            |
| Blocked     | 0           | 0            |
| Deferred    | 0           | 0            |
| **Total**   | **58**      | **~380**     |

---

## Epic 1: Stock Consignment Management

| Story ID | Title                                | Priority    | Points | Status      | Assigned To | Sprint | Notes |
|----------|--------------------------------------|-------------|--------|-------------|-------------|--------|-------|
| US-1.1.1 | Upload Consignment Data via CSV File | Must Have   | 8      | Not Started | -           | -      | -     |
| US-1.1.2 | Manual Consignment Data Entry via UI | Must Have   | 8      | Not Started | -           | -      | -     |
| US-1.1.3 | Ingest Consignment Data from D365    | Should Have | 8      | Not Started | -           | -      | -     |
| US-1.1.4 | Validate Consignment Data            | Must Have   | 5      | Not Started | -           | -      | -     |
| US-1.1.5 | Create Stock Consignment             | Must Have   | 8      | Not Started | -           | -      | -     |
| US-1.1.6 | Confirm Consignment Receipt          | Must Have   | 5      | Not Started | -           | -      | -     |
| US-1.1.7 | Handle Partial Consignment Receipt   | Should Have | 5      | Not Started | -           | -      | -     |

**Epic Total:** 7 stories, 47 points

---

## Epic 2: Stock Classification and Expiration Management

| Story ID | Title                                      | Priority  | Points | Status      | Assigned To | Sprint | Notes |
|----------|--------------------------------------------|-----------|--------|-------------|-------------|--------|-------|
| US-2.1.1 | Classify Stock by Expiration Date          | Must Have | 5      | Not Started | -           | -      | -     |
| US-2.1.2 | Assign Locations Based on FEFO Principles  | Must Have | 8      | Not Started | -           | -      | -     |
| US-2.1.3 | Track Expiration Dates and Generate Alerts | Must Have | 5      | Not Started | -           | -      | -     |
| US-2.1.4 | Prevent Picking of Expired Stock           | Must Have | 3      | Not Started | -           | -      | -     |

**Epic Total:** 4 stories, 21 points

---

## Epic 3: Warehouse Location Management

| Story ID | Title                                  | Priority  | Points | Status      | Assigned To | Sprint | Notes |
|----------|----------------------------------------|-----------|--------|-------------|-------------|--------|-------|
| US-3.1.1 | Create Warehouse Location with Barcode | Must Have | 5      | Not Started | -           | -      | -     |
| US-3.1.2 | Scan Location Barcode                  | Must Have | 5      | Not Started | -           | -      | -     |
| US-3.2.1 | Assign Location to Stock               | Must Have | 8      | Not Started | -           | -      | -     |
| US-3.3.1 | Track Stock Movement                   | Must Have | 8      | Not Started | -           | -      | -     |
| US-3.3.2 | Initiate Stock Movement                | Must Have | 5      | Not Started | -           | -      | -     |
| US-3.4.1 | Manage Location Status                 | Must Have | 5      | Not Started | -           | -      | -     |

**Epic Total:** 6 stories, 36 points

---

## Epic 4: Product Identification

| Story ID | Title                                     | Priority    | Points | Status      | Assigned To | Sprint | Notes |
|----------|-------------------------------------------|-------------|--------|-------------|-------------|--------|-------|
| US-4.1.1 | Upload Product Master Data via CSV File   | Must Have   | 8      | Not Started | -           | -      | -     |
| US-4.1.2 | Manual Product Master Data Entry via UI   | Must Have   | 8      | Not Started | -           | -      | -     |
| US-4.1.3 | Synchronize Product Master Data from D365 | Should Have | 8      | Not Started | -           | -      | -     |
| US-4.1.4 | Validate Product Barcode                  | Must Have   | 5      | Not Started | -           | -      | -     |
| US-4.1.5 | Scan Product Barcode                      | Must Have   | 5      | Not Started | -           | -      | -     |

**Epic Total:** 5 stories, 34 points

---

## Epic 5: Stock Level Management

| Story ID | Title                                    | Priority  | Points | Status      | Assigned To | Sprint | Notes |
|----------|------------------------------------------|-----------|--------|-------------|-------------|--------|-------|
| US-5.1.1 | Monitor Stock Levels                     | Must Have | 5      | Not Started | -           | -      | -     |
| US-5.1.2 | Enforce Minimum and Maximum Stock Levels | Must Have | 5      | Not Started | -           | -      | -     |
| US-5.1.3 | Generate Restock Request                 | Must Have | 5      | Not Started | -           | -      | -     |

**Epic Total:** 3 stories, 15 points

---

## Epic 6: Picking List Management

| Story ID | Title                            | Priority    | Points | Status      | Assigned To | Sprint | Notes |
|----------|----------------------------------|-------------|--------|-------------|-------------|--------|-------|
| US-6.1.1 | Upload Picking List via CSV File | Must Have   | 8      | Not Started | -           | -      | -     |
| US-6.1.2 | Manual Picking List Entry via UI | Must Have   | 8      | Not Started | -           | -      | -     |
| US-6.1.3 | Ingest Picking List from D365    | Should Have | 8      | Not Started | -           | -      | -     |
| US-6.1.4 | Create Picking List              | Must Have   | 5      | Not Started | -           | -      | -     |
| US-6.2.1 | Plan Picking Locations           | Must Have   | 13     | Not Started | -           | -      | -     |
| US-6.2.2 | Map Orders to Loads              | Must Have   | 5      | Not Started | -           | -      | -     |
| US-6.3.1 | Execute Picking Task             | Must Have   | 8      | Not Started | -           | -      | -     |
| US-6.3.2 | Complete Picking                 | Must Have   | 5      | Not Started | -           | -      | -     |

**Epic Total:** 8 stories, 60 points

---

## Epic 7: Returns Management

| Story ID | Title                            | Priority    | Points | Status      | Assigned To | Sprint | Notes |
|----------|----------------------------------|-------------|--------|-------------|-------------|--------|-------|
| US-7.1.1 | Handle Partial Order Acceptance  | Must Have   | 8      | Not Started | -           | -      | -     |
| US-7.2.1 | Process Full Order Return        | Must Have   | 5      | Not Started | -           | -      | -     |
| US-7.3.1 | Handle Damage-in-Transit Returns | Must Have   | 5      | Not Started | -           | -      | -     |
| US-7.4.1 | Assign Return Location           | Must Have   | 5      | Not Started | -           | -      | -     |
| US-7.5.1 | Reconcile Returns with D365      | Should Have | 5      | Not Started | -           | -      | -     |

**Epic Total:** 5 stories, 28 points

---

## Epic 8: Reconciliation

| Story ID | Title                                     | Priority    | Points | Status      | Assigned To | Sprint | Notes |
|----------|-------------------------------------------|-------------|--------|-------------|-------------|--------|-------|
| US-8.1.1 | Generate Electronic Stock Count Worksheet | Must Have   | 8      | Not Started | -           | -      | -     |
| US-8.1.2 | Perform Stock Count Entry                 | Must Have   | 8      | Not Started | -           | -      | -     |
| US-8.1.3 | Complete Stock Count                      | Must Have   | 5      | Not Started | -           | -      | -     |
| US-8.2.1 | Investigate Stock Count Variances         | Must Have   | 5      | Not Started | -           | -      | -     |
| US-8.3.1 | Reconcile Stock Counts with D365          | Should Have | 8      | Not Started | -           | -      | -     |

**Epic Total:** 5 stories, 34 points

---

## Epic 9: Integration Requirements

| Story ID | Title                                 | Priority    | Points | Status      | Assigned To | Sprint | Notes |
|----------|---------------------------------------|-------------|--------|-------------|-------------|--------|-------|
| US-9.1.1 | D365 Integration Setup                | Should Have | 13     | Not Started | -           | -      | -     |
| US-9.1.2 | Send Consignment Confirmation to D365 | Should Have | 5      | Not Started | -           | -      | -     |
| US-9.1.3 | Send Returns Data to D365             | Should Have | 5      | Not Started | -           | -      | -     |
| US-9.1.4 | Send Reconciliation Data to D365      | Should Have | 5      | Not Started | -           | -      | -     |
| US-9.1.5 | Send Restock Request to D365          | Should Have | 3      | Not Started | -           | -      | -     |

**Epic Total:** 5 stories, 31 points

---

## Non-Functional Requirements

| Story ID   | Title                            | Priority  | Points | Status      | Assigned To | Sprint | Notes |
|------------|----------------------------------|-----------|--------|-------------|-------------|--------|-------|
| US-NFR-1.1 | Response Time Requirements       | Must Have | 8      | Not Started | -           | -      | -     |
| US-NFR-2.1 | System Availability              | Must Have | 8      | Not Started | -           | -      | -     |
| US-NFR-3.1 | Authentication and Authorization | Must Have | 8      | Not Started | -           | -      | -     |
| US-NFR-4.1 | PWA Implementation               | Must Have | 13     | Not Started | -           | -      | -     |
| US-NFR-4.2 | Accessibility Compliance         | Must Have | 8      | Not Started | -           | -      | -     |
| US-NFR-4.3 | Multi-Language Support           | Must Have | 5      | Not Started | -           | -      | -     |

**NFR Total:** 6 stories, 50 points

---

## Sprint Planning

### Sprint 1: Foundation (Planned)

**Sprint Goal:** Establish foundational infrastructure and core domain models

**Stories:**

- US-3.1.1: Create Warehouse Location with Barcode (5)
- US-4.1.1: Upload Product Master Data via CSV File (8)
- US-4.1.2: Manual Product Master Data Entry via UI (8)
- US-NFR-3.1: Authentication and Authorization (8)

**Total Points:** 29

---

### Sprint 2: Stock Receipt (Planned)

**Sprint Goal:** Enable stock consignment receipt and validation

**Stories:**

- US-1.1.1: Upload Consignment Data via CSV File (8)
- US-1.1.2: Manual Consignment Data Entry via UI (8)
- US-1.1.4: Validate Consignment Data (5)
- US-1.1.5: Create Stock Consignment (8)
- US-4.1.4: Validate Product Barcode (5)

**Total Points:** 34

---

### Sprint 3: Stock Classification (Planned)

**Sprint Goal:** Implement stock classification and FEFO location assignment

**Stories:**

- US-2.1.1: Classify Stock by Expiration Date (5)
- US-2.1.2: Assign Locations Based on FEFO Principles (8)
- US-3.2.1: Assign Location to Stock (8)
- US-1.1.6: Confirm Consignment Receipt (5)

**Total Points:** 26

---

### Sprint 4: Stock Movement and Levels (Planned)

**Sprint Goal:** Track stock movements and monitor stock levels

**Stories:**

- US-3.3.1: Track Stock Movement (8)
- US-3.3.2: Initiate Stock Movement (5)
- US-3.4.1: Manage Location Status (5)
- US-5.1.1: Monitor Stock Levels (5)
- US-5.1.2: Enforce Minimum and Maximum Stock Levels (5)

**Total Points:** 28

---

### Sprint 5: Picking Operations (Planned)

**Sprint Goal:** Enable picking list management and execution

**Stories:**

- US-6.1.1: Upload Picking List via CSV File (8)
- US-6.1.2: Manual Picking List Entry via UI (8)
- US-6.1.4: Create Picking List (5)
- US-6.2.1: Plan Picking Locations (13)
- US-6.2.2: Map Orders to Loads (5)

**Total Points:** 39

---

### Sprint 6: Picking Execution (Planned)

**Sprint Goal:** Complete picking operations and expiration tracking

**Stories:**

- US-6.3.1: Execute Picking Task (8)
- US-6.3.2: Complete Picking (5)
- US-2.1.3: Track Expiration Dates and Generate Alerts (5)
- US-2.1.4: Prevent Picking of Expired Stock (3)
- US-5.1.3: Generate Restock Request (5)

**Total Points:** 26

---

### Sprint 7: Returns Management (Planned)

**Sprint Goal:** Handle returns processing

**Stories:**

- US-7.1.1: Handle Partial Order Acceptance (8)
- US-7.2.1: Process Full Order Return (5)
- US-7.3.1: Handle Damage-in-Transit Returns (5)
- US-7.4.1: Assign Return Location (5)
- US-7.5.1: Reconcile Returns with D365 (5)

**Total Points:** 28

---

### Sprint 8: Reconciliation (Planned)

**Sprint Goal:** Implement stock counting and reconciliation

**Stories:**

- US-8.1.1: Generate Electronic Stock Count Worksheet (8)
- US-8.1.2: Perform Stock Count Entry (8)
- US-8.1.3: Complete Stock Count (5)
- US-8.2.1: Investigate Stock Count Variances (5)
- US-8.3.1: Reconcile Stock Counts with D365 (8)

**Total Points:** 34

---

### Sprint 9: Integration and Frontend (Planned)

**Sprint Goal:** Complete D365 integration and frontend PWA

**Stories:**

- US-9.1.2: Send Consignment Confirmation to D365 (5)
- US-9.1.3: Send Returns Data to D365 (5)
- US-9.1.4: Send Reconciliation Data to D365 (5)
- US-9.1.5: Send Restock Request to D365 (3)
- US-NFR-4.1: PWA Implementation (13)

**Total Points:** 31

---

### Sprint 10: Frontend and Polish (Planned)

**Sprint Goal:** Complete frontend features and non-functional requirements

**Stories:**

- US-3.1.2: Scan Location Barcode (5)
- US-4.1.5: Scan Product Barcode (5)
- US-1.1.7: Handle Partial Consignment Receipt (5)
- US-NFR-4.2: Accessibility Compliance (8)
- US-NFR-4.3: Multi-Language Support (5)
- US-NFR-1.1: Response Time Requirements (8)
- US-NFR-2.1: System Availability (8)

**Total Points:** 44

---

## Progress Tracking

### Velocity Tracking

| Sprint    | Planned Points | Completed Points | Velocity |
|-----------|----------------|------------------|----------|
| Sprint 1  | 29             | 0                | -        |
| Sprint 2  | 34             | 0                | -        |
| Sprint 3  | 26             | 0                | -        |
| Sprint 4  | 28             | 0                | -        |
| Sprint 5  | 39             | 0                | -        |
| Sprint 6  | 26             | 0                | -        |
| Sprint 7  | 28             | 0                | -        |
| Sprint 8  | 34             | 0                | -        |
| Sprint 9  | 31             | 0                | -        |
| Sprint 10 | 44             | 0                | -        |

**Average Velocity:** TBD

---

## Dependencies and Blockers

### Current Blockers

None

### Dependency Chain

1. **Foundation Layer:**
    - US-3.1.1 (Locations) - No dependencies
    - US-4.1.1 (Products CSV Upload) - No dependencies
    - US-4.1.2 (Products Manual Entry) - No dependencies
    - US-4.1.3 (Products D365 Sync) - Depends on US-9.1.1 (D365 Integration, optional)

2. **Stock Receipt Layer:**
    - US-1.1.1 (CSV Upload) - Depends on US-4.1.4 (Product Barcode Validation)
    - US-1.1.2 (Manual Entry) - Depends on US-4.1.4 (Product Barcode Validation)
    - US-1.1.3 (D365 Ingest) - Depends on US-9.1.1 (D365 Integration, optional)
    - US-1.1.4 (Validate) - Depends on US-1.1.1, US-1.1.2, US-4.1.4
    - US-1.1.5 (Create) - Depends on US-1.1.4, US-3.2.1

3. **Stock Classification Layer:**
    - US-2.1.1 (Classify) - Depends on US-1.1.3
    - US-2.1.2 (FEFO Assignment) - Depends on US-3.2.1, US-2.1.1
    - US-3.2.1 (Assign Location) - Depends on US-3.1.1, US-2.1.2

---

## Notes

- This tracking document will be updated weekly during sprint planning and daily during sprint execution
- Story status will be updated as work progresses
- Dependencies and blockers will be tracked and resolved promptly
- Velocity will be calculated after first sprint completion

---

**Document Control**

- **Version History:** This document will be version controlled with change tracking
- **Update Frequency:** Daily during sprints, weekly during planning
- **Distribution:** This document will be distributed to all project team members

