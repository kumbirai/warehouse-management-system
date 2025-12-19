package com.ccbsa.wms.gateway.api.util;

import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

/**
 * Utility for extracting cookies from HTTP response headers.
 */
public class CookieExtractor {

    private static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";

    /**
     * Extract refresh token cookie from response headers.
     *
     * @param headers the response headers
     * @return ResponseCookie or null if not found
     */
    public static ResponseCookie extractRefreshTokenCookie(HttpHeaders headers) {
        List<String> cookies = headers.get(HttpHeaders.SET_COOKIE);
        if (cookies == null || cookies.isEmpty()) {
            return null;
        }

        for (String cookieHeader : cookies) {
            if (cookieHeader.startsWith(REFRESH_TOKEN_COOKIE_NAME + "=")) {
                return parseCookieHeader(cookieHeader);
            }
        }

        return null;
    }

    /**
     * Parse a Set-Cookie header string into a ResponseCookie.
     *
     * @param cookieHeader the Set-Cookie header value
     * @return ResponseCookie parsed from header
     */
    private static ResponseCookie parseCookieHeader(String cookieHeader) {
        String[] parts = cookieHeader.split(";");
        String nameValue = parts[0].trim();
        String[] nameValuePair = nameValue.split("=", 2);

        if (nameValuePair.length != 2) {
            return null;
        }

        String name = nameValuePair[0].trim();
        String value = nameValuePair[1].trim();

        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, value);

        // Parse attributes
        for (int i = 1; i < parts.length; i++) {
            String attr = parts[i].trim().toLowerCase();
            if (attr.startsWith("httponly")) {
                builder.httpOnly(true);
            } else if (attr.startsWith("samesite=")) {
                String sameSite = attr.substring("samesite=".length()).trim();
                builder.sameSite(sameSite);
            } else if (attr.startsWith("secure")) {
                builder.secure(true);
            } else if (attr.startsWith("max-age=")) {
                try {
                    long maxAge = Long.parseLong(attr.substring("max-age=".length()).trim());
                    builder.maxAge(java.time.Duration.ofSeconds(maxAge));
                } catch (NumberFormatException e) {
                    // Ignore invalid max-age
                }
            }
        }

        return builder.build();
    }

    /**
     * Extract cookie by name from response headers.
     *
     * @param headers    the response headers
     * @param cookieName the cookie name
     * @return ResponseCookie or null if not found
     */
    public static ResponseCookie extractCookie(HttpHeaders headers, String cookieName) {
        List<String> cookies = headers.get(HttpHeaders.SET_COOKIE);
        if (cookies == null || cookies.isEmpty()) {
            return null;
        }

        for (String cookieHeader : cookies) {
            if (cookieHeader.startsWith(cookieName + "=")) {
                return parseCookieHeader(cookieHeader);
            }
        }

        return null;
    }
}

