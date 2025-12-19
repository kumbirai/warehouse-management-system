import apiClient from './apiClient';
import axios from 'axios';
import { getTokenExpirationTime, isTokenExpired, shouldRefreshToken } from '../utils/jwtUtils';
import { logger } from '../utils/logger';
import { tokenStorage } from './tokenStorage';

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

export interface ApiError {
  code: string;
  message: string;
  details?: Record<string, any>;
  timestamp?: string;
  path?: string;
  requestId?: string;
}

export type ApiResponse<T> = { data: T; error?: never } | { data?: never; error: ApiError };

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
    try {
      logger.debug('Sending login request', {
        username: request.username,
        endpoint: '/bff/auth/login',
      });

      const response = await apiClient.post<ApiResponse<LoginResponse>>(
        '/bff/auth/login',
        request,
        {
          withCredentials: true, // Required to receive httpOnly cookies
        }
      );

      // Log raw response for debugging
      console.log('[authService] Raw login response:', {
        status: response.status,
        statusText: response.statusText,
        headers: response.headers,
        data: response.data,
        dataString: JSON.stringify(response.data, null, 2),
      });

      // Log response for debugging
      logger.debug('Login response received', {
        hasData: !!response.data,
        hasNestedData: !!response.data?.data,
        responseKeys: response.data ? Object.keys(response.data) : [],
        status: response.status,
        fullResponse: JSON.stringify(response.data, null, 2),
      });

      // Validate response structure
      if (!response.data) {
        logger.error('Login response missing data field', {
          response: response,
          responseString: JSON.stringify(response, null, 2),
        });
        console.error('Login response missing data field - Full response:', response);
        throw new Error('Invalid response format from authentication service: missing data field');
      }

      // Check if response is an error response
      if (response.data.error) {
        logger.error('Login response contains error', {
          error: response.data.error,
          errorCode: response.data.error?.code,
          errorMessage: response.data.error?.message,
          fullResponse: JSON.stringify(response.data, null, 2),
        });
        console.error('Login response contains error:', response.data.error);
        throw new Error(response.data.error?.message || 'Authentication failed');
      }

      // Handle both normal format and array format (with type information)
      let loginData: any;

      if (Array.isArray(response.data.data)) {
        // Handle WRAPPER_ARRAY format: ["ClassName", { actualData }]
        // This happens when Jackson includes type information
        logger.warn(
          'Login response is in array format (type information included), extracting data',
          {
            arrayLength: response.data.data.length,
            firstElement: response.data.data[0],
          }
        );
        if (response.data.data.length >= 2 && typeof response.data.data[1] === 'object') {
          loginData = response.data.data[1];
        } else {
          logger.error('Login response array format is invalid', {
            responseData: response.data,
            responseDataString: JSON.stringify(response.data, null, 2),
          });
          console.error(
            'Login response array format is invalid - Full response.data:',
            response.data
          );
          throw new Error(
            'Invalid response format from authentication service: invalid array format'
          );
        }
      } else if (response.data.data && typeof response.data.data === 'object') {
        // Normal format: { actualData }
        loginData = response.data.data;
      } else {
        const dataValue = (response.data as any).data;
        logger.error('Login response missing nested data field', {
          responseData: response.data,
          responseDataKeys: Object.keys(response.data),
          responseDataString: JSON.stringify(response.data, null, 2),
          dataType: typeof dataValue,
          isArray: Array.isArray(dataValue),
        });
        console.error(
          'Login response missing nested data field - Full response.data:',
          response.data
        );
        throw new Error(
          'Invalid response format from authentication service: missing nested data field'
        );
      }

      // Log the actual loginData structure
      logger.debug('Login data extracted', {
        loginDataKeys: Object.keys(loginData),
        loginDataString: JSON.stringify(loginData, null, 2),
        hasAccessToken: 'accessToken' in loginData,
        accessTokenValue: loginData.accessToken,
        accessTokenType: typeof loginData.accessToken,
      });

      // Validate required fields - check for both camelCase and potential snake_case
      const accessToken = loginData.accessToken || (loginData as any).access_token;
      if (!accessToken) {
        // Log the full response structure for debugging
        const errorDetails = {
          loginDataKeys: Object.keys(loginData),
          loginDataString: JSON.stringify(loginData, null, 2),
          loginDataValues: Object.entries(loginData).map(([key, value]) => ({
            key,
            value: value === null ? 'null' : value === undefined ? 'undefined' : typeof value,
            actualValue: value,
          })),
          hasAccessToken: 'accessToken' in loginData,
          hasAccess_token: 'access_token' in loginData,
          fullResponse: JSON.stringify(response.data, null, 2),
        };

        logger.error('Login response missing accessToken', errorDetails);
        console.error('Login response missing accessToken - Full details:', errorDetails);

        throw new Error('Invalid response format from authentication service: missing accessToken');
      }

      // If accessToken was in snake_case, normalize it
      if (!loginData.accessToken && (loginData as any).access_token) {
        loginData.accessToken = (loginData as any).access_token;
      }

      // Ensure we're using the normalized accessToken
      if (!loginData.accessToken && accessToken) {
        loginData.accessToken = accessToken;
      }

      if (!loginData.userContext) {
        logger.error('Login response missing userContext', {
          loginDataKeys: Object.keys(loginData),
        });
        throw new Error('Invalid response format from authentication service: missing userContext');
      }

      logger.info('Login successful', {
        username: loginData.userContext.username,
        userId: loginData.userContext.userId,
        roles: loginData.userContext.roles,
      });

      // Refresh token is in httpOnly cookie (not in response body)
      // Access token is in response body (stored in memory)
      return loginData;
    } catch (error) {
      // Use logger.error with error as second parameter for proper error handling
      logger.error('Login failed', error instanceof Error ? error : undefined, {
        errorMessage: error instanceof Error ? error.message : String(error),
        errorStack: error instanceof Error ? error.stack : undefined,
        errorName: error instanceof Error ? error.name : undefined,
      });
      throw error;
    }
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
    // Get base URL using the same logic as apiClient (handles HTTP/HTTPS conversion for localhost)
    const baseURL = (() => {
      const envUrl = import.meta.env.VITE_API_BASE_URL;
      if (!envUrl) return '/api/v1';
      if (envUrl.startsWith('/')) return envUrl;
      // In development, force HTTP for localhost connections
      if (import.meta.env.DEV && envUrl.includes('localhost')) {
        return envUrl.replace(/^https:\/\//i, 'http://');
      }
      return envUrl;
    })();
    const response = await axios.post<ApiResponse<LoginResponse>>(
      `${baseURL}/bff/auth/refresh`,
      request || {}, // Empty body if not provided (backend will use cookie)
      {
        headers: {
          'Content-Type': 'application/json',
        },
        withCredentials: true, // Required to send httpOnly cookies
      }
    );

    // Check for error response
    if (response.data.error) {
      throw new Error(response.data.error.message || 'Token refresh failed');
    }

    if (!response.data.data) {
      throw new Error('Invalid response format from authentication service: missing data field');
    }

    return response.data.data;
  },

  /**
   * Get current user context.
   */
  async getCurrentUser(): Promise<UserContext> {
    const response = await apiClient.get<ApiResponse<UserContext>>('/bff/auth/me');

    // Check for error response
    if (response.data.error) {
      throw new Error(response.data.error.message || 'Failed to get current user');
    }

    if (!response.data.data) {
      throw new Error('Invalid response format from authentication service: missing data field');
    }

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
      // Send empty object - backend accepts optional body with @RequestBody(required = false)
      // Empty object should be handled correctly by Spring's Map deserialization
      await apiClient.post(
        '/bff/auth/logout',
        {},
        {
          withCredentials: true, // Required to send cookies
        }
      );
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
   * Handles invalid JSON gracefully by cleaning up corrupted data.
   */
  getStoredUserContext(): UserContext | null {
    try {
      const stored = localStorage.getItem('userContext');
      if (!stored) {
        return null;
      }

      // Handle case where stored value is the string "undefined" or "null"
      if (stored === 'undefined' || stored === 'null') {
        localStorage.removeItem('userContext');
        return null;
      }

      const parsed = JSON.parse(stored);
      // Validate that parsed value is an object with expected structure
      if (parsed && typeof parsed === 'object' && parsed.userId) {
        return parsed as UserContext;
      }

      // Invalid structure - clean up
      localStorage.removeItem('userContext');
      return null;
    } catch (error) {
      // Invalid JSON - clean up corrupted data
      logger.warn('Failed to parse stored user context, cleaning up', {
        error: error instanceof Error ? error.message : String(error),
      });
      localStorage.removeItem('userContext');
      return null;
    }
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
