package com.ccbsa.common.application.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import jakarta.servlet.http.HttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("RequestContext Tests")
class RequestContextTest {
    @Test
    @DisplayName("Should extract request ID from X-Request-Id header")
    void shouldExtractRequestIdFromRequestIdHeader() {
        // Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        String expectedRequestId = "req-123";
        when(request.getHeader(RequestContext.REQUEST_ID_HEADER)).thenReturn(expectedRequestId);

        // When
        String requestId = RequestContext.getRequestId(request);

        // Then
        assertThat(requestId).isEqualTo(expectedRequestId);
    }

    @Test
    @DisplayName("Should extract request ID from X-Correlation-Id header when X-Request-Id is missing")
    void shouldExtractRequestIdFromCorrelationIdHeader() {
        // Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        String expectedRequestId = "corr-456";
        when(request.getHeader(RequestContext.REQUEST_ID_HEADER)).thenReturn(null);
        when(request.getHeader(RequestContext.CORRELATION_ID_HEADER)).thenReturn(expectedRequestId);

        // When
        String requestId = RequestContext.getRequestId(request);

        // Then
        assertThat(requestId).isEqualTo(expectedRequestId);
    }

    @Test
    @DisplayName("Should generate UUID when no request ID header is present")
    void shouldGenerateUuidWhenNoRequestIdHeaderIsPresent() {
        // Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader(RequestContext.REQUEST_ID_HEADER)).thenReturn(null);
        when(request.getHeader(RequestContext.CORRELATION_ID_HEADER)).thenReturn(null);

        // When
        String requestId = RequestContext.getRequestId(request);

        // Then
        assertThat(requestId).isNotNull();
        assertThat(requestId).isNotEmpty();
        // UUID format: 8-4-4-4-12 hex digits
        assertThat(requestId).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    }

    @Test
    @DisplayName("Should generate UUID when request is null")
    void shouldGenerateUuidWhenRequestIsNull() {
        // When
        String requestId = RequestContext.getRequestId(null);

        // Then
        assertThat(requestId).isNotNull();
        assertThat(requestId).isNotEmpty();
        assertThat(requestId).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    }

    @Test
    @DisplayName("Should trim whitespace from request ID header")
    void shouldTrimWhitespaceFromRequestIdHeader() {
        // Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader(RequestContext.REQUEST_ID_HEADER)).thenReturn("  req-123  ");

        // When
        String requestId = RequestContext.getRequestId(request);

        // Then
        assertThat(requestId).isEqualTo("req-123");
    }

    @Test
    @DisplayName("Should generate UUID when request ID header is empty string")
    void shouldGenerateUuidWhenRequestIdHeaderIsEmpty() {
        // Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader(RequestContext.REQUEST_ID_HEADER)).thenReturn("");
        when(request.getHeader(RequestContext.CORRELATION_ID_HEADER)).thenReturn(null);

        // When
        String requestId = RequestContext.getRequestId(request);

        // Then
        assertThat(requestId).isNotNull();
        assertThat(requestId).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    }

    @Test
    @DisplayName("Should extract request path")
    void shouldExtractRequestPath() {
        // Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        String expectedPath = "/api/v1/tenants/123";
        when(request.getRequestURI()).thenReturn(expectedPath);

        // When
        String path = RequestContext.getRequestPath(request);

        // Then
        assertThat(path).isEqualTo(expectedPath);
    }

    @Test
    @DisplayName("Should return null path when request is null")
    void shouldReturnNullPathWhenRequestIsNull() {
        // When
        String path = RequestContext.getRequestPath(null);

        // Then
        assertThat(path).isNull();
    }

    @Test
    @DisplayName("Should extract request method")
    void shouldExtractRequestMethod() {
        // Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        String expectedMethod = "POST";
        when(request.getMethod()).thenReturn(expectedMethod);

        // When
        String method = RequestContext.getRequestMethod(request);

        // Then
        assertThat(method).isEqualTo(expectedMethod);
    }

    @Test
    @DisplayName("Should return null method when request is null")
    void shouldReturnNullMethodWhenRequestIsNull() {
        // When
        String method = RequestContext.getRequestMethod(null);

        // Then
        assertThat(method).isNull();
    }

    @Test
    @DisplayName("Should extract query string")
    void shouldExtractQueryString() {
        // Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        String expectedQuery = "page=1&size=20";
        when(request.getQueryString()).thenReturn(expectedQuery);

        // When
        String queryString = RequestContext.getQueryString(request);

        // Then
        assertThat(queryString).isEqualTo(expectedQuery);
    }

    @Test
    @DisplayName("Should return null query string when request is null")
    void shouldReturnNullQueryStringWhenRequestIsNull() {
        // When
        String queryString = RequestContext.getQueryString(null);

        // Then
        assertThat(queryString).isNull();
    }

    @Test
    @DisplayName("Should get full request URL with query string")
    void shouldGetFullRequestUrlWithQueryString() {
        // Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        StringBuffer url = new StringBuffer("https://api.example.com/api/v1/tenants");
        String queryString = "page=1&size=20";
        when(request.getRequestURL()).thenReturn(url);
        when(request.getQueryString()).thenReturn(queryString);

        // When
        String fullUrl = RequestContext.getFullRequestUrl(request);

        // Then
        assertThat(fullUrl).isEqualTo("https://api.example.com/api/v1/tenants?page=1&size=20");
    }

    @Test
    @DisplayName("Should get full request URL without query string")
    void shouldGetFullRequestUrlWithoutQueryString() {
        // Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        StringBuffer url = new StringBuffer("https://api.example.com/api/v1/tenants");
        when(request.getRequestURL()).thenReturn(url);
        when(request.getQueryString()).thenReturn(null);

        // When
        String fullUrl = RequestContext.getFullRequestUrl(request);

        // Then
        assertThat(fullUrl).isEqualTo("https://api.example.com/api/v1/tenants");
    }

    @Test
    @DisplayName("Should return null full URL when request is null")
    void shouldReturnNullFullUrlWhenRequestIsNull() {
        // When
        String fullUrl = RequestContext.getFullRequestUrl(null);

        // Then
        assertThat(fullUrl).isNull();
    }
}

