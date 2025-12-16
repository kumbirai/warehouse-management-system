package com.ccbsa.wms.locationmanagement.config;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.ccbsa.common.application.context.CorrelationContext;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Request logging interceptor.
 * Adds correlation ID and logs request/response details.
 */
@Component
public class RequestLoggingInterceptor implements HandlerInterceptor {
    private static final Logger logger = LoggerFactory.getLogger(RequestLoggingInterceptor.class);
    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    private static final String CORRELATION_ID_MDC = "correlationId";

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
                             @NonNull HttpServletResponse response,
                             @NonNull Object handler) {
        // Generate or extract correlation ID
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.trim().isEmpty()) {
            correlationId = UUID.randomUUID().toString();
            logger.debug("Generated new correlation ID: {}", correlationId);
        } else {
            correlationId = correlationId.trim();
            logger.debug("Using existing correlation ID: {}", correlationId);
        }

        // Set in CorrelationContext for event publishing
        CorrelationContext.setCorrelationId(correlationId);

        // Add to MDC for logging
        MDC.put(CORRELATION_ID_MDC, correlationId);

        // Add to response header
        response.setHeader(CORRELATION_ID_HEADER, correlationId);

        // Add API version header
        response.setHeader("X-API-Version", "1.0");

        // Add request ID header (same as correlation ID for tracking)
        response.setHeader("X-Request-ID", correlationId);

        // Log request (excluding sensitive endpoints)
        if (!isSensitiveEndpoint(request.getRequestURI())) {
            logger.debug("Incoming request: {} {} from {} [{}]",
                    request.getMethod(),
                    request.getRequestURI(),
                    request.getRemoteAddr(),
                    correlationId);
        }

        return true;
    }

    private boolean isSensitiveEndpoint(String uri) {
        // Don't log sensitive authentication endpoints
        return uri != null && (uri.contains("/bff/auth/login") || uri.contains("/bff/auth/refresh"));
    }

    @Override
    public void afterCompletion(@NonNull HttpServletRequest request,
                                @NonNull HttpServletResponse response,
                                @NonNull Object handler,
                                @Nullable Exception ex) {
        // Log response (excluding sensitive endpoints)
        if (!isSensitiveEndpoint(request.getRequestURI())) {
            logger.debug("Completed request: {} {} - Status: {}",
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus());
        }

        // Clear MDC
        MDC.clear();

        // Clear CorrelationContext
        CorrelationContext.clear();
    }
}

