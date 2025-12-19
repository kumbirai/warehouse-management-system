# ProductManagementTest Implementation Plan

## Overview
`ProductManagementTest` validates product management functionality through the gateway service. Tests authenticate as TENANT_ADMIN and verify product CRUD operations, barcode validation, CSV upload, pagination, and tenant-scoped access control.

---

## Objectives

1. **Product Creation**: Test manual product creation with single and multiple barcodes
2. **Product CSV Upload**: Test bulk product upload via CSV file
3. **Product Queries**: Test list products with pagination, search, and filtering
4. **Product Updates**: Test product updates with barcode management
5. **Product Deletion**: Test product deletion and soft delete
6. **Barcode Validation**: Test primary and secondary barcode uniqueness
7. **Tenant Isolation**: Verify TENANT_ADMIN can only manage products in own tenant
8. **Authorization Checks**: Verify role-based access control
9. **Validation Rules**: Test product data validation (SKU, name, barcodes)
10. **Event Publishing**: Validate ProductCreatedEvent, ProductUpdatedEvent

---

## Test Scenarios

### 1. Product Creation Tests (Manual Entry)

#### Test: Create Product Successfully with Single Barcode
- **Setup**: Login as TENANT_ADMIN
- **Action**: POST `/api/v1/products`
- **Request Body**:
  ```json
  {
    "sku": "SKU-001",
    "name": "Coca-Cola 500ml",
    "description": "Carbonated soft drink",
    "primaryBarcode": "7894900011517",
    "secondaryBarcodes": [],
    "category": "BEVERAGES",
    "unitOfMeasure": "EA",
    "weight": 0.5,
    "dimensions": {
      "length": 20.0,
      "width": 6.5,
      "height": 6.5,
      "unit": "CM"
    }
  }
  ```
- **Assertions**:
  - Status: 201 CREATED
  - Response contains `productId`, `sku`, `name`, `primaryBarcode`
  - ProductCreatedEvent published
  - Product appears in list products query
  - Product created in correct tenant schema

#### Test: Create Product with Multiple Secondary Barcodes
- **Setup**: Login as TENANT_ADMIN
- **Action**: POST `/api/v1/products` with multiple barcodes
- **Request Body**:
  ```json
  {
    "sku": "SKU-002",
    "name": "Coca-Cola 500ml Pack",
    "primaryBarcode": "7894900011524",
    "secondaryBarcodes": [
      "7894900011531",
      "7894900011548",
      "7894900011555"
    ],
    "category": "BEVERAGES",
    "unitOfMeasure": "PK"
  }
  ```
- **Assertions**:
  - Status: 201 CREATED
  - Response includes all secondary barcodes
  - All barcodes unique in database

#### Test: Create Product with Duplicate Primary Barcode
- **Setup**: Login as TENANT_ADMIN, create product with barcode "7894900011517"
- **Action**: POST `/api/v1/products` with same primary barcode
- **Assertions**:
  - Status: 400 BAD REQUEST or 409 CONFLICT
  - Error message indicates duplicate barcode

#### Test: Create Product with Duplicate Secondary Barcode
- **Setup**:
  - Login as TENANT_ADMIN
  - Create product A with secondary barcode "7894900011531"
- **Action**: Create product B with "7894900011531" as primary barcode
- **Assertions**:
  - Status: 400 BAD REQUEST or 409 CONFLICT
  - Error message indicates barcode already exists

#### Test: Create Product with Invalid SKU Format
- **Setup**: Login as TENANT_ADMIN
- **Action**: POST `/api/v1/products` with empty SKU
- **Assertions**:
  - Status: 400 BAD REQUEST
  - Validation error for SKU

#### Test: Create Product with Missing Required Fields
- **Setup**: Login as TENANT_ADMIN
- **Action**: POST `/api/v1/products` with missing `name` or `primaryBarcode`
- **Assertions**:
  - Status: 400 BAD REQUEST
  - Validation errors for missing fields

