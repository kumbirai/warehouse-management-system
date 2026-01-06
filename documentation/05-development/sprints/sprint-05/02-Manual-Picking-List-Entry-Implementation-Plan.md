# Manual Picking List Entry Implementation Plan

## US-6.1.2: Manual Picking List Entry via UI

**Service:** Picking Service
**Priority:** Must Have
**Story Points:** 8
**Sprint:** Sprint 5

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
**I want** to manually enter picking lists through the UI
**So that** I can create individual picking list records or correct data

### Business Requirements

- System provides form-based UI for picking list data entry
- Form includes fields: load number, order numbers, customer information, products, quantities, priorities
- System validates required fields and data formats in real-time
- System supports adding multiple orders to a single load
- System supports adding multiple products per order
- System validates product codes against master data
- System provides autocomplete/suggestions for product codes and customer information
- System allows saving draft picking lists for later completion
- System provides clear validation error messages
- System publishes `PickingListReceivedEvent` after successful entry

### Technical Requirements

- Follow DDD, Clean Hexagonal Architecture, CQRS, Event-Driven Choreography
- Pure Java domain core (no framework dependencies)
- Real-time validation with debouncing
- Multi-tenant support
- Optimistic UI updates
- Draft saving with localStorage backup
- Accessibility (WCAG 2.1 Level AA)

---

## UI Design

### Picking List Entry Form

**Component:** `PickingListForm.tsx`

**Features:**

- **Multi-Step Form** - Step 1: Load Info, Step 2: Orders, Step 3: Review
- **Dynamic Order Management** - Add/remove orders within a load
- **Dynamic Line Items** - Add/remove products per order
- **Product Autocomplete** - Search and select products with real-time validation
- **Customer Autocomplete** - Search and select customers
- **Draft Saving** - Auto-save drafts every 30 seconds
- **Validation** - Real-time field validation with clear error messages
- **Accessibility** - Keyboard navigation, screen reader support, ARIA labels

**UI Flow:**

1. User navigates to "Create Picking List" page
2. System displays **Step 1: Load Information**
   - Load Number (auto-generated or manual)
   - Notes (optional)
   - Action: Next
3. User clicks "Next"
4. System displays **Step 2: Add Orders**
   - Order list (initially empty)
   - "Add Order" button
5. User clicks "Add Order"
6. System displays order form:
   - Order Number (auto-generated or manual)
   - Customer Code (autocomplete)
   - Customer Name (auto-filled from customer code)
   - Priority (dropdown: HIGH, NORMAL, LOW)
   - Line Items section (initially empty)
   - "Add Product" button
7. User clicks "Add Product"
8. System displays line item form:
   - Product Code (autocomplete with validation)
   - Product Description (auto-filled)
   - Quantity (number input)
   - Notes (optional)
   - Remove button
9. User enters product details and quantity
10. User can add more products or orders
11. User clicks "Review"
12. System displays **Step 3: Review & Submit**
    - Summary of load, orders, and line items
    - Total quantities
    - Validation status
    - Actions: Back, Save Draft, Submit
13. User clicks "Submit"
14. System validates and creates picking list
15. System displays success message with picking list ID
16. User can view picking list or create another

**Step 1: Load Information**

```typescript
<Paper sx={{ p: 3 }}>
  <Typography variant="h6" gutterBottom>
    Load Information
  </Typography>

  <Grid container spacing={3}>
    <Grid item xs={12} md={6}>
      <TextField
        fullWidth
        label="Load Number"
        name="loadNumber"
        value={formData.loadNumber}
        onChange={handleLoadNumberChange}
        error={!!errors.loadNumber}
        helperText={errors.loadNumber || "Auto-generated if left empty"}
        InputProps={{
          endAdornment: (
            <InputAdornment position="end">
              <Tooltip title="Auto-generate">
                <IconButton onClick={handleGenerateLoadNumber}>
                  <AutorenewIcon />
                </IconButton>
              </Tooltip>
            </InputAdornment>
          ),
        }}
      />
    </Grid>

    <Grid item xs={12}>
      <TextField
        fullWidth
        multiline
        rows={3}
        label="Notes"
        name="notes"
        value={formData.notes}
        onChange={handleNotesChange}
        inputProps={{ maxLength: 500 }}
        helperText={`${formData.notes.length}/500 characters`}
      />
    </Grid>
  </Grid>

  <Box sx={{ mt: 3, display: 'flex', justifyContent: 'flex-end' }}>
    <Button
      variant="contained"
      onClick={handleNext}
      disabled={!isStep1Valid}
    >
      Next: Add Orders
    </Button>
  </Box>
</Paper>
```

