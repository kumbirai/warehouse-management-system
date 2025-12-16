# Product Manual Entry Implementation Plan

## US-4.1.2: Manual Product Master Data Entry via UI

**Service:** Product Service  
**Priority:** Must Have  
**Story Points:** 8  
**Sprint:** Sprint 1

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

**As a** system administrator  
**I want** to manually enter product master data through the UI  
**So that** I can create individual product records or correct data

### Business Requirements

- Provide form-based UI for product data entry
- Form includes fields: product code, description, barcode(s), unit of measure
- Validate required fields and data formats in real-time
- Validate product code uniqueness
- Support adding multiple barcodes per product (primary and secondary)
- Provide clear validation error messages
- Allow saving draft products for later completion
- Publish `ProductCreatedEvent` or `ProductUpdatedEvent` after successful entry

### Technical Requirements

- Follow DDD, Clean Hexagonal Architecture, CQRS, Event-Driven Choreography
- Multi-tenant support (tenant isolation)
- Real-time validation (client and server)
- Draft saving using localStorage
- Event publishing for product creation/update
- REST API with proper error handling
- Frontend form with comprehensive validation

---

## UI Design

### Product Creation Form

**Component:** `ProductCreationForm.tsx`

**Fields:**

- **Product Code** (required, text input with uniqueness check)
- **Description** (required, textarea, max 500 characters)
- **Primary Barcode** (required, text input with format validation)
- **Unit of Measure** (required, dropdown: EA, CS, PK, BOX, PAL)
- **Category** (optional, text input with autocomplete)
- **Brand** (optional, text input with autocomplete)
- **Secondary Barcodes** (optional, dynamic list with add/remove)

**Validation:**

- Real-time validation for all fields
- Product code uniqueness check (debounced API call)
- Barcode format validation (EAN-13, Code 128, etc.)
- Description length validation
- Clear error messages for each field

**Actions:**

- **Save Draft** - Save to localStorage for later completion
- **Create Product** - Submit form to create product
- **Cancel** - Navigate back to product list
- **Add Secondary Barcode** - Add another barcode field
- **Remove Secondary Barcode** - Remove a secondary barcode

**UI Flow:**

1. User navigates to "Products" → "Create Product"
2. Form displays with all fields
3. User enters product code (system checks uniqueness in real-time)
4. User enters description and other required fields
5. User optionally adds secondary barcodes
6. User can save as draft at any time
7. User clicks "Create Product"
8. System validates and creates product
9. Success message displayed
10. User redirected to product detail page

### Product Update Form

**Component:** `ProductUpdateForm.tsx`

**Features:**

- Pre-populated with existing product data
- Same validation as creation form
- Product code is read-only (cannot be changed)
- Update button instead of create
- Publish `ProductUpdatedEvent` on update

### Product List View

**Component:** `ProductList.tsx`

**Features:**

- List all products with pagination
- Filter by category, brand, unit of measure
- Search by product code, description, barcode
- Sort by product code, description, category
- Actions: View, Edit, Delete
- Display primary barcode with QR code option

### Product Detail View

**Component:** `ProductDetail.tsx`

**Features:**

- Display product details
- Display all barcodes (primary and secondary)
- Print barcode option
- Edit product option
- Delete product option

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
    
    public void updatePrimaryBarcode(ProductBarcode barcode) { ... }
    
    public void addSecondaryBarcode(ProductBarcode barcode) { ... }
    
    public void removeSecondaryBarcode(ProductBarcode barcode) { ... }
    
    public boolean hasBarcode(String barcodeValue) { ... }
}
```

**Note:** Domain model is shared with CSV upload story. See [02-Product-CSV-Upload-Implementation-Plan.md](02-Product-CSV-Upload-Implementation-Plan.md) for complete domain model
details.

---

## Backend Implementation

### Phase 1: Application Service

**Module:** `product-domain/product-application-service`

**Command Handler:**

```java
@Component
public class CreateProductCommandHandler {
    
    private final ProductRepository repository;
    private final ProductEventPublisher eventPublisher;
    
