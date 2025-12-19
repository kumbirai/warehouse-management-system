# Test Data Builders and Helper Classes Implementation Plan

## Overview
This document provides comprehensive implementation plans for test data builders, fixture classes, and helper utilities. These components ensure realistic, consistent, and maintainable test data generation across all test classes.

---

## Package Structure

```
gateway-api-tests/src/test/java/com/ccbsa/wms/gateway/api/
├── fixture/
│   ├── TestData.java
│   ├── TenantTestDataBuilder.java
│   ├── UserTestDataBuilder.java
│   ├── ProductTestDataBuilder.java
│   ├── LocationTestDataBuilder.java
│   ├── ConsignmentTestDataBuilder.java
│   ├── PickingTestDataBuilder.java
│   ├── ReturnsTestDataBuilder.java
│   └── ReconciliationTestDataBuilder.java
├── helper/
│   ├── AuthenticationHelper.java
│   ├── TenantHelper.java
│   ├── UserHelper.java
│   └── StockHelper.java
└── util/
    ├── WebTestClientConfig.java
    ├── RequestHeaderHelper.java
    ├── CookieExtractor.java
    └── BarcodeGenerator.java
```

---

## 1. Core Test Data Builder: TestData.java

### Purpose
Central registry and factory for creating common test data with predefined realistic values.

### Implementation

```java
package com.ccbsa.wms.gateway.api.fixture;

import net.datafaker.Faker;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

/**
 * Central test data factory providing realistic test data for all tests.
 * Uses Faker for data generation with consistent locale settings.
 */
public class TestData {

    private static final Faker faker = new Faker(new Locale("en", "US"));

    // ==================== TENANT DATA ====================

    public static String tenantName() {
        return faker.company().name();
    }

    public static String tenantEmail() {
        return faker.internet().emailAddress();
    }

    // ==================== USER DATA ====================

    public static String username() {
        return faker.name().username();
    }

    public static String email() {
        return faker.internet().emailAddress();
    }

    public static String firstName() {
        return faker.name().firstName();
    }

    public static String lastName() {
        return faker.name().lastName();
    }

    public static String password() {
        return "Password123@";
    }

    // ==================== PRODUCT DATA ====================

    public static String productSKU() {
        return "SKU-" + faker.number().digits(6);
    }

    public static String productName() {
        return faker.commerce().productName();
    }

    public static String productDescription() {
        return faker.lorem().sentence();
    }

    public static String barcode() {
        return BarcodeGenerator.generateEAN13();
    }

    public static List<String> secondaryBarcodes(int count) {
        return java.util.stream.IntStream.range(0, count)
                .mapToObj(i -> BarcodeGenerator.generateEAN13())
                .collect(java.util.stream.Collectors.toList());
    }

    public static String productCategory() {
        return faker.options().option("BEVERAGES", "SNACKS", "DAIRY", "FROZEN", "FRESH_PRODUCE");
    }

    public static String unitOfMeasure() {
        return faker.options().option("EA", "PK", "CS", "KG", "LB");
    }

    public static double productWeight() {
        return faker.number().randomDouble(2, 0, 50);
    }

    // ==================== LOCATION DATA ====================

    public static String warehouseCode() {
        return "WH-" + faker.number().digits(2);
    }

    public static String zoneCode() {
        return "ZONE-" + faker.bothify("?").toUpperCase();
    }

    public static String aisleCode() {
        return "AISLE-" + faker.number().digits(2);
    }

    public static String rackCode() {
        return "RACK-" + faker.bothify("?#").toUpperCase();
    }

    public static String binCode() {
        return "BIN-" + faker.number().digits(2);
    }

    public static int locationCapacity(String type) {
        switch (type) {
            case "WAREHOUSE":
                return faker.number().numberBetween(5000, 20000);
            case "ZONE":
                return faker.number().numberBetween(1000, 5000);
            case "AISLE":
                return faker.number().numberBetween(200, 1000);
            case "RACK":
                return faker.number().numberBetween(50, 200);
            case "BIN":
                return faker.number().numberBetween(10, 50);
            default:
                return 100;
        }
    }

    // ==================== STOCK DATA ====================

    public static int stockQuantity() {
        return faker.number().numberBetween(10, 500);
    }

    public static String batchNumber() {
        return "BATCH-" + faker.number().digits(6);
    }

    public static LocalDate manufactureDate() {
        return LocalDate.now().minusDays(faker.number().numberBetween(15, 90));
    }

    public static LocalDate expirationDate() {
        return LocalDate.now().plusMonths(faker.number().numberBetween(3, 12));
    }

    public static String supplierReference() {
        return "PO-" + faker.number().digits(5);
    }

    // ==================== ORDER DATA ====================

    public static String orderId() {
        return "ORDER-" + faker.number().digits(5);
    }

    public static String customerId() {
        return "CUST-" + faker.number().digits(4);
    }

    // ==================== COMMON ====================

    public static Faker faker() {
        return faker;
    }
}
```

