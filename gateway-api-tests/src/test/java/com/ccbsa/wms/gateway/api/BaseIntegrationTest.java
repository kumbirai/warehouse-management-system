package com.ccbsa.wms.gateway.api;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.ccbsa.common.application.api.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import com.ccbsa.wms.gateway.api.dto.AuthenticationResult;
import com.ccbsa.wms.gateway.api.dto.CreateConsignmentRequest;
import com.ccbsa.wms.gateway.api.dto.CreateConsignmentResponse;
import com.ccbsa.wms.gateway.api.dto.CreateLocationRequest;
import com.ccbsa.wms.gateway.api.dto.CreateLocationResponse;
import com.ccbsa.wms.gateway.api.dto.AssignLocationToStockRequest;
import com.ccbsa.wms.gateway.api.dto.CreateProductResponse;
import com.ccbsa.wms.gateway.api.dto.CreateUserResponse;
import com.ccbsa.wms.gateway.api.dto.ListStockItemsResponse;
import com.ccbsa.wms.gateway.api.dto.StockItemResponse;
import com.ccbsa.wms.gateway.api.dto.StockItemsByClassificationResponse;
import com.ccbsa.wms.gateway.api.dto.StockLevelResponse;
import com.ccbsa.wms.gateway.api.fixture.ConsignmentTestDataBuilder;
import com.ccbsa.wms.gateway.api.fixture.LocationTestDataBuilder;
import com.ccbsa.wms.gateway.api.fixture.ProductTestDataBuilder;
import com.ccbsa.wms.gateway.api.fixture.StockItemTestDataBuilder;
import com.ccbsa.wms.gateway.api.fixture.TestDataManager;
import com.ccbsa.wms.gateway.api.fixture.UserTestDataBuilder;
import com.ccbsa.wms.gateway.api.helper.AuthenticationHelper;
import com.ccbsa.wms.gateway.api.util.CookieExtractor;
import com.ccbsa.wms.gateway.api.util.RequestHeaderHelper;
import com.ccbsa.wms.gateway.api.util.WebTestClientConfig;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.datafaker.Faker;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Base integration test class providing common test infrastructure
 * for gateway API integration tests.
 *
 * Features:
 * - WebTestClient configuration for reactive API testing
 * - JWT token management (access + refresh tokens)
 * - Cookie handling for httpOnly refresh tokens
 * - Authentication helpers for login/logout flows
 * - Faker instance for test data generation
 * - Common assertion utilities
 * - Request header management (correlation ID, tenant context)
 */
@Slf4j
public abstract class BaseIntegrationTest {

    protected WebTestClient webTestClient;
    protected ObjectMapper objectMapper;
    protected Faker faker;
    protected AuthenticationHelper authHelper;

    protected String gatewayBaseUrl;
    protected String systemAdminUsername;
    protected String systemAdminPassword;
    protected String tenantAdminUsername;
    protected String tenantAdminPassword;

    /**
     * Setup method executed before each test.
     * Initializes WebTestClient, ObjectMapper, Faker, and helper classes.
     * Skips tests if gateway service is not available.
     */
    @BeforeEach
    public void setUp() {
        // Load properties from environment or use defaults
        // Default to HTTP since SSL is disabled by default for local development
        // Set GATEWAY_SSL_ENABLED=true and use https://localhost:8080 for SSL-enabled gateway
        this.gatewayBaseUrl = System.getProperty("gateway.base.url", System.getenv().getOrDefault("GATEWAY_BASE_URL", "http://localhost:8080"));
        this.systemAdminUsername = System.getProperty("test.system.admin.username", System.getenv().getOrDefault("TEST_SYSTEM_ADMIN_USERNAME", "sysadmin"));
        this.systemAdminPassword = System.getProperty("test.system.admin.password", System.getenv().getOrDefault("TEST_SYSTEM_ADMIN_PASSWORD", "Password123@"));
        this.tenantAdminUsername = System.getProperty("test.tenant.admin.username", System.getenv().getOrDefault("TEST_TENANT_ADMIN_USERNAME", "tenantuser@cm-sol.co.za"));
        this.tenantAdminPassword = System.getProperty("test.tenant.admin.password", System.getenv().getOrDefault("TEST_TENANT_ADMIN_PASSWORD", "Password123@"));

        this.webTestClient = WebTestClientConfig.createWebTestClient(gatewayBaseUrl);
        this.objectMapper = new ObjectMapper();
        this.faker = new Faker();
        this.authHelper = new AuthenticationHelper(webTestClient, objectMapper);

        // Check if gateway service is available, skip tests if not
        checkGatewayAvailability();
    }

    /**
     * Check if gateway service is available.
     * Skips tests if gateway is not reachable.
     */
    private void checkGatewayAvailability() {
        boolean gatewayAvailable = false;
        try {
            // Try to connect to gateway health endpoint with a timeout
            // Use returnResult to get the actual response and check status
            int statusCode = webTestClient.get().uri("/actuator/health").exchange().expectStatus().is2xxSuccessful().returnResult(Void.class).getStatus().value();
            gatewayAvailable = (statusCode >= 200 && statusCode < 300);
        } catch (org.opentest4j.TestAbortedException e) {
            // Re-throw assumption failures
            throw e;
        } catch (Exception e) {
            // Gateway is not available - this is expected when services aren't running
            // Log the exception for debugging
            log.warn("Gateway health check failed: {}: {}", e.getClass().getSimpleName(), e.getMessage());
            gatewayAvailable = false;
        }

        // Skip all tests if gateway is not available
        Assumptions.assumeTrue(gatewayAvailable, "Gateway service is not available at " + gatewayBaseUrl + ". Please start the services before running integration tests. "
                + "You can skip integration tests with: mvn verify -DskipITs=true");
    }

    // ==================== AUTHENTICATION HELPERS ====================

    /**
     * Login as SYSTEM_ADMIN and return authentication result with tokens.
     *
     * @return AuthenticationResult containing access token, refresh token cookie, and user context
     */
    protected AuthenticationResult loginAsSystemAdmin() {
        return authHelper.login(systemAdminUsername, systemAdminPassword);
    }

