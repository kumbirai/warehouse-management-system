# Product CSV Upload Implementation Plan

## US-4.1.1: Upload Product Master Data via CSV File

**Service:** Product Service  
**Priority:** Must Have  
**Story Points:** 8  
**Sprint:** Sprint 1

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

**As a** system administrator  
**I want** to upload product master data via CSV file  
**So that** I can efficiently import bulk product information

### Business Requirements

- Accept CSV file uploads through web interface
- CSV format includes: product codes, descriptions, barcodes, unit of measure
- Validate CSV file format and required columns before processing
- Provide clear error messages for invalid CSV data
- Process CSV file and create/update product records
- Display upload progress and completion status
- Support CSV file sizes up to 10MB
- Log all CSV upload events for audit
- Publish `ProductCreatedEvent`, `ProductUpdatedEvent` for changes

### Technical Requirements

- Follow DDD, Clean Hexagonal Architecture, CQRS, Event-Driven Choreography
- Multi-tenant support (tenant isolation)
- Streaming CSV parser for large files
- Batch processing for performance
- Event publishing for each product created/updated
- REST API with multipart file upload
- Frontend file upload component with progress tracking

---

## UI Design

### CSV Upload Component

**Component:** `ProductCsvUploadForm.tsx`

**Features:**
- **File Input** - Drag and drop or file picker
- **File Validation** - Client-side validation (size, format)
- **Upload Progress** - Progress bar showing upload percentage
- **Preview** - Show first few rows before upload
- **Error Display** - Show validation errors clearly
- **Success Summary** - Show number of products created/updated

**UI Flow:**
1. User navigates to "Products" → "Upload CSV"
2. User selects CSV file (drag and drop or file picker)
3. System validates file client-side (size, format)
4. System shows preview of first 5 rows
5. User clicks "Upload"
6. System shows upload progress
7. System processes file and shows results:
   - Number of products created
   - Number of products updated
   - Number of errors (if any)
   - List of errors with row numbers
8. User can download error report (CSV with errors)

**Error Handling:**
- File too large (>10MB) - Show error immediately
- Invalid format - Show format requirements
- Validation errors - Show row-by-row errors
- Network errors - Show retry option

### Upload Progress Component

**Component:** `UploadProgress.tsx`

**Features:**
- Progress bar (0-100%)
- Status message ("Uploading...", "Processing...", "Complete")
- Estimated time remaining
- Cancel button (if supported)

---

## CSV Format Specification

### Required Columns

| Column Name | Type | Required | Description | Example |
|------------|------|----------|-------------|---------|
| product_code | String | Yes | Unique product identifier | "PROD-001" |
| description | String | Yes | Product description | "Coca-Cola 330ml Can" |
| primary_barcode | String | Yes | Primary barcode (EAN-13, Code 128, etc.) | "6001067101234" |
| unit_of_measure | String | Yes | Unit of measure | "EA", "CS", "PK" |
| secondary_barcode | String | No | Secondary barcode | "6001067101235" |
| category | String | No | Product category | "Beverages" |
| brand | String | No | Product brand | "Coca-Cola" |

### CSV Template

```csv
product_code,description,primary_barcode,unit_of_measure,secondary_barcode,category,brand
PROD-001,Coca-Cola 330ml Can,6001067101234,EA,6001067101235,Beverages,Coca-Cola
PROD-002,Sprite 330ml Can,6001067101236,EA,,Beverages,Sprite
PROD-003,Fanta Orange 330ml Can,6001067101237,EA,,Beverages,Fanta
```

### Validation Rules

1. **product_code**: Required, unique per tenant, alphanumeric with hyphens/underscores
2. **description**: Required, max 500 characters
3. **primary_barcode**: Required, valid barcode format (EAN-13, Code 128, etc.)
4. **unit_of_measure**: Required, valid UOM code
5. **secondary_barcode**: Optional, valid barcode format if provided
6. **File size**: Maximum 10MB
7. **Row limit**: Maximum 10,000 rows per file

---

## Domain Model Design

### Product Aggregate Root

**Package:** `com.ccbsa.wms.product.domain.core.entity`

```java
public class Product extends TenantAwareAggregateRoot<ProductId> {
    
    private ProductCode productCode;
    private String description;
    private ProductBarcode primaryBarcode;
    private List<ProductBarcode> secondaryBarcodes;
    private UnitOfMeasure unitOfMeasure;
    private String category;
    private String brand;
    private LocalDateTime createdAt;
    private LocalDateTime lastModifiedAt;
    
    // Business logic methods
    public static Builder builder() { ... }
    
    public void updateDescription(String description) { ... }
    
    public void addSecondaryBarcode(ProductBarcode barcode) { ... }
    
    public void removeSecondaryBarcode(ProductBarcode barcode) { ... }
    
    public boolean hasBarcode(String barcodeValue) { ... }
}
```

