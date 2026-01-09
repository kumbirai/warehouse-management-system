# Handle Partial Order Acceptance Implementation Plan

## US-7.1.1: Handle Partial Order Acceptance

**Service:** Returns Service
**Priority:** Must Have
**Story Points:** 8
**Sprint:** Sprint 7

---

## Table of Contents

1. [Overview](#overview)
2. [UI Design](#ui-design)
3. [Domain Model Design](#domain-model-design)
4. [Backend Implementation](#backend-implementation)
5. [Frontend Implementation](#frontend-implementation)
6. [Data Flow](#data-flow)
7. [Testing Strategy](#testing-strategy)
8. [Acceptance Criteria Validation](#acceptance-criteria-validation)

---

## Overview

### User Story

**As a** warehouse operator
**I want** to handle partial order acceptance
**So that** I can process returns when customers accept only part of their order

### Business Requirements

- System records accepted quantities per order line
- System identifies returned quantities (ordered - accepted)
- System updates order status accordingly
- System initiates returns process for unaccepted items
- System publishes `ReturnInitiatedEvent` for partial returns
- System records customer signature and acceptance timestamp
- System supports multiple product lines per order
- System validates accepted quantities do not exceed ordered quantities

### Technical Requirements

- Follow DDD, Clean Hexagonal Architecture, CQRS, Event-Driven Choreography
- Pure Java domain core (no framework dependencies)
- Multi-tenant support
- Move common value objects to `common-domain` (DRY principle)
- Implement proper error handling and validation
- Integration with Picking Service for order data
- Integration with Stock Management Service for stock updates

---

## UI Design

### Partial Order Acceptance Component

**Component:** `PartialOrderAcceptancePage.tsx`

**Features:**

- **Order Selection** - Select completed picking list and order
- **Product Line Display** - Show all products in order with quantities
- **Accepted Quantity Input** - Input accepted quantities per line
- **Return Reason Selection** - Select reason for partial rejection
- **Customer Signature** - Capture customer signature (digital)
- **Validation** - Real-time validation of accepted quantities
- **Return Summary** - Display summary of returned items
- **Confirmation** - Confirm partial acceptance and initiate return

**UI Flow:**

1. User navigates to "Partial Order Acceptance" page
2. System displays order selection:
    - Search/filter by load number, order number, customer
    - Show only completed picking lists
    - Display order status (PICKED, DELIVERED, etc.)
3. User selects order
4. System displays order details:
    - Customer information
    - Order lines with product details
    - Picked quantities per line
    - Input fields for accepted quantities
5. User enters accepted quantities per line:
    - System validates: 0 ≤ accepted ≤ picked
    - System calculates returned quantity: picked - accepted
    - System highlights lines with returns (returned > 0)
6. User selects return reason for each returned line:
    - CUSTOMER_REJECTION
    - QUALITY_ISSUE
    - OVERSTOCK
    - DAMAGE
    - OTHER
7. User adds optional notes per returned line
8. System displays return summary:
    - Total lines with returns
    - Total returned quantity
    - Total accepted quantity
    - Return reasons breakdown
9. User captures customer signature (digital signature pad)
10. User clicks "Confirm Partial Acceptance"
11. System validates all inputs
12. System submits partial acceptance
13. System displays confirmation:
    - Return reference number
    - Accepted lines summary
    - Returned lines summary
    - Next steps (return location assignment)

**Partial Order Acceptance Form Layout:**

```typescript
<Box>
  <PageBreadcrumbs
    items={[
      { label: 'Returns', href: '/returns' },
      { label: 'Partial Order Acceptance' }
    ]}
  />

  <Typography variant="h4">Partial Order Acceptance</Typography>

  {/* Order Selection */}
  <Paper sx={{ p: 3, mb: 3 }}>
    <Typography variant="h6">Select Order</Typography>

    <Grid container spacing={2}>
      <Grid item xs={12} md={4}>
        <TextField
          label="Load Number"
          value={searchFilters.loadNumber}
          onChange={handleLoadNumberChange}
          fullWidth
        />
      </Grid>
      <Grid item xs={12} md={4}>
        <TextField
          label="Order Number"
          value={searchFilters.orderNumber}
          onChange={handleOrderNumberChange}
          fullWidth
        />
      </Grid>
      <Grid item xs={12} md={4}>
        <TextField
          label="Customer"
          value={searchFilters.customer}
          onChange={handleCustomerChange}
          fullWidth
        />
      </Grid>
    </Grid>

    <Button
      variant="contained"
      onClick={handleSearchOrders}
      startIcon={<SearchIcon />}
      sx={{ mt: 2 }}
    >
      Search Orders
    </Button>

    {orders.length > 0 && (
      <TableContainer sx={{ mt: 2 }}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>Load Number</TableCell>
              <TableCell>Order Number</TableCell>
              <TableCell>Customer</TableCell>
              <TableCell>Status</TableCell>
              <TableCell>Picked Date</TableCell>
              <TableCell>Actions</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {orders.map((order) => (
              <TableRow key={order.orderId}>
                <TableCell>{order.loadNumber}</TableCell>
                <TableCell>{order.orderNumber}</TableCell>
                <TableCell>{order.customerName}</TableCell>
                <TableCell>
                  <StatusBadge status={order.status} />
                </TableCell>
                <TableCell>{formatDateTime(order.pickedAt)}</TableCell>
                <TableCell>
                  <Button
                    size="small"
                    onClick={() => handleSelectOrder(order)}
                  >
                    Select
                  </Button>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>
    )}
  </Paper>

  {/* Order Details and Acceptance Form */}
  {selectedOrder && (
    <>
      {/* Customer and Order Information */}
      <Paper sx={{ p: 3, mb: 3 }}>
        <Typography variant="h6">Order Details</Typography>

        <Grid container spacing={2} sx={{ mt: 1 }}>
          <Grid item xs={12} md={6}>
            <Typography variant="body2" color="textSecondary">
              Load Number
            </Typography>
            <Typography variant="body1" fontWeight="bold">
              {selectedOrder.loadNumber}
            </Typography>
          </Grid>
          <Grid item xs={12} md={6}>
            <Typography variant="body2" color="textSecondary">
              Order Number
            </Typography>
            <Typography variant="body1" fontWeight="bold">
              {selectedOrder.orderNumber}
            </Typography>
          </Grid>
          <Grid item xs={12} md={6}>
            <Typography variant="body2" color="textSecondary">
              Customer
            </Typography>
            <Typography variant="body1">
              {selectedOrder.customerName}
            </Typography>
            <Typography variant="body2" color="textSecondary">
              {selectedOrder.customerCode}
            </Typography>
          </Grid>
          <Grid item xs={12} md={6}>
            <Typography variant="body2" color="textSecondary">
              Picked Date
            </Typography>
            <Typography variant="body1">
              {formatDateTime(selectedOrder.pickedAt)}
            </Typography>
          </Grid>
        </Grid>
      </Paper>

      {/* Order Lines - Acceptance Quantities */}
      <Paper sx={{ p: 3, mb: 3 }}>
        <Typography variant="h6">Product Lines</Typography>

        <Alert severity="info" sx={{ mt: 2, mb: 2 }}>
          <AlertTitle>Instructions</AlertTitle>
          Enter the accepted quantity for each product line. The system will
          calculate the returned quantity automatically.
        </Alert>

        <TableContainer>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>Product Code</TableCell>
                <TableCell>Product Description</TableCell>
                <TableCell align="right">Ordered Qty</TableCell>
                <TableCell align="right">Picked Qty</TableCell>
                <TableCell align="right">Accepted Qty</TableCell>
                <TableCell align="right">Returned Qty</TableCell>
                <TableCell>Return Reason</TableCell>
                <TableCell>Notes</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {orderLines.map((line, index) => (
                <TableRow
                  key={line.lineId}
                  sx={{
                    backgroundColor:
                      line.returnedQuantity > 0 ? 'warning.light' : 'inherit'
                  }}
                >
                  <TableCell>{line.productCode}</TableCell>
                  <TableCell>{line.productDescription}</TableCell>
                  <TableCell align="right">{line.orderedQuantity}</TableCell>
                  <TableCell align="right">{line.pickedQuantity}</TableCell>
                  <TableCell align="right">
                    <TextField
                      type="number"
                      value={line.acceptedQuantity}
                      onChange={(e) =>
                        handleAcceptedQuantityChange(index, e.target.value)
                      }
                      error={
                        line.acceptedQuantity < 0 ||
                        line.acceptedQuantity > line.pickedQuantity
                      }
                      helperText={
                        line.acceptedQuantity < 0
                          ? 'Must be >= 0'
                          : line.acceptedQuantity > line.pickedQuantity
                          ? 'Cannot exceed picked quantity'
                          : ''
                      }
                      inputProps={{
                        min: 0,
                        max: line.pickedQuantity,
                        step: 1
                      }}
                      size="small"
                      sx={{ width: 100 }}
                    />
                  </TableCell>
                  <TableCell
                    align="right"
                    sx={{
                      fontWeight: line.returnedQuantity > 0 ? 'bold' : 'normal',
                      color: line.returnedQuantity > 0 ? 'error.main' : 'inherit'
                    }}
                  >
                    {line.returnedQuantity}
                  </TableCell>
                  <TableCell>
                    {line.returnedQuantity > 0 && (
                      <Select
                        value={line.returnReason || ''}
                        onChange={(e) =>
                          handleReturnReasonChange(index, e.target.value)
                        }
                        size="small"
                        fullWidth
                        error={!line.returnReason}
                      >
                        <MenuItem value="CUSTOMER_REJECTION">
                          Customer Rejection
                        </MenuItem>
                        <MenuItem value="QUALITY_ISSUE">
                          Quality Issue
                        </MenuItem>
                        <MenuItem value="OVERSTOCK">
                          Overstock
                        </MenuItem>
                        <MenuItem value="DAMAGE">
                          Damage
                        </MenuItem>
                        <MenuItem value="OTHER">
                          Other
                        </MenuItem>
                      </Select>
                    )}
                  </TableCell>
                  <TableCell>
                    {line.returnedQuantity > 0 && (
                      <TextField
                        value={line.notes || ''}
                        onChange={(e) =>
                          handleNotesChange(index, e.target.value)
                        }
                        placeholder="Optional notes..."
                        size="small"
                        fullWidth
                        multiline
                        rows={1}
                      />
                    )}
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>

        {/* Summary Section */}
        <Box sx={{ mt: 3, p: 2, backgroundColor: 'grey.100', borderRadius: 1 }}>
          <Grid container spacing={2}>
            <Grid item xs={12} md={3}>
              <Typography variant="body2" color="textSecondary">
                Total Lines
              </Typography>
              <Typography variant="h6">{orderLines.length}</Typography>
            </Grid>
            <Grid item xs={12} md={3}>
              <Typography variant="body2" color="textSecondary">
                Lines with Returns
              </Typography>
              <Typography variant="h6" color="warning.main">
                {linesWithReturns}
              </Typography>
            </Grid>
            <Grid item xs={12} md={3}>
              <Typography variant="body2" color="textSecondary">
                Total Accepted Quantity
              </Typography>
              <Typography variant="h6" color="success.main">
                {totalAcceptedQuantity}
              </Typography>
            </Grid>
            <Grid item xs={12} md={3}>
              <Typography variant="body2" color="textSecondary">
                Total Returned Quantity
              </Typography>
              <Typography variant="h6" color="error.main">
                {totalReturnedQuantity}
              </Typography>
            </Grid>
          </Grid>
        </Box>
      </Paper>

      {/* Customer Signature */}
      <Paper sx={{ p: 3, mb: 3 }}>
        <Typography variant="h6">Customer Signature</Typography>

        <Alert severity="info" sx={{ mt: 2, mb: 2 }}>
          <AlertTitle>Signature Required</AlertTitle>
          Please ask the customer to sign below to confirm partial acceptance.
        </Alert>

        <Box sx={{ border: '2px solid', borderColor: 'grey.300', p: 2 }}>
          <SignaturePad
            ref={signaturePadRef}
            options={{
              minWidth: 1,
              maxWidth: 3,
              penColor: 'black',
              backgroundColor: 'white'
            }}
          />
        </Box>

        <Box sx={{ mt: 2, display: 'flex', gap: 2 }}>
          <Button
            variant="outlined"
            onClick={handleClearSignature}
            startIcon={<ClearIcon />}
          >
            Clear Signature
          </Button>
        </Box>
      </Paper>

      {/* Action Buttons */}
      <Box sx={{ display: 'flex', gap: 2, justifyContent: 'flex-end' }}>
        <Button
          variant="outlined"
          onClick={handleCancel}
          disabled={submitting}
        >
          Cancel
        </Button>
        <Button
          variant="contained"
          onClick={handleSubmitPartialAcceptance}
          disabled={!isFormValid || submitting}
          startIcon={submitting ? <CircularProgress size={20} /> : <CheckIcon />}
        >
          {submitting ? 'Processing...' : 'Confirm Partial Acceptance'}
        </Button>
      </Box>
    </>
  )}

  {/* Success Dialog */}
  <Dialog open={showSuccessDialog} onClose={handleCloseSuccessDialog}>
    <DialogTitle>
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
        <CheckCircleIcon color="success" />
        Partial Acceptance Confirmed
      </Box>
    </DialogTitle>
    <DialogContent>
      <Typography variant="body1" gutterBottom>
        Return reference: <strong>{returnReference}</strong>
      </Typography>

      <Box sx={{ mt: 2 }}>
        <Typography variant="subtitle2" gutterBottom>
          Accepted Lines Summary:
        </Typography>
        <Typography variant="body2" color="success.main">
          • {acceptedLinesCount} lines accepted
        </Typography>
        <Typography variant="body2" color="success.main">
          • Total accepted quantity: {totalAcceptedQuantity}
        </Typography>
      </Box>

      <Box sx={{ mt: 2 }}>
        <Typography variant="subtitle2" gutterBottom>
          Returned Lines Summary:
        </Typography>
        <Typography variant="body2" color="error.main">
          • {returnedLinesCount} lines returned
        </Typography>
        <Typography variant="body2" color="error.main">
          • Total returned quantity: {totalReturnedQuantity}
        </Typography>
      </Box>

      <Alert severity="info" sx={{ mt: 2 }}>
        <AlertTitle>Next Steps</AlertTitle>
        The returned items will be assigned to a return location automatically.
        You will be notified once the location assignment is complete.
      </Alert>
    </DialogContent>
    <DialogActions>
      <Button onClick={handleViewReturn}>View Return Details</Button>
      <Button onClick={handleCloseSuccessDialog} variant="contained">
        Close
      </Button>
    </DialogActions>
  </Dialog>
</Box>
```

**Validation Rules:**

- Accepted quantity must be >= 0
- Accepted quantity must be <= picked quantity
- If returned quantity > 0, return reason is required
- Customer signature is required
- At least one line must have accepted quantity < picked quantity (otherwise it's not a partial acceptance)

**Error Handling:**

- Display validation errors inline per field
- Show summary of validation errors at top of form
- Prevent submission if validation errors exist
- Handle network errors gracefully with retry option
- Show clear error messages for API failures

---

## Domain Model Design

### Aggregates

#### Return Aggregate

**Package:** `com.ccbsa.wms.returns.domain.core.entity`

**Aggregate Root:** `Return`

**Responsibilities:**

- Manage return lifecycle
- Validate return quantities
- Calculate returned quantities
- Track return status
- Publish domain events

**State:**

```java
public class Return {
    private ReturnId returnId;
    private OrderId orderId;
    private LoadNumber loadNumber;
    private CustomerId customerId;
    private CustomerInfo customerInfo;
    private ReturnType returnType; // PARTIAL, FULL
    private ReturnStatus status;
    private List<ReturnLineItem> lineItems;
    private CustomerSignature customerSignature;
    private ZonedDateTime returnedAt;
    private TenantId tenantId;
    private boolean active;
    
    // Audit fields
    private ZonedDateTime createdAt;
    private String createdBy;
    private ZonedDateTime updatedAt;
    private String updatedBy;
}
```

**Business Methods:**

```java
// Factory method for partial return
public static Return initiatePartialReturn(
    ReturnId returnId,
    OrderId orderId,
    LoadNumber loadNumber,
    CustomerId customerId,
    CustomerInfo customerInfo,
    List<ReturnLineItem> lineItems,
    CustomerSignature customerSignature,
    TenantId tenantId
);

// Validate return quantities
public void validateReturnQuantities();

// Complete return processing
public void complete();

// Assign location
public void assignLocation(LocationId locationId);

// Cancel return
public void cancel(String reason);
```

**Domain Events:**

- `ReturnInitiatedEvent` - Published when partial return is initiated
- `ReturnValidatedEvent` - Published after validation
- `ReturnLocationAssignedEvent` - Published when location assigned
- `ReturnCompletedEvent` - Published when return processing completed
- `ReturnCancelledEvent` - Published when return cancelled

**Invariants:**

- Return ID must not be null
- Order ID must not be null
- Return must have at least one line item with returned quantity > 0
- Returned quantity per line must not exceed picked quantity
- Accepted quantity per line must be >= 0
- Return type must match line items (PARTIAL if any accepted quantity > 0)
- Customer signature required for partial returns
- Status transitions must follow valid flow: INITIATED → LOCATION_ASSIGNED → COMPLETED

---

#### ReturnLineItem Entity

**Package:** `com.ccbsa.wms.returns.domain.core.entity`

**Entity:** `ReturnLineItem`

**Responsibilities:**

- Track individual product line return details
- Calculate returned quantities
- Store return reason per line

**State:**

```java
public class ReturnLineItem {
    private ReturnLineItemId lineItemId;
    private ProductId productId;
    private ProductCode productCode;
    private String productDescription;
    private Quantity orderedQuantity;
    private Quantity pickedQuantity;
    private Quantity acceptedQuantity;
    private Quantity returnedQuantity; // pickedQuantity - acceptedQuantity
    private ReturnReason returnReason;
    private String notes;
    private ZonedDateTime createdAt;
}
```

**Business Methods:**

```java
// Factory method
public static ReturnLineItem create(
    ProductId productId,
    ProductCode productCode,
    String productDescription,
    Quantity orderedQuantity,
    Quantity pickedQuantity,
    Quantity acceptedQuantity,
    ReturnReason returnReason,
    String notes
);

// Calculate returned quantity
public Quantity calculateReturnedQuantity();

// Validate quantities
public void validateQuantities();
```

**Invariants:**

- Product ID must not be null
- Ordered quantity must be > 0
- Picked quantity must be > 0
- Accepted quantity must be >= 0 and <= picked quantity
- Returned quantity = picked quantity - accepted quantity
- If returned quantity > 0, return reason is required

---

### Value Objects

#### ReturnId

**Package:** `com.ccbsa.wms.returns.domain.core.valueobject`

```java
public class ReturnId extends BaseId<UUID> {
    private ReturnId(UUID value) {
        super(value);
    }
    
    public static ReturnId of(UUID value) {
        return new ReturnId(value);
    }
    
    public static ReturnId newId() {
        return new ReturnId(UUID.randomUUID());
    }
}
```

---

#### ReturnLineItemId

**Package:** `com.ccbsa.wms.returns.domain.core.valueobject`

```java
public class ReturnLineItemId extends BaseId<UUID> {
    private ReturnLineItemId(UUID value) {
        super(value);
    }
    
    public static ReturnLineItemId of(UUID value) {
        return new ReturnLineItemId(value);
    }
    
    public static ReturnLineItemId newId() {
        return new ReturnLineItemId(UUID.randomUUID());
    }
}
```

---

#### ReturnType (Enum)

**Package:** `com.ccbsa.wms.returns.domain.core.valueobject`

```java
public enum ReturnType {
    PARTIAL,  // Some items accepted, some returned
    FULL      // All items returned
}
```

---

#### ReturnStatus (Enum)

**Package:** `com.ccbsa.common.domain.valueobject` (Move to common-domain - DRY)

```java
public enum ReturnStatus {
    INITIATED,           // Return initiated
    PROCESSING,          // Return being processed
    LOCATION_ASSIGNED,   // Return location assigned
    COMPLETED,           // Return processing completed
    RECONCILED,          // Reconciled with D365
    CANCELLED            // Return cancelled
}
```

---

#### ReturnReason (Enum)

**Package:** `com.ccbsa.common.domain.valueobject` (Move to common-domain - DRY)

```java
public enum ReturnReason {
    CUSTOMER_REJECTION("Customer Rejection", "Customer rejected the product"),
    QUALITY_ISSUE("Quality Issue", "Product quality does not meet standards"),
    OVERSTOCK("Overstock", "Customer has excess stock"),
    DAMAGE("Damage", "Product damaged during transit or storage"),
    PRICING_DISPUTE("Pricing Dispute", "Dispute over product pricing"),
    EXPIRED("Expired", "Product expired or near expiration"),
    WRONG_PRODUCT("Wrong Product", "Wrong product delivered"),
    OTHER("Other", "Other reason");
    
    private final String displayName;
    private final String description;
    
    ReturnReason(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
}
```

---

#### CustomerSignature

**Package:** `com.ccbsa.wms.returns.domain.core.valueobject`

```java
public class CustomerSignature {
    private final String signatureData; // Base64 encoded signature image
    private final ZonedDateTime signedAt;
    private final String signerName; // Optional
    
    private CustomerSignature(String signatureData, ZonedDateTime signedAt, String signerName) {
        validateSignatureData(signatureData);
        this.signatureData = signatureData;
        this.signedAt = signedAt;
        this.signerName = signerName;
    }
    
    public static CustomerSignature of(String signatureData, ZonedDateTime signedAt, String signerName) {
        return new CustomerSignature(signatureData, signedAt, signerName);
    }
    
    private void validateSignatureData(String signatureData) {
        if (signatureData == null || signatureData.trim().isEmpty()) {
            throw new IllegalArgumentException("Signature data cannot be null or empty");
        }
        // Additional validation for Base64 format
    }
    
    public String getSignatureData() {
        return signatureData;
    }
    
    public ZonedDateTime getSignedAt() {
        return signedAt;
    }
    
    public String getSignerName() {
        return signerName;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CustomerSignature that = (CustomerSignature) o;
        return Objects.equals(signatureData, that.signatureData) &&
               Objects.equals(signedAt, that.signedAt);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(signatureData, signedAt);
    }
}
```

---

### Domain Events

#### ReturnInitiatedEvent

**Package:** `com.ccbsa.wms.returns.domain.core.event`

```java
public class ReturnInitiatedEvent extends ReturnsEvent<Return> {
    private final ReturnId returnId;
    private final OrderId orderId;
    private final LoadNumber loadNumber;
    private final ReturnType returnType;
    private final int totalLines;
    private final int linesWithReturns;
    private final BigDecimal totalReturnedQuantity;
    
    private ReturnInitiatedEvent(Builder builder) {
        super(builder.returnAggregate);
        this.returnId = builder.returnId;
        this.orderId = builder.orderId;
        this.loadNumber = builder.loadNumber;
        this.returnType = builder.returnType;
        this.totalLines = builder.totalLines;
        this.linesWithReturns = builder.linesWithReturns;
        this.totalReturnedQuantity = builder.totalReturnedQuantity;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    // Getters
    public ReturnId getReturnId() { return returnId; }
    public OrderId getOrderId() { return orderId; }
    public LoadNumber getLoadNumber() { return loadNumber; }
    public ReturnType getReturnType() { return returnType; }
    public int getTotalLines() { return totalLines; }
    public int getLinesWithReturns() { return linesWithReturns; }
    public BigDecimal getTotalReturnedQuantity() { return totalReturnedQuantity; }
    
    public static class Builder {
        private Return returnAggregate;
        private ReturnId returnId;
        private OrderId orderId;
        private LoadNumber loadNumber;
        private ReturnType returnType;
        private int totalLines;
        private int linesWithReturns;
        private BigDecimal totalReturnedQuantity;
        
        public Builder returnAggregate(Return returnAggregate) {
            this.returnAggregate = returnAggregate;
            return this;
        }
        
        public Builder returnId(ReturnId returnId) {
            this.returnId = returnId;
            return this;
        }
        
        public Builder orderId(OrderId orderId) {
            this.orderId = orderId;
            return this;
        }
        
        public Builder loadNumber(LoadNumber loadNumber) {
            this.loadNumber = loadNumber;
            return this;
        }
        
        public Builder returnType(ReturnType returnType) {
            this.returnType = returnType;
            return this;
        }
        
        public Builder totalLines(int totalLines) {
            this.totalLines = totalLines;
            return this;
        }
        
        public Builder linesWithReturns(int linesWithReturns) {
            this.linesWithReturns = linesWithReturns;
            return this;
        }
        
        public Builder totalReturnedQuantity(BigDecimal totalReturnedQuantity) {
            this.totalReturnedQuantity = totalReturnedQuantity;
            return this;
        }
        
        public ReturnInitiatedEvent build() {
            return new ReturnInitiatedEvent(this);
        }
    }
}
```

---

### Domain Service

While domain services are generally avoided in favor of rich domain models, we may need a domain service for complex return quantity calculations or validations that span multiple aggregates.

**For now, keep logic within Return aggregate.**

---

## Backend Implementation

### Layer 1: Domain Core (Pure Java)

#### 1.1 Return Aggregate Implementation

**File:** `services/returns-service/returns-domain/returns-domain-core/src/main/java/com/ccbsa/wms/returns/domain/core/entity/Return.java`

```java
package com.ccbsa.wms.returns.domain.core.entity;

import com.ccbsa.common.domain.entity.AggregateRoot;
import com.ccbsa.common.domain.valueobject.*;
import com.ccbsa.wms.returns.domain.core.event.ReturnInitiatedEvent;
import com.ccbsa.wms.returns.domain.core.exception.InvalidReturnException;
import com.ccbsa.wms.returns.domain.core.valueobject.*;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Return extends AggregateRoot<ReturnId> {
    private final OrderId orderId;
    private final LoadNumber loadNumber;
    private final CustomerId customerId;
    private final CustomerInfo customerInfo;
    private final ReturnType returnType;
    private ReturnStatus status;
    private final List<ReturnLineItem> lineItems;
    private final CustomerSignature customerSignature;
    private final ZonedDateTime returnedAt;
    private final TenantId tenantId;
    private LocationId assignedLocationId;
    private boolean active;
    
    // Audit fields
    private final ZonedDateTime createdAt;
    private final String createdBy;
    private ZonedDateTime updatedAt;
    private String updatedBy;
    
    private Return(Builder builder) {
        setId(builder.returnId);
        this.orderId = builder.orderId;
        this.loadNumber = builder.loadNumber;
        this.customerId = builder.customerId;
        this.customerInfo = builder.customerInfo;
        this.returnType = builder.returnType;
        this.status = builder.status;
        this.lineItems = builder.lineItems;
        this.customerSignature = builder.customerSignature;
        this.returnedAt = builder.returnedAt;
        this.tenantId = builder.tenantId;
        this.assignedLocationId = builder.assignedLocationId;
        this.active = builder.active;
        this.createdAt = builder.createdAt;
        this.createdBy = builder.createdBy;
        this.updatedAt = builder.updatedAt;
        this.updatedBy = builder.updatedBy;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Factory method to initiate a partial return
     */
    public static Return initiatePartialReturn(
            ReturnId returnId,
            OrderId orderId,
            LoadNumber loadNumber,
            CustomerId customerId,
            CustomerInfo customerInfo,
            List<ReturnLineItem> lineItems,
            CustomerSignature customerSignature,
            TenantId tenantId,
            String createdBy) {
        
        // Validate inputs
        validatePartialReturnInputs(returnId, orderId, loadNumber, lineItems, customerSignature, tenantId);
        
        // Validate return quantities
        validateReturnQuantities(lineItems);
        
        // Determine if it's truly a partial return
        boolean hasAcceptedItems = lineItems.stream()
            .anyMatch(line -> line.getAcceptedQuantity().getValue().compareTo(BigDecimal.ZERO) > 0);
        boolean hasReturnedItems = lineItems.stream()
            .anyMatch(line -> line.getReturnedQuantity().getValue().compareTo(BigDecimal.ZERO) > 0);
        
        if (!hasAcceptedItems || !hasReturnedItems) {
            throw new InvalidReturnException(
                "Partial return must have both accepted and returned items. " +
                "Use full return if all items are returned."
            );
        }
        
        ZonedDateTime now = ZonedDateTime.now();
        
        Return returnAggregate = Return.builder()
            .returnId(returnId)
            .orderId(orderId)
            .loadNumber(loadNumber)
            .customerId(customerId)
            .customerInfo(customerInfo)
            .returnType(ReturnType.PARTIAL)
            .status(ReturnStatus.INITIATED)
            .lineItems(new ArrayList<>(lineItems))
            .customerSignature(customerSignature)
            .returnedAt(now)
            .tenantId(tenantId)
            .active(true)
            .createdAt(now)
            .createdBy(createdBy)
            .updatedAt(now)
            .updatedBy(createdBy)
            .build();
        
        // Publish domain event
        returnAggregate.registerEvent(createReturnInitiatedEvent(returnAggregate));
        
        return returnAggregate;
    }
    
    /**
     * Validate partial return inputs
     */
    private static void validatePartialReturnInputs(
            ReturnId returnId,
            OrderId orderId,
            LoadNumber loadNumber,
            List<ReturnLineItem> lineItems,
            CustomerSignature customerSignature,
            TenantId tenantId) {
        
        if (returnId == null) {
            throw new InvalidReturnException("Return ID cannot be null");
        }
        if (orderId == null) {
            throw new InvalidReturnException("Order ID cannot be null");
        }
        if (loadNumber == null) {
            throw new InvalidReturnException("Load number cannot be null");
        }
        if (lineItems == null || lineItems.isEmpty()) {
            throw new InvalidReturnException("Return must have at least one line item");
        }
        if (customerSignature == null) {
            throw new InvalidReturnException("Customer signature is required for partial returns");
        }
        if (tenantId == null) {
            throw new InvalidReturnException("Tenant ID cannot be null");
        }
    }
    
    /**
     * Validate return quantities across all line items
     */
    private static void validateReturnQuantities(List<ReturnLineItem> lineItems) {
        for (ReturnLineItem lineItem : lineItems) {
            lineItem.validateQuantities();
        }
    }
    
    /**
     * Create ReturnInitiatedEvent
     */
    private static ReturnInitiatedEvent createReturnInitiatedEvent(Return returnAggregate) {
        int linesWithReturns = (int) returnAggregate.getLineItems().stream()
            .filter(line -> line.getReturnedQuantity().getValue().compareTo(BigDecimal.ZERO) > 0)
            .count();
        
        BigDecimal totalReturnedQuantity = returnAggregate.getLineItems().stream()
            .map(line -> line.getReturnedQuantity().getValue())
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        return ReturnInitiatedEvent.builder()
            .returnAggregate(returnAggregate)
            .returnId(returnAggregate.getId())
            .orderId(returnAggregate.getOrderId())
            .loadNumber(returnAggregate.getLoadNumber())
            .returnType(returnAggregate.getReturnType())
            .totalLines(returnAggregate.getLineItems().size())
            .linesWithReturns(linesWithReturns)
            .totalReturnedQuantity(totalReturnedQuantity)
            .build();
    }
    
    /**
     * Assign location to return
     */
    public void assignLocation(LocationId locationId, String updatedBy) {
        if (locationId == null) {
            throw new InvalidReturnException("Location ID cannot be null");
        }
        if (this.status != ReturnStatus.INITIATED && this.status != ReturnStatus.PROCESSING) {
            throw new InvalidReturnException(
                "Cannot assign location to return in status: " + this.status
            );
        }
        
        this.assignedLocationId = locationId;
        this.status = ReturnStatus.LOCATION_ASSIGNED;
        this.updatedAt = ZonedDateTime.now();
        this.updatedBy = updatedBy;
        
        // Publish domain event
        registerEvent(createLocationAssignedEvent());
    }
    
    /**
     * Complete return processing
     */
    public void complete(String updatedBy) {
        if (this.status != ReturnStatus.LOCATION_ASSIGNED) {
            throw new InvalidReturnException(
                "Cannot complete return without location assignment. Current status: " + this.status
            );
        }
        
        this.status = ReturnStatus.COMPLETED;
        this.updatedAt = ZonedDateTime.now();
        this.updatedBy = updatedBy;
        
        // Publish domain event
        registerEvent(createReturnCompletedEvent());
    }
    
    /**
     * Cancel return
     */
    public void cancel(String reason, String updatedBy) {
        if (this.status == ReturnStatus.COMPLETED || this.status == ReturnStatus.CANCELLED) {
            throw new InvalidReturnException(
                "Cannot cancel return in status: " + this.status
            );
        }
        
        this.status = ReturnStatus.CANCELLED;
        this.active = false;
        this.updatedAt = ZonedDateTime.now();
        this.updatedBy = updatedBy;
        
        // Publish domain event
        registerEvent(createReturnCancelledEvent(reason));
    }
    
    // Event creation methods
    private ReturnLocationAssignedEvent createLocationAssignedEvent() {
        // Implementation
        return null; // Placeholder
    }
    
    private ReturnCompletedEvent createReturnCompletedEvent() {
        // Implementation
        return null; // Placeholder
    }
    
    private ReturnCancelledEvent createReturnCancelledEvent(String reason) {
        // Implementation
        return null; // Placeholder
    }
    
    // Getters
    public OrderId getOrderId() { return orderId; }
    public LoadNumber getLoadNumber() { return loadNumber; }
    public CustomerId getCustomerId() { return customerId; }
    public CustomerInfo getCustomerInfo() { return customerInfo; }
    public ReturnType getReturnType() { return returnType; }
    public ReturnStatus getStatus() { return status; }
    public List<ReturnLineItem> getLineItems() { return new ArrayList<>(lineItems); }
    public CustomerSignature getCustomerSignature() { return customerSignature; }
    public ZonedDateTime getReturnedAt() { return returnedAt; }
    public TenantId getTenantId() { return tenantId; }
    public LocationId getAssignedLocationId() { return assignedLocationId; }
    public boolean isActive() { return active; }
    public ZonedDateTime getCreatedAt() { return createdAt; }
    public String getCreatedBy() { return createdBy; }
    public ZonedDateTime getUpdatedAt() { return updatedAt; }
    public String getUpdatedBy() { return updatedBy; }
    
    public static class Builder {
        private ReturnId returnId;
        private OrderId orderId;
        private LoadNumber loadNumber;
        private CustomerId customerId;
        private CustomerInfo customerInfo;
        private ReturnType returnType;
        private ReturnStatus status;
        private List<ReturnLineItem> lineItems;
        private CustomerSignature customerSignature;
        private ZonedDateTime returnedAt;
        private TenantId tenantId;
        private LocationId assignedLocationId;
        private boolean active;
        private ZonedDateTime createdAt;
        private String createdBy;
        private ZonedDateTime updatedAt;
        private String updatedBy;
        
        public Builder returnId(ReturnId returnId) {
            this.returnId = returnId;
            return this;
        }
        
        public Builder orderId(OrderId orderId) {
            this.orderId = orderId;
            return this;
        }
        
        public Builder loadNumber(LoadNumber loadNumber) {
            this.loadNumber = loadNumber;
            return this;
        }
        
        public Builder customerId(CustomerId customerId) {
            this.customerId = customerId;
            return this;
        }
        
        public Builder customerInfo(CustomerInfo customerInfo) {
            this.customerInfo = customerInfo;
            return this;
        }
        
        public Builder returnType(ReturnType returnType) {
            this.returnType = returnType;
            return this;
        }
        
        public Builder status(ReturnStatus status) {
            this.status = status;
            return this;
        }
        
        public Builder lineItems(List<ReturnLineItem> lineItems) {
            this.lineItems = lineItems;
            return this;
        }
        
        public Builder customerSignature(CustomerSignature customerSignature) {
            this.customerSignature = customerSignature;
            return this;
        }
        
        public Builder returnedAt(ZonedDateTime returnedAt) {
            this.returnedAt = returnedAt;
            return this;
        }
        
        public Builder tenantId(TenantId tenantId) {
            this.tenantId = tenantId;
            return this;
        }
        
        public Builder assignedLocationId(LocationId assignedLocationId) {
            this.assignedLocationId = assignedLocationId;
            return this;
        }
        
        public Builder active(boolean active) {
            this.active = active;
            return this;
        }
        
        public Builder createdAt(ZonedDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }
        
        public Builder createdBy(String createdBy) {
            this.createdBy = createdBy;
            return this;
        }
        
        public Builder updatedAt(ZonedDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }
        
        public Builder updatedBy(String updatedBy) {
            this.updatedBy = updatedBy;
            return this;
        }
        
        public Return build() {
            return new Return(this);
        }
    }
}
```

Due to character limits, I'll continue with the remaining sections in the next response. The plan continues with:
- ReturnLineItem implementation
- Application Service layer (Commands, Handlers, Ports)
- Application Layer (REST Controllers, DTOs)
- Data Access Layer
- Frontend Implementation
- Testing Strategy
- Acceptance Criteria Validation

Would you like me to continue with the remaining sections?

#### 1.2 ReturnLineItem Entity Implementation

**File:** `services/returns-service/returns-domain/returns-domain-core/src/main/java/com/ccbsa/wms/returns/domain/core/entity/ReturnLineItem.java`

```java
package com.ccbsa.wms.returns.domain.core.entity;

import com.ccbsa.common.domain.entity.BaseEntity;
import com.ccbsa.common.domain.valueobject.*;
import com.ccbsa.wms.returns.domain.core.exception.InvalidReturnException;
import com.ccbsa.wms.returns.domain.core.valueobject.*;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.Objects;

public class ReturnLineItem extends BaseEntity<ReturnLineItemId> {
    private final ProductId productId;
    private final ProductCode productCode;
    private final String productDescription;
    private final Quantity orderedQuantity;
    private final Quantity pickedQuantity;
    private final Quantity acceptedQuantity;
    private final Quantity returnedQuantity;
    private final ReturnReason returnReason;
    private final String notes;
    private final ZonedDateTime createdAt;
    
    private ReturnLineItem(Builder builder) {
        setId(builder.lineItemId);
        this.productId = builder.productId;
        this.productCode = builder.productCode;
        this.productDescription = builder.productDescription;
        this.orderedQuantity = builder.orderedQuantity;
        this.pickedQuantity = builder.pickedQuantity;
        this.acceptedQuantity = builder.acceptedQuantity;
        this.returnedQuantity = builder.returnedQuantity;
        this.returnReason = builder.returnReason;
        this.notes = builder.notes;
        this.createdAt = builder.createdAt;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Factory method to create a return line item
     */
    public static ReturnLineItem create(
            ProductId productId,
            ProductCode productCode,
            String productDescription,
            Quantity orderedQuantity,
            Quantity pickedQuantity,
            Quantity acceptedQuantity,
            ReturnReason returnReason,
            String notes) {
        
        // Calculate returned quantity
        Quantity returnedQuantity = calculateReturnedQuantity(pickedQuantity, acceptedQuantity);
        
        ReturnLineItem lineItem = ReturnLineItem.builder()
            .lineItemId(ReturnLineItemId.newId())
            .productId(productId)
            .productCode(productCode)
            .productDescription(productDescription)
            .orderedQuantity(orderedQuantity)
            .pickedQuantity(pickedQuantity)
            .acceptedQuantity(acceptedQuantity)
            .returnedQuantity(returnedQuantity)
            .returnReason(returnReason)
            .notes(notes)
            .createdAt(ZonedDateTime.now())
            .build();
        
        // Validate after creation
        lineItem.validateQuantities();
        
        return lineItem;
    }
    
    /**
     * Calculate returned quantity
     */
    private static Quantity calculateReturnedQuantity(Quantity pickedQuantity, Quantity acceptedQuantity) {
        BigDecimal returnedValue = pickedQuantity.getValue().subtract(acceptedQuantity.getValue());
        return Quantity.of(returnedValue);
    }
    
    /**
     * Validate quantities
     */
    public void validateQuantities() {
        // Product ID validation
        if (productId == null) {
            throw new InvalidReturnException("Product ID cannot be null");
        }
        
        // Ordered quantity validation
        if (orderedQuantity == null || orderedQuantity.getValue().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidReturnException(
                "Ordered quantity must be greater than zero for product: " + productCode.getValue()
            );
        }
        
        // Picked quantity validation
        if (pickedQuantity == null || pickedQuantity.getValue().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidReturnException(
                "Picked quantity must be greater than zero for product: " + productCode.getValue()
            );
        }
        
        // Accepted quantity validation
        if (acceptedQuantity == null || acceptedQuantity.getValue().compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidReturnException(
                "Accepted quantity cannot be negative for product: " + productCode.getValue()
            );
        }
        
        // Accepted cannot exceed picked
        if (acceptedQuantity.getValue().compareTo(pickedQuantity.getValue()) > 0) {
            throw new InvalidReturnException(
                "Accepted quantity cannot exceed picked quantity for product: " + productCode.getValue() +
                ". Picked: " + pickedQuantity.getValue() + ", Accepted: " + acceptedQuantity.getValue()
            );
        }
        
        // Returned quantity validation
        BigDecimal expectedReturned = pickedQuantity.getValue().subtract(acceptedQuantity.getValue());
        if (returnedQuantity.getValue().compareTo(expectedReturned) != 0) {
            throw new InvalidReturnException(
                "Returned quantity mismatch for product: " + productCode.getValue() +
                ". Expected: " + expectedReturned + ", Actual: " + returnedQuantity.getValue()
            );
        }
        
        // Return reason required if returned quantity > 0
        if (returnedQuantity.getValue().compareTo(BigDecimal.ZERO) > 0 && returnReason == null) {
            throw new InvalidReturnException(
                "Return reason is required when returned quantity > 0 for product: " + productCode.getValue()
            );
        }
    }
    
    // Getters
    public ProductId getProductId() { return productId; }
    public ProductCode getProductCode() { return productCode; }
    public String getProductDescription() { return productDescription; }
    public Quantity getOrderedQuantity() { return orderedQuantity; }
    public Quantity getPickedQuantity() { return pickedQuantity; }
    public Quantity getAcceptedQuantity() { return acceptedQuantity; }
    public Quantity getReturnedQuantity() { return returnedQuantity; }
    public ReturnReason getReturnReason() { return returnReason; }
    public String getNotes() { return notes; }
    public ZonedDateTime getCreatedAt() { return createdAt; }
    
    public static class Builder {
        private ReturnLineItemId lineItemId;
        private ProductId productId;
        private ProductCode productCode;
        private String productDescription;
        private Quantity orderedQuantity;
        private Quantity pickedQuantity;
        private Quantity acceptedQuantity;
        private Quantity returnedQuantity;
        private ReturnReason returnReason;
        private String notes;
        private ZonedDateTime createdAt;
        
        public Builder lineItemId(ReturnLineItemId lineItemId) {
            this.lineItemId = lineItemId;
            return this;
        }
        
        public Builder productId(ProductId productId) {
            this.productId = productId;
            return this;
        }
        
        public Builder productCode(ProductCode productCode) {
            this.productCode = productCode;
            return this;
        }
        
        public Builder productDescription(String productDescription) {
            this.productDescription = productDescription;
            return this;
        }
        
        public Builder orderedQuantity(Quantity orderedQuantity) {
            this.orderedQuantity = orderedQuantity;
            return this;
        }
        
        public Builder pickedQuantity(Quantity pickedQuantity) {
            this.pickedQuantity = pickedQuantity;
            return this;
        }
        
        public Builder acceptedQuantity(Quantity acceptedQuantity) {
            this.acceptedQuantity = acceptedQuantity;
            return this;
        }
        
        public Builder returnedQuantity(Quantity returnedQuantity) {
            this.returnedQuantity = returnedQuantity;
            return this;
        }
        
        public Builder returnReason(ReturnReason returnReason) {
            this.returnReason = returnReason;
            return this;
        }
        
        public Builder notes(String notes) {
            this.notes = notes;
            return this;
        }
        
        public Builder createdAt(ZonedDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }
        
        public ReturnLineItem build() {
            return new ReturnLineItem(this);
        }
    }
}
```

---

### Layer 2: Application Service (Use Case Orchestration)

#### 2.1 Command DTO

**File:** `services/returns-service/returns-domain/returns-application-service/src/main/java/com/ccbsa/wms/returns/application/service/command/dto/HandlePartialOrderAcceptanceCommand.java`

```java
package com.ccbsa.wms.returns.application.service.command.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class HandlePartialOrderAcceptanceCommand {
    private final UUID orderId;
    private final String loadNumber;
    private final UUID customerId;
    private final String customerCode;
    private final String customerName;
    private final List<PartialReturnLineItemCommand> lineItems;
    private final String signatureData;
    private final String signerName;
    private final ZonedDateTime signedAt;
    
    @Getter
    @Builder
    public static class PartialReturnLineItemCommand {
        private final UUID productId;
        private final String productCode;
        private final String productDescription;
        private final BigDecimal orderedQuantity;
        private final BigDecimal pickedQuantity;
        private final BigDecimal acceptedQuantity;
        private final String returnReason;
        private final String notes;
    }
}
```

---

#### 2.2 Result DTO

**File:** `services/returns-service/returns-domain/returns-application-service/src/main/java/com/ccbsa/wms/returns/application/service/command/dto/HandlePartialOrderAcceptanceResult.java`

```java
package com.ccbsa.wms.returns.application.service.command.dto;

import com.ccbsa.wms.returns.domain.core.valueobject.ReturnStatus;
import com.ccbsa.wms.returns.domain.core.valueobject.ReturnType;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

public class HandlePartialOrderAcceptanceResult {
    private final UUID returnId;
    private final UUID orderId;
    private final String loadNumber;
    private final ReturnType returnType;
    private final ReturnStatus status;
    private final int totalLines;
    private final int linesWithReturns;
    private final BigDecimal totalAcceptedQuantity;
    private final BigDecimal totalReturnedQuantity;
    private final List<ReturnLineItemResult> lineItems;
    private final ZonedDateTime returnedAt;
    
    private HandlePartialOrderAcceptanceResult(Builder builder) {
        this.returnId = builder.returnId;
        this.orderId = builder.orderId;
        this.loadNumber = builder.loadNumber;
        this.returnType = builder.returnType;
        this.status = builder.status;
        this.totalLines = builder.totalLines;
        this.linesWithReturns = builder.linesWithReturns;
        this.totalAcceptedQuantity = builder.totalAcceptedQuantity;
        this.totalReturnedQuantity = builder.totalReturnedQuantity;
        this.lineItems = builder.lineItems;
        this.returnedAt = builder.returnedAt;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    // Getters
    public UUID getReturnId() { return returnId; }
    public UUID getOrderId() { return orderId; }
    public String getLoadNumber() { return loadNumber; }
    public ReturnType getReturnType() { return returnType; }
    public ReturnStatus getStatus() { return status; }
    public int getTotalLines() { return totalLines; }
    public int getLinesWithReturns() { return linesWithReturns; }
    public BigDecimal getTotalAcceptedQuantity() { return totalAcceptedQuantity; }
    public BigDecimal getTotalReturnedQuantity() { return totalReturnedQuantity; }
    public List<ReturnLineItemResult> getLineItems() { return lineItems; }
    public ZonedDateTime getReturnedAt() { return returnedAt; }
    
    public static class ReturnLineItemResult {
        private final UUID lineItemId;
        private final String productCode;
        private final String productDescription;
        private final BigDecimal orderedQuantity;
        private final BigDecimal pickedQuantity;
        private final BigDecimal acceptedQuantity;
        private final BigDecimal returnedQuantity;
        private final String returnReason;
        
        private ReturnLineItemResult(Builder builder) {
            this.lineItemId = builder.lineItemId;
            this.productCode = builder.productCode;
            this.productDescription = builder.productDescription;
            this.orderedQuantity = builder.orderedQuantity;
            this.pickedQuantity = builder.pickedQuantity;
            this.acceptedQuantity = builder.acceptedQuantity;
            this.returnedQuantity = builder.returnedQuantity;
            this.returnReason = builder.returnReason;
        }
        
        public static Builder builder() {
            return new Builder();
        }
        
        // Getters
        public UUID getLineItemId() { return lineItemId; }
        public String getProductCode() { return productCode; }
        public String getProductDescription() { return productDescription; }
        public BigDecimal getOrderedQuantity() { return orderedQuantity; }
        public BigDecimal getPickedQuantity() { return pickedQuantity; }
        public BigDecimal getAcceptedQuantity() { return acceptedQuantity; }
        public BigDecimal getReturnedQuantity() { return returnedQuantity; }
        public String getReturnReason() { return returnReason; }
        
        public static class Builder {
            private UUID lineItemId;
            private String productCode;
            private String productDescription;
            private BigDecimal orderedQuantity;
            private BigDecimal pickedQuantity;
            private BigDecimal acceptedQuantity;
            private BigDecimal returnedQuantity;
            private String returnReason;
            
            public Builder lineItemId(UUID lineItemId) {
                this.lineItemId = lineItemId;
                return this;
            }
            
            public Builder productCode(String productCode) {
                this.productCode = productCode;
                return this;
            }
            
            public Builder productDescription(String productDescription) {
                this.productDescription = productDescription;
                return this;
            }
            
            public Builder orderedQuantity(BigDecimal orderedQuantity) {
                this.orderedQuantity = orderedQuantity;
                return this;
            }
            
            public Builder pickedQuantity(BigDecimal pickedQuantity) {
                this.pickedQuantity = pickedQuantity;
                return this;
            }
            
            public Builder acceptedQuantity(BigDecimal acceptedQuantity) {
                this.acceptedQuantity = acceptedQuantity;
                return this;
            }
            
            public Builder returnedQuantity(BigDecimal returnedQuantity) {
                this.returnedQuantity = returnedQuantity;
                return this;
            }
            
            public Builder returnReason(String returnReason) {
                this.returnReason = returnReason;
                return this;
            }
            
            public ReturnLineItemResult build() {
                return new ReturnLineItemResult(this);
            }
        }
    }
    
    public static class Builder {
        private UUID returnId;
        private UUID orderId;
        private String loadNumber;
        private ReturnType returnType;
        private ReturnStatus status;
        private int totalLines;
        private int linesWithReturns;
        private BigDecimal totalAcceptedQuantity;
        private BigDecimal totalReturnedQuantity;
        private List<ReturnLineItemResult> lineItems;
        private ZonedDateTime returnedAt;
        
        public Builder returnId(UUID returnId) {
            this.returnId = returnId;
            return this;
        }
        
        public Builder orderId(UUID orderId) {
            this.orderId = orderId;
            return this;
        }
        
        public Builder loadNumber(String loadNumber) {
            this.loadNumber = loadNumber;
            return this;
        }
        
        public Builder returnType(ReturnType returnType) {
            this.returnType = returnType;
            return this;
        }
        
        public Builder status(ReturnStatus status) {
            this.status = status;
            return this;
        }
        
        public Builder totalLines(int totalLines) {
            this.totalLines = totalLines;
            return this;
        }
        
        public Builder linesWithReturns(int linesWithReturns) {
            this.linesWithReturns = linesWithReturns;
            return this;
        }
        
        public Builder totalAcceptedQuantity(BigDecimal totalAcceptedQuantity) {
            this.totalAcceptedQuantity = totalAcceptedQuantity;
            return this;
        }
        
        public Builder totalReturnedQuantity(BigDecimal totalReturnedQuantity) {
            this.totalReturnedQuantity = totalReturnedQuantity;
            return this;
        }
        
        public Builder lineItems(List<ReturnLineItemResult> lineItems) {
            this.lineItems = lineItems;
            return this;
        }
        
        public Builder returnedAt(ZonedDateTime returnedAt) {
            this.returnedAt = returnedAt;
            return this;
        }
        
        public HandlePartialOrderAcceptanceResult build() {
            return new HandlePartialOrderAcceptanceResult(this);
        }
    }
}
```

---

#### 2.3 Command Handler

**File:** `services/returns-service/returns-domain/returns-application-service/src/main/java/com/ccbsa/wms/returns/application/service/command/HandlePartialOrderAcceptanceCommandHandler.java`

```java
package com.ccbsa.wms.returns.application.service.command;

import com.ccbsa.common.domain.event.publisher.DomainEventPublisher;
import com.ccbsa.common.domain.valueobject.*;
import com.ccbsa.wms.returns.application.service.command.dto.HandlePartialOrderAcceptanceCommand;
import com.ccbsa.wms.returns.application.service.command.dto.HandlePartialOrderAcceptanceResult;
import com.ccbsa.wms.returns.application.service.port.repository.ReturnRepository;
import com.ccbsa.wms.returns.application.service.port.service.PickingServicePort;
import com.ccbsa.wms.returns.domain.core.entity.Return;
import com.ccbsa.wms.returns.domain.core.entity.ReturnLineItem;
import com.ccbsa.wms.returns.domain.core.valueobject.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class HandlePartialOrderAcceptanceCommandHandler {
    
    private final ReturnRepository returnRepository;
    private final PickingServicePort pickingServicePort;
    private final DomainEventPublisher<Return> domainEventPublisher;
    
    @Transactional
    public HandlePartialOrderAcceptanceResult handle(
            HandlePartialOrderAcceptanceCommand command,
            TenantId tenantId,
            String userId) {
        
        log.info("Handling partial order acceptance for order: {}, tenant: {}", 
                 command.getOrderId(), tenantId.getValue());
        
        // Step 1: Validate order exists and picking is completed
        validateOrderCompletedPicking(OrderId.of(command.getOrderId()), tenantId);
        
        // Step 2: Build domain objects
        ReturnId returnId = ReturnId.newId();
        OrderId orderId = OrderId.of(command.getOrderId());
        LoadNumber loadNumber = LoadNumber.of(command.getLoadNumber());
        CustomerId customerId = CustomerId.of(command.getCustomerId());
        CustomerInfo customerInfo = CustomerInfo.of(
            command.getCustomerCode(),
            command.getCustomerName()
        );
        
        // Step 3: Create return line items
        List<ReturnLineItem> lineItems = createReturnLineItems(command.getLineItems());
        
        // Step 4: Create customer signature
        CustomerSignature customerSignature = CustomerSignature.of(
            command.getSignatureData(),
            command.getSignedAt(),
            command.getSignerName()
        );
        
        // Step 5: Initiate partial return
        Return returnAggregate = Return.initiatePartialReturn(
            returnId,
            orderId,
            loadNumber,
            customerId,
            customerInfo,
            lineItems,
            customerSignature,
            tenantId,
            userId
        );
        
        // Step 6: Save return
        Return savedReturn = returnRepository.save(returnAggregate);
        
        // Step 7: Publish domain events
        domainEventPublisher.publish(savedReturn);
        
        log.info("Partial order acceptance completed. Return ID: {}", returnId.getValue());
        
        // Step 8: Build result
        return buildResult(savedReturn);
    }
    
    private void validateOrderCompletedPicking(OrderId orderId, TenantId tenantId) {
        boolean isCompleted = pickingServicePort.isOrderPickingCompleted(orderId, tenantId);
        if (!isCompleted) {
            throw new IllegalStateException(
                "Cannot process return for order " + orderId.getValue() + 
                ". Picking must be completed first."
            );
        }
    }
    
    private List<ReturnLineItem> createReturnLineItems(
            List<HandlePartialOrderAcceptanceCommand.PartialReturnLineItemCommand> lineItemCommands) {
        
        return lineItemCommands.stream()
            .map(lineItemCommand -> ReturnLineItem.create(
                ProductId.of(lineItemCommand.getProductId()),
                ProductCode.of(lineItemCommand.getProductCode()),
                lineItemCommand.getProductDescription(),
                Quantity.of(lineItemCommand.getOrderedQuantity()),
                Quantity.of(lineItemCommand.getPickedQuantity()),
                Quantity.of(lineItemCommand.getAcceptedQuantity()),
                ReturnReason.valueOf(lineItemCommand.getReturnReason()),
                lineItemCommand.getNotes()
            ))
            .collect(Collectors.toList());
    }
    
    private HandlePartialOrderAcceptanceResult buildResult(Return returnAggregate) {
        // Calculate totals
        BigDecimal totalAcceptedQuantity = returnAggregate.getLineItems().stream()
            .map(line -> line.getAcceptedQuantity().getValue())
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalReturnedQuantity = returnAggregate.getLineItems().stream()
            .map(line -> line.getReturnedQuantity().getValue())
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        int linesWithReturns = (int) returnAggregate.getLineItems().stream()
            .filter(line -> line.getReturnedQuantity().getValue().compareTo(BigDecimal.ZERO) > 0)
            .count();
        
        // Build line item results
        List<HandlePartialOrderAcceptanceResult.ReturnLineItemResult> lineItemResults = 
            returnAggregate.getLineItems().stream()
                .map(lineItem -> HandlePartialOrderAcceptanceResult.ReturnLineItemResult.builder()
                    .lineItemId(lineItem.getId().getValue())
                    .productCode(lineItem.getProductCode().getValue())
                    .productDescription(lineItem.getProductDescription())
                    .orderedQuantity(lineItem.getOrderedQuantity().getValue())
                    .pickedQuantity(lineItem.getPickedQuantity().getValue())
                    .acceptedQuantity(lineItem.getAcceptedQuantity().getValue())
                    .returnedQuantity(lineItem.getReturnedQuantity().getValue())
                    .returnReason(lineItem.getReturnReason() != null ? 
                        lineItem.getReturnReason().name() : null)
                    .build())
                .collect(Collectors.toList());
        
        return HandlePartialOrderAcceptanceResult.builder()
            .returnId(returnAggregate.getId().getValue())
            .orderId(returnAggregate.getOrderId().getValue())
            .loadNumber(returnAggregate.getLoadNumber().getValue())
            .returnType(returnAggregate.getReturnType())
            .status(returnAggregate.getStatus())
            .totalLines(returnAggregate.getLineItems().size())
            .linesWithReturns(linesWithReturns)
            .totalAcceptedQuantity(totalAcceptedQuantity)
            .totalReturnedQuantity(totalReturnedQuantity)
            .lineItems(lineItemResults)
            .returnedAt(returnAggregate.getReturnedAt())
            .build();
    }
}
```

---

#### 2.4 Repository Port Interface

**File:** `services/returns-service/returns-domain/returns-application-service/src/main/java/com/ccbsa/wms/returns/application/service/port/repository/ReturnRepository.java`

```java
package com.ccbsa.wms.returns.application.service.port.repository;

import com.ccbsa.common.domain.valueobject.OrderId;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.returns.domain.core.entity.Return;
import com.ccbsa.wms.returns.domain.core.valueobject.ReturnId;

import java.util.List;
import java.util.Optional;

public interface ReturnRepository {
    
    /**
     * Save return
     */
    Return save(Return returnAggregate);
    
    /**
     * Find return by ID
     */
    Optional<Return> findById(ReturnId returnId, TenantId tenantId);
    
    /**
     * Find returns by order ID
     */
    List<Return> findByOrderId(OrderId orderId, TenantId tenantId);
    
    /**
     * Find all active returns for tenant
     */
    List<Return> findAllActive(TenantId tenantId);
}
```

---

#### 2.5 Service Port Interface (Picking Service Integration)

**File:** `services/returns-service/returns-domain/returns-application-service/src/main/java/com/ccbsa/wms/returns/application/service/port/service/PickingServicePort.java`

```java
package com.ccbsa.wms.returns.application.service.port.service;

import com.ccbsa.common.domain.valueobject.OrderId;
import com.ccbsa.common.domain.valueobject.TenantId;

public interface PickingServicePort {
    
    /**
     * Check if order picking is completed
     */
    boolean isOrderPickingCompleted(OrderId orderId, TenantId tenantId);
    
    /**
     * Get order details for return validation
     */
    OrderDetailsDto getOrderDetails(OrderId orderId, TenantId tenantId);
}
```

---

Due to length constraints, the plan continues with:
- Layer 3: Application Layer (REST Controllers, DTOs, Mappers)
- Layer 4: Data Access Layer (JPA Entities, Repository Adapters)
- Layer 5: Messaging (Event Publishers, Listeners)
- Frontend Implementation (Hooks, Services, Components)
- Testing Strategy
- Acceptance Criteria Validation

Shall I continue with the remaining sections?

### Layer 3: Application Layer (REST API)

#### 3.1 Request DTO

**File:** `services/returns-service/returns-application/src/main/java/com/ccbsa/wms/returns/application/dto/command/HandlePartialOrderAcceptanceRequestDTO.java`

```java
package com.ccbsa.wms.returns.application.dto.command;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.Valid;
import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to handle partial order acceptance")
public class HandlePartialOrderAcceptanceRequestDTO {
    
    @NotNull(message = "Order ID is required")
    @Schema(description = "Order ID", example = "550e8400-e29b-41d4-a716-446655440000", required = true)
    private UUID orderId;
    
    @NotBlank(message = "Load number is required")
    @Size(max = 50, message = "Load number must not exceed 50 characters")
    @Schema(description = "Load number", example = "LOAD-2025-001", required = true)
    private String loadNumber;
    
    @NotNull(message = "Customer ID is required")
    @Schema(description = "Customer ID", example = "650e8400-e29b-41d4-a716-446655440000", required = true)
    private UUID customerId;
    
    @NotBlank(message = "Customer code is required")
    @Size(max = 50, message = "Customer code must not exceed 50 characters")
    @Schema(description = "Customer code", example = "CUST-001", required = true)
    private String customerCode;
    
    @Size(max = 200, message = "Customer name must not exceed 200 characters")
    @Schema(description = "Customer name", example = "ABC Company")
    private String customerName;
    
    @NotEmpty(message = "Line items are required")
    @Valid
    @Schema(description = "Return line items", required = true)
    private List<PartialReturnLineItemRequestDTO> lineItems;
    
    @NotBlank(message = "Signature data is required")
    @Schema(description = "Base64 encoded signature image", required = true)
    private String signatureData;
    
    @Size(max = 200, message = "Signer name must not exceed 200 characters")
    @Schema(description = "Name of person who signed")
    private String signerName;
    
    @NotNull(message = "Signed at timestamp is required")
    @Schema(description = "Signature timestamp", required = true)
    private ZonedDateTime signedAt;
    
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Partial return line item")
    public static class PartialReturnLineItemRequestDTO {
        
        @NotNull(message = "Product ID is required")
        @Schema(description = "Product ID", example = "750e8400-e29b-41d4-a716-446655440000", required = true)
        private UUID productId;
        
        @NotBlank(message = "Product code is required")
        @Size(max = 50, message = "Product code must not exceed 50 characters")
        @Schema(description = "Product code", example = "PROD-001", required = true)
        private String productCode;
        
        @Size(max = 500, message = "Product description must not exceed 500 characters")
        @Schema(description = "Product description", example = "Product Description")
        private String productDescription;
        
        @NotNull(message = "Ordered quantity is required")
        @DecimalMin(value = "0.01", message = "Ordered quantity must be greater than 0")
        @Schema(description = "Ordered quantity", example = "100", required = true)
        private BigDecimal orderedQuantity;
        
        @NotNull(message = "Picked quantity is required")
        @DecimalMin(value = "0.01", message = "Picked quantity must be greater than 0")
        @Schema(description = "Picked quantity", example = "100", required = true)
        private BigDecimal pickedQuantity;
        
        @NotNull(message = "Accepted quantity is required")
        @DecimalMin(value = "0", message = "Accepted quantity cannot be negative")
        @Schema(description = "Accepted quantity", example = "80", required = true)
        private BigDecimal acceptedQuantity;
        
        @Schema(description = "Return reason (required if accepted < picked)", 
                example = "CUSTOMER_REJECTION",
                allowableValues = {"CUSTOMER_REJECTION", "QUALITY_ISSUE", "OVERSTOCK", 
                                   "DAMAGE", "PRICING_DISPUTE", "EXPIRED", 
                                   "WRONG_PRODUCT", "OTHER"})
        private String returnReason;
        
        @Size(max = 500, message = "Notes must not exceed 500 characters")
        @Schema(description = "Optional notes about the return")
        private String notes;
    }
}
```

---

#### 3.2 Response DTO

**File:** `services/returns-service/returns-application/src/main/java/com/ccbsa/wms/returns/application/dto/command/HandlePartialOrderAcceptanceResponseDTO.java`

```java
package com.ccbsa.wms.returns.application.dto.command;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response for partial order acceptance")
public class HandlePartialOrderAcceptanceResponseDTO {
    
    @Schema(description = "Return ID", example = "850e8400-e29b-41d4-a716-446655440000")
    private UUID returnId;
    
    @Schema(description = "Order ID", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID orderId;
    
    @Schema(description = "Load number", example = "LOAD-2025-001")
    private String loadNumber;
    
    @Schema(description = "Return type", example = "PARTIAL")
    private String returnType;
    
    @Schema(description = "Return status", example = "INITIATED")
    private String status;
    
    @Schema(description = "Total number of lines", example = "5")
    private Integer totalLines;
    
    @Schema(description = "Number of lines with returns", example = "2")
    private Integer linesWithReturns;
    
    @Schema(description = "Total accepted quantity", example = "380")
    private BigDecimal totalAcceptedQuantity;
    
    @Schema(description = "Total returned quantity", example = "20")
    private BigDecimal totalReturnedQuantity;
    
    @Schema(description = "Return line items")
    private List<ReturnLineItemResponseDTO> lineItems;
    
    @Schema(description = "Return timestamp")
    private ZonedDateTime returnedAt;
    
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Return line item response")
    public static class ReturnLineItemResponseDTO {
        
        @Schema(description = "Line item ID")
        private UUID lineItemId;
        
        @Schema(description = "Product code", example = "PROD-001")
        private String productCode;
        
        @Schema(description = "Product description")
        private String productDescription;
        
        @Schema(description = "Ordered quantity", example = "100")
        private BigDecimal orderedQuantity;
        
        @Schema(description = "Picked quantity", example = "100")
        private BigDecimal pickedQuantity;
        
        @Schema(description = "Accepted quantity", example = "80")
        private BigDecimal acceptedQuantity;
        
        @Schema(description = "Returned quantity", example = "20")
        private BigDecimal returnedQuantity;
        
        @Schema(description = "Return reason", example = "CUSTOMER_REJECTION")
        private String returnReason;
    }
}
```

---

#### 3.3 Command Controller

**File:** `services/returns-service/returns-application/src/main/java/com/ccbsa/wms/returns/application/command/ReturnCommandController.java`

```java
package com.ccbsa.wms.returns.application.command;

import com.ccbsa.common.application.context.TenantContext;
import com.ccbsa.common.application.context.UserContext;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.returns.application.dto.command.HandlePartialOrderAcceptanceRequestDTO;
import com.ccbsa.wms.returns.application.dto.command.HandlePartialOrderAcceptanceResponseDTO;
import com.ccbsa.wms.returns.application.dto.mapper.ReturnDTOMapper;
import com.ccbsa.wms.returns.application.service.command.HandlePartialOrderAcceptanceCommandHandler;
import com.ccbsa.wms.returns.application.service.command.dto.HandlePartialOrderAcceptanceCommand;
import com.ccbsa.wms.returns.application.service.command.dto.HandlePartialOrderAcceptanceResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@Slf4j
@RestController
@RequestMapping("/api/v1/returns")
@RequiredArgsConstructor
@Validated
@Tag(name = "Returns Management", description = "Returns management command operations")
public class ReturnCommandController {
    
    private final HandlePartialOrderAcceptanceCommandHandler handlePartialOrderAcceptanceCommandHandler;
    private final ReturnDTOMapper returnDTOMapper;
    
    @PostMapping("/partial-acceptance")
    @PreAuthorize("hasAuthority('ROLE_WAREHOUSE_OPERATOR')")
    @Operation(
        summary = "Handle partial order acceptance",
        description = "Process partial order acceptance when customer accepts only part of their order"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Partial acceptance processed successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "404", description = "Order not found"),
        @ApiResponse(responseCode = "409", description = "Order picking not completed")
    })
    public ResponseEntity<HandlePartialOrderAcceptanceResponseDTO> handlePartialOrderAcceptance(
            @Valid @RequestBody HandlePartialOrderAcceptanceRequestDTO requestDTO) {
        
        log.info("Received partial order acceptance request for order: {}", requestDTO.getOrderId());
        
        // Get tenant and user context
        TenantId tenantId = TenantContext.getTenantId();
        String userId = UserContext.getUserId();
        
        // Map request to command
        HandlePartialOrderAcceptanceCommand command = returnDTOMapper.toHandlePartialOrderAcceptanceCommand(requestDTO);
        
        // Handle command
        HandlePartialOrderAcceptanceResult result = handlePartialOrderAcceptanceCommandHandler.handle(
            command, 
            tenantId, 
            userId
        );
        
        // Map result to response
        HandlePartialOrderAcceptanceResponseDTO responseDTO = returnDTOMapper.toHandlePartialOrderAcceptanceResponseDTO(result);
        
        log.info("Partial order acceptance completed. Return ID: {}", result.getReturnId());
        
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDTO);
    }
}
```

---

#### 3.4 DTO Mapper

**File:** `services/returns-service/returns-application/src/main/java/com/ccbsa/wms/returns/application/dto/mapper/ReturnDTOMapper.java`

```java
package com.ccbsa.wms.returns.application.dto.mapper;

import com.ccbsa.wms.returns.application.dto.command.HandlePartialOrderAcceptanceRequestDTO;
import com.ccbsa.wms.returns.application.dto.command.HandlePartialOrderAcceptanceResponseDTO;
import com.ccbsa.wms.returns.application.service.command.dto.HandlePartialOrderAcceptanceCommand;
import com.ccbsa.wms.returns.application.service.command.dto.HandlePartialOrderAcceptanceResult;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class ReturnDTOMapper {
    
    public HandlePartialOrderAcceptanceCommand toHandlePartialOrderAcceptanceCommand(
            HandlePartialOrderAcceptanceRequestDTO requestDTO) {
        
        return HandlePartialOrderAcceptanceCommand.builder()
            .orderId(requestDTO.getOrderId())
            .loadNumber(requestDTO.getLoadNumber())
            .customerId(requestDTO.getCustomerId())
            .customerCode(requestDTO.getCustomerCode())
            .customerName(requestDTO.getCustomerName())
            .lineItems(requestDTO.getLineItems().stream()
                .map(lineItemDTO -> HandlePartialOrderAcceptanceCommand.PartialReturnLineItemCommand.builder()
                    .productId(lineItemDTO.getProductId())
                    .productCode(lineItemDTO.getProductCode())
                    .productDescription(lineItemDTO.getProductDescription())
                    .orderedQuantity(lineItemDTO.getOrderedQuantity())
                    .pickedQuantity(lineItemDTO.getPickedQuantity())
                    .acceptedQuantity(lineItemDTO.getAcceptedQuantity())
                    .returnReason(lineItemDTO.getReturnReason())
                    .notes(lineItemDTO.getNotes())
                    .build())
                .collect(Collectors.toList()))
            .signatureData(requestDTO.getSignatureData())
            .signerName(requestDTO.getSignerName())
            .signedAt(requestDTO.getSignedAt())
            .build();
    }
    
    public HandlePartialOrderAcceptanceResponseDTO toHandlePartialOrderAcceptanceResponseDTO(
            HandlePartialOrderAcceptanceResult result) {
        
        return HandlePartialOrderAcceptanceResponseDTO.builder()
            .returnId(result.getReturnId())
            .orderId(result.getOrderId())
            .loadNumber(result.getLoadNumber())
            .returnType(result.getReturnType().name())
            .status(result.getStatus().name())
            .totalLines(result.getTotalLines())
            .linesWithReturns(result.getLinesWithReturns())
            .totalAcceptedQuantity(result.getTotalAcceptedQuantity())
            .totalReturnedQuantity(result.getTotalReturnedQuantity())
            .lineItems(result.getLineItems().stream()
                .map(lineItemResult -> HandlePartialOrderAcceptanceResponseDTO.ReturnLineItemResponseDTO.builder()
                    .lineItemId(lineItemResult.getLineItemId())
                    .productCode(lineItemResult.getProductCode())
                    .productDescription(lineItemResult.getProductDescription())
                    .orderedQuantity(lineItemResult.getOrderedQuantity())
                    .pickedQuantity(lineItemResult.getPickedQuantity())
                    .acceptedQuantity(lineItemResult.getAcceptedQuantity())
                    .returnedQuantity(lineItemResult.getReturnedQuantity())
                    .returnReason(lineItemResult.getReturnReason())
                    .build())
                .collect(Collectors.toList()))
            .returnedAt(result.getReturnedAt())
            .build();
    }
}
```

---

## Frontend Implementation

### React Hooks

#### useHandlePartialOrderAcceptance Hook

**File:** `frontend-app/src/features/returns/hooks/useHandlePartialOrderAcceptance.ts`

```typescript
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { partialOrderAcceptanceService } from '../services/partialOrderAcceptanceService';
import { HandlePartialOrderAcceptanceRequest } from '../types/partialOrderAcceptanceTypes';
import { useSnackbar } from 'notistack';

export const useHandlePartialOrderAcceptance = () => {
  const queryClient = useQueryClient();
  const { enqueueSnackbar } = useSnackbar();

  return useMutation({
    mutationFn: (request: HandlePartialOrderAcceptanceRequest) =>
      partialOrderAcceptanceService.handlePartialOrderAcceptance(request),
    onSuccess: (data) => {
      // Invalidate relevant queries
      queryClient.invalidateQueries({ queryKey: ['returns'] });
      queryClient.invalidateQueries({ queryKey: ['orders'] });
      
      enqueueSnackbar('Partial order acceptance processed successfully', {
        variant: 'success',
      });
    },
    onError: (error: any) => {
      const errorMessage = error.response?.data?.message || 
                          'Failed to process partial order acceptance';
      enqueueSnackbar(errorMessage, {
        variant: 'error',
      });
    },
  });
};
```

---

### API Service

**File:** `frontend-app/src/features/returns/services/partialOrderAcceptanceService.ts`

```typescript
import apiClient from '../../../services/apiClient';
import {
  HandlePartialOrderAcceptanceRequest,
  HandlePartialOrderAcceptanceResponse,
} from '../types/partialOrderAcceptanceTypes';

class PartialOrderAcceptanceService {
  async handlePartialOrderAcceptance(
    request: HandlePartialOrderAcceptanceRequest
  ): Promise<HandlePartialOrderAcceptanceResponse> {
    const response = await apiClient.post<HandlePartialOrderAcceptanceResponse>(
      '/api/v1/returns/partial-acceptance',
      request
    );
    return response.data;
  }
}

export const partialOrderAcceptanceService = new PartialOrderAcceptanceService();
```

---

### TypeScript Types

**File:** `frontend-app/src/features/returns/types/partialOrderAcceptanceTypes.ts`

```typescript
export interface HandlePartialOrderAcceptanceRequest {
  orderId: string;
  loadNumber: string;
  customerId: string;
  customerCode: string;
  customerName?: string;
  lineItems: PartialReturnLineItemRequest[];
  signatureData: string;
  signerName?: string;
  signedAt: string;
}

export interface PartialReturnLineItemRequest {
  productId: string;
  productCode: string;
  productDescription?: string;
  orderedQuantity: number;
  pickedQuantity: number;
  acceptedQuantity: number;
  returnReason?: string;
  notes?: string;
}

export interface HandlePartialOrderAcceptanceResponse {
  returnId: string;
  orderId: string;
  loadNumber: string;
  returnType: 'PARTIAL' | 'FULL';
  status: 'INITIATED' | 'PROCESSING' | 'LOCATION_ASSIGNED' | 'COMPLETED' | 'RECONCILED' | 'CANCELLED';
  totalLines: number;
  linesWithReturns: number;
  totalAcceptedQuantity: number;
  totalReturnedQuantity: number;
  lineItems: ReturnLineItemResponse[];
  returnedAt: string;
}

export interface ReturnLineItemResponse {
  lineItemId: string;
  productCode: string;
  productDescription?: string;
  orderedQuantity: number;
  pickedQuantity: number;
  acceptedQuantity: number;
  returnedQuantity: number;
  returnReason?: string;
}
```

---

## Testing Strategy

### Unit Tests

#### Domain Core Tests

**Test File:** `Return.test.java`

```java
@Test
void initiatePartialReturn_WithValidData_ShouldCreateReturn() {
    // Arrange
    ReturnId returnId = ReturnId.newId();
    OrderId orderId = OrderId.newId();
    LoadNumber loadNumber = LoadNumber.of("LOAD-001");
    // ... other test data
    
    // Act
    Return returnAggregate = Return.initiatePartialReturn(
        returnId, orderId, loadNumber, customerId, customerInfo,
        lineItems, customerSignature, tenantId, "user-1"
    );
    
    // Assert
    assertThat(returnAggregate).isNotNull();
    assertThat(returnAggregate.getStatus()).isEqualTo(ReturnStatus.INITIATED);
    assertThat(returnAggregate.getReturnType()).isEqualTo(ReturnType.PARTIAL);
}

@Test
void initiatePartialReturn_WithAllItemsReturned_ShouldThrowException() {
    // Arrange - all accepted quantities = 0
    
    // Act & Assert
    assertThatThrownBy(() -> Return.initiatePartialReturn(...))
        .isInstanceOf(InvalidReturnException.class)
        .hasMessageContaining("Partial return must have both accepted and returned items");
}
```

---

## Acceptance Criteria Validation

### AC1: System records accepted quantities per order line ✅

**Implementation:**
- `ReturnLineItem` stores `acceptedQuantity`
- `HandlePartialOrderAcceptanceCommand` includes accepted quantities per line
- Database persistence via `ReturnEntity` and `ReturnLineItemEntity`

### AC2: System identifies returned quantities ✅

**Implementation:**
- `ReturnLineItem.calculateReturnedQuantity()` computes: picked - accepted
- Returned quantity automatically calculated and validated
- Stored in domain model and database

### AC3: System updates order status accordingly ✅

**Implementation:**
- `ReturnInitiatedEvent` published after successful creation
- Stock Management Service listens and updates stock levels
- Order status updated via event choreography

### AC4: System initiates returns process for unaccepted items ✅

**Implementation:**
- `Return.initiatePartialReturn()` creates Return aggregate
- Line items with returnedQuantity > 0 flagged for return processing
- Return reason required for returned items

### AC5: System publishes ReturnInitiatedEvent for partial returns ✅

**Implementation:**
- Event published in `Return.initiatePartialReturn()`
- Event includes return metadata (totals, line counts)
- Event consumed by Stock Management, Location Management, Notification services

### AC6: System records customer signature and acceptance timestamp ✅

**Implementation:**
- `CustomerSignature` value object stores signature data (Base64) and timestamp
- Validation ensures signature is not null or empty
- Signature stored in database via `ReturnEntity`

### AC7: System supports multiple product lines per order ✅

**Implementation:**
- `Return` aggregate contains `List<ReturnLineItem>`
- Each line item represents one product
- Validation ensures at least one line item exists

---

## Implementation Checklist

### Domain Core ✅
- [x] Return aggregate
- [x] ReturnLineItem entity
- [x] Value objects (ReturnId, ReturnStatus, ReturnReason, CustomerSignature)
- [x] Domain events (ReturnInitiatedEvent)
- [x] Business logic validation

### Application Service ✅
- [x] HandlePartialOrderAcceptanceCommand
- [x] HandlePartialOrderAcceptanceResult
- [x] HandlePartialOrderAcceptanceCommandHandler
- [x] ReturnRepository port
- [x] PickingServicePort port

### Application Layer ✅
- [x] HandlePartialOrderAcceptanceRequestDTO
- [x] HandlePartialOrderAcceptanceResponseDTO
- [x] ReturnCommandController
- [x] ReturnDTOMapper
- [x] OpenAPI documentation

### Data Access (Covered in separate implementation)
- [ ] ReturnEntity
- [ ] ReturnLineItemEntity
- [ ] ReturnRepositoryAdapter
- [ ] JPA mappings

### Frontend ✅
- [x] PartialOrderAcceptancePage component
- [x] useHandlePartialOrderAcceptance hook
- [x] partialOrderAcceptanceService
- [x] TypeScript types
- [x] Form validation
- [x] Signature capture

### Testing
- [ ] Domain core unit tests
- [ ] Command handler unit tests
- [ ] REST controller integration tests
- [ ] Frontend component tests
- [ ] E2E tests

### Documentation ✅
- [x] Implementation plan
- [x] API documentation (OpenAPI)
- [x] Architecture decisions

---

**End of US-7.1.1 Implementation Plan**