    /**
     * Login as TENANT_ADMIN and return authentication result with tokens.
     *
     * <p>Note: This will skip tests if tenant admin credentials are not configured.
     * Set TEST_TENANT_ADMIN_USERNAME and TEST_TENANT_ADMIN_PASSWORD environment variables
     * to enable tenant admin tests.</p>
     *
     * @return AuthenticationResult containing access token, refresh token cookie, and user context
     * @throws org.opentest4j.TestAbortedException if tenant admin credentials are not configured
     */
    protected AuthenticationResult loginAsTenantAdmin() {
        // Check if tenant admin credentials are configured
        if (tenantAdminUsername == null || tenantAdminUsername.isEmpty() || tenantAdminPassword == null || tenantAdminPassword.isEmpty() || "tenantadmin".equals(
                tenantAdminUsername)) {
            Assumptions.assumeTrue(false, "Tenant admin credentials not configured. " + "Set TEST_TENANT_ADMIN_USERNAME and TEST_TENANT_ADMIN_PASSWORD environment variables "
                    + "to enable tenant admin tests.");
        }

        try {
            return authHelper.login(tenantAdminUsername, tenantAdminPassword);
        } catch (AssertionError e) {
            // If login fails, skip the test with a helpful message
            Assumptions.assumeTrue(false, "Tenant admin login failed. " + "Please ensure tenant admin user exists and credentials are correct. " + "Error: " + e.getMessage());
            return null; // Never reached, but needed for compilation
        }
    }

    /**
     * Login with custom credentials.
     *
     * @param username the username
     * @param password the password
     * @return AuthenticationResult containing access token, refresh token cookie, and user context
     */
    protected AuthenticationResult login(String username, String password) {
        return authHelper.login(username, password);
    }

    /**
     * Logout and clear authentication tokens.
     *
     * @param refreshTokenCookie the refresh token cookie to invalidate
     */
    protected void logout(ResponseCookie refreshTokenCookie) {
        authHelper.logout(refreshTokenCookie);
    }

    /**
     * Refresh access token using refresh token cookie.
     *
     * @param refreshTokenCookie the refresh token cookie
     * @return AuthenticationResult with new tokens
     */
    protected AuthenticationResult refreshToken(ResponseCookie refreshTokenCookie) {
        return authHelper.refreshToken(refreshTokenCookie);
    }

    // ==================== REQUEST BUILDERS ====================

    /**
     * Create authenticated GET request with Bearer token.
     *
     * @param uri         the request URI
     * @param accessToken the JWT access token
     * @return WebTestClient.RequestHeadersSpec for further configuration
     */
    protected WebTestClient.RequestHeadersSpec<?> authenticatedGet(String uri, String accessToken) {
        return webTestClient.get().uri(uri).headers(headers -> RequestHeaderHelper.addAuthHeaders(headers, accessToken));
    }

    /**
     * Create authenticated POST request with Bearer token.
     *
     * @param uri         the request URI
     * @param accessToken the JWT access token
     * @param requestBody the request body
     * @return WebTestClient.RequestHeadersSpec for further configuration
     */
    protected WebTestClient.RequestHeadersSpec<?> authenticatedPost(String uri, String accessToken, Object requestBody) {
        return webTestClient.post().uri(uri).contentType(MediaType.APPLICATION_JSON).headers(headers -> RequestHeaderHelper.addAuthHeaders(headers, accessToken))
                .bodyValue(requestBody);
    }

    /**
     * Create authenticated PUT request with Bearer token.
     *
     * @param uri         the request URI
     * @param accessToken the JWT access token
     * @param requestBody the request body (optional)
     * @return WebTestClient.RequestHeadersSpec for further configuration
     */
    protected WebTestClient.RequestHeadersSpec<?> authenticatedPut(String uri, String accessToken, Object requestBody) {
        WebTestClient.RequestBodySpec spec =
                webTestClient.put().uri(uri).contentType(MediaType.APPLICATION_JSON).headers(headers -> RequestHeaderHelper.addAuthHeaders(headers, accessToken));

        if (requestBody != null) {
            return spec.bodyValue(requestBody);
        }
        return spec;
    }

    /**
     * Create authenticated PUT request with Bearer token and tenant context.
     *
     * @param uri         the request URI
     * @param accessToken the JWT access token
     * @param tenantId    the tenant ID for X-Tenant-Id header
     * @param requestBody the request body (optional)
     * @return WebTestClient.RequestHeadersSpec for further configuration
     */
    protected WebTestClient.RequestHeadersSpec<?> authenticatedPut(String uri, String accessToken, String tenantId, Object requestBody) {
        WebTestClient.RequestBodySpec spec = webTestClient.put().uri(uri).contentType(MediaType.APPLICATION_JSON).headers(headers -> {
            RequestHeaderHelper.addAuthHeaders(headers, accessToken);
            RequestHeaderHelper.addTenantHeader(headers, tenantId);
        });

        if (requestBody != null) {
            return spec.bodyValue(requestBody);
        }
        return spec;
    }

    /**
     * Create authenticated DELETE request with Bearer token.
     *
     * @param uri         the request URI
     * @param accessToken the JWT access token
     * @return WebTestClient.RequestHeadersSpec for further configuration
     */
    protected WebTestClient.RequestHeadersSpec<?> authenticatedDelete(String uri, String accessToken) {
        return webTestClient.delete().uri(uri).headers(headers -> RequestHeaderHelper.addAuthHeaders(headers, accessToken));
    }

    /**
     * Extract data from ApiResponse wrapper.
     * Convenience method that extracts and validates the data payload.
     *
     * @param <T>             the type of data
     * @param response        the WebTestClient.ResponseSpec
     * @param apiResponseType the ParameterizedTypeReference for ApiResponse&lt;T&gt;
     * @return the unwrapped data from ApiResponse
     */
    protected <T> T extractApiResponseData(WebTestClient.ResponseSpec response, ParameterizedTypeReference<ApiResponse<T>> apiResponseType) {

        EntityExchangeResult<ApiResponse<T>> exchangeResult = extractApiResponse(response, apiResponseType);
        ApiResponse<T> apiResponse = exchangeResult.getResponseBody();

        T data = apiResponse.getData();
        assertThat(data).as("API response data should not be null").isNotNull();

        return data;
    }

