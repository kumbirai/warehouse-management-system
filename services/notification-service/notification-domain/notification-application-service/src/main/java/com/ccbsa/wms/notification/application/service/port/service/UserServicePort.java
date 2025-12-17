package com.ccbsa.wms.notification.application.service.port.service;

import com.ccbsa.common.domain.valueobject.EmailAddress;
import com.ccbsa.common.domain.valueobject.UserId;

/**
 * Port: UserServicePort
 * <p>
 * Defines the contract for retrieving user information from user-service. Implemented by service adapters in infrastructure layers.
 */
public interface UserServicePort {

    /**
     * Gets the email address for a user.
     *
     * @param userId User identifier
     * @return EmailAddress address of the user
     * @throws RuntimeException if user not found or email retrieval fails
     */
    EmailAddress getUserEmail(UserId userId);
}

