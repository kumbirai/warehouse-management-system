package com.ccbsa.wms.user.application.api.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Utility class for managing httpOnly cookies for refresh tokens.
 *
 * <p>Implements industry best practices for secure cookie handling:
 * <ul>
 *   <li>httpOnly: Prevents JavaScript access (XSS protection)</li>
 *   <li>Secure: Only sent over HTTPS</li>
 *   <li>SameSite: CSRF protection</li>
 *   <li>Path: Restricted to authentication endpoints</li>
 * </ul>
 */
@Component
public class CookieUtil {
    private static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";
    private static final String COOKIE_PATH = "/api/v1/bff/auth";

    @Value("${app.security.cookie.secure:true}")
    private boolean cookieSecure;

    @Value("${app.security.cookie.same-site:Strict}")
    private String cookieSameSite;

    @Value("${app.security.cookie.max-age-seconds:86400}")
    private int cookieMaxAgeSeconds;

    /**
     * Extracts refresh token from request cookie or returns null if not found.
     *
     * @param request HTTP request
     * @return Refresh token from cookie or null
     */
    public String getRefreshTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }

        for (Cookie cookie : request.getCookies()) {
            if (REFRESH_TOKEN_COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }

        return null;
    }

    /**
     * Adds refresh token cookie to response using ResponseCookie.
     *
     * @param response     HTTP response
     * @param refreshToken Refresh token value
     */
    public void addRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        ResponseCookie cookie = createRefreshTokenCookie(refreshToken);
        response.addHeader("Set-Cookie", cookie.toString());
    }

    /**
     * Creates a secure httpOnly cookie for refresh token using ResponseCookie (Spring Boot 3.x).
     *
     * @param refreshToken The refresh token value (must not be null)
     * @return Configured ResponseCookie object
     * @throws IllegalArgumentException if refreshToken is null or empty
     */
    public ResponseCookie createRefreshTokenCookie(String refreshToken) {
        if (refreshToken == null || refreshToken.isEmpty()) {
            throw new IllegalArgumentException("Refresh token cannot be null or empty");
        }

        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, refreshToken)
                .httpOnly(true) // Prevents JavaScript access (XSS protection)
                .secure(cookieSecure) // Only sent over HTTPS
                .path(COOKIE_PATH) // Restricted to auth endpoints
                .maxAge(cookieMaxAgeSeconds); // 24 hours default

        // Set SameSite attribute for CSRF protection
        // Ensure non-null value for type safety
        final String sameSiteValue;
        if (cookieSameSite != null && !cookieSameSite.isEmpty()) {
            sameSiteValue = cookieSameSite;
        } else {
            sameSiteValue = "Strict";
        }

        if ("Strict".equalsIgnoreCase(sameSiteValue)) {
            builder.sameSite("Strict"); // Cookie not sent on cross-site requests
        } else if ("Lax".equalsIgnoreCase(sameSiteValue)) {
            builder.sameSite("Lax"); // Cookie sent on top-level navigation
        } else if ("None".equalsIgnoreCase(sameSiteValue)) {
            builder.sameSite("None"); // Cookie sent on all requests (requires Secure=true)
            builder.secure(true); // Ensure Secure is true for SameSite=None
        } else {
            // Default to Strict if invalid value
            builder.sameSite("Strict");
        }

        return builder.build();
    }

    /**
     * Removes refresh token cookie from response.
     *
     * @param response HTTP response
     */
    public void removeRefreshTokenCookie(HttpServletResponse response) {
        ResponseCookie cookie = createDeleteRefreshTokenCookie();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    /**
     * Creates a cookie to delete/clear the refresh token.
     *
     * @return ResponseCookie with maxAge=0 to delete the cookie
     */
    public ResponseCookie createDeleteRefreshTokenCookie() {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .path(COOKIE_PATH)
                .maxAge(0) // Delete cookie
                .sameSite("Strict")
                .build();
    }
}