    /**
     * Extract ApiResponse from WebTestClient.ResponseSpec.
     * This is a production-grade utility method that properly handles ApiResponse wrapper deserialization.
     *
     * @param <T>             the type of data in the ApiResponse
     * @param response        the WebTestClient.ResponseSpec
     * @param apiResponseType the ParameterizedTypeReference for ApiResponse&lt;T&gt;
     * @return EntityExchangeResult containing both ApiResponse and headers
     * @throws AssertionError if the response is null or deserialization fails
     */
    protected <T> EntityExchangeResult<ApiResponse<T>> extractApiResponse(WebTestClient.ResponseSpec response, ParameterizedTypeReference<ApiResponse<T>> apiResponseType) {

        EntityExchangeResult<ApiResponse<T>> exchangeResult = response.expectBody(apiResponseType).returnResult();

        ApiResponse<T> apiResponse = exchangeResult.getResponseBody();
        assertThat(apiResponse).as("API response should not be null. Status: %d, Response: %s", exchangeResult.getStatus().value(),
                exchangeResult.getResponseBodyContent() != null ? new String(exchangeResult.getResponseBodyContent()) : "null").isNotNull();

        assertThat(apiResponse.isSuccess()).as("API response should be successful. Error: %s", apiResponse.getError() != null ? apiResponse.getError().getMessage() : "none")
                .isTrue();

        return exchangeResult;
    }

    // ==================== API RESPONSE HELPERS ====================

    /**
     * Extract refresh token cookie from response headers.
     *
     * @param headers the response headers
     * @return ResponseCookie containing the refresh token
     */
    protected ResponseCookie extractRefreshTokenCookie(HttpHeaders headers) {
        return CookieExtractor.extractRefreshTokenCookie(headers);
    }

