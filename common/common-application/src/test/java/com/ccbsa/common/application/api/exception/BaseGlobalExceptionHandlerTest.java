package com.ccbsa.common.application.api.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.ccbsa.common.application.api.ApiResponse;
import com.ccbsa.common.domain.exception.DomainException;
import com.ccbsa.common.domain.exception.EntityNotFoundException;
import com.ccbsa.common.domain.exception.InvalidOperationException;

import jakarta.servlet.http.HttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("BaseGlobalExceptionHandler Tests")
class BaseGlobalExceptionHandlerTest {
    private BaseGlobalExceptionHandler handler;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new BaseGlobalExceptionHandler() {
        };
        request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Request-Id")).thenReturn("test-request-id");
        when(request.getRequestURI()).thenReturn("/api/v1/test");
    }

    @Test
    @DisplayName("Should handle EntityNotFoundException")
    void shouldHandleEntityNotFoundException() {
        // Given
        EntityNotFoundException ex = new EntityNotFoundException("Resource not found");

        // When
        ResponseEntity<ApiResponse<Void>> response = handler.handleEntityNotFound(ex, request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        ApiResponse<Void> body = response.getBody();
        assertThat(body).isNotNull();
        if (body != null) {
            assertThat(body.isError()).isTrue();
            assertThat(body.getError()).isNotNull();
            if (body.getError() != null) {
                assertThat(body.getError()
                        .getCode()).isEqualTo("RESOURCE_NOT_FOUND");
                assertThat(body.getError()
                        .getMessage()).isEqualTo("Resource not found");
            }
        }
    }

    @Test
    @DisplayName("Should handle InvalidOperationException")
    void shouldHandleInvalidOperationException() {
        // Given
        InvalidOperationException ex = new InvalidOperationException("Cannot perform operation");

        // When
        ResponseEntity<ApiResponse<Void>> response = handler.handleInvalidOperation(ex, request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ApiResponse<Void> body = response.getBody();
        assertThat(body).isNotNull();
        if (body != null && body.getError() != null) {
            assertThat(body.getError()
                    .getCode()).isEqualTo("INVALID_OPERATION");
        }
    }

    @Test
    @DisplayName("Should handle DomainException")
    void shouldHandleDomainException() {
        // Given
        DomainException ex = new DomainException("Domain error") {
        };

        // When
        ResponseEntity<ApiResponse<Void>> response = handler.handleDomainException(ex, request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ApiResponse<Void> body = response.getBody();
        assertThat(body).isNotNull();
        if (body != null && body.getError() != null) {
            assertThat(body.getError()
                    .getCode()).isEqualTo("DOMAIN_ERROR");
        }
    }

    @Test
    @DisplayName("Should handle IllegalArgumentException")
    void shouldHandleIllegalArgumentException() {
        // Given
        IllegalArgumentException ex = new IllegalArgumentException("Invalid argument");

        // When
        ResponseEntity<ApiResponse<Void>> response = handler.handleIllegalArgument(ex, request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ApiResponse<Void> body = response.getBody();
        assertThat(body).isNotNull();
        if (body != null && body.getError() != null) {
            assertThat(body.getError()
                    .getCode()).isEqualTo("VALIDATION_ERROR");
        }
    }

    @Test
    @DisplayName("Should handle IllegalStateException")
    void shouldHandleIllegalStateException() {
        // Given
        IllegalStateException ex = new IllegalStateException("Invalid state");

        // When
        ResponseEntity<ApiResponse<Void>> response = handler.handleIllegalState(ex, request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ApiResponse<Void> body = response.getBody();
        assertThat(body).isNotNull();
        if (body != null && body.getError() != null) {
            assertThat(body.getError()
                    .getCode()).isEqualTo("INVALID_OPERATION");
        }
    }

    @Test
    @DisplayName("Should handle generic Exception")
    void shouldHandleGenericException() {
        // Given
        Exception ex = new Exception("Unexpected error");

        // When
        ResponseEntity<ApiResponse<Void>> response = handler.handleGeneric(ex, request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        ApiResponse<Void> body = response.getBody();
        assertThat(body).isNotNull();
        if (body != null && body.getError() != null) {
            assertThat(body.getError()
                    .getCode()).isEqualTo("INTERNAL_SERVER_ERROR");
        }
    }

    @Test
    @DisplayName("Should include request context in error responses")
    void shouldIncludeRequestContextInErrorResponses() {
        // Given
        EntityNotFoundException ex = new EntityNotFoundException("Resource not found");
        when(request.getHeader("X-Request-Id")).thenReturn("custom-request-id");
        when(request.getRequestURI()).thenReturn("/api/v1/custom");

        // When
        ResponseEntity<ApiResponse<Void>> response = handler.handleEntityNotFound(ex, request);

        // Then
        ApiResponse<Void> body = response.getBody();
        assertThat(body).isNotNull();
        if (body != null && body.getError() != null) {
            assertThat(body.getError()
                    .getRequestId()).isEqualTo("custom-request-id");
            assertThat(body.getError()
                    .getPath()).isEqualTo("/api/v1/custom");
        }
    }
}

