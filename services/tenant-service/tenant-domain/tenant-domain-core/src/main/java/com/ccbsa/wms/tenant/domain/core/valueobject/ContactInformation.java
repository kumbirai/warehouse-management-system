package com.ccbsa.wms.tenant.domain.core.valueobject;

import java.util.Objects;
import java.util.Optional;

import com.ccbsa.common.domain.valueobject.EmailAddress;

/**
 * Value Object: ContactInformation
 * <p>
 * Represents contact information for a tenant. Immutable and validated on construction.
 */
public final class ContactInformation {
    private final EmailAddress emailAddress;
    private final String phone;
    private final String address;

    private ContactInformation(EmailAddress emailAddress, String phone, String address) {
        this.emailAddress = emailAddress;

        // Phone validation
        if (phone != null && !phone.trim()
                .isEmpty()) {
            if (phone.length() > 50) {
                throw new IllegalArgumentException("Phone number cannot exceed 50 characters");
            }
            this.phone = phone.trim();
        } else {
            this.phone = null;
        }

        // Address validation
        if (address != null && !address.trim()
                .isEmpty()) {
            if (address.length() > 500) {
                throw new IllegalArgumentException("Address cannot exceed 500 characters");
            }
            this.address = address.trim();
        } else {
            this.address = null;
        }
    }

    /**
     * Creates ContactInformation from emailAddress, phone, and address.
     *
     * @param emailAddress EmailAddress value object (can be null)
     * @param phone        Phone number string (can be null)
     * @param address      Address string (can be null)
     * @return ContactInformation instance
     */
    public static ContactInformation of(EmailAddress emailAddress, String phone, String address) {
        return new ContactInformation(emailAddress, phone, address);
    }

    /**
     * Creates ContactInformation from string values. EmailAddress string will be converted to EmailAddress value object.
     *
     * @param email   EmailAddress string (can be null or empty)
     * @param phone   Phone number string (can be null)
     * @param address Address string (can be null)
     * @return ContactInformation instance
     */
    public static ContactInformation of(String email, String phone, String address) {
        EmailAddress emailAddressValueObject = EmailAddress.ofNullable(email);
        return new ContactInformation(emailAddressValueObject, phone, address);
    }

    /**
     * Creates ContactInformation with emailAddress only.
     *
     * @param emailAddress EmailAddress value object (can be null)
     * @return ContactInformation instance
     */
    public static ContactInformation emailOnly(EmailAddress emailAddress) {
        return new ContactInformation(emailAddress, null, null);
    }

    /**
     * Creates ContactInformation with emailAddress only from string.
     *
     * @param email EmailAddress string (can be null or empty)
     * @return ContactInformation instance
     */
    public static ContactInformation emailOnly(String email) {
        EmailAddress emailAddressValueObject = EmailAddress.ofNullable(email);
        return new ContactInformation(emailAddressValueObject, null, null);
    }

    public Optional<EmailAddress> getEmail() {
        return Optional.ofNullable(emailAddress);
    }

    /**
     * Gets emailAddress as string value.
     *
     * @return EmailAddress string value or null if emailAddress is not set
     */
    public Optional<String> getEmailValue() {
        return emailAddress != null ? Optional.of(emailAddress.getValue()) : Optional.empty();
    }

    public Optional<String> getPhone() {
        return Optional.ofNullable(phone);
    }

    public Optional<String> getAddress() {
        return Optional.ofNullable(address);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ContactInformation that = (ContactInformation) o;
        return Objects.equals(emailAddress, that.emailAddress) && Objects.equals(phone, that.phone) && Objects.equals(address, that.address);
    }

    @Override
    public int hashCode() {
        return Objects.hash(emailAddress, phone, address);
    }

    @Override
    public String toString() {
        return String.format("ContactInformation{emailAddress='%s', phone='%s', address='%s'}", emailAddress, phone, address);
    }
}

