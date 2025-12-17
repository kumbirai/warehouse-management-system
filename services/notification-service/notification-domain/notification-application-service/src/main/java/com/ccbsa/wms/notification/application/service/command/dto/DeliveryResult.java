package com.ccbsa.wms.notification.application.service.command.dto;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * DTO: DeliveryResult
 * <p>
 * Represents the result of a notification delivery attempt. Immutable DTO with factory methods for success and failure cases.
 */
public final class DeliveryResult {
    private final boolean success;
    private final String externalId;
    private final String errorMessage;
    private final LocalDateTime sentAt;

    private DeliveryResult(boolean success, String externalId, String errorMessage, LocalDateTime sentAt) {
        this.success = success;
        this.externalId = externalId;
        this.errorMessage = errorMessage;
        this.sentAt = sentAt;
    }

    /**
     * Creates a successful delivery result.
     *
     * @param externalId External identifier (e.g., email message ID)
     * @return DeliveryResult with success=true
     */
    public static DeliveryResult success(String externalId) {
        return new DeliveryResult(true, externalId, null, LocalDateTime.now());
    }

    /**
     * Creates a failed delivery result.
     *
     * @param errorMessage Error message describing the failure
     * @return DeliveryResult with success=false
     */
    public static DeliveryResult failure(String errorMessage) {
        return new DeliveryResult(false, null, errorMessage, LocalDateTime.now());
    }

    public boolean isSuccess() {
        return success;
    }

    public String getExternalId() {
        return externalId;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DeliveryResult that = (DeliveryResult) o;
        return success == that.success && Objects.equals(externalId, that.externalId) && Objects.equals(errorMessage, that.errorMessage) && Objects.equals(sentAt, that.sentAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(success, externalId, errorMessage, sentAt);
    }

    @Override
    public String toString() {
        return String.format("DeliveryResult{success=%s, externalId='%s', errorMessage='%s', sentAt=%s}", success, externalId, errorMessage, sentAt);
    }
}