#### Test: Create Product with Invalid Barcode Format
- **Setup**: Login as TENANT_ADMIN
- **Action**: POST `/api/v1/products` with barcode "INVALID123"
- **Assertions**:
  - Status: 400 BAD REQUEST
  - Validation error for barcode format (EAN-13, UPC-A)

#### Test: Create Product Without Authentication
- **Setup**: No authentication
- **Action**: POST `/api/v1/products` without Bearer token
- **Assertions**:
  - Status: 401 UNAUTHORIZED

---

### 2. Product CSV Upload Tests

#### Test: Upload Valid CSV with Products
- **Setup**: Login as TENANT_ADMIN, prepare CSV file
- **CSV Content**:
  ```csv
  sku,name,description,primaryBarcode,secondaryBarcodes,category,unitOfMeasure,weight
  SKU-CSV-001,Sprite 500ml,Lemon-lime soda,7891234567890,,BEVERAGES,EA,0.5
  SKU-CSV-002,Fanta Orange 500ml,Orange soda,7891234567906,7891234567913|7891234567920,BEVERAGES,EA,0.5
  SKU-CSV-003,Water 1L,Mineral water,7891234567937,,BEVERAGES,EA,1.0
  ```
- **Action**: POST `/api/v1/products/upload/csv` with multipart/form-data
- **Assertions**:
  - Status: 200 OK or 201 CREATED
  - Response contains upload summary:
    - `totalRows: 3`
    - `successCount: 3`
    - `failureCount: 0`
    - `errors: []`
  - All 3 products created in database
  - ProductCreatedEvent published for each product

#### Test: Upload CSV with Duplicate Barcodes
- **Setup**: Login as TENANT_ADMIN, prepare CSV with duplicate barcodes
- **CSV Content**:
  ```csv
  sku,name,primaryBarcode
  SKU-CSV-004,Product A,7891234567944
  SKU-CSV-005,Product B,7891234567944
  ```
- **Action**: POST `/api/v1/products/upload/csv`
- **Assertions**:
  - Status: 200 OK (partial success) or 400 BAD REQUEST
  - Response contains errors:
    - `totalRows: 2`
    - `successCount: 1`
    - `failureCount: 1`
    - `errors: [{row: 2, message: "Duplicate barcode 7891234567944"}]`

#### Test: Upload CSV with Invalid Data
- **Setup**: Login as TENANT_ADMIN, prepare CSV with invalid data
- **CSV Content**:
  ```csv
  sku,name,primaryBarcode
  ,Missing SKU,7891234567951
  SKU-CSV-006,,7891234567968
  SKU-CSV-007,Missing Barcode,
  ```
- **Action**: POST `/api/v1/products/upload/csv`
- **Assertions**:
  - Status: 200 OK (partial success) or 400 BAD REQUEST
  - Response contains validation errors for each row

#### Test: Upload Empty CSV
- **Setup**: Login as TENANT_ADMIN, prepare empty CSV
- **Action**: POST `/api/v1/products/upload/csv` with empty file
- **Assertions**:
  - Status: 400 BAD REQUEST
  - Error message indicates empty file

#### Test: Upload CSV with Invalid Format
- **Setup**: Login as TENANT_ADMIN, upload non-CSV file (e.g., .txt)
- **Action**: POST `/api/v1/products/upload/csv` with .txt file
- **Assertions**:
  - Status: 400 BAD REQUEST or 415 UNSUPPORTED MEDIA TYPE
  - Error message indicates invalid file format

#### Test: Upload Large CSV File
- **Setup**: Login as TENANT_ADMIN, generate CSV with 1000 products
- **Action**: POST `/api/v1/products/upload/csv`
- **Assertions**:
  - Status: 200 OK
  - All 1000 products created (or batch processing limit)
  - Response time acceptable (< 30 seconds)

---

### 3. Product Query Tests

