package com.ccbsa.common.application.context;

import java.util.UUID;

/**
 * Thread-local correlation context holder. Stores correlation ID for the current request thread to enable distributed tracing.
 *
 * <p>Correlation ID is used to track requests across service boundaries and through
 * event-driven flows. It is typically set at the entry point (API Gateway or first service handling request) and propagated through all downstream operations.</p>
 *
 * <p>Usage Example:</p>
 * <pre>{@code
 * // Set correlation ID at request entry point
 * CorrelationContext.setCorrelationId("req-123");
 *
 * // Access correlation ID in event publishers
 * String correlationId = CorrelationContext.getCorrelationId();
 *
 * // Clear after request completion
 * CorrelationContext.clear();
 * }</pre>
 */
public final class CorrelationContext {
    private static final ThreadLocal<String> CORRELATION_ID = new ThreadLocal<>();

    private CorrelationContext() {
        // Utility class - prevent instantiation
    }

    /**
     * Gets the correlation ID for the current thread.
     *
     * @return the correlation ID, or null if not set
     */
    public static String getCorrelationId() {
        return CORRELATION_ID.get();
    }

    /**
     * Sets the correlation ID for the current thread.
     *
     * @param correlationId the correlation ID (typically a UUID string)
     * @throws IllegalArgumentException if correlationId is null or empty
     */
    public static void setCorrelationId(String correlationId) {
        if (correlationId == null || correlationId.trim().isEmpty()) {
            throw new IllegalArgumentException("Correlation ID cannot be null or empty");
        }
        CORRELATION_ID.set(correlationId.trim());
    }

    /**
     * Generates and sets a new correlation ID for the current thread.
     *
     * @return the generated correlation ID
     */
    public static String generateAndSetCorrelationId() {
        String correlationId = UUID.randomUUID().toString();
        CORRELATION_ID.set(correlationId);
        return correlationId;
    }

    /**
     * Clears the correlation context for the current thread. Should be called after request processing to prevent memory leaks.
     */
    public static void clear() {
        CORRELATION_ID.remove();
    }
}

