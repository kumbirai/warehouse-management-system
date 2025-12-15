# CSV Format Specification

## Warehouse Management System Integration - CCBSA LDP System

**Document Version:** 1.0  
**Date:** 2025-11  
**Status:** Draft  
**Related Documents:**

- [API Specifications](API_Specifications.md)
- [User Story Breakdown](../06-project-management/User_Story_Breakdown.md)
- [Service Architecture Document](../01-architecture/Service_Architecture_Document.md)

---

## Table of Contents

1. [Overview](#overview)
2. [General CSV Format Requirements](#general-csv-format-requirements)
3. [Product Master Data CSV Format](#product-master-data-csv-format)
4. [Stock Consignment CSV Format](#stock-consignment-csv-format)
5. [Picking List CSV Format](#picking-list-csv-format)
6. [D365 Data Entity Mapping](#d365-data-entity-mapping)
7. [Validation Rules](#validation-rules)
8. [Error Handling](#error-handling)
9. [Examples](#examples)

---

## Overview

### Purpose

This document defines the CSV file formats for data ingestion into the Warehouse Management System. The formats are designed to:

1. **Support Current System Ingestion**: CSV files can be uploaded directly to the system via REST API endpoints
2. **Enable Future D365 Integration**: CSV formats align with D365 Finance and Operations data entities to facilitate seamless API integration
3. **Ensure Data Consistency**: Standardized formats reduce errors and rework

### CSV File Types

The system supports three types of CSV file uploads:

1. **Product Master Data** - Product information including codes, descriptions, barcodes
2. **Stock Consignment** - Incoming stock consignment data with quantities and expiration dates
3. **Picking List** - Picking lists with orders, customers, and products

### Design Principles

- **D365 Alignment**: Column names align with D365 data entity field names where applicable
- **Dual Compatibility**: Formats work for both direct system ingestion and future D365 OData API integration
- **Validation First**: All data types and formats are validated before processing
- **Error Reporting**: Clear error messages identify specific rows and columns with issues

---

## General CSV Format Requirements

### File Structure

- **Encoding**: UTF-8 with BOM (Byte Order Mark) recommended for Excel compatibility
- **Line Endings**: CRLF (Windows) or LF (Unix) - both supported
- **Delimiter**: Comma (`,`)
- **Text Qualifier**: Double quotes (`"`) for fields containing commas, quotes, or newlines
- **Header Row**: Required - must be the first row
- **Empty Rows**: Ignored during processing
- **Maximum File Size**: 10MB per file

### Column Naming Conventions

- **Format**: PascalCase (e.g., `ProductCode`, `ConsignmentReference`)
- **D365 Alignment**: Column names match D365 entity field names where possible
- **Case Sensitivity**: Column names are case-insensitive during processing
- **Spaces**: Not allowed in column names (use PascalCase instead)

### Data Type Formats

#### Dates

- **Format**: ISO 8601 (`YYYY-MM-DD` or `YYYY-MM-DDTHH:mm:ssZ`)
- **Examples**:
    - `2025-11-15` (date only)
    - `2025-11-15T10:30:00Z` (date with time, UTC)
- **Timezone**: UTC recommended, or local timezone if specified

#### Numbers

- **Format**: Decimal point (`.`), no thousands separator
- **Examples**:
    - `100` (integer)
    - `100.50` (decimal)
- **Negative Numbers**: Use minus sign (`-`) prefix

#### Text

- **Encoding**: UTF-8
- **Special Characters**: Must be properly quoted if containing commas, quotes, or newlines
- **Maximum Length**: Varies by field (see field specifications)

#### Boolean

- **Format**: `true` or `false` (case-insensitive)
- **Alternative**: `1` (true) or `0` (false) also accepted

---

## Product Master Data CSV Format

### File Name Convention

`products_YYYYMMDD_HHMMSS.csv` (e.g., `products_20251115_103000.csv`)

### Required Columns

| Column Name          | Data Type | Required | Max Length | Description                                   | D365 Entity Field    |
|----------------------|-----------|----------|------------|-----------------------------------------------|----------------------|
| `ProductCode`        | String    | Yes      | 50         | Unique product identifier                     | `ProductNumber`      |
| `ProductName`        | String    | Yes      | 200        | Product name/description                      | `ProductName`        |
| `ProductDescription` | String    | No       | 1000       | Detailed product description                  | `ProductDescription` |
| `PrimaryBarcode`     | String    | Yes      | 50         | Primary barcode (EAN-13, Code 128, etc.)      | `GTIN`               |
| `SecondaryBarcode`   | String    | No       | 50         | Secondary barcode if applicable               | `GTIN` (secondary)   |
| `UnitOfMeasure`      | String    | Yes      | 10         | Unit of measure code (e.g., "BOTTLE", "CASE") | `UnitSymbol`         |
| `ProductType`        | String    | No       | 20         | Product type (e.g., "ITEM", "SERVICE")        | `ProductType`        |
| `IsPerishable`       | Boolean   | No       | -          | Whether product has expiration date           | -                    |
| `DefaultExpiryDays`  | Integer   | No       | -          | Default shelf life in days                    | -                    |

### Optional Columns

| Column Name | Data Type | Required | Max Length | Description               | D365 Entity Field |
|-------------|-----------|----------|------------|---------------------------|-------------------|
| `Category`  | String    | No       | 50         | Product category          | `ProductCategory` |
| `Brand`     | String    | No       | 50         | Product brand             | `BrandName`       |
| `Weight`    | Decimal   | No       | -          | Product weight in kg      | `NetWeight`       |
| `Volume`    | Decimal   | No       | -          | Product volume in liters  | `Volume`          |
| `IsActive`  | Boolean   | No       | -          | Whether product is active | `IsActive`        |

### Sample CSV

```csv
ProductCode,ProductName,ProductDescription,PrimaryBarcode,SecondaryBarcode,UnitOfMeasure,ProductType,IsPerishable,DefaultExpiryDays
PROD-001,Coca Cola 500ml,Coca Cola Soft Drink 500ml,6001067101234,,BOTTLE,ITEM,true,365
PROD-002,Sprite 500ml,Sprite Soft Drink 500ml,6001067101235,,BOTTLE,ITEM,true,365
PROD-003,Fanta Orange 500ml,Fanta Orange Soft Drink 500ml,6001067101236,,BOTTLE,ITEM,true,365
```

### Validation Rules

1. `ProductCode` must be unique within the file
2. `PrimaryBarcode` must be unique within the file
3. `UnitOfMeasure` must match valid unit codes in the system
4. `ProductCode` cannot be empty or whitespace
5. `ProductName` cannot be empty or whitespace
6. `PrimaryBarcode` must match valid barcode format (EAN-13, Code 128, etc.)

### D365 Entity Mapping

**Primary Entity**: `EcoResReleasedProductV2Entity` (Released Products V2)

| CSV Column           | D365 Field           | Notes                    |
|----------------------|----------------------|--------------------------|
| `ProductCode`        | `ProductNumber`      | Primary key              |
| `ProductName`        | `ProductName`        | Display name             |
| `ProductDescription` | `ProductDescription` | Detailed description     |
| `PrimaryBarcode`     | `GTIN`               | Global Trade Item Number |
| `UnitOfMeasure`      | `UnitSymbol`         | Unit of measure          |
| `ProductType`        | `ProductType`        | Item type                |
| `IsActive`           | `IsActive`           | Active status            |

---

## Stock Consignment CSV Format

### File Name Convention

`consignments_YYYYMMDD_HHMMSS.csv` (e.g., `consignments_20251115_103000.csv`)

### Required Columns

| Column Name            | Data Type | Required | Max Length | Description                              | D365 Entity Field     |
|------------------------|-----------|----------|------------|------------------------------------------|-----------------------|
| `ConsignmentReference` | String    | Yes      | 50         | Unique consignment reference number      | `TransferOrderNumber` |
| `ProductCode`          | String    | Yes      | 50         | Product code (must exist in master data) | `ItemNumber`          |
| `Quantity`             | Decimal   | Yes      | -          | Quantity received                        | `Qty`                 |
| `ExpirationDate`       | Date      | No       | -          | Product expiration date (ISO 8601)       | `ExpirationDate`      |
| `BatchNumber`          | String    | No       | 50         | Batch or lot number                      | `BatchNumber`         |
| `ReceivedDate`         | DateTime  | Yes      | -          | Date/time when stock was received        | `ReceiptDate`         |
| `ReceivedBy`           | String    | No       | 100        | Name of person who received stock        | -                     |
| `WarehouseId`          | String    | Yes      | 50         | Warehouse identifier                     | `WarehouseId`         |

### Optional Columns

| Column Name           | Data Type | Required | Max Length | Description                   | D365 Entity Field     |
|-----------------------|-----------|----------|------------|-------------------------------|-----------------------|
| `SerialNumber`        | String    | No       | 100        | Serial number if applicable   | `SerialNumber`        |
| `ManufacturingDate`   | Date      | No       | -          | Manufacturing date (ISO 8601) | `ManufacturingDate`   |
| `SupplierCode`        | String    | No       | 50         | Supplier code                 | `VendorAccountNumber` |
| `PurchaseOrderNumber` | String    | No       | 50         | Related purchase order number | `PurchaseOrderNumber` |
| `Notes`               | String    | No       | 500        | Additional notes or comments  | `Notes`               |

### Sample CSV

```csv
ConsignmentReference,ProductCode,Quantity,ExpirationDate,BatchNumber,ReceivedDate,ReceivedBy,WarehouseId
CONS-2025-001,PROD-001,100,2026-06-30,BATCH-001,2025-11-15T10:00:00Z,John Doe,WH-001
CONS-2025-001,PROD-002,150,2026-07-15,BATCH-002,2025-11-15T10:00:00Z,John Doe,WH-001
CONS-2025-002,PROD-003,200,2026-08-01,BATCH-003,2025-11-15T11:30:00Z,Jane Smith,WH-001
```

### Validation Rules

1. `ConsignmentReference` must be unique within the file
2. `ProductCode` must exist in product master data
3. `Quantity` must be positive (> 0)
4. `ExpirationDate` must be in the future (or null for non-perishable items)
5. `ReceivedDate` cannot be in the future
6. `WarehouseId` must be a valid warehouse identifier
7. Multiple rows with same `ConsignmentReference` represent line items for the same consignment

### D365 Entity Mapping

**Primary Entity**: `InventTransferOrderLineEntity` (Inventory Transfer Order Line)

| CSV Column             | D365 Field            | Notes                    |
|------------------------|-----------------------|--------------------------|
| `ConsignmentReference` | `TransferOrderNumber` | Transfer order reference |
| `ProductCode`          | `ItemNumber`          | Product identifier       |
| `Quantity`             | `Qty`                 | Quantity                 |
| `ExpirationDate`       | `ExpirationDate`      | Expiration date          |
| `BatchNumber`          | `BatchNumber`         | Batch identifier         |
| `ReceivedDate`         | `ReceiptDate`         | Receipt timestamp        |
| `WarehouseId`          | `WarehouseId`         | Warehouse identifier     |

**Alternative Entity**: `InventTransferOrderEntity` (Inventory Transfer Order Header)

For consignment confirmations sent back to D365, use:

- `TransferOrderNumber` = `ConsignmentReference`
- `TransferStatus` = "Received"
- `ReceiptDate` = `ReceivedDate`

---

## Picking List CSV Format

### File Name Convention

`picking_lists_YYYYMMDD_HHMMSS.csv` (e.g., `picking_lists_20251115_103000.csv`)

### Required Columns

| Column Name             | Data Type | Required | Max Length | Description                              | D365 Entity Field   |
|-------------------------|-----------|----------|------------|------------------------------------------|---------------------|
| `LoadNumber`            | String    | Yes      | 50         | Unique load identifier                   | `LoadId`            |
| `OrderNumber`           | String    | Yes      | 50         | Sales order number                       | `SalesOrderNumber`  |
| `OrderLineNumber`       | Integer   | Yes      | -          | Line number within the order             | `LineNumber`        |
| `ProductCode`           | String    | Yes      | 50         | Product code (must exist in master data) | `ItemNumber`        |
| `Quantity`              | Decimal   | Yes      | -          | Quantity to pick                         | `Qty`               |
| `CustomerCode`          | String    | Yes      | 50         | Customer identifier                      | `CustomerAccount`   |
| `CustomerName`          | String    | No       | 200        | Customer name                            | `CustomerName`      |
| `Priority`              | String    | No       | 20         | Priority level (HIGH, MEDIUM, LOW)       | `Priority`          |
| `RequestedDeliveryDate` | Date      | No       | -          | Requested delivery date (ISO 8601)       | `RequestedShipDate` |
| `WarehouseId`           | String    | Yes      | 50         | Warehouse identifier                     | `WarehouseId`       |

### Optional Columns

| Column Name           | Data Type | Required | Max Length | Description                              | D365 Entity Field |
|-----------------------|-----------|----------|------------|------------------------------------------|-------------------|
| `CustomerAddress`     | String    | No       | 500        | Customer delivery address                | `DeliveryAddress` |
| `CustomerPhone`       | String    | No       | 50         | Customer contact phone                   | `ContactPhone`    |
| `SpecialInstructions` | String    | No       | 500        | Special picking or delivery instructions | `Notes`           |
| `SalesOrderDate`      | Date      | No       | -          | Original sales order date                | `OrderDate`       |
| `RouteNumber`         | String    | No       | 50         | Delivery route identifier                | `RouteId`         |

### Sample CSV

```csv
LoadNumber,OrderNumber,OrderLineNumber,ProductCode,Quantity,CustomerCode,CustomerName,Priority,RequestedDeliveryDate,WarehouseId
LOAD-2025-001,ORD-001,1,PROD-001,50,CUST-001,ABC Store,HIGH,2025-11-20,WH-001
LOAD-2025-001,ORD-001,2,PROD-002,75,CUST-001,ABC Store,HIGH,2025-11-20,WH-001
LOAD-2025-001,ORD-002,1,PROD-003,100,CUST-002,XYZ Supermarket,MEDIUM,2025-11-21,WH-001
LOAD-2025-002,ORD-003,1,PROD-001,25,CUST-003,Corner Shop,LOW,2025-11-22,WH-001
```

### Validation Rules

1. `LoadNumber` groups multiple orders into a single load
2. `OrderNumber` + `OrderLineNumber` must be unique within a load
3. `ProductCode` must exist in product master data
4. `Quantity` must be positive (> 0)
5. `CustomerCode` must be provided
6. `Priority` must be one of: HIGH, MEDIUM, LOW (case-insensitive)
7. `RequestedDeliveryDate` cannot be in the past
8. Multiple rows with same `LoadNumber` represent different orders/products in the same load

### D365 Entity Mapping

**Primary Entity**: `WHSLoadLineEntity` (Warehouse Load Line)

| CSV Column              | D365 Field          | Notes                 |
|-------------------------|---------------------|-----------------------|
| `LoadNumber`            | `LoadId`            | Load identifier       |
| `OrderNumber`           | `SalesOrderNumber`  | Sales order reference |
| `OrderLineNumber`       | `LineNumber`        | Order line number     |
| `ProductCode`           | `ItemNumber`        | Product identifier    |
| `Quantity`              | `Qty`               | Quantity to pick      |
| `CustomerCode`          | `CustomerAccount`   | Customer identifier   |
| `CustomerName`          | `CustomerName`      | Customer name         |
| `Priority`              | `Priority`          | Priority level        |
| `RequestedDeliveryDate` | `RequestedShipDate` | Delivery date         |
| `WarehouseId`           | `WarehouseId`       | Warehouse identifier  |

**Related Entity**: `WHSLoadEntity` (Warehouse Load Header)

For load-level information:

- `LoadId` = `LoadNumber`
- `LoadStatus` = "Open" (when first created)
- `WarehouseId` = `WarehouseId`

---

## D365 Data Entity Mapping

### Overview

This section provides detailed mapping between CSV columns and D365 Finance and Operations data entities. The mappings enable:

1. **Direct OData API Integration**: CSV data can be transformed to D365 OData entity format
2. **Data Management Framework Import**: CSV files can be imported via D365 Data Management workspace
3. **Bidirectional Sync**: Data can flow both ways between the system and D365

### Product Master Data Mapping

**D365 Entity**: `EcoResReleasedProductV2Entity`

| CSV Column           | D365 Field           | Field Type   | Required | Notes                       |
|----------------------|----------------------|--------------|----------|-----------------------------|
| `ProductCode`        | `ProductNumber`      | String(20)   | Yes      | Primary key, must be unique |
| `ProductName`        | `ProductName`        | String(100)  | Yes      | Display name                |
| `ProductDescription` | `ProductDescription` | String(1000) | No       | Detailed description        |
| `PrimaryBarcode`     | `GTIN`               | String(50)   | Yes      | Global Trade Item Number    |
| `UnitOfMeasure`      | `UnitSymbol`         | String(10)   | Yes      | Must exist in D365          |
| `ProductType`        | `ProductType`        | Enum         | No       | ITEM, SERVICE, etc.         |
| `IsActive`           | `IsActive`           | Boolean      | No       | Active status               |

**D365 OData Endpoint**: `/data/EcoResReleasedProductV2Entity`

**Sample OData Payload**:

```json
{
  "ProductNumber": "PROD-001",
  "ProductName": "Coca Cola 500ml",
  "ProductDescription": "Coca Cola Soft Drink 500ml",
  "GTIN": "6001067101234",
  "UnitSymbol": "BOTTLE",
  "ProductType": "Item",
  "IsActive": true
}
```

### Stock Consignment Mapping

**D365 Entity**: `InventTransferOrderLineEntity`

| CSV Column             | D365 Field            | Field Type    | Required | Notes                    |
|------------------------|-----------------------|---------------|----------|--------------------------|
| `ConsignmentReference` | `TransferOrderNumber` | String(20)    | Yes      | Transfer order reference |
| `ProductCode`          | `ItemNumber`          | String(20)    | Yes      | Must exist in D365       |
| `Quantity`             | `Qty`                 | Decimal(16,2) | Yes      | Must be positive         |
| `ExpirationDate`       | `ExpirationDate`      | Date          | No       | ISO 8601 format          |
| `BatchNumber`          | `BatchNumber`         | String(20)    | No       | Batch identifier         |
| `ReceivedDate`         | `ReceiptDate`         | DateTime      | Yes      | ISO 8601 format          |
| `WarehouseId`          | `WarehouseId`         | String(10)    | Yes      | Must exist in D365       |

**D365 OData Endpoint**: `/data/InventTransferOrderLineEntity`

**Sample OData Payload**:

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

**Consignment Confirmation Entity**: `InventTransferOrderEntity`

When confirming consignment receipt, update the transfer order header:

| CSV Column             | D365 Field            | Field Type | Notes                    |
|------------------------|-----------------------|------------|--------------------------|
| `ConsignmentReference` | `TransferOrderNumber` | String(20) | Transfer order reference |
| `ReceivedDate`         | `ReceiptDate`         | DateTime   | Confirmation timestamp   |
| -                      | `TransferStatus`      | Enum       | Set to "Received"        |

### Picking List Mapping

**D365 Entity**: `WHSLoadLineEntity`

| CSV Column              | D365 Field          | Field Type    | Required | Notes                 |
|-------------------------|---------------------|---------------|----------|-----------------------|
| `LoadNumber`            | `LoadId`            | String(20)    | Yes      | Load identifier       |
| `OrderNumber`           | `SalesOrderNumber`  | String(20)    | Yes      | Sales order reference |
| `OrderLineNumber`       | `LineNumber`        | Integer       | Yes      | Order line number     |
| `ProductCode`           | `ItemNumber`        | String(20)    | Yes      | Must exist in D365    |
| `Quantity`              | `Qty`               | Decimal(16,2) | Yes      | Must be positive      |
| `CustomerCode`          | `CustomerAccount`   | String(20)    | Yes      | Customer identifier   |
| `CustomerName`          | `CustomerName`      | String(100)   | No       | Customer name         |
| `Priority`              | `Priority`          | Enum          | No       | HIGH, MEDIUM, LOW     |
| `RequestedDeliveryDate` | `RequestedShipDate` | Date          | No       | ISO 8601 format       |
| `WarehouseId`           | `WarehouseId`       | String(10)    | Yes      | Must exist in D365    |

**D365 OData Endpoint**: `/data/WHSLoadLineEntity`

**Sample OData Payload**:

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

### Data Transformation Notes

1. **Date/Time Conversion**:
    - CSV dates in ISO 8601 format are directly compatible with D365
    - D365 expects UTC timezone or timezone-aware datetime

2. **Enum Values**:
    - D365 enum values may be case-sensitive
    - Transform CSV values to match D365 enum values exactly

3. **Decimal Precision**:
    - D365 typically uses 16,2 precision for quantities
    - Ensure CSV decimal values don't exceed precision

4. **String Length**:
    - D365 has maximum field lengths
    - Truncate or validate CSV values against D365 limits

---

## Validation Rules

### General Validation

1. **File Format Validation**:
    - File must be valid CSV format
    - Header row must be present
    - All required columns must be present
    - Column names are case-insensitive

2. **Data Type Validation**:
    - Dates must be valid ISO 8601 format
    - Numbers must be valid decimal format
    - Booleans must be `true`/`false` or `1`/`0`
    - Strings must not exceed maximum length

3. **Business Rule Validation**:
    - Product codes must exist in master data (for consignments and picking lists)
    - Quantities must be positive
    - Dates must be in valid ranges (e.g., expiration dates in future)
    - References must be unique where required

### Product Master Data Validation

1. `ProductCode` uniqueness (within file and system)
2. `PrimaryBarcode` uniqueness (within file and system)
3. `UnitOfMeasure` must exist in system
4. `ProductCode` format validation (alphanumeric, no special characters)

### Stock Consignment Validation

1. `ConsignmentReference` uniqueness (within file)
2. `ProductCode` must exist in product master data
3. `Quantity` must be positive
4. `ExpirationDate` must be in future (if provided)
5. `ReceivedDate` cannot be in future
6. `WarehouseId` must be valid warehouse

### Picking List Validation

1. `LoadNumber` groups related orders
2. `OrderNumber` + `OrderLineNumber` uniqueness (within load)
3. `ProductCode` must exist in product master data
4. `Quantity` must be positive
5. `Priority` must be valid enum value (HIGH, MEDIUM, LOW)
6. `RequestedDeliveryDate` cannot be in past
7. `CustomerCode` must be provided

---

## Error Handling

### Error Response Format

When CSV upload fails, the system returns detailed error information:

```json
{
  "error": {
    "code": "CSV_VALIDATION_ERROR",
    "message": "CSV file validation failed",
    "details": {
      "file": "consignments_20251115_103000.csv",
      "totalRows": 100,
      "validRows": 95,
      "invalidRows": 5,
      "errors": [
        {
          "row": 3,
          "column": "ProductCode",
          "message": "Product code 'PROD-999' does not exist in master data",
          "value": "PROD-999"
        },
        {
          "row": 7,
          "column": "Quantity",
          "message": "Quantity must be positive",
          "value": "-10"
        }
      ]
    },
    "timestamp": "2025-11-15T10:30:00Z",
    "path": "/api/v1/stock-management/consignments/upload-csv"
  }
}
```

### Error Codes

| Error Code             | Description                | Resolution                                    |
|------------------------|----------------------------|-----------------------------------------------|
| `CSV_FORMAT_ERROR`     | Invalid CSV file format    | Check file encoding, delimiters, line endings |
| `CSV_MISSING_COLUMN`   | Required column missing    | Add missing column to CSV header              |
| `CSV_VALIDATION_ERROR` | Data validation failed     | Review error details and fix invalid rows     |
| `CSV_DUPLICATE_KEY`    | Duplicate key value        | Ensure unique values for key fields           |
| `CSV_FILE_TOO_LARGE`   | File exceeds maximum size  | Split file into smaller files (max 10MB)      |
| `CSV_EMPTY_FILE`       | File contains no data rows | Ensure file has at least one data row         |

### Partial Processing

- **Valid Rows**: Processed and created in the system
- **Invalid Rows**: Skipped with error details returned
- **Transaction**: Each row processed independently (no rollback on errors)

---

## Examples

### Complete Product Master Data CSV

```csv
ProductCode,ProductName,ProductDescription,PrimaryBarcode,SecondaryBarcode,UnitOfMeasure,ProductType,IsPerishable,DefaultExpiryDays,Category,Brand,IsActive
PROD-001,Coca Cola 500ml,Coca Cola Soft Drink 500ml,6001067101234,,BOTTLE,ITEM,true,365,Beverages,Coca Cola,true
PROD-002,Sprite 500ml,Sprite Soft Drink 500ml,6001067101235,,BOTTLE,ITEM,true,365,Beverages,Sprite,true
PROD-003,Fanta Orange 500ml,Fanta Orange Soft Drink 500ml,6001067101236,,BOTTLE,ITEM,true,365,Beverages,Fanta,true
PROD-004,Coca Cola 2L,Coca Cola Soft Drink 2 Liter,6001067101237,,BOTTLE,ITEM,true,365,Beverages,Coca Cola,true
```

### Complete Stock Consignment CSV

```csv
ConsignmentReference,ProductCode,Quantity,ExpirationDate,BatchNumber,ReceivedDate,ReceivedBy,WarehouseId,SerialNumber,ManufacturingDate,SupplierCode,PurchaseOrderNumber,Notes
CONS-2025-001,PROD-001,100,2026-06-30,BATCH-001,2025-11-15T10:00:00Z,John Doe,WH-001,,2025-11-01,SUP-001,PO-2025-001,Initial consignment
CONS-2025-001,PROD-002,150,2026-07-15,BATCH-002,2025-11-15T10:00:00Z,John Doe,WH-001,,2025-11-01,SUP-001,PO-2025-001,Initial consignment
CONS-2025-002,PROD-003,200,2026-08-01,BATCH-003,2025-11-15T11:30:00Z,Jane Smith,WH-001,,2025-11-05,SUP-002,PO-2025-002,Second consignment
```

### Complete Picking List CSV

```csv
LoadNumber,OrderNumber,OrderLineNumber,ProductCode,Quantity,CustomerCode,CustomerName,Priority,RequestedDeliveryDate,WarehouseId,CustomerAddress,CustomerPhone,SpecialInstructions,SalesOrderDate,RouteNumber
LOAD-2025-001,ORD-001,1,PROD-001,50,CUST-001,ABC Store,HIGH,2025-11-20,WH-001,"123 Main St, Johannesburg",+27123456789,Handle with care,2025-11-15,ROUTE-01
LOAD-2025-001,ORD-001,2,PROD-002,75,CUST-001,ABC Store,HIGH,2025-11-20,WH-001,"123 Main St, Johannesburg",+27123456789,Handle with care,2025-11-15,ROUTE-01
LOAD-2025-001,ORD-002,1,PROD-003,100,CUST-002,XYZ Supermarket,MEDIUM,2025-11-21,WH-001,"456 Oak Ave, Cape Town",+27987654321,Urgent delivery,2025-11-16,ROUTE-02
LOAD-2025-002,ORD-003,1,PROD-001,25,CUST-003,Corner Shop,LOW,2025-11-22,WH-001,"789 Pine Rd, Durban",+27555666777,Standard delivery,2025-11-17,ROUTE-03
```

---

## Appendix

### CSV Template Files

Template CSV files with headers only are available for download:

1. `product_master_data_template.csv`
2. `stock_consignment_template.csv`
3. `picking_list_template.csv`

### D365 Integration Resources

- **D365 Data Entities Documentation**: [Data entities overview](https://learn.microsoft.com/en-us/dynamics365/fin-ops-core/dev-itpro/data-entities/data-entities)
- **OData API Documentation**: [OData integration](https://learn.microsoft.com/en-us/dynamics365/fin-ops-core/dev-itpro/data-entities/odata)
- **Data Management Framework**: [Data Management workspace](https://learn.microsoft.com/en-us/dynamics365/fin-ops-core/dev-itpro/data-entities/data-entities-data-packages)

### Support

For questions or issues with CSV format:

- **Technical Support**: Contact system administrator
- **Documentation Updates**: Submit change requests to documentation team

---

**Document Control**

- **Version History**: This document will be version controlled with change tracking
- **Review Cycle**: This document will be reviewed quarterly or when D365 entity changes occur
- **Distribution**: This document will be distributed to all integration team members and CSV file creators

