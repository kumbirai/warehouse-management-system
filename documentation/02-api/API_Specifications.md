# API Specifications

## Warehouse Management System Integration - CCBSA LDP System

**Document Version:** 1.0  
**Date:** 2025-11  
**Status:** Draft  
**Related Documents:**

- [Business Requirements Document](../00-business-requiremants/business-requirements-document.md)
- [Service Architecture Document](../architecture/Service_Architecture_Document.md)
- [Domain Model Design](../architecture/Domain_Model_Design.md)
- [Project Roadmap](../project-management/project-roadmap.md)
- [CSV Format Specification](CSV_Format_Specification.md)
- [D365 Entity Mapping Guide](D365_Entity_Mapping_Guide.md)

---

## Table of Contents

1. [Overview](#overview)
2. [API Design Principles](#api-design-principles)
3. [API Gateway](#api-gateway)
4. [Authentication and Authorization](#authentication-and-authorization)
5. [Common Patterns](#common-patterns)
6. [Stock Management Service API](#stock-management-service-api)
7. [Location Management Service API](#location-management-service-api)
8. [Product Service API](#product-service-api)
9. [Picking Service API](#picking-service-api)
10. [Returns Service API](#returns-service-api)
11. [Reconciliation Service API](#reconciliation-service-api)
12. [Integration Service API](#integration-service-api)
13. [Tenant Service API](#tenant-service-api)
14. [Error Handling](#error-handling)
15. [API Versioning](#api-versioning)

---

## Overview

### Purpose

This document defines the REST API specifications for all services in the Warehouse Management System Integration. All APIs follow RESTful principles and use OpenAPI 3.0
specification.

### API Base URLs

**Development:**

- API Gateway: `https://api-dev.ccbsa-wms.local`
- Stock Management: `https://stock-management-dev.ccbsa-wms.local`
- Location Management: `https://location-management-dev.ccbsa-wms.local`
- Product Service: `https://product-dev.ccbsa-wms.local`
- Picking Service: `https://picking-dev.ccbsa-wms.local`
- Returns Service: `https://returns-dev.ccbsa-wms.local`
- Reconciliation Service: `https://reconciliation-dev.ccbsa-wms.local`
- Integration Service: `https://integration-dev.ccbsa-wms.local`
- Tenant Service: `https://tenant-service-dev.ccbsa-wms.local`

**Production:**

- API Gateway: `https://api.ccbsa-wms.com`
- Individual services: Internal only (accessed via API Gateway)

### API Documentation

- **Swagger UI:** `https://api.ccbsa-wms.com/swagger-ui.html`
- **OpenAPI Spec:** `https://api.ccbsa-wms.com/v3/api-docs`

---

## API Design Principles

### RESTful Principles

1. **Resource-Based URLs** - URLs represent resources, not actions
2. **HTTP Methods** - Use standard HTTP methods (GET, POST, PUT, PATCH, DELETE)
3. **Stateless** - Each request contains all information needed
4. **JSON** - Use JSON for request/response bodies
5. **HATEOAS** - Include links to related resources (optional)

### Naming Conventions

- **Resources:** Plural nouns (e.g., `/stock-counts`, `/picking-tasks`)
- **Actions:** Use HTTP methods, not verbs in URLs
- **Query Parameters:** camelCase (e.g., `worksheetId`, `status`)
- **Path Parameters:** camelCase (e.g., `/stock-counts/{stockCountId}`)

### HTTP Methods

- **GET** - Retrieve resources (idempotent)
- **POST** - Create resources or perform actions
- **PUT** - Replace entire resource (idempotent)
- **PATCH** - Partial update (idempotent)
- **DELETE** - Delete resource (idempotent)

### Standardized Response Format

All backend services MUST use the standardized `ApiResponse<T>` wrapper to ensure consistent frontend consumption. The `ApiResponse` class is provided in the `common-application`
module.

#### Success Response Format

**Structure:**

```json
{
  "data": { ... },
  "links": { ... },
  "meta": { ... }
}
```

**Fields:**

- `data` (required for success): The response payload containing the actual data
- `links` (optional): HATEOAS links for related resources
- `meta` (optional): Metadata such as pagination information

**Example - Single Resource:**

```json
{
  "data": {
    "id": "cons-123",
    "consignmentReference": "CONS-2025-001",
    "status": "RECEIVED",
    "warehouseId": "wh-123"
  }
}
```

**Example - With Links:**

```json
{
  "data": {
    "id": "cons-123",
    "consignmentReference": "CONS-2025-001"
  },
  "links": {
    "self": "/api/v1/stock-management/consignments/cons-123",
    "confirm": "/api/v1/stock-management/consignments/cons-123/confirm"
  }
}
```

**Example - With Pagination:**

```json
{
  "data": [
    { "id": "cons-123", ... },
    { "id": "cons-124", ... }
  ],
  "meta": {
    "pagination": {
      "page": 1,
      "size": 20,
      "totalElements": 150,
      "totalPages": 8,
      "hasNext": true,
      "hasPrevious": false
    }
  }
}
```

#### Error Response Format

**Structure:**

```json
{
  "error": {
    "code": "ERROR_CODE",
    "message": "Human-readable error message",
    "details": { ... },
    "timestamp": "2025-11-15T10:30:00Z",
    "path": "/api/v1/stock-counts",
    "requestId": "req-123"
  }
}
```

**Fields:**

- `error.code` (required): Machine-readable error code (e.g., `VALIDATION_ERROR`, `RESOURCE_NOT_FOUND`)
- `error.message` (required): Human-readable error message
- `error.details` (optional): Additional error details (validation errors, field-specific messages, etc.)
- `error.timestamp` (required): ISO-8601 timestamp of when the error occurred
- `error.path` (optional): The API path where the error occurred
- `error.requestId` (optional): Request correlation ID for tracing

**Example - Validation Error:**

```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Validation failed",
    "details": {
      "field": "quantity",
      "message": "Quantity must be positive"
    },
    "timestamp": "2025-11-15T10:30:00Z",
    "path": "/api/v1/stock-counts",
    "requestId": "req-123"
  }
}
```

**Example - Resource Not Found:**

```json
{
  "error": {
    "code": "RESOURCE_NOT_FOUND",
    "message": "Tenant not found: ldp-001",
    "timestamp": "2025-11-15T10:30:00Z",
    "path": "/api/v1/tenants/ldp-001",
    "requestId": "req-456"
  }
}
```

#### Implementation Requirements

**Backend Services MUST:**

1. Use `ApiResponse<T>` wrapper for all REST API responses
2. Use `ApiResponseBuilder` utility class for consistent response creation
3. Use `ApiError` class for all error responses
4. Include error details in exception handlers using `ApiError.builder()`
5. Return `ResponseEntity<ApiResponse<T>>` from all controller methods

**Frontend Applications MUST:**

1. Expect `ApiResponse<T>` format for all API responses
2. Check `isError()` or `isSuccess()` methods to determine response type
3. Access data via `response.data` for success responses
4. Access error details via `response.error` for error responses
5. Handle pagination via `response.meta.pagination` when present

**Code Examples:**

**Backend - Success Response:**

```java
@GetMapping("/{id}")
public ResponseEntity<ApiResponse<TenantResponse>> getTenant(@PathVariable String id) {
    TenantResponse data = // ... fetch tenant
    return ApiResponseBuilder.ok(data);
}
```

**Backend - Created Response:**

```java
@PostMapping
public ResponseEntity<ApiResponse<CreateTenantResponse>> createTenant(@RequestBody CreateTenantRequest request) {
    CreateTenantResponse data = // ... create tenant
    return ApiResponseBuilder.created(data);
}
```

**Backend - Error Response:**

```java
@ExceptionHandler(EntityNotFoundException.class)
public ResponseEntity<ApiResponse<Void>> handleNotFound(EntityNotFoundException ex, HttpServletRequest request) {
    ApiError error = ApiError.builder("RESOURCE_NOT_FOUND", ex.getMessage())
        .path(request.getRequestURI())
        .requestId(getRequestId(request))
        .build();
    return ApiResponseBuilder.error(HttpStatus.NOT_FOUND, error);
}
```

**Frontend - TypeScript Example:**

```typescript
interface ApiResponse<T> {
  data?: T;
  error?: ApiError;
  links?: Record<string, string>;
  meta?: ApiMeta;
}

interface ApiError {
  code: string;
  message: string;
  details?: Record<string, any>;
  timestamp: string;
  path?: string;
  requestId?: string;
}

// Usage
const response: ApiResponse<TenantResponse> = await api.get('/tenants/123');
if (response.error) {
  // Handle error
  console.error(response.error.code, response.error.message);
} else {
  // Handle success
  const tenant = response.data;
}
```

---

## API Gateway

### Gateway Routes

All external requests go through the API Gateway, which routes to appropriate services:

```
/api/v1/stock-management/* → Stock Management Service
/api/v1/location-management/* → Location Management Service
/api/v1/products/* → Product Service
/api/v1/picking/* → Picking Service
/api/v1/returns/* → Returns Service
/api/v1/reconciliation/* → Reconciliation Service
/api/v1/integration/* → Integration Service (internal only)
```

### Gateway Features

- **Authentication** - JWT token validation
- **Rate Limiting** - Per tenant/user rate limits
- **Request/Response Transformation** - Standardize formats
- **CORS** - Cross-origin resource sharing
- **Load Balancing** - Distribute requests across service instances

---

## Authentication and Authorization

### Authentication

**OAuth 2.0 / JWT Token:**

All API requests require authentication via Bearer token:

```
Authorization: Bearer <access_token>
```

**Token Endpoint:**

```
POST /oauth/token
Content-Type: application/x-www-form-urlencoded

grant_type=password&username=<username>&password=<password>&client_id=<client_id>
```

**Response:**

```json
{
  "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "token_type": "Bearer",
  "expires_in": 3600,
  "refresh_token": "..."
}
```

### Authorization

**Role-Based Access Control (RBAC):**

Roles:

- `ADMIN` - Full system access
- `MANAGER` - Warehouse management access
- `OPERATOR` - Operational access (picking, counting)
- `VIEWER` - Read-only access

**Permission Headers:**

- `X-User-Id` - User ID (set by gateway)
- `X-Tenant-Id` - Tenant ID (set by gateway)
- `X-Role` - User role (set by gateway)

---

## Common Patterns

### Pagination

**Query Parameters:**

- `page` - Page number (default: 1)
- `size` - Page size (default: 20, max: 100)
- `sort` - Sort field and direction (e.g., `createdAt,desc`)

**Response:**

```json
{
  "data": [ ... ],
  "pagination": {
    "page": 1,
    "size": 20,
    "totalElements": 150,
    "totalPages": 8,
    "hasNext": true,
    "hasPrevious": false
  }
}
```

### Filtering

**Query Parameters:**

- Filter by field: `?status=ACTIVE&warehouseId=wh-123`
- Date ranges: `?createdAtFrom=2025-11-01&createdAtTo=2025-11-30`
- Search: `?search=keyword`

### Field Selection

**Query Parameter:**

- `fields` - Comma-separated list of fields to include (e.g., `?fields=id,name,status`)

### Bulk Operations

**Endpoint Pattern:**

- `POST /resources/bulk` - Create multiple resources
- `PUT /resources/bulk` - Update multiple resources
- `DELETE /resources/bulk` - Delete multiple resources

**Request:**

```json
{
  "items": [
    { "id": "1", "data": { ... } },
    { "id": "2", "data": { ... } }
  ]
}
```

---

## Stock Management Service API

### Base Path

`/api/v1/stock-management`

### Consignments

#### Create Consignment

```
POST /consignments
```

**Request:**

```json
{
  "consignmentReference": "CONS-2025-001",
  "warehouseId": "wh-123",
  "receivedAt": "2025-11-15T10:00:00Z",
  "lineItems": [
    {
      "productId": "prod-123",
      "quantity": 100,
      "expirationDate": "2026-06-30",
      "batchNumber": "BATCH-001"
    }
  ]
}
```

**Response:** `201 Created`

```json
{
  "data": {
    "id": "cons-123",
    "consignmentReference": "CONS-2025-001",
    "status": "RECEIVED",
    "warehouseId": "wh-123",
    "receivedAt": "2025-11-15T10:00:00Z",
    "lineItems": [ ... ]
  }
}
```

#### Get Consignment

```
GET /consignments/{consignmentId}
```

**Response:** `200 OK`

```json
{
  "data": {
    "id": "cons-123",
    "consignmentReference": "CONS-2025-001",
    "status": "CONFIRMED",
    "warehouseId": "wh-123",
    "receivedAt": "2025-11-15T10:00:00Z",
    "confirmedAt": "2025-11-15T10:05:00Z",
    "lineItems": [ ... ]
  }
}
```

#### Confirm Consignment

```
POST /consignments/{consignmentId}/confirm
```

**Response:** `200 OK`

```json
{
  "data": {
    "id": "cons-123",
    "status": "CONFIRMED",
    "confirmedAt": "2025-11-15T10:05:00Z"
  }
}
```

#### List Consignments

```
GET /consignments?status=RECEIVED&warehouseId=wh-123&page=1&size=20
```

#### Upload Consignment Data via CSV

```
POST /consignments/upload-csv
Content-Type: multipart/form-data
```

**Request:**

- `file`: CSV file (multipart/form-data)
- See [CSV Format Specification](CSV_Format_Specification.md) for file format

**Response:** `200 OK`

```json
{
  "data": {
    "uploadId": "upload-123",
    "fileName": "consignments_20251115_103000.csv",
    "totalRows": 100,
    "validRows": 95,
    "invalidRows": 5,
    "status": "COMPLETED",
    "errors": [
      {
        "row": 3,
        "column": "ProductCode",
        "message": "Product code 'PROD-999' does not exist",
        "value": "PROD-999"
      }
    ]
  }
}
```

### Stock Items

#### Get Stock Item

```
GET /stock-items/{stockItemId}
```

#### List Stock Items

```
GET /stock-items?productId=prod-123&locationId=loc-456&classification=NEAR_EXPIRY
```

#### Update Stock Item Classification

```
PATCH /stock-items/{stockItemId}/classification
```

**Request:**

```json
{
  "classification": "NEAR_EXPIRY"
}
```

### Stock Levels

#### Get Stock Level

```
GET /stock-levels/{stockLevelId}
```

#### List Stock Levels

```
GET /stock-levels?productId=prod-123&warehouseId=wh-123
```

#### Update Stock Level

```
PUT /stock-levels/{stockLevelId}
```

**Request:**

```json
{
  "currentQuantity": 150,
  "minimumQuantity": 50,
  "maximumQuantity": 500
}
```

### Restock Requests

#### List Restock Requests

```
GET /restock-requests?status=PENDING&warehouseId=wh-123
```

#### Get Restock Request

```
GET /restock-requests/{restockRequestId}
```

---

## Location Management Service API

### Base Path

`/api/v1/location-management`

### Locations

#### Create Location

```
POST /locations
```

**Request:**

```json
{
  "locationCode": "A-01-02-03",
  "barcode": "LOC-A010203",
  "warehouseId": "wh-123",
  "locationType": "STORAGE",
  "capacity": {
    "maxWeight": 1000,
    "maxVolume": 50
  },
  "coordinates": {
    "zone": "A",
    "aisle": "01",
    "rack": "02",
    "level": "03"
  }
}
```

#### Get Location

```
GET /locations/{locationId}
```

#### List Locations

```
GET /locations?warehouseId=wh-123&status=AVAILABLE&locationType=STORAGE
```

#### Update Location Status

```
PATCH /locations/{locationId}/status
```

**Request:**

```json
{
  "status": "BLOCKED",
  "reason": "Maintenance"
}
```

### Stock Movements

#### Create Stock Movement

```
POST /stock-movements
```

**Request:**

```json
{
  "sourceLocationId": "loc-123",
  "destinationLocationId": "loc-456",
  "productId": "prod-123",
  "quantity": 50,
  "reason": "PICKING",
  "userId": "user-123"
}
```

#### Get Stock Movement

```
GET /stock-movements/{movementId}
```

#### List Stock Movements

```
GET /stock-movements?productId=prod-123&locationId=loc-456&fromDate=2025-11-01
```

#### Complete Stock Movement

```
POST /stock-movements/{movementId}/complete
```

---

## Product Service API

### Base Path

`/api/v1/products`

### Products

#### Get Product

```
GET /products/{productId}
```

**Response:** `200 OK`

```json
{
  "data": {
    "id": "prod-123",
    "productCode": "PROD-001",
    "name": "Coca Cola 500ml",
    "description": "Coca Cola Soft Drink 500ml",
    "unitOfMeasure": "BOTTLE",
    "barcodes": [
      {
        "barcode": "6001067101234",
        "format": "EAN_13",
        "isPrimary": true
      }
    ],
    "createdAt": "2025-11-01T10:00:00Z",
    "updatedAt": "2025-11-15T10:00:00Z"
  }
}
```

#### Get Product by Barcode

```
GET /products/by-barcode/{barcode}
```

#### List Products

```
GET /products?search=coca&page=1&size=20
```

#### Upload Product Master Data via CSV

```
POST /products/upload-csv
Content-Type: multipart/form-data
```

**Request:**

- `file`: CSV file (multipart/form-data)
- See [CSV Format Specification](CSV_Format_Specification.md) for file format

**Response:** `200 OK`

```json
{
  "data": {
    "uploadId": "upload-123",
    "fileName": "products_20251115_103000.csv",
    "totalRows": 50,
    "validRows": 48,
    "invalidRows": 2,
    "status": "COMPLETED",
    "errors": [
      {
        "row": 5,
        "column": "PrimaryBarcode",
        "message": "Barcode '1234567890123' is not a valid format",
        "value": "1234567890123"
      }
    ]
  }
}
```

#### Sync Products from D365

```
POST /products/sync
```

**Response:** `202 Accepted`

```json
{
  "data": {
    "syncId": "sync-123",
    "status": "IN_PROGRESS",
    "startedAt": "2025-11-15T10:00:00Z"
  }
}
```

---

## Picking Service API

### Base Path

`/api/v1/picking`

### Picking Lists

#### Create Picking List

```
POST /picking-lists
```

**Request:**

```json
{
  "pickingListReference": "PL-2025-001",
  "loadNumber": "LOAD-001",
  "warehouseId": "wh-123",
  "orders": [
    {
      "orderNumber": "ORD-001",
      "customerId": "cust-123",
      "customerName": "ABC Store",
      "lineItems": [
        {
          "productId": "prod-123",
          "quantity": 50,
          "priority": "HIGH"
        }
      ]
    }
  ]
}
```

#### Get Picking List

```
GET /picking-lists/{pickingListId}
```

#### List Picking Lists

```
GET /picking-lists?status=PENDING&warehouseId=wh-123
```

#### Upload Picking List via CSV

```
POST /picking-lists/upload-csv
Content-Type: multipart/form-data
```

**Request:**

- `file`: CSV file (multipart/form-data)
- See [CSV Format Specification](CSV_Format_Specification.md) for file format

**Response:** `200 OK`

```json
{
  "data": {
    "uploadId": "upload-123",
    "fileName": "picking_lists_20251115_103000.csv",
    "totalRows": 200,
    "validRows": 195,
    "invalidRows": 5,
    "status": "COMPLETED",
    "errors": [
      {
        "row": 10,
        "column": "ProductCode",
        "message": "Product code 'PROD-999' does not exist",
        "value": "PROD-999"
      }
    ]
  }
}
```

### Loads

#### Get Load

```
GET /loads/{loadId}
```

#### List Loads

```
GET /loads?status=PLANNED&warehouseId=wh-123
```

#### Plan Load

```
POST /loads/{loadId}/plan
```

**Response:** `200 OK`

```json
{
  "data": {
    "loadId": "load-123",
    "status": "PLANNED",
    "pickingTasks": [
      {
        "id": "task-001",
        "locationId": "loc-123",
        "productId": "prod-123",
        "quantity": 50,
        "sequence": 1,
        "expirationDate": "2026-06-30"
      }
    ]
  }
}
```

### Picking Tasks

#### Get Picking Task

```
GET /picking-tasks/{taskId}
```

#### List Picking Tasks

```
GET /picking-tasks?loadId=load-123&status=PENDING
```

#### Start Picking Task

```
POST /picking-tasks/{taskId}/start
```

#### Complete Picking Task

```
POST /picking-tasks/{taskId}/complete
```

**Request:**

```json
{
  "pickedQuantity": 50,
  "locationId": "loc-123",
  "productId": "prod-123"
}
```

#### Partial Complete Picking Task

```
POST /picking-tasks/{taskId}/partial-complete
```

**Request:**

```json
{
  "pickedQuantity": 45,
  "reason": "INSUFFICIENT_STOCK"
}
```

---

## Returns Service API

### Base Path

`/api/v1/returns`

### Returns

#### Create Return

```
POST /returns
```

**Request:**

```json
{
  "orderId": "ord-123",
  "returnType": "PARTIAL",
  "reason": "DAMAGED_IN_TRANSIT",
  "items": [
    {
      "productId": "prod-123",
      "quantity": 10,
      "condition": "DAMAGED",
      "damageType": "CRUSHED"
    }
  ]
}
```

#### Get Return

```
GET /returns/{returnId}
```

#### List Returns

```
GET /returns?orderId=ord-123&status=PROCESSED
```

#### Process Return

```
POST /returns/{returnId}/process
```

**Request:**

```json
{
  "locationId": "loc-123",
  "condition": "DAMAGED",
  "availableForRepicking": false
}
```

#### Reconcile Return

```
POST /returns/{returnId}/reconcile
```

---

## Reconciliation Service API

### Base Path

`/api/v1/reconciliation`

### Stock Counts

#### Create Stock Count

```
POST /stock-counts
```

**Request:**

```json
{
  "countType": "CYCLE",
  "warehouseId": "wh-123",
  "locations": ["loc-123", "loc-456"],
  "scheduledDate": "2025-11-15"
}
```

**Response:** `201 Created`

```json
{
  "data": {
    "id": "count-123",
    "worksheetId": "ws-123",
    "countType": "CYCLE",
    "status": "IN_PROGRESS",
    "warehouseId": "wh-123",
    "createdAt": "2025-11-15T10:00:00Z"
  }
}
```

#### Get Stock Count

```
GET /stock-counts/{stockCountId}
```

#### List Stock Counts

```
GET /stock-counts?status=IN_PROGRESS&warehouseId=wh-123
```

#### Generate Worksheet

```
POST /stock-counts/{stockCountId}/worksheet
```

**Response:** `200 OK`

```json
{
  "data": {
    "worksheetId": "ws-123",
    "entries": [
      {
        "locationId": "loc-123",
        "locationBarcode": "LOC-123",
        "products": [
          {
            "productId": "prod-123",
            "productBarcode": "6001067101234",
            "systemQuantity": 100
          }
        ]
      }
    ]
  }
}
```

### Stock Count Entries

#### Add Stock Count Entry

```
POST /stock-counts/{stockCountId}/entries
```

**Request:**

```json
{
  "locationId": "loc-123",
  "productId": "prod-123",
  "countedQuantity": 95
}
```

#### Get Stock Count Entry

```
GET /stock-counts/{stockCountId}/entries/{entryId}
```

#### List Stock Count Entries

```
GET /stock-counts/{stockCountId}/entries
```

#### Complete Stock Count

```
POST /stock-counts/{stockCountId}/complete
```

**Response:** `200 OK`

```json
{
  "data": {
    "id": "count-123",
    "status": "COMPLETED",
    "completedAt": "2025-11-15T15:00:00Z",
    "variances": [
      {
        "locationId": "loc-123",
        "productId": "prod-123",
        "systemQuantity": 100,
        "countedQuantity": 95,
        "variance": -5
      }
    ]
  }
}
```

### Variances

#### List Variances

```
GET /stock-counts/{stockCountId}/variances?significantOnly=true
```

#### Investigate Variance

```
POST /variances/{varianceId}/investigate
```

**Request:**

```json
{
  "reason": "DAMAGE",
  "notes": "Found damaged stock"
}
```

### Reconciliation

#### Initiate Reconciliation

```
POST /stock-counts/{stockCountId}/reconcile
```

**Response:** `202 Accepted`

```json
{
  "data": {
    "reconciliationId": "recon-123",
    "status": "IN_PROGRESS",
    "initiatedAt": "2025-11-15T16:00:00Z"
  }
}
```

#### Get Reconciliation

```
GET /reconciliations/{reconciliationId}
```

#### List Reconciliations

```
GET /reconciliations?status=COMPLETED&warehouseId=wh-123
```

---

## Integration Service API

### Base Path

`/api/v1/integration`

**Note:** Integration Service API is primarily internal. External access limited to admin operations.

### D365 Integration

#### Sync Products from D365

```
POST /d365/products/sync
```

#### Sync Consignment from D365

```
POST /d365/consignments/sync
```

**Request:**

```json
{
  "consignmentReference": "CONS-2025-001",
  "warehouseId": "wh-123",
  "lineItems": [ ... ]
}
```

#### Send Consignment Confirmation to D365

```
POST /d365/consignments/{consignmentId}/confirm
```

#### Send Stock Movement to D365

```
POST /d365/stock-movements
```

#### Send Return to D365

```
POST /d365/returns
```

#### Send Reconciliation to D365

```
POST /d365/reconciliations
```

#### Send Restock Request to D365

```
POST /d365/restock-requests
```

### Integration Status

#### Get Integration Status

```
GET /d365/status
```

**Response:** `200 OK`

```json
{
  "data": {
    "status": "CONNECTED",
    "lastSyncAt": "2025-11-15T10:00:00Z",
    "pendingOperations": 5,
    "failedOperations": 0
  }
}
```

#### List Failed Operations

```
GET /d365/failed-operations?limit=50
```

#### Retry Failed Operation

```
POST /d365/failed-operations/{operationId}/retry
```

---

## Tenant Service API

### Base Path

**Path:** `/api/v1/tenants`

**Authentication:** Required (JWT token with appropriate role)

### Overview

The Tenant Service manages tenant (LDP) lifecycle, configuration, and metadata. It provides endpoints for tenant creation, activation, deactivation, and configuration management.

### Tenants

#### Create Tenant

**POST** `/api/v1/tenants`

Creates a new tenant (LDP).

**Authorization:** `ADMIN` role required

**Request Body:**

```json
{
  "tenantId": "ldp-001",
  "name": "Local Distribution Partner 001",
  "emailAddress": "contact@ldp001.com",
  "phone": "+27123456789",
  "address": "123 Main Street, Johannesburg",
  "keycloakRealmName": "tenant-ldp-001",
  "usePerTenantRealm": true
}
```

**Response:** `201 Created`

```json
{
  "data": {
    "tenantId": "ldp-001",
    "success": true,
    "message": "Tenant created successfully"
  }
}
```

#### Get Tenant

**GET** `/api/v1/tenants/{id}`

Retrieves tenant information by ID.

**Authorization:** `ADMIN` role or `USER` role with matching tenant

**Response:** `200 OK`

```json
{
  "data": {
    "tenantId": "ldp-001",
    "name": "Local Distribution Partner 001",
    "status": "PENDING",
    "emailAddress": "contact@ldp001.com",
    "phone": "+27123456789",
    "address": "123 Main Street, Johannesburg",
    "keycloakRealmName": "tenant-ldp-001",
    "usePerTenantRealm": true,
    "createdAt": "2025-01-15T10:00:00Z",
    "activatedAt": null,
    "deactivatedAt": null
  }
}
```

#### Activate Tenant

**PUT** `/api/v1/tenants/{id}/activate`

Activates a tenant and creates/enables Keycloak realm if configured.

**Authorization:** `ADMIN` role required

**Response:** `204 No Content`

#### Get Tenant Status

**GET** `/api/v1/tenants/{id}/status`

Retrieves tenant status.

**Authorization:** `ADMIN` role or `USER` role with matching tenant

**Response:** `200 OK`

```json
{
  "data": "ACTIVE"
}
```

#### Get Tenant Realm

**GET** `/api/v1/tenants/{id}/realm`

Retrieves the Keycloak realm name for a tenant. Used by user-service to determine which realm to create users in.

**Authorization:** `ADMIN` or `SERVICE` role required

**Response:** `200 OK`

```json
{
  "tenantId": "ldp-001",
  "realmName": "tenant-ldp-001"
}
```

**Note:** Returns `null` for `realmName` if tenant uses default realm (single realm strategy).

### Tenant Status Values

- `PENDING` - Tenant created but not yet activated
- `ACTIVE` - Tenant is active and operational
- `INACTIVE` - Tenant is deactivated
- `SUSPENDED` - Tenant is temporarily suspended

---

## Error Handling

### HTTP Status Codes

- **200 OK** - Successful GET, PUT, PATCH
- **201 Created** - Successful POST (resource created)
- **202 Accepted** - Request accepted for async processing
- **204 No Content** - Successful DELETE
- **400 Bad Request** - Invalid request data
- **401 Unauthorized** - Authentication required
- **403 Forbidden** - Insufficient permissions
- **404 Not Found** - Resource not found
- **409 Conflict** - Resource conflict (e.g., duplicate)
- **422 Unprocessable Entity** - Validation errors
- **429 Too Many Requests** - Rate limit exceeded
- **500 Internal Server Error** - Server error
- **503 Service Unavailable** - Service temporarily unavailable

### Error Response Format

All error responses follow the standardized `ApiResponse` format with an `error` object. See [Standardized Response Format](#standardized-response-format) for details.

**Example Error Response:**

```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Validation failed",
    "details": {
      "field": "quantity",
      "message": "Quantity must be positive"
    },
    "timestamp": "2025-11-15T10:30:00Z",
    "path": "/api/v1/stock-counts",
    "requestId": "req-123"
  }
}
```

**Implementation:** All services MUST use `ApiError` class from `common-application` module and return `ApiResponse.error(ApiError)` via `ApiResponseBuilder.error()`.

### Error Codes

**Common Error Codes:**

- `VALIDATION_ERROR` - Request validation failed
- `RESOURCE_NOT_FOUND` - Resource not found
- `DUPLICATE_RESOURCE` - Resource already exists
- `INSUFFICIENT_PERMISSIONS` - Insufficient permissions
- `RATE_LIMIT_EXCEEDED` - Rate limit exceeded
- `SERVICE_UNAVAILABLE` - Service unavailable
- `INTEGRATION_ERROR` - External integration error

**Domain-Specific Error Codes:**

- `STOCK_LEVEL_EXCEEDED` - Stock level exceeds maximum
- `EXPIRED_STOCK` - Cannot pick expired stock
- `INSUFFICIENT_STOCK` - Insufficient stock available
- `LOCATION_CAPACITY_EXCEEDED` - Location capacity exceeded
- `INVALID_BARCODE` - Invalid barcode format

---

## API Versioning

### Versioning Strategy

**URL Versioning:**

- Current version: `/api/v1/`
- Future versions: `/api/v2/`, `/api/v3/`, etc.

### Versioning Rules

1. **Breaking Changes** - Require new API version
2. **Non-Breaking Changes** - Can be added to current version
3. **Deprecation** - Deprecated endpoints marked, removed after 6 months
4. **Backward Compatibility** - Previous versions supported for 12 months

### Version Headers

**Request:**

```
Accept: application/vnd.ccbsa-wms.v1+json
```

**Response:**

```
Content-Type: application/vnd.ccbsa-wms.v1+json
```

---

## Appendix

### OpenAPI Specification

Full OpenAPI 3.0 specifications available at:

- `https://api.ccbsa-wms.com/v3/api-docs/stock-management`
- `https://api.ccbsa-wms.com/v3/api-docs/location-management`
- `https://api.ccbsa-wms.com/v3/api-docs/product`
- `https://api.ccbsa-wms.com/v3/api-docs/picking`
- `https://api.ccbsa-wms.com/v3/api-docs/returns`
- `https://api.ccbsa-wms.com/v3/api-docs/reconciliation`
- `https://api.ccbsa-wms.com/v3/api-docs/integration`
- `https://api.ccbsa-wms.com/v3/api-docs/tenant`

### Rate Limits

**Per Tenant:**

- Standard: 1000 requests/hour
- Premium: 5000 requests/hour

**Per User:**

- Standard: 100 requests/hour
- Premium: 500 requests/hour

**Rate Limit Headers:**

```
X-RateLimit-Limit: 1000
X-RateLimit-Remaining: 950
X-RateLimit-Reset: 1636984800
```

### Webhooks (Future)

**Supported Events:**

- `stock.consignment.confirmed`
- `stock.level.below_minimum`
- `picking.task.completed`
- `return.processed`
- `reconciliation.completed`

**Webhook Payload:**

```json
{
  "event": "stock.consignment.confirmed",
  "timestamp": "2025-11-15T10:00:00Z",
  "data": {
    "consignmentId": "cons-123",
    "consignmentReference": "CONS-2025-001"
  }
}
```

---

**Document Control**

- **Version History:** This document will be version controlled with change tracking
- **Review Cycle:** This document will be reviewed weekly during architecture phase
- **Distribution:** This document will be distributed to all API development team members

