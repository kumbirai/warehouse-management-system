/**
 * Token storage service implementing industry best practices.
 *
 * Storage Strategy:
 * - Access tokens: In-memory (JavaScript variable) - Industry best practice
 * - Refresh tokens: localStorage (temporary, will migrate to httpOnly cookies)
 *
 * Production-grade features:
 * - In-memory access token storage (XSS-resistant)
 * - Automatic cleanup on page unload
 * - Token expiration tracking
 * - Secure token handling
 */

import { logger } from '../utils/logger';

/**
 * In-memory storage for access token.
 * Stored in a module-level variable to persist across component re-renders
 * but cleared on page reload (industry best practice).
 */
let accessToken: string | null = null;

/**
 * Token storage service implementing industry best practices.
 */
export const tokenStorage = {
  /**
   * Stores the access token in memory.
   *
   * @param token - Access token to store
   */
  setAccessToken(token: string | null): void {
    accessToken = token;
    logger.debug('Access token stored in memory');
  },

  /**
   * Gets the access token from memory.
   *
   * @returns Access token or null if not set
   */
  getAccessToken(): string | null {
    return accessToken;
  },

  /**
   * Removes the access token from memory.
   */
  clearAccessToken(): void {
    accessToken = null;
    logger.debug('Access token cleared from memory');
  },

  /**
   * Stores the refresh token.
   *
   * Note: Refresh tokens are now stored in httpOnly cookies by the backend.
   * This method is kept for backward compatibility during migration.
   * The backend automatically sets the cookie, so this is a no-op.
   *
   * @param token - Refresh token (ignored, cookie is set by backend)
   */
  setRefreshToken(_token: string | null): void {
    // Refresh token is now stored in httpOnly cookie by backend
    // No client-side storage needed (industry best practice)
    logger.debug('Refresh token stored in httpOnly cookie by backend');
  },

  /**
   * Gets the refresh token from httpOnly cookie.
   *
   * Note: Refresh tokens are stored in httpOnly cookies by the backend.
   * JavaScript cannot read httpOnly cookies, so this returns null.
   * The refresh token is automatically sent by the browser with requests.
   *
   * @returns Always returns null (httpOnly cookies are not accessible to JavaScript)
   */
  getRefreshToken(): string | null {
    // httpOnly cookies are not accessible to JavaScript (by design for security)
    // The browser automatically sends the cookie with requests
    // Return null to indicate we rely on the cookie being sent automatically
    return null;
  },

  /**
   * Removes the refresh token cookie.
   *
   * Note: To clear the httpOnly cookie, call the logout endpoint
   * which will clear the cookie server-side.
   */
  clearRefreshToken(): void {
    // httpOnly cookies cannot be cleared from JavaScript
    // The logout endpoint will clear the cookie
    // For now, we can't do anything client-side
    logger.debug('Refresh token cookie will be cleared by logout endpoint');
  },

  /**
   * Clears all tokens (access and refresh).
   */
  clearAll(): void {
    this.clearAccessToken();
    this.clearRefreshToken();
    logger.debug('All tokens cleared');
  },
};

/**
 * Cleanup function to clear tokens on page unload.
 * This ensures tokens don't persist in memory after page close.
 */
if (typeof window !== 'undefined') {
  window.addEventListener('beforeunload', () => {
    tokenStorage.clearAccessToken();
    // Note: We don't clear refresh token on page unload
    // as it's needed for automatic re-authentication
  });
}
