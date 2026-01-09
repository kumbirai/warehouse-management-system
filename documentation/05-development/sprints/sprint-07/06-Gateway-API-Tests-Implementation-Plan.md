# Sprint 7: Gateway API Tests - Implementation Plan

**Sprint:** Sprint 7 - Returns Management
**Module:** Gateway API Tests
**Test Coverage Target:** 100% of Returns Service endpoints
**Priority:** High

---

## Overview

This implementation plan covers **comprehensive end-to-end API testing** for all Returns Management Service endpoints introduced in Sprint 7. The tests validate the complete flow from HTTP request to database persistence and event publishing.

### Scope

The Gateway API Tests validate:

1. **US-7.1.1** - Handle Partial Order Acceptance endpoints
2. **US-7.2.1** - Process Full Order Return endpoints
3. **US-7.3.1** - Handle Damage-in-Transit Returns endpoints
4. **US-7.4.1** - Assign Return Location endpoints
5. **US-7.5.1** - D365 Reconciliation endpoints

---

## Test Architecture

### Test Structure

```
gateway-api-tests/
├── src/test/java/com/ccbsa/wms/gateway/api/
│   ├── ReturnsServiceTest.java                 # Main returns test class
│   ├── DamageAssessmentTest.java               # Damage assessment tests
│   ├── ReturnReconciliationTest.java           # D365 reconciliation tests
│   ├── fixture/
│   │   ├── ReturnsTestDataBuilder.java         # Return test data builder
│   │   ├── DamageAssessmentTestDataBuilder.java
│   │   └── ReconciliationTestDataBuilder.java
│   ├── helper/
│   │   ├── ReturnsHelper.java                  # Returns test helper
│   │   └── ReconciliationHelper.java
│   └── dto/
│       ├── ProcessPartialReturnRequest.java
│       ├── ProcessFullReturnRequest.java
│       ├── RecordDamageAssessmentRequest.java
│       └── RetryD365SyncRequest.java
```

---

## Test Data Builders

### 1. Returns Test Data Builder

**Location:** `gateway-api-tests/src/test/java/com/ccbsa/wms/gateway/api/fixture/ReturnsTestDataBuilder.java`

```java
package com.ccbsa.wms.gateway.api.fixture;

import com.ccbsa.wms.gateway.api.dto.*;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Builder
@AllArgsConstructor
public class ReturnsTestDataBuilder {

    private String orderId;
    private String loadNumber;
    private String customerId;
    private String customerCode;
    private String customerName;
    private List<PartialReturnLineItemRequest> partialLineItems;
    private List<FullReturnLineItemRequest> fullLineItems;

    public static ReturnsTestDataBuilder defaultPartialReturn() {
        return ReturnsTestDataBuilder.builder()
            .orderId(UUID.randomUUID().toString())
            .loadNumber("LOAD-" + System.currentTimeMillis())
            .customerId(UUID.randomUUID().toString())
            .customerCode("CUST-001")
            .customerName("Test Customer Ltd")
            .partialLineItems(createDefaultPartialLineItems())
            .build();
    }

    public static ReturnsTestDataBuilder defaultFullReturn() {
        return ReturnsTestDataBuilder.builder()
            .orderId(UUID.randomUUID().toString())
            .loadNumber("LOAD-" + System.currentTimeMillis())
            .customerId(UUID.randomUUID().toString())
            .customerCode("CUST-001")
            .customerName("Test Customer Ltd")
            .fullLineItems(createDefaultFullReturnLineItems())
            .build();
    }

    private static List<PartialReturnLineItemRequest> createDefaultPartialLineItems() {
        List<PartialReturnLineItemRequest> lineItems = new ArrayList<>();

        // Line 1: Partial acceptance (50 ordered, 45 picked, 40 accepted, 5 returned)
        lineItems.add(PartialReturnLineItemRequest.builder()
            .productId(UUID.randomUUID().toString())
            .productCode("PROD-001")
            .productDescription("Coca-Cola 330ml Can (24-pack)")
            .orderedQuantity(BigDecimal.valueOf(50))
            .pickedQuantity(BigDecimal.valueOf(45))
            .acceptedQuantity(BigDecimal.valueOf(40))
            .returnedQuantity(BigDecimal.valueOf(5))
            .returnReason("DAMAGED_IN_TRANSIT")
            .productCondition("DAMAGED")
            .lineNotes("Minor dents on 5 cases")
            .build());

        // Line 2: Full line acceptance
        lineItems.add(PartialReturnLineItemRequest.builder()
            .productId(UUID.randomUUID().toString())
            .productCode("PROD-002")
            .productDescription("Sprite 2L Bottle (6-pack)")
            .orderedQuantity(BigDecimal.valueOf(30))
            .pickedQuantity(BigDecimal.valueOf(30))
            .acceptedQuantity(BigDecimal.valueOf(30))
            .returnedQuantity(BigDecimal.ZERO)
            .returnReason(null)
            .productCondition("GOOD")
            .lineNotes(null)
            .build());

        return lineItems;
    }

    private static List<FullReturnLineItemRequest> createDefaultFullReturnLineItems() {
        List<FullReturnLineItemRequest> lineItems = new ArrayList<>();

        // All products returned due to customer rejection
        lineItems.add(FullReturnLineItemRequest.builder()
            .productId(UUID.randomUUID().toString())
            .productCode("PROD-001")
            .productDescription("Coca-Cola 330ml Can (24-pack)")
            .orderedQuantity(BigDecimal.valueOf(50))
            .pickedQuantity(BigDecimal.valueOf(50))
            .productCondition("GOOD")
            .lineNotes("Customer rejected entire order - over-ordered")
            .build());

        lineItems.add(FullReturnLineItemRequest.builder()
            .productId(UUID.randomUUID().toString())
            .productCode("PROD-002")
            .productDescription("Sprite 2L Bottle (6-pack)")
            .orderedQuantity(BigDecimal.valueOf(30))
            .pickedQuantity(BigDecimal.valueOf(30))
            .productCondition("GOOD")
            .lineNotes("Customer rejected entire order - over-ordered")
            .build());

        return lineItems;
    }

    public ProcessPartialReturnRequest buildPartialReturnRequest() {
        return ProcessPartialReturnRequest.builder()
            .orderId(orderId)
            .loadNumber(loadNumber)
            .customerId(customerId)
            .customerCode(customerCode)
            .customerName(customerName)
            .lineItems(partialLineItems)
            .customerSignature("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAUA...")
            .evidenceUrls(List.of("https://evidence.example.com/photo1.jpg"))
            .build();
    }

    public ProcessFullReturnRequest buildFullReturnRequest() {
        return ProcessFullReturnRequest.builder()
            .orderId(orderId)
            .loadNumber(loadNumber)
            .customerId(customerId)
            .customerCode(customerCode)
            .customerName(customerName)
            .primaryReturnReason("CUSTOMER_REJECTION")
            .returnNotes("Customer over-ordered, requested full return")
            .lineItems(fullLineItems)
            .evidenceUrls(List.of("https://evidence.example.com/photo1.jpg"))
            .build();
    }

    // Builder methods for customization
    public ReturnsTestDataBuilder withOrderId(String orderId) {
        this.orderId = orderId;
        return this;
    }

    public ReturnsTestDataBuilder withCustomerId(String customerId) {
        this.customerId = customerId;
        return this;
    }

    public ReturnsTestDataBuilder withPartialLineItems(List<PartialReturnLineItemRequest> lineItems) {
        this.partialLineItems = lineItems;
        return this;
    }

    public ReturnsTestDataBuilder withFullLineItems(List<FullReturnLineItemRequest> lineItems) {
        this.fullLineItems = lineItems;
        return this;
    }
}
```