### Value Objects

**ProductId:**
```java
public final class ProductId {
    private final UUID value;
    
    public static ProductId of(UUID value) { ... }
    public static ProductId generate() { ... }
}
```

**ProductCode:**
```java
public final class ProductCode {
    private final String value;
    
    public static ProductCode of(String value) { ... }
    
    private void validate() { ... } // Alphanumeric with hyphens/underscores
}
```

**ProductBarcode:**
```java
public final class ProductBarcode {
    private final String value;
    private final BarcodeType type;
    
    public static ProductBarcode of(String value) { ... }
    
    private void validateFormat() { ... } // EAN-13, Code 128, etc.
}
```

**UnitOfMeasure:**
```java
public enum UnitOfMeasure {
    EA,    // Each
    CS,    // Case
    PK,    // Pack
    BOX,   // Box
    PAL    // Pallet
}
```

### Domain Events

**ProductCreatedEvent:**
```java
public class ProductCreatedEvent extends ProductEvent<Product> {
    private final ProductCode productCode;
    private final ProductBarcode primaryBarcode;
    private final UnitOfMeasure unitOfMeasure;
    
    // Constructor and getters
}
```

**ProductUpdatedEvent:**
```java
public class ProductUpdatedEvent extends ProductEvent<Product> {
    private final ProductCode productCode;
    private final List<String> changedFields;
    
    // Constructor and getters
}
```

---

## Backend Implementation

### Phase 1: Domain Core

**Module:** `product-domain/product-domain-core`

**Files to Create:**
1. `Product.java` - Aggregate root
2. `ProductId.java` - Value object
3. `ProductCode.java` - Value object
4. `ProductBarcode.java` - Value object
5. `BarcodeType.java` - Enum
6. `UnitOfMeasure.java` - Enum
7. `ProductCreatedEvent.java` - Domain event
8. `ProductUpdatedEvent.java` - Domain event
9. `ProductEvent.java` - Base service event

### Phase 2: Application Service

**Module:** `product-domain/product-application-service`

**Command Handler:**
```java
@Component
public class UploadProductCsvCommandHandler {
    
    private final ProductRepository repository;
    private final ProductEventPublisher eventPublisher;
    private final CsvParser csvParser;
    
    @Transactional
    public UploadProductCsvResult handle(UploadProductCsvCommand command) {
        List<ProductCsvRow> rows = csvParser.parse(command.getCsvContent());
        
        UploadProductCsvResult.Builder resultBuilder = UploadProductCsvResult.builder();
        List<DomainEvent<?>> allEvents = new ArrayList<>();
        
        for (ProductCsvRow row : rows) {
            try {
                // Validate row
                validateRow(row, command.getTenantId());
                
                // Check if product exists
                Optional<Product> existingProduct = repository.findByProductCodeAndTenantId(
                    ProductCode.of(row.getProductCode()),
                    command.getTenantId()
                );
                
                Product product;
                if (existingProduct.isPresent()) {
                    // Update existing product
                    product = existingProduct.get();
                    updateProductFromRow(product, row);
                    resultBuilder.updatedCount(resultBuilder.getUpdatedCount() + 1);
                } else {
                    // Create new product
                    product = createProductFromRow(row, command.getTenantId());
                    resultBuilder.createdCount(resultBuilder.getCreatedCount() + 1);
                }
                
                // Persist
                repository.save(product);
                
                // Collect events
                allEvents.addAll(product.getDomainEvents());
                product.clearDomainEvents();
                
            } catch (Exception e) {
                resultBuilder.addError(ProductCsvError.builder()
                    .rowNumber(row.getRowNumber())
                    .productCode(row.getProductCode())
                    .errorMessage(e.getMessage())
                    .build());
            }
        }
        
        // Publish all events
        eventPublisher.publish(allEvents);
        
        return resultBuilder.build();
    }
    
    private void validateRow(ProductCsvRow row, TenantId tenantId) {
        // Validate product code format
        ProductCode.of(row.getProductCode()); // Throws if invalid
        
        // Validate barcode format
        ProductBarcode.of(row.getPrimaryBarcode()); // Throws if invalid
        
        // Validate product code uniqueness (for new products)
        if (repository.existsByProductCodeAndTenantId(
            ProductCode.of(row.getProductCode()),
            tenantId
        )) {
            // Will be handled as update
        }
    }
    
    private Product createProductFromRow(ProductCsvRow row, TenantId tenantId) {
        return Product.builder()
            .productId(ProductId.generate())
            .tenantId(tenantId)
            .productCode(ProductCode.of(row.getProductCode()))
            .description(row.getDescription())
            .primaryBarcode(ProductBarcode.of(row.getPrimaryBarcode()))
            .unitOfMeasure(UnitOfMeasure.valueOf(row.getUnitOfMeasure()))
            .category(row.getCategory())
            .brand(row.getBrand())
            .build();
    }
    
    private void updateProductFromRow(Product product, ProductCsvRow row) {
        product.updateDescription(row.getDescription());
        // Update other fields as needed
    }
}
```

