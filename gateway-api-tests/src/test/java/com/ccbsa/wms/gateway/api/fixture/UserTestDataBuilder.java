package com.ccbsa.wms.gateway.api.fixture;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

import com.ccbsa.wms.gateway.api.helper.AuthenticationHelper;
import com.ccbsa.wms.gateway.api.util.RequestHeaderHelper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

/**
 * Builder for creating test users via the gateway API.
 *
 * <p>This builder:
 * <ul>
 *   <li>Creates users with realistic test data</li>
 *   <li>Supports creating users in specific states (ACTIVE, INACTIVE, SUSPENDED)</li>
 *   <li>Handles API calls and response parsing</li>
 *   <li>Returns created user IDs for use in tests</li>
 * </ul>
 *
 * <p>Usage:
 * <pre>
 * String activeUserId = UserTestDataBuilder.builder()
 *     .withWebTestClient(webTestClient)
 *     .withAuthHelper(authHelper)
 *     .withAccessToken(accessToken)
 *     .withObjectMapper(objectMapper)
 *     .create()
 *     .withStatus(UserStatus.ACTIVE)
 *     .build();
 * </pre>
 */
@Slf4j
public class UserTestDataBuilder {

    private final WebTestClient webTestClient;
    private final AuthenticationHelper authHelper;
    private final String accessToken;
    private final ObjectMapper objectMapper;
    private final TestData testData;

    // User properties (with defaults)
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String password;
    private String tenantId;
    private List<String> roles;
    private UserStatus targetStatus;

    private UserTestDataBuilder(WebTestClient webTestClient,
                                AuthenticationHelper authHelper,
                                String accessToken,
                                ObjectMapper objectMapper) {
        this.webTestClient = webTestClient;
        this.authHelper = authHelper;
        this.accessToken = accessToken;
        this.objectMapper = objectMapper;
        this.testData = TestData.getInstance();

        // Set defaults
        this.username = testData.generateUsername();
        this.email = testData.generateEmail();
        this.firstName = testData.generateFirstName();
        this.lastName = testData.generateLastName();
        this.password = testData.getDefaultPassword();
        this.tenantId = authHelper.getTenantIdFromToken(accessToken);
        this.roles = new ArrayList<>();
        this.targetStatus = UserStatus.ACTIVE; // Default status
    }

    /**
     * Creates a new builder instance.
     */
    public static BuilderStarter builder() {
        return new BuilderStarter();
    }

    public UserTestDataBuilder withUsername(String username) {
        this.username = username;
        return this;
    }

    public UserTestDataBuilder withEmail(String email) {
        this.email = email;
        return this;
    }

    public UserTestDataBuilder withFirstName(String firstName) {
        this.firstName = firstName;
        return this;
    }

    public UserTestDataBuilder withLastName(String lastName) {
        this.lastName = lastName;
        return this;
    }

    public UserTestDataBuilder withPassword(String password) {
        this.password = password;
        return this;
    }

    public UserTestDataBuilder withTenantId(String tenantId) {
        this.tenantId = tenantId;
        return this;
    }

    public UserTestDataBuilder withRoles(List<String> roles) {
        this.roles = new ArrayList<>(roles);
        return this;
    }

    public UserTestDataBuilder withRole(String role) {
        this.roles.add(role);
        return this;
    }

    /**
     * Sets the target status for the user.
     * If INACTIVE or SUSPENDED, the user will be created as ACTIVE first,
     * then transitioned to the target status.
     *
     * @param status Target status
     * @return This builder
     */
    public UserTestDataBuilder withStatus(UserStatus status) {
        this.targetStatus = status;
        return this;
    }

    /**
     * Creates the user and returns the user ID.
     *
     * @return Created user ID
     * @throws RuntimeException if user creation fails
     */
    public String build() {
        // Step 1: Create user (always created as ACTIVE initially)
        String userId = createUser();
        log.debug("Created user {} with ID: {}", username, userId);

        // Step 2: Transition to target status if needed
        if (targetStatus == UserStatus.INACTIVE) {
            deactivateUser(userId);
            log.debug("Deactivated user {}", userId);
        } else if (targetStatus == UserStatus.SUSPENDED) {
            suspendUser(userId);
            log.debug("Suspended user {}", userId);
        }

        return userId;
    }

    /**
     * Creates a user via the API.
     *
     * @return Created user ID
     */
    private String createUser() {
        String createUserRequest = String.format("""
                        {
                            "tenantId": "%s",
                            "username": "%s",
                            "emailAddress": "%s",
                            "firstName": "%s",
                            "lastName": "%s",
                            "password": "%s"
                        }
                        """,
                tenantId,
                username,
                email,
                firstName,
                lastName,
                password
        );

        byte[] responseBody = RequestHeaderHelper.addTenantHeaderIfNeeded(
                        webTestClient
                                .post()
                                .uri("/users")
                                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", accessToken))
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(BodyInserters.fromValue(createUserRequest)),
                        authHelper,
                        accessToken)
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data.userId").exists()
                .returnResult()
                .getResponseBody();

        return extractUserId(responseBody);
    }

    /**
     * Deactivates a user via the API.
     *
     * @param userId User ID to deactivate
     */
    private void deactivateUser(String userId) {
        RequestHeaderHelper.addTenantHeaderIfNeeded(
                        webTestClient
                                .put()
                                .uri(String.format("/users/%s/deactivate", userId))
                                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", accessToken)),
                        authHelper,
                        accessToken)
                .exchange()
                .expectStatus().isNoContent();
    }

    /**
     * Suspends a user via the API.
     *
     * @param userId User ID to suspend
     */
    private void suspendUser(String userId) {
        RequestHeaderHelper.addTenantHeaderIfNeeded(
                        webTestClient
                                .put()
                                .uri(String.format("/users/%s/suspend", userId))
                                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", accessToken)),
                        authHelper,
                        accessToken)
                .exchange()
                .expectStatus().isNoContent();
    }

    /**
     * Extracts user ID from API response.
     */
    private String extractUserId(byte[] responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode data = root.path("data");
            JsonNode userIdNode = data.path("userId");

            if (userIdNode.isMissingNode()) {
                throw new RuntimeException("User ID not found in response");
            }

            return userIdNode.asText();
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract user ID from response", e);
        }
    }

    /**
     * User status enum for test data builder.
     */
    public enum UserStatus {
        ACTIVE,
        INACTIVE,
        SUSPENDED
    }

    /**
     * Builder starter to enforce required parameters.
     */
    public static class BuilderStarter {
        private WebTestClient webTestClient;
        private AuthenticationHelper authHelper;
        private String accessToken;
        private ObjectMapper objectMapper;

        public BuilderStarter withWebTestClient(WebTestClient webTestClient) {
            this.webTestClient = webTestClient;
            return this;
        }

        public BuilderStarter withAuthHelper(AuthenticationHelper authHelper) {
            this.authHelper = authHelper;
            return this;
        }

        public BuilderStarter withAccessToken(String accessToken) {
            this.accessToken = accessToken;
            return this;
        }

        public BuilderStarter withObjectMapper(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
            return this;
        }

        public UserTestDataBuilder create() {
            if (webTestClient == null || authHelper == null ||
                    accessToken == null || objectMapper == null) {
                throw new IllegalStateException(
                        "WebTestClient, AuthHelper, AccessToken, and ObjectMapper are required");
            }
            return new UserTestDataBuilder(webTestClient, authHelper,
                    accessToken, objectMapper);
        }
    }
}
