package com.ccbsa.common.application.api;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ApiError Tests")
class ApiErrorTest {
    @Test
    @DisplayName("Should create ApiError with required fields")
    void shouldCreateApiErrorWithRequiredFields() {
        // Given
        String code = "ERROR_CODE";
        String message = "Error message";

        // When
        ApiError error = ApiError.builder(code, message).build();

        // Then
        assertThat(error).isNotNull();
        assertThat(error.getCode()).isEqualTo(code);
        assertThat(error.getMessage()).isEqualTo(message);
        assertThat(error.getTimestamp()).isNotNull();
        assertThat(error.getDetails()).isNull();
        assertThat(error.getPath()).isNull();
        assertThat(error.getRequestId()).isNull();
    }

    @Test
    @DisplayName("Should create ApiError with all fields")
    void shouldCreateApiErrorWithAllFields() {
        // Given
        String code = "ERROR_CODE";
        String message = "Error message";
        Map<String, Object> details = Map.of("field", "value");
        String path = "/api/v1/resource";
        String requestId = "req-123";
        Instant timestamp = Instant.now();

        // When
        ApiError error = ApiError.builder(code, message).details(details).path(path).requestId(requestId).timestamp(timestamp).build();

        // Then
        assertThat(error).isNotNull();
        assertThat(error.getCode()).isEqualTo(code);
        assertThat(error.getMessage()).isEqualTo(message);
        assertThat(error.getDetails()).isEqualTo(details);
        assertThat(error.getPath()).isEqualTo(path);
        assertThat(error.getRequestId()).isEqualTo(requestId);
        assertThat(error.getTimestamp()).isEqualTo(timestamp);
    }

    @Test
    @DisplayName("Should use current timestamp when not provided")
    void shouldUseCurrentTimestampWhenNotProvided() {
        // Given
        Instant before = Instant.now();

        // When
        ApiError error = ApiError.builder("ERROR_CODE", "Error message").build();

        // Then
        Instant after = Instant.now();
        assertThat(error.getTimestamp()).isNotNull();
        assertThat(error.getTimestamp()).isAfterOrEqualTo(before);
        assertThat(error.getTimestamp()).isBeforeOrEqualTo(after);
    }

    @Test
    @DisplayName("Should throw exception when code is null")
    void shouldThrowExceptionWhenCodeIsNull() {
        // When/Then
        assertThatThrownBy(() -> ApiError.builder(null, "message").build()).isInstanceOf(IllegalStateException.class).hasMessageContaining("Error code is required");
    }

    @Test
    @DisplayName("Should throw exception when code is empty")
    void shouldThrowExceptionWhenCodeIsEmpty() {
        // When/Then
        assertThatThrownBy(() -> ApiError.builder("", "message").build()).isInstanceOf(IllegalStateException.class).hasMessageContaining("Error code is required");
    }

    @Test
    @DisplayName("Should throw exception when code is blank")
    void shouldThrowExceptionWhenCodeIsBlank() {
        // When/Then
        assertThatThrownBy(() -> ApiError.builder("   ", "message").build()).isInstanceOf(IllegalStateException.class).hasMessageContaining("Error code is required");
    }

    @Test
    @DisplayName("Should throw exception when message is null")
    void shouldThrowExceptionWhenMessageIsNull() {
        // When/Then
        assertThatThrownBy(() -> ApiError.builder("ERROR_CODE", null).build()).isInstanceOf(IllegalStateException.class).hasMessageContaining("Error message is required");
    }

    @Test
    @DisplayName("Should throw exception when message is empty")
    void shouldThrowExceptionWhenMessageIsEmpty() {
        // When/Then
        assertThatThrownBy(() -> ApiError.builder("ERROR_CODE", "").build()).isInstanceOf(IllegalStateException.class).hasMessageContaining("Error message is required");
    }

    @Test
    @DisplayName("Should throw exception when message is blank")
    void shouldThrowExceptionWhenMessageIsBlank() {
        // When/Then
        assertThatThrownBy(() -> ApiError.builder("ERROR_CODE", "   ").build()).isInstanceOf(IllegalStateException.class).hasMessageContaining("Error message is required");
    }

    @Test
    @DisplayName("Should create ApiError using builder pattern")
    void shouldCreateApiErrorUsingBuilderPattern() {
        // When
        ApiError error = ApiError.builder().code("ERROR_CODE").message("Error message").build();

        // Then
        assertThat(error).isNotNull();
        assertThat(error.getCode()).isEqualTo("ERROR_CODE");
        assertThat(error.getMessage()).isEqualTo("Error message");
    }
}

