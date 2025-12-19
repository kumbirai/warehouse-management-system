package com.ccbsa.wms.gateway.api;

import com.ccbsa.common.application.api.ApiResponse;
import com.ccbsa.wms.gateway.api.dto.AuthenticationResult;
import com.ccbsa.wms.gateway.api.dto.CreateTenantRequest;
import com.ccbsa.wms.gateway.api.dto.CreateTenantResponse;
import com.ccbsa.wms.gateway.api.dto.TenantResponse;
import com.ccbsa.wms.gateway.api.fixture.TenantTestDataBuilder;
import com.ccbsa.wms.gateway.api.fixture.TestData;
import org.junit.jupiter.api.*;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TenantManagementTest extends BaseIntegrationTest {

    private AuthenticationResult systemAdminAuth;

    @BeforeEach
    public void setUpTenantTest() {
        // Login as SYSTEM_ADMIN before each test
        systemAdminAuth = loginAsSystemAdmin();
    }

    // ==================== TENANT CREATION TESTS ====================

    @Test
    @Order(1)
    public void testCreateTenant_Success() {
        // Arrange
        CreateTenantRequest request = TenantTestDataBuilder.buildCreateTenantRequest();

        // Act
        WebTestClient.ResponseSpec response = authenticatedPost(
                "/api/v1/tenants",
                systemAdminAuth.getAccessToken(),
                request
        ).exchange();

        // Assert
        EntityExchangeResult<ApiResponse<CreateTenantResponse>> exchangeResult = response
                .expectStatus().isCreated()
                .expectBody(new ParameterizedTypeReference<ApiResponse<CreateTenantResponse>>() {})
                .returnResult();
        
        ApiResponse<CreateTenantResponse> apiResponse = exchangeResult.getResponseBody();
        assertThat(apiResponse).isNotNull();
        assertThat(apiResponse.isSuccess()).isTrue();
        
        CreateTenantResponse tenant = apiResponse.getData();
        assertThat(tenant).isNotNull();
        assertThat(tenant.getTenantId()).isNotBlank();
        assertThat(tenant.isSuccess()).isTrue();
        assertThat(tenant.getMessage()).isNotBlank();
    }

    @Test
    @Order(2)
    public void testCreateTenant_DuplicateTenantId() {
        // Arrange - Create tenant with specific tenantId
        String duplicateTenantId = TestData.tenantId();
        CreateTenantRequest firstRequest = CreateTenantRequest.builder()
                .tenantId(duplicateTenantId)
                .name(TestData.tenantName())
                .emailAddress(TestData.tenantEmail())
                .build();

        // Create first tenant
        authenticatedPost("/api/v1/tenants", systemAdminAuth.getAccessToken(), firstRequest)
                .exchange()
                .expectStatus().isCreated();

        // Act - Try to create duplicate with same tenantId (different name)
        CreateTenantRequest duplicateRequest = CreateTenantRequest.builder()
                .tenantId(duplicateTenantId) // Same tenantId
                .name("Different Name") // Different name
                .emailAddress(TestData.tenantEmail())
                .build();
        
        EntityExchangeResult<ApiResponse<CreateTenantResponse>> exchangeResult = authenticatedPost(
                "/api/v1/tenants",
                systemAdminAuth.getAccessToken(),
                duplicateRequest
        ).exchange()
                .expectStatus().isCreated() // Service returns 201 even for duplicates
                .expectBody(new ParameterizedTypeReference<ApiResponse<CreateTenantResponse>>() {})
                .returnResult();

        // Assert - Check that the response indicates failure
        ApiResponse<CreateTenantResponse> apiResponse = exchangeResult.getResponseBody();
        assertThat(apiResponse).isNotNull();
        // The service returns success=true wrapper, but data.success=false for duplicates
        CreateTenantResponse responseData = apiResponse.getData();
        assertThat(responseData).isNotNull();
        assertThat(responseData.isSuccess()).isFalse();
        assertThat(responseData.getMessage()).contains("already exists");
    }

    @Test
    @Order(3)
    public void testCreateTenant_InvalidData() {
        // Arrange
        CreateTenantRequest request = CreateTenantRequest.builder()
                .tenantId(TestData.tenantId())
                .name("") // Empty name
                .emailAddress("invalid-email") // Invalid email
                .build();

        // Act
        WebTestClient.ResponseSpec response = authenticatedPost(
                "/api/v1/tenants",
                systemAdminAuth.getAccessToken(),
                request
        ).exchange();

        // Assert
        response.expectStatus().isBadRequest();
    }

    @Test
    @Order(4)
    public void testCreateTenant_Unauthorized() {
        // Arrange
        CreateTenantRequest request = TenantTestDataBuilder.buildCreateTenantRequest();

        // Act - No authentication
        WebTestClient.ResponseSpec response = webTestClient.post()
                .uri("/api/v1/tenants")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange();

        // Assert
        response.expectStatus().isUnauthorized();
    }

    // ==================== TENANT QUERY TESTS ====================

    @Test
    @Order(10)
    public void testListTenants_Success() {
        // Act
        WebTestClient.ResponseSpec response = authenticatedGet(
                "/api/v1/tenants?page=0&size=10",
                systemAdminAuth.getAccessToken()
        ).exchange();

        // Assert
        response.expectStatus().isOk();
    }

    @Test
    @Order(11)
    public void testGetTenantById_Success() {
        // Arrange - Create tenant first
        CreateTenantRequest request = TenantTestDataBuilder.buildCreateTenantRequest();
        EntityExchangeResult<ApiResponse<CreateTenantResponse>> createExchangeResult = authenticatedPost(
                "/api/v1/tenants",
                systemAdminAuth.getAccessToken(),
                request
        ).exchange()
                .expectStatus().isCreated()
                .expectBody(new ParameterizedTypeReference<ApiResponse<CreateTenantResponse>>() {})
                .returnResult();
        
        ApiResponse<CreateTenantResponse> createApiResponse = createExchangeResult.getResponseBody();
        assertThat(createApiResponse).isNotNull();
        assertThat(createApiResponse.isSuccess()).isTrue();
        
        CreateTenantResponse createdTenant = createApiResponse.getData();
        assertThat(createdTenant).isNotNull();

        // Act
        WebTestClient.ResponseSpec response = authenticatedGet(
                "/api/v1/tenants/" + createdTenant.getTenantId(),
                systemAdminAuth.getAccessToken()
        ).exchange();

        // Assert
        EntityExchangeResult<ApiResponse<TenantResponse>> getExchangeResult = response
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<ApiResponse<TenantResponse>>() {})
                .returnResult();
        
        ApiResponse<TenantResponse> getApiResponse = getExchangeResult.getResponseBody();
        assertThat(getApiResponse).isNotNull();
        assertThat(getApiResponse.isSuccess()).isTrue();
        
        TenantResponse tenant = getApiResponse.getData();
        assertThat(tenant).isNotNull();
        assertThat(tenant.getTenantId()).isEqualTo(createdTenant.getTenantId());
    }

    @Test
    @Order(12)
    public void testGetTenantById_NotFound() {
        // Act
        WebTestClient.ResponseSpec response = authenticatedGet(
                "/api/v1/tenants/" + randomUUID(),
                systemAdminAuth.getAccessToken()
        ).exchange();

        // Assert
        response.expectStatus().isNotFound();
    }

    // ==================== TENANT LIFECYCLE TESTS ====================

    @Test
    @Order(20)
    public void testActivateTenant_Success() {
        // Arrange - Create tenant (status will be PENDING)
        CreateTenantRequest request = TenantTestDataBuilder.buildCreateTenantRequest();
        EntityExchangeResult<ApiResponse<CreateTenantResponse>> exchangeResult = authenticatedPost(
                "/api/v1/tenants",
                systemAdminAuth.getAccessToken(),
                request
        ).exchange()
                .expectStatus().isCreated()
                .expectBody(new ParameterizedTypeReference<ApiResponse<CreateTenantResponse>>() {})
                .returnResult();
        
        ApiResponse<CreateTenantResponse> apiResponse = exchangeResult.getResponseBody();
        assertThat(apiResponse).isNotNull();
        assertThat(apiResponse.isSuccess()).isTrue();
        
        CreateTenantResponse tenant = apiResponse.getData();
        assertThat(tenant).isNotNull();

        // Act - Activate tenant (PENDING -> ACTIVE)
        WebTestClient.ResponseSpec response = authenticatedPut(
                "/api/v1/tenants/" + tenant.getTenantId() + "/activate",
                systemAdminAuth.getAccessToken(),
                null
        ).exchange();

        // Assert - Activation returns 204 NO_CONTENT (as per ApiResponseBuilder.noContent())
        response.expectStatus().isNoContent();
    }

    @Test
    @Order(21)
    public void testDeactivateTenant_Success() {
        // Arrange - Create and activate tenant first (can only deactivate ACTIVE tenants)
        CreateTenantRequest request = TenantTestDataBuilder.buildCreateTenantRequest();
        EntityExchangeResult<ApiResponse<CreateTenantResponse>> exchangeResult = authenticatedPost(
                "/api/v1/tenants",
                systemAdminAuth.getAccessToken(),
                request
        ).exchange()
                .expectStatus().isCreated()
                .expectBody(new ParameterizedTypeReference<ApiResponse<CreateTenantResponse>>() {})
                .returnResult();
        
        ApiResponse<CreateTenantResponse> apiResponse = exchangeResult.getResponseBody();
        assertThat(apiResponse).isNotNull();
        assertThat(apiResponse.isSuccess()).isTrue();
        
        CreateTenantResponse tenant = apiResponse.getData();
        assertThat(tenant).isNotNull();

        // Activate tenant first (PENDING -> ACTIVE)
        authenticatedPut("/api/v1/tenants/" + tenant.getTenantId() + "/activate", systemAdminAuth.getAccessToken(), null)
                .exchange()
                .expectStatus().isNoContent();

        // Act - Deactivate tenant (ACTIVE -> INACTIVE)
        WebTestClient.ResponseSpec response = authenticatedPut(
                "/api/v1/tenants/" + tenant.getTenantId() + "/deactivate",
                systemAdminAuth.getAccessToken(),
                null
        ).exchange();

        // Assert - Deactivation returns 204 NO_CONTENT
        response.expectStatus().isNoContent();
    }

    @Test
    @Order(22)
    public void testSuspendTenant_Success() {
        // Arrange - Create and activate tenant first (can only suspend ACTIVE tenants)
        CreateTenantRequest request = TenantTestDataBuilder.buildCreateTenantRequest();
        EntityExchangeResult<ApiResponse<CreateTenantResponse>> exchangeResult = authenticatedPost(
                "/api/v1/tenants",
                systemAdminAuth.getAccessToken(),
                request
        ).exchange()
                .expectStatus().isCreated()
                .expectBody(new ParameterizedTypeReference<ApiResponse<CreateTenantResponse>>() {})
                .returnResult();
        
        ApiResponse<CreateTenantResponse> apiResponse = exchangeResult.getResponseBody();
        assertThat(apiResponse).isNotNull();
        assertThat(apiResponse.isSuccess()).isTrue();
        
        CreateTenantResponse tenant = apiResponse.getData();
        assertThat(tenant).isNotNull();

        // Activate tenant first (PENDING -> ACTIVE)
        authenticatedPut("/api/v1/tenants/" + tenant.getTenantId() + "/activate", systemAdminAuth.getAccessToken(), null)
                .exchange()
                .expectStatus().isNoContent();

        // Act - Suspend tenant (ACTIVE -> SUSPENDED)
        WebTestClient.ResponseSpec response = authenticatedPut(
                "/api/v1/tenants/" + tenant.getTenantId() + "/suspend",
                systemAdminAuth.getAccessToken(),
                null
        ).exchange();

        // Assert - Suspension returns 204 NO_CONTENT
        response.expectStatus().isNoContent();
    }
}