---

### 2. Damage Assessment Test Data Builder

**Location:** `gateway-api-tests/src/test/java/com/ccbsa/wms/gateway/api/fixture/DamageAssessmentTestDataBuilder.java`

```java
package com.ccbsa.wms.gateway.api.fixture;

import com.ccbsa.wms.gateway.api.dto.*;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Builder
@AllArgsConstructor
public class DamageAssessmentTestDataBuilder {

    private String orderId;
    private String loadNumber;
    private String damageType;
    private String damageSeverity;
    private String damageSource;
    private List<DamagedProductRequestDTO> damagedProducts;
    private InsuranceClaimRequestDTO insuranceClaim;

    public static DamageAssessmentTestDataBuilder defaultDamageAssessment() {
        return DamageAssessmentTestDataBuilder.builder()
            .orderId(UUID.randomUUID().toString())
            .loadNumber("LOAD-" + System.currentTimeMillis())
            .damageType("CRUSHED")
            .damageSeverity("MODERATE")
            .damageSource("CARRIER")
            .damagedProducts(createDefaultDamagedProducts())
            .insuranceClaim(createDefaultInsuranceClaim())
            .build();
    }

    public static DamageAssessmentTestDataBuilder severeDamageAssessment() {
        return DamageAssessmentTestDataBuilder.builder()
            .orderId(UUID.randomUUID().toString())
            .loadNumber("LOAD-" + System.currentTimeMillis())
            .damageType("BROKEN")
            .damageSeverity("SEVERE")
            .damageSource("CARRIER")
            .damagedProducts(createSevereDamagedProducts())
            .insuranceClaim(createDefaultInsuranceClaim())
            .build();
    }

    private static List<DamagedProductRequestDTO> createDefaultDamagedProducts() {
        List<DamagedProductRequestDTO> products = new ArrayList<>();

        products.add(DamagedProductRequestDTO.builder()
            .productId(UUID.randomUUID().toString())
            .productCode("PROD-001")
            .productDescription("Coca-Cola 330ml Can (24-pack)")
            .orderedQuantity(BigDecimal.valueOf(50))
            .damagedQuantity(BigDecimal.TEN)
            .productCondition("DAMAGED")
            .estimatedUnitLoss(BigDecimal.valueOf(120.00))
            .photoUrls(List.of(
                "https://damage-evidence.example.com/prod001-damage1.jpg",
                "https://damage-evidence.example.com/prod001-damage2.jpg"
            ))
            .damageNotes("Cases crushed on one corner, cans dented but sealed")
            .build());

        return products;
    }

    private static List<DamagedProductRequestDTO> createSevereDamagedProducts() {
        List<DamagedProductRequestDTO> products = new ArrayList<>();

        products.add(DamagedProductRequestDTO.builder()
            .productId(UUID.randomUUID().toString())
            .productCode("PROD-001")
            .productDescription("Coca-Cola 330ml Can (24-pack)")
            .orderedQuantity(BigDecimal.valueOf(50))
            .damagedQuantity(BigDecimal.valueOf(50))
            .productCondition("WRITE_OFF")
            .estimatedUnitLoss(BigDecimal.valueOf(120.00))
            .photoUrls(List.of(
                "https://damage-evidence.example.com/prod001-severe1.jpg",
                "https://damage-evidence.example.com/prod001-severe2.jpg",
                "https://damage-evidence.example.com/prod001-severe3.jpg"
            ))
            .damageNotes("Pallet completely destroyed, all cans punctured and leaking")
            .build());

        return products;
    }

    private static InsuranceClaimRequestDTO createDefaultInsuranceClaim() {
        return InsuranceClaimRequestDTO.builder()
            .claimReference("CLM-2026-" + System.currentTimeMillis())
            .carrierName("ABC Transport Ltd")
            .trackingNumber("TRACK-" + System.currentTimeMillis())
            .incidentDateTime(ZonedDateTime.now().minusDays(1))
            .build();
    }

    public RecordDamageAssessmentRequest buildRequest() {
        return RecordDamageAssessmentRequest.builder()
            .orderId(orderId)
            .loadNumber(loadNumber)
            .damageType(damageType)
            .damageSeverity(damageSeverity)
            .damageSource(damageSource)
            .damagedProducts(damagedProducts)
            .insuranceClaim(insuranceClaim)
            .generalPhotoUrls(List.of("https://evidence.example.com/pallet-damage.jpg"))
            .documentUrls(List.of("https://evidence.example.com/delivery-note.pdf"))
            .damageNotes("Damage discovered during unloading at warehouse")
            .build();
    }

    // Builder methods for customization
    public DamageAssessmentTestDataBuilder withDamageSeverity(String severity) {
        this.damageSeverity = severity;
        return this;
    }

    public DamageAssessmentTestDataBuilder withDamagedProducts(List<DamagedProductRequestDTO> products) {
        this.damagedProducts = products;
        return this;
    }

    public DamageAssessmentTestDataBuilder withoutInsuranceClaim() {
        this.insuranceClaim = null;
        return this;
    }
}
```

