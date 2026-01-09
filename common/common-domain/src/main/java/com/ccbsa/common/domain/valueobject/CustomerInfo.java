package com.ccbsa.common.domain.valueobject;

import java.util.Objects;

/**
 * Value Object: CustomerInfo
 * <p>
 * Represents customer information including customer code and name. Immutable and self-validating.
 * <p>
 * This value object is shared across services (DRY principle).
 * <p>
 * Business Rules:
 * - Customer code cannot be null or empty
 * - Customer code cannot exceed 50 characters
 * - Customer name is optional but cannot exceed 200 characters if provided
 */
public final class CustomerInfo {
    private final String customerCode;
    private final String customerName;

    private CustomerInfo(String customerCode, String customerName) {
        if (customerCode == null || customerCode.isBlank()) {
            throw new IllegalArgumentException("Customer code cannot be null or empty");
        }
        if (customerCode.length() > 50) {
            throw new IllegalArgumentException("Customer code cannot exceed 50 characters");
        }
        if (customerName != null && customerName.length() > 200) {
            throw new IllegalArgumentException("Customer name cannot exceed 200 characters");
        }
        this.customerCode = customerCode;
        this.customerName = customerName;
    }

    /**
     * Factory method to create CustomerInfo with customer code and optional name.
     *
     * @param customerCode Customer code (required, max 50 chars)
     * @param customerName Customer name (optional, max 200 chars)
     * @return CustomerInfo instance
     * @throws IllegalArgumentException if customer code is null, empty, or invalid
     */
    public static CustomerInfo of(String customerCode, String customerName) {
        return new CustomerInfo(customerCode, customerName);
    }

    /**
     * Factory method to create CustomerInfo with customer code only.
     *
     * @param customerCode Customer code (required, max 50 chars)
     * @return CustomerInfo instance
     * @throws IllegalArgumentException if customer code is null, empty, or invalid
     */
    public static CustomerInfo of(String customerCode) {
        return new CustomerInfo(customerCode, null);
    }

    /**
     * Returns the customer code.
     *
     * @return Customer code
     */
    public String getCustomerCode() {
        return customerCode;
    }

    /**
     * Returns the customer name.
     *
     * @return Customer name (may be null)
     */
    public String getCustomerName() {
        return customerName;
    }

    /**
     * Checks if customer name is present.
     *
     * @return true if customer name is not null and not blank
     */
    public boolean hasCustomerName() {
        return customerName != null && !customerName.isBlank();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CustomerInfo that = (CustomerInfo) o;
        return Objects.equals(customerCode, that.customerCode) && Objects.equals(customerName, that.customerName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(customerCode, customerName);
    }

    @Override
    public String toString() {
        if (customerName != null && !customerName.isBlank()) {
            return String.format("CustomerInfo{customerCode='%s', customerName='%s'}", customerCode, customerName);
        }
        return String.format("CustomerInfo{customerCode='%s'}", customerCode);
    }
}
