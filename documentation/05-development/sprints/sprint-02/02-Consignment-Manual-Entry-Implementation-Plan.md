# Consignment Manual Entry Implementation Plan

## US-1.1.2: Manual Consignment Data Entry via UI

**Service:** Stock Management Service  
**Priority:** Must Have  
**Story Points:** 8  
**Sprint:** Sprint 2

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
**I want** to manually enter consignment data through the UI  
**So that** I can create individual consignment records or correct data

### Business Requirements

- Provide form-based UI for consignment data entry
- Form includes fields: consignment reference, warehouse ID, line items (product, quantity, expiration date)
- Validate required fields and data formats in real-time
- Validate product codes exist in product master data
- Support adding multiple line items per consignment
- Support product barcode scanning for product identification
- Provide clear validation error messages
- Allow saving draft consignments for later completion
- Publish `StockConsignmentReceivedEvent` after successful entry

### Technical Requirements

- Follow DDD, Clean Hexagonal Architecture, CQRS, Event-Driven Choreography
- Multi-tenant support (tenant isolation)
- Real-time validation (client and server)
- Draft saving using localStorage
- Event publishing for consignment creation
- REST API with proper error handling
- Frontend form with comprehensive validation
- Product barcode scanning integration

---

## UI Design

### Consignment Creation Form

**Component:** `ConsignmentCreationForm.tsx`

**Fields:**

- **Consignment Reference** (required, text input with uniqueness check)
- **Warehouse ID** (required, dropdown or text input)
- **Received Date** (required, datetime picker, default: now)
- **Received By** (optional, text input)
- **Line Items** (required, dynamic list):
    - Product Code or Barcode (required, **BarcodeInput component - scan first, manual input as fallback**)
    - Quantity (required, number input, must be positive)
    - Expiration Date (optional, date picker)
    - Batch Number (optional, text input)

**Validation:**

- Real-time validation for all fields
- Consignment reference uniqueness check (debounced API call)
- Product code/barcode validation (synchronous call to Product Service)
- Quantity validation (positive number)
- Date validation (expiration in future, received not in future)
- Clear error messages for each field

**Actions:**

- **Save Draft** - Save to localStorage for later completion
- **Add Line Item** - Add another line item row
- **Remove Line Item** - Remove a line item
- **Scan Barcode** - Open barcode scanner for product identification
- **Create Consignment** - Submit form to create consignment
- **Cancel** - Navigate back to consignment list

**UI Flow:**

1. User navigates to "Stock Management" → "Consignments" → "Create Consignment"
2. Form displays with consignment header fields
3. User enters consignment reference (system checks uniqueness in real-time)
4. User selects warehouse ID
5. User adds line items:
    - **Primary:** Scans product barcode (handheld scanner or camera)
    - **Fallback:** Enters product code manually if scanning fails
    - System validates product exists and displays product name
    - User enters quantity
    - User optionally enters expiration date and batch number
6. User can add more line items
7. User can save as draft at any time
8. User clicks "Create Consignment"
9. System validates and creates consignment
10. Success message displayed
11. User redirected to consignment detail page

### Barcode Scanner Integration (Barcode-First Principle)

**Component:** `BarcodeInput` (from `@/components/common`)

**Barcode-First Implementation:**

- **Primary Method:** Barcode scanning
  - Handheld scanner support (USB/Bluetooth - acts as keyboard)
  - Camera-based scanning (ZXing library via camera button)
  - Auto-focus on barcode fields for optimal scanner UX
- **Fallback Method:** Manual keyboard input
  - Full keyboard support when scanning fails
  - Clear helper text: "Scan barcode first, or enter manually if scanning fails"
- **Features:**
  - Product lookup by barcode
  - Product information display
  - Error handling for invalid barcodes
  - Auto-population of product details when barcode is validated

---

## Domain Model Design

See [04-Stock-Consignment-Creation-Implementation-Plan.md](04-Stock-Consignment-Creation-Implementation-Plan.md) for complete domain model details.

---

## Backend Implementation

### Command Handler