**CSV Parser:**
```java
@Component
public class ProductCsvParser {
    
    public List<ProductCsvRow> parse(String csvContent) {
        List<ProductCsvRow> rows = new ArrayList<>();
        
        try (CSVParser parser = CSVParser.parse(csvContent, CSVFormat.DEFAULT
            .withFirstRecordAsHeader()
            .withIgnoreHeaderCase()
            .withTrim())) {
            
            for (CSVRecord record : parser) {
                ProductCsvRow row = ProductCsvRow.builder()
                    .rowNumber(record.getRecordNumber())
                    .productCode(record.get("product_code"))
                    .description(record.get("description"))
                    .primaryBarcode(record.get("primary_barcode"))
                    .unitOfMeasure(record.get("unit_of_measure"))
                    .secondaryBarcode(record.get("secondary_barcode"))
                    .category(record.get("category"))
                    .brand(record.get("brand"))
                    .build();
                
                rows.add(row);
            }
        } catch (IOException e) {
            throw new CsvParsingException("Failed to parse CSV", e);
        }
        
        return rows;
    }
}
```

**Port Interfaces:**
```java
// Repository Port
public interface ProductRepository {
    void save(Product product);
    Optional<Product> findByIdAndTenantId(ProductId id, TenantId tenantId);
    Optional<Product> findByProductCodeAndTenantId(ProductCode productCode, TenantId tenantId);
    boolean existsByProductCodeAndTenantId(ProductCode productCode, TenantId tenantId);
    Optional<Product> findByBarcodeAndTenantId(String barcode, TenantId tenantId);
    List<Product> findByTenantId(TenantId tenantId);
}

// Event Publisher Port
public interface ProductEventPublisher extends EventPublisher {
    void publish(ProductEvent<?> event);
}
```

### Phase 3: Data Access

**Module:** `product-dataaccess`

**JPA Entity:**
```java
@Entity
@Table(name = "products", schema = "tenant_schema")
public class ProductEntity {
    @Id
    private UUID id;
    
    @Column(name = "tenant_id", nullable = false)
    private String tenantId;
    
    @Column(name = "product_code", nullable = false, unique = true)
    private String productCode;
    
    @Column(name = "description", nullable = false, length = 500)
    private String description;
    
    @Column(name = "primary_barcode", nullable = false)
    private String primaryBarcode;
    
    @Column(name = "unit_of_measure", nullable = false)
    private String unitOfMeasure;
    
    @Column(name = "category")
    private String category;
    
    @Column(name = "brand")
    private String brand;
    
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL)
    private List<ProductBarcodeEntity> secondaryBarcodes;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "last_modified_at", nullable = false)
    private LocalDateTime lastModifiedAt;
    
    @Version
    private Long version;
    
    // Getters and setters
}

@Entity
@Table(name = "product_barcodes", schema = "tenant_schema")
public class ProductBarcodeEntity {
    @Id
    private UUID id;
    
    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private ProductEntity product;
    
    @Column(name = "barcode", nullable = false)
    private String barcode;
    
    @Column(name = "barcode_type", nullable = false)
    private String barcodeType;
    
    // Getters and setters
}
```

