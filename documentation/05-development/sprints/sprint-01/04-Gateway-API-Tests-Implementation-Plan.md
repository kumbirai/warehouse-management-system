# Gateway API Tests Implementation Plan

## Sprint 1: Gateway API Tests for Location and Product Management

**Purpose:** Create comprehensive gateway API tests to mimic frontend calls to backend through gateway-service  
**Sprint:** Sprint 1  
**Story Points:** Included in individual story estimates

---

## Table of Contents

1. [Overview](#overview)
2. [Test Architecture](#test-architecture)
3. [Test Structure](#test-structure)
4. [Location Management Tests](#location-management-tests)
5. [Product Management Tests](#product-management-tests)
6. [Test Data Management](#test-data-management)
7. [Error Scenario Testing](#error-scenario-testing)
8. [Test Execution](#test-execution)

---

## Overview

### Purpose

Gateway API tests validate that:

1. Frontend calls are correctly routed through gateway to backend services
2. Authentication and authorization work correctly
3. Data flows correctly from frontend → gateway → backend → response
4. Error handling is consistent across services
5. Multi-tenant isolation is enforced

### Test Scope

**Sprint 1 Coverage:**

- Location Management Service endpoints
- Product Service endpoints (CSV upload and manual entry)
- Authentication and authorization
- Error scenarios
- Multi-tenant isolation

### Test Principles

- **Mimic Frontend Calls** - Tests simulate actual frontend HTTP requests
- **End-to-End Validation** - Tests validate complete request/response flow
- **Realistic Test Data** - Use test data builders for realistic scenarios
- **Isolation** - Each test is independent and can run in any order
- **Cleanup** - Tests clean up created data after execution

---

## Test Architecture

### Base Test Infrastructure

**Base Class:** `BaseIntegrationTest.java`

**Features:**

- Authentication setup
- WebTestClient configuration
- Test data builders
- Common utilities
- Cleanup helpers

**Structure:**

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
public abstract class BaseIntegrationTest {
    
    @Autowired
    protected WebTestClient webTestClient;
    
    @Autowired
    protected ObjectMapper objectMapper;
    
    @Autowired
    protected AuthenticationHelper authHelper;
    
    @Autowired
    protected TestData testData;
    
    protected String accessToken;
    
    @BeforeEach
    void setUp() {
        // Authenticate and get access token
        accessToken = authHelper.authenticate();
    }
    
    @AfterEach
    void tearDown() {
        // Cleanup test data if needed
    }
}
```

### Test Utilities

**RequestHeaderHelper:**

- Adds tenant header automatically
- Handles authentication headers
- Provides consistent request building

**TestData:**

- Generates realistic test data
- Provides unique identifiers
- Creates test entities

**AuthenticationHelper:**

- Handles authentication flow
- Manages access tokens
- Provides user context

---

## Test Structure

### Package Structure

```
gateway-api-tests/
└── src/test/java/com/ccbsa/wms/gateway/api/
    ├── BaseIntegrationTest.java
    ├── LocationManagementTest.java
    ├── ProductManagementTest.java
    ├── ProductCsvUploadTest.java
    ├── util/
    │   ├── RequestHeaderHelper.java
    │   ├── WebTestClientConfig.java
    │   └── MockGatewayServer.java
    ├── helper/
    │   └── AuthenticationHelper.java
    ├── fixture/
    │   ├── TestData.java
    │   ├── LocationTestDataBuilder.java
    │   └── ProductTestDataBuilder.java
    └── dto/
        ├── UserContext.java
        ├── LoginRequest.java
        └── LoginResponse.java
```

### Test Naming Convention

- Test class: `{Service}ManagementTest.java`
- Test method: `should{Action}When{Condition}()`
- Display name: `@DisplayName("Should {action} when {condition}")`

---

## Location Management Tests

### Test Class: `LocationManagementTest.java`

**Test Cases:**

1. **Create Location**
    - Should create location with valid data
    - Should generate barcode automatically if not provided
    - Should reject duplicate barcode
    - Should reject invalid coordinates
    - Should require authentication

2. **Get Location**
    - Should get location by ID
    - Should return 404 for non-existent location
    - Should enforce tenant isolation
    - Should require authentication

3. **List Locations**
    - Should list locations with pagination
    - Should filter by zone
    - Should filter by status
    - Should sort by coordinates
    - Should require authentication

4. **Update Location**
    - Should update location status
    - Should update location capacity
    - Should reject invalid updates
    - Should require authentication

**Example Test:**

```java
@DisplayName("Location Management API Tests")
class LocationManagementTest extends BaseIntegrationTest {
    
    @Test
    @DisplayName("Should create location with valid data")
    void shouldCreateLocation() {
        Map<String, Object> createLocationRequest = new HashMap<>();
        createLocationRequest.put("zone", "A");
        createLocationRequest.put("aisle", "01");
        createLocationRequest.put("rack", "01");
        createLocationRequest.put("level", "01");
        createLocationRequest.put("description", "Main storage area");
        
        RequestHeaderHelper.addTenantHeaderIfNeeded(
            webTestClient
                .post()
                .uri("/location-management/locations")
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", accessToken))
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(createLocationRequest)),
            authHelper,
            accessToken)
            .exchange()
            .expectStatus().isCreated()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.success").isEqualTo(true)
            .jsonPath("$.data.locationId").exists()
            .jsonPath("$.data.barcode").exists()
            .jsonPath("$.data.coordinates.zone").isEqualTo("A")
            .jsonPath("$.data.coordinates.aisle").isEqualTo("01")
            .jsonPath("$.data.coordinates.rack").isEqualTo("01")
            .jsonPath("$.data.coordinates.level").isEqualTo("01")
            .jsonPath("$.data.status").isEqualTo("AVAILABLE");
    }
    
    @Test
    @DisplayName("Should reject duplicate barcode")
    void shouldRejectDuplicateBarcode() {
        String barcode = testData.generateUniqueBarcode();
        
        // Create first location with barcode
        createTestLocationWithBarcode(barcode);
        
        // Try to create duplicate
        Map<String, Object> createLocationRequest = new HashMap<>();
        createLocationRequest.put("zone", "B");
        createLocationRequest.put("aisle", "02");
        createLocationRequest.put("rack", "02");
        createLocationRequest.put("level", "02");
        createLocationRequest.put("barcode", barcode);
        
        RequestHeaderHelper.addTenantHeaderIfNeeded(
            webTestClient
                .post()
                .uri("/location-management/locations")
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", accessToken))
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(createLocationRequest)),
            authHelper,
            accessToken)
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody()
            .jsonPath("$.success").isEqualTo(false)
            .jsonPath("$.error.code").isEqualTo("BARCODE_ALREADY_EXISTS");
    }
    
    @Test
    @DisplayName("Should enforce tenant isolation")
    void shouldEnforceTenantIsolation() {
        // Create location in tenant A
        String locationId = createTestLocationInTenant("tenant-a");
        
        // Try to access from tenant B
        RequestHeaderHelper.addTenantHeaderIfNeeded(
            webTestClient
                .get()
                .uri(String.format("/location-management/locations/%s", locationId))
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", accessToken)),
            authHelper,
            accessToken,
            "tenant-b")
            .exchange()
            .expectStatus().isNotFound();
    }
    
    @Test
    @DisplayName("Should require authentication")
    void shouldRequireAuthentication() {
        webTestClient
            .post()
            .uri("/location-management/locations")
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(new HashMap<>()))
            .exchange()
            .expectStatus().isUnauthorized();
    }
    
    // Helper methods
    private String createTestLocationWithBarcode(String barcode) {
        Map<String, Object> request = LocationTestDataBuilder.builder()
            .zone("A")
            .aisle("01")
            .rack("01")
            .level("01")
            .barcode(barcode)
            .build();
        
        return createTestLocation(request);
    }
    
    private String createTestLocation(Map<String, Object> request) {
        byte[] responseBody = RequestHeaderHelper.addTenantHeaderIfNeeded(
            webTestClient
                .post()
                .uri("/location-management/locations")
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", accessToken))
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(request)),
            authHelper,
            accessToken)
            .exchange()
            .expectStatus().isCreated()
            .returnResult()
            .getResponseBody();
        
        try {
            String responseBodyString = new String(responseBody, StandardCharsets.UTF_8);
            JsonNode response = objectMapper.readTree(responseBodyString);
            return response.get("data").get("locationId").asText();
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract location ID", e);
        }
    }
}
```

---

## Product Management Tests

### Test Class: `ProductManagementTest.java`

**Test Cases:**

1. **Create Product**
    - Should create product with valid data
    - Should reject duplicate product code
    - Should reject duplicate barcode
    - Should support multiple secondary barcodes
    - Should require authentication

2. **Update Product**
    - Should update product description
    - Should update product barcodes
    - Should reject invalid updates
    - Should require authentication

3. **Get Product**
    - Should get product by ID
    - Should get product by barcode
    - Should return 404 for non-existent product
    - Should enforce tenant isolation
    - Should require authentication

4. **List Products**
    - Should list products with pagination
    - Should filter by category
    - Should filter by brand
    - Should search by product code
    - Should require authentication

**Example Test:**

```java
@DisplayName("Product Management API Tests")
class ProductManagementTest extends BaseIntegrationTest {
    
    @Test
    @DisplayName("Should create product with valid data")
    void shouldCreateProduct() {
        Map<String, Object> createProductRequest = ProductTestDataBuilder.builder()
            .productCode(testData.generateUniqueProductCode())
            .description("Test Product")
            .primaryBarcode("6001067101234")
            .unitOfMeasure("EA")
            .category("Beverages")
            .brand("Test Brand")
            .secondaryBarcodes(Arrays.asList("6001067101235", "6001067101236"))
            .build();
        
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
            .jsonPath("$.data.productCode").exists()
            .jsonPath("$.data.primaryBarcode").isEqualTo("6001067101234")
            .jsonPath("$.data.secondaryBarcodes").isArray()
            .jsonPath("$.data.secondaryBarcodes.length()").isEqualTo(2);
    }
    
    @Test
    @DisplayName("Should reject duplicate product code")
    void shouldRejectDuplicateProductCode() {
        String productCode = testData.generateUniqueProductCode();
        
        // Create first product
        createTestProduct(productCode);
        
        // Try to create duplicate
        Map<String, Object> createProductRequest = ProductTestDataBuilder.builder()
            .productCode(productCode)
            .description("Duplicate Product")
            .primaryBarcode("6001067101237")
            .unitOfMeasure("EA")
            .build();
        
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
            .expectStatus().isBadRequest()
            .expectBody()
            .jsonPath("$.success").isEqualTo(false)
            .jsonPath("$.error.code").isEqualTo("PRODUCT_CODE_ALREADY_EXISTS");
    }
    
    @Test
    @DisplayName("Should get product by barcode")
    void shouldGetProductByBarcode() {
        String barcode = "6001067101234";
        String productId = createTestProductWithBarcode(barcode);
        
        RequestHeaderHelper.addTenantHeaderIfNeeded(
            webTestClient
                .get()
                .uri(String.format("/product-service/products/by-barcode/%s", barcode))
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", accessToken)),
            authHelper,
            accessToken)
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.success").isEqualTo(true)
            .jsonPath("$.data.productId").isEqualTo(productId)
            .jsonPath("$.data.primaryBarcode").isEqualTo(barcode);
    }
}
```

### Test Class: `ProductCsvUploadTest.java`

**Test Cases:**

1. **CSV Upload**
    - Should upload CSV file successfully
    - Should create products from CSV
    - Should update existing products from CSV
    - Should handle validation errors
    - Should reject file larger than 10MB
    - Should reject invalid CSV format
    - Should require authentication

**Example Test:**

```java
@DisplayName("Product CSV Upload API Tests")
class ProductCsvUploadTest extends BaseIntegrationTest {
    
    @Test
    @DisplayName("Should upload product CSV file successfully")
    void shouldUploadProductCsv() throws IOException {
        // Create test CSV content
        String csvContent = "product_code,description,primary_barcode,unit_of_measure,category,brand\n" +
            "PROD-001,Test Product 1,6001067101234,EA,Beverages,Test Brand\n" +
            "PROD-002,Test Product 2,6001067101235,EA,Beverages,Test Brand\n" +
            "PROD-003,Test Product 3,6001067101236,EA,Beverages,Test Brand";
        
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
            .jsonPath("$.data.totalRows").isEqualTo(3)
            .jsonPath("$.data.createdCount").isEqualTo(3)
            .jsonPath("$.data.updatedCount").isEqualTo(0)
            .jsonPath("$.data.errorCount").isEqualTo(0);
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
            .expectStatus().isBadRequest()
            .expectBody()
            .jsonPath("$.success").isEqualTo(false)
            .jsonPath("$.error.code").isEqualTo("FILE_TOO_LARGE");
    }
    
    @Test
    @DisplayName("Should handle validation errors in CSV")
    void shouldHandleValidationErrors() throws IOException {
        // Create CSV with validation errors
        String csvContent = "product_code,description,primary_barcode,unit_of_measure\n" +
            "PROD-001,Valid Product,6001067101234,EA\n" +
            ",Invalid Product,6001067101235,EA\n" +  // Missing product code
            "PROD-003,Valid Product,,EA\n";  // Missing barcode
        
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "products.csv",
            "text/csv",
            csvContent.getBytes()
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
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.success").isEqualTo(true)
            .jsonPath("$.data.totalRows").isEqualTo(3)
            .jsonPath("$.data.createdCount").isEqualTo(1)
            .jsonPath("$.data.errorCount").isEqualTo(2)
            .jsonPath("$.data.errors").isArray()
            .jsonPath("$.data.errors.length()").isEqualTo(2);
    }
}
```

---

## Test Data Management

### Test Data Builders

**LocationTestDataBuilder:**

```java
public class LocationTestDataBuilder {
    private String zone;
    private String aisle;
    private String rack;
    private String level;
    private String barcode;
    private String description;
    
    public static LocationTestDataBuilder builder() {
        return new LocationTestDataBuilder();
    }
    
    public LocationTestDataBuilder zone(String zone) {
        this.zone = zone;
        return this;
    }
    
    // ... other builder methods
    
    public Map<String, Object> build() {
        Map<String, Object> request = new HashMap<>();
        if (zone != null) request.put("zone", zone);
        if (aisle != null) request.put("aisle", aisle);
        if (rack != null) request.put("rack", rack);
        if (level != null) request.put("level", level);
        if (barcode != null) request.put("barcode", barcode);
        if (description != null) request.put("description", description);
        return request;
    }
}
```

**ProductTestDataBuilder:**

```java
public class ProductTestDataBuilder {
    private String productCode;
    private String description;
    private String primaryBarcode;
    private String unitOfMeasure;
    private String category;
    private String brand;
    private List<String> secondaryBarcodes;
    
    public static ProductTestDataBuilder builder() {
        return new ProductTestDataBuilder();
    }
    
    // ... builder methods
    
    public Map<String, Object> build() {
        Map<String, Object> request = new HashMap<>();
        if (productCode != null) request.put("productCode", productCode);
        if (description != null) request.put("description", description);
        if (primaryBarcode != null) request.put("primaryBarcode", primaryBarcode);
        if (unitOfMeasure != null) request.put("unitOfMeasure", unitOfMeasure);
        if (category != null) request.put("category", category);
        if (brand != null) request.put("brand", brand);
        if (secondaryBarcodes != null) request.put("secondaryBarcodes", secondaryBarcodes);
        return request;
    }
}
```

### Test Data Generation

**TestData Utility:**

```java
@Component
public class TestData {
    
    public String generateUniqueProductCode() {
        return String.format("PROD-%s", UUID.randomUUID().toString().substring(0, 8).toUpperCase());
    }
    
    public String generateUniqueBarcode() {
        return String.format("6001067%06d", new Random().nextInt(1000000));
    }
    
    public String generateUniqueTenantId() {
        return String.format("tenant-%s", UUID.randomUUID().toString().substring(0, 8));
    }
    
    public String generateCompanyName() {
        String[] companies = {"ABC Corp", "XYZ Ltd", "Test Company", "Sample Inc"};
        return companies[new Random().nextInt(companies.length)] + " " + UUID.randomUUID().toString().substring(0, 4);
    }
}
```

---

## Error Scenario Testing

### Common Error Scenarios

1. **Authentication Errors**
    - Missing authentication token
    - Invalid authentication token
    - Expired authentication token

2. **Authorization Errors**
    - Insufficient permissions
    - Wrong role

3. **Validation Errors**
    - Missing required fields
    - Invalid field formats
    - Duplicate values

4. **Business Logic Errors**
    - Duplicate product codes
    - Duplicate barcodes
    - Invalid references

5. **Resource Errors**
    - Non-existent resources
    - Tenant isolation violations

### Error Response Validation

```java
@Test
@DisplayName("Should return standardized error response")
void shouldReturnStandardizedErrorResponse() {
    // Invalid request
    Map<String, Object> invalidRequest = new HashMap<>();
    // Missing required fields
    
    RequestHeaderHelper.addTenantHeaderIfNeeded(
        webTestClient
            .post()
            .uri("/product-service/products")
            .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", accessToken))
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(invalidRequest)),
        authHelper,
        accessToken)
        .exchange()
        .expectStatus().isBadRequest()
        .expectBody()
        .jsonPath("$.success").isEqualTo(false)
        .jsonPath("$.error").exists()
        .jsonPath("$.error.code").exists()
        .jsonPath("$.error.message").exists()
        .jsonPath("$.error.timestamp").exists();
}
```

---

## Test Execution

### Running Tests

**All Tests:**

```bash
mvn test
```

**Specific Test Class:**

```bash
mvn test -Dtest=LocationManagementTest
```

**Specific Test Method:**

```bash
mvn test -Dtest=LocationManagementTest#shouldCreateLocation
```

### Test Configuration

**application-test.yml:**

```yaml
spring:
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
  
  datasource:
    url: jdbc:postgresql://localhost:5432/wms_test
    username: postgres
    password: secret

gateway:
  test:
    base-url: http://localhost:8080
    timeout: 30s
```

### CI/CD Integration

Tests should run:

- On every pull request
- Before merging to main branch
- As part of deployment pipeline

---

## Test Coverage Goals

### Minimum Coverage

- **Location Management:** 100% of endpoints
- **Product Management:** 100% of endpoints
- **Error Scenarios:** All common error cases
- **Authentication/Authorization:** All security scenarios
- **Multi-Tenant Isolation:** All tenant scenarios

### Test Metrics

- **Test Count:** ~30-40 tests for Sprint 1
- **Coverage:** 100% of API endpoints
- **Execution Time:** < 5 minutes for full suite
- **Reliability:** 100% pass rate on clean environment

---

## Definition of Done

- [ ] All location management endpoint tests written
- [ ] All product management endpoint tests written
- [ ] All CSV upload tests written
- [ ] All error scenario tests written
- [ ] All authentication/authorization tests written
- [ ] All multi-tenant isolation tests written
- [ ] Test data builders implemented
- [ ] Test utilities implemented
- [ ] All tests passing
- [ ] Test documentation updated
- [ ] Code reviewed and approved

---

**Document Control**

- **Version:** 1.0
- **Date:** 2025-01
- **Status:** Draft