**Step 2: Orders and Line Items**

```typescript
<Paper sx={{ p: 3 }}>
  <Typography variant="h6" gutterBottom>
    Orders
  </Typography>

  {orders.map((order, orderIndex) => (
    <Card key={order.tempId} sx={{ mb: 3, border: 1, borderColor: 'divider' }}>
      <CardHeader
        title={`Order ${orderIndex + 1}: ${order.orderNumber || 'New Order'}`}
        action={
          <IconButton onClick={() => handleRemoveOrder(orderIndex)}>
            <DeleteIcon />
          </IconButton>
        }
      />
      <CardContent>
        <Grid container spacing={2}>
          {/* Order Number */}
          <Grid item xs={12} md={6}>
            <TextField
              fullWidth
              label="Order Number"
              value={order.orderNumber}
              onChange={(e) => handleOrderFieldChange(orderIndex, 'orderNumber', e.target.value)}
              error={!!orderErrors[orderIndex]?.orderNumber}
              helperText={orderErrors[orderIndex]?.orderNumber || "Auto-generated if left empty"}
              InputProps={{
                endAdornment: (
                  <InputAdornment position="end">
                    <Tooltip title="Auto-generate">
                      <IconButton onClick={() => handleGenerateOrderNumber(orderIndex)}>
                        <AutorenewIcon />
                      </IconButton>
                    </Tooltip>
                  </InputAdornment>
                ),
              }}
            />
          </Grid>

          {/* Customer Code Autocomplete */}
          <Grid item xs={12} md={6}>
            <Autocomplete
              freeSolo
              options={customerOptions}
              loading={loadingCustomers}
              value={order.customerCode}
              onInputChange={(event, newValue) => {
                handleCustomerSearch(newValue);
                handleOrderFieldChange(orderIndex, 'customerCode', newValue);
              }}
              onChange={(event, newValue) => {
                if (typeof newValue === 'object' && newValue) {
                  handleCustomerSelect(orderIndex, newValue);
                }
              }}
              renderInput={(params) => (
                <TextField
                  {...params}
                  label="Customer Code"
                  error={!!orderErrors[orderIndex]?.customerCode}
                  helperText={orderErrors[orderIndex]?.customerCode}
                  InputProps={{
                    ...params.InputProps,
                    endAdornment: (
                      <>
                        {loadingCustomers ? <CircularProgress size={20} /> : null}
                        {params.InputProps.endAdornment}
                      </>
                    ),
                  }}
                />
              )}
              renderOption={(props, option) => (
                <li {...props}>
                  <Box>
                    <Typography variant="body2">{option.code}</Typography>
                    <Typography variant="caption" color="textSecondary">
                      {option.name}
                    </Typography>
                  </Box>
                </li>
              )}
            />
          </Grid>

          {/* Customer Name (auto-filled) */}
          <Grid item xs={12} md={6}>
            <TextField
              fullWidth
              label="Customer Name"
              value={order.customerName}
              onChange={(e) => handleOrderFieldChange(orderIndex, 'customerName', e.target.value)}
              disabled={!!order.customerCode}
              helperText={order.customerCode ? "Auto-filled from customer code" : ""}
            />
          </Grid>

          {/* Priority */}
          <Grid item xs={12} md={6}>
            <FormControl fullWidth>
              <InputLabel>Priority</InputLabel>
              <Select
                value={order.priority}
                onChange={(e) => handleOrderFieldChange(orderIndex, 'priority', e.target.value)}
                label="Priority"
              >
                <MenuItem value="HIGH">
                  <Chip label="HIGH" color="error" size="small" sx={{ mr: 1 }} />
                  High Priority
                </MenuItem>
                <MenuItem value="NORMAL">
                  <Chip label="NORMAL" color="primary" size="small" sx={{ mr: 1 }} />
                  Normal Priority
                </MenuItem>
                <MenuItem value="LOW">
                  <Chip label="LOW" color="default" size="small" sx={{ mr: 1 }} />
                  Low Priority
                </MenuItem>
              </Select>
            </FormControl>
          </Grid>

          {/* Line Items */}
          <Grid item xs={12}>
            <Divider sx={{ my: 2 }} />
            <Typography variant="subtitle1" gutterBottom>
              Products
            </Typography>

            {order.lineItems.map((lineItem, lineItemIndex) => (
              <Card key={lineItem.tempId} variant="outlined" sx={{ mb: 2 }}>
                <CardContent>
                  <Grid container spacing={2} alignItems="center">
                    {/* Product Code Autocomplete */}
                    <Grid item xs={12} md={5}>
                      <Autocomplete
                        freeSolo
                        options={productOptions}
                        loading={loadingProducts}
                        value={lineItem.productCode}
                        onInputChange={(event, newValue) => {
                          handleProductSearch(newValue);
                          handleLineItemFieldChange(orderIndex, lineItemIndex, 'productCode', newValue);
                        }}
                        onChange={(event, newValue) => {
                          if (typeof newValue === 'object' && newValue) {
                            handleProductSelect(orderIndex, lineItemIndex, newValue);
                          }
                        }}
                        renderInput={(params) => (
                          <TextField
                            {...params}
                            label="Product Code"
                            error={!!lineItemErrors[orderIndex]?.[lineItemIndex]?.productCode}
                            helperText={lineItemErrors[orderIndex]?.[lineItemIndex]?.productCode}
                            InputProps={{
                              ...params.InputProps,
                              endAdornment: (
                                <>
                                  {loadingProducts ? <CircularProgress size={20} /> : null}
                                  {params.InputProps.endAdornment}
                                </>
                              ),
                            }}
                          />
                        )}
                        renderOption={(props, option) => (
                          <li {...props}>
                            <Box>
                              <Typography variant="body2">{option.code}</Typography>
                              <Typography variant="caption" color="textSecondary">
                                {option.description}
                              </Typography>
                            </Box>
                          </li>
                        )}
                      />
                    </Grid>

                    {/* Product Description (auto-filled) */}
                    <Grid item xs={12} md={4}>
                      <TextField
                        fullWidth
                        label="Product Description"
                        value={lineItem.productDescription}
                        disabled
                        helperText="Auto-filled from product code"
                      />
                    </Grid>

                    {/* Quantity */}
                    <Grid item xs={12} md={2}>
                      <TextField
                        fullWidth
                        type="number"
                        label="Quantity"
                        value={lineItem.quantity}
                        onChange={(e) => handleLineItemFieldChange(orderIndex, lineItemIndex, 'quantity', e.target.value)}
                        error={!!lineItemErrors[orderIndex]?.[lineItemIndex]?.quantity}
                        helperText={lineItemErrors[orderIndex]?.[lineItemIndex]?.quantity}
                        inputProps={{ min: 0, step: 1 }}
                      />
                    </Grid>

                    {/* Remove Button */}
                    <Grid item xs={12} md={1}>
                      <IconButton
                        color="error"
                        onClick={() => handleRemoveLineItem(orderIndex, lineItemIndex)}
                        disabled={order.lineItems.length === 1}
                      >
                        <DeleteIcon />
                      </IconButton>
                    </Grid>

                    {/* Notes */}
                    <Grid item xs={12}>
                      <TextField
                        fullWidth
                        multiline
                        rows={2}
                        label="Notes"
                        value={lineItem.notes}
                        onChange={(e) => handleLineItemFieldChange(orderIndex, lineItemIndex, 'notes', e.target.value)}
                        inputProps={{ maxLength: 200 }}
                        helperText={`${lineItem.notes?.length || 0}/200 characters`}
                      />
                    </Grid>
                  </Grid>
                </CardContent>
              </Card>
            ))}

            <Button
              variant="outlined"
              startIcon={<AddIcon />}
              onClick={() => handleAddLineItem(orderIndex)}
            >
              Add Product
            </Button>
          </Grid>
        </Grid>
      </CardContent>
    </Card>
  ))}

  <Button
    variant="outlined"
    startIcon={<AddIcon />}
    onClick={handleAddOrder}
    fullWidth
    sx={{ mb: 2 }}
  >
    Add Another Order
  </Button>

  <Box sx={{ mt: 3, display: 'flex', justifyContent: 'space-between' }}>
    <Button onClick={handleBack}>
      Back
    </Button>
    <Box sx={{ display: 'flex', gap: 2 }}>
      <Button
        variant="outlined"
        onClick={handleSaveDraft}
        startIcon={<SaveIcon />}
      >
        Save Draft
      </Button>
      <Button
        variant="contained"
        onClick={handleReview}
        disabled={!isStep2Valid}
      >
        Review
      </Button>
    </Box>
  </Box>
</Paper>
```

