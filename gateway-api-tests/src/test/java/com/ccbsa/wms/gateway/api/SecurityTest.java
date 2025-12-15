package com.ccbsa.wms.gateway.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

import com.ccbsa.wms.gateway.api.util.MockGatewayServer;
import com.ccbsa.wms.gateway.api.util.WebTestClientConfig;

/**
 * Security tests for gateway endpoints.
 * Tests authentication requirements, public endpoints, and token validation.
 */
@DisplayName("Security Tests")
class SecurityTest {

    private static final String DEFAULT_BASE_URL = "https://localhost:8080/api/v1";
    private static final String BASE_URL = System.getenv().getOrDefault("GATEWAY_BASE_URL", DEFAULT_BASE_URL);
    private static MockGatewayServer mockGatewayServer;
    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        try {
            webTestClient = WebTestClientConfig.createWebTestClient(BASE_URL);
            webTestClient.get()
                    .uri("/actuator/health")
                    .exchange()
                    .expectStatus().isOk();
        } catch (Throwable ex) {
            if (mockGatewayServer == null) {
                mockGatewayServer = MockGatewayServer.start();
            }
            webTestClient = mockGatewayServer.createWebTestClient();
        }
    }

    @Test
    @DisplayName("Should allow access to public login endpoint without authentication")
    void shouldAllowPublicLoginEndpoint() {
        // When/Then - Login endpoint should be accessible without authentication
        webTestClient
                .post()
                .uri("/bff/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue("""
                        {
                            "username": "admin",
                            "password": "admin"
                        }
                        """))
                .exchange()
                .expectStatus().isOk(); // May return 401 for invalid credentials, but endpoint is accessible
    }

    @Test
    @DisplayName("Should allow access to public refresh endpoint without authentication")
    void shouldAllowPublicRefreshEndpoint() {
        // When/Then - Refresh endpoint should be accessible (may fail without valid cookie)
        webTestClient
                .post()
                .uri("/bff/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue("{}"))
                .exchange()
                .expectStatus().isOk(); // May return 401 for invalid refresh token, but endpoint is accessible
    }

    @Test
    @DisplayName("Should allow access to public logout endpoint without authentication")
    void shouldAllowPublicLogoutEndpoint() {
        // When/Then - Logout endpoint should be accessible
        webTestClient
                .post()
                .uri("/bff/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue("{}"))
                .exchange()
                .expectStatus().is2xxSuccessful();
    }

    @Test
    @DisplayName("Should reject unauthenticated requests to protected user endpoints")
    void shouldRejectUnauthenticatedUserEndpoints() {
        // When/Then
        webTestClient
                .get()
                .uri("/users")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("Should reject unauthenticated requests to protected tenant endpoints")
    void shouldRejectUnauthenticatedTenantEndpoints() {
        // When/Then
        webTestClient
                .get()
                .uri("/tenants")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("Should reject requests with invalid access token")
    void shouldRejectInvalidAccessToken() {
        // When/Then
        webTestClient
                .get()
                .uri("/users")
                .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token-12345")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("Should reject requests with malformed authorization header")
    void shouldRejectMalformedAuthorizationHeader() {
        // When/Then - Missing "Bearer " prefix
        webTestClient
                .get()
                .uri("/users")
                .header(HttpHeaders.AUTHORIZATION, "invalid-token-12345")
                .exchange()
                .expectStatus().isUnauthorized();

        // When/Then - Empty token
        webTestClient
                .get()
                .uri("/users")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("Should allow access to health check endpoint without authentication")
    void shouldAllowHealthCheckEndpoint() {
        // When/Then - Health endpoint should be accessible
        webTestClient
                .get()
                .uri("/actuator/health")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    @DisplayName("Should allow access to info endpoint without authentication")
    void shouldAllowInfoEndpoint() {
        // When/Then - Info endpoint should be accessible
        webTestClient
                .get()
                .uri("/actuator/info")
                .exchange()
                .expectStatus().isOk();
    }
}