---

## 2. Tenant Test Data Builder

```java
package com.ccbsa.wms.gateway.api.fixture;

import com.ccbsa.wms.gateway.api.dto.CreateTenantRequest;
import com.ccbsa.wms.gateway.api.dto.TenantConfiguration;

import java.util.List;
import java.util.Map;

/**
 * Builder for creating tenant test data.
 */
public class TenantTestDataBuilder {

    public static CreateTenantRequest buildCreateTenantRequest() {
        return CreateTenantRequest.builder()
                .name(TestData.tenantName())
                .email(TestData.tenantEmail())
                .configuration(defaultTenantConfiguration())
                .build();
    }

    public static CreateTenantRequest buildCreateTenantRequestWithName(String name) {
        return CreateTenantRequest.builder()
                .name(name)
                .email(TestData.tenantEmail())
                .configuration(defaultTenantConfiguration())
                .build();
    }

    public static TenantConfiguration defaultTenantConfiguration() {
        return TenantConfiguration.builder()
                .maxUsers(100)
                .storageQuotaMB(5120)
                .features(List.of("STOCK_MANAGEMENT", "PICKING", "RETURNS", "RECONCILIATION"))
                .settings(Map.of(
                        "defaultLanguage", "en",
                        "timezone", "UTC",
                        "dateFormat", "yyyy-MM-dd"
                ))
                .build();
    }

    public static TenantConfiguration customTenantConfiguration(int maxUsers, int storageQuotaMB) {
        return TenantConfiguration.builder()
                .maxUsers(maxUsers)
                .storageQuotaMB(storageQuotaMB)
                .features(List.of("STOCK_MANAGEMENT"))
                .settings(Map.of("defaultLanguage", "en"))
                .build();
    }
}
```

---

## 3. User Test Data Builder

```java
package com.ccbsa.wms.gateway.api.fixture;

import com.ccbsa.wms.gateway.api.dto.AssignRoleRequest;
import com.ccbsa.wms.gateway.api.dto.CreateUserRequest;

import java.util.List;

/**
 * Builder for creating user test data.
 */
public class UserTestDataBuilder {

    public static CreateUserRequest buildCreateUserRequest(String tenantId) {
        return CreateUserRequest.builder()
                .username(TestData.username())
                .email(TestData.email())
                .firstName(TestData.firstName())
                .lastName(TestData.lastName())
                .tenantId(tenantId)
                .build();
    }

    public static CreateUserRequest buildCreateUserRequestWithUsername(String username, String tenantId) {
        return CreateUserRequest.builder()
                .username(username)
                .email(TestData.email())
                .firstName(TestData.firstName())
                .lastName(TestData.lastName())
                .tenantId(tenantId)
                .build();
    }

    public static CreateUserRequest buildCreateUserRequestWithRole(String tenantId, String role) {
        // Note: Role assignment happens after user creation
        return buildCreateUserRequest(tenantId);
    }

    public static AssignRoleRequest buildAssignRoleRequest(String roleName) {
        return AssignRoleRequest.builder()
                .roleName(roleName)
                .build();
    }

    /**
     * All available roles in the system.
     */
    public static class Roles {
        public static final String SYSTEM_ADMIN = "SYSTEM_ADMIN";
        public static final String TENANT_ADMIN = "TENANT_ADMIN";
        public static final String WAREHOUSE_MANAGER = "WAREHOUSE_MANAGER";
        public static final String STOCK_MANAGER = "STOCK_MANAGER";
        public static final String LOCATION_MANAGER = "LOCATION_MANAGER";
        public static final String RECONCILIATION_MANAGER = "RECONCILIATION_MANAGER";
        public static final String RETURNS_MANAGER = "RETURNS_MANAGER";
        public static final String PICKER = "PICKER";
        public static final String STOCK_CLERK = "STOCK_CLERK";
        public static final String RECONCILIATION_CLERK = "RECONCILIATION_CLERK";
        public static final String RETURNS_CLERK = "RETURNS_CLERK";
        public static final String OPERATOR = "OPERATOR";
        public static final String VIEWER = "VIEWER";
    }
}
```