**Step 3: Review and Submit**

```typescript
<Paper sx={{ p: 3 }}>
  <Typography variant="h6" gutterBottom>
    Review Picking List
  </Typography>

  {/* Load Summary */}
  <Card variant="outlined" sx={{ mb: 3 }}>
    <CardHeader title="Load Information" />
    <CardContent>
      <Grid container spacing={2}>
        <Grid item xs={12} md={6}>
          <Typography variant="body2" color="textSecondary">Load Number</Typography>
          <Typography variant="body1">{formData.loadNumber || 'Auto-generated'}</Typography>
        </Grid>
        <Grid item xs={12} md={6}>
          <Typography variant="body2" color="textSecondary">Total Orders</Typography>
          <Typography variant="body1">{orders.length}</Typography>
        </Grid>
        {formData.notes && (
          <Grid item xs={12}>
            <Typography variant="body2" color="textSecondary">Notes</Typography>
            <Typography variant="body1">{formData.notes}</Typography>
          </Grid>
        )}
      </Grid>
    </CardContent>
  </Card>

  {/* Orders Summary */}
  {orders.map((order, orderIndex) => (
    <Card key={order.tempId} variant="outlined" sx={{ mb: 2 }}>
      <CardHeader
        title={`Order ${orderIndex + 1}: ${order.orderNumber || 'Auto-generated'}`}
        subheader={`${order.customerCode} - ${order.customerName}`}
        action={
          <Chip
            label={order.priority}
            color={order.priority === 'HIGH' ? 'error' : order.priority === 'NORMAL' ? 'primary' : 'default'}
            size="small"
          />
        }
      />
      <CardContent>
        <TableContainer>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Product Code</TableCell>
                <TableCell>Description</TableCell>
                <TableCell align="right">Quantity</TableCell>
                <TableCell>Notes</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {order.lineItems.map((lineItem, index) => (
                <TableRow key={lineItem.tempId}>
                  <TableCell>{lineItem.productCode}</TableCell>
                  <TableCell>{lineItem.productDescription}</TableCell>
                  <TableCell align="right">{lineItem.quantity}</TableCell>
                  <TableCell>{lineItem.notes || '-'}</TableCell>
                </TableRow>
              ))}
              <TableRow>
                <TableCell colSpan={2} align="right">
                  <Typography variant="subtitle2">Total Items:</Typography>
                </TableCell>
                <TableCell align="right">
                  <Typography variant="subtitle2">
                    {order.lineItems.reduce((sum, item) => sum + Number(item.quantity), 0)}
                  </Typography>
                </TableCell>
                <TableCell />
              </TableRow>
            </TableBody>
          </Table>
        </TableContainer>
      </CardContent>
    </Card>
  ))}

  {/* Validation Status */}
  {validationStatus && (
    <Alert
      severity={validationStatus.severity}
      sx={{ mb: 3 }}
    >
      <AlertTitle>{validationStatus.title}</AlertTitle>
      {validationStatus.message}
      {validationStatus.errors && validationStatus.errors.length > 0 && (
        <ul>
          {validationStatus.errors.map((error, index) => (
            <li key={index}>{error}</li>
          ))}
        </ul>
      )}
    </Alert>
  )}

  <Box sx={{ mt: 3, display: 'flex', justifyContent: 'space-between' }}>
    <Button onClick={handleBack}>
      Back to Edit
    </Button>
    <Box sx={{ display: 'flex', gap: 2 }}>
      <Button
        variant="outlined"
        onClick={handleSaveDraft}
        startIcon={<SaveIcon />}
      >
        Save Draft
      </Button>
      <Button
        variant="contained"
        onClick={handleSubmit}
        disabled={submitting || !isFormValid}
        startIcon={submitting ? <CircularProgress size={20} /> : <CheckIcon />}
      >
        {submitting ? 'Creating...' : 'Create Picking List'}
      </Button>
    </Box>
  </Box>
</Paper>
```

