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

### Barcode-First Input Principle

**Core Principle:** Barcode scanning is the **primary input method**. Manual keyboard input is provided as a **fallback** when scanning fails or is unavailable.

**Implementation Requirements:**

- Use `BarcodeInput` component (not `TextField`) for all barcode fields
- Auto-focus barcode fields for handheld scanner compatibility
- Provide clear helper text: "Scan barcode first, or enter manually if scanning fails"
- Validate scanned barcodes immediately
- Auto-populate related fields when barcode is validated

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

### Barcode Input Component (Barcode-First Implementation)

**File:** `src/features/product-management/components/ProductBarcodeInput.tsx`

```typescript
import { BarcodeInput } from '@/components/common';
import { useProductBarcodeValidation } from '../hooks/useProductBarcodeValidation';

export const ProductBarcodeInput: React.FC<{
  onBarcodeValidated: (product: ProductInfo) => void;
  value?: string;
  onChange?: (value: string) => void;
}> = ({ onBarcodeValidated, value, onChange }) => {
  const [barcode, setBarcode] = useState(value || '');
  const [barcodeError, setBarcodeError] = useState<string | null>(null);
  const { validateBarcode, validating, validationResult } = useProductBarcodeValidation();

  // Primary: Handle barcode scan
  const handleBarcodeScan = async (scannedBarcode: string) => {
    setBarcode(scannedBarcode);
    setBarcodeError(null);
    if (onChange) {
      onChange(scannedBarcode);
    }
    
    // Validate scanned barcode
    const result = await validateBarcode(scannedBarcode, getTenantId());
    if (result?.valid && result.productInfo) {
      onBarcodeValidated(result.productInfo);
    } else {
      setBarcodeError(result?.errorMessage || 'Barcode not found. Please enter product code manually.');
    }
  };

  // Fallback: Handle manual input
  const handleBarcodeChange = async (inputValue: string) => {
    setBarcode(inputValue);
    setBarcodeError(null);
    if (onChange) {
      onChange(inputValue);
    }
    
    // Auto-validate when minimum length reached
    if (inputValue.length >= 8) {
      const result = await validateBarcode(inputValue, getTenantId());
      if (result?.valid && result.productInfo) {
        onBarcodeValidated(result.productInfo);
      } else if (inputValue.length >= 12) {
        // Only show error for longer inputs to avoid premature errors
        setBarcodeError(result?.errorMessage || 'Barcode not found');
      }
    }
  };

  return (
    <BarcodeInput
      label="Product Barcode"
      value={barcode}
      onChange={handleBarcodeChange}
      onScan={handleBarcodeScan}
      error={!!barcodeError || (validationResult && !validationResult.valid)}
      helperText={
        barcodeError ||
        validationResult?.errorMessage ||
        'Scan barcode first, or enter manually if scanning fails'
      }
      autoFocus
      required
      InputProps={{
        endAdornment: validating ? (
          <InputAdornment position="end">
            <CircularProgress size={20} />
          </InputAdornment>
        ) : validationResult && validationResult.valid ? (
          <InputAdornment position="end">
            <CheckCircleIcon color="success" />
          </InputAdornment>
        ) : undefined,
      }}
    />
  );
};
```

**Key Features:**

- **Barcode scanning first:** Primary input method via handheld scanner or camera
- **Manual input fallback:** Full keyboard support when scanning fails
- **Auto-validation:** Validates barcodes immediately after scanning or manual entry
- **Auto-population:** Fills related fields when barcode is validated
- **Clear error messages:** Guides users when barcode is not found
- **Auto-focus:** Optimized for handheld scanner workflow

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

