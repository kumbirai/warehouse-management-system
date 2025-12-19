# Consignment CSV Upload Implementation Plan

## US-1.1.1: Upload Consignment Data via CSV File

**Service:** Stock Management Service  
**Priority:** Must Have  
**Story Points:** 8  
**Sprint:** Sprint 2

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
**I want** to upload consignment data via CSV file  
**So that** I can efficiently import bulk stock consignment information

### Business Requirements

- Accept CSV file uploads through web interface
- CSV format includes: consignment reference, product codes, quantities, expiration dates
- Validate CSV file format and required columns before processing
- Validate product codes exist in product master data
- Provide clear error messages for invalid CSV data
- Process CSV file and create stock consignment records
- Display upload progress and completion status
- Support CSV file sizes up to 10MB
- Log all CSV upload events for audit
- Publish `StockConsignmentReceivedEvent` for each consignment

### Technical Requirements

- Follow DDD, Clean Hexagonal Architecture, CQRS, Event-Driven Choreography
- Multi-tenant support (tenant isolation)
- Streaming CSV parser for large files
- Batch processing for performance
- Event publishing for each consignment created
- REST API with multipart file upload
- Frontend file upload component with progress tracking
- Product Service integration for product validation

---

## UI Design

### CSV Upload Component

**Component:** `ConsignmentCsvUploadForm.tsx`

**Features:**

- **File Input** - Drag and drop or file picker
- **File Validation** - Client-side validation (size, format)
- **Upload Progress** - Progress bar showing upload percentage
- **Preview** - Show first few rows before upload
- **Error Display** - Show validation errors clearly
- **Success Summary** - Show number of consignments created

**UI Flow:**

1. User navigates to "Stock Management" → "Consignments" → "Upload CSV"
2. User selects CSV file (drag and drop or file picker)
3. System validates file client-side (size, format)
4. System shows preview of first 5 rows
5. User clicks "Upload"
6. System shows upload progress
7. System processes file and shows results:
    - Number of consignments created
    - Number of line items processed
    - Number of errors (if any)
    - List of errors with row numbers
8. User can download error report (CSV with errors)

**Error Handling:**

- File too large (>10MB) - Show error immediately
- Invalid format - Show format requirements
- Validation errors - Show row-by-row errors
- Network errors - Show retry option
- Product not found - Show product code and suggest adding to master data

---

## CSV Format Specification

### Required Columns