**Draft Management:**

```typescript
// Auto-save draft every 30 seconds
useEffect(() => {
  const autoSaveInterval = setInterval(() => {
    if (isDirty && !submitting) {
      saveDraftToLocalStorage();
    }
  }, 30000); // 30 seconds

  return () => clearInterval(autoSaveInterval);
}, [isDirty, submitting]);

// Load draft on mount
useEffect(() => {
  const draft = loadDraftFromLocalStorage();
  if (draft) {
    setShowDraftDialog(true);
    setSavedDraft(draft);
  }
}, []);

// Draft dialog
<Dialog open={showDraftDialog} onClose={() => setShowDraftDialog(false)}>
  <DialogTitle>Draft Found</DialogTitle>
  <DialogContent>
    <Typography>
      A draft picking list was found from {formatDateTime(savedDraft?.savedAt)}.
      Would you like to continue editing it?
    </Typography>
  </DialogContent>
  <DialogActions>
    <Button onClick={handleDiscardDraft}>Discard</Button>
    <Button onClick={handleLoadDraft} variant="contained">Load Draft</Button>
  </DialogActions>
</Dialog>
```

---

## Domain Model Design

### Command DTOs

**CreatePickingListCommand**

```java
package com.ccbsa.wms.picking.application.service.command;

import com.ccbsa.common.domain.valueobject.TenantId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class CreatePickingListCommand {
    private final TenantId tenantId;
    private final String loadNumber;
    private final String notes;
    private final List<OrderCommand> orders;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class OrderCommand {
        private final String orderNumber;
        private final String customerCode;
        private final String customerName;
        private final String priority;
        private final List<LineItemCommand> lineItems;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class LineItemCommand {
        private final String productCode;
        private final String quantity;
        private final String notes;
    }
}
```

