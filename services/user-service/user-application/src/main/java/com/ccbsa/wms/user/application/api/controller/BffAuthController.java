package com.ccbsa.wms.user.application.api.controller;

import java.util.Map;
import java.util.Objects;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ccbsa.common.application.api.ApiResponse;
import com.ccbsa.common.application.api.ApiResponseBuilder;
import com.ccbsa.wms.user.application.api.dto.LoginRequest;
import com.ccbsa.wms.user.application.api.dto.LoginResponse;
import com.ccbsa.wms.user.application.api.dto.RefreshTokenRequest;
import com.ccbsa.wms.user.application.api.dto.UserContextResponse;
import com.ccbsa.wms.user.application.api.mapper.AuthMapper;
import com.ccbsa.wms.user.application.api.util.CookieUtil;
import com.ccbsa.wms.user.application.service.command.LoginCommandHandler;
import com.ccbsa.wms.user.application.service.command.RefreshTokenCommandHandler;
import com.ccbsa.wms.user.application.service.command.dto.AuthenticationResult;
import com.ccbsa.wms.user.application.service.command.dto.LoginCommand;
import com.ccbsa.wms.user.application.service.command.dto.RefreshTokenCommand;
import com.ccbsa.wms.user.application.service.query.GetUserContextQueryHandler;
import com.ccbsa.wms.user.application.service.query.dto.UserContextQuery;
import com.ccbsa.wms.user.application.service.query.dto.UserContextView;

import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

/**
 * REST Controller: BffAuthController
 * <p>
 * BFF (Backend for Frontend) authentication endpoints.
 * Masks Keycloak IAM complexity from frontend clients.
 */
@RestController
@RequestMapping("/bff/auth")
@Tag(name = "BFF Authentication",
        description = "Backend for Frontend authentication endpoints")
public class BffAuthController {
    private final LoginCommandHandler loginCommandHandler;
    private final RefreshTokenCommandHandler refreshTokenCommandHandler;
    private final GetUserContextQueryHandler getUserContextQueryHandler;
    private final AuthMapper mapper;
    private final CookieUtil cookieUtil;

    /**
     * Constructs a new BffAuthController with required dependencies.
     * <p>
     * All dependencies are stored as final fields and used internally.
     * They are not exposed through any public methods, maintaining proper encapsulation.
     *
     * @param loginCommandHandler        the login command handler, must not be null
     * @param refreshTokenCommandHandler the refresh token command handler, must not be null
     * @param getUserContextQueryHandler the user context query handler, must not be null
     * @param mapper                     the authentication mapper, must not be null
     * @param cookieUtil                 the cookie utility, must not be null
     */
    public BffAuthController(LoginCommandHandler loginCommandHandler,
                             RefreshTokenCommandHandler refreshTokenCommandHandler,
                             GetUserContextQueryHandler getUserContextQueryHandler,
                             AuthMapper mapper,
                             CookieUtil cookieUtil) {
        this.loginCommandHandler = Objects.requireNonNull(loginCommandHandler, "LoginCommandHandler cannot be null");
        this.refreshTokenCommandHandler = Objects.requireNonNull(refreshTokenCommandHandler, "RefreshTokenCommandHandler cannot be null");
        this.getUserContextQueryHandler = Objects.requireNonNull(getUserContextQueryHandler, "GetUserContextQueryHandler cannot be null");
        this.mapper = Objects.requireNonNull(mapper, "AuthMapper cannot be null");
        this.cookieUtil = Objects.requireNonNull(cookieUtil, "CookieUtil cannot be null");
    }

    @PostMapping("/login")
    @Operation(summary = "Login",
            description = "Authenticates user and returns tokens with user context. Refresh token is set as httpOnly cookie.")
    @Timed(value = "bff.auth.login",
            description = "Time taken to process login request")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request,
                                                            HttpServletResponse httpResponse) {
        LoginCommand command = mapper.toLoginCommand(request);
        AuthenticationResult result = loginCommandHandler.handle(command);
        LoginResponse response = mapper.toLoginResponse(result);

        // Set refresh token as httpOnly cookie (industry best practice)
        cookieUtil.addRefreshTokenCookie(httpResponse, result.getRefreshToken());

        // Remove refresh token from response body for security
        // Access token remains in response body (stored in memory on frontend)
        response.setRefreshToken(null);

        return ApiResponseBuilder.ok(response);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh token",
            description = "Refreshes access token using refresh token from httpOnly cookie (preferred) or request body (backward compatibility)")
    @Timed(value = "bff.auth.refresh",
            description = "Time taken to refresh token")
    public ResponseEntity<ApiResponse<LoginResponse>> refreshToken(
            @CookieValue(value = "refreshToken", required = false) String refreshTokenFromCookie,
            @RequestBody(required = false) RefreshTokenRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        // Prefer refresh token from httpOnly cookie (industry best practice)
        // Fallback to request body for backward compatibility during migration
        String refreshToken = refreshTokenFromCookie;
        if (refreshToken == null || refreshToken.isEmpty()) {
            // Try to get from cookie using CookieUtil (handles null cookies)
            refreshToken = cookieUtil.getRefreshTokenFromCookie(httpRequest);
        }

        // Fallback to request body if not in cookie (backward compatibility)
        if ((refreshToken == null || refreshToken.isEmpty()) && request != null && request.getRefreshToken() != null) {
            refreshToken = request.getRefreshToken();
        }

        if (refreshToken == null || refreshToken.isEmpty()) {
            throw new IllegalArgumentException("Refresh token is required (from httpOnly cookie or request body)");
        }

        RefreshTokenCommand command = new RefreshTokenCommand(refreshToken);
        AuthenticationResult result = refreshTokenCommandHandler.handle(command);
        LoginResponse response = mapper.toLoginResponse(result);

        // Set new refresh token as httpOnly cookie (token rotation)
        cookieUtil.addRefreshTokenCookie(httpResponse, result.getRefreshToken());

        // Remove refresh token from response body for security
        // Access token remains in response body (stored in memory on frontend)
        response.setRefreshToken(null);

        return ApiResponseBuilder.ok(response);
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user context",
            description = "Returns current authenticated user context")
    @Timed(value = "bff.auth.me",
            description = "Time taken to get user context")
    public ResponseEntity<ApiResponse<UserContextResponse>> getCurrentUser(@AuthenticationPrincipal Jwt jwt) {
        UserContextQuery query = mapper.toUserContextQuery(jwt.getTokenValue());
        UserContextView view = getUserContextQueryHandler.handle(query);
        UserContextResponse response = mapper.toUserContextResponse(view);
        return ApiResponseBuilder.ok(response);
    }

    @PostMapping(value = "/logout", consumes = {"application/json", "application/*+json", "*/*"})
    @Operation(summary = "Logout",
            description = "Logs out the current user and clears refresh token cookie")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestBody(required = false) Map<String, Object> body,
            HttpServletResponse httpResponse) {
        // Clear refresh token cookie
        cookieUtil.removeRefreshTokenCookie(httpResponse);

        // For JWT tokens, logout is handled client-side by discarding access token
        // Keycloak doesn't support token revocation for stateless JWT tokens
        // In a production system, you might want to implement token blacklisting
        // Request body is optional (frontend may send empty {} or no body)
        return ApiResponseBuilder.noContent();
    }
}

