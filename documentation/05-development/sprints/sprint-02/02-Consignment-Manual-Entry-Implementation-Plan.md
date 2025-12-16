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
    - Product Code or Barcode (required, with barcode scanner)
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
    - Scans product barcode or enters product code
    - System validates product exists and displays product name
    - User enters quantity
    - User optionally enters expiration date and batch number
6. User can add more line items
7. User can save as draft at any time
8. User clicks "Create Consignment"
9. System validates and creates consignment
10. Success message displayed
11. User redirected to consignment detail page

### Barcode Scanner Integration

**Component:** `ProductBarcodeScanner.tsx`

**Features:**

- Camera-based barcode scanning (ZXing library)
- Keyboard input support (handheld scanners)
- Product lookup by barcode
- Product information display
- Error handling for invalid barcodes

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

  const handleScanBarcode = async (index: number) => {
    try {
      const barcode = await scanBarcode();
      const product = await validateProductBarcode(barcode);
      const updatedLineItems = [...lineItems];
      updatedLineItems[index].productCode = product.productCode;
      setLineItems(updatedLineItems);
    } catch (error) {
      // Show error message
    }
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
                onScanBarcode={() => handleScanBarcode(index)}
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