```java
@Component
public class CreateConsignmentCommandHandler {
    
    private final StockConsignmentRepository repository;
    private final StockManagementEventPublisher eventPublisher;
    private final ProductServicePort productServicePort;
    private final ConsignmentValidationService validationService;
    
    @Transactional
    public CreateConsignmentResult handle(CreateConsignmentCommand command) {
        // 1. Validate consignment reference uniqueness
        validateConsignmentReference(
            command.getConsignmentReference(), 
            command.getTenantId()
        );
        
        // 2. Validate and create line items
        List<ConsignmentLineItem> lineItems = new ArrayList<>();
        for (CreateConsignmentLineItemCommand lineItemCmd : command.getLineItems()) {
            // Validate product exists
            ProductInfo productInfo = productServicePort.getProductByCode(
                lineItemCmd.getProductCode(),
                command.getTenantId()
            ).orElseThrow(() -> new ProductNotFoundException(lineItemCmd.getProductCode()));
            
            // Validate line item
            validationService.validateLineItem(lineItemCmd, productInfo);
            
            // Create line item
            ConsignmentLineItem lineItem = ConsignmentLineItem.builder()
                .productCode(ProductCode.of(lineItemCmd.getProductCode()))
                .quantity(Quantity.of(lineItemCmd.getQuantity()))
                .expirationDate(lineItemCmd.getExpirationDate() != null ? 
                    ExpirationDate.of(lineItemCmd.getExpirationDate()) : null)
                .batchNumber(lineItemCmd.getBatchNumber())
                .build();
            
            lineItems.add(lineItem);
        }
        
        // 3. Create consignment
        StockConsignment consignment = StockConsignment.builder()
            .consignmentId(ConsignmentId.generate())
            .tenantId(command.getTenantId())
            .consignmentReference(command.getConsignmentReference())
            .warehouseId(command.getWarehouseId())
            .receivedAt(command.getReceivedAt())
            .receivedBy(command.getReceivedBy())
            .lineItems(lineItems)
            .build();
        
        // 4. Persist
        repository.save(consignment);
        
        // 5. Publish events
        eventPublisher.publish(consignment.getDomainEvents());
        consignment.clearDomainEvents();
        
        return CreateConsignmentResult.builder()
            .consignmentId(consignment.getId())
            .consignmentReference(consignment.getConsignmentReference())
            .status(consignment.getStatus())
            .build();
    }
}
```

---

## Frontend Implementation

### Consignment Creation Form Component

**File:** `src/features/consignment-management/components/ConsignmentCreationForm.tsx`

