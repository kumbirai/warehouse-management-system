package com.ccbsa.wms.returns.domain.core.valueobject;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Value Object: InsuranceClaimInfo
 * <p>
 * Represents insurance claim information for damaged products. Immutable and self-validating.
 * <p>
 * Business Rules:
 * - Claim number cannot be null or empty
 * - Insurance company cannot be null or empty
 * - Claim amount must be positive if provided
 */
public final class InsuranceClaimInfo {
    private final String claimNumber;
    private final String insuranceCompany;
    private final String claimStatus;
    private final BigDecimal claimAmount;

    private InsuranceClaimInfo(String claimNumber, String insuranceCompany, String claimStatus, BigDecimal claimAmount) {
        if (claimNumber == null || claimNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Claim number cannot be null or empty");
        }
        if (insuranceCompany == null || insuranceCompany.trim().isEmpty()) {
            throw new IllegalArgumentException("Insurance company cannot be null or empty");
        }
        if (claimNumber.length() > 100) {
            throw new IllegalArgumentException("Claim number cannot exceed 100 characters");
        }
        if (insuranceCompany.length() > 200) {
            throw new IllegalArgumentException("Insurance company cannot exceed 200 characters");
        }
        if (claimStatus != null && claimStatus.length() > 50) {
            throw new IllegalArgumentException("Claim status cannot exceed 50 characters");
        }
        if (claimAmount != null && claimAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Claim amount cannot be negative");
        }
        this.claimNumber = claimNumber.trim();
        this.insuranceCompany = insuranceCompany.trim();
        this.claimStatus = claimStatus != null ? claimStatus.trim() : null;
        this.claimAmount = claimAmount;
    }

    /**
     * Factory method to create InsuranceClaimInfo.
     *
     * @param claimNumber      Claim number (required, max 100 chars)
     * @param insuranceCompany Insurance company name (required, max 200 chars)
     * @param claimStatus      Claim status (optional, max 50 chars)
     * @param claimAmount      Claim amount (optional, must be positive if provided)
     * @return InsuranceClaimInfo instance
     * @throws IllegalArgumentException if validation fails
     */
    public static InsuranceClaimInfo of(String claimNumber, String insuranceCompany, String claimStatus, BigDecimal claimAmount) {
        return new InsuranceClaimInfo(claimNumber, insuranceCompany, claimStatus, claimAmount);
    }

    /**
     * Factory method to create InsuranceClaimInfo without optional fields.
     *
     * @param claimNumber      Claim number (required)
     * @param insuranceCompany Insurance company name (required)
     * @return InsuranceClaimInfo instance
     * @throws IllegalArgumentException if validation fails
     */
    public static InsuranceClaimInfo of(String claimNumber, String insuranceCompany) {
        return new InsuranceClaimInfo(claimNumber, insuranceCompany, null, null);
    }

    /**
     * Returns the claim number.
     *
     * @return Claim number
     */
    public String getClaimNumber() {
        return claimNumber;
    }

    /**
     * Returns the insurance company name.
     *
     * @return Insurance company
     */
    public String getInsuranceCompany() {
        return insuranceCompany;
    }

    /**
     * Returns the claim status.
     *
     * @return Claim status (may be null)
     */
    public String getClaimStatus() {
        return claimStatus;
    }

    /**
     * Returns the claim amount.
     *
     * @return Claim amount (may be null)
     */
    public BigDecimal getClaimAmount() {
        return claimAmount;
    }

    /**
     * Checks if claim status is present.
     *
     * @return true if claim status is not null and not blank
     */
    public boolean hasClaimStatus() {
        return claimStatus != null && !claimStatus.isBlank();
    }

    /**
     * Checks if claim amount is present.
     *
     * @return true if claim amount is not null
     */
    public boolean hasClaimAmount() {
        return claimAmount != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        InsuranceClaimInfo that = (InsuranceClaimInfo) o;
        return Objects.equals(claimNumber, that.claimNumber) && Objects.equals(insuranceCompany, that.insuranceCompany) && Objects.equals(claimStatus, that.claimStatus)
                && Objects.equals(claimAmount, that.claimAmount);
    }

    @Override
    public int hashCode() {
        return Objects.hash(claimNumber, insuranceCompany, claimStatus, claimAmount);
    }

    @Override
    public String toString() {
        return String.format("InsuranceClaimInfo{claimNumber='%s', insuranceCompany='%s', claimStatus='%s', claimAmount=%s}", claimNumber, insuranceCompany, claimStatus,
                claimAmount);
    }
}
