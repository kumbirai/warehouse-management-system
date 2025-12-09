package com.ccbsa.common.application.api;

import java.time.Instant;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Standardized API Error structure for error responses.
 *
 * <p>Error Response Format:</p>
 * <pre>{@code
 * {
 *   "error": {
 *     "code": "ERROR_CODE",
 *     "message": "Human-readable error message",
 *     "details": { ... },
 *     "timestamp": "2025-11-15T10:30:00Z",
 *     "path": "/api/v1/resource",
 *     "requestId": "req-123"
 *   }
 * }
 * }</pre>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class ApiError {
    private final String code;
    private final String message;
    private final Map<String, Object> details;
    private final Instant timestamp;
    private final String path;
    private final String requestId;

    private ApiError(Builder builder) {
        this.code = builder.code;
        this.message = builder.message;
        this.details = builder.details != null ? Map.copyOf(builder.details) : null;
        this.timestamp = builder.timestamp != null ? builder.timestamp : Instant.now();
        this.path = builder.path;
        this.requestId = builder.requestId;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(String code,
                                  String message) {
        return new Builder().code(code)
                .message(message);
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public Map<String, Object> getDetails() {
        return details;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getPath() {
        return path;
    }

    public String getRequestId() {
        return requestId;
    }

    /**
     * Builder for ApiError.
     */
    public static final class Builder {
        private String code;
        private String message;
        private Map<String, Object> details;
        private Instant timestamp;
        private String path;
        private String requestId;

        private Builder() {
        }

        public Builder code(String code) {
            this.code = code;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder details(Map<String, Object> details) {
            this.details = details == null ? null : Map.copyOf(details);
            return this;
        }

        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder path(String path) {
            this.path = path;
            return this;
        }

        public Builder requestId(String requestId) {
            this.requestId = requestId;
            return this;
        }

        /**
         * Builds the ApiError instance.
         *
         * @return ApiError instance
         * @throws IllegalStateException if code or message is null or empty
         */
        public ApiError build() {
            if (code == null || code.trim()
                    .isEmpty()) {
                throw new IllegalStateException("Error code is required and cannot be empty");
            }
            if (message == null || message.trim()
                    .isEmpty()) {
                throw new IllegalStateException("Error message is required and cannot be empty");
            }
            return new ApiError(this);
        }
    }
}

