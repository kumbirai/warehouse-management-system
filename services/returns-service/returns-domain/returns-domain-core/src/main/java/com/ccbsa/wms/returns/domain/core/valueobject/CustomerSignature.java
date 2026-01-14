package com.ccbsa.wms.returns.domain.core.valueobject;

import java.time.Instant;
import java.util.Objects;

/**
 * Value Object: CustomerSignature
 * <p>
 * Represents a customer signature for return authorization. Immutable and self-validating.
 * <p>
 * Business Rules:
 * - Signature data (base64 encoded image) cannot be null or empty
 * - Timestamp cannot be null
 * - Signature data cannot exceed 1MB (approximately 1,048,576 characters in base64)
 */
public final class CustomerSignature {
    private static final int MAX_SIGNATURE_SIZE = 1_048_576; // 1MB in characters

    private final String signatureData; // Base64 encoded image
    private final Instant timestamp;

    private CustomerSignature(String signatureData, Instant timestamp) {
        if (signatureData == null || signatureData.trim().isEmpty()) {
            throw new IllegalArgumentException("Signature data cannot be null or empty");
        }
        if (signatureData.length() > MAX_SIGNATURE_SIZE) {
            throw new IllegalArgumentException(String.format("Signature data cannot exceed %d characters", MAX_SIGNATURE_SIZE));
        }
        if (timestamp == null) {
            throw new IllegalArgumentException("Timestamp cannot be null");
        }
        this.signatureData = signatureData;
        this.timestamp = timestamp;
    }

    /**
     * Factory method to create CustomerSignature.
     *
     * @param signatureData Base64 encoded signature image (required, max 1MB)
     * @param timestamp     Timestamp when signature was captured (required)
     * @return CustomerSignature instance
     * @throws IllegalArgumentException if validation fails
     */
    public static CustomerSignature of(String signatureData, Instant timestamp) {
        return new CustomerSignature(signatureData, timestamp);
    }

    /**
     * Factory method to create CustomerSignature with current timestamp.
     *
     * @param signatureData Base64 encoded signature image (required, max 1MB)
     * @return CustomerSignature instance
     * @throws IllegalArgumentException if validation fails
     */
    public static CustomerSignature of(String signatureData) {
        return new CustomerSignature(signatureData, Instant.now());
    }

    /**
     * Returns the signature data (base64 encoded image).
     *
     * @return Signature data
     */
    public String getSignatureData() {
        return signatureData;
    }

    /**
     * Returns the timestamp when signature was captured.
     *
     * @return Timestamp
     */
    public Instant getTimestamp() {
        return timestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CustomerSignature that = (CustomerSignature) o;
        return Objects.equals(signatureData, that.signatureData) && Objects.equals(timestamp, that.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(signatureData, timestamp);
    }

    @Override
    public String toString() {
        return String.format("CustomerSignature{timestamp=%s, dataLength=%d}", timestamp, signatureData.length());
    }
}
