package com.ccbsa.wms.notification.application.service.port.service;

import java.util.Optional;

import com.ccbsa.common.domain.valueobject.EmailAddress;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.common.domain.valueobject.UserId;

/**
 * Port: UserServicePort
 * <p>
 * Defines the contract for retrieving user information from user-service. Implemented by service adapters in infrastructure layers.
 */
public interface UserServicePort {

    /**
     * Gets the email address for a user.
     * <p>
     * Email is optional - some users may not have an email address set. Returns empty Optional if email is not available.
     *
     * @param userId   User identifier
     * @param tenantId Tenant identifier (required for X-Tenant-Id header in service-to-service calls)
     * @return Optional containing EmailAddress if available, empty if user has no email or user not found
     * @throws RuntimeException if user service call fails (network errors, etc.)
     */
    Optional<EmailAddress> getUserEmail(UserId userId, TenantId tenantId);
}