**CreatePickingListResponse**

```java
package com.ccbsa.wms.picking.application.service.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class CreatePickingListResponse {
    private final UUID pickingListId;
    private final String loadNumber;
    private final String message;
}
```

---

## Backend Implementation

### Application Service Layer

**CreatePickingListCommandHandler**

```java
package com.ccbsa.wms.picking.application.service.command;

import com.ccbsa.common.domain.valueobject.*;
import com.ccbsa.wms.picking.application.service.port.data.PickingListRepository;
import com.ccbsa.wms.picking.application.service.port.event.PickingEventPublisher;
import com.ccbsa.wms.picking.application.service.port.service.ProductServicePort;
import com.ccbsa.wms.picking.domain.core.entity.*;
import com.ccbsa.wms.picking.domain.core.valueobject.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class CreatePickingListCommandHandler {
    private final PickingListRepository pickingListRepository;
    private final ProductServicePort productServicePort;
    private final PickingEventPublisher pickingEventPublisher;

    @Transactional
    public CreatePickingListResponse handle(CreatePickingListCommand command) {
        log.info("Creating picking list for tenant: {}", command.getTenantId());

        // Validate products
        validateProducts(command);

        // Generate load number if not provided
        String loadNumber = command.getLoadNumber() != null && !command.getLoadNumber().isBlank()
                ? command.getLoadNumber()
                : LoadNumber.generate().getValue();

        // Create Load with Orders
        Load load = createLoad(loadNumber, command.getOrders());

        // Create PickingList aggregate
        PickingList pickingList = PickingList.builder()
                .pickingListId(PickingListId.newId())
                .tenantId(command.getTenantId())
                .loads(List.of(load))
                .status(PickingListStatus.RECEIVED)
                .receivedAt(ZonedDateTime.now())
                .notes(command.getNotes())
                .build();

        // Validate and initialize
        pickingList.validatePickingList();
        pickingList.initializePickingList();

        // Save picking list
        PickingList savedPickingList = pickingListRepository.save(pickingList);

        // Publish domain events
        pickingEventPublisher.publishPickingListReceivedEvent(savedPickingList);

        log.info("Successfully created picking list: {}, load: {}",
                savedPickingList.getId().getValue(), loadNumber);

        return CreatePickingListResponse.builder()
                .pickingListId(savedPickingList.getId().getValue())
                .loadNumber(loadNumber)
                .message("Picking list created successfully")
                .build();
    }

    private void validateProducts(CreatePickingListCommand command) {
        // Extract all unique product codes
        List<String> productCodes = command.getOrders().stream()
                .flatMap(order -> order.getLineItems().stream())
                .map(CreatePickingListCommand.LineItemCommand::getProductCode)
                .distinct()
                .collect(Collectors.toList());

        // Validate all products exist
        productServicePort.validateProducts(productCodes);
    }

    private Load createLoad(String loadNumber, List<CreatePickingListCommand.OrderCommand> orderCommands) {
        List<Order> orders = new ArrayList<>();

        for (CreatePickingListCommand.OrderCommand orderCommand : orderCommands) {
            // Generate order number if not provided
            String orderNumber = orderCommand.getOrderNumber() != null && !orderCommand.getOrderNumber().isBlank()
                    ? orderCommand.getOrderNumber()
                    : OrderNumber.generate().getValue();

            // Create line items
            List<OrderLineItem> lineItems = orderCommand.getLineItems().stream()
                    .map(this::createLineItem)
                    .collect(Collectors.toList());

            // Create order
            Order order = Order.builder()
                    .orderId(OrderId.newId())
                    .orderNumber(new OrderNumber(orderNumber))
                    .customerInfo(new CustomerInfo(
                            orderCommand.getCustomerCode(),
                            orderCommand.getCustomerName()
                    ))
                    .priority(Priority.fromString(orderCommand.getPriority()))
                    .lineItems(lineItems)
                    .status(OrderStatus.PENDING)
                    .createdAt(ZonedDateTime.now())
                    .build();

            orders.add(order);
        }

        // Create load
        return Load.builder()
                .loadId(LoadId.newId())
                .loadNumber(new LoadNumber(loadNumber))
                .orders(orders)
                .status(LoadStatus.PENDING)
                .createdAt(ZonedDateTime.now())
                .build();
    }

    private OrderLineItem createLineItem(CreatePickingListCommand.LineItemCommand lineItemCommand) {
        return OrderLineItem.builder()
                .orderLineItemId(OrderLineItemId.newId())
                .productCode(new ProductCode(lineItemCommand.getProductCode()))
                .quantity(new Quantity(new BigDecimal(lineItemCommand.getQuantity())))
                .notes(lineItemCommand.getNotes())
                .build();
    }
}
```

