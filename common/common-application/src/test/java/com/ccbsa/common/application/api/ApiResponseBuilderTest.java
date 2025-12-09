package com.ccbsa.common.application.api;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ApiResponseBuilder Tests")
class ApiResponseBuilderTest {
    @Test
    @DisplayName("Should create 200 OK response")
    void shouldCreateOkResponse() {
        // Given
        String data = "test-data";

        // When
        ResponseEntity<ApiResponse<String>> response = ApiResponseBuilder.ok(data);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        ApiResponse<String> body = response.getBody();
        assertThat(body).isNotNull();
        if (body != null) {
            assertThat(body.isSuccess()).isTrue();
            assertThat(body.getData()).isEqualTo(data);
        }
    }

    @Test
    @DisplayName("Should create 200 OK response with links")
    void shouldCreateOkResponseWithLinks() {
        // Given
        String data = "test-data";
        Map<String, String> links = Map.of("self",
                "/api/v1/resource");

        // When
        ResponseEntity<ApiResponse<String>> response = ApiResponseBuilder.ok(data,
                links);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        ApiResponse<String> body = response.getBody();
        assertThat(body).isNotNull();
        if (body != null) {
            assertThat(body.getLinks()).isEqualTo(links);
        }
    }

    @Test
    @DisplayName("Should create 200 OK response with links and meta")
    void shouldCreateOkResponseWithLinksAndMeta() {
        // Given
        String data = "test-data";
        Map<String, String> links = Map.of("self",
                "/api/v1/resource");
        ApiMeta meta = ApiMeta.builder()
                .pagination(ApiMeta.Pagination.of(1,
                        20,
                        100))
                .build();

        // When
        ResponseEntity<ApiResponse<String>> response = ApiResponseBuilder.ok(data,
                links,
                meta);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        ApiResponse<String> body = response.getBody();
        assertThat(body).isNotNull();
        if (body != null) {
            assertThat(body.getMeta()).isEqualTo(meta);
        }
    }

    @Test
    @DisplayName("Should create 201 Created response")
    void shouldCreateCreatedResponse() {
        // Given
        String data = "test-data";

        // When
        ResponseEntity<ApiResponse<String>> response = ApiResponseBuilder.created(data);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        ApiResponse<String> body = response.getBody();
        assertThat(body).isNotNull();
        if (body != null) {
            assertThat(body.getData()).isEqualTo(data);
        }
    }

    @Test
    @DisplayName("Should create 201 Created response with links")
    void shouldCreateCreatedResponseWithLinks() {
        // Given
        String data = "test-data";
        Map<String, String> links = Map.of("self",
                "/api/v1/resource/123");

        // When
        ResponseEntity<ApiResponse<String>> response = ApiResponseBuilder.created(data,
                links);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        ApiResponse<String> body = response.getBody();
        assertThat(body).isNotNull();
        if (body != null) {
            assertThat(body.getLinks()).isEqualTo(links);
        }
    }

    @Test
    @DisplayName("Should create 202 Accepted response")
    void shouldCreateAcceptedResponse() {
        // Given
        String data = "test-data";

        // When
        ResponseEntity<ApiResponse<String>> response = ApiResponseBuilder.accepted(data);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        ApiResponse<String> body = response.getBody();
        assertThat(body).isNotNull();
        if (body != null) {
            assertThat(body.getData()).isEqualTo(data);
        }
    }

    @Test
    @DisplayName("Should create 204 No Content response")
    void shouldCreateNoContentResponse() {
        // When
        ResponseEntity<ApiResponse<Void>> response = ApiResponseBuilder.noContent();

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNull();
    }

    @Test
    @DisplayName("Should create error response with status code, error code, and message")
    void shouldCreateErrorResponseWithStatusCodeAndMessage() {
        // Given
        HttpStatus status = HttpStatus.BAD_REQUEST;
        String errorCode = "VALIDATION_ERROR";
        String message = "Validation failed";

        // When
        ResponseEntity<ApiResponse<Void>> response = ApiResponseBuilder.error(status,
                errorCode,
                message);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(status);
        ApiResponse<Void> body = response.getBody();
        assertThat(body).isNotNull();
        if (body != null) {
            assertThat(body.isError()).isTrue();
            ApiError error = body.getError();
            assertThat(error).isNotNull();
            if (error != null) {
                assertThat(error.getCode()).isEqualTo(errorCode);
                assertThat(error.getMessage()).isEqualTo(message);
            }
        }
    }

    @Test
    @DisplayName("Should create error response with details")
    void shouldCreateErrorResponseWithDetails() {
        // Given
        HttpStatus status = HttpStatus.BAD_REQUEST;
        String errorCode = "VALIDATION_ERROR";
        String message = "Validation failed";
        Map<String, Object> details = Map.of("field",
                "quantity",
                "message",
                "Must be positive");

        // When
        ResponseEntity<ApiResponse<Void>> response = ApiResponseBuilder.error(status,
                errorCode,
                message,
                details);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(status);
        ApiResponse<Void> body = response.getBody();
        assertThat(body).isNotNull();
        if (body != null) {
            ApiError error = body.getError();
            assertThat(error).isNotNull();
            if (error != null) {
                assertThat(error.getDetails()).isEqualTo(details);
            }
        }
    }

    @Test
    @DisplayName("Should create error response with ApiError object")
    void shouldCreateErrorResponseWithApiErrorObject() {
        // Given
        HttpStatus status = HttpStatus.NOT_FOUND;
        ApiError error = ApiError.builder("RESOURCE_NOT_FOUND",
                        "Resource not found")
                .path("/api/v1/resource")
                .requestId("req-123")
                .build();

        // When
        ResponseEntity<ApiResponse<Void>> response = ApiResponseBuilder.error(status,
                error);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(status);
        ApiResponse<Void> body = response.getBody();
        assertThat(body).isNotNull();
        if (body != null) {
            assertThat(body.getError()).isEqualTo(error);
        }
    }

    @Test
    @DisplayName("Should throw exception when status is null")
    void shouldThrowExceptionWhenStatusIsNull() {
        // When/Then
        assertThatThrownBy(() -> ApiResponseBuilder.error(null,
                "ERROR_CODE",
                "message")).isInstanceOf(NullPointerException.class)
                .hasMessage("HTTP status cannot be null");
    }

    @Test
    @DisplayName("Should throw exception when ApiError is null")
    void shouldThrowExceptionWhenApiErrorIsNull() {
        // When/Then
        assertThatThrownBy(() -> ApiResponseBuilder.error(HttpStatus.BAD_REQUEST,
                (ApiError) null)).isInstanceOf(NullPointerException.class)
                .hasMessage("ApiError cannot be null");
    }
}

