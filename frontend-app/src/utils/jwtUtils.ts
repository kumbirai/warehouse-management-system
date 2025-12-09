/**
 * JWT utility functions for client-side token validation.
 *
 * Note: These functions decode JWT tokens WITHOUT verification.
 * Token signature verification is performed by the backend (gateway/service).
 * Client-side decoding is only used to check expiration and extract claims.
 *
 * Production-grade features:
 * - Safe JWT decoding (no verification - backend validates)
 * - Expiration checking
 * - Token expiration time extraction
 */

interface JwtPayload {
    exp?: number; // Expiration time (Unix timestamp)
    iat?: number; // Issued at time
    sub?: string; // Subject (user ID)
    tenant_id?: string; // Tenant ID
    realm_access?: {
        roles?: string[];
    };

    [key: string]: unknown;
}

/**
 * Decodes a JWT token without verification.
 *
 * @param token - JWT token string
 * @returns Decoded payload or null if invalid
 */
export function decodeJwt(token: string): JwtPayload | null {
    try {
        const parts = token.split('.');
        if (parts.length !== 3) {
            return null;
        }

        // Decode the payload (second part)
        const payload = parts[1];
        const decoded = atob(payload.replace(/-/g, '+').replace(/_/g, '/'));
        return JSON.parse(decoded) as JwtPayload;
    } catch (error) {
        // Invalid token format
        return null;
    }
}

/**
 * Checks if a JWT token is expired.
 *
 * @param token - JWT token string
 * @returns true if token is expired or invalid, false otherwise
 */
export function isTokenExpired(token: string): boolean {
    const payload = decodeJwt(token);
    if (!payload || !payload.exp) {
        return true; // Consider invalid tokens as expired
    }

    // Check if token is expired (with 60 second buffer for clock skew)
    const expirationTime = payload.exp * 1000; // Convert to milliseconds
    const currentTime = Date.now();
    const bufferTime = 60 * 1000; // 60 seconds buffer

    return currentTime >= (expirationTime - bufferTime);
}

/**
 * Gets the expiration time of a JWT token.
 *
 * @param token - JWT token string
 * @returns Expiration time in milliseconds, or null if invalid
 */
export function getTokenExpirationTime(token: string): number | null {
    const payload = decodeJwt(token);
    if (!payload || !payload.exp) {
        return null;
    }

    return payload.exp * 1000; // Convert to milliseconds
}

/**
 * Checks if a token should be refreshed proactively.
 *
 * A token should be refreshed if it's within the refresh window
 * (e.g., 80% of token lifetime has passed).
 *
 * @param token - JWT token string
 * @param refreshWindowPercent - Percentage of token lifetime after which to refresh (default: 80)
 * @returns true if token should be refreshed, false otherwise
 */
export function shouldRefreshToken(token: string, refreshWindowPercent: number = 80): boolean {
    const payload = decodeJwt(token);
    if (!payload || !payload.exp || !payload.iat) {
        return true; // Invalid token should be refreshed
    }

    const issuedAt = payload.iat * 1000; // Convert to milliseconds
    const expirationTime = payload.exp * 1000; // Convert to milliseconds
    const currentTime = Date.now();

    // Calculate token lifetime
    const tokenLifetime = expirationTime - issuedAt;

    // Calculate refresh threshold (e.g., 80% of lifetime)
    const refreshThreshold = issuedAt + (tokenLifetime * refreshWindowPercent / 100);

    // Check if we're past the refresh threshold
    return currentTime >= refreshThreshold;
}

/**
 * Gets the time remaining until token expiration.
 *
 * @param token - JWT token string
 * @returns Time remaining in milliseconds, or null if invalid/expired
 */
export function getTimeUntilExpiration(token: string): number | null {
    const payload = decodeJwt(token);
    if (!payload || !payload.exp) {
        return null;
    }

    const expirationTime = payload.exp * 1000; // Convert to milliseconds
    const currentTime = Date.now();
    const timeRemaining = expirationTime - currentTime;

    return timeRemaining > 0 ? timeRemaining : null;
}

/**
 * Extracts tenant ID from JWT token.
 *
 * @param token - JWT token string
 * @returns Tenant ID or null if not present
 */
export function getTenantIdFromToken(token: string): string | null {
    const payload = decodeJwt(token);
    return payload?.tenant_id ?? null;
}

/**
 * Extracts user ID from JWT token.
 *
 * @param token - JWT token string
 * @returns User ID (subject) or null if not present
 */
export function getUserIdFromToken(token: string): string | null {
    const payload = decodeJwt(token);
    return payload?.sub ?? null;
}

/**
 * Extracts roles from JWT token.
 *
 * @param token - JWT token string
 * @returns Array of roles or empty array if not present
 */
export function getRolesFromToken(token: string): string[] {
    const payload = decodeJwt(token);
    return payload?.realm_access?.roles ?? [];
}

