# Consignment Validation Implementation Plan

## US-1.1.4: Validate Consignment Data

**Service:** Stock Management Service  
**Priority:** Must Have  
**Story Points:** 5  
**Sprint:** Sprint 2

---

## Table of Contents

1. [Overview](#overview)
2. [Validation Rules](#validation-rules)
3. [Backend Implementation](#backend-implementation)
4. [Frontend Implementation](#frontend-implementation)
5. [Testing Strategy](#testing-strategy)
6. [Acceptance Criteria Validation](#acceptance-criteria-validation)

---

## Overview

### User Story

**As a** warehouse operator  
**I want** to validate consignment data before processing  
**So that** I can identify and fix errors early

### Business Requirements

- Validate consignment reference uniqueness
- Validate product codes exist in product master data
- Validate quantities are positive
- Validate expiration dates are in the future (if provided)
- Validate received dates are not in the future
- Validate warehouse IDs are valid
- Provide detailed validation error messages
- Support batch validation for CSV uploads
- Return validation results before processing

### Technical Requirements

- Follow DDD, Clean Hexagonal Architecture, CQRS
- Multi-tenant support (tenant isolation)
- Synchronous validation service
- Product Service integration for product validation
- Comprehensive error reporting

---

## Validation Rules

### Consignment Reference Validation

- **Uniqueness:** Consignment reference must be unique per tenant
- **Format:** Alphanumeric with hyphens/underscores, max 50 characters
- **Required:** Cannot be null or empty

### Product Code Validation

- **Existence:** Product code must exist in product master data (validated via Product Service)
- **Format:** Must match product code format
- **Required:** Cannot be null or empty

### Quantity Validation

- **Positive:** Quantity must be greater than 0
- **Type:** Must be a valid decimal number
- **Required:** Cannot be null or empty

### Date Validation

- **Expiration Date:** If provided, must be in the future
- **Received Date:** Cannot be in the future
- **Format:** Must be valid ISO 8601 date/datetime

### Warehouse ID Validation

- **Format:** Alphanumeric, max 50 characters
- **Required:** Cannot be null or empty

---

## Backend Implementation

### Validation Service

**Module:** `stock-management-domain/stock-management-application-service`

```java
@Component
public class ConsignmentValidationService {
    
    private final StockConsignmentRepository consignmentRepository;
    private final ProductServicePort productServicePort;
    
    public ValidationResult validateConsignment(
            ConsignmentReference consignmentReference,
            TenantId tenantId,
            List<ConsignmentLineItem> lineItems,
            WarehouseId warehouseId,
            LocalDateTime receivedAt
    ) {
        ValidationResult.Builder resultBuilder = ValidationResult.builder();
        
        // Validate consignment reference uniqueness
        if (consignmentRepository.existsByConsignmentReferenceAndTenantId(
            consignmentReference, 
            tenantId
        )) {
            resultBuilder.addError(ValidationError.builder()
                .field("consignmentReference")
                .message(String.format("Consignment reference '%s' already exists", 
                    consignmentReference.getValue()))
                .build());
        }
        
        // Validate warehouse ID format
        validateWarehouseId(warehouseId, resultBuilder);
        
        // Validate received date
        if (receivedAt.isAfter(LocalDateTime.now())) {
            resultBuilder.addError(ValidationError.builder()
                .field("receivedAt")
                .message("Received date cannot be in the future")
                .build());
        }
        
        // Validate line items
        for (int i = 0; i < lineItems.size(); i++) {
            ConsignmentLineItem lineItem = lineItems.get(i);
            validateLineItem(lineItem, i, tenantId, resultBuilder);
        }
        
        return resultBuilder.build();
    }
    
    private void validateLineItem(
            ConsignmentLineItem lineItem,
            int index,
            TenantId tenantId,
            ValidationResult.Builder resultBuilder
    ) {
        String prefix = String.format("lineItems[%d]", index);
        
        // Validate product exists
        Optional<ProductInfo> productInfo = productServicePort.getProductByCode(
            lineItem.getProductCode().getValue(),
            tenantId
        );
        
        if (productInfo.isEmpty()) {
            resultBuilder.addError(ValidationError.builder()
                .field(prefix + ".productCode")
                .message(String.format("Product code '%s' does not exist in master data", 
                    lineItem.getProductCode().getValue()))
                .build());
        }
        
        // Validate quantity
        if (lineItem.getQuantity().getValue() <= 0) {
            resultBuilder.addError(ValidationError.builder()
                .field(prefix + ".quantity")
                .message("Quantity must be greater than 0")
                .build());
        }
        
        // Validate expiration date
        if (lineItem.getExpirationDate() != null) {
            if (lineItem.getExpirationDate().getValue().isBefore(LocalDate.now())) {
                resultBuilder.addError(ValidationError.builder()
                    .field(prefix + ".expirationDate")
                    .message("Expiration date must be in the future")
                    .build());
            }
        }
    }
}
```

### Validation Command Handler

```java
@Component
public class ValidateConsignmentCommandHandler {
    
    private final ConsignmentValidationService validationService;
    
    @Transactional(readOnly = true)
    public ValidationResult handle(ValidateConsignmentCommand command) {
        return validationService.validateConsignment(
            command.getConsignmentReference(),
            command.getTenantId(),
            command.getLineItems(),
            command.getWarehouseId(),
            command.getReceivedAt()
        );
    }
}
```

### REST API Endpoint

```java
@RestController
@RequestMapping("/api/v1/stock-management/consignments")
public class StockConsignmentCommandController {
    
    private final ValidateConsignmentCommandHandler validateHandler;
    
    @PostMapping("/validate")
    @Operation(summary = "Validate consignment data")
    @PreAuthorize("hasRole('OPERATOR')")
    public ResponseEntity<ApiResponse<ValidationResultDTO>> validate(
            @RequestBody ValidateConsignmentRequestDTO request,
            @RequestHeader("X-Tenant-Id") String tenantId
    ) {
        ValidateConsignmentCommand command = ValidateConsignmentCommand.builder()
            .consignmentReference(ConsignmentReference.of(request.getConsignmentReference()))
            .tenantId(TenantId.of(tenantId))
            .warehouseId(WarehouseId.of(request.getWarehouseId()))
            .receivedAt(request.getReceivedAt())
            .lineItems(mapLineItems(request.getLineItems()))
            .build();
        
        ValidationResult result = validateHandler.handle(command);
        
        return ResponseEntity.ok(ApiResponseBuilder.ok(mapper.toDTO(result)));
    }
}
```

---

## Frontend Implementation

### Validation Hook

**File:** `src/features/consignment-management/hooks/useConsignmentValidation.ts`

```typescript
export const useConsignmentValidation = () => {
  const [validating, setValidating] = useState(false);
  const [validationResult, setValidationResult] = useState<ValidationResult | null>(null);

  const validateConsignment = async (data: ConsignmentFormData) => {
    setValidating(true);
    try {
      const response = await apiClient.post<ApiResponse<ValidationResult>>(
        '/stock-management/consignments/validate',
        data
      );
      setValidationResult(response.data.data);
      return response.data.data;
    } catch (error) {
      // Handle error
    } finally {
      setValidating(false);
    }
  };

  return { validateConsignment, validating, validationResult };
};
```

### Validation Display Component

```typescript
export const ValidationErrors: React.FC<{ errors: ValidationError[] }> = ({ errors }) => {
  return (
    <Alert severity="error">
      <AlertTitle>Validation Errors</AlertTitle>
      <List>
        {errors.map((error, index) => (
          <ListItem key={index}>
            <ListItemText
              primary={error.field}
              secondary={error.message}
            />
          </ListItem>
        ))}
      </List>
    </Alert>
  );
};
```

---

## Testing Strategy

### Unit Tests

- **Validation Service** - Test all validation rules
- **Command Handler** - Test validation flow
- **Error Messages** - Test error message clarity

### Integration Tests

- **Validation Endpoint** - Test validation API
- **Product Service Integration** - Test product validation calls
- **Database Integration** - Test consignment reference uniqueness check

---

## Acceptance Criteria Validation

- ✅ System validates consignment reference uniqueness
- ✅ System validates product codes exist in product master data
- ✅ System validates quantities are positive
- ✅ System validates expiration dates are in the future (if provided)
- ✅ System validates received dates are not in the future
- ✅ System validates warehouse IDs are valid
- ✅ System provides detailed validation error messages
- ✅ System supports batch validation for CSV uploads
- ✅ System returns validation results before processing

---

**Document Control**

- **Version:** 1.0
- **Date:** 2025-01
- **Status:** Draft