#### Test: List All Products with Pagination
- **Setup**: Login as TENANT_ADMIN, create 15 products
- **Action**: GET `/api/v1/products?page=0&size=10`
- **Assertions**:
  - Status: 200 OK
  - Response contains:
    - `products: [...]` (10 items)
    - Pagination metadata:
      - `page: 0`
      - `size: 10`
      - `totalElements: 15`
      - `totalPages: 2`

#### Test: List Products with Search Filter
- **Setup**: Login as TENANT_ADMIN, create products:
  - "Coca-Cola 500ml"
  - "Coca-Cola 1L"
  - "Sprite 500ml"
- **Action**: GET `/api/v1/products?search=Coca-Cola`
- **Assertions**:
  - Status: 200 OK
  - Response contains only products matching "Coca-Cola" (2 results)

#### Test: List Products with Category Filter
- **Setup**: Login as TENANT_ADMIN, create products in different categories
- **Action**: GET `/api/v1/products?category=BEVERAGES`
- **Assertions**:
  - Status: 200 OK
  - Response contains only BEVERAGES products

#### Test: Get Product by ID
- **Setup**: Login as TENANT_ADMIN, create product
- **Action**: GET `/api/v1/products/{productId}`
- **Assertions**:
  - Status: 200 OK
  - Response contains product details (id, sku, name, barcodes, category)

#### Test: Get Non-Existent Product
- **Setup**: Login as TENANT_ADMIN
- **Action**: GET `/api/v1/products/{randomUUID}`
- **Assertions**:
  - Status: 404 NOT FOUND

#### Test: Get Product by Barcode
- **Setup**: Login as TENANT_ADMIN, create product with barcode "7894900011517"
- **Action**: GET `/api/v1/products/barcode/7894900011517`
- **Assertions**:
  - Status: 200 OK
  - Response contains product with matching barcode

#### Test: Get Product by Non-Existent Barcode
- **Setup**: Login as TENANT_ADMIN
- **Action**: GET `/api/v1/products/barcode/9999999999999`
- **Assertions**:
  - Status: 404 NOT FOUND

---

### 4. Product Update Tests

#### Test: Update Product Details Successfully
- **Setup**: Login as TENANT_ADMIN, create product
- **Action**: PUT `/api/v1/products/{productId}`
- **Request Body**:
  ```json
  {
    "name": "Coca-Cola 500ml Updated",
    "description": "Updated description",
    "category": "BEVERAGES",
    "unitOfMeasure": "EA"
  }
  ```
- **Assertions**:
  - Status: 200 OK
  - Product details updated in database
  - ProductUpdatedEvent published

#### Test: Update Product Add Secondary Barcode
- **Setup**: Login as TENANT_ADMIN, create product with no secondary barcodes
- **Action**: PUT `/api/v1/products/{productId}/barcodes`
- **Request Body**:
  ```json
  {
    "secondaryBarcodes": ["7894900011524", "7894900011531"]
  }
  ```
- **Assertions**:
  - Status: 200 OK
  - Secondary barcodes added to product

#### Test: Update Product Remove Secondary Barcode
- **Setup**: Login as TENANT_ADMIN, create product with secondary barcodes
- **Action**: DELETE `/api/v1/products/{productId}/barcodes/{barcode}`
- **Assertions**:
  - Status: 200 OK or 204 NO CONTENT
  - Barcode removed from product

#### Test: Update Product with Duplicate Barcode
- **Setup**:
  - Create product A with barcode "7894900011517"
  - Create product B
- **Action**: Update product B to add "7894900011517" as secondary barcode
- **Assertions**:
  - Status: 400 BAD REQUEST or 409 CONFLICT
  - Error message indicates duplicate barcode

---

### 5. Product Deletion Tests

#### Test: Delete Product Successfully
- **Setup**: Login as TENANT_ADMIN, create product
- **Action**: DELETE `/api/v1/products/{productId}`
- **Assertions**:
  - Status: 200 OK or 204 NO CONTENT
  - Product marked as deleted (soft delete) or removed from database