---

## 4. Product Test Data Builder

```java
package com.ccbsa.wms.gateway.api.fixture;

import com.ccbsa.wms.gateway.api.dto.CreateProductRequest;
import com.ccbsa.wms.gateway.api.dto.ProductDimensions;

import java.util.List;

/**
 * Builder for creating product test data.
 */
public class ProductTestDataBuilder {

    public static CreateProductRequest buildCreateProductRequest() {
        return CreateProductRequest.builder()
                .sku(TestData.productSKU())
                .name(TestData.productName())
                .description(TestData.productDescription())
                .primaryBarcode(TestData.barcode())
                .secondaryBarcodes(List.of())
                .category(TestData.productCategory())
                .unitOfMeasure(TestData.unitOfMeasure())
                .weight(TestData.productWeight())
                .dimensions(defaultProductDimensions())
                .build();
    }

    public static CreateProductRequest buildCreateProductRequestWithBarcode(String barcode) {
        return CreateProductRequest.builder()
                .sku(TestData.productSKU())
                .name(TestData.productName())
                .primaryBarcode(barcode)
                .category(TestData.productCategory())
                .unitOfMeasure(TestData.unitOfMeasure())
                .build();
    }

    public static CreateProductRequest buildCreateProductRequestWithSecondaryBarcodes(int count) {
        return CreateProductRequest.builder()
                .sku(TestData.productSKU())
                .name(TestData.productName())
                .primaryBarcode(TestData.barcode())
                .secondaryBarcodes(TestData.secondaryBarcodes(count))
                .category(TestData.productCategory())
                .unitOfMeasure(TestData.unitOfMeasure())
                .build();
    }

    private static ProductDimensions defaultProductDimensions() {
        return ProductDimensions.builder()
                .length(20.0)
                .width(10.0)
                .height(15.0)
                .unit("CM")
                .build();
    }
}
```

---

## 5. Location Test Data Builder

```java
package com.ccbsa.wms.gateway.api.fixture;

import com.ccbsa.wms.gateway.api.dto.CreateLocationRequest;
import com.ccbsa.wms.gateway.api.dto.LocationDimensions;

/**
 * Builder for creating location test data.
 */
public class LocationTestDataBuilder {

    public static CreateLocationRequest buildWarehouseRequest() {
        return CreateLocationRequest.builder()
                .code(TestData.warehouseCode())
                .name(TestData.faker().company().name() + " Warehouse")
                .type("WAREHOUSE")
                .parentLocationId(null)
                .capacity(TestData.locationCapacity("WAREHOUSE"))
                .dimensions(warehouseDimensions())
                .build();
    }

    public static CreateLocationRequest buildZoneRequest(String parentLocationId) {
        return CreateLocationRequest.builder()
                .code(TestData.zoneCode())
                .name("Zone " + TestData.zoneCode())
                .type("ZONE")
                .parentLocationId(parentLocationId)
                .capacity(TestData.locationCapacity("ZONE"))
                .build();
    }

    public static CreateLocationRequest buildAisleRequest(String parentLocationId) {
        return CreateLocationRequest.builder()
                .code(TestData.aisleCode())
                .name("Aisle " + TestData.aisleCode())
                .type("AISLE")
                .parentLocationId(parentLocationId)
                .capacity(TestData.locationCapacity("AISLE"))
                .build();
    }

    public static CreateLocationRequest buildRackRequest(String parentLocationId) {
        return CreateLocationRequest.builder()
                .code(TestData.rackCode())
                .name("Rack " + TestData.rackCode())
                .type("RACK")
                .parentLocationId(parentLocationId)
                .capacity(TestData.locationCapacity("RACK"))
                .build();
    }

    public static CreateLocationRequest buildBinRequest(String parentLocationId) {
        return CreateLocationRequest.builder()
                .code(TestData.binCode())
                .name("Bin " + TestData.binCode())
                .type("BIN")
                .parentLocationId(parentLocationId)
                .capacity(TestData.locationCapacity("BIN"))
                .build();
    }

    private static LocationDimensions warehouseDimensions() {
        return LocationDimensions.builder()
                .length(100.0)
                .width(80.0)
                .height(10.0)
                .unit("M")
                .build();
    }
}
```