```typescript
export const ConsignmentCreationForm: React.FC = () => {
  const { register, handleSubmit, formState: { errors }, watch, setValue } = useForm<CreateConsignmentFormData>();
  const [lineItems, setLineItems] = useState<LineItemFormData[]>([]);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [draftSaved, setDraftSaved] = useState(false);

  // Load draft from localStorage on mount
  useEffect(() => {
    const draft = loadDraftFromLocalStorage('consignment-draft');
    if (draft) {
      // Restore form data
      setValue('consignmentReference', draft.consignmentReference);
      setLineItems(draft.lineItems);
    }
  }, []);

  const handleAddLineItem = () => {
    setLineItems([...lineItems, {
      productCode: '',
      quantity: 0,
      expirationDate: null,
      batchNumber: ''
    }]);
  };

  const handleRemoveLineItem = (index: number) => {
    setLineItems(lineItems.filter((_, i) => i !== index));
  };

  // Barcode-First: Handle barcode scan (primary method)
  const handleBarcodeScan = async (index: number, scannedBarcode: string) => {
    try {
      const product = await validateProductBarcode(scannedBarcode);
      const updatedLineItems = [...lineItems];
      updatedLineItems[index].productCode = product.productCode;
      setLineItems(updatedLineItems);
    } catch (error) {
      // Show error message - user can enter manually as fallback
      console.error('Barcode validation failed:', error);
    }
  };

  // Fallback: Handle manual product code input
  const handleProductCodeChange = (index: number, productCode: string) => {
    const updatedLineItems = [...lineItems];
    updatedLineItems[index].productCode = productCode;
    setLineItems(updatedLineItems);
  };

  const handleSaveDraft = () => {
    const draft = {
      consignmentReference: watch('consignmentReference'),
      lineItems
    };
    saveDraftToLocalStorage('consignment-draft', draft);
    setDraftSaved(true);
    setTimeout(() => setDraftSaved(false), 2000);
  };

  const onSubmit = async (data: CreateConsignmentFormData) => {
    setIsSubmitting(true);
    try {
      const command: CreateConsignmentCommand = {
        consignmentReference: data.consignmentReference,
        warehouseId: data.warehouseId,
        receivedAt: data.receivedAt || new Date().toISOString(),
        receivedBy: data.receivedBy,
        lineItems: lineItems.map(item => ({
          productCode: item.productCode,
          quantity: item.quantity,
          expirationDate: item.expirationDate,
          batchNumber: item.batchNumber
        }))
      };
      
      await apiClient.post('/stock-management/consignments', command);
      
      // Clear draft
      clearDraftFromLocalStorage('consignment-draft');
      
      // Navigate to success page
      navigate('/consignments');
    } catch (error) {
      // Show error message
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <Paper sx={{ p: 3 }}>
      <Typography variant="h5" gutterBottom>
        Create Consignment
      </Typography>
      
      <form onSubmit={handleSubmit(onSubmit)}>
        <Grid container spacing={2}>
          <Grid item xs={12} md={6}>
            <TextField
              {...register('consignmentReference', { required: true })}
              label="Consignment Reference"
              error={!!errors.consignmentReference}
              helperText={errors.consignmentReference?.message}
            />
          </Grid>
          
          <Grid item xs={12} md={6}>
            <TextField
              {...register('warehouseId', { required: true })}
              label="Warehouse ID"
              error={!!errors.warehouseId}
            />
          </Grid>
          
          <Grid item xs={12}>
            <Typography variant="h6" gutterBottom>
              Line Items
            </Typography>
            
            {lineItems.map((item, index) => (
              <LineItemRow
                key={index}
                item={item}
                index={index}
                onUpdate={(updatedItem) => {
                  const updated = [...lineItems];
                  updated[index] = updatedItem;
                  setLineItems(updated);
                }}
                onRemove={() => handleRemoveLineItem(index)}
                onBarcodeScan={(barcode) => handleBarcodeScan(index, barcode)}
                onProductCodeChange={(code) => handleProductCodeChange(index, code)}
              />
            ))}
            
            <Button onClick={handleAddLineItem} sx={{ mt: 2 }}>
              Add Line Item
            </Button>
          </Grid>
          
          <Grid item xs={12}>
            <Box sx={{ display: 'flex', gap: 2 }}>
              <Button
                variant="outlined"
                onClick={handleSaveDraft}
              >
                Save Draft
              </Button>
              {draftSaved && <Typography variant="body2" color="success">Draft saved!</Typography>}
              
              <Button
                type="submit"
                variant="contained"
                disabled={isSubmitting || lineItems.length === 0}
              >
                Create Consignment
              </Button>
            </Box>
          </Grid>
        </Grid>
      </form>
    </Paper>
  );
};
```

### Line Item Row Component (Barcode-First Implementation)

**File:** `src/features/consignment-management/components/LineItemRow.tsx`