---

## Test Classes

### 1. Returns Service Test

**Location:** `gateway-api-tests/src/test/java/com/ccbsa/wms/gateway/api/ReturnsServiceTest.java`

```java
package com.ccbsa.wms.gateway.api;

import com.ccbsa.wms.gateway.api.dto.*;
import com.ccbsa.wms.gateway.api.fixture.ReturnsTestDataBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Returns Service API Tests")
public class ReturnsServiceTest extends BaseIntegrationTest {

    // ========================================================================
    // US-7.1.1: Handle Partial Order Acceptance Tests
    // ========================================================================

    @Test
    @DisplayName("POST /api/returns/partial-return - Should process partial return successfully")
    void processPartialReturn_WithValidRequest_ShouldReturn201() {
        // Arrange
        ProcessPartialReturnRequest request = ReturnsTestDataBuilder
            .defaultPartialReturn()
            .buildPartialReturnRequest();

        // Act & Assert
        webTestClient.post()
            .uri("/api/returns/partial-return")
            .header("X-Tenant-ID", testTenantId)
            .header("Authorization", "Bearer " + authToken)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isCreated()
            .expectBody(ProcessPartialReturnResponse.class)
            .value(response -> {
                assertThat(response.getReturnId()).isNotBlank();
                assertThat(response.getOrderId()).isEqualTo(request.getOrderId());
                assertThat(response.getReturnType()).isEqualTo("PARTIAL");
                assertThat(response.getStatus()).isEqualTo("INITIATED");
                assertThat(response.getTotalLines()).isEqualTo(2);
                assertThat(response.getAcceptedLines()).isEqualTo(2);
                assertThat(response.getReturnedLines()).isEqualTo(1);
                assertThat(response.getTotalAcceptedQuantity()).isEqualByComparingTo(BigDecimal.valueOf(70));
                assertThat(response.getTotalReturnedQuantity()).isEqualByComparingTo(BigDecimal.valueOf(5));
                assertThat(response.getCustomerSignatureUrl()).isNotBlank();
                assertThat(response.getReturnedAt()).isNotNull();
            });
    }

    @Test
    @DisplayName("POST /api/returns/partial-return - Should reject when all items returned (use full return)")
    void processPartialReturn_WithAllItemsReturned_ShouldReturn400() {
        // Arrange
        ProcessPartialReturnRequest request = ReturnsTestDataBuilder
            .defaultPartialReturn()
            .withPartialLineItems(createAllReturnedLineItems())
            .buildPartialReturnRequest();

        // Act & Assert
        webTestClient.post()
            .uri("/api/returns/partial-return")
            .header("X-Tenant-ID", testTenantId)
            .header("Authorization", "Bearer " + authToken)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody()
            .jsonPath("$.message").value(message ->
                assertThat(message.toString()).contains("all items are returned")
            );
    }

    @Test
    @DisplayName("POST /api/returns/partial-return - Should require customer signature")
    void processPartialReturn_WithoutCustomerSignature_ShouldReturn400() {
        // Arrange
        ProcessPartialReturnRequest request = ReturnsTestDataBuilder
            .defaultPartialReturn()
            .buildPartialReturnRequest();
        request.setCustomerSignature(null); // Remove signature

        // Act & Assert
        webTestClient.post()
            .uri("/api/returns/partial-return")
            .header("X-Tenant-ID", testTenantId)
            .header("Authorization", "Bearer " + authToken)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody()
            .jsonPath("$.errors[?(@.field == 'customerSignature')]").exists();
    }

    // ========================================================================
    // US-7.2.1: Process Full Order Return Tests
    // ========================================================================

    @Test
    @DisplayName("POST /api/returns/full-return - Should process full return successfully")
    void processFullReturn_WithValidRequest_ShouldReturn201() {
        // Arrange
        ProcessFullReturnRequest request = ReturnsTestDataBuilder
            .defaultFullReturn()
            .buildFullReturnRequest();

        // Act & Assert
        webTestClient.post()
            .uri("/api/returns/full-return")
            .header("X-Tenant-ID", testTenantId)
            .header("Authorization", "Bearer " + authToken)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isCreated()
            .expectBody(ProcessFullReturnResponse.class)
            .value(response -> {
                assertThat(response.getReturnId()).isNotBlank();
                assertThat(response.getOrderId()).isEqualTo(request.getOrderId());
                assertThat(response.getReturnType()).isEqualTo("FULL");
                assertThat(response.getStatus()).isEqualTo("INITIATED");
                assertThat(response.getPrimaryReturnReason()).isEqualTo("CUSTOMER_REJECTION");
                assertThat(response.getTotalLines()).isEqualTo(2);
                assertThat(response.getTotalReturnedQuantity()).isEqualByComparingTo(BigDecimal.valueOf(80));
                assertThat(response.getGoodConditionLines()).isEqualTo(2);
                assertThat(response.getDamagedLines()).isEqualTo(0);
                assertThat(response.getWriteOffLines()).isEqualTo(0);
            });
    }

    @Test
    @DisplayName("POST /api/returns/full-return - Should require product condition assessment")
    void processFullReturn_WithoutProductCondition_ShouldReturn400() {
        // Arrange
        List<FullReturnLineItemRequest> lineItems = createFullReturnLineItemsWithoutCondition();
        ProcessFullReturnRequest request = ReturnsTestDataBuilder
            .defaultFullReturn()
            .withFullLineItems(lineItems)
            .buildFullReturnRequest();

        // Act & Assert
        webTestClient.post()
            .uri("/api/returns/full-return")
            .header("X-Tenant-ID", testTenantId)
            .header("Authorization", "Bearer " + authToken)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody()
            .jsonPath("$.errors[?(@.field == 'lineItems[0].productCondition')]").exists();
    }

    @Test
    @DisplayName("POST /api/returns/full-return - Should track condition breakdown")
    void processFullReturn_WithMixedConditions_ShouldTrackConditionBreakdown() {
        // Arrange
        ProcessFullReturnRequest request = ReturnsTestDataBuilder
            .defaultFullReturn()
            .withFullLineItems(createMixedConditionLineItems())
            .buildFullReturnRequest();

        // Act & Assert
        webTestClient.post()
            .uri("/api/returns/full-return")
            .header("X-Tenant-ID", testTenantId)
            .header("Authorization", "Bearer " + authToken)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isCreated()
            .expectBody(ProcessFullReturnResponse.class)
            .value(response -> {
                assertThat(response.getGoodConditionLines()).isEqualTo(1);
                assertThat(response.getDamagedLines()).isEqualTo(1);
                assertThat(response.getWriteOffLines()).isEqualTo(1);
            });
    }

    // ========================================================================
    // Query Tests
    // ========================================================================

    @Test
    @DisplayName("GET /api/returns/{returnId} - Should retrieve return by ID")
    void getReturn_WithValidId_ShouldReturn200() {
        // Arrange
        String returnId = createTestPartialReturn();

        // Act & Assert
        webTestClient.get()
            .uri("/api/returns/{returnId}", returnId)
            .header("X-Tenant-ID", testTenantId)
            .header("Authorization", "Bearer " + authToken)
            .exchange()
            .expectStatus().isOk()
            .expectBody(ReturnQueryDTO.class)
            .value(returnDTO -> {
                assertThat(returnDTO.getReturnId()).isEqualTo(returnId);
                assertThat(returnDTO.getReturnType()).isNotNull();
                assertThat(returnDTO.getStatus()).isNotNull();
                assertThat(returnDTO.getLineItems()).isNotEmpty();
            });
    }

    @Test
    @DisplayName("GET /api/returns - Should list all returns with pagination")
    void listReturns_WithPagination_ShouldReturn200() {
        // Arrange
        createTestPartialReturn();
        createTestFullReturn();

        // Act & Assert
        webTestClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/api/returns")
                .queryParam("page", 0)
                .queryParam("size", 10)
                .build())
            .header("X-Tenant-ID", testTenantId)
            .header("Authorization", "Bearer " + authToken)
            .exchange()
            .expectStatus().isOk()
            .expectBody(ListReturnsResponse.class)
            .value(response -> {
                assertThat(response.getReturns()).hasSizeGreaterThanOrEqualTo(2);
                assertThat(response.getTotalElements()).isGreaterThanOrEqualTo(2);
                assertThat(response.getPageNumber()).isEqualTo(0);
                assertThat(response.getPageSize()).isEqualTo(10);
            });
    }

    @Test
    @DisplayName("GET /api/returns - Should filter by return type")
    void listReturns_FilterByReturnType_ShouldReturnFiltered() {
        // Arrange
        createTestPartialReturn();
        createTestFullReturn();

        // Act & Assert
        webTestClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/api/returns")
                .queryParam("returnType", "PARTIAL")
                .build())
            .header("X-Tenant-ID", testTenantId)
            .header("Authorization", "Bearer " + authToken)
            .exchange()
            .expectStatus().isOk()
            .expectBody(ListReturnsResponse.class)
            .value(response -> {
                assertThat(response.getReturns())
                    .allMatch(returnDTO -> returnDTO.getReturnType().equals("PARTIAL"));
            });
    }

    // ========================================================================
    // Helper Methods
    // ========================================================================

    private String createTestPartialReturn() {
        ProcessPartialReturnRequest request = ReturnsTestDataBuilder
            .defaultPartialReturn()
            .buildPartialReturnRequest();

        ProcessPartialReturnResponse response = webTestClient.post()
            .uri("/api/returns/partial-return")
            .header("X-Tenant-ID", testTenantId)
            .header("Authorization", "Bearer " + authToken)
            .bodyValue(request)
            .exchange()
            .expectStatus().isCreated()
            .expectBody(ProcessPartialReturnResponse.class)
            .returnResult()
            .getResponseBody();

        return response.getReturnId();
    }

    private String createTestFullReturn() {
        ProcessFullReturnRequest request = ReturnsTestDataBuilder
            .defaultFullReturn()
            .buildFullReturnRequest();

        ProcessFullReturnResponse response = webTestClient.post()
            .uri("/api/returns/full-return")
            .header("X-Tenant-ID", testTenantId)
            .header("Authorization", "Bearer " + authToken)
            .bodyValue(request)
            .exchange()
            .expectStatus().isCreated()
            .expectBody(ProcessFullReturnResponse.class)
            .returnResult()
            .getResponseBody();

        return response.getReturnId();
    }

    private List<PartialReturnLineItemRequest> createAllReturnedLineItems() {
        // Implementation: All items with acceptedQuantity = 0
        return List.of(
            PartialReturnLineItemRequest.builder()
                .productId(UUID.randomUUID().toString())
                .productCode("PROD-001")
                .orderedQuantity(BigDecimal.valueOf(50))
                .pickedQuantity(BigDecimal.valueOf(50))
                .acceptedQuantity(BigDecimal.ZERO)
                .returnedQuantity(BigDecimal.valueOf(50))
                .returnReason("CUSTOMER_REJECTION")
                .productCondition("GOOD")
                .build()
        );
    }

    private List<FullReturnLineItemRequest> createFullReturnLineItemsWithoutCondition() {
        return List.of(
            FullReturnLineItemRequest.builder()
                .productId(UUID.randomUUID().toString())
                .productCode("PROD-001")
                .orderedQuantity(BigDecimal.valueOf(50))
                .pickedQuantity(BigDecimal.valueOf(50))
                .productCondition(null) // Missing condition
                .build()
        );
    }

    private List<FullReturnLineItemRequest> createMixedConditionLineItems() {
        return List.of(
            FullReturnLineItemRequest.builder()
                .productId(UUID.randomUUID().toString())
                .productCode("PROD-001")
                .orderedQuantity(BigDecimal.valueOf(50))
                .pickedQuantity(BigDecimal.valueOf(50))
                .productCondition("GOOD")
                .build(),
            FullReturnLineItemRequest.builder()
                .productId(UUID.randomUUID().toString())
                .productCode("PROD-002")
                .orderedQuantity(BigDecimal.valueOf(30))
                .pickedQuantity(BigDecimal.valueOf(30))
                .productCondition("DAMAGED")
                .build(),
            FullReturnLineItemRequest.builder()
                .productId(UUID.randomUUID().toString())
                .productCode("PROD-003")
                .orderedQuantity(BigDecimal.valueOf(20))
                .pickedQuantity(BigDecimal.valueOf(20))
                .productCondition("WRITE_OFF")
                .build()
        );
    }
}
```

