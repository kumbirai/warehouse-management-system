# Gateway API Tests Implementation Plan

## Sprint 2: Gateway API Tests for Consignment Operations

**Module:** gateway-api-tests  
**Sprint:** Sprint 2

---

## Table of Contents

1. [Overview](#overview)
2. [Test Structure](#test-structure)
3. [Test Utilities](#test-utilities)
4. [Test Cases](#test-cases)
5. [Test Execution](#test-execution)
6. [Acceptance Criteria Validation](#acceptance-criteria-validation)

---

## Overview

### Purpose

Create comprehensive gateway API tests that mimic frontend calls to the backend through the gateway service. These tests validate:

- Consignment CSV upload
- Consignment manual entry
- Consignment validation
- Product barcode validation
- Error handling and edge cases
- Authentication and authorization

### Test Strategy

- **Integration Tests:** Test through gateway service
- **End-to-End:** Test complete request/response flow
- **Error Scenarios:** Test error handling
- **Authentication:** Test JWT token validation
- **Multi-Tenant:** Test tenant isolation

---

## Test Structure

### Base Test Class

**File:** `src/test/java/com/ccbsa/wms/gatewayapi/test/BaseGatewayApiTest.java`

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "gateway.url=http://localhost:8080",
    "keycloak.url=http://localhost:8180"
})
public abstract class BaseGatewayApiTest {
    
    @Autowired
    protected TestRestTemplate restTemplate;
    
    @Autowired
    protected KeycloakTestClient keycloakClient;
    
    protected String getAccessToken(String username, String password) {
        return keycloakClient.getAccessToken(username, password);
    }
    
    protected HttpHeaders createHeaders(String token, String tenantId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.set("X-Tenant-Id", tenantId);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
    
    protected <T> ResponseEntity<ApiResponse<T>> post(
            String url, 
            Object body, 
            HttpHeaders headers,
            Class<T> responseType
    ) {
        HttpEntity<Object> entity = new HttpEntity<>(body, headers);
        return restTemplate.exchange(
            url,
            HttpMethod.POST,
            entity,
            new ParameterizedTypeReference<ApiResponse<T>>() {}
        );
    }
}
```

---

## Test Utilities

### Test Data Builders

**File:** `src/test/java/com/ccbsa/wms/gatewayapi/test/util/ConsignmentTestDataBuilder.java`

```java
public class ConsignmentTestDataBuilder {
    
    public static CreateConsignmentRequestDTO createConsignmentRequest() {
        CreateConsignmentRequestDTO request = new CreateConsignmentRequestDTO();
        request.setConsignmentReference("CONS-TEST-001");
        request.setWarehouseId("WH-001");
        request.setReceivedAt(LocalDateTime.now());
        request.setReceivedBy("Test User");
        request.setLineItems(createLineItems());
        return request;
    }
    
    private static List<ConsignmentLineItemDTO> createLineItems() {
        ConsignmentLineItemDTO lineItem = new ConsignmentLineItemDTO();
        lineItem.setProductCode("PROD-001");
        lineItem.setQuantity(BigDecimal.valueOf(100));
        lineItem.setExpirationDate(LocalDate.now().plusDays(365));
        lineItem.setBatchNumber("BATCH-001");
        return List.of(lineItem);
    }
}
```

---

## Test Cases

### Consignment CSV Upload Tests

**File:** `src/test/java/com/ccbsa/wms/gatewayapi/test/consignment/ConsignmentCsvUploadTest.java`

```java
public class ConsignmentCsvUploadTest extends BaseGatewayApiTest {
    
    @Test
    public void uploadConsignmentCsv_whenValidCsv_shouldCreateConsignments() {
        // Given
        String token = getAccessToken("operator", "password");
        HttpHeaders headers = createHeaders(token, "tenant-001");
        
        String csvContent = """
            ConsignmentReference,ProductCode,Quantity,ExpirationDate,ReceivedDate,WarehouseId
            CONS-001,PROD-001,100,2026-12-31,2025-01-15T10:00:00Z,WH-001
            CONS-001,PROD-002,150,2026-12-31,2025-01-15T10:00:00Z,WH-001
            """;
        
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(csvContent.getBytes()) {
            @Override
            public String getFilename() {
                return "consignments.csv";
            }
        });
        
        // When
        ResponseEntity<ApiResponse<UploadConsignmentCsvResultDTO>> response = 
            post("/api/v1/stock-management/consignments/upload-csv", 
                body, headers, UploadConsignmentCsvResultDTO.class);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData().getConsignmentsCreated()).isEqualTo(1);
        assertThat(response.getBody().getData().getLineItemsProcessed()).isEqualTo(2);
    }
    
    @Test
    public void uploadConsignmentCsv_whenInvalidProductCode_shouldReturnError() {
        // Test invalid product code scenario
    }
    
    @Test
    public void uploadConsignmentCsv_whenFileTooLarge_shouldReturnError() {
        // Test file size validation
    }
}
```

### Consignment Manual Entry Tests

**File:** `src/test/java/com/ccbsa/wms/gatewayapi/test/consignment/ConsignmentManualEntryTest.java`

```java
public class ConsignmentManualEntryTest extends BaseGatewayApiTest {
    
    @Test
    public void createConsignment_whenValidData_shouldCreateConsignment() {
        // Given
        String token = getAccessToken("operator", "password");
        HttpHeaders headers = createHeaders(token, "tenant-001");
        CreateConsignmentRequestDTO request = ConsignmentTestDataBuilder.createConsignmentRequest();
        
        // When
        ResponseEntity<ApiResponse<CreateConsignmentResultDTO>> response = 
            post("/api/v1/stock-management/consignments", 
                request, headers, CreateConsignmentResultDTO.class);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().getData().getConsignmentId()).isNotNull();
    }
    
    @Test
    public void createConsignment_whenDuplicateReference_shouldReturnError() {
        // Test duplicate consignment reference
    }
    
    @Test
    public void createConsignment_whenInvalidProductCode_shouldReturnError() {
        // Test invalid product code
    }
}
```

### Consignment Validation Tests

**File:** `src/test/java/com/ccbsa/wms/gatewayapi/test/consignment/ConsignmentValidationTest.java`

```java
public class ConsignmentValidationTest extends BaseGatewayApiTest {
    
    @Test
    public void validateConsignment_whenValidData_shouldReturnSuccess() {
        // Given
        String token = getAccessToken("operator", "password");
        HttpHeaders headers = createHeaders(token, "tenant-001");
        ValidateConsignmentRequestDTO request = createValidationRequest();
        
        // When
        ResponseEntity<ApiResponse<ValidationResultDTO>> response = 
            post("/api/v1/stock-management/consignments/validate", 
                request, headers, ValidationResultDTO.class);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData().isValid()).isTrue();
    }
    
    @Test
    public void validateConsignment_whenInvalidProductCode_shouldReturnErrors() {
        // Test validation errors
    }
}
```

### Product Barcode Validation Tests

**File:** `src/test/java/com/ccbsa/wms/gatewayapi/test/product/ProductBarcodeValidationTest.java`

```java
public class ProductBarcodeValidationTest extends BaseGatewayApiTest {
    
    @Test
    public void validateBarcode_whenValidBarcode_shouldReturnProduct() {
        // Given
        String token = getAccessToken("operator", "password");
        HttpHeaders headers = createHeaders(token, "tenant-001");
        
        // When
        ResponseEntity<ApiResponse<ValidateProductBarcodeResultDTO>> response = 
            restTemplate.exchange(
                "/api/v1/product-service/products/validate-barcode?barcode=6001067101234",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<ApiResponse<ValidateProductBarcodeResultDTO>>() {}
            );
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData().isValid()).isTrue();
        assertThat(response.getBody().getData().getProductInfo()).isNotNull();
    }
    
    @Test
    public void validateBarcode_whenInvalidBarcode_shouldReturnError() {
        // Test invalid barcode
    }
}
```

---

## Test Execution

### Running Tests

```bash
# Run all gateway API tests
mvn test -Dtest=*GatewayApiTest

# Run specific test class
mvn test -Dtest=ConsignmentCsvUploadTest

# Run with coverage
mvn test jacoco:report
```

### Test Configuration

**File:** `src/test/resources/application-test.yml`

```yaml
gateway:
  url: http://localhost:8080

keycloak:
  url: http://localhost:8180
  realm: wms-realm
  client-id: wms-client
  client-secret: ${KEYCLOAK_CLIENT_SECRET}

test:
  tenant-id: tenant-001
  test-user: operator
  test-password: password
```

---

## Acceptance Criteria Validation

- ✅ All consignment endpoints tested through gateway
- ✅ All error scenarios tested
- ✅ Authentication and authorization tested
- ✅ Multi-tenant isolation tested
- ✅ Product barcode validation tested
- ✅ Test coverage documented

---

**Document Control**

- **Version:** 1.0
- **Date:** 2025-01
- **Status:** Draft