```typescript
import { BarcodeInput } from '@/components/common';
import { TextField, IconButton, Grid } from '@mui/material';
import { Delete as DeleteIcon } from '@mui/icons-material';

interface LineItemRowProps {
  item: LineItemFormData;
  index: number;
  onUpdate: (item: LineItemFormData) => void;
  onRemove: () => void;
  onBarcodeScan: (barcode: string) => void;
  onProductCodeChange: (code: string) => void;
}

export const LineItemRow: React.FC<LineItemRowProps> = ({
  item,
  index,
  onUpdate,
  onRemove,
  onBarcodeScan,
  onProductCodeChange,
}) => {
  // Primary: Handle barcode scan
  const handleBarcodeScan = async (scannedBarcode: string) => {
    onBarcodeScan(scannedBarcode);
  };

  // Fallback: Handle manual product code input
  const handleProductCodeChange = (value: string) => {
    onProductCodeChange(value);
    onUpdate({ ...item, productCode: value });
  };

  return (
    <Grid container spacing={2} sx={{ mb: 2 }}>
      <Grid item xs={12} md={4}>
        <BarcodeInput
          label="Product Code or Barcode"
          value={item.productCode}
          onChange={handleProductCodeChange}
          onScan={handleBarcodeScan}
          helperText="Scan barcode first, or enter product code manually if scanning fails"
          autoFocus={index === 0}
          required
          fullWidth
        />
      </Grid>
      
      <Grid item xs={12} md={2}>
        <TextField
          label="Quantity"
          type="number"
          value={item.quantity}
          onChange={(e) => onUpdate({ ...item, quantity: Number(e.target.value) })}
          inputProps={{ min: 1 }}
          required
          fullWidth
        />
      </Grid>
      
      <Grid item xs={12} md={2}>
        <TextField
          label="Expiration Date"
          type="date"
          value={item.expirationDate || ''}
          onChange={(e) => onUpdate({ ...item, expirationDate: e.target.value || null })}
          InputLabelProps={{ shrink: true }}
          fullWidth
        />
      </Grid>
      
      <Grid item xs={12} md={3}>
        <TextField
          label="Batch Number"
          value={item.batchNumber}
          onChange={(e) => onUpdate({ ...item, batchNumber: e.target.value })}
          fullWidth
        />
      </Grid>
      
      <Grid item xs={12} md={1}>
        <IconButton onClick={onRemove} color="error">
          <DeleteIcon />
        </IconButton>
      </Grid>
    </Grid>
  );
};
```

**Key Features:**
- **BarcodeInput for product code:** Primary scanning, manual input as fallback
- **Auto-focus:** First line item auto-focuses for scanner workflow
- **Clear helper text:** Guides users to scan first
- **Immediate validation:** Validates barcode and populates product details

---

## Data Flow

```
Frontend (React)
  ↓ POST /api/v1/stock-management/consignments
Gateway Service
  ↓ Route to Stock Management Service
Stock Management Service (Command Controller)
  ↓ CreateConsignmentCommand
Command Handler
  ↓ Validate ConsignmentReference (uniqueness)
  ↓ For each line item:
    Validate Product Code (synchronous call to Product Service)
    Validate Line Item (quantity, dates)
    Create ConsignmentLineItem
  ↓ StockConsignment.builder()
  Domain Core (StockConsignment Aggregate)
  ↓ StockConsignmentReceivedEvent
Event Publisher
  ↓ Kafka Topic: stock-management-events
Query Handler
  ↓ CreateConsignmentResult
Query Controller
  ↓ Response
Gateway Service
  ↓ Response
Frontend (React)
```

---

## Testing Strategy

### Unit Tests

- **Command Handler** - Test consignment creation logic
- **Validation Service** - Test validation rules
- **Domain Model** - Test business logic

### Integration Tests

- **Consignment Creation Endpoint** - Test full creation flow
- **Product Service Integration** - Test product validation calls
- **Event Publishing** - Test event publication

### Gateway API Tests

- **Consignment Creation** - Test creation through gateway
- **Error Scenarios** - Test error handling

---

## Acceptance Criteria Validation

- ✅ System provides form-based UI for consignment data entry
- ✅ Form includes required fields
- ✅ System validates required fields and data formats in real-time
- ✅ System validates product codes exist in product master data
- ✅ System supports adding multiple line items per consignment
- ✅ System supports product barcode scanning for product identification
- ✅ System provides clear validation error messages
- ✅ System allows saving draft consignments for later completion
- ✅ System publishes `StockConsignmentReceivedEvent` after successful entry

---

**Document Control**

- **Version:** 1.0
- **Date:** 2025-01
- **Status:** Draft