---

### 2. Damage Assessment Test

**Location:** `gateway-api-tests/src/test/java/com/ccbsa/wms/gateway/api/DamageAssessmentTest.java`

```java
package com.ccbsa.wms.gateway.api;

import com.ccbsa.wms.gateway.api.dto.*;
import com.ccbsa.wms.gateway.api.fixture.DamageAssessmentTestDataBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Damage Assessment API Tests")
public class DamageAssessmentTest extends BaseIntegrationTest {

    // ========================================================================
    // US-7.3.1: Handle Damage-in-Transit Returns Tests
    // ========================================================================

    @Test
    @DisplayName("POST /api/returns/damage-assessments - Should record damage assessment successfully")
    void recordDamageAssessment_WithValidRequest_ShouldReturn201() {
        // Arrange
        RecordDamageAssessmentRequest request = DamageAssessmentTestDataBuilder
            .defaultDamageAssessment()
            .buildRequest();

        // Act & Assert
        webTestClient.post()
            .uri("/api/returns/damage-assessments")
            .header("X-Tenant-ID", testTenantId)
            .header("Authorization", "Bearer " + authToken)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isCreated()
            .expectBody(RecordDamageAssessmentResponse.class)
            .value(response -> {
                assertThat(response.getDamageAssessmentId()).isNotBlank();
                assertThat(response.getOrderId()).isEqualTo(request.getOrderId());
                assertThat(response.getDamageType()).isEqualTo("CRUSHED");
                assertThat(response.getDamageSeverity()).isEqualTo("MODERATE");
                assertThat(response.getEstimatedTotalLoss()).isEqualByComparingTo(BigDecimal.valueOf(1200.00));
                assertThat(response.getTotalDamagedProducts()).isEqualTo(1);
                assertThat(response.getTotalDamagedUnits()).isEqualTo(10);
                assertThat(response.getTotalPhotoCount()).isGreaterThanOrEqualTo(2);
                assertThat(response.getStatus()).isEqualTo("SUBMITTED");
            });
    }

    @Test
    @DisplayName("POST /api/returns/damage-assessments - Should require photo evidence")
    void recordDamageAssessment_WithoutPhotos_ShouldReturn400() {
        // Arrange
        RecordDamageAssessmentRequest request = DamageAssessmentTestDataBuilder
            .defaultDamageAssessment()
            .withDamagedProducts(createDamagedProductsWithoutPhotos())
            .buildRequest();

        // Act & Assert
        webTestClient.post()
            .uri("/api/returns/damage-assessments")
            .header("X-Tenant-ID", testTenantId)
            .header("Authorization", "Bearer " + authToken)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody()
            .jsonPath("$.message").value(message ->
                assertThat(message.toString()).contains("photographic evidence")
            );
    }

    @Test
    @DisplayName("POST /api/returns/damage-assessments - Should require insurance claim for severe damage")
    void recordDamageAssessment_SevereDamageWithoutInsuranceClaim_ShouldReturn400() {
        // Arrange
        RecordDamageAssessmentRequest request = DamageAssessmentTestDataBuilder
            .severeDamageAssessment()
            .withoutInsuranceClaim()
            .buildRequest();

        // Act & Assert
        webTestClient.post()
            .uri("/api/returns/damage-assessments")
            .header("X-Tenant-ID", testTenantId)
            .header("Authorization", "Bearer " + authToken)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody()
            .jsonPath("$.message").value(message ->
                assertThat(message.toString()).contains("Insurance claim information is required")
            );
    }

    @Test
    @DisplayName("POST /api/returns/damage-assessments - Should calculate condition breakdown")
    void recordDamageAssessment_WithMixedConditions_ShouldCalculateBreakdown() {
        // Arrange
        RecordDamageAssessmentRequest request = DamageAssessmentTestDataBuilder
            .defaultDamageAssessment()
            .withDamagedProducts(createMixedConditionDamagedProducts())
            .buildRequest();

        // Act & Assert
        webTestClient.post()
            .uri("/api/returns/damage-assessments")
            .header("X-Tenant-ID", testTenantId)
            .header("Authorization", "Bearer " + authToken)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isCreated()
            .expectBody(RecordDamageAssessmentResponse.class)
            .value(response -> {
                assertThat(response.getConditionBreakdown()).isNotNull();
                assertThat(response.getConditionBreakdown().get("QUARANTINE")).isEqualTo(1);
                assertThat(response.getConditionBreakdown().get("DAMAGED")).isEqualTo(1);
                assertThat(response.getConditionBreakdown().get("WRITE_OFF")).isEqualTo(1);
            });
    }

    // ========================================================================
    // Helper Methods
    // ========================================================================

    private List<DamagedProductRequestDTO> createDamagedProductsWithoutPhotos() {
        return List.of(
            DamagedProductRequestDTO.builder()
                .productId(UUID.randomUUID().toString())
                .productCode("PROD-001")
                .orderedQuantity(BigDecimal.valueOf(50))
                .damagedQuantity(BigDecimal.TEN)
                .productCondition("DAMAGED")
                .estimatedUnitLoss(BigDecimal.valueOf(120.00))
                .photoUrls(List.of()) // Empty list - no photos
                .build()
        );
    }

    private List<DamagedProductRequestDTO> createMixedConditionDamagedProducts() {
        return List.of(
            DamagedProductRequestDTO.builder()
                .productId(UUID.randomUUID().toString())
                .productCode("PROD-001")
                .orderedQuantity(BigDecimal.valueOf(50))
                .damagedQuantity(BigDecimal.TEN)
                .productCondition("QUARANTINE")
                .estimatedUnitLoss(BigDecimal.valueOf(120.00))
                .photoUrls(List.of("https://photo1.jpg"))
                .build(),
            DamagedProductRequestDTO.builder()
                .productId(UUID.randomUUID().toString())
                .productCode("PROD-002")
                .orderedQuantity(BigDecimal.valueOf(30))
                .damagedQuantity(BigDecimal.valueOf(5))
                .productCondition("DAMAGED")
                .estimatedUnitLoss(BigDecimal.valueOf(80.00))
                .photoUrls(List.of("https://photo2.jpg"))
                .build(),
            DamagedProductRequestDTO.builder()
                .productId(UUID.randomUUID().toString())
                .productCode("PROD-003")
                .orderedQuantity(BigDecimal.valueOf(20))
                .damagedQuantity(BigDecimal.valueOf(20))
                .productCondition("WRITE_OFF")
                .estimatedUnitLoss(BigDecimal.valueOf(150.00))
                .photoUrls(List.of("https://photo3.jpg"))
                .build()
        );
    }
}
```

