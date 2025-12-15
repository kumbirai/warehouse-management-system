# D365 Data Entity Mapping Guide

## Warehouse Management System Integration - CCBSA LDP System

**Document Version:** 1.0  
**Date:** 2025-11  
**Status:** Draft  
**Related Documents:**

- [CSV Format Specification](CSV_Format_Specification.md)
- [API Specifications](API_Specifications.md)
- [Service Architecture Document](../01-architecture/Service_Architecture_Document.md)

---

## Table of Contents

1. [Overview](#overview)
2. [D365 Data Entities Overview](#d365-data-entities-overview)
3. [Product Master Data Mapping](#product-master-data-mapping)
4. [Stock Consignment Mapping](#stock-consignment-mapping)
5. [Picking List Mapping](#picking-list-mapping)
6. [Bidirectional Data Flow](#bidirectional-data-flow)
7. [OData API Integration](#odata-api-integration)
8. [Data Transformation Rules](#data-transformation-rules)
9. [Error Handling and Retry Logic](#error-handling-and-retry-logic)

---

## Overview

### Purpose

This document provides detailed mapping between the Warehouse Management System's data structures and Microsoft Dynamics 365 Finance and Operations (D365) data entities. The
mappings enable:

1. **Seamless API Integration**: Direct OData API calls to D365
2. **CSV Import Compatibility**: CSV files align with D365 entity structures
3. **Bidirectional Sync**: Data flows both ways between systems
4. **Minimal Rework**: CSV formats designed for D365 compatibility from the start

### Integration Architecture

```
┌─────────────────┐         CSV Files          ┌──────────────────┐
│  CSV Upload     │─────────────────────────────>│  WMS System      │
│  (Manual/EDI)   │                              │  (Current)       │
└─────────────────┘                              └──────────────────┘
                                                          │
                                                          │ OData API
                                                          │ (Future)
                                                          ▼
┌─────────────────┐                              ┌──────────────────┐
│  D365 F&O       │<─────────────────────────────│  Integration     │
│  (ERP System)   │         OData API            │  Service         │
└─────────────────┘                              └──────────────────┘
```

---

## D365 Data Entities Overview

### Key Data Entities

D365 Finance and Operations uses data entities to expose business data via OData APIs. Key entities for this integration:

1. **Product Entities**:
    - `EcoResProductV2Entity` - Shared product definitions
    - `EcoResReleasedProductV2Entity` - Released products (available for transactions)

2. **Inventory Entities**:
    - `InventTransferOrderEntity` - Inventory transfer order headers
    - `InventTransferOrderLineEntity` - Inventory transfer order lines
    - `InventOnHandEntity` - On-hand inventory quantities

3. **Warehouse Management Entities**:
    - `WHSLoadEntity` - Warehouse load headers
    - `WHSLoadLineEntity` - Warehouse load lines
    - `WHSWorkTableEntity` - Warehouse work transactions

4. **Sales Order Entities**:
    - `SalesOrderHeaderV2Entity` - Sales order headers
    - `SalesOrderLineV2Entity` - Sales order lines

### Entity Access

- **OData Endpoint**: `https://<environment>.cloud.dynamics.com/data/<EntityName>`
- **Authentication**: OAuth 2.0 / Azure AD
- **Protocol**: OData 4.0
- **Format**: JSON

---

## Product Master Data Mapping

### CSV to D365 Entity Mapping

**D365 Entity**: `EcoResReleasedProductV2Entity`

| CSV Column           | D365 Field           | Field Type    | Required | Validation           | Notes                       |
|----------------------|----------------------|---------------|----------|----------------------|-----------------------------|
| `ProductCode`        | `ProductNumber`      | String(20)    | Yes      | Alphanumeric, unique | Primary key                 |
| `ProductName`        | `ProductName`        | String(100)   | Yes      | Non-empty            | Display name                |
| `ProductDescription` | `ProductDescription` | String(1000)  | No       | Max 1000 chars       | Detailed description        |
| `PrimaryBarcode`     | `GTIN`               | String(50)    | Yes      | Valid barcode format | Global Trade Item Number    |
| `SecondaryBarcode`   | `GTIN` (secondary)   | String(50)    | No       | Valid barcode format | Secondary barcode           |
| `UnitOfMeasure`      | `UnitSymbol`         | String(10)    | Yes      | Must exist in D365   | Unit of measure code        |
| `ProductType`        | `ProductType`        | Enum          | No       | ITEM, SERVICE, etc.  | Product type                |
| `IsPerishable`       | -                    | Boolean       | No       | true/false           | Custom field (if available) |
| `DefaultExpiryDays`  | -                    | Integer       | No       | Positive integer     | Custom field (if available) |
| `Category`           | `ProductCategory`    | String(50)    | No       | Max 50 chars         | Product category            |
| `Brand`              | `BrandName`          | String(50)    | No       | Max 50 chars         | Brand name                  |
| `Weight`             | `NetWeight`          | Decimal(16,4) | No       | Positive decimal     | Weight in kg                |
| `Volume`             | `Volume`             | Decimal(16,4) | No       | Positive decimal     | Volume in liters            |
| `IsActive`           | `IsActive`           | Boolean       | No       | true/false           | Active status               |

### D365 OData API Example

**Endpoint**: `POST /data/EcoResReleasedProductV2Entity`

**Request Payload**:

```json
{
  "ProductNumber": "PROD-001",
  "ProductName": "Coca Cola 500ml",
  "ProductDescription": "Coca Cola Soft Drink 500ml",
  "GTIN": "6001067101234",
  "UnitSymbol": "BOTTLE",
  "ProductType": "Item",
  "ProductCategory": "Beverages",
  "BrandName": "Coca Cola",
  "NetWeight": 0.5,
  "Volume": 0.5,
  "IsActive": true
}
```

**Response**: `201 Created` with entity data

### Data Transformation Rules

1. **ProductCode → ProductNumber**:
    - Direct mapping, no transformation
    - Must be unique across all products

2. **PrimaryBarcode → GTIN**:
    - Direct mapping
    - Must be valid barcode format (EAN-13, Code 128, etc.)

3. **UnitOfMeasure → UnitSymbol**:
    - Must exist in D365 unit of measure master data
    - Validate before sending to D365

4. **Boolean Values**:
    - CSV: `true`/`false` or `1`/`0`
    - D365: `true`/`false` (JSON boolean)

---

## Stock Consignment Mapping

### CSV to D365 Entity Mapping

**D365 Entity**: `InventTransferOrderLineEntity` (for line items)

| CSV Column             | D365 Field            | Field Type    | Required | Validation          | Notes                    |
|------------------------|-----------------------|---------------|----------|---------------------|--------------------------|
| `ConsignmentReference` | `TransferOrderNumber` | String(20)    | Yes      | Unique per transfer | Transfer order reference |
| `ProductCode`          | `ItemNumber`          | String(20)    | Yes      | Must exist in D365  | Product identifier       |
| `Quantity`             | `Qty`                 | Decimal(16,2) | Yes      | Positive decimal    | Quantity received        |
| `ExpirationDate`       | `ExpirationDate`      | Date          | No       | Future date         | Expiration date          |
| `BatchNumber`          | `BatchNumber`         | String(20)    | No       | Max 20 chars        | Batch identifier         |
| `ReceivedDate`         | `ReceiptDate`         | DateTime      | Yes      | Valid datetime      | Receipt timestamp        |
| `WarehouseId`          | `WarehouseId`         | String(10)    | Yes      | Must exist in D365  | Warehouse identifier     |
| `SerialNumber`         | `SerialNumber`        | String(100)   | No       | Max 100 chars       | Serial number            |
| `ManufacturingDate`    | `ManufacturingDate`   | Date          | No       | Valid date          | Manufacturing date       |
| `SupplierCode`         | `VendorAccountNumber` | String(20)    | No       | Must exist in D365  | Supplier identifier      |
| `PurchaseOrderNumber`  | `PurchaseOrderNumber` | String(20)    | No       | Max 20 chars        | PO reference             |

**D365 Entity**: `InventTransferOrderEntity` (for header/confirmation)

| CSV Column             | D365 Field            | Field Type | Required | Notes                             |
|------------------------|-----------------------|------------|----------|-----------------------------------|
| `ConsignmentReference` | `TransferOrderNumber` | String(20) | Yes      | Transfer order reference          |
| `ReceivedDate`         | `ReceiptDate`         | DateTime   | Yes      | Confirmation timestamp            |
| -                      | `TransferStatus`      | Enum       | Yes      | Set to "Received" on confirmation |
| `WarehouseId`          | `ToWarehouseId`       | String(10) | Yes      | Destination warehouse             |

### D365 OData API Example

**Create Transfer Order Line**:

```
POST /data/InventTransferOrderLineEntity
```

**Request Payload**:

```json
{
  "TransferOrderNumber": "CONS-2025-001",
  "ItemNumber": "PROD-001",
  "Qty": 100.00,
  "ExpirationDate": "2026-06-30T00:00:00Z",
  "BatchNumber": "BATCH-001",
  "ReceiptDate": "2025-11-15T10:00:00Z",
  "WarehouseId": "WH-001"
}
```

**Confirm Transfer Order**:

```
PATCH /data/InventTransferOrderEntity(TransferOrderNumber='CONS-2025-001')
```

**Request Payload**:

```json
{
  "TransferStatus": "Received",
  "ReceiptDate": "2025-11-15T10:00:00Z"
}
```

### Data Transformation Rules

1. **ConsignmentReference → TransferOrderNumber**:
    - Direct mapping
    - Used to group multiple line items

2. **Quantity → Qty**:
    - Decimal precision: 16,2
    - Must be positive

3. **Date/Time Fields**:
    - CSV: ISO 8601 format (`YYYY-MM-DDTHH:mm:ssZ`)
    - D365: ISO 8601 format (same)
    - Timezone: UTC recommended

4. **WarehouseId Validation**:
    - Must exist in D365 warehouse master data
    - Validate before sending to D365

---

## Picking List Mapping

### CSV to D365 Entity Mapping

**D365 Entity**: `WHSLoadLineEntity` (for load lines)

| CSV Column              | D365 Field          | Field Type    | Required | Validation         | Notes                 |
|-------------------------|---------------------|---------------|----------|--------------------|-----------------------|
| `LoadNumber`            | `LoadId`            | String(20)    | Yes      | Unique per load    | Load identifier       |
| `OrderNumber`           | `SalesOrderNumber`  | String(20)    | Yes      | Must exist in D365 | Sales order reference |
| `OrderLineNumber`       | `LineNumber`        | Integer       | Yes      | Positive integer   | Order line number     |
| `ProductCode`           | `ItemNumber`        | String(20)    | Yes      | Must exist in D365 | Product identifier    |
| `Quantity`              | `Qty`               | Decimal(16,2) | Yes      | Positive decimal   | Quantity to pick      |
| `CustomerCode`          | `CustomerAccount`   | String(20)    | Yes      | Must exist in D365 | Customer identifier   |
| `CustomerName`          | `CustomerName`      | String(100)   | No       | Max 100 chars      | Customer name         |
| `Priority`              | `Priority`          | Enum          | No       | HIGH, MEDIUM, LOW  | Priority level        |
| `RequestedDeliveryDate` | `RequestedShipDate` | Date          | No       | Future date        | Delivery date         |
| `WarehouseId`           | `WarehouseId`       | String(10)    | Yes      | Must exist in D365 | Warehouse identifier  |
| `CustomerAddress`       | `DeliveryAddress`   | String(500)   | No       | Max 500 chars      | Delivery address      |
| `CustomerPhone`         | `ContactPhone`      | String(50)    | No       | Max 50 chars       | Contact phone         |
| `SpecialInstructions`   | `Notes`             | String(500)   | No       | Max 500 chars      | Special instructions  |
| `SalesOrderDate`        | `OrderDate`         | Date          | No       | Valid date         | Order date            |
| `RouteNumber`           | `RouteId`           | String(50)    | No       | Max 50 chars       | Route identifier      |

**D365 Entity**: `WHSLoadEntity` (for load header)

| CSV Column    | D365 Field            | Field Type | Required | Notes                      |
|---------------|-----------------------|------------|----------|----------------------------|
| `LoadNumber`  | `LoadId`              | String(20) | Yes      | Load identifier            |
| `WarehouseId` | `WarehouseId`         | String(10) | Yes      | Warehouse identifier       |
| -             | `LoadStatus`          | Enum       | Yes      | Set to "Open" when created |
| -             | `LoadCreatedDateTime` | DateTime   | Yes      | Auto-set by D365           |

### D365 OData API Example

**Create Load Header**:

```
POST /data/WHSLoadEntity
```

**Request Payload**:

```json
{
  "LoadId": "LOAD-2025-001",
  "WarehouseId": "WH-001",
  "LoadStatus": "Open"
}
```

**Create Load Line**:

```
POST /data/WHSLoadLineEntity
```

**Request Payload**:

```json
{
  "LoadId": "LOAD-2025-001",
  "SalesOrderNumber": "ORD-001",
  "LineNumber": 1,
  "ItemNumber": "PROD-001",
  "Qty": 50.00,
  "CustomerAccount": "CUST-001",
  "CustomerName": "ABC Store",
  "Priority": "High",
  "RequestedShipDate": "2025-11-20T00:00:00Z",
  "WarehouseId": "WH-001"
}
```

### Data Transformation Rules

1. **LoadNumber → LoadId**:
    - Direct mapping
    - Groups multiple orders into single load

2. **Priority Mapping**:
    - CSV: `HIGH`, `MEDIUM`, `LOW` (case-insensitive)
    - D365: `High`, `Medium`, `Low` (case-sensitive enum)
    - Transform to match D365 enum values

3. **OrderNumber → SalesOrderNumber**:
    - Must exist in D365 sales order master data
    - Validate before sending to D365

4. **Quantity → Qty**:
    - Decimal precision: 16,2
    - Must be positive

---

## Bidirectional Data Flow

### Inbound Data Flow (D365 → WMS)

**Consignment Data**:

```
D365 Transfer Order → Integration Service → Kafka Event → Stock Management Service
```

**Picking List Data**:

```
D365 Load → Integration Service → Kafka Event → Picking Service
```

**Product Master Data**:

```
D365 Product → Integration Service → Kafka Event → Product Service
```

### Outbound Data Flow (WMS → D365)

**Consignment Confirmation**:

```
Stock Management Service → Kafka Event → Integration Service → D365 Transfer Order Update
```

**Picking Completion**:

```
Picking Service → Kafka Event → Integration Service → D365 Load Update
```

**Stock Movement**:

```
Location Management Service → Kafka Event → Integration Service → D365 Inventory Update
```

**Returns Data**:

```
Returns Service → Kafka Event → Integration Service → D365 Return Order
```

**Reconciliation Data**:

```
Reconciliation Service → Kafka Event → Integration Service → D365 Inventory Adjustment
```

### Data Synchronization Strategy

1. **Real-Time Events**: Critical transactions (consignment confirmations, picking completions)
2. **Scheduled Sync**: Master data (products, customers) - daily
3. **On-Demand Sync**: Ad-hoc data retrieval when needed
4. **Batch Processing**: Large datasets (initial data load)

---

## OData API Integration

### Authentication

**OAuth 2.0 / Azure AD**:

```
POST https://login.microsoftonline.com/<tenant-id>/oauth2/token

grant_type=client_credentials
&client_id=<client-id>
&client_secret=<client-secret>
&resource=https://<environment>.cloud.dynamics.com
```

**Response**:

```json
{
  "access_token": "eyJ0eXAiOiJKV1QiLCJhbGc...",
  "token_type": "Bearer",
  "expires_in": 3600
}
```

### API Endpoints

**Base URL**: `https://<environment>.cloud.dynamics.com/data`

**Product Entity**:

- `GET /EcoResReleasedProductV2Entity` - List products
- `GET /EcoResReleasedProductV2Entity('<ProductNumber>')` - Get product
- `POST /EcoResReleasedProductV2Entity` - Create product
- `PATCH /EcoResReleasedProductV2Entity('<ProductNumber>')` - Update product

**Transfer Order Entity**:

- `GET /InventTransferOrderEntity` - List transfer orders
- `GET /InventTransferOrderEntity('<TransferOrderNumber>')` - Get transfer order
- `POST /InventTransferOrderLineEntity` - Create transfer order line
- `PATCH /InventTransferOrderEntity('<TransferOrderNumber>')` - Update transfer order

**Load Entity**:

- `GET /WHSLoadEntity` - List loads
- `GET /WHSLoadEntity('<LoadId>')` - Get load
- `POST /WHSLoadEntity` - Create load
- `POST /WHSLoadLineEntity` - Create load line
- `PATCH /WHSLoadEntity('<LoadId>')` - Update load

### Query Parameters

**Filtering**:

```
GET /EcoResReleasedProductV2Entity?$filter=ProductNumber eq 'PROD-001'
```

**Selecting Fields**:

```
GET /EcoResReleasedProductV2Entity?$select=ProductNumber,ProductName,GTIN
```

**Ordering**:

```
GET /EcoResReleasedProductV2Entity?$orderby=ProductName asc
```

**Pagination**:

```
GET /EcoResReleasedProductV2Entity?$top=100&$skip=0
```

---

## Data Transformation Rules

### Date/Time Transformation

**CSV Format**: ISO 8601 (`YYYY-MM-DD` or `YYYY-MM-DDTHH:mm:ssZ`)
**D365 Format**: ISO 8601 (same format)
**Timezone**: UTC recommended

**Example**:

- CSV: `2025-11-15T10:00:00Z`
- D365: `2025-11-15T10:00:00Z` (no transformation needed)

### Number Transformation

**CSV Format**: Decimal point (`.`), no thousands separator
**D365 Format**: Decimal point (`.`), no thousands separator
**Precision**: 16,2 for quantities

**Example**:

- CSV: `100.50`
- D365: `100.50` (no transformation needed)

### Enum Value Transformation

**Priority Mapping**:

- CSV: `HIGH`, `MEDIUM`, `LOW` (case-insensitive)
- D365: `High`, `Medium`, `Low` (case-sensitive)

**Transformation**:

```java
String d365Priority = csvPriority.substring(0, 1).toUpperCase() + 
                       csvPriority.substring(1).toLowerCase();
```

### String Length Validation

**Rule**: Truncate or validate strings to match D365 field lengths

**Example**:

- CSV: `ProductName` max 200 chars
- D365: `ProductName` max 100 chars
- **Action**: Truncate to 100 chars or validate before sending

---

## Error Handling and Retry Logic

### Error Response Format

**D365 OData Error Response**:

```json
{
  "error": {
    "code": "BadRequest",
    "message": "Validation failed",
    "innererror": {
      "message": "ProductNumber 'PROD-999' does not exist",
      "type": "ValidationException"
    }
  }
}
```

### Retry Strategy

**Transient Errors** (HTTP 429, 503, 500):

- Retry with exponential backoff
- Max retries: 3
- Backoff: 1s, 2s, 4s

**Permanent Errors** (HTTP 400, 404):

- No retry
- Log error and send to dead letter queue
- Require manual intervention

### Error Codes Mapping

| HTTP Status | D365 Error Code     | Action               |
|-------------|---------------------|----------------------|
| 200         | Success             | Process response     |
| 201         | Created             | Process response     |
| 400         | BadRequest          | No retry, log error  |
| 401         | Unauthorized        | Refresh token, retry |
| 403         | Forbidden           | No retry, log error  |
| 404         | NotFound            | No retry, log error  |
| 429         | TooManyRequests     | Retry with backoff   |
| 500         | InternalServerError | Retry with backoff   |
| 503         | ServiceUnavailable  | Retry with backoff   |

### Dead Letter Queue

**Failed Operations**:

- Log to dead letter queue
- Include original request, error response, timestamp
- Provide admin interface for retry/resolution

---

## Appendix

### D365 Entity Reference Links

- **Products**: [EcoResReleasedProductV2Entity](https://learn.microsoft.com/en-us/dynamics365/fin-ops-core/dev-itpro/data-entities/data-entities)
- **Transfer Orders**: [InventTransferOrderEntity](https://learn.microsoft.com/en-us/dynamics365/fin-ops-core/dev-itpro/data-entities/data-entities)
- **Warehouse Loads**: [WHSLoadEntity](https://learn.microsoft.com/en-us/dynamics365/fin-ops-core/dev-itpro/data-entities/data-entities)

### OData API Documentation

- **OData Overview**: [OData integration](https://learn.microsoft.com/en-us/dynamics365/fin-ops-core/dev-itpro/data-entities/odata)
- **Authentication**: [OAuth 2.0 setup](https://learn.microsoft.com/en-us/dynamics365/fin-ops-core/dev-itpro/data-entities/odata)

### Support

For questions or issues with D365 integration:

- **Technical Support**: Contact integration team
- **D365 Documentation**: [Microsoft Learn](https://learn.microsoft.com/en-us/dynamics365/fin-ops-core/)

---

**Document Control**

- **Version History**: This document will be version controlled with change tracking
- **Review Cycle**: This document will be reviewed quarterly or when D365 entity changes occur
- **Distribution**: This document will be distributed to all integration team members