**Database Migration:**
```sql
-- V1__Create_products_table.sql
CREATE TABLE IF NOT EXISTS products (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL,
    product_code VARCHAR(100) NOT NULL,
    description VARCHAR(500) NOT NULL,
    primary_barcode VARCHAR(255) NOT NULL,
    unit_of_measure VARCHAR(50) NOT NULL,
    category VARCHAR(100),
    brand VARCHAR(100),
    created_at TIMESTAMP NOT NULL,
    last_modified_at TIMESTAMP NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_products_tenant_product_code UNIQUE (tenant_id, product_code),
    CONSTRAINT uk_products_tenant_primary_barcode UNIQUE (tenant_id, primary_barcode)
);

CREATE TABLE IF NOT EXISTS product_barcodes (
    id UUID PRIMARY KEY,
    product_id UUID NOT NULL,
    barcode VARCHAR(255) NOT NULL,
    barcode_type VARCHAR(50) NOT NULL,
    CONSTRAINT fk_product_barcodes_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    CONSTRAINT uk_product_barcodes_barcode UNIQUE (barcode)
);

CREATE INDEX idx_products_tenant_id ON products(tenant_id);
CREATE INDEX idx_products_product_code ON products(product_code);
CREATE INDEX idx_products_primary_barcode ON products(primary_barcode);
CREATE INDEX idx_product_barcodes_product_id ON product_barcodes(product_id);
CREATE INDEX idx_product_barcodes_barcode ON product_barcodes(barcode);
```

### Phase 4: REST API

**Module:** `product-application`

**Command Controller:**
```java
@RestController
@RequestMapping("/api/v1/product-service/products")
@Tag(name = "Product Commands", description = "Product command operations")
public class ProductCommandController {
    
    private final UploadProductCsvCommandHandler commandHandler;
    private final ProductDTOMapper mapper;
    
    @PostMapping("/upload-csv")
    @Operation(summary = "Upload product master data via CSV file")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UploadProductCsvResultDTO>> uploadProductCsv(
            @RequestParam("file") MultipartFile file,
            @RequestHeader("X-Tenant-Id") String tenantId
    ) {
        // Validate file
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        
        if (file.getSize() > 10 * 1024 * 1024) { // 10MB
            throw new IllegalArgumentException("File size exceeds 10MB limit");
        }
        
        // Read file content
        String csvContent;
        try {
            csvContent = new String(file.getBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new FileProcessingException("Failed to read file", e);
        }
        
        // Create command
        UploadProductCsvCommand command = UploadProductCsvCommand.builder()
            .csvContent(csvContent)
            .tenantId(TenantId.of(tenantId))
            .fileName(file.getOriginalFilename())
            .build();
        
        // Execute command
        UploadProductCsvResult result = commandHandler.handle(command);
        
        // Map to DTO
        UploadProductCsvResultDTO resultDTO = mapper.toDTO(result);
        
        return ApiResponseBuilder.ok(resultDTO);
    }
}
```

**DTOs:**
```java
// UploadProductCsvResultDTO
public class UploadProductCsvResultDTO {
    private int totalRows;
    private int createdCount;
    private int updatedCount;
    private int errorCount;
    private List<ProductCsvErrorDTO> errors;
    
    // Getters and setters
}

// ProductCsvErrorDTO
public class ProductCsvErrorDTO {
    private int rowNumber;
    private String productCode;
    private String errorMessage;
    
    // Getters and setters
}
```

---

## Frontend Implementation

### API Client

**File:** `frontend-app/src/services/productApiClient.ts`

```typescript
export interface UploadProductCsvResultDTO {
  totalRows: number;
  createdCount: number;
  updatedCount: number;
  errorCount: number;
  errors: ProductCsvErrorDTO[];
}

export interface ProductCsvErrorDTO {
  rowNumber: number;
  productCode: string;
  errorMessage: string;
}

class ProductApiClient {
  async uploadProductCsv(
    file: File,
    onProgress?: (progress: number) => void
  ): Promise<UploadProductCsvResultDTO> {
    const formData = new FormData();
    formData.append('file', file);
    
    const response = await apiClient.post<ApiResponse<UploadProductCsvResultDTO>>(
      '/product-service/products/upload-csv',
      formData,
      {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
        onUploadProgress: (progressEvent) => {
          if (onProgress && progressEvent.total) {
            const progress = Math.round(
              (progressEvent.loaded * 100) / progressEvent.total
            );
            onProgress(progress);
          }
        },
      }
    );
    
    return response.data.data;
  }
}

export const productApiClient = new ProductApiClient();
```

### React Components