### Service Port (Product Validation)

**ProductServicePort**

```java
package com.ccbsa.wms.picking.application.service.port.service;

import java.util.List;

public interface ProductServicePort {
    /**
     * Validate that all product codes exist in the product master data
     * @param productCodes List of product codes to validate
     * @throws ProductValidationException if any product code is invalid
     */
    void validateProducts(List<String> productCodes);
}
```

**ProductServiceAdapter** (Infrastructure Layer)

```java
package com.ccbsa.wms.picking.dataaccess.adapter;

import com.ccbsa.wms.picking.application.service.port.service.ProductServicePort;
import com.ccbsa.wms.picking.domain.core.exception.ProductValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductServiceAdapter implements ProductServicePort {
    private final RestTemplate restTemplate;
    private final String productServiceUrl = "http://product-service/api/v1/products";

    @Override
    public void validateProducts(List<String> productCodes) {
        log.debug("Validating {} product codes", productCodes.size());

        try {
            // Call Product Service validation endpoint
            String url = productServiceUrl + "/validate";
            restTemplate.postForObject(url, productCodes, Void.class);

            log.debug("All product codes validated successfully");
        } catch (Exception e) {
            log.error("Product validation failed", e);
            throw new ProductValidationException("Failed to validate product codes: " + e.getMessage());
        }
    }
}
```