    @Transactional
    public CreateProductResult handle(CreateProductCommand command) {
        // 1. Validate product code uniqueness
        validateProductCodeUniqueness(command.getProductCode(), command.getTenantId());
        
        // 2. Validate barcode uniqueness
        validateBarcodeUniqueness(command.getPrimaryBarcode(), command.getTenantId());
        
        // 3. Validate secondary barcodes uniqueness
        if (command.getSecondaryBarcodes() != null) {
            for (ProductBarcode barcode : command.getSecondaryBarcodes()) {
                validateBarcodeUniqueness(barcode, command.getTenantId());
            }
        }
        
        // 4. Create product aggregate
        Product product = Product.builder()
            .productId(ProductId.generate())
            .tenantId(command.getTenantId())
            .productCode(ProductCode.of(command.getProductCode()))
            .description(command.getDescription())
            .primaryBarcode(ProductBarcode.of(command.getPrimaryBarcode()))
            .unitOfMeasure(UnitOfMeasure.valueOf(command.getUnitOfMeasure()))
            .category(command.getCategory())
            .brand(command.getBrand())
            .build();
        
        // 5. Add secondary barcodes
        if (command.getSecondaryBarcodes() != null) {
            for (String barcodeValue : command.getSecondaryBarcodes()) {
                product.addSecondaryBarcode(ProductBarcode.of(barcodeValue));
            }
        }
        
        // 6. Persist
        repository.save(product);
        
        // 7. Publish events
        eventPublisher.publish(product.getDomainEvents());
        product.clearDomainEvents();
        
        // 8. Return result
        return CreateProductResult.builder()
            .productId(product.getId())
            .productCode(product.getProductCode())
            .primaryBarcode(product.getPrimaryBarcode())
            .description(product.getDescription())
            .unitOfMeasure(product.getUnitOfMeasure())
            .build();
    }
    
    private void validateProductCodeUniqueness(String productCode, TenantId tenantId) {
        if (repository.existsByProductCodeAndTenantId(
            ProductCode.of(productCode),
            tenantId
        )) {
            throw new ProductCodeAlreadyExistsException(productCode);
        }
    }
    
    private void validateBarcodeUniqueness(String barcode, TenantId tenantId) {
        if (repository.existsByBarcodeAndTenantId(barcode, tenantId)) {
            throw new BarcodeAlreadyExistsException(barcode);
        }
    }
}
```

**Update Command Handler:**

```java
@Component
public class UpdateProductCommandHandler {
    
    private final ProductRepository repository;
    private final ProductEventPublisher eventPublisher;
    
    @Transactional
    public UpdateProductResult handle(UpdateProductCommand command) {
        // 1. Load existing product
        Product product = repository.findByIdAndTenantId(
            command.getProductId(),
            command.getTenantId()
        ).orElseThrow(() -> new ProductNotFoundException(command.getProductId()));
        
        // 2. Validate barcode uniqueness if primary barcode changed
        if (command.getPrimaryBarcode() != null && 
            !product.getPrimaryBarcode().getValue().equals(command.getPrimaryBarcode())) {
            validateBarcodeUniqueness(command.getPrimaryBarcode(), command.getTenantId());
        }
        
        // 3. Update product
        if (command.getDescription() != null) {
            product.updateDescription(command.getDescription());
        }
        
        if (command.getPrimaryBarcode() != null) {
            product.updatePrimaryBarcode(ProductBarcode.of(command.getPrimaryBarcode()));
        }
        
        if (command.getCategory() != null) {
            product.updateCategory(command.getCategory());
        }
        
        if (command.getBrand() != null) {
            product.updateBrand(command.getBrand());
        }
        
        // 4. Handle secondary barcodes
        if (command.getSecondaryBarcodes() != null) {
            // Remove existing secondary barcodes not in command
            List<ProductBarcode> toRemove = product.getSecondaryBarcodes().stream()
                .filter(barcode -> !command.getSecondaryBarcodes().contains(barcode.getValue()))
                .collect(Collectors.toList());
            
            toRemove.forEach(product::removeSecondaryBarcode);
            
            // Add new secondary barcodes
            for (String barcodeValue : command.getSecondaryBarcodes()) {
                if (!product.hasBarcode(barcodeValue)) {
                    validateBarcodeUniqueness(barcodeValue, command.getTenantId());
                    product.addSecondaryBarcode(ProductBarcode.of(barcodeValue));
                }
            }
        }
        
        // 5. Persist
        repository.save(product);
        
        // 6. Publish events
        eventPublisher.publish(product.getDomainEvents());
        product.clearDomainEvents();
        
        // 7. Return result
        return UpdateProductResult.builder()
            .productId(product.getId())
            .productCode(product.getProductCode())
            .description(product.getDescription())
            .build();
    }
}
```

**Query Handler:**

```java
@Component
public class GetProductQueryHandler {
    