**CSV Upload Form:**
```typescript
// frontend-app/src/features/product/commands/ProductCsvUploadForm.tsx
export const ProductCsvUploadForm: React.FC = () => {
  const [file, setFile] = useState<File | null>(null);
  const [uploadProgress, setUploadProgress] = useState(0);
  const [isUploading, setIsUploading] = useState(false);
  const [result, setResult] = useState<UploadProductCsvResultDTO | null>(null);
  
  const handleFileSelect = (event: React.ChangeEvent<HTMLInputElement>) => {
    const selectedFile = event.target.files?.[0];
    if (selectedFile) {
      // Validate file size
      if (selectedFile.size > 10 * 1024 * 1024) {
        toast.error('File size exceeds 10MB limit');
        return;
      }
      
      // Validate file type
      if (!selectedFile.name.endsWith('.csv')) {
        toast.error('Please select a CSV file');
        return;
      }
      
      setFile(selectedFile);
    }
  };
  
  const handleUpload = async () => {
    if (!file) return;
    
    setIsUploading(true);
    setUploadProgress(0);
    
    try {
      const uploadResult = await productApiClient.uploadProductCsv(
        file,
        (progress) => setUploadProgress(progress)
      );
      
      setResult(uploadResult);
      toast.success(
        `Upload complete: ${uploadResult.createdCount} created, ` +
        `${uploadResult.updatedCount} updated`
      );
    } catch (error) {
      toast.error('Failed to upload CSV file');
    } finally {
      setIsUploading(false);
    }
  };
  
  return (
    <Box>
      <Typography variant="h5">Upload Product CSV</Typography>
      
      <input
        type="file"
        accept=".csv"
        onChange={handleFileSelect}
        disabled={isUploading}
      />
      
      {file && (
        <Box>
          <Typography>Selected: {file.name}</Typography>
          <Typography>Size: {(file.size / 1024).toFixed(2)} KB</Typography>
        </Box>
      )}
      
      {isUploading && (
        <Box>
          <LinearProgress variant="determinate" value={uploadProgress} />
          <Typography>{uploadProgress}%</Typography>
        </Box>
      )}
      
      {result && (
        <Box>
          <Typography>Total Rows: {result.totalRows}</Typography>
          <Typography>Created: {result.createdCount}</Typography>
          <Typography>Updated: {result.updatedCount}</Typography>
          <Typography>Errors: {result.errorCount}</Typography>
          
          {result.errors.length > 0 && (
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>Row</TableCell>
                  <TableCell>Product Code</TableCell>
                  <TableCell>Error</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {result.errors.map((error, index) => (
                  <TableRow key={index}>
                    <TableCell>{error.rowNumber}</TableCell>
                    <TableCell>{error.productCode}</TableCell>
                    <TableCell>{error.errorMessage}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </Box>
      )}
      
      <Button
        onClick={handleUpload}
        disabled={!file || isUploading}
        variant="contained"
      >
        Upload CSV
      </Button>
    </Box>
  );
};
```

---

## Data Flow

### Complete Flow Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                    Frontend (React)                          │
│  ┌──────────────────────────────────────────────────────┐   │
│  │  ProductCsvUploadForm                                │   │
│  │  - User selects CSV file                             │   │
│  │  - Client-side validation                            │   │
│  │  - Shows preview                                      │   │
│  │  - Clicks "Upload"                                   │   │
│  └───────────────────┬──────────────────────────────────┘   │
│                      │ POST /api/v1/product-service/products/upload-csv
│                      │ multipart/form-data
│                      │ file: products.csv
└──────────────────────┼───────────────────────────────────────┘
                       │
┌──────────────────────▼───────────────────────────────────────┐
│                  Gateway Service                              │
│  - Validates authentication                                   │
│  - Extracts tenant ID from token                             │
│  - Routes to Product Service                                 │
└──────────────────────┬───────────────────────────────────────┘
                       │
┌──────────────────────▼───────────────────────────────────────┐
│              Product Service                                  │
│  ┌──────────────────────────────────────────────────────┐   │
│  │  ProductCommandController                            │   │
│  │  - Validates file (size, format)                     │   │
│  │  - Reads file content                                │   │
│  │  - Creates UploadProductCsvCommand                   │   │
│  └───────────────────┬──────────────────────────────────┘   │
│                      │
│  ┌───────────────────▼──────────────────────────────────┐   │
│  │  UploadProductCsvCommandHandler                      │   │
│  │  - Parses CSV content                                │   │
│  │  - For each row:                                     │   │
│  │    - Validates row data                              │   │
│  │    - Checks if product exists                        │   │
│  │    - Creates or updates Product aggregate            │   │
│  │    - Saves to repository                             │   │
│  │    - Collects domain events                          │   │
│  │  - Publishes all events                              │   │
│  └───────────────────┬──────────────────────────────────┘   │
│                      │
│  ┌───────────────────▼──────────────────────────────────┐   │
│  │  ProductRepositoryAdapter                            │   │
│  │  - Saves ProductEntity to database                   │   │
│  └───────────────────┬──────────────────────────────────┘   │
│                      │
│  ┌───────────────────▼──────────────────────────────────┐   │
│  │  ProductEventPublisher                               │   │
│  │  - Publishes ProductCreatedEvent/ProductUpdatedEvent │   │
│  └───────────────────┬──────────────────────────────────┘   │
└──────────────────────┼───────────────────────────────────────┘
                       │
