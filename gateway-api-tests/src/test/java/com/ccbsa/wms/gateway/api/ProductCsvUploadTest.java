package com.ccbsa.wms.gateway.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.BodyInserters;

import com.ccbsa.wms.gateway.api.util.RequestHeaderHelper;

/**
 * Gateway API integration tests for Product CSV Upload functionality.
 * <p>
 * These tests validate CSV file upload through the gateway to the Product Service.
 * <p>
 * Note: Full multipart file upload testing requires more complex setup.
 * These tests validate authentication and basic endpoint availability.
 */
@DisplayName("Product CSV Upload API Tests")
class ProductCsvUploadTest extends BaseIntegrationTest {

    @Test
    @DisplayName("Should require authentication for CSV upload")
    void shouldRequireAuthenticationForCsvUpload() {
        webTestClient
                .post()
                .uri("/product-service/products/upload-csv")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData("file", "test content".getBytes()))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("Should reject request without file")
    void shouldRejectRequestWithoutFile() {
        RequestHeaderHelper.addTenantHeaderIfNeeded(
                        webTestClient
                                .post()
                                .uri("/product-service/products/upload-csv")
                                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", accessToken))
                                .contentType(MediaType.MULTIPART_FORM_DATA),
                        authHelper,
                        accessToken)
                .exchange()
                .expectStatus().is4xxClientError();
    }
}