#### Test: Delete Non-Existent Product
- **Setup**: Login as TENANT_ADMIN
- **Action**: DELETE `/api/v1/products/{randomUUID}`
- **Assertions**:
  - Status: 404 NOT FOUND

---

### 6. Barcode Validation Tests

#### Test: Validate EAN-13 Barcode
- **Setup**: Login as TENANT_ADMIN
- **Action**: POST `/api/v1/products` with EAN-13 barcode "7894900011517"
- **Assertions**:
  - Status: 201 CREATED
  - Barcode accepted

#### Test: Validate UPC-A Barcode
- **Setup**: Login as TENANT_ADMIN
- **Action**: POST `/api/v1/products` with UPC-A barcode "012345678905"
- **Assertions**:
  - Status: 201 CREATED
  - Barcode accepted

#### Test: Reject Invalid Barcode Length
- **Setup**: Login as TENANT_ADMIN
- **Action**: POST `/api/v1/products` with barcode "123456" (too short)
- **Assertions**:
  - Status: 400 BAD REQUEST
  - Validation error for barcode format

#### Test: Reject Invalid Barcode Checksum
- **Setup**: Login as TENANT_ADMIN
- **Action**: POST `/api/v1/products` with invalid checksum barcode "7894900011518"
- **Assertions**:
  - Status: 400 BAD REQUEST (if checksum validation enabled)
  - Or 201 CREATED (if checksum validation disabled)

---

### 7. Tenant Isolation Tests

#### Test: TENANT_ADMIN Lists Only Own Tenant Products
- **Setup**:
  - Login as SYSTEM_ADMIN, create Tenant A and Tenant B
  - Create 3 products in Tenant A
  - Create 2 products in Tenant B
  - Login as TENANT_ADMIN (Tenant A)
- **Action**: GET `/api/v1/products`
- **Assertions**:
  - Status: 200 OK
  - Response contains only Tenant A products (3 products)
  - Tenant B products not visible

#### Test: TENANT_ADMIN Cannot Access Product from Different Tenant
- **Setup**:
  - Login as SYSTEM_ADMIN, create Tenant A and Tenant B
  - Create product in Tenant B
  - Login as TENANT_ADMIN (Tenant A)
- **Action**: GET `/api/v1/products/{tenantBProductId}`
- **Assertions**:
  - Status: 403 FORBIDDEN or 404 NOT FOUND

---

### 8. Authorization Tests

#### Test: WAREHOUSE_MANAGER Can Read Products
- **Setup**: Create user with WAREHOUSE_MANAGER role, login
- **Action**: GET `/api/v1/products`
- **Assertions**:
  - Status: 200 OK

#### Test: WAREHOUSE_MANAGER Cannot Create Products
- **Setup**: Login as WAREHOUSE_MANAGER
- **Action**: POST `/api/v1/products` with valid data
- **Assertions**:
  - Status: 403 FORBIDDEN (read-only access)

#### Test: VIEWER Can Read Products
- **Setup**: Create user with VIEWER role, login
- **Action**: GET `/api/v1/products`
- **Assertions**:
  - Status: 200 OK

#### Test: VIEWER Cannot Modify Products
- **Setup**: Login as VIEWER
- **Action**: POST `/api/v1/products` with valid data
- **Assertions**:
  - Status: 403 FORBIDDEN

---

### 9. Edge Case Tests

#### Test: Create Product with Very Long Name
- **Setup**: Login as TENANT_ADMIN
- **Action**: POST `/api/v1/products` with 500-character name
- **Assertions**:
  - Status: 400 BAD REQUEST
  - Validation error for name length

#### Test: Create Product with Special Characters in Name
- **Setup**: Login as TENANT_ADMIN
- **Action**: POST `/api/v1/products` with name "Coca-Cola® 500ml"
- **Assertions**:
  - Status: 201 CREATED (special characters allowed)

#### Test: Create Product with Negative Weight
- **Setup**: Login as TENANT_ADMIN
- **Action**: POST `/api/v1/products` with weight `-0.5`
- **Assertions**:
  - Status: 400 BAD REQUEST
  - Validation error for negative weight