---

### 3. Return Reconciliation Test

**Location:** `gateway-api-tests/src/test/java/com/ccbsa/wms/gateway/api/ReturnReconciliationTest.java`

```java
package com.ccbsa.wms.gateway.api;

import com.ccbsa.wms.gateway.api.dto.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.awaitility.Awaitility;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Return Reconciliation API Tests")
public class ReturnReconciliationTest extends BaseIntegrationTest {

    // ========================================================================
    // US-7.5.1: Reconcile Returns with D365 Tests
    // ========================================================================

    @Test
    @DisplayName("D365 Reconciliation - Partial return should sync to D365 successfully")
    void d365Reconciliation_AfterPartialReturn_ShouldSyncSuccessfully() {
        // Arrange: Create a partial return
        String returnId = createTestPartialReturn();

        // Act: Wait for async D365 reconciliation (up to 15 seconds)
        Awaitility.await()
            .atMost(15, TimeUnit.SECONDS)
            .pollInterval(1, TimeUnit.SECONDS)
            .until(() -> isReturnReconciled(returnId));

        // Assert: Query reconciliation status
        ReconciliationRecord reconciliation = getReconciliationRecord(returnId);

        assertThat(reconciliation.getD365SyncStatus()).isEqualTo("SYNCED");
        assertThat(reconciliation.getD365ReturnOrderId()).isNotBlank();
        assertThat(reconciliation.getSyncAttempts()).isGreaterThanOrEqualTo(1);
        assertThat(reconciliation.getErrorMessage()).isNull();
    }

    @Test
    @DisplayName("D365 Reconciliation - Full return should sync to D365 successfully")
    void d365Reconciliation_AfterFullReturn_ShouldSyncSuccessfully() {
        // Arrange: Create a full return
        String returnId = createTestFullReturn();

        // Act: Wait for async D365 reconciliation
        Awaitility.await()
            .atMost(15, TimeUnit.SECONDS)
            .pollInterval(1, TimeUnit.SECONDS)
            .until(() -> isReturnReconciled(returnId));

        // Assert
        ReconciliationRecord reconciliation = getReconciliationRecord(returnId);

        assertThat(reconciliation.getD365SyncStatus()).isEqualTo("SYNCED");
        assertThat(reconciliation.getD365ReturnOrderId()).isNotBlank();
    }

    @Test
    @DisplayName("POST /api/returns/reconciliation/{returnId}/retry-sync - Should retry failed sync")
    void retryD365Sync_ForFailedReconciliation_ShouldSucceedOnRetry() {
        // Arrange: Create a return (simulating initial failure)
        String returnId = createTestPartialReturn();

        // Wait for initial sync attempt
        Awaitility.await()
            .atMost(10, TimeUnit.SECONDS)
            .until(() -> getReconciliationRecord(returnId) != null);

        // Act: Retry sync
        webTestClient.post()
            .uri("/api/returns/reconciliation/{returnId}/retry-sync", returnId)
            .header("X-Tenant-ID", testTenantId)
            .header("Authorization", "Bearer " + authToken)
            .exchange()
            .expectStatus().isOk()
            .expectBody(RetryD365SyncResponse.class)
            .value(response -> {
                assertThat(response.getReturnId()).isEqualTo(returnId);
                assertThat(response.getSyncStatus()).isIn("SYNCED", "RETRYING");
                assertThat(response.getMessage()).isNotBlank();
            });
    }

    @Test
    @DisplayName("GET /api/returns/reconciliation/records - Should list reconciliation records")
    void getReconciliationRecords_ShouldReturn200() {
        // Arrange
        createTestPartialReturn();
        createTestFullReturn();

        // Act & Assert
        webTestClient.get()
            .uri("/api/returns/reconciliation/records")
            .header("X-Tenant-ID", testTenantId)
            .header("Authorization", "Bearer " + authToken)
            .exchange()
            .expectStatus().isOk()
            .expectBody(ListReconciliationRecordsResponse.class)
            .value(response -> {
                assertThat(response.getRecords()).hasSizeGreaterThanOrEqualTo(2);
                assertThat(response.getRecords()).allMatch(record ->
                    record.getD365SyncStatus() != null
                );
            });
    }

    @Test
    @DisplayName("GET /api/returns/reconciliation/{returnId}/audit-trail - Should return audit trail")
    void getReconciliationAuditTrail_ShouldReturn200() {
        // Arrange
        String returnId = createTestPartialReturn();

        Awaitility.await()
            .atMost(10, TimeUnit.SECONDS)
            .until(() -> isReturnReconciled(returnId));

        // Act & Assert
        webTestClient.get()
            .uri("/api/returns/reconciliation/{returnId}/audit-trail", returnId)
            .header("X-Tenant-ID", testTenantId)
            .header("Authorization", "Bearer " + authToken)
            .exchange()
            .expectStatus().isOk()
            .expectBody(AuditTrailResponse.class)
            .value(response -> {
                assertThat(response.getAuditEntries()).isNotEmpty();
                assertThat(response.getAuditEntries().get(0).getAction()).isNotBlank();
                assertThat(response.getAuditEntries().get(0).getTimestamp()).isNotNull();
            });
    }

    @Test
    @DisplayName("GET /api/returns/reconciliation/summary - Should return reconciliation summary")
    void getReconciliationSummary_ShouldReturn200() {
        // Act & Assert
        webTestClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/api/returns/reconciliation/summary")
                .queryParam("startDate", "2026-01-01")
                .queryParam("endDate", "2026-01-31")
                .build())
            .header("X-Tenant-ID", testTenantId)
            .header("Authorization", "Bearer " + authToken)
            .exchange()
            .expectStatus().isOk()
            .expectBody(ReconciliationSummary.class)
            .value(summary -> {
                assertThat(summary.getTotalReturns()).isGreaterThanOrEqualTo(0);
                assertThat(summary.getSyncedReturns()).isGreaterThanOrEqualTo(0);
                assertThat(summary.getPendingReturns()).isGreaterThanOrEqualTo(0);
                assertThat(summary.getFailedReturns()).isGreaterThanOrEqualTo(0);
                assertThat(summary.getSyncSuccessRate()).isBetween(0.0, 100.0);
            });
    }

    // ========================================================================
    // Helper Methods
    // ========================================================================

    private boolean isReturnReconciled(String returnId) {
        try {
            ReconciliationRecord record = getReconciliationRecord(returnId);
            return record != null &&
                   (record.getD365SyncStatus().equals("SYNCED") ||
                    record.getD365SyncStatus().equals("FAILED"));
        } catch (Exception e) {
            return false;
        }
    }

    private ReconciliationRecord getReconciliationRecord(String returnId) {
        return webTestClient.get()
            .uri("/api/returns/reconciliation/records")
            .header("X-Tenant-ID", testTenantId)
            .header("Authorization", "Bearer " + authToken)
            .exchange()
            .expectBody(ListReconciliationRecordsResponse.class)
            .returnResult()
            .getResponseBody()
            .getRecords()
            .stream()
            .filter(record -> record.getReturnId().equals(returnId))
            .findFirst()
            .orElse(null);
    }
}
```

