package com.ccbsa.wms.gateway.api;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import com.ccbsa.wms.gateway.api.fixture.LocationTestDataBuilder;
import com.ccbsa.wms.gateway.api.util.RequestHeaderHelper;

import reactor.core.publisher.Mono;

/**
 * Gateway API integration tests for Location Management Service.
 * <p>
 * These tests validate end-to-end flow from gateway through to backend services,
 * ensuring proper routing, authentication, authorization, and data flow.
 */
@DisplayName("Location Management API Tests")
class LocationManagementTest extends BaseIntegrationTest {

    @Test
    @DisplayName("Should create location with valid data")
    void shouldCreateLocation() {
        Map<String, Object> createLocationRequest = LocationTestDataBuilder.builder()
                .zone("A")
                .aisle("01")
                .rack("01")
                .level("01")
                .description("Main storage area")
                .build();

        RequestHeaderHelper.addTenantHeaderIfNeeded(
                webTestClient
                        .post()
                        .uri("/location-management/locations")
                        .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Mono.just(createLocationRequest), Map.class),
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
    @DisplayName("Should generate barcode automatically if not provided")
    void shouldGenerateBarcodeAutomatically() {
        Map<String, Object> createLocationRequest = LocationTestDataBuilder.builder()
                .zone("B")
                .aisle("02")
                .rack("02")
                .level("02")
                .build();

        RequestHeaderHelper.addTenantHeaderIfNeeded(
                webTestClient
                        .post()
                        .uri("/location-management/locations")
                        .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Mono.just(createLocationRequest), Map.class),
                authHelper,
                accessToken)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data.barcode").exists()
                .jsonPath("$.data.barcode").isNotEmpty();
    }

    @Test
    @DisplayName("Should get location by ID")
    void shouldGetLocationById() {
        // First create a location
        String locationId = createTestLocation();

        // Then retrieve it
        RequestHeaderHelper.addTenantHeaderIfNeeded(
                webTestClient
                        .get()
                        .uri(String.format("/location-management/locations/%s", locationId))
                        .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", accessToken)),
                authHelper,
                accessToken)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data.locationId").isEqualTo(locationId)
                .jsonPath("$.data.barcode").exists()
                .jsonPath("$.data.coordinates").exists();
    }

    @Test
    @DisplayName("Should return 404 for non-existent location")
    void shouldReturn404ForNonExistentLocation() {
        String nonExistentId = "00000000-0000-0000-0000-000000000000";

        RequestHeaderHelper.addTenantHeaderIfNeeded(
                webTestClient
                        .get()
                        .uri(String.format("/location-management/locations/%s", nonExistentId))
                        .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", accessToken)),
                authHelper,
                accessToken)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    @DisplayName("Should require authentication")
    void shouldRequireAuthentication() {
        Map<String, Object> createLocationRequest = new HashMap<>();

        webTestClient
                .post()
                .uri("/location-management/locations")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(createLocationRequest), Map.class)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    /**
     * Helper method to create a test location and return its ID.
     *
     * @return Location ID
     */
    private String createTestLocation() {
        Map<String, Object> request = LocationTestDataBuilder.createDefault();

        byte[] responseBody = RequestHeaderHelper.addTenantHeaderIfNeeded(
                webTestClient
                        .post()
                        .uri("/location-management/locations")
                        .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Mono.just(request), Map.class),
                authHelper,
                accessToken)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .returnResult()
                .getResponseBody();

        return extractLocationId(responseBody);
    }

    /**
     * Extracts location ID from API response.
     */
    private String extractLocationId(byte[] responseBody) {
        try {
            com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(responseBody);
            com.fasterxml.jackson.databind.JsonNode data = root.path("data");
            com.fasterxml.jackson.databind.JsonNode locationIdNode = data.path("locationId");

            if (locationIdNode.isMissingNode()) {
                throw new RuntimeException("Location ID not found in response");
            }

            return locationIdNode.asText();
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract location ID from response", e);
        }
    }
}

