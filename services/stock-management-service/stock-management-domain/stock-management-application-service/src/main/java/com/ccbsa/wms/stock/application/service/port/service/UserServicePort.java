package com.ccbsa.wms.stock.application.service.port.service;

import java.util.Optional;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.common.domain.valueobject.UserId;

/**
 * Port: UserServicePort
 * <p>
 * Interface for User Management Service integration.
 * <p>
 * This port is used for synchronous calls to User Management Service
 * to retrieve user information.
 */
public interface UserServicePort {

    /**
     * Gets user information by user ID.
     *
     * @param userId   User ID
     * @param tenantId Tenant ID
     * @return Optional UserInfo containing user details
     */
    Optional<UserInfo> getUserInfo(UserId userId, TenantId tenantId);

    /**
     * User information result object.
     */
    record UserInfo(String userId, String firstName, String lastName, String username) {
        /**
         * Gets the display name for the user.
         * Prefers "firstName lastName", falls back to username, then userId.
         *
         * @return Display name string
         */
        public String getDisplayName() {
            if ((firstName != null && !firstName.trim().isEmpty()) || (lastName != null && !lastName.trim().isEmpty())) {
                String first = firstName != null ? firstName.trim() : "";
                String last = lastName != null ? lastName.trim() : "";
                String fullName = (first + " " + last).trim();
                if (!fullName.isEmpty()) {
                    return fullName;
                }
            }
            if (username != null && !username.trim().isEmpty()) {
                return username.trim();
            }
            return userId;
        }
    }
}
