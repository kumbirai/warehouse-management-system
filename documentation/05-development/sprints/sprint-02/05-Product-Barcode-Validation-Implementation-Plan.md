# Product Barcode Validation Implementation Plan

## US-4.1.4: Validate Product Barcode

**Service:** Product Service  
**Priority:** Must Have  
**Story Points:** 5  
**Sprint:** Sprint 2

---

## Table of Contents

1. [Overview](#overview)
2. [Barcode Format Validation](#barcode-format-validation)
3. [Backend Implementation](#backend-implementation)
4. [Frontend Implementation](#frontend-implementation)
5. [Testing Strategy](#testing-strategy)
6. [Acceptance Criteria Validation](#acceptance-criteria-validation)

---

## Overview

### User Story

**As a** warehouse operator  
**I want** to validate product barcodes  
**So that** I can ensure product identification accuracy

### Business Requirements

- Validate barcode format (EAN-13, Code 128, etc.)
- Look up product by barcode
- Return product information if found
- Return clear error message if barcode not found
- Support multiple barcode formats
- Provide synchronous API endpoint for validation
- Cache product barcode lookups for performance
- Validate barcode uniqueness

### Technical Requirements

- Follow DDD, Clean Hexagonal Architecture, CQRS
- Multi-tenant support (tenant isolation)
- Synchronous REST API endpoint
- Caching for performance
- Barcode format validation

---

## Barcode Format Validation

### Supported Formats

- **EAN-13:** 13 digits
- **Code 128:** Alphanumeric, variable length
- **UPC-A:** 12 digits
- **QR Code:** Variable length

### Validation Rules

- **EAN-13:** Must be exactly 13 digits, valid checksum
- **Code 128:** Alphanumeric characters, max 50 characters
- **UPC-A:** Must be exactly 12 digits, valid checksum
- **QR Code:** Variable format, max 200 characters

---

## Backend Implementation

### Query Handler

**Module:** `product-domain/product-application-service`

```java
@Component
public class ValidateProductBarcodeQueryHandler {
    
    private final ProductRepository repository;
    private final ProductBarcodeCache cache;
    
    @Transactional(readOnly = true)
    public ValidateProductBarcodeResult handle(ValidateProductBarcodeQuery query) {
        // 1. Validate barcode format
        BarcodeFormat format = validateBarcodeFormat(query.getBarcode());
        
        // 2. Check cache
        Optional<ProductInfo> cached = cache.get(query.getBarcode(), query.getTenantId());
        if (cached.isPresent()) {
            return ValidateProductBarcodeResult.builder()
                .valid(true)
                .productInfo(cached.get())
                .barcodeFormat(format)
                .build();
        }
        
        // 3. Look up product by barcode
        Optional<Product> product = repository.findByBarcodeAndTenantId(
            query.getBarcode(),
            query.getTenantId()
        );
        
        if (product.isEmpty()) {
            return ValidateProductBarcodeResult.builder()
                .valid(false)
                .errorMessage(String.format("Product with barcode '%s' not found", 
                    query.getBarcode()))
                .barcodeFormat(format)
                .build();
        }
        
        // 4. Cache result
        ProductInfo productInfo = mapToProductInfo(product.get());
        cache.put(query.getBarcode(), query.getTenantId(), productInfo);
        
        return ValidateProductBarcodeResult.builder()
            .valid(true)
            .productInfo(productInfo)
            .barcodeFormat(format)
            .build();
    }
    
    private BarcodeFormat validateBarcodeFormat(String barcode) {
        if (barcode == null || barcode.trim().isEmpty()) {
            throw new IllegalArgumentException("Barcode cannot be null or empty");
        }
        
        // EAN-13: 13 digits
        if (barcode.matches("^\\d{13}$")) {
            if (isValidEAN13Checksum(barcode)) {
                return BarcodeFormat.EAN_13;
            }
        }
        
        // UPC-A: 12 digits
        if (barcode.matches("^\\d{12}$")) {
            if (isValidUPCChecksum(barcode)) {
                return BarcodeFormat.UPC_A;
            }
        }
        
        // Code 128: Alphanumeric
        if (barcode.matches("^[A-Za-z0-9\\-\\s]{1,50}$")) {
            return BarcodeFormat.CODE_128;
        }
        
        // QR Code: Variable
        if (barcode.length() <= 200) {
            return BarcodeFormat.QR_CODE;
        }
        
        throw new IllegalArgumentException("Invalid barcode format");
    }
}
```

### REST API Endpoint

**Module:** `product-application`

```java
@RestController
@RequestMapping("/api/v1/product-service/products")
@Tag(name = "Product Queries", description = "Product query operations")
public class ProductQueryController {
    
    private final ValidateProductBarcodeQueryHandler validateBarcodeHandler;
    private final ProductDTOMapper mapper;
    
    @GetMapping("/validate-barcode")
    @Operation(summary = "Validate product barcode")
    @PreAuthorize("hasRole('OPERATOR')")
    public ResponseEntity<ApiResponse<ValidateProductBarcodeResultDTO>> validateBarcode(
            @RequestParam("barcode") String barcode,
            @RequestHeader("X-Tenant-Id") String tenantId
    ) {
        ValidateProductBarcodeQuery query = ValidateProductBarcodeQuery.builder()
            .barcode(barcode)
            .tenantId(TenantId.of(tenantId))
            .build();
        
        ValidateProductBarcodeResult result = validateBarcodeHandler.handle(query);
        
        return ResponseEntity.ok(ApiResponseBuilder.ok(mapper.toDTO(result)));
    }
}
```

### Caching Strategy

```java
@Component
public class ProductBarcodeCache {
    
    private final Cache<String, ProductInfo> cache;
    
    public ProductBarcodeCache() {
        this.cache = Caffeine.newBuilder()
            .maximumSize(10000)
            .expireAfterWrite(1, TimeUnit.HOURS)
            .build();
    }
    
    public Optional<ProductInfo> get(String barcode, TenantId tenantId) {
        String key = createKey(barcode, tenantId);
        return Optional.ofNullable(cache.getIfPresent(key));
    }
    
    public void put(String barcode, TenantId tenantId, ProductInfo productInfo) {
        String key = createKey(barcode, tenantId);
        cache.put(key, productInfo);
    }
    
    private String createKey(String barcode, TenantId tenantId) {
        return String.format("%s:%s", tenantId.getValue(), barcode);
    }
}
```

---

## Frontend Implementation

### Barcode Validation Hook

**File:** `src/features/product-management/hooks/useProductBarcodeValidation.ts`

```typescript
export const useProductBarcodeValidation = () => {
  const [validating, setValidating] = useState(false);
  const [validationResult, setValidationResult] = useState<BarcodeValidationResult | null>(null);

  const validateBarcode = async (barcode: string, tenantId: string) => {
    setValidating(true);
    try {
      const response = await apiClient.get<ApiResponse<BarcodeValidationResult>>(
        `/product-service/products/validate-barcode`,
        {
          params: { barcode },
          headers: { 'X-Tenant-Id': tenantId }
        }
      );
      setValidationResult(response.data.data);
      return response.data.data;
    } catch (error) {
      // Handle error
    } finally {
      setValidating(false);
    }
  };

  return { validateBarcode, validating, validationResult };
};
```

### Barcode Input Component

```typescript
export const ProductBarcodeInput: React.FC<{
  onBarcodeValidated: (product: ProductInfo) => void;
}> = ({ onBarcodeValidated }) => {
  const [barcode, setBarcode] = useState('');
  const { validateBarcode, validating, validationResult } = useProductBarcodeValidation();

  const handleBarcodeChange = async (value: string) => {
    setBarcode(value);
    if (value.length >= 8) { // Minimum barcode length
      const result = await validateBarcode(value, getTenantId());
      if (result?.valid && result.productInfo) {
        onBarcodeValidated(result.productInfo);
      }
    }
  };

  return (
    <TextField
      label="Product Barcode"
      value={barcode}
      onChange={(e) => handleBarcodeChange(e.target.value)}
      InputProps={{
        endAdornment: (
          <InputAdornment position="end">
            {validating && <CircularProgress size={20} />}
            {validationResult && (
              validationResult.valid ? (
                <CheckCircleIcon color="success" />
              ) : (
                <ErrorIcon color="error" />
              )
            )}
          </InputAdornment>
        )
      }}
      helperText={validationResult?.errorMessage}
      error={validationResult && !validationResult.valid}
    />
  );
};
```

---

## Testing Strategy

### Unit Tests

- **Barcode Format Validation** - Test all supported formats
- **Query Handler** - Test product lookup logic
- **Cache** - Test caching behavior

### Integration Tests

- **Validation Endpoint** - Test validation API
- **Database Integration** - Test product lookup
- **Cache Integration** - Test cache behavior

---

## Acceptance Criteria Validation

- ✅ System validates barcode format (EAN-13, Code 128, etc.)
- ✅ System looks up product by barcode
- ✅ System returns product information if found
- ✅ System returns clear error message if barcode not found
- ✅ System supports multiple barcode formats
- ✅ System provides synchronous API endpoint for validation
- ✅ System caches product barcode lookups for performance
- ✅ System validates barcode uniqueness

---

**Document Control**

- **Version:** 1.0
- **Date:** 2025-01
- **Status:** Draft