---

## Frontend Implementation

### React Component Structure

```typescript
// src/features/picking-management/pages/PickingListCreatePage.tsx
export const PickingListCreatePage: React.FC = () => {
  const [activeStep, setActiveStep] = useState(0);
  const [formData, setFormData] = useState<PickingListFormData>(initialFormData);
  const [orders, setOrders] = useState<OrderFormData[]>([createEmptyOrder()]);
  const [errors, setErrors] = useState<FormErrors>({});
  const [isDirty, setIsDirty] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  const steps = ['Load Information', 'Orders & Products', 'Review & Submit'];

  // ... implementation
};

// src/features/picking-management/hooks/useCreatePickingList.ts
export const useCreatePickingList = () => {
  const apiClient = useApiClient();
  const { showSuccess, showError } = useToast();

  return useMutation({
    mutationFn: async (data: CreatePickingListRequest) => {
      const response = await apiClient.post('/picking/picking-lists', data);
      return response.data;
    },
    onSuccess: (data) => {
      showSuccess(`Picking list created successfully: ${data.loadNumber}`);
    },
    onError: (error) => {
      showError('Failed to create picking list');
    },
  });
};
```

---

## Data Flow

```
User fills form (Step 1: Load Info)
  ↓
User clicks "Next"
  ↓
Validation: Load Number format
  ↓
User fills form (Step 2: Orders)
  ↓
User searches Product Code
  ↓
Debounced API call to Product Service
  ↓
Autocomplete suggestions displayed
  ↓
User selects product
  ↓
Product description auto-filled
  ↓
User adds multiple products, orders
  ↓
Auto-save draft every 30s (localStorage)
  ↓
User clicks "Review"
  ↓
Validation: All required fields
  ↓
Display summary (Step 3: Review)
  ↓
User clicks "Create Picking List"
  ↓
Frontend → POST /api/v1/picking/picking-lists
  ↓
Gateway Service
  ↓
Picking Service (Command Controller)
  ↓
CreatePickingListCommandHandler
  ↓
Validate products (Product Service)
  ↓
Generate Load/Order numbers if needed
  ↓
Create PickingList aggregate
  ↓
PickingList.initializePickingList()
  ↓
PickingListReceivedEvent
  ↓
Save to database
  ↓
Publish event to Kafka
  ↓
Return response
  ↓
Frontend displays success
  ↓
Clear draft from localStorage
  ↓
Navigate to picking list detail
```

---

## Testing Strategy

### Unit Tests

**Frontend:**
- Form validation logic
- Draft save/load logic
- Autocomplete search debouncing
- Product/customer selection handling

**Backend:**
- CreatePickingListCommandHandler
- Product validation
- Load number generation
- Order number generation
- Value object validation

### Integration Tests

- Product Service integration
- Database persistence
- Event publishing
- Draft recovery

### E2E Tests

- Complete form submission flow
- Draft save and recovery
- Validation error handling
- Product autocomplete
- Multi-order creation

---

## Acceptance Criteria Validation

| Acceptance Criteria | Implementation | Status |
|---------------------|----------------|--------|
| AC1: Form-based UI for picking list entry | Multi-step form component | ✅ Planned |
| AC2: Required fields included | All fields in form | ✅ Planned |
| AC3: Real-time validation | Field-level validation with debouncing | ✅ Planned |
| AC4: Multiple orders per load | Dynamic order management | ✅ Planned |
| AC5: Multiple products per order | Dynamic line items | ✅ Planned |
| AC6: Product validation against master data | Product Service integration | ✅ Planned |
| AC7: Autocomplete for products/customers | Autocomplete components | ✅ Planned |
| AC8: Draft saving | localStorage + backend | ✅ Planned |
| AC9: Clear validation errors | Field-level error display | ✅ Planned |
| AC10: Publish PickingListReceivedEvent | Event publishing | ✅ Planned |

---

**Document Control**

- **Version:** 1.0
- **Date:** 2025-01
- **Status:** Draft