See [CSV Format Specification](../../../02-api/CSV_Format_Specification.md#stock-consignment-csv-format) for complete specification.

**Key Columns:**

- `ConsignmentReference` - Unique consignment reference
- `ProductCode` - Product code (must exist in master data)
- `Quantity` - Quantity received (must be positive)
- `ExpirationDate` - Product expiration date (ISO 8601, optional)
- `ReceivedDate` - Date/time when stock was received (ISO 8601)
- `WarehouseId` - Warehouse identifier

### Validation Rules

1. `ConsignmentReference` must be unique within the file
2. `ProductCode` must exist in product master data (validated via Product Service)
3. `Quantity` must be positive (> 0)
4. `ExpirationDate` must be in the future (if provided)
5. `ReceivedDate` cannot be in the future
6. `WarehouseId` must be a valid warehouse identifier
7. Multiple rows with same `ConsignmentReference` represent line items for the same consignment

---

## Domain Model Design

### StockConsignment Aggregate Root

**Package:** `com.ccbsa.wms.stock.domain.core.entity`

```java
public class StockConsignment extends TenantAwareAggregateRoot<ConsignmentId> {
    
    private ConsignmentReference consignmentReference;
    private WarehouseId warehouseId;
    private ConsignmentStatus status;
    private LocalDateTime receivedAt;
    private LocalDateTime confirmedAt;
    private List<ConsignmentLineItem> lineItems;
    
    // Business logic methods
    public static Builder builder() { ... }
    
    public void confirm() { ... }
    
    public void addLineItem(ConsignmentLineItem lineItem) { ... }
}
```

### Value Objects

**ConsignmentId, ConsignmentReference, ConsignmentLineItem, WarehouseId** -
See [04-Stock-Consignment-Creation-Implementation-Plan.md](04-Stock-Consignment-Creation-Implementation-Plan.md) for details.

### Domain Events

**StockConsignmentReceivedEvent:**

```java
public class StockConsignmentReceivedEvent extends StockManagementEvent<StockConsignment> {
    private final ConsignmentReference consignmentReference;
    private final WarehouseId warehouseId;
    private final List<ConsignmentLineItem> lineItems;
    
    // Constructor and getters
}
```

---

## Backend Implementation

### Phase 1: Domain Core

**Module:** `stock-management-domain/stock-management-domain-core`

**Files to Create:**

1. `StockConsignment.java` - Aggregate root
2. `ConsignmentId.java` - Value object
3. `ConsignmentReference.java` - Value object
4. `ConsignmentLineItem.java` - Value object
5. `ConsignmentStatus.java` - Enum
6. `WarehouseId.java` - Value object
7. `StockConsignmentReceivedEvent.java` - Domain event
8. `StockManagementEvent.java` - Base service event

### Phase 2: Application Service

**Module:** `stock-management-domain/stock-management-application-service`

**Command Handler:**

```java
@Component
public class UploadConsignmentCsvCommandHandler {
    
    private final StockConsignmentRepository repository;
    private final StockManagementEventPublisher eventPublisher;
    private final ConsignmentCsvParser csvParser;
    private final ProductServicePort productServicePort;
    private final ConsignmentValidationService validationService;
    
    @Transactional
    public UploadConsignmentCsvResult handle(UploadConsignmentCsvCommand command) {
        List<ConsignmentCsvRow> rows = csvParser.parse(command.getCsvContent());
        
        // Group rows by consignment reference
        Map<String, List<ConsignmentCsvRow>> consignmentGroups = 
            rows.stream().collect(Collectors.groupingBy(ConsignmentCsvRow::getConsignmentReference));
        
        UploadConsignmentCsvResult.Builder resultBuilder = UploadConsignmentCsvResult.builder();
        List<DomainEvent<?>> allEvents = new ArrayList<>();
        
        for (Map.Entry<String, List<ConsignmentCsvRow>> entry : consignmentGroups.entrySet()) {
            try {
                String consignmentRef = entry.getKey();
                List<ConsignmentCsvRow> lineItems = entry.getValue();
                
                // Validate consignment reference uniqueness
                validateConsignmentReference(ConsignmentReference.of(consignmentRef), command.getTenantId());
                
                // Validate and create line items
                List<ConsignmentLineItem> validatedLineItems = new ArrayList<>();
                for (ConsignmentCsvRow row : lineItems) {
                    // Validate product exists
                    ProductInfo productInfo = productServicePort.getProductByCode(
                        row.getProductCode(), 
                        command.getTenantId()
                    ).orElseThrow(() -> new ProductNotFoundException(row.getProductCode()));
                    
                    // Validate row data
                    validationService.validateLineItem(row, productInfo);
                    
                    // Create line item
                    ConsignmentLineItem lineItem = ConsignmentLineItem.builder()
                        .productCode(ProductCode.of(row.getProductCode()))
                        .quantity(Quantity.of(row.getQuantity()))
                        .expirationDate(row.getExpirationDate() != null ? 
                            ExpirationDate.of(row.getExpirationDate()) : null)
                        .batchNumber(row.getBatchNumber())
                        .build();
                    
                    validatedLineItems.add(lineItem);
                }
                
                // Create consignment
                StockConsignment consignment = StockConsignment.builder()
                    .consignmentId(ConsignmentId.generate())
                    .tenantId(command.getTenantId())
                    .consignmentReference(ConsignmentReference.of(consignmentRef))
                    .warehouseId(WarehouseId.of(lineItems.get(0).getWarehouseId()))
                    .receivedAt(parseReceivedDate(lineItems.get(0).getReceivedDate()))
                    .lineItems(validatedLineItems)
                    .build();
                
                // Persist
                repository.save(consignment);
                
                // Collect events
                allEvents.addAll(consignment.getDomainEvents());
                consignment.clearDomainEvents();
                
                resultBuilder.consignmentsCreated(resultBuilder.getConsignmentsCreated() + 1);
                resultBuilder.lineItemsProcessed(resultBuilder.getLineItemsProcessed() + validatedLineItems.size());
                
            } catch (Exception e) {
                resultBuilder.addError(ConsignmentCsvError.builder()
                    .consignmentReference(entry.getKey())
                    .errorMessage(e.getMessage())
                    .build());
            }
        }
        
        // Publish all events
        eventPublisher.publish(allEvents);
        
        return resultBuilder.build();
    }
}
```

**CSV Parser:**

```java
@Component
public class ConsignmentCsvParser {
    
    public List<ConsignmentCsvRow> parse(String csvContent) {
        List<ConsignmentCsvRow> rows = new ArrayList<>();
        
        try (CSVParser parser = CSVParser.parse(csvContent, CSVFormat.DEFAULT
            .withFirstRecordAsHeader()
            .withIgnoreHeaderCase()
            .withTrim())) {
            
            for (CSVRecord record : parser) {
                ConsignmentCsvRow row = ConsignmentCsvRow.builder()
                    .rowNumber(record.getRecordNumber())
                    .consignmentReference(record.get("ConsignmentReference"))
                    .productCode(record.get("ProductCode"))
                    .quantity(parseDecimal(record.get("Quantity")))
                    .expirationDate(parseDate(record.get("ExpirationDate")))
                    .batchNumber(record.get("BatchNumber"))
                    .receivedDate(parseDateTime(record.get("ReceivedDate")))
                    .receivedBy(record.get("ReceivedBy"))
                    .warehouseId(record.get("WarehouseId"))
                    .build();
                
                rows.add(row);
            }
        }
        
        return rows;
    }
}
```

### Phase 3: REST API

**Module:** `stock-management-application`

**Command Controller:**

```java
@RestController
@RequestMapping("/api/v1/stock-management/consignments")
@Tag(name = "Stock Consignment Commands", description = "Stock consignment command operations")
public class StockConsignmentCommandController {
    
    private final UploadConsignmentCsvCommandHandler uploadCsvHandler;
    private final StockConsignmentDTOMapper mapper;
    
    @PostMapping("/upload-csv")
    @Operation(summary = "Upload consignment data via CSV file")
    @PreAuthorize("hasRole('OPERATOR')")
    public ResponseEntity<ApiResponse<UploadConsignmentCsvResultDTO>> uploadCsv(
            @RequestParam("file") MultipartFile file,
            @RequestHeader("X-Tenant-Id") String tenantId
    ) {
        // Validate file
        validateFile(file);
        
        // Parse CSV
        String csvContent = new String(file.getBytes(), StandardCharsets.UTF_8);
        
        // Create command
        UploadConsignmentCsvCommand command = UploadConsignmentCsvCommand.builder()
            .csvContent(csvContent)
            .tenantId(TenantId.of(tenantId))
            .fileName(file.getOriginalFilename())
            .build();
        
        // Handle command
        UploadConsignmentCsvResult result = uploadCsvHandler.handle(command);
        
        return ResponseEntity.ok(ApiResponseBuilder.ok(mapper.toDTO(result)));
    }
}
```

---

## Frontend Implementation

### CSV Upload Component

**File:** `src/features/consignment-management/components/ConsignmentCsvUploadForm.tsx`

```typescript
export const ConsignmentCsvUploadForm: React.FC = () => {
  const [file, setFile] = useState<File | null>(null);
  const [uploading, setUploading] = useState(false);
  const [progress, setProgress] = useState(0);
  const [result, setResult] = useState<UploadResult | null>(null);
  const [errors, setErrors] = useState<CsvError[]>([]);

  const handleFileSelect = (selectedFile: File) => {
    // Validate file size
    if (selectedFile.size > 10 * 1024 * 1024) {
      setErrors([{ message: 'File size must be less than 10MB' }]);
      return;
    }
    
    setFile(selectedFile);
    setErrors([]);
  };

  const handleUpload = async () => {
    if (!file) return;
    
    setUploading(true);
    setProgress(0);
    
    try {
      const formData = new FormData();
      formData.append('file', file);
      
      const response = await apiClient.post<ApiResponse<UploadResult>>(
        '/stock-management/consignments/upload-csv',
        formData,
        {
          headers: { 'Content-Type': 'multipart/form-data' },
          onUploadProgress: (progressEvent) => {
            const percentCompleted = Math.round(
              (progressEvent.loaded * 100) / (progressEvent.total || 1)
            );
            setProgress(percentCompleted);
          }
        }
      );
      
      setResult(response.data.data);
      if (response.data.data.errors && response.data.data.errors.length > 0) {
        setErrors(response.data.data.errors);
      }
    } catch (error) {
      setErrors([{ message: 'Upload failed. Please try again.' }]);
    } finally {
      setUploading(false);
    }
  };

  return (
    <Paper sx={{ p: 3 }}>
      <Typography variant="h5" gutterBottom>
        Upload Consignment CSV
      </Typography>
      
      <FileUpload
        onFileSelect={handleFileSelect}
        acceptedTypes={['.csv']}
        maxSize={10 * 1024 * 1024}
      />
      
      {uploading && (
        <Box sx={{ mt: 2 }}>
          <LinearProgress variant="determinate" value={progress} />
          <Typography variant="body2" sx={{ mt: 1 }}>
            {progress}% uploaded
          </Typography>
        </Box>
      )}
      
      {result && (
        <Alert severity="success" sx={{ mt: 2 }}>
          <AlertTitle>Upload Complete</AlertTitle>
          {result.consignmentsCreated} consignments created
          <br />
          {result.lineItemsProcessed} line items processed
        </Alert>
      )}
      
      {errors.length > 0 && (
        <ErrorList errors={errors} />
      )}
      
      <Button
        variant="contained"
        onClick={handleUpload}
        disabled={!file || uploading}
        sx={{ mt: 2 }}
      >
        Upload
      </Button>
    </Paper>
  );
};
```

---

## Data Flow

```
Frontend (React)
  ↓ POST /api/v1/stock-management/consignments/upload-csv (multipart/form-data)
Gateway Service
  ↓ Route to Stock Management Service
Stock Management Service (Command Controller)
  ↓ UploadConsignmentCsvCommand
Command Handler
  ↓ CSV Parser
  ↓ Group by ConsignmentReference
  ↓ For each consignment:
    Validate ConsignmentReference (uniqueness)
    For each line item:
      Validate Product Code (synchronous call to Product Service)
      Validate Line Item (quantity, dates)
      Create ConsignmentLineItem
    ↓ StockConsignment.builder()
    Domain Core (StockConsignment Aggregate)
    ↓ StockConsignmentReceivedEvent
Event Publisher
  ↓ Kafka Topic: stock-management-events
Query Handler
  ↓ UploadConsignmentCsvResult
Query Controller
  ↓ Response with upload summary
Gateway Service
  ↓ Response
Frontend (React)
```

---

## Testing Strategy

### Unit Tests

- **CSV Parser** - Test parsing of valid/invalid CSV formats
- **Command Handler** - Test consignment creation logic
- **Validation Service** - Test validation rules
- **Domain Model** - Test business logic

### Integration Tests

- **CSV Upload Endpoint** - Test full upload flow
- **Product Service Integration** - Test product validation calls
- **Event Publishing** - Test event publication
- **Database** - Test consignment persistence

### Gateway API Tests

- **CSV Upload** - Test upload through gateway
- **Error Scenarios** - Test error handling
- **Authentication** - Test authorization

---

## Acceptance Criteria Validation

- ✅ System accepts CSV file uploads through web interface
- ✅ CSV format includes required columns
- ✅ System validates CSV file format and required columns
- ✅ System validates product codes exist in product master data
- ✅ System provides clear error messages for invalid CSV data
- ✅ System processes CSV file and creates stock consignment records
- ✅ System displays upload progress and completion status
- ✅ System supports CSV file sizes up to 10MB
- ✅ System logs all CSV upload events for audit
- ✅ System publishes `StockConsignmentReceivedEvent` for each consignment

---

**Document Control**

- **Version:** 1.0
- **Date:** 2025-01
- **Status:** Draft