#### Test: Concurrent Product Creation
- **Setup**: Login as TENANT_ADMIN
- **Action**: Send 5 concurrent POST requests to create products
- **Assertions**:
  - All requests succeed (201 CREATED)
  - Each product has unique ID and barcode

---

## Test Data Strategy

### Faker Data Generation

```java
private CreateProductRequest createRandomProductRequest() {
    return CreateProductRequest.builder()
            .sku(faker.code().isbn10())
            .name(faker.commerce().productName())
            .description(faker.lorem().sentence())
            .primaryBarcode(generateValidEAN13())
            .category("BEVERAGES")
            .unitOfMeasure("EA")
            .weight(faker.number().randomDouble(2, 0, 10))
            .build();
}

private String generateValidEAN13() {
    // Generate valid EAN-13 barcode with checksum
    long baseNumber = faker.number().numberBetween(100000000000L, 999999999999L);
    int checksum = calculateEAN13Checksum(baseNumber);
    return baseNumber + String.valueOf(checksum);
}
```

---

## Test Class Structure

```java
package com.ccbsa.wms.gateway.api;

import com.ccbsa.wms.gateway.api.dto.AuthenticationResult;
import com.ccbsa.wms.gateway.api.fixture.ProductTestDataBuilder;
import org.junit.jupiter.api.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

import static org.assertj.core.api.Assertions.assertThat;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ProductManagementTest extends BaseIntegrationTest {

    private static AuthenticationResult tenantAdminAuth;
    private static String testTenantId;

    @BeforeAll
    public static void setupTestData() {
        // Login as TENANT_ADMIN
        tenantAdminAuth = loginAsTenantAdmin();
        testTenantId = tenantAdminAuth.getTenantId();
    }

    // ==================== PRODUCT CREATION TESTS ====================

    @Test
    @Order(1)
    public void testCreateProduct_Success() {
        // Arrange
        CreateProductRequest request = ProductTestDataBuilder.buildCreateProductRequest(faker);

        // Act
        WebTestClient.ResponseSpec response = authenticatedPost(
                "/api/v1/products",
                tenantAdminAuth.getAccessToken(),
                testTenantId,
                request
        ).exchange();

        // Assert
        response.expectStatus().isCreated();

        CreateProductResponse product = response.expectBody(CreateProductResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(product).isNotNull();
        assertThat(product.getProductId()).isNotBlank();
        assertThat(product.getSku()).isEqualTo(request.getSku());
        assertThat(product.getPrimaryBarcode()).isEqualTo(request.getPrimaryBarcode());
    }

    // ==================== CSV UPLOAD TESTS ====================

    @Test
    @Order(10)
    public void testUploadProductsCsv_Success() {
        // Arrange
        MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
        bodyBuilder.part("file", new ClassPathResource("test-data/products.csv"));

        // Act
        WebTestClient.ResponseSpec response = webTestClient.post()
                .uri("/api/v1/products/upload/csv")
                .headers(headers -> {
                    RequestHeaderHelper.addAuthHeaders(headers, tenantAdminAuth.getAccessToken());
                    RequestHeaderHelper.addTenantHeader(headers, testTenantId);
                })
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
                .exchange();

        // Assert
        response.expectStatus().isOk();

        CsvUploadResponse uploadResponse = response.expectBody(CsvUploadResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(uploadResponse).isNotNull();
        assertThat(uploadResponse.getTotalRows()).isGreaterThan(0);
        assertThat(uploadResponse.getSuccessCount()).isGreaterThan(0);
    }

    // ... Additional test methods
}
```

---

## Test Fixtures

### ProductTestDataBuilder

Create `gateway-api-tests/src/test/java/com/ccbsa/wms/gateway/api/fixture/ProductTestDataBuilder.java`:

```java
package com.ccbsa.wms.gateway.api.fixture;

import net.datafaker.Faker;

import java.util.List;

public class ProductTestDataBuilder {

    public static CreateProductRequest buildCreateProductRequest(Faker faker) {
        return CreateProductRequest.builder()
                .sku(faker.code().isbn10())
                .name(faker.commerce().productName())
                .description(faker.lorem().sentence())
                .primaryBarcode(generateValidEAN13(faker))
                .secondaryBarcodes(List.of())
                .category("BEVERAGES")
                .unitOfMeasure("EA")
                .weight(faker.number().randomDouble(2, 0, 10))
                .build();
    }

    public static CreateProductRequest buildCreateProductRequestWithBarcode(String barcode, Faker faker) {
        return CreateProductRequest.builder()
                .sku(faker.code().isbn10())
                .name(faker.commerce().productName())
                .primaryBarcode(barcode)
                .category("BEVERAGES")
                .unitOfMeasure("EA")
                .build();
    }

    private static String generateValidEAN13(Faker faker) {
        long baseNumber = faker.number().numberBetween(100000000000L, 999999999999L);
        int checksum = calculateEAN13Checksum(String.valueOf(baseNumber));
        return baseNumber + String.valueOf(checksum);
    }

    private static int calculateEAN13Checksum(String code) {
        int sum = 0;
        for (int i = 0; i < 12; i++) {
            int digit = Character.getNumericValue(code.charAt(i));
            sum += (i % 2 == 0) ? digit : digit * 3;
        }
        return (10 - (sum % 10)) % 10;
    }
}
```

---

## DTOs Required

### CreateProductRequest

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateProductRequest {
    private String sku;
    private String name;
    private String description;
    private String primaryBarcode;
    private List<String> secondaryBarcodes;
    private String category;
    private String unitOfMeasure;
    private Double weight;
    private ProductDimensions dimensions;
}
```

### CreateProductResponse

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateProductResponse {
    private String productId;
    private String sku;
    private String name;
    private String primaryBarcode;
}
```

### CsvUploadResponse

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CsvUploadResponse {
    private Integer totalRows;
    private Integer successCount;
    private Integer failureCount;
    private List<CsvUploadError> errors;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class CsvUploadError {
    private Integer row;
    private String message;
}
```

---

## Environment Variables

```bash
TEST_TENANT_ADMIN_USERNAME=<user-input>
TEST_TENANT_ADMIN_PASSWORD=Password123@
```

---

## Testing Checklist

- [ ] Product creation succeeds with single barcode
- [ ] Product creation succeeds with multiple secondary barcodes
- [ ] Product creation fails with duplicate barcode
- [ ] Product creation fails with invalid data
- [ ] CSV upload succeeds with valid data
- [ ] CSV upload handles duplicate barcodes
- [ ] CSV upload handles invalid data
- [ ] List products with pagination works
- [ ] List products with search filter works
- [ ] Get product by ID returns correct data
- [ ] Get product by barcode works
- [ ] Update product details succeeds
- [ ] Add/remove secondary barcodes works
- [ ] Delete product succeeds
- [ ] Barcode validation enforces EAN-13/UPC-A format
- [ ] Tenant isolation verified
- [ ] Authorization checks prevent unauthorized access
- [ ] ProductCreatedEvent published on creation

---

## Next Steps

1. **Implement ProductManagementTest** with all test scenarios
2. **Create ProductTestDataBuilder** for test data generation
3. **Create DTO classes** for product requests/responses
4. **Prepare test CSV files** in `src/test/resources/test-data/`
5. **Implement barcode generation** with valid checksums
6. **Validate event publishing** (ProductCreatedEvent, ProductUpdatedEvent)
7. **Document test results** and edge cases discovered

---

## Notes

- **Barcode Formats**: Support EAN-13 (13 digits) and UPC-A (12 digits)
- **CSV Delimiter**: Comma-separated, pipe-delimited for multiple secondary barcodes
- **Rate Limiting**: Product service has 100 req/min limit
- **Tenant Isolation**: Products scoped to tenant schema via X-Tenant-Id header
