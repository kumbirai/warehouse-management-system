package com.ccbsa.wms.gateway.api;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.ccbsa.common.application.api.ApiResponse;
import com.ccbsa.wms.gateway.api.dto.AuthenticationResult;
import com.ccbsa.wms.gateway.api.dto.CreateProductRequest;
import com.ccbsa.wms.gateway.api.dto.CreateProductResponse;
import com.ccbsa.wms.gateway.api.dto.RecordDamageAssessmentRequest;
import com.ccbsa.wms.gateway.api.dto.RecordDamageAssessmentResponse;
import com.ccbsa.wms.gateway.api.fixture.DamageAssessmentTestDataBuilder;
import com.ccbsa.wms.gateway.api.fixture.ProductTestDataBuilder;

import static org.assertj.core.api.Assertions.assertThat;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DamageAssessmentTest extends BaseIntegrationTest {

    private static AuthenticationResult tenantAdminAuth;
    private static String testTenantId;
    private static String testProductId;
    private static String testOrderNumber;

    @BeforeAll
    public static void setupTestData() {
        // Login as TENANT_ADMIN
        // Note: This will be set up in first test
    }

    @BeforeEach
    public void setUpDamageAssessmentTest() {
        if (tenantAdminAuth == null) {
            tenantAdminAuth = loginAsTenantAdmin();
            testTenantId = tenantAdminAuth.getTenantId();
            testOrderNumber = "ORD-" + faker.number().digits(8);

            // Try to reuse product from repository, otherwise create new
            CreateProductResponse product = createProduct(tenantAdminAuth.getAccessToken(), testTenantId);
            testProductId = product.getProductId();
        }
    }

    // ==================== DAMAGE ASSESSMENT CREATION TESTS ====================

    @Test
    @Order(1)
    public void testRecordDamageAssessment_Success() {
        // Arrange
        RecordDamageAssessmentRequest request = DamageAssessmentTestDataBuilder.buildRecordDamageAssessmentRequest(testOrderNumber, testProductId);

        // Act
        WebTestClient.ResponseSpec response = authenticatedPost("/api/v1/returns/damage-assessment", tenantAdminAuth.getAccessToken(), testTenantId, request).exchange();

        // Assert
        EntityExchangeResult<ApiResponse<RecordDamageAssessmentResponse>> exchangeResult =
                response.expectStatus().isCreated().expectBody(new ParameterizedTypeReference<ApiResponse<RecordDamageAssessmentResponse>>() {
                }).returnResult();

        ApiResponse<RecordDamageAssessmentResponse> apiResponse = exchangeResult.getResponseBody();
        assertThat(apiResponse).isNotNull();
        assertThat(apiResponse.isSuccess()).isTrue();

        RecordDamageAssessmentResponse result = apiResponse.getData();
        assertThat(result).isNotNull();
        assertThat(result.getDamageAssessmentId()).isNotBlank();
        assertThat(result.getOrderNumber()).isEqualTo(testOrderNumber);
        assertThat(result.getStatus()).isEqualTo("SUBMITTED");
    }

    @Test
    @Order(2)
    public void testRecordDamageAssessmentWithInsurance_Success() {
        // Arrange
        RecordDamageAssessmentRequest request = DamageAssessmentTestDataBuilder.buildRecordDamageAssessmentRequestWithInsurance(testOrderNumber + "-2", testProductId);

        // Act
        WebTestClient.ResponseSpec response = authenticatedPost("/api/v1/returns/damage-assessment", tenantAdminAuth.getAccessToken(), testTenantId, request).exchange();

        // Assert
        EntityExchangeResult<ApiResponse<RecordDamageAssessmentResponse>> exchangeResult =
                response.expectStatus().isCreated().expectBody(new ParameterizedTypeReference<ApiResponse<RecordDamageAssessmentResponse>>() {
                }).returnResult();

        ApiResponse<RecordDamageAssessmentResponse> apiResponse = exchangeResult.getResponseBody();
        assertThat(apiResponse).isNotNull();
        assertThat(apiResponse.isSuccess()).isTrue();

        RecordDamageAssessmentResponse result = apiResponse.getData();
        assertThat(result).isNotNull();
        assertThat(result.getDamageAssessmentId()).isNotBlank();
        assertThat(result.getDamageSeverity()).isEqualTo("SEVERE");
    }

    @Test
    @Order(3)
    public void testRecordDamageAssessment_ValidationError() {
        // Arrange - Missing required fields
        RecordDamageAssessmentRequest request = RecordDamageAssessmentRequest.builder().orderNumber("").damageType("PHYSICAL").damageSeverity("SEVERE").damageSource("TRANSPORT")
                .damagedProducts(java.util.Collections.emptyList()).build();

        // Act
        WebTestClient.ResponseSpec response = authenticatedPost("/api/v1/returns/damage-assessment", tenantAdminAuth.getAccessToken(), testTenantId, request).exchange();

        // Assert
        response.expectStatus().isBadRequest();
    }
}