    private final ProductRepository repository;
    
    @Transactional(readOnly = true)
    public ProductQueryResult handle(GetProductQuery query) {
        Product product = repository.findByIdAndTenantId(
            query.getProductId(),
            query.getTenantId()
        ).orElseThrow(() -> new ProductNotFoundException(query.getProductId()));
        
        return ProductQueryResult.builder()
            .productId(product.getId())
            .productCode(product.getProductCode())
            .description(product.getDescription())
            .primaryBarcode(product.getPrimaryBarcode())
            .secondaryBarcodes(product.getSecondaryBarcodes())
            .unitOfMeasure(product.getUnitOfMeasure())
            .category(product.getCategory())
            .brand(product.getBrand())
            .createdAt(product.getCreatedAt())
            .lastModifiedAt(product.getLastModifiedAt())
            .build();
    }
}
```

**Product Code Uniqueness Check Query:**

```java
@Component
public class CheckProductCodeUniquenessQueryHandler {
    
    private final ProductRepository repository;
    
    @Transactional(readOnly = true)
    public ProductCodeUniquenessResult handle(CheckProductCodeUniquenessQuery query) {
        boolean exists = repository.existsByProductCodeAndTenantId(
            ProductCode.of(query.getProductCode()),
            query.getTenantId()
        );
        
        return ProductCodeUniquenessResult.builder()
            .productCode(query.getProductCode())
            .isUnique(!exists)
            .build();
    }
}
```

### Phase 2: REST API

**Module:** `product-application`

**Command Controller:**

```java
@RestController
@RequestMapping("/api/v1/product-service/products")
@Tag(name = "Product Commands", description = "Product command operations")
public class ProductCommandController {
    
    private final CreateProductCommandHandler createHandler;
    private final UpdateProductCommandHandler updateHandler;
    private final ProductDTOMapper mapper;
    
    @PostMapping
    @Operation(summary = "Create a new product")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CreateProductResultDTO>> createProduct(
            @Valid @RequestBody CreateProductCommandDTO commandDTO,
            @RequestHeader("X-Tenant-Id") String tenantId
    ) {
        CreateProductCommand command = mapper.toCreateCommand(commandDTO, TenantId.of(tenantId));
        CreateProductResult result = createHandler.handle(command);
        CreateProductResultDTO resultDTO = mapper.toCreateResultDTO(result);
        
        return ApiResponseBuilder.created(resultDTO);
    }
    
    @PutMapping("/{productId}")
    @Operation(summary = "Update an existing product")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UpdateProductResultDTO>> updateProduct(
            @PathVariable String productId,
            @Valid @RequestBody UpdateProductCommandDTO commandDTO,
            @RequestHeader("X-Tenant-Id") String tenantId
    ) {
        UpdateProductCommand command = mapper.toUpdateCommand(
            productId,
            commandDTO,
            TenantId.of(tenantId)
        );
        UpdateProductResult result = updateHandler.handle(command);
        UpdateProductResultDTO resultDTO = mapper.toUpdateResultDTO(result);
        
        return ApiResponseBuilder.ok(resultDTO);
    }
}
```

**Query Controller:**

```java
@RestController
@RequestMapping("/api/v1/product-service/products")
@Tag(name = "Product Queries", description = "Product query operations")
public class ProductQueryController {
    