---

## 6. Barcode Generator Utility

```java
package com.ccbsa.wms.gateway.api.util;

import java.util.Random;

/**
 * Utility for generating valid barcodes (EAN-13, UPC-A).
 */
public class BarcodeGenerator {

    private static final Random random = new Random();

    /**
     * Generate valid EAN-13 barcode with checksum.
     *
     * @return 13-digit EAN-13 barcode
     */
    public static String generateEAN13() {
        // Generate 12 random digits
        long baseNumber = random.nextLong(100000000000L, 999999999999L);
        String base = String.valueOf(baseNumber);

        // Calculate checksum
        int checksum = calculateEAN13Checksum(base);

        return base + checksum;
    }

    /**
     * Generate valid UPC-A barcode with checksum.
     *
     * @return 12-digit UPC-A barcode
     */
    public static String generateUPCA() {
        // Generate 11 random digits
        long baseNumber = random.nextLong(10000000000L, 99999999999L);
        String base = String.valueOf(baseNumber);

        // Calculate checksum
        int checksum = calculateUPCAChecksum(base);

        return base + checksum;
    }

    /**
     * Calculate EAN-13 checksum digit.
     *
     * @param code 12-digit base code
     * @return checksum digit (0-9)
     */
    public static int calculateEAN13Checksum(String code) {
        int sum = 0;
        for (int i = 0; i < 12; i++) {
            int digit = Character.getNumericValue(code.charAt(i));
            sum += (i % 2 == 0) ? digit : digit * 3;
        }
        return (10 - (sum % 10)) % 10;
    }

    /**
     * Calculate UPC-A checksum digit.
     *
     * @param code 11-digit base code
     * @return checksum digit (0-9)
     */
    public static int calculateUPCAChecksum(String code) {
        int sum = 0;
        for (int i = 0; i < 11; i++) {
            int digit = Character.getNumericValue(code.charAt(i));
            sum += (i % 2 == 0) ? digit * 3 : digit;
        }
        return (10 - (sum % 10)) % 10;
    }

    /**
     * Validate EAN-13 barcode checksum.
     *
     * @param barcode 13-digit EAN-13 barcode
     * @return true if checksum is valid
     */
    public static boolean isValidEAN13(String barcode) {
        if (barcode == null || barcode.length() != 13) {
            return false;
        }

        String base = barcode.substring(0, 12);
        int expectedChecksum = Character.getNumericValue(barcode.charAt(12));
        int actualChecksum = calculateEAN13Checksum(base);

        return expectedChecksum == actualChecksum;
    }
}
```

---

## 7. Stock Test Data Builder

```java
package com.ccbsa.wms.gateway.api.fixture;

import com.ccbsa.wms.gateway.api.dto.CreateConsignmentRequest;

/**
 * Builder for creating stock consignment test data.
 */
public class ConsignmentTestDataBuilder {

    public static CreateConsignmentRequest buildCreateConsignmentRequest(String productId, String locationId) {
        return CreateConsignmentRequest.builder()
                .productId(productId)
                .locationId(locationId)
                .quantity(TestData.stockQuantity())
                .batchNumber(TestData.batchNumber())
                .expirationDate(TestData.expirationDate())
                .manufactureDate(TestData.manufactureDate())
                .supplierReference(TestData.supplierReference())
                .receivedDate(java.time.LocalDate.now())
                .build();
    }

    public static CreateConsignmentRequest buildCreateConsignmentRequestWithQuantity(
            String productId, String locationId, int quantity) {
        return CreateConsignmentRequest.builder()
                .productId(productId)
                .locationId(locationId)
                .quantity(quantity)
                .batchNumber(TestData.batchNumber())
                .expirationDate(TestData.expirationDate())
                .manufactureDate(TestData.manufactureDate())
                .supplierReference(TestData.supplierReference())
                .receivedDate(java.time.LocalDate.now())
                .build();
    }

    public static CreateConsignmentRequest buildCreateConsignmentRequestWithExpiration(
            String productId, String locationId, java.time.LocalDate expirationDate) {
        return CreateConsignmentRequest.builder()
                .productId(productId)
                .locationId(locationId)
                .quantity(TestData.stockQuantity())
                .batchNumber(TestData.batchNumber())
                .expirationDate(expirationDate)
                .manufactureDate(TestData.manufactureDate())
                .supplierReference(TestData.supplierReference())
                .receivedDate(java.time.LocalDate.now())
                .build();
    }
}
```

