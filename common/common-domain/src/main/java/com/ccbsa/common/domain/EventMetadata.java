package com.ccbsa.common.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Immutable value object representing metadata for domain events. Contains traceability information for distributed tracing and event correlation.
 *
 * <p>Event metadata includes:
 * <ul>
 *   <li>correlationId - Tracks entire business flow across services</li>
 *   <li>causationId - Tracks immediate cause of event (parent event ID)</li>
 *   <li>userId - User identifier who triggered the event</li>
 * </ul>
 * </p>
 *
 * <p>Usage Example:</p>
 * <pre>{@code
 * EventMetadata metadata = EventMetadata.builder()
 *     .correlationId("req-123")
 *     .causationId(UUID.fromString("evt-001"))
 *     .userId("user-456")
 *     .build();
 * }</pre>
 */
public final class EventMetadata {
    private final String correlationId;
    private final UUID causationId;
    private final String userId;

    private EventMetadata(Builder builder) {
        this.correlationId = builder.correlationId;
        this.causationId = builder.causationId;
        this.userId = builder.userId;
    }

    /**
     * Creates a new builder for EventMetadata.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Gets the correlation ID. Correlation ID tracks the entire business flow across services and operations.
     *
     * @return the correlation ID, or null if not set
     */
    public String getCorrelationId() {
        return correlationId;
    }

    /**
     * Gets the causation ID. Causation ID is the event ID of the event that caused this event.
     *
     * @return the causation ID, or null if not set
     */
    public UUID getCausationId() {
        return causationId;
    }

    /**
     * Gets the user ID. User ID identifies the user who triggered the event.
     *
     * @return the user ID, or null if not set
     */
    public String getUserId() {
        return userId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        EventMetadata that = (EventMetadata) o;
        return Objects.equals(correlationId, that.correlationId) && Objects.equals(causationId, that.causationId) && Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(correlationId, causationId, userId);
    }

    @Override
    public String toString() {
        return String.format("EventMetadata{correlationId='%s', causationId=%s, userId='%s'}", correlationId, causationId, userId);
    }

    /**
     * Builder for EventMetadata.
     */
    public static final class Builder {
        private String correlationId;
        private UUID causationId;
        private String userId;

        private Builder() {
            // Builder constructor
        }

        /**
         * Sets the correlation ID.
         *
         * @param correlationId the correlation ID (typically a UUID string)
         * @return this builder
         */
        public Builder correlationId(String correlationId) {
            if (correlationId != null && correlationId.trim()
                    .isEmpty()) {
                throw new IllegalArgumentException("Correlation ID cannot be empty");
            }
            this.correlationId = correlationId != null ? correlationId.trim() : null;
            return this;
        }

        /**
         * Sets the causation ID.
         *
         * @param causationId the causation ID (parent event ID)
         * @return this builder
         */
        public Builder causationId(UUID causationId) {
            this.causationId = causationId;
            return this;
        }

        /**
         * Sets the user ID.
         *
         * @param userId the user ID
         * @return this builder
         */
        public Builder userId(String userId) {
            if (userId != null && userId.trim()
                    .isEmpty()) {
                throw new IllegalArgumentException("User ID cannot be empty");
            }
            this.userId = userId != null ? userId.trim() : null;
            return this;
        }

        /**
         * Builds the EventMetadata instance.
         *
         * @return the EventMetadata instance
         */
        public EventMetadata build() {
            return new EventMetadata(this);
        }
    }
}

