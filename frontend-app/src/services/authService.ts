import apiClient from './apiClient';
import axios from 'axios';
import {getTokenExpirationTime, isTokenExpired, shouldRefreshToken} from '../utils/jwtUtils';
import {logger} from '../utils/logger';
import {tokenStorage} from './tokenStorage';

export interface LoginRequest {
    username: string;
    password: string;
}

export interface LoginResponse {
    accessToken: string;
    refreshToken: string | null; // May be null - stored in httpOnly cookie by backend
    tokenType: string;
    expiresIn: number;
    userContext: UserContext;
}

export interface UserContext {
    userId: string;
    username: string;
    tenantId: string | null;
    roles: string[];
    email: string | null;
    firstName: string | null;
    lastName: string | null;
}

export interface RefreshTokenRequest {
    refreshToken: string;
}

/**
 * Authentication service for BFF integration.
 * Handles login, logout, token refresh, and user context retrieval.
 */
export const authService = {
    /**
     * Login with username and password.
     *
     * Note: Refresh token is automatically stored in httpOnly cookie by backend.
     * Only access token is returned in response body.
     *
     * @throws {AxiosError} If authentication fails or network error occurs
     */
    async login(request: LoginRequest): Promise<LoginResponse> {
        const response = await apiClient.post<{ data: LoginResponse }>(
            '/bff/auth/login',
            request,
            {
                withCredentials: true, // Required to receive httpOnly cookies
            }
        );

        // Validate response structure
        if (!response.data || !response.data.data) {
            throw new Error('Invalid response format from authentication service');
        }

        // Refresh token is in httpOnly cookie (not in response body)
        // Access token is in response body (stored in memory)
        return response.data.data;
    },

    /**
     * Refresh access token.
     *
     * Note: Refresh token is now read from httpOnly cookie by the backend.
     * The request body is optional (for backward compatibility during migration).
     *
     * @param request - RefreshTokenRequest (optional, cookie is preferred)
     */
    async refreshToken(request?: RefreshTokenRequest): Promise<LoginResponse> {
        // Use direct axios call to avoid token injection for refresh endpoint
        // Include credentials to send httpOnly cookies
        const response = await axios.post<{ data: LoginResponse }>(
            `${import.meta.env.VITE_API_BASE_URL || '/api/v1'}/bff/auth/refresh`,
            request || {}, // Empty body if not provided (backend will use cookie)
            {
                headers: {
                    'Content-Type': 'application/json',
                },
                withCredentials: true, // Required to send httpOnly cookies
            }
        );
        return response.data.data;
    },

    /**
     * Get current user context.
     */
    async getCurrentUser(): Promise<UserContext> {
        const response = await apiClient.get<{ data: UserContext }>('/bff/auth/me');
        return response.data.data;
    },

    /**
     * Logout (client-side token removal and server-side cookie clearing).
     *
     * Note: Calls backend logout endpoint to clear httpOnly refresh token cookie.
     * Client-side cleanup happens regardless of backend call success.
     */
    async logout(): Promise<void> {
        // Clear client-side tokens first
        tokenStorage.clearAll();
        localStorage.removeItem('userContext');

        // Call backend logout endpoint to clear httpOnly refresh token cookie
        try {
            await apiClient.post('/bff/auth/logout', {}, {
                withCredentials: true, // Required to send cookies
            });
            logger.debug('Logout endpoint called successfully - refresh token cookie cleared');
        } catch (error) {
            // Log error but don't fail logout - client-side cleanup already done
            logger.warn('Logout endpoint call failed, but client-side cleanup completed', {
                error: error instanceof Error ? error.message : String(error),
            });
        }
    },

    /**
     * Check if user is authenticated.
     * Also validates that the token is not expired.
     */
    isAuthenticated(): boolean {
        const token = tokenStorage.getAccessToken();
        if (!token) {
            return false;
        }

        // Check if token is expired
        if (isTokenExpired(token)) {
            logger.debug('Access token is expired');
            return false;
        }

        return true;
    },

    /**
     * Checks if the access token should be refreshed proactively.
     * Returns true if token is within the refresh window (80% of lifetime).
     */
    shouldRefreshToken(): boolean {
        const token = tokenStorage.getAccessToken();
        if (!token) {
            return false;
        }

        return shouldRefreshToken(token);
    },

    /**
     * Gets the expiration time of the current access token.
     * @returns Expiration time in milliseconds, or null if no token or invalid
     */
    getTokenExpirationTime(): number | null {
        const token = tokenStorage.getAccessToken();
        if (!token) {
            return null;
        }

        return getTokenExpirationTime(token);
    },

    /**
     * Get stored user context.
     */
    getStoredUserContext(): UserContext | null {
        const stored = localStorage.getItem('userContext');
        return stored ? JSON.parse(stored) : null;
    },

    /**
     * Store user context.
     */
    storeUserContext(context: UserContext): void {
        localStorage.setItem('userContext', JSON.stringify(context));
    },

    /**
     * Store tokens.
     *
     * Access token is stored in memory (industry best practice).
     * Refresh token is stored in httpOnly cookie by backend (industry best practice).
     *
     * @param accessToken - Access token to store in memory
     * @param refreshToken - Refresh token (ignored, stored in httpOnly cookie by backend)
     */
    storeTokens(accessToken: string, _refreshToken: string): void {
        tokenStorage.setAccessToken(accessToken);
        // Refresh token is automatically stored in httpOnly cookie by backend
        // No client-side storage needed
    },
};