---

## 8. Helper Classes

### TenantHelper

```java
package com.ccbsa.wms.gateway.api.helper;

import com.ccbsa.wms.gateway.api.dto.AuthenticationResult;
import com.ccbsa.wms.gateway.api.dto.CreateTenantRequest;
import com.ccbsa.wms.gateway.api.dto.CreateTenantResponse;
import com.ccbsa.wms.gateway.api.fixture.TenantTestDataBuilder;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Helper for tenant-related test operations.
 */
public class TenantHelper {

    private final WebTestClient webTestClient;

    public TenantHelper(WebTestClient webTestClient) {
        this.webTestClient = webTestClient;
    }

    /**
     * Create tenant and return tenant ID.
     */
    public String createTenant(AuthenticationResult auth) {
        CreateTenantRequest request = TenantTestDataBuilder.buildCreateTenantRequest();

        CreateTenantResponse response = webTestClient.post()
                .uri("/api/v1/tenants")
                .headers(headers -> {
                    headers.setBearerAuth(auth.getAccessToken());
                })
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(CreateTenantResponse.class)
                .returnResult()
                .getResponseBody();

        return response.getTenantId();
    }

    /**
     * Find first active tenant or create one.
     */
    public String findOrCreateActiveTenant(AuthenticationResult auth) {
        // Try to find existing active tenant
        // If none found, create new tenant
        return createTenant(auth);
    }
}
```

### UserHelper

```java
package com.ccbsa.wms.gateway.api.helper;

import com.ccbsa.wms.gateway.api.dto.AuthenticationResult;
import com.ccbsa.wms.gateway.api.dto.AssignRoleRequest;
import com.ccbsa.wms.gateway.api.dto.CreateUserRequest;
import com.ccbsa.wms.gateway.api.dto.CreateUserResponse;
import com.ccbsa.wms.gateway.api.fixture.UserTestDataBuilder;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Helper for user-related test operations.
 */
public class UserHelper {

    private final WebTestClient webTestClient;

    public UserHelper(WebTestClient webTestClient) {
        this.webTestClient = webTestClient;
    }

    /**
     * Create user and return user ID.
     */
    public String createUser(AuthenticationResult auth, String tenantId) {
        CreateUserRequest request = UserTestDataBuilder.buildCreateUserRequest(tenantId);

        CreateUserResponse response = webTestClient.post()
                .uri("/api/v1/users")
                .headers(headers -> {
                    headers.setBearerAuth(auth.getAccessToken());
                    headers.set("X-Tenant-Id", tenantId);
                })
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(CreateUserResponse.class)
                .returnResult()
                .getResponseBody();

        return response.getUserId();
    }

    /**
     * Create user with specific role.
     */
    public String createUserWithRole(AuthenticationResult auth, String tenantId, String role) {
        String userId = createUser(auth, tenantId);
        assignRole(auth, tenantId, userId, role);
        return userId;
    }

    /**
     * Assign role to user.
     */
    public void assignRole(AuthenticationResult auth, String tenantId, String userId, String role) {
        AssignRoleRequest request = UserTestDataBuilder.buildAssignRoleRequest(role);

        webTestClient.post()
                .uri("/api/v1/users/" + userId + "/roles")
                .headers(headers -> {
                    headers.setBearerAuth(auth.getAccessToken());
                    headers.set("X-Tenant-Id", tenantId);
                })
                .bodyValue(request)
                .exchange()
                .expectStatus().is2xxSuccessful();
    }
}
```

---

## 9. CSV Test Data Generation

### CSV Generator Utility