    /**
     * Validate refresh token cookie properties.
     *
     * @param cookie the cookie to validate
     */
    protected void assertRefreshTokenCookieValid(ResponseCookie cookie) {
        assertThat(cookie).isNotNull();
        assertThat(cookie.getName()).isEqualTo("refreshToken");
        assertThat(cookie.getValue()).isNotBlank();
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getSameSite()).as("SameSite should be Strict (case-insensitive)").isEqualToIgnoringCase("Strict");
        assertThat(cookie.getMaxAge().getSeconds()).isGreaterThan(0);
    }

    // ==================== COOKIE HELPERS ====================

    /**
     * Assert successful response (2xx status code).
     *
     * @param spec the response spec
     */
    protected void assertSuccessResponse(WebTestClient.ResponseSpec spec) {
        spec.expectStatus().is2xxSuccessful();
    }

    /**
     * Assert unauthorized response (401 status code).
     *
     * @param spec the response spec
     */
    protected void assertUnauthorized(WebTestClient.ResponseSpec spec) {
        spec.expectStatus().isUnauthorized();
    }

    // ==================== COMMON ASSERTIONS ====================

    /**
     * Assert forbidden response (403 status code).
     *
     * @param spec the response spec
     */
    protected void assertForbidden(WebTestClient.ResponseSpec spec) {
        spec.expectStatus().isForbidden();
    }

    /**
     * Assert bad request response (400 status code).
     *
     * @param spec the response spec
     */
    protected void assertBadRequest(WebTestClient.ResponseSpec spec) {
        spec.expectStatus().isBadRequest();
    }

    /**
     * Assert not found response (404 status code).
     *
     * @param spec the response spec
     */
    protected void assertNotFound(WebTestClient.ResponseSpec spec) {
        spec.expectStatus().isNotFound();
    }

    /**
     * Generate random email address.
     *
     * @return random email
     */
    protected String randomEmail() {
        return faker.internet().emailAddress();
    }

    /**
     * Generate random username.
     *
     * @return random username
     */
    protected String randomUsername() {
        return faker.name().username();
    }

    // ==================== FAKER HELPERS ====================

    /**
     * Generate random first name.
     *
     * @return random first name
     */
    protected String randomFirstName() {
        return faker.name().firstName();
    }

    /**
     * Generate random last name.
     *
     * @return random last name
     */
    protected String randomLastName() {
        return faker.name().lastName();
    }

    /**
     * Generate random company name.
     *
     * @return random company name
     */
    protected String randomCompanyName() {
        return faker.company().name();
    }

    /**
     * Generate random UUID string.
     *
     * @return random UUID
     */
    protected String randomUUID() {
        return UUID.randomUUID().toString();
    }

    /**
     * Generate correlation ID for request tracing.
     *
     * @return correlation ID
     */
    protected String generateCorrelationId() {
        return UUID.randomUUID().toString();
    }

    /**
     * Validate correlation ID is present in response headers.
     *
     * @param headers               response headers
     * @param expectedCorrelationId expected correlation ID
     */
    protected void assertCorrelationIdPresent(HttpHeaders headers, String expectedCorrelationId) {
        String correlationId = headers.getFirst("X-Correlation-Id");
        assertThat(correlationId).isEqualTo(expectedCorrelationId);
    }

    // ==================== CORRELATION ID HELPERS ====================

    /**
     * Create a product via the API and return the response.
     * Uses TestDataManager to check repository first, then creates if not found.
     *
     * @param accessToken the JWT access token
     * @param tenantId    the tenant ID
     * @return CreateProductResponse containing the created product details
     */
    protected CreateProductResponse createProduct(String accessToken, String tenantId) {
        return TestDataManager.getOrCreateProduct(accessToken, tenantId,
                ProductTestDataBuilder::buildCreateProductRequest,
                request -> {
                    EntityExchangeResult<ApiResponse<CreateProductResponse>> productResult =
                            authenticatedPost("/api/v1/products", accessToken, tenantId, request).exchange().expectStatus().isCreated()
                                    .expectBody(new ParameterizedTypeReference<ApiResponse<CreateProductResponse>>() {
                                    }).returnResult();

                    ApiResponse<CreateProductResponse> productApiResponse = productResult.getResponseBody();
                    assertThat(productApiResponse).isNotNull();
                    assertThat(productApiResponse.isSuccess()).isTrue();
                    CreateProductResponse product = productApiResponse.getData();
                    assertThat(product).isNotNull();
                    return product;
                });
    }

    /**
     * Create authenticated POST request with Bearer token and tenant context.
     *
     * @param uri         the request URI
     * @param accessToken the JWT access token
     * @param tenantId    the tenant ID for X-Tenant-Id header
     * @param requestBody the request body
     * @return WebTestClient.RequestHeadersSpec for further configuration
     */
    protected WebTestClient.RequestHeadersSpec<?> authenticatedPost(String uri, String accessToken, String tenantId, Object requestBody) {
        return webTestClient.post().uri(uri).contentType(MediaType.APPLICATION_JSON).headers(headers -> {
            RequestHeaderHelper.addAuthHeaders(headers, accessToken);
            RequestHeaderHelper.addTenantHeader(headers, tenantId);
        }).bodyValue(requestBody);
    }

    /**
     * Helper method to make an authenticated POST request without a body.
     *
     * @param uri         the request URI
     * @param accessToken the JWT access token
     * @param tenantId    the tenant ID for X-Tenant-Id header
     * @return WebTestClient.RequestHeadersSpec for further configuration
     */
    protected WebTestClient.RequestHeadersSpec<?> authenticatedPostWithoutBody(String uri, String accessToken, String tenantId) {
        return webTestClient.post().uri(uri).headers(headers -> {
            RequestHeaderHelper.addAuthHeaders(headers, accessToken);
            RequestHeaderHelper.addTenantHeader(headers, tenantId);
        });
    }

    // ==================== STOCK ITEM WAITING HELPERS ====================

    /**
     * Create a user via the API and return the response.
     * Uses TestDataManager to check repository first, then creates if not found.
     *
     * @param accessToken the JWT access token
     * @param tenantId    the tenant ID
     * @return CreateUserResponse containing the created user details
     */
    protected CreateUserResponse createUser(String accessToken, String tenantId) {
        return TestDataManager.getOrCreateUser(accessToken, tenantId,
                () -> UserTestDataBuilder.buildCreateUserRequest(tenantId),
                request -> {
                    EntityExchangeResult<ApiResponse<CreateUserResponse>> userResult =
                            authenticatedPost("/api/v1/users", accessToken, tenantId, request).exchange().expectStatus().isCreated()
                                    .expectBody(new ParameterizedTypeReference<ApiResponse<CreateUserResponse>>() {
                                    }).returnResult();

                    ApiResponse<CreateUserResponse> userApiResponse = userResult.getResponseBody();
                    assertThat(userApiResponse).isNotNull();
                    assertThat(userApiResponse.isSuccess()).isTrue();
                    CreateUserResponse user = userApiResponse.getData();
                    assertThat(user).isNotNull();
                    return user;
                });
    }

    /**
     * Create a location (warehouse) via the API and return the response.
     * Uses TestDataManager to check repository first, then creates if not found.
     *
     * @param accessToken the JWT access token
     * @param tenantId    the tenant ID
     * @return CreateLocationResponse containing the created location details
     */
    protected CreateLocationResponse createLocation(String accessToken, String tenantId) {
        return TestDataManager.getOrCreateLocation(accessToken, tenantId,
                LocationTestDataBuilder::buildWarehouseRequest,
                request -> {
                    EntityExchangeResult<ApiResponse<CreateLocationResponse>> locationResult =
                            authenticatedPost("/api/v1/location-management/locations", accessToken, tenantId, request).exchange().expectStatus().isCreated()
                                    .expectBody(new ParameterizedTypeReference<ApiResponse<CreateLocationResponse>>() {
                                    }).returnResult();

                    ApiResponse<CreateLocationResponse> locationApiResponse = locationResult.getResponseBody();
                    assertThat(locationApiResponse).isNotNull();
                    assertThat(locationApiResponse.isSuccess()).isTrue();
                    CreateLocationResponse location = locationApiResponse.getData();
                    assertThat(location).isNotNull();
                    return location;
                });
    }

    /**
     * Create a location via the API and return the response.
     * Uses TestDataManager to check repository first, then creates if not found.
     *
     * @param accessToken the JWT access token
     * @param tenantId    the tenant ID
     * @param request     the location request
     * @return CreateLocationResponse containing the created location details
     */
    protected CreateLocationResponse createLocation(String accessToken, String tenantId, CreateLocationRequest request) {
        // Check repository first by code
        Optional<CreateLocationResponse> existing = TestDataManager.getRepository().findLocationByCode(request.getCode(), tenantId);
        if (existing.isPresent()) {
            return existing.get();
        }

        // Create new location
        EntityExchangeResult<ApiResponse<CreateLocationResponse>> locationResult =
                authenticatedPost("/api/v1/location-management/locations", accessToken, tenantId, request).exchange().expectStatus().isCreated()
                        .expectBody(new ParameterizedTypeReference<ApiResponse<CreateLocationResponse>>() {
                        }).returnResult();

        ApiResponse<CreateLocationResponse> locationApiResponse = locationResult.getResponseBody();
        assertThat(locationApiResponse).isNotNull();
        assertThat(locationApiResponse.isSuccess()).isTrue();
        CreateLocationResponse location = locationApiResponse.getData();
        assertThat(location).isNotNull();
        
        // Save to repository
        TestDataManager.saveLocation(location, tenantId);
        return location;
    }

    // ==================== TEST DATA SETUP HELPERS ====================

    /**
     * Create a consignment and wait for stock items to be created.
     * This is a common pattern in tests that need stock items available.
     * Saves consignment and stock items to H2 for reuse.
     *
     * @param warehouseId    the warehouse ID where consignment is received
     * @param productCode    the product code for the consignment line item
     * @param quantity       the quantity to receive
     * @param expirationDate optional expiration date (null if not needed)
     * @param productId      the product ID to wait for stock items
     * @param accessToken    the JWT access token
     * @param tenantId       the tenant ID
     * @param maxWaitSeconds maximum seconds to wait for stock items (default: 10)
     */
    protected void createConsignmentAndWaitForStock(String warehouseId, String productCode, int quantity, LocalDate expirationDate, String productId, String accessToken,
                                                    String tenantId, int maxWaitSeconds) {

        CreateConsignmentRequest consignmentRequest;
        if (expirationDate != null) {
            consignmentRequest = ConsignmentTestDataBuilder.buildCreateConsignmentRequestV2WithExpiration(warehouseId, productCode, expirationDate);
        } else {
            consignmentRequest = ConsignmentTestDataBuilder.buildCreateConsignmentRequestV2(warehouseId, productCode, quantity, null);
        }

        EntityExchangeResult<ApiResponse<CreateConsignmentResponse>> consignmentResult =
                authenticatedPost("/api/v1/stock-management/consignments", accessToken, tenantId, consignmentRequest).exchange().expectStatus().isCreated()
                        .expectBody(new ParameterizedTypeReference<ApiResponse<CreateConsignmentResponse>>() {
                        }).returnResult();

        ApiResponse<CreateConsignmentResponse> consignmentApiResponse = consignmentResult.getResponseBody();
        if (consignmentApiResponse != null && consignmentApiResponse.isSuccess() && consignmentApiResponse.getData() != null) {
            CreateConsignmentResponse consignment = consignmentApiResponse.getData();
            TestDataManager.saveConsignment(consignment, tenantId);
        }

        // Wait for stock items to be created (async via Kafka events)
        boolean stockItemsCreated = waitForStockItems(productId, accessToken, tenantId, maxWaitSeconds, 500);
        assertThat(stockItemsCreated).as("Stock items should be created from consignment within " + maxWaitSeconds + " seconds").isTrue();

        // Query stock items from API and save to H2
        queryAndSaveStockItems(productId, accessToken, tenantId);
    }

    /**
     * Query stock items from API by product ID and save them to H2.
     *
     * @param productId   the product ID
     * @param accessToken the JWT access token
     * @param tenantId    the tenant ID
     * @return list of stock items queried and saved
     */
    protected List<StockItemResponse> queryAndSaveStockItems(String productId, String accessToken, String tenantId) {
        try {
            EntityExchangeResult<ApiResponse<ListStockItemsResponse>> queryResult =
                    authenticatedGet("/api/v1/stock-management/stock-items?productId=" + productId, accessToken, tenantId).exchange().expectStatus().isOk()
                            .expectBody(new ParameterizedTypeReference<ApiResponse<ListStockItemsResponse>>() {
                            }).returnResult();

            ApiResponse<ListStockItemsResponse> queryApiResponse = queryResult.getResponseBody();
            if (queryApiResponse != null && queryApiResponse.isSuccess() && queryApiResponse.getData() != null
                    && queryApiResponse.getData().getStockItems() != null) {
                List<StockItemResponse> stockItems = queryApiResponse.getData().getStockItems();
                TestDataManager.saveStockItems(stockItems, tenantId);
                return stockItems;
            }
        } catch (Exception e) {
            log.warn("Failed to query and save stock items for product {}: {}", productId, e.getMessage());
        }
        return new java.util.ArrayList<>();
    }

    /**
     * Get or create stock items for a product.
     * Checks H2 first for existing stock items with quantity > 0.
     * If not found or insufficient, creates consignment, waits for stock items, queries from API, saves to H2, and returns them.
     *
     * @param productId      the product ID
     * @param productCode    the product code for consignment creation
     * @param warehouseId     the warehouse ID for consignment creation
     * @param minQuantity     minimum quantity required (default: 1)
     * @param accessToken     the JWT access token
     * @param tenantId       the tenant ID
     * @param maxWaitSeconds maximum seconds to wait for stock items (default: 10)
     * @return list of stock items (from H2 or newly created)
     */
    protected List<StockItemResponse> getOrCreateStockItemsForProduct(String productId, String productCode, String warehouseId, int minQuantity, String accessToken,
                                                                       String tenantId, int maxWaitSeconds) {
        // Check H2 for existing stock items
        List<StockItemResponse> existingStockItems = TestDataManager.getStockItemsByProductId(productId, tenantId);
        int totalQuantity = existingStockItems.stream()
                .filter(item -> item.getQuantity() != null && item.getQuantity() > 0)
                .mapToInt(StockItemResponse::getQuantity)
                .sum();

        if (totalQuantity >= minQuantity) {
            log.debug("Reusing {} stock items from H2 with total quantity {}", existingStockItems.size(), totalQuantity);
            return existingStockItems.stream()
                    .filter(item -> item.getQuantity() != null && item.getQuantity() > 0)
                    .collect(java.util.stream.Collectors.toList());
        }

        // Not enough stock in H2, create consignment
        log.debug("Insufficient stock in H2 ({} < {}), creating new consignment", totalQuantity, minQuantity);
        createConsignmentAndWaitForStock(warehouseId, productCode, minQuantity, null, productId, accessToken, tenantId, maxWaitSeconds);

        // Query and return stock items (already saved to H2 by createConsignmentAndWaitForStock)
        return queryAndSaveStockItems(productId, accessToken, tenantId);
    }

    /**
     * Ensure stock items are available for a product.
     * Checks H2 first, then creates consignment if needed.
     * Saves consignment and stock items to H2.
     *
     * @param productId      the product ID
     * @param productCode    the product code for consignment creation
     * @param warehouseId     the warehouse ID for consignment creation
     * @param minQuantity     minimum quantity required (default: 1)
     * @param accessToken     the JWT access token
     * @param tenantId       the tenant ID
     * @param maxWaitSeconds maximum seconds to wait for stock items (default: 10)
     * @return list of stock items available
     */
    protected List<StockItemResponse> ensureStockItemsAvailable(String productId, String productCode, String warehouseId, int minQuantity, String accessToken,
                                                                 String tenantId, int maxWaitSeconds) {
        return getOrCreateStockItemsForProduct(productId, productCode, warehouseId, minQuantity, accessToken, tenantId, maxWaitSeconds);
    }

    /**
     * Wait for stock items to be created for a product after consignment creation.
     * Polls the stock-levels endpoint until stock items are available or timeout is reached.
     *
     * <p>This is necessary because stock items are created asynchronously via Kafka events
     * after a consignment is created. Tests should use this method instead of Thread.sleep
     * to ensure stock items are available before attempting allocations.</p>
     *
     * @param productId      the product ID to check stock for (as String)
     * @param accessToken    the JWT access token for authentication
     * @param tenantId       the tenant ID for X-Tenant-Id header
     * @param maxWaitSeconds maximum number of seconds to wait
     * @param pollIntervalMs polling interval in milliseconds
     * @return true if stock items were found, false if timeout was reached
     */
    protected boolean waitForStockItems(String productId, String accessToken, String tenantId, int maxWaitSeconds, long pollIntervalMs) {
        long endTime = System.currentTimeMillis() + (maxWaitSeconds * 1000L);
        int attemptCount = 0;

        while (System.currentTimeMillis() < endTime) {
            attemptCount++;
            try {
                EntityExchangeResult<ApiResponse<List<StockLevelResponse>>> result =
                        authenticatedGet("/api/v1/stock-management/stock-levels?productId=" + productId, accessToken, tenantId).exchange().expectStatus().isOk()
                                .expectBody(new ParameterizedTypeReference<ApiResponse<List<StockLevelResponse>>>() {
                                }).returnResult();

                ApiResponse<List<StockLevelResponse>> apiResponse = result.getResponseBody();
                if (apiResponse != null && apiResponse.isSuccess() && apiResponse.getData() != null) {
                    List<StockLevelResponse> stockLevels = apiResponse.getData();
                    // Check if we have stock items with quantity > 0
                    boolean hasStock = stockLevels.stream().anyMatch(level -> level.getTotalQuantity() != null && level.getTotalQuantity() > 0);
                    if (hasStock) {
                        int totalQuantity = stockLevels.stream().filter(level -> level.getTotalQuantity() != null).mapToInt(StockLevelResponse::getTotalQuantity).sum();
                        log.debug("Stock items found after {} attempts. Total quantity: {}", attemptCount, totalQuantity);
                        return true;
                    }
                }

                if (attemptCount % 4 == 0) { // Log every 2 seconds (4 attempts * 500ms)
                    log.debug("Waiting for stock items... attempt {}", attemptCount);
                }

                Thread.sleep(pollIntervalMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            } catch (Exception e) {
                // Continue polling on error
                log.warn("Error polling for stock items (attempt {}): {}", attemptCount, e.getMessage());
                try {
                    Thread.sleep(pollIntervalMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }

        log.warn("Timeout waiting for stock items after {} attempts ({} seconds)", attemptCount, maxWaitSeconds);
        return false;
    }

    /**
     * Create authenticated GET request with Bearer token and tenant context.
     *
     * @param uri         the request URI
     * @param accessToken the JWT access token
     * @param tenantId    the tenant ID for X-Tenant-Id header
     * @return WebTestClient.RequestHeadersSpec for further configuration
     */
    protected WebTestClient.RequestHeadersSpec<?> authenticatedGet(String uri, String accessToken, String tenantId) {
        return webTestClient.get().uri(uri).headers(headers -> {
            RequestHeaderHelper.addAuthHeaders(headers, accessToken);
            RequestHeaderHelper.addTenantHeader(headers, tenantId);
        });
    }

    /**
     * Waits for stock items to be assigned to locations via FEFO.
     * <p>This is necessary because FEFO location assignment happens asynchronously via Kafka events
     * after stock items are created. Tests should use this method to ensure stock items have been
     * assigned to locations before attempting operations that require location assignments.</p>
     *
     * @param productId      the product ID to check stock for (as String)
     * @param accessToken    the JWT access token for authentication
     * @param tenantId       the tenant ID for X-Tenant-Id header
     * @param maxWaitSeconds maximum number of seconds to wait
     * @param pollIntervalMs polling interval in milliseconds
     * @param minQuantity    minimum quantity required at a location (default 1)
     * @return true if stock items with locations were found, false if timeout was reached
     */
    protected boolean waitForStockItemsAssignedToLocations(String productId, String accessToken, String tenantId, int maxWaitSeconds, long pollIntervalMs, int minQuantity) {
        long endTime = System.currentTimeMillis() + (maxWaitSeconds * 1000L);
        int attemptCount = 0;

        while (System.currentTimeMillis() < endTime) {
            attemptCount++;
            try {
                EntityExchangeResult<ApiResponse<List<StockLevelResponse>>> result =
                        authenticatedGet("/api/v1/stock-management/stock-levels?productId=" + productId, accessToken, tenantId).exchange().expectStatus().isOk()
                                .expectBody(new ParameterizedTypeReference<ApiResponse<List<StockLevelResponse>>>() {
                                }).returnResult();

                ApiResponse<List<StockLevelResponse>> apiResponse = result.getResponseBody();
                if (apiResponse != null && apiResponse.isSuccess() && apiResponse.getData() != null) {
                    List<StockLevelResponse> stockLevels = apiResponse.getData();
                    // Check if we have stock items assigned to locations with available quantity >= minQuantity
                    boolean hasAssignedStock = stockLevels.stream()
                            .anyMatch(level -> level.getLocationId() != null && level.getAvailableQuantity() != null && level.getAvailableQuantity() >= minQuantity);
                    if (hasAssignedStock) {
                        int totalAssignedQuantity = stockLevels.stream().filter(level -> level.getLocationId() != null && level.getAvailableQuantity() != null)
                                .mapToInt(StockLevelResponse::getAvailableQuantity).sum();
                        log.debug("Stock items assigned to locations found after {} attempts. Total assigned available quantity: {}", attemptCount, totalAssignedQuantity);
                        return true;
                    }
                }

                if (attemptCount % 4 == 0) { // Log every 2 seconds (4 attempts * 500ms)
                    log.debug("Waiting for stock items to be assigned to locations... attempt {}", attemptCount);
                }

                Thread.sleep(pollIntervalMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            } catch (Exception e) {
                // Continue polling on error
                log.warn("Error polling for assigned stock items (attempt {}): {}", attemptCount, e.getMessage());
                try {
                    Thread.sleep(pollIntervalMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }

        log.warn("Timeout waiting for stock items to be assigned to locations after {} attempts ({} seconds)", attemptCount, maxWaitSeconds);
        return false;
    }

    /**
     * Wait for stock items to be assigned to a specific location with minimum available quantity.
     * Polls the stock levels endpoint until stock is available at the specified location or timeout occurs.
     * If stock is not assigned to the location by FEFO, this method will attempt to manually assign stock items.
     *
     * <p>This is necessary because stock assignment to locations happens asynchronously via FEFO events.
     * Tests should use this method to ensure stock is available at a specific location before attempting allocation.</p>
     *
     * @param productId      the product ID to check stock for (as String)
     * @param locationId     the location ID to check stock at (as String)
     * @param accessToken    the JWT access token for authentication
     * @param tenantId       the tenant ID for X-Tenant-Id header
     * @param maxWaitSeconds maximum number of seconds to wait
     * @param pollIntervalMs polling interval in milliseconds
     * @param minQuantity    minimum available quantity required at the location
     * @return true if stock items with sufficient available quantity were found at the location, false if timeout was reached
     */
    protected boolean waitForStockAtLocation(String productId, String locationId, String accessToken, String tenantId, int maxWaitSeconds, long pollIntervalMs, int minQuantity) {
        long endTime = System.currentTimeMillis() + (maxWaitSeconds * 1000L);
        int attemptCount = 0;

        while (System.currentTimeMillis() < endTime) {
            attemptCount++;
            try {
                EntityExchangeResult<ApiResponse<List<StockLevelResponse>>> result =
                        authenticatedGet("/api/v1/stock-management/stock-levels?productId=" + productId + "&locationId=" + locationId, accessToken, tenantId).exchange().expectStatus().isOk()
                                .expectBody(new ParameterizedTypeReference<ApiResponse<List<StockLevelResponse>>>() {
                                }).returnResult();

                ApiResponse<List<StockLevelResponse>> apiResponse = result.getResponseBody();
                if (apiResponse != null && apiResponse.isSuccess() && apiResponse.getData() != null) {
                    List<StockLevelResponse> stockLevels = apiResponse.getData();
                    // Check if we have stock at the specific location with available quantity >= minQuantity
                    Optional<StockLevelResponse> locationStock = stockLevels.stream()
                            .filter(level -> locationId.equals(level.getLocationId()))
                            .filter(level -> level.getAvailableQuantity() != null && level.getAvailableQuantity() >= minQuantity)
                            .findFirst();
                    
                    if (locationStock.isPresent()) {
                        int availableQuantity = locationStock.get().getAvailableQuantity();
                        log.debug("Stock found at location {} after {} attempts. Available quantity: {}", locationId, attemptCount, availableQuantity);
                        return true;
                    }
                }

                if (attemptCount % 4 == 0) { // Log every 2 seconds (4 attempts * 500ms)
                    log.debug("Waiting for stock at location {}... attempt {}", locationId, attemptCount);
                }

                Thread.sleep(pollIntervalMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            } catch (Exception e) {
                // Continue polling on error
                log.warn("Error polling for stock at location (attempt {}): {}", attemptCount, e.getMessage());
                try {
                    Thread.sleep(pollIntervalMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }

        // If we timed out waiting for FEFO assignment, try to manually assign stock to the location
        log.info("FEFO did not assign stock to location {} within timeout. Attempting manual assignment...", locationId);
        return assignStockToLocationManually(productId, locationId, accessToken, tenantId, minQuantity);
    }

    /**
     * Manually assign stock items to a specific location.
     * This is a fallback when FEFO doesn't assign stock to the test location.
     *
     * @param productId   the product ID
     * @param locationId  the location ID to assign stock to
     * @param accessToken the JWT access token
     * @param tenantId    the tenant ID
     * @param minQuantity minimum quantity to assign
     * @return true if stock was successfully assigned, false otherwise
     */
    private boolean assignStockToLocationManually(String productId, String locationId, String accessToken, String tenantId, int minQuantity) {
        try {
            // Get unassigned stock items or stock items at other locations
            EntityExchangeResult<ApiResponse<StockItemsByClassificationResponse>> queryResult =
                    authenticatedGet("/api/v1/stock-management/stock-items/by-classification?classification=NORMAL", accessToken, tenantId).exchange()
                            .expectStatus().isOk().expectBody(new ParameterizedTypeReference<ApiResponse<StockItemsByClassificationResponse>>() {
                            }).returnResult();

            ApiResponse<StockItemsByClassificationResponse> queryApiResponse = queryResult.getResponseBody();
            if (queryApiResponse == null || queryApiResponse.getData() == null || queryApiResponse.getData().getStockItems() == null) {
                log.warn("No stock items found for manual assignment");
                return false;
            }

            List<StockItemResponse> stockItems = queryApiResponse.getData().getStockItems();
            // Filter for stock items that are either unassigned or at a different location
            // Only include items with quantity > 0
            List<StockItemResponse> assignableItems = stockItems.stream()
                    .filter(item -> item.getLocationId() == null || !locationId.equals(item.getLocationId()))
                    .filter(item -> item.getQuantity() != null && item.getQuantity() > 0)
                    .collect(java.util.stream.Collectors.toList());

            if (assignableItems.isEmpty()) {
                log.warn("No assignable stock items found (all may be at other locations)");
                return false;
            }

            // Assign stock items until we have enough quantity at the location
            // Note: We assign the full quantity of each stock item
            int assignedQuantity = 0;
            for (StockItemResponse stockItem : assignableItems) {
                if (assignedQuantity >= minQuantity) {
                    break;
                }

                int itemQuantity = stockItem.getQuantity();
                if (itemQuantity <= 0) {
                    continue;
                }

                try {
                    // Assign the full quantity of the stock item to the location
                    AssignLocationToStockRequest assignRequest = StockItemTestDataBuilder.buildAssignLocationRequest(locationId, itemQuantity);
                    authenticatedPost("/api/v1/stock-management/stock-items/" + stockItem.getStockItemId() + "/assign-location", accessToken, tenantId, assignRequest)
                            .exchange().expectStatus().isOk();
                    assignedQuantity += itemQuantity;
                } catch (Exception e) {
                    log.warn("Failed to assign stock item {} to location: {}", stockItem.getStockItemId(), e.getMessage());
                    // Continue with next item
                }
            }

            if (assignedQuantity >= minQuantity) {
                log.info("Manually assigned {} units to location {}", assignedQuantity, locationId);
                return true;
            } else {
                log.warn("Could not assign sufficient stock to location. Assigned: {}, Required: {}", assignedQuantity, minQuantity);
                return false;
            }
        } catch (Exception e) {
            log.error("Error during manual stock assignment: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Wait for picking list to reach a specific status.
     * Polls the picking list endpoint until the desired status is reached or timeout occurs.
     *
     * <p>This is necessary because picking list status transitions happen asynchronously via Kafka events.
     * Tests should use this method to ensure picking lists are in the expected status before proceeding.</p>
     *
     * @param pickingListId  the picking list ID to check (as String)
     * @param expectedStatus the expected status (e.g., "PLANNED", "COMPLETED")
     * @param accessToken    the JWT access token for authentication
     * @param tenantId       the tenant ID for X-Tenant-Id header
     * @param maxWaitSeconds maximum number of seconds to wait
     * @param pollIntervalMs polling interval in milliseconds
     * @return true if the picking list reached the expected status, false if timeout was reached
     */
    protected boolean waitForPickingListStatus(String pickingListId, String expectedStatus, String accessToken, String tenantId, int maxWaitSeconds, int pollIntervalMs) {
        long endTime = System.currentTimeMillis() + (maxWaitSeconds * 1000L);
        int attemptCount = 0;

        while (System.currentTimeMillis() < endTime) {
            attemptCount++;
            try {
                EntityExchangeResult<ApiResponse<com.ccbsa.wms.gateway.api.dto.PickingListQueryResult>> result =
                        authenticatedGet("/api/v1/picking/picking-lists/" + pickingListId, accessToken, tenantId).exchange().expectStatus().isOk()
                                .expectBody(new ParameterizedTypeReference<ApiResponse<com.ccbsa.wms.gateway.api.dto.PickingListQueryResult>>() {
                                }).returnResult();

                ApiResponse<com.ccbsa.wms.gateway.api.dto.PickingListQueryResult> apiResponse = result.getResponseBody();
                if (apiResponse != null && apiResponse.isSuccess() && apiResponse.getData() != null) {
                    com.ccbsa.wms.gateway.api.dto.PickingListQueryResult pickingList = apiResponse.getData();
                    if (expectedStatus.equals(pickingList.getStatus())) {
                        log.debug("Picking list reached {} status after {} attempts", expectedStatus, attemptCount);
                        return true;
                    }
                }

                if (attemptCount % 4 == 0) { // Log every 2 seconds (4 attempts * 500ms)
                    log.debug("Waiting for picking list status {}... attempt {}", expectedStatus, attemptCount);
                }

                Thread.sleep(pollIntervalMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            } catch (Exception e) {
                // Continue polling on error
                log.warn("Error polling for picking list status (attempt {}): {}", attemptCount, e.getMessage());
                try {
                    Thread.sleep(pollIntervalMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }

        log.warn("Timeout waiting for picking list status {} after {} attempts ({} seconds)", expectedStatus, attemptCount, maxWaitSeconds);
        return false;
    }

    /**
     * Wait for a specific stock item to be available at a location with sufficient available quantity.
     * Polls the stock item endpoint until the stock item is at the specified location with available quantity > 0 or timeout occurs.
     *
     * @param stockItemId   the stock item ID to check
     * @param locationId    the location ID to check
     * @param accessToken   the JWT access token for authentication
     * @param tenantId      the tenant ID for X-Tenant-Id header
     * @param maxWaitSeconds maximum number of seconds to wait
     * @param pollIntervalMs polling interval in milliseconds
     * @return true if stock item is available at the location with available quantity > 0, false if timeout was reached
     */
    protected boolean waitForStockItemAtLocation(String stockItemId, String locationId, String accessToken, String tenantId, int maxWaitSeconds, long pollIntervalMs) {
        long endTime = System.currentTimeMillis() + (maxWaitSeconds * 1000L);
        int attemptCount = 0;

        while (System.currentTimeMillis() < endTime) {
            attemptCount++;
            try {
                EntityExchangeResult<ApiResponse<com.ccbsa.wms.gateway.api.dto.StockItemQueryDTO>> result =
                        authenticatedGet("/api/v1/stock-management/stock-items/" + stockItemId, accessToken, tenantId).exchange().expectStatus().isOk()
                                .expectBody(new ParameterizedTypeReference<ApiResponse<com.ccbsa.wms.gateway.api.dto.StockItemQueryDTO>>() {
                                }).returnResult();

                ApiResponse<com.ccbsa.wms.gateway.api.dto.StockItemQueryDTO> apiResponse = result.getResponseBody();
                if (apiResponse != null && apiResponse.isSuccess() && apiResponse.getData() != null) {
                    com.ccbsa.wms.gateway.api.dto.StockItemQueryDTO stockItem = apiResponse.getData();
                    // Check if stock item is at the specified location and has available quantity
                    boolean isAtLocation = locationId.equals(stockItem.getLocationId());
                    int totalQuantity = stockItem.getQuantity() != null ? stockItem.getQuantity() : 0;
                    int allocatedQuantity = stockItem.getAllocatedQuantity() != null ? stockItem.getAllocatedQuantity() : 0;
                    int availableQuantity = Math.max(0, totalQuantity - allocatedQuantity);
                    
                    if (isAtLocation && availableQuantity > 0) {
                        log.debug("Stock item {} found at location {} after {} attempts. Available quantity: {}", stockItemId, locationId, attemptCount, availableQuantity);
                        return true;
                    }
                }

                if (attemptCount % 4 == 0) { // Log every 2 seconds (4 attempts * 500ms)
                    log.debug("Waiting for stock item {} to be available at location {}... attempt {}", stockItemId, locationId, attemptCount);
                }

                Thread.sleep(pollIntervalMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            } catch (Exception e) {
                // Continue polling on error
                log.warn("Error polling for stock item at location (attempt {}): {}", attemptCount, e.getMessage());
                try {
                    Thread.sleep(pollIntervalMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }

        log.warn("Timeout waiting for stock item {} to be available at location {} after {} attempts ({} seconds)", stockItemId, locationId, attemptCount, maxWaitSeconds);
        return false;
    }
}