    private final GetProductQueryHandler queryHandler;
    private final CheckProductCodeUniquenessQueryHandler uniquenessHandler;
    private final ProductDTOMapper mapper;
    
    @GetMapping("/{productId}")
    @Operation(summary = "Get product by ID")
    @PreAuthorize("hasRole('VIEWER')")
    public ResponseEntity<ApiResponse<ProductQueryResultDTO>> getProduct(
            @PathVariable String productId,
            @RequestHeader("X-Tenant-Id") String tenantId
    ) {
        GetProductQuery query = GetProductQuery.builder()
            .productId(ProductId.of(UUID.fromString(productId)))
            .tenantId(TenantId.of(tenantId))
            .build();
        
        ProductQueryResult result = queryHandler.handle(query);
        ProductQueryResultDTO resultDTO = mapper.toQueryResultDTO(result);
        
        return ApiResponseBuilder.ok(resultDTO);
    }
    
    @GetMapping("/check-uniqueness/{productCode}")
    @Operation(summary = "Check if product code is unique")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProductCodeUniquenessResultDTO>> checkProductCodeUniqueness(
            @PathVariable String productCode,
            @RequestHeader("X-Tenant-Id") String tenantId
    ) {
        CheckProductCodeUniquenessQuery query = CheckProductCodeUniquenessQuery.builder()
            .productCode(productCode)
            .tenantId(TenantId.of(tenantId))
            .build();
        
        ProductCodeUniquenessResult result = uniquenessHandler.handle(query);
        ProductCodeUniquenessResultDTO resultDTO = mapper.toUniquenessResultDTO(result);
        
        return ApiResponseBuilder.ok(resultDTO);
    }
}
```

**DTOs:**

```java
// CreateProductCommandDTO
public class CreateProductCommandDTO {
    @NotBlank
    @Pattern(regexp = "^[A-Z0-9_-]+$", message = "Product code must be alphanumeric with hyphens/underscores")
    private String productCode;
    
    @NotBlank
    @Size(max = 500)
    private String description;
    
    @NotBlank
    private String primaryBarcode;
    
    @NotBlank
    private String unitOfMeasure;
    
    private String category;
    
    private String brand;
    
    private List<String> secondaryBarcodes;
    
    // Getters and setters
}

// UpdateProductCommandDTO
public class UpdateProductCommandDTO {
    @Size(max = 500)
    private String description;
    
    private String primaryBarcode;
    
    private String unitOfMeasure;
    
    private String category;
    
    private String brand;
    
    private List<String> secondaryBarcodes;
    
    // Getters and setters
}

// ProductQueryResultDTO
public class ProductQueryResultDTO {
    private String productId;
    private String productCode;
    private String description;
    private String primaryBarcode;
    private List<String> secondaryBarcodes;
    private String unitOfMeasure;
    private String category;
    private String brand;
    private String createdAt;
    private String lastModifiedAt;
    
    // Getters and setters
}
```

---

## Frontend Implementation

### API Client

**File:** `frontend-app/src/services/productApiClient.ts`

```typescript
export interface CreateProductCommandDTO {
  productCode: string;
  description: string;
  primaryBarcode: string;
  unitOfMeasure: string;
  category?: string;
  brand?: string;
  secondaryBarcodes?: string[];
}

export interface UpdateProductCommandDTO {
  description?: string;
  primaryBarcode?: string;
  unitOfMeasure?: string;
  category?: string;
  brand?: string;
  secondaryBarcodes?: string[];
}

export interface ProductQueryResultDTO {
  productId: string;
  productCode: string;
  description: string;
  primaryBarcode: string;
  secondaryBarcodes: string[];
  unitOfMeasure: string;
  category?: string;
  brand?: string;
  createdAt: string;
  lastModifiedAt: string;
}

export interface ProductCodeUniquenessResultDTO {
  productCode: string;
  isUnique: boolean;
}