```java
package com.ccbsa.wms.gateway.api.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Utility for generating CSV test files.
 */
public class CsvTestDataGenerator {

    /**
     * Generate product CSV file.
     */
    public static File generateProductCsv(Path outputPath, int rowCount) throws IOException {
        File csvFile = outputPath.resolve("products-test.csv").toFile();

        try (FileWriter writer = new FileWriter(csvFile)) {
            // Write header
            writer.write("sku,name,description,primaryBarcode,secondaryBarcodes,category,unitOfMeasure,weight\n");

            // Write rows
            for (int i = 0; i < rowCount; i++) {
                writer.write(String.format("%s,%s,%s,%s,,%s,%s,%.2f\n",
                        TestData.productSKU(),
                        TestData.productName(),
                        TestData.productDescription(),
                        TestData.barcode(),
                        TestData.productCategory(),
                        TestData.unitOfMeasure(),
                        TestData.productWeight()
                ));
            }
        }

        return csvFile;
    }

    /**
     * Generate consignment CSV file.
     */
    public static File generateConsignmentCsv(Path outputPath, int rowCount, String productId, String locationId) throws IOException {
        File csvFile = outputPath.resolve("consignments-test.csv").toFile();

        try (FileWriter writer = new FileWriter(csvFile)) {
            // Write header
            writer.write("productId,locationId,quantity,batchNumber,expirationDate,manufactureDate,supplierReference\n");

            // Write rows
            for (int i = 0; i < rowCount; i++) {
                writer.write(String.format("%s,%s,%d,%s,%s,%s,%s\n",
                        productId,
                        locationId,
                        TestData.stockQuantity(),
                        TestData.batchNumber(),
                        TestData.expirationDate(),
                        TestData.manufactureDate(),
                        TestData.supplierReference()
                ));
            }
        }

        return csvFile;
    }
}
```

---

## Usage Examples

### Example 1: Create Complete Test Hierarchy

```java
@BeforeAll
public static void setupCompleteTestEnvironment() {
    // Login as SYSTEM_ADMIN
    systemAdminAuth = loginAsSystemAdmin();

    // Create tenant
    TenantHelper tenantHelper = new TenantHelper(webTestClient);
    testTenantId = tenantHelper.createTenant(systemAdminAuth);

    // Create users with roles
    UserHelper userHelper = new UserHelper(webTestClient);
    warehouseManagerId = userHelper.createUserWithRole(systemAdminAuth, testTenantId, "WAREHOUSE_MANAGER");
    stockClerkId = userHelper.createUserWithRole(systemAdminAuth, testTenantId, "STOCK_CLERK");
    pickerId = userHelper.createUserWithRole(systemAdminAuth, testTenantId, "PICKER");

    // Login as TENANT_ADMIN
    tenantAdminAuth = loginAsTenantAdmin();

    // Create products
    testProductId = createProduct(tenantAdminAuth);

    // Create locations
    testLocationId = createLocation(tenantAdminAuth);

    // Create stock
    testConsignmentId = createConsignment(tenantAdminAuth, testProductId, testLocationId);
}
```

### Example 2: Use Test Data Builders

```java
@Test
public void testCreateProduct() {
    // Simple usage
    CreateProductRequest request = ProductTestDataBuilder.buildCreateProductRequest();

    // Custom barcode
    CreateProductRequest requestWithBarcode = ProductTestDataBuilder.buildCreateProductRequestWithBarcode("7894900011517");

    // Multiple secondary barcodes
    CreateProductRequest requestWithSecondaryBarcodes = ProductTestDataBuilder.buildCreateProductRequestWithSecondaryBarcodes(3);
}
```

---

## Testing Checklist

- [ ] TestData provides all common test data
- [ ] Barcode generator creates valid EAN-13/UPC-A codes
- [ ] All test data builders use TestData factory
- [ ] Helper classes simplify complex test setups
- [ ] CSV generator creates valid test files
- [ ] Faker locale configured consistently (en-US)
- [ ] Test data realistic and varied

---

## Next Steps

1. **Implement all test data builders** in fixture package
2. **Implement helper classes** for common operations
3. **Implement BarcodeGenerator utility** with validation
4. **Create CSV generator** for bulk upload tests
5. **Write unit tests** for utilities (BarcodeGenerator)
6. **Document usage patterns** in test class examples

---

## Notes

- **Faker Locale**: en-US for consistent English test data
- **Barcode Validation**: EAN-13 checksums calculated correctly
- **Test Data Variety**: Faker ensures unique data per test run
- **Helper Classes**: Reduce boilerplate in test setup
- **CSV Generation**: Useful for bulk upload tests