┌──────────────────────▼───────────────────────────────────────┐
│                    Kafka                                      │
│  Topic: product-events                                        │
│  Events: ProductCreatedEvent, ProductUpdatedEvent            │
└───────────────────────────────────────────────────────────────┘
```

---

## Testing Strategy

### Unit Tests

**Domain Core:**
- Product aggregate creation
- Product code validation
- Barcode format validation
- Business logic methods

**Application Service:**
- CSV parser logic
- Command handler logic
- Row validation logic
- Error handling

**Data Access:**
- Repository adapter operations
- Entity mapping

### Integration Tests

- End-to-end CSV upload
- Database operations
- Event publishing
- Large file handling

### Gateway API Tests

**File:** `gateway-api-tests/src/test/java/com/ccbsa/wms/gateway/api/ProductCsvUploadTest.java`

```java
@DisplayName("Product CSV Upload API Tests")
class ProductCsvUploadTest extends BaseIntegrationTest {
    
    @Test
    @DisplayName("Should upload product CSV file successfully")
    void shouldUploadProductCsv() throws IOException {
        // Create test CSV content
        String csvContent = "product_code,description,primary_barcode,unit_of_measure\n" +
            "PROD-001,Test Product 1,6001067101234,EA\n" +
            "PROD-002,Test Product 2,6001067101235,EA";
        
        // Create multipart file
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "products.csv",
            "text/csv",
            csvContent.getBytes()
        );
        
        // Upload file
        RequestHeaderHelper.addTenantHeaderIfNeeded(
            webTestClient
                .post()
                .uri("/product-service/products/upload-csv")
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", accessToken))
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData("file", file)),
            authHelper,
            accessToken)
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.success").isEqualTo(true)
            .jsonPath("$.data.createdCount").exists()
            .jsonPath("$.data.updatedCount").exists();
    }
    
    @Test
    @DisplayName("Should reject file larger than 10MB")
    void shouldRejectLargeFile() {
        // Create large file (simulated)
        byte[] largeContent = new byte[11 * 1024 * 1024]; // 11MB
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "large.csv",
            "text/csv",
            largeContent
        );
        
        RequestHeaderHelper.addTenantHeaderIfNeeded(
            webTestClient
                .post()
                .uri("/product-service/products/upload-csv")
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", accessToken))
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData("file", file)),
            authHelper,
            accessToken)
            .exchange()
            .expectStatus().isBadRequest();
    }
}
```

---

## Acceptance Criteria Validation

| AC | Description | Validation Method |
|----|-------------|-------------------|
| AC1 | System accepts CSV file uploads through web interface | Integration test + Frontend test |
| AC2 | CSV format includes required columns | Unit test + Integration test |
| AC3 | System validates CSV file format and required columns | Unit test + Integration test |
| AC4 | System provides clear error messages for invalid CSV data | Integration test + Frontend test |
| AC5 | System processes CSV file and creates/updates product records | Integration test |
| AC6 | System displays upload progress and completion status | Frontend test |
| AC7 | System supports CSV file sizes up to 10MB | Integration test |
| AC8 | System logs all CSV upload events for audit | Integration test |
| AC9 | System publishes ProductCreatedEvent, ProductUpdatedEvent | Integration test |

---

## Definition of Done

- [ ] Domain core implemented with all value objects and aggregate
- [ ] Application service implemented with CSV upload command handler
- [ ] CSV parser implemented with validation
- [ ] Data access implemented with JPA entities and repository adapters
- [ ] REST API implemented with multipart file upload
- [ ] Event publishing implemented
- [ ] Frontend upload component implemented with progress tracking
- [ ] Gateway API tests written and passing
- [ ] Unit tests written (80%+ coverage)
- [ ] Integration tests written and passing
- [ ] Code reviewed and approved
- [ ] Documentation updated

---

**Document Control**

- **Version:** 1.0
- **Date:** 2025-01
- **Status:** Draft