class ProductApiClient {
  async createProduct(command: CreateProductCommandDTO): Promise<ProductQueryResultDTO> {
    const response = await apiClient.post<ApiResponse<ProductQueryResultDTO>>(
      '/product-service/products',
      command
    );
    return response.data.data;
  }
  
  async updateProduct(
    productId: string,
    command: UpdateProductCommandDTO
  ): Promise<ProductQueryResultDTO> {
    const response = await apiClient.put<ApiResponse<ProductQueryResultDTO>>(
      `/product-service/products/${productId}`,
      command
    );
    return response.data.data;
  }
  
  async getProduct(productId: string): Promise<ProductQueryResultDTO> {
    const response = await apiClient.get<ApiResponse<ProductQueryResultDTO>>(
      `/product-service/products/${productId}`
    );
    return response.data.data;
  }
  
  async checkProductCodeUniqueness(
    productCode: string
  ): Promise<ProductCodeUniquenessResultDTO> {
    const response = await apiClient.get<ApiResponse<ProductCodeUniquenessResultDTO>>(
      `/product-service/products/check-uniqueness/${productCode}`
    );
    return response.data.data;
  }
}

export const productApiClient = new ProductApiClient();
```

### React Components

**Product Creation Form:**

```typescript
// frontend-app/src/features/product/commands/CreateProductForm.tsx
export const CreateProductForm: React.FC = () => {
  const { register, handleSubmit, formState: { errors }, watch, setValue } = useForm<CreateProductCommandDTO>();
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [secondaryBarcodes, setSecondaryBarcodes] = useState<string[]>([]);
  const [productCodeUniqueness, setProductCodeUniqueness] = useState<boolean | null>(null);
  const navigate = useNavigate();
  
  const productCode = watch('productCode');
  
  // Debounced product code uniqueness check
  useEffect(() => {
    if (!productCode) {
      setProductCodeUniqueness(null);
      return;
    }
    
    const timeoutId = setTimeout(async () => {
      try {
        const result = await productApiClient.checkProductCodeUniqueness(productCode);
        setProductCodeUniqueness(result.isUnique);
      } catch (error) {
        setProductCodeUniqueness(null);
      }
    }, 500);
    
    return () => clearTimeout(timeoutId);
  }, [productCode]);
  
  const addSecondaryBarcode = () => {
    setSecondaryBarcodes([...secondaryBarcodes, '']);
  };
  
  const removeSecondaryBarcode = (index: number) => {
    setSecondaryBarcodes(secondaryBarcodes.filter((_, i) => i !== index));
  };
  
  const saveDraft = () => {
    const formData = watch();
    localStorage.setItem('product-draft', JSON.stringify({
      ...formData,
      secondaryBarcodes
    }));
    toast.success('Draft saved');
  };
  
  const loadDraft = () => {
    const draft = localStorage.getItem('product-draft');
    if (draft) {
      const data = JSON.parse(draft);
      Object.keys(data).forEach(key => {
        if (key !== 'secondaryBarcodes') {
          setValue(key as keyof CreateProductCommandDTO, data[key]);
        }
      });
      setSecondaryBarcodes(data.secondaryBarcodes || []);
    }
  };
  
  const onSubmit = async (data: CreateProductCommandDTO) => {
    setIsSubmitting(true);
    try {
      const command: CreateProductCommandDTO = {
        ...data,
        secondaryBarcodes: secondaryBarcodes.filter(b => b.trim() !== '')
      };
      
      const product = await productApiClient.createProduct(command);
      localStorage.removeItem('product-draft');
      toast.success('Product created successfully');
      navigate(`/products/${product.productId}`);
    } catch (error) {
      toast.error('Failed to create product');
    } finally {
      setIsSubmitting(false);
    }
  };
  
  return (
    <form onSubmit={handleSubmit(onSubmit)}>
      <TextField
        {...register('productCode', {
          required: 'Product code is required',
          pattern: {
            value: /^[A-Z0-9_-]+$/,
            message: 'Product code must be alphanumeric with hyphens/underscores'
          }
        })}
        label="Product Code"
        error={!!errors.productCode || productCodeUniqueness === false}
        helperText={
          errors.productCode?.message ||
          (productCodeUniqueness === false ? 'Product code already exists' : '') ||
          (productCodeUniqueness === true ? 'Product code is available' : '')
        }
      />
      
      <TextField
        {...register('description', {
          required: 'Description is required',
          maxLength: { value: 500, message: 'Description must be 500 characters or less' }
        })}
        label="Description"
        multiline
        rows={3}
        error={!!errors.description}
        helperText={errors.description?.message}
      />
      
      <TextField
        {...register('primaryBarcode', { required: 'Primary barcode is required' })}
        label="Primary Barcode"
        error={!!errors.primaryBarcode}
        helperText={errors.primaryBarcode?.message}
      />
      
      <FormControl>
        <InputLabel>Unit of Measure</InputLabel>
        <Select
          {...register('unitOfMeasure', { required: 'Unit of measure is required' })}
          error={!!errors.unitOfMeasure}
        >
          <MenuItem value="EA">Each</MenuItem>
          <MenuItem value="CS">Case</MenuItem>
          <MenuItem value="PK">Pack</MenuItem>
          <MenuItem value="BOX">Box</MenuItem>
          <MenuItem value="PAL">Pallet</MenuItem>
        </Select>
      </FormControl>
      
      {/* Secondary barcodes */}
      <Box>
        <Typography variant="h6">Secondary Barcodes</Typography>
        {secondaryBarcodes.map((barcode, index) => (
          <Box key={index} display="flex" gap={2}>
            <TextField
              value={barcode}
              onChange={(e) => {
                const newBarcodes = [...secondaryBarcodes];
                newBarcodes[index] = e.target.value;
                setSecondaryBarcodes(newBarcodes);
              }}
              label={`Secondary Barcode ${index + 1}`}
            />
            <IconButton onClick={() => removeSecondaryBarcode(index)}>
              <DeleteIcon />
            </IconButton>
          </Box>
        ))}
        <Button onClick={addSecondaryBarcode}>Add Secondary Barcode</Button>
      </Box>
      
      <Box display="flex" gap={2}>
        <Button onClick={saveDraft} variant="outlined">
          Save Draft
        </Button>
        <Button onClick={loadDraft} variant="outlined">
          Load Draft
        </Button>
        <Button type="submit" disabled={isSubmitting} variant="contained">
          Create Product
        </Button>
      </Box>
    </form>
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
│  │  CreateProductForm                                   │   │
│  │  - User enters product data                          │   │
│  │  - Real-time validation                              │   │
│  │  - Product code uniqueness check (debounced)        │   │
│  │  - Clicks "Create Product"                          │   │
│  └───────────────────┬──────────────────────────────────┘   │
│                      │ POST /api/v1/product-service/products
│                      │ { productCode, description, ... }
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
│  │  - Validates DTO                                      │   │
│  │  - Maps to CreateProductCommand                      │   │
│  └───────────────────┬──────────────────────────────────┘   │
│                      │
│  ┌───────────────────▼──────────────────────────────────┐   │
│  │  CreateProductCommandHandler                         │   │
│  │  - Validates product code uniqueness                 │   │
│  │  - Validates barcode uniqueness                      │   │
│  │  - Creates Product aggregate                         │   │
│  │  - Adds secondary barcodes                           │   │
│  └───────────────────┬──────────────────────────────────┘   │
│                      │
│  ┌───────────────────▼──────────────────────────────────┐   │
│  │  ProductRepositoryAdapter                            │   │
│  │  - Saves ProductEntity to database                   │   │
│  └───────────────────┬──────────────────────────────────┘   │
│                      │
│  ┌───────────────────▼──────────────────────────────────┐   │
│  │  ProductEventPublisher                              │   │
│  │  - Publishes ProductCreatedEvent to Kafka           │   │
│  └───────────────────┬──────────────────────────────────┘   │
└──────────────────────┼───────────────────────────────────────┘
                       │
┌──────────────────────▼───────────────────────────────────────┐
│                    Kafka                                      │
│  Topic: product-events                                        │
│  Event: ProductCreatedEvent                                   │
└───────────────────────────────────────────────────────────────┘
```

---

## Testing Strategy

### Unit Tests

**Domain Core:**

- Product aggregate creation
- Product code validation
- Barcode format validation
- Secondary barcode management
- Business logic methods

**Application Service:**

- Command handler logic
- Validation logic
- Error handling

**Data Access:**

- Repository adapter operations
- Entity mapping

### Integration Tests

- End-to-end product creation
- End-to-end product update
- Database operations
- Event publishing
- Product code uniqueness check

### Gateway API Tests

**File:** `gateway-api-tests/src/test/java/com/ccbsa/wms/gateway/api/ProductManagementTest.java`

```java
@DisplayName("Product Management API Tests")
class ProductManagementTest extends BaseIntegrationTest {
    
    @Test
    @DisplayName("Should create product with valid data")
    void shouldCreateProduct() {
        Map<String, Object> createProductRequest = new HashMap<>();
        createProductRequest.put("productCode", testData.generateUniqueProductCode());
        createProductRequest.put("description", "Test Product");
        createProductRequest.put("primaryBarcode", "6001067101234");
        createProductRequest.put("unitOfMeasure", "EA");
        createProductRequest.put("category", "Beverages");
        createProductRequest.put("brand", "Test Brand");
        
        RequestHeaderHelper.addTenantHeaderIfNeeded(
            webTestClient
                .post()
                .uri("/product-service/products")
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", accessToken))
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(createProductRequest)),
            authHelper,
            accessToken)
            .exchange()
            .expectStatus().isCreated()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.success").isEqualTo(true)
            .jsonPath("$.data.productId").exists()
            .jsonPath("$.data.productCode").exists();
    }
    
    @Test
    @DisplayName("Should reject duplicate product code")
    void shouldRejectDuplicateProductCode() {
        String productCode = testData.generateUniqueProductCode();
        
        // Create first product
        createTestProduct(productCode);
        
        // Try to create duplicate
        Map<String, Object> createProductRequest = new HashMap<>();
        createProductRequest.put("productCode", productCode);
        createProductRequest.put("description", "Duplicate Product");
        createProductRequest.put("primaryBarcode", "6001067101235");
        createProductRequest.put("unitOfMeasure", "EA");
        
        RequestHeaderHelper.addTenantHeaderIfNeeded(
            webTestClient
                .post()
                .uri("/product-service/products")
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", accessToken))
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(createProductRequest)),
            authHelper,
            accessToken)
            .exchange()
            .expectStatus().isBadRequest();
    }
}
```

---

## Acceptance Criteria Validation

| AC  | Description                                                    | Validation Method                |
|-----|----------------------------------------------------------------|----------------------------------|
| AC1 | System provides form-based UI for product data entry           | Frontend test                    |
| AC2 | Form includes all required fields                              | Frontend test                    |
| AC3 | System validates required fields and data formats in real-time | Frontend test + Backend test     |
| AC4 | System validates product code uniqueness                       | Integration test + Frontend test |
| AC5 | System supports adding multiple barcodes per product           | Integration test + Frontend test |
| AC6 | System provides clear validation error messages                | Frontend test + Backend test     |
| AC7 | System allows saving draft products for later completion       | Frontend test                    |
| AC8 | System publishes ProductCreatedEvent or ProductUpdatedEvent    | Integration test                 |

---

## Definition of Done

- [ ] Domain core implemented (shared with CSV upload)
- [ ] Application service implemented with create/update command handlers
- [ ] Product code uniqueness check query handler implemented
- [ ] Data access implemented (shared with CSV upload)
- [ ] REST API implemented with proper DTOs and error handling
- [ ] Event publishing implemented (shared with CSV upload)
- [ ] Frontend form implemented with real-time validation
- [ ] Draft saving functionality implemented
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

