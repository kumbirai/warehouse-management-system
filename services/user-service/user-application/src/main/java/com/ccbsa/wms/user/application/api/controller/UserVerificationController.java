package com.ccbsa.wms.user.application.api.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ccbsa.common.application.api.ApiResponse;
import com.ccbsa.common.application.api.ApiResponseBuilder;
import com.ccbsa.common.domain.valueobject.UserId;
import com.ccbsa.wms.user.application.service.exception.KeycloakServiceException;
import com.ccbsa.wms.user.application.service.exception.UserNotFoundException;
import com.ccbsa.wms.user.application.service.port.auth.AuthenticationServicePort;
import com.ccbsa.wms.user.application.service.port.repository.UserRepository;
import com.ccbsa.wms.user.domain.core.entity.User;
import com.ccbsa.wms.user.domain.core.valueobject.KeycloakUserId;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST Controller: UserVerificationController
 * <p>
 * Handles user verification operations (email verification, password reset).
 */
@RestController
@RequestMapping("/users")
@Tag(name = "User Verification", description = "User verification and password reset operations")
public class UserVerificationController {
    private final UserRepository userRepository;
    private final AuthenticationServicePort authenticationService;
    private final String frontendBaseUrl;

    public UserVerificationController(UserRepository userRepository, AuthenticationServicePort authenticationService,
                                      @Value("${frontend.base-url:http://localhost:3000}") String frontendBaseUrl) {
        this.userRepository = userRepository;
        this.authenticationService = authenticationService;
        this.frontendBaseUrl = frontendBaseUrl;
    }

    @PostMapping("/{userId}/resend-verification")
    @Operation(summary = "Resend Verification Email", description = "Resends email verification and password reset email to user (admin only)")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'TENANT_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> resendVerificationEmail(@PathVariable String userId) {
        // Load user to get Keycloak user ID
        User user = userRepository.findById(UserId.of(userId)).orElseThrow(() -> new UserNotFoundException(String.format("User not found: %s", userId)));

        // Check if user has Keycloak user ID
        if (!user.getKeycloakUserId().isPresent()) {
            throw new IllegalStateException(String.format("User does not have a Keycloak account: %s", userId));
        }

        KeycloakUserId keycloakUserId = user.getKeycloakUserId().get();

        try {
            // Send verification and password reset email
            String redirectUri = String.format("%s/verify-email", frontendBaseUrl);
            authenticationService.sendEmailVerificationAndPasswordReset(keycloakUserId, redirectUri);

            return ApiResponseBuilder.ok(null);
        } catch (KeycloakServiceException e) {
            throw new KeycloakServiceException(String.format("Failed to resend verification email: %s", e.getMessage()), e);
        }
    }
}

