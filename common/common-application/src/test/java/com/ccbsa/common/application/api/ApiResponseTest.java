package com.ccbsa.common.application.api;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ApiResponse Tests")
class ApiResponseTest {
    @Test
    @DisplayName("Should create success response with data")
    void shouldCreateSuccessResponseWithData() {
        // Given
        String testData = "test-data";

        // When
        ApiResponse<String> response = ApiResponse.success(testData);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.isError()).isFalse();
        assertThat(response.getData()).isEqualTo(testData);
        assertThat(response.getError()).isNull();
        assertThat(response.getLinks()).isNull();
        assertThat(response.getMeta()).isNull();
    }

    @Test
    @DisplayName("Should create success response with null data")
    void shouldCreateSuccessResponseWithNullData() {
        // When
        ApiResponse<String> response = ApiResponse.success(null);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isNull();
    }

    @Test
    @DisplayName("Should create success response with data and links")
    void shouldCreateSuccessResponseWithDataAndLinks() {
        // Given
        String testData = "test-data";
        Map<String, String> links = Map.of("self", "/api/v1/resource", "next", "/api/v1/resource/next");

        // When
        ApiResponse<String> response = ApiResponse.success(testData, links);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isEqualTo(testData);
        assertThat(response.getLinks()).isEqualTo(links);
    }

    @Test
    @DisplayName("Should create success response with data, links, and meta")
    void shouldCreateSuccessResponseWithDataLinksAndMeta() {
        // Given
        String testData = "test-data";
        Map<String, String> links = Map.of("self", "/api/v1/resource");
        ApiMeta.Pagination pagination = ApiMeta.Pagination.of(1, 20, 100);
        ApiMeta meta = ApiMeta.builder().pagination(pagination).build();

        // When
        ApiResponse<String> response = ApiResponse.success(testData, links, meta);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isEqualTo(testData);
        assertThat(response.getLinks()).isEqualTo(links);
        assertThat(response.getMeta()).isEqualTo(meta);
    }

    @Test
    @DisplayName("Should create error response")
    void shouldCreateErrorResponse() {
        // Given
        ApiError error = ApiError.builder("ERROR_CODE", "Error message").build();

        // When
        ApiResponse<String> response = ApiResponse.error(error);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.isError()).isTrue();
        assertThat(response.isSuccess()).isFalse(); // isSuccess returns false when error is present
        assertThat(response.getError()).isEqualTo(error);
        assertThat(response.getData()).isNull();
    }

    @Test
    @DisplayName("Should throw NullPointerException when creating error response with null error")
    void shouldThrowExceptionWhenCreatingErrorResponseWithNullError() {
        // When/Then
        assertThatThrownBy(() -> ApiResponse.error(null)).isInstanceOf(NullPointerException.class).hasMessage("Error cannot be null");
    }

    @Test
    @DisplayName("Should create no-content response")
    void shouldCreateNoContentResponse() {
        // When
        ApiResponse<String> response = ApiResponse.noContent();

        // Then
        assertThat(response).isNotNull();
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isNull();
        assertThat(response.getError()).isNull();
    }

    @Test
    @DisplayName("Should correctly identify error response")
    void shouldCorrectlyIdentifyErrorResponse() {
        // Given
        ApiError error = ApiError.builder("ERROR_CODE", "Error message").build();
        ApiResponse<String> errorResponse = ApiResponse.error(error);

        // When/Then
        assertThat(errorResponse.isError()).isTrue();
        assertThat(errorResponse.isSuccess()).isFalse(); // isSuccess returns false when error is present
    }

    @Test
    @DisplayName("Should correctly identify success response")
    void shouldCorrectlyIdentifySuccessResponse() {
        // Given
        ApiResponse<String> successResponse = ApiResponse.success("data");

        // When/Then
        assertThat(successResponse.isSuccess()).isTrue();
        assertThat(successResponse.isError()).isFalse();
    }
}