---

## Test Execution and CI Integration

### Maven Configuration

**Update:** `gateway-api-tests/pom.xml`

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-surefire-plugin</artifactId>
            <version>3.0.0-M7</version>
            <configuration>
                <includes>
                    <include>**/*Test.java</include>
                </includes>
                <parallel>methods</parallel>
                <threadCount>4</threadCount>
                <systemPropertyVariables>
                    <test.environment>integration</test.environment>
                </systemPropertyVariables>
            </configuration>
        </plugin>
    </plugins>
</build>
```

### Test Execution Commands

```bash
# Run all Returns Service tests
mvn test -Dtest=ReturnsServiceTest

# Run Damage Assessment tests
mvn test -Dtest=DamageAssessmentTest

# Run Reconciliation tests
mvn test -Dtest=ReturnReconciliationTest

# Run all Sprint 7 tests
mvn test -Dtest=ReturnsServiceTest,DamageAssessmentTest,ReturnReconciliationTest

# Generate test coverage report
mvn clean verify jacoco:report
```

---

## Implementation Checklist

### Test Data Builders
- [ ] Create `ReturnsTestDataBuilder`
- [ ] Create `DamageAssessmentTestDataBuilder`
- [ ] Create `ReconciliationTestDataBuilder`
- [ ] Create helper methods for common scenarios

### DTOs for Tests
- [ ] Create `ProcessPartialReturnRequest`
- [ ] Create `ProcessFullReturnRequest`
- [ ] Create `RecordDamageAssessmentRequest`
- [ ] Create all response DTOs
- [ ] Create query result DTOs

### Returns Service Tests
- [ ] Test partial return processing (US-7.1.1)
- [ ] Test full return processing (US-7.2.1)
- [ ] Test validation scenarios
- [ ] Test query endpoints
- [ ] Test filtering and pagination

### Damage Assessment Tests
- [ ] Test damage assessment creation (US-7.3.1)
- [ ] Test photo evidence validation
- [ ] Test insurance claim requirements
- [ ] Test condition breakdown calculation

### Reconciliation Tests
- [ ] Test D365 sync after partial return (US-7.5.1)
- [ ] Test D365 sync after full return
- [ ] Test retry mechanism
- [ ] Test audit trail queries
- [ ] Test reconciliation summary

### Test Helpers
- [ ] Create `ReturnsHelper` for common operations
- [ ] Create `ReconciliationHelper` for D365 operations
- [ ] Add async waiting utilities
- [ ] Add assertion helpers

### CI Integration
- [ ] Configure Maven Surefire plugin
- [ ] Set up parallel test execution
- [ ] Configure test coverage reporting
- [ ] Add test execution to CI pipeline

---

**End of Gateway API Tests Implementation Plan**
