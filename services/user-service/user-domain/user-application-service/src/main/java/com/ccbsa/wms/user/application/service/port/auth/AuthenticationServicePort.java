package com.ccbsa.wms.user.application.service.port.auth;

import java.util.List;

import com.ccbsa.wms.user.application.service.command.dto.AuthenticationResult;
import com.ccbsa.wms.user.application.service.command.dto.LoginCommand;
import com.ccbsa.wms.user.application.service.command.dto.RefreshTokenCommand;
import com.ccbsa.wms.user.application.service.exception.AuthenticationException;
import com.ccbsa.wms.user.application.service.query.dto.UserContextQuery;
import com.ccbsa.wms.user.application.service.query.dto.UserContextView;
import com.ccbsa.wms.user.domain.core.valueobject.KeycloakUserId;

/**
 * Port interface for authentication operations.
 * <p>
 * Defines the contract for authentication services (BFF layer) and user management operations.
 */
public interface AuthenticationServicePort {
    /**
     * Authenticates a user with username and password.
     *
     * @param command Login command
     * @return Authentication result with tokens and user context
     * @throws AuthenticationException if authentication fails (invalid credentials)
     */
    AuthenticationResult login(LoginCommand command);

    /**
     * Refreshes an access token using a refresh token.
     *
     * @param command Refresh token command
     * @return Authentication result with new tokens and user context
     * @throws AuthenticationException if refresh token is invalid or expired
     */
    AuthenticationResult refreshToken(RefreshTokenCommand command);

    /**
     * Gets user context from JWT access token.
     *
     * @param query User context query
     * @return User context view
     */
    UserContextView getUserContext(UserContextQuery query);

    /**
     * Creates a user in Keycloak.
     *
     * @param tenantId  Tenant identifier
     * @param username  Username
     * @param email     EmailAddress address
     * @param password  Initial password
     * @param firstName First name (optional)
     * @param lastName  Last name (optional)
     * @return Keycloak user identifier
     * @throws com.ccbsa.wms.user.application.service.exception.KeycloakServiceException if user creation fails
     */
    KeycloakUserId createUser(String tenantId, String username, String email, String password, String firstName, String lastName);

    /**
     * Updates a user in Keycloak.
     *
     * @param keycloakUserId Keycloak user identifier
     * @param email          EmailAddress address
     * @param firstName      First name (optional)
     * @param lastName       Last name (optional)
     * @throws com.ccbsa.wms.user.application.service.exception.KeycloakServiceException if update fails
     */
    void updateUser(KeycloakUserId keycloakUserId, String email, String firstName, String lastName);

    /**
     * Enables a user in Keycloak.
     *
     * @param keycloakUserId Keycloak user identifier
     * @throws com.ccbsa.wms.user.application.service.exception.KeycloakServiceException if operation fails
     */
    void enableUser(KeycloakUserId keycloakUserId);

    /**
     * Disables a user in Keycloak.
     *
     * @param keycloakUserId Keycloak user identifier
     * @throws com.ccbsa.wms.user.application.service.exception.KeycloakServiceException if operation fails
     */
    void disableUser(KeycloakUserId keycloakUserId);

    /**
     * Assigns a role to a user in Keycloak.
     *
     * @param keycloakUserId Keycloak user identifier
     * @param roleName       Role name
     * @throws com.ccbsa.wms.user.application.service.exception.KeycloakServiceException if role assignment fails
     */
    void assignRole(KeycloakUserId keycloakUserId, String roleName);

    /**
     * Removes a role from a user in Keycloak.
     *
     * @param keycloakUserId Keycloak user identifier
     * @param roleName       Role name
     * @throws com.ccbsa.wms.user.application.service.exception.KeycloakServiceException if role removal fails
     */
    void removeRole(KeycloakUserId keycloakUserId, String roleName);

    /**
     * Gets all roles assigned to a user in Keycloak.
     *
     * @param keycloakUserId Keycloak user identifier
     * @return List of role names
     * @throws com.ccbsa.wms.user.application.service.exception.KeycloakServiceException if operation fails
     */
    List<String> getUserRoles(KeycloakUserId keycloakUserId);

    /**
     * Sends email verification and password reset email to a user.
     * <p>
     * This method sends a Keycloak action email containing both VERIFY_EMAIL and UPDATE_PASSWORD actions. The user will receive an email with links to verify their email address
     * and set their password.
     *
     * @param keycloakUserId Keycloak user identifier
     * @param redirectUri    Optional redirect URI for the action links (frontend verification page)
     * @throws com.ccbsa.wms.user.application.service.exception.KeycloakServiceException if email sending fails
     */
    void sendEmailVerificationAndPasswordReset(KeycloakUserId keycloakUserId, String redirectUri);
}

