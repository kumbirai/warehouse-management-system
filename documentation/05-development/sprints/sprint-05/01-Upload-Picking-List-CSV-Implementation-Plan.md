# Upload Picking List via CSV Implementation Plan

## US-6.1.1: Upload Picking List via CSV File

**Service:** Picking Service
**Priority:** Must Have
**Story Points:** 8
**Sprint:** Sprint 5

---

## Table of Contents

1. [Overview](#overview)
2. [UI Design](#ui-design)
3. [CSV Format Specification](#csv-format-specification)
4. [Domain Model Design](#domain-model-design)
5. [Backend Implementation](#backend-implementation)
6. [Frontend Implementation](#frontend-implementation)
7. [Data Flow](#data-flow)
8. [Testing Strategy](#testing-strategy)
9. [Acceptance Criteria Validation](#acceptance-criteria-validation)

---

## Overview

### User Story

**As a** warehouse operator
**I want** to upload picking lists via CSV file
**So that** I can efficiently import bulk picking list information

### Business Requirements

- System accepts CSV file uploads through web interface
- CSV format includes: load number, order numbers, customer information, products, quantities, priorities
- System validates CSV file format and required columns before processing
- System provides clear error messages for invalid CSV data
- System processes CSV file and creates picking list records
- System displays upload progress and completion status
- System supports CSV file sizes up to 10MB
- System logs all CSV upload events for audit
- System publishes `PickingListReceivedEvent`

### Technical Requirements

- Follow DDD, Clean Hexagonal Architecture, CQRS, Event-Driven Choreography
- Pure Java domain core (no framework dependencies)
- CSV streaming parsing for large files
- Multi-tenant support
- Move common value objects to `common-domain` (DRY principle)
- Implement proper error handling and validation

---

## UI Design

### Picking List CSV Upload Component

**Component:** `PickingListCsvUploadForm.tsx`

**Features:**

- **File Upload** - Drag-and-drop or browse file selection
- **CSV Template Download** - Download CSV template with sample data
- **Upload Progress** - Real-time upload progress indicator
- **Validation Results** - Display validation errors with line numbers
- **Success Confirmation** - Show number of picking lists created
- **Error Recovery** - Allow users to fix errors and re-upload

**UI Flow:**

1. User navigates to "Upload Picking List" page
2. System displays upload form with:
   - CSV template download link
   - File upload area (drag-and-drop or browse)
   - Upload button (disabled until file selected)
3. User downloads CSV template (optional)
4. User selects or drags CSV file to upload area
5. System validates file:
   - File type must be .csv
   - File size must be ≤ 10MB
   - Shows file info (name, size, last modified)
6. User clicks "Upload" button
7. System displays upload progress:
   - Uploading... (0-50%)
   - Processing... (50-90%)
   - Validating... (90-100%)
8. System displays results:
   - Success: "Successfully created X picking lists from Y rows"
   - Partial Success: "Created X picking lists, Y errors found"
   - Failure: "Upload failed, Z errors found"
9. If errors, system displays error table:
   - Row number
   - Field name
   - Error message
   - Invalid value
10. User can download error report (CSV)
11. User can fix errors and re-upload

**CSV Template:**

```csv
Load Number,Order Number,Customer Code,Customer Name,Product Code,Quantity,Priority,Notes
LOAD-001,ORD-001,CUST-001,ABC Company,PROD-001,100,HIGH,Urgent delivery
LOAD-001,ORD-001,CUST-001,ABC Company,PROD-002,50,HIGH,
LOAD-001,ORD-002,CUST-002,XYZ Corp,PROD-003,200,NORMAL,
LOAD-002,ORD-003,CUST-003,DEF Ltd,PROD-001,75,LOW,
```

**Upload Form Layout:**

```typescript
<Box>
  <Typography variant="h5">Upload Picking List</Typography>

  <Alert severity="info">
    <AlertTitle>CSV Format Requirements</AlertTitle>
    <ul>
      <li>Required columns: Load Number, Order Number, Customer Code, Product Code, Quantity</li>
      <li>Optional columns: Customer Name, Priority, Notes</li>
      <li>Maximum file size: 10MB</li>
      <li>File encoding: UTF-8</li>
    </ul>
  </Alert>

  <Button
    startIcon={<DownloadIcon />}
    onClick={handleDownloadTemplate}
  >
    Download CSV Template
  </Button>

  <Paper
    sx={{
      p: 3,
      border: '2px dashed',
      borderColor: isDragActive ? 'primary.main' : 'grey.300'
    }}
    onDrop={handleDrop}
    onDragOver={handleDragOver}
  >
    {selectedFile ? (
      <>
        <Typography variant="h6">{selectedFile.name}</Typography>
        <Typography variant="body2" color="textSecondary">
          Size: {(selectedFile.size / 1024).toFixed(2)} KB
        </Typography>
        <Button onClick={handleRemoveFile}>Remove</Button>
      </>
    ) : (
      <>
        <CloudUploadIcon sx={{ fontSize: 48, color: 'grey.400' }} />
        <Typography variant="body1">
          Drag and drop CSV file here, or click to browse
        </Typography>
        <input
          type="file"
          accept=".csv"
          onChange={handleFileSelect}
          style={{ display: 'none' }}
          ref={fileInputRef}
        />
        <Button onClick={() => fileInputRef.current?.click()}>
          Browse Files
        </Button>
      </>
    )}
  </Paper>

  {uploadProgress > 0 && (
    <Box sx={{ mt: 2 }}>
      <LinearProgress variant="determinate" value={uploadProgress} />
      <Typography variant="body2" color="textSecondary" align="center">
        {uploadStatus}
      </Typography>
    </Box>
  )}

  {errors.length > 0 && (
    <Alert severity="error" sx={{ mt: 2 }}>
      <AlertTitle>Validation Errors ({errors.length})</AlertTitle>
      <TableContainer>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Row</TableCell>
              <TableCell>Field</TableCell>
              <TableCell>Error</TableCell>
              <TableCell>Value</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {errors.slice(0, 10).map((error, index) => (
              <TableRow key={index}>
                <TableCell>{error.row}</TableCell>
                <TableCell>{error.field}</TableCell>
                <TableCell>{error.message}</TableCell>
                <TableCell>{error.value}</TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>
      {errors.length > 10 && (
        <Typography variant="body2" sx={{ mt: 1 }}>
          Showing 10 of {errors.length} errors.{' '}
          <Link onClick={handleDownloadErrors}>Download full error report</Link>
        </Typography>
      )}
    </Alert>
  )}

  {uploadResult && (
    <Alert severity={uploadResult.severity} sx={{ mt: 2 }}>
      <AlertTitle>{uploadResult.title}</AlertTitle>
      {uploadResult.message}
    </Alert>
  )}

  <Box sx={{ mt: 2, display: 'flex', gap: 2 }}>
    <Button
      variant="contained"
      onClick={handleUpload}
      disabled={!selectedFile || uploading}
      startIcon={uploading ? <CircularProgress size={20} /> : <UploadIcon />}
    >
      {uploading ? 'Uploading...' : 'Upload Picking List'}
    </Button>
    <Button
      variant="outlined"
      onClick={handleCancel}
      disabled={uploading}
    >
      Cancel
    </Button>
  </Box>
</Box>
```

**Validation Display:**

- **Success:** Green alert with success icon
- **Partial Success:** Orange alert with warning icon
- **Failure:** Red alert with error icon
- **Error Table:** Scrollable table with pagination
- **Download Errors:** CSV file with all errors

---

## CSV Format Specification

### Required Columns

| Column Name    | Data Type | Description                        | Validation Rules                                      |
|----------------|-----------|------------------------------------|----------------------------------------------------- |
| Load Number    | String    | Unique load identifier             | Required, max 50 chars, alphanumeric with dashes     |
| Order Number   | String    | Unique order identifier            | Required, max 50 chars, alphanumeric with dashes     |
| Customer Code  | String    | Customer identifier                | Required, max 50 chars, alphanumeric                 |
| Product Code   | String    | Product identifier                 | Required, max 50 chars, must exist in Product master |
| Quantity       | Decimal   | Quantity to pick                   | Required, positive number, max 10000                 |

### Optional Columns

| Column Name    | Data Type | Description                        | Validation Rules                                      |
|----------------|-----------|------------------------------------|----------------------------------------------------- |
| Customer Name  | String    | Customer display name              | Optional, max 200 chars                              |
| Priority       | String    | Picking priority                   | Optional, enum: HIGH, NORMAL, LOW (default: NORMAL)  |
| Notes          | String    | Additional notes                   | Optional, max 500 chars                              |

### CSV File Requirements

- **Encoding:** UTF-8
- **Delimiter:** Comma (,)
- **Quote Character:** Double quote (")
- **Header Row:** First row must contain column names
- **Max File Size:** 10MB
- **Max Rows:** 10,000 rows (excluding header)

### Example CSV

```csv
Load Number,Order Number,Customer Code,Customer Name,Product Code,Quantity,Priority,Notes
LOAD-2025-001,ORD-001,CUST-001,ABC Company,PROD-001,100,HIGH,Urgent delivery
LOAD-2025-001,ORD-001,CUST-001,ABC Company,PROD-002,50,HIGH,
LOAD-2025-001,ORD-002,CUST-002,XYZ Corp,PROD-003,200,NORMAL,
LOAD-2025-002,ORD-003,CUST-003,DEF Ltd,PROD-001,75,LOW,Handle with care
LOAD-2025-002,ORD-003,CUST-003,DEF Ltd,PROD-004,120,LOW,
```

### Validation Rules

1. **File Validation:**
   - File must be .csv format
   - File size must be ≤ 10MB
   - File must be UTF-8 encoded
   - File must not be empty

2. **Header Validation:**
   - All required columns must be present
   - Column names are case-insensitive
   - Extra columns are ignored

3. **Row Validation:**
   - Required fields must not be empty
   - Quantity must be positive decimal number
   - Priority must be valid enum value (if provided)
   - Product Code must exist in Product master data
   - Customer Code must be valid format

4. **Business Validation:**
   - Load Number can have multiple orders
   - Order Number can have multiple products (line items)
   - Customer Code must be consistent within same Order Number
   - Product Code availability will be checked during picking planning

---

## Domain Model Design

### Value Objects (Common Domain)

**LoadNumber**
- Package: `com.ccbsa.common.domain.valueobject`
- Immutable value object representing load identifier
- Validation: Not null, max 50 chars, alphanumeric with dashes

```java
package com.ccbsa.common.domain.valueobject;

import java.util.Objects;
import java.util.UUID;

public class LoadNumber {
    private final String value;

    public LoadNumber(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Load number cannot be null or empty");
        }
        if (value.length() > 50) {
            throw new IllegalArgumentException("Load number cannot exceed 50 characters");
        }
        if (!value.matches("^[A-Za-z0-9-]+$")) {
            throw new IllegalArgumentException("Load number must be alphanumeric with dashes only");
        }
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static LoadNumber generate() {
        return new LoadNumber("LOAD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LoadNumber that = (LoadNumber) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
```

**OrderNumber**
- Package: `com.ccbsa.common.domain.valueobject`
- Immutable value object representing order identifier
- Validation: Not null, max 50 chars, alphanumeric with dashes

```java
package com.ccbsa.common.domain.valueobject;

import java.util.Objects;
import java.util.UUID;

public class OrderNumber {
    private final String value;

    public OrderNumber(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Order number cannot be null or empty");
        }
        if (value.length() > 50) {
            throw new IllegalArgumentException("Order number cannot exceed 50 characters");
        }
        if (!value.matches("^[A-Za-z0-9-]+$")) {
            throw new IllegalArgumentException("Order number must be alphanumeric with dashes only");
        }
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static OrderNumber generate() {
        return new OrderNumber("ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrderNumber that = (OrderNumber) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
```

**CustomerInfo**
- Package: `com.ccbsa.common.domain.valueobject`
- Immutable value object representing customer information
- Contains customer code and name

```java
package com.ccbsa.common.domain.valueobject;

import java.util.Objects;

public class CustomerInfo {
    private final String customerCode;
    private final String customerName;

    public CustomerInfo(String customerCode, String customerName) {
        if (customerCode == null || customerCode.isBlank()) {
            throw new IllegalArgumentException("Customer code cannot be null or empty");
        }
        if (customerCode.length() > 50) {
            throw new IllegalArgumentException("Customer code cannot exceed 50 characters");
        }
        if (!customerCode.matches("^[A-Za-z0-9-]+$")) {
            throw new IllegalArgumentException("Customer code must be alphanumeric with dashes only");
        }
        if (customerName != null && customerName.length() > 200) {
            throw new IllegalArgumentException("Customer name cannot exceed 200 characters");
        }
        this.customerCode = customerCode;
        this.customerName = customerName;
    }

    public String getCustomerCode() {
        return customerCode;
    }

    public String getCustomerName() {
        return customerName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CustomerInfo that = (CustomerInfo) o;
        return Objects.equals(customerCode, that.customerCode) &&
               Objects.equals(customerName, that.customerName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(customerCode, customerName);
    }

    @Override
    public String toString() {
        return customerCode + (customerName != null ? " (" + customerName + ")" : "");
    }
}
```

**Priority**
- Package: `com.ccbsa.common.domain.valueobject`
- Enum representing picking priority

```java
package com.ccbsa.common.domain.valueobject;

public enum Priority {
    HIGH("High Priority"),
    NORMAL("Normal Priority"),
    LOW("Low Priority");

    private final String displayName;

    Priority(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static Priority fromString(String value) {
        if (value == null || value.isBlank()) {
            return NORMAL; // Default
        }
        try {
            return Priority.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid priority: " + value + ". Must be HIGH, NORMAL, or LOW");
        }
    }
}
```

### Domain Model (Picking Service)

**PickingList Aggregate Root**
- Package: `com.ccbsa.wms.picking.domain.core.entity`
- Aggregate root for picking list
- Contains loads, orders, and line items

```java
package com.ccbsa.wms.picking.domain.core.entity;

import com.ccbsa.common.domain.entity.AggregateRoot;
import com.ccbsa.common.domain.valueobject.*;
import com.ccbsa.wms.picking.domain.core.event.PickingListReceivedEvent;
import com.ccbsa.wms.picking.domain.core.valueobject.PickingListStatus;

import java.time.ZonedDateTime;
import java.util.*;

public class PickingList extends AggregateRoot<PickingListId> {
    private final TenantId tenantId;
    private final List<Load> loads;
    private PickingListStatus status;
    private final ZonedDateTime receivedAt;
    private ZonedDateTime processedAt;
    private String notes;

    private PickingList(Builder builder) {
        super.setId(builder.pickingListId);
        this.tenantId = builder.tenantId;
        this.loads = builder.loads;
        this.status = builder.status;
        this.receivedAt = builder.receivedAt;
        this.processedAt = builder.processedAt;
        this.notes = builder.notes;
    }

    public void initializePickingList() {
        this.status = PickingListStatus.RECEIVED;
        this.registerEvent(new PickingListReceivedEvent(this));
    }

    public void validatePickingList() {
        if (loads == null || loads.isEmpty()) {
            throw new IllegalStateException("Picking list must have at least one load");
        }
        loads.forEach(Load::validateLoad);
    }

    // Getters
    public TenantId getTenantId() { return tenantId; }
    public List<Load> getLoads() { return Collections.unmodifiableList(loads); }
    public PickingListStatus getStatus() { return status; }
    public ZonedDateTime getReceivedAt() { return receivedAt; }
    public ZonedDateTime getProcessedAt() { return processedAt; }
    public String getNotes() { return notes; }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private PickingListId pickingListId;
        private TenantId tenantId;
        private List<Load> loads;
        private PickingListStatus status;
        private ZonedDateTime receivedAt;
        private ZonedDateTime processedAt;
        private String notes;

        private Builder() {}

        public Builder pickingListId(PickingListId val) {
            pickingListId = val;
            return this;
        }

        public Builder tenantId(TenantId val) {
            tenantId = val;
            return this;
        }

        public Builder loads(List<Load> val) {
            loads = val;
            return this;
        }

        public Builder status(PickingListStatus val) {
            status = val;
            return this;
        }

        public Builder receivedAt(ZonedDateTime val) {
            receivedAt = val;
            return this;
        }

        public Builder processedAt(ZonedDateTime val) {
            processedAt = val;
            return this;
        }

        public Builder notes(String val) {
            notes = val;
            return this;
        }

        public PickingList build() {
            return new PickingList(this);
        }
    }
}
```

**Load Aggregate Root**
- Package: `com.ccbsa.wms.picking.domain.core.entity`
- Aggregate for load containing multiple orders

```java
package com.ccbsa.wms.picking.domain.core.entity;

import com.ccbsa.common.domain.entity.AggregateRoot;
import com.ccbsa.common.domain.valueobject.*;
import com.ccbsa.wms.picking.domain.core.valueobject.LoadStatus;

import java.time.ZonedDateTime;
import java.util.*;

public class Load extends AggregateRoot<LoadId> {
    private final LoadNumber loadNumber;
    private final List<Order> orders;
    private LoadStatus status;
    private final ZonedDateTime createdAt;
    private ZonedDateTime plannedAt;

    private Load(Builder builder) {
        super.setId(builder.loadId);
        this.loadNumber = builder.loadNumber;
        this.orders = builder.orders;
        this.status = builder.status;
        this.createdAt = builder.createdAt;
        this.plannedAt = builder.plannedAt;
    }

    public void validateLoad() {
        if (orders == null || orders.isEmpty()) {
            throw new IllegalStateException("Load must have at least one order");
        }
        orders.forEach(Order::validateOrder);
    }

    // Getters
    public LoadNumber getLoadNumber() { return loadNumber; }
    public List<Order> getOrders() { return Collections.unmodifiableList(orders); }
    public LoadStatus getStatus() { return status; }
    public ZonedDateTime getCreatedAt() { return createdAt; }
    public ZonedDateTime getPlannedAt() { return plannedAt; }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private LoadId loadId;
        private LoadNumber loadNumber;
        private List<Order> orders;
        private LoadStatus status;
        private ZonedDateTime createdAt;
        private ZonedDateTime plannedAt;

        private Builder() {}

        public Builder loadId(LoadId val) {
            loadId = val;
            return this;
        }

        public Builder loadNumber(LoadNumber val) {
            loadNumber = val;
            return this;
        }

        public Builder orders(List<Order> val) {
            orders = val;
            return this;
        }

        public Builder status(LoadStatus val) {
            status = val;
            return this;
        }

        public Builder createdAt(ZonedDateTime val) {
            createdAt = val;
            return this;
        }

        public Builder plannedAt(ZonedDateTime val) {
            plannedAt = val;
            return this;
        }

        public Load build() {
            return new Load(this);
        }
    }
}
```

Continue with the rest of the domain model in the next section...

---

## Backend Implementation

### 1. Domain Core Layer

**PickingListId**

```java
package com.ccbsa.wms.picking.domain.core.valueobject;

import com.ccbsa.common.domain.valueobject.BaseId;

import java.util.UUID;

public class PickingListId extends BaseId<UUID> {
    public PickingListId(UUID value) {
        super(value);
    }

    public static PickingListId of(UUID value) {
        return new PickingListId(value);
    }

    public static PickingListId newId() {
        return new PickingListId(UUID.randomUUID());
    }
}
```

**LoadId**

```java
package com.ccbsa.wms.picking.domain.core.valueobject;

import com.ccbsa.common.domain.valueobject.BaseId;

import java.util.UUID;

public class LoadId extends BaseId<UUID> {
    public LoadId(UUID value) {
        super(value);
    }

    public static LoadId of(UUID value) {
        return new LoadId(value);
    }

    public static LoadId newId() {
        return new LoadId(UUID.randomUUID());
    }
}
```

**PickingListStatus Enum**

```java
package com.ccbsa.wms.picking.domain.core.valueobject;

public enum PickingListStatus {
    RECEIVED("Received"),
    PLANNING("Planning"),
    PLANNED("Planned"),
    IN_PROGRESS("In Progress"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled");

    private final String displayName;

    PickingListStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
```

**LoadStatus Enum**

```java
package com.ccbsa.wms.picking.domain.core.valueobject;

public enum LoadStatus {
    PENDING("Pending"),
    PLANNED("Planned"),
    IN_PROGRESS("In Progress"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled");

    private final String displayName;

    LoadStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
```

**PickingListReceivedEvent**

```java
package com.ccbsa.wms.picking.domain.core.event;

import com.ccbsa.common.domain.event.DomainEvent;
import com.ccbsa.wms.picking.domain.core.entity.PickingList;

import java.time.ZonedDateTime;

public class PickingListReceivedEvent extends DomainEvent<PickingList> {
    private final PickingList pickingList;

    public PickingListReceivedEvent(PickingList pickingList) {
        super(pickingList, ZonedDateTime.now());
        this.pickingList = pickingList;
    }

    public PickingList getPickingList() {
        return pickingList;
    }
}
```

Due to the length, let me continue with the application service layer and the rest of the implementation in the next message. Let me save what we have so far.

---

## Testing Strategy

### Unit Tests

**Domain Core Tests:**
- PickingList creation and validation
- Load creation and validation
- Order creation and validation
- Value object validation (LoadNumber, OrderNumber, CustomerInfo, Priority)
- Event publishing verification

**CSV Parser Tests:**
- Valid CSV parsing
- Invalid CSV format handling
- Missing required columns
- Invalid data types
- Empty file handling
- Large file handling (streaming)

**Command Handler Tests:**
- Valid command handling
- Invalid command handling
- Product validation
- Customer validation
- Duplicate load number handling

### Integration Tests

**Service Integration Tests:**
- CSV upload end-to-end
- Product Service integration
- Database persistence
- Event publishing to Kafka

**Repository Tests:**
- PickingList repository operations
- Load repository operations
- Order repository operations

### Gateway API Tests

- CSV upload through gateway
- Authentication and authorization
- Error handling
- File size limits
- Content type validation

---

## Acceptance Criteria Validation

| Acceptance Criteria | Implementation | Status |
|---------------------|----------------|--------|
| AC1: System accepts CSV file uploads through web interface | PickingListCsvUploadForm.tsx component with drag-and-drop | ✅ Planned |
| AC2: CSV format includes required fields | CSV format specification documented | ✅ Planned |
| AC3: System validates CSV file format and columns | CSV parser with validation | ✅ Planned |
| AC4: System provides clear error messages | Error display component with detailed messages | ✅ Planned |
| AC5: System processes CSV and creates picking lists | UploadPickingListCsvCommandHandler | ✅ Planned |
| AC6: System displays upload progress | Upload progress indicator component | ✅ Planned |
| AC7: System supports CSV up to 10MB | File size validation | ✅ Planned |
| AC8: System logs all CSV upload events | Audit logging in command handler | ✅ Planned |
| AC9: System publishes PickingListReceivedEvent | Event publishing after successful creation | ✅ Planned |

---

**Document Control**

- **Version:** 1.0
- **Date:** 2025-01
- **Status:** Draft - Section 1 Complete
- **Next:** Application Service Layer implementation details
