import axios, {AxiosError, AxiosInstance, InternalAxiosRequestConfig} from 'axios';
import {LoginResponse} from './authService';
import {logger} from '../utils/logger';
import {tokenStorage} from './tokenStorage';
import {correlationIdService} from './correlationIdService';

/**
 * Extended AxiosInstance with internal refresh promise tracking.
 * Used to prevent race conditions during token refresh.
 */
interface ExtendedAxiosInstance extends AxiosInstance {
  _refreshPromise?: Promise<string>;
}

/**
 * Base API client configuration.
 * Handles authentication token injection and error handling.
 *
 * Production-grade features:
 * - Request timeout configuration (30 seconds)
 * - Token refresh with race condition prevention
 * - Proper error handling and logging
 *
 * Note: In development, use relative URL (/api/v1) to go through Vite proxy.
 * In production, use absolute URL (https://gateway.example.com/api/v1).
 */
const getBaseURL = (): string => {
  const envUrl = import.meta.env.VITE_API_BASE_URL;

  // If no env var is set, use relative URL (goes through Vite proxy in dev)
  if (!envUrl) {
    logger.debug('No VITE_API_BASE_URL set, using relative URL /api/v1 (will use Vite proxy)');
    return '/api/v1';
  }

  // If relative URL is provided, use it
  if (envUrl.startsWith('/')) {
    logger.debug('Using relative URL from VITE_API_BASE_URL', { url: envUrl });
    return envUrl;
  }

  // If absolute URL is provided, check if it's localhost in development
  // In development, force HTTP for localhost connections (gateway SSL is disabled by default)
  if (import.meta.env.DEV && envUrl.includes('localhost')) {
    // Replace HTTPS with HTTP for localhost in development
    const httpUrl = envUrl.replace(/^https:\/\//i, 'http://');
    if (httpUrl !== envUrl) {
      logger.debug('Converting HTTPS to HTTP for localhost in development', {
        original: envUrl,
        converted: httpUrl,
      });
      return httpUrl;
    }
  }

  // If absolute URL is provided, use it (for production or direct connection)
  logger.debug('Using absolute URL from VITE_API_BASE_URL', { url: envUrl });
  return envUrl;
};

const apiClient: ExtendedAxiosInstance = axios.create({
  baseURL: getBaseURL(),
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 30000, // 30 seconds timeout for production
  withCredentials: true, // Required to send httpOnly cookies (refresh token)
});

// Request interceptor to inject access token and log requests
// Note: Proactive token refresh is handled by response interceptor on 401 errors
// to avoid circular dependencies and complexity in request interceptor
apiClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    // Get access token from in-memory storage (industry best practice)
    const token = tokenStorage.getAccessToken();
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`;
    }

    // Inject correlation ID for traceability
    const correlationId = correlationIdService.getCorrelationId();
    if (config.headers) {
      config.headers[correlationIdService.getCorrelationIdHeader()] = correlationId;
    }

    // Log request URL for debugging (only in development)
    if (import.meta.env.DEV) {
      const fullUrl =
        config.baseURL && config.url
          ? `${config.baseURL}${config.url.startsWith('/') ? '' : '/'}${config.url}`
          : config.url;
      logger.debug('API Request:', {
        method: config.method?.toUpperCase(),
        url: fullUrl,
        baseURL: config.baseURL,
        hasToken: !!token,
        tokenLength: token ? token.length : 0,
        correlationId, // Include correlation ID in logs
      });
    }

    return config;
  },
  error => {
    logger.error('Request interceptor error:', error);
    return Promise.reject(error);
  }
);

// Response interceptor for error handling
apiClient.interceptors.response.use(
  response => {
    // Don't handle error objects in successful responses here
    // Let the components handle them - they may be business logic errors, not auth errors
    // Only handle HTTP status codes in the error handler below
    return response;
  },
  async (error: AxiosError) => {
    const originalRequest = error.config as InternalAxiosRequestConfig & { _retry?: boolean };

    // Log error details for debugging
    if (import.meta.env.DEV) {
      // Extract error response data if available, ensuring proper serialization
      let errorResponseData: unknown = undefined;
      if (error.response?.data) {
        try {
          // Try to serialize response data, handling circular references
          if (typeof error.response.data === 'object' && error.response.data !== null) {
            errorResponseData = JSON.parse(
              JSON.stringify(error.response.data, (key, value) => {
                // Handle circular references
                if (key === 'config' && typeof value === 'object') {
                  return '[Circular]';
                }
                // Handle Error objects
                if (value instanceof Error) {
                  return {
                    name: value.name,
                    message: value.message,
                    stack: value.stack,
                  };
                }
                return value;
              })
            );
          } else {
            errorResponseData = error.response.data;
          }
        } catch (e) {
          // If serialization fails, use string representation
          errorResponseData = String(error.response.data);
        }
      }

      // Use logger.error with error as second parameter for proper error handling
      logger.error('API Error:', error, {
        message: error.message,
        code: error.code,
        status: error.response?.status,
        statusText: error.response?.statusText,
        url: error.config?.url,
        baseURL: error.config?.baseURL,
        timeout: error.code === 'ECONNABORTED',
        responseData: errorResponseData,
        // Include correlation ID if available
        correlationId: error.config?.headers?.[correlationIdService.getCorrelationIdHeader()],
      });
    }

    // Handle timeout errors
    if (error.code === 'ECONNABORTED' || error.message?.includes('timeout')) {
      logger.warn('Request timeout:', {
        url: error.config?.url,
        timeout: error.config?.timeout,
      });
      // Don't retry timeout errors - they indicate a connectivity issue
      return Promise.reject(error);
    }

    // Handle 429 Too Many Requests - rate limited
    if (error.response?.status === 429) {
      // Don't retry rate limit errors immediately - wait a bit
      const retryAfter = error.response.headers['retry-after'];
      const waitTime = retryAfter ? parseInt(retryAfter, 10) * 1000 : 2000;

      if (originalRequest && !originalRequest._retry) {
        originalRequest._retry = true;
        await new Promise(resolve => setTimeout(resolve, waitTime));
        return apiClient(originalRequest);
      }
      // If already retried, reject to prevent infinite loops
      return Promise.reject(error);
    }

    // Handle 401 Unauthorized - token expired
    // Use a shared promise to prevent multiple simultaneous refresh attempts (race condition prevention)
    if (error.response?.status === 401 && originalRequest && !originalRequest._retry) {
      originalRequest._retry = true;

      // Check if there's already a refresh in progress
      if (!apiClient._refreshPromise) {
        apiClient._refreshPromise = (async () => {
          try {
            // Refresh token is now in httpOnly cookie (automatically sent by browser)
            // No need to get it from storage - backend will read from cookie
            // Attempt token refresh (use direct axios to avoid interceptor loop)
            // Get base URL using the same logic as getBaseURL (handles HTTP/HTTPS conversion for localhost)
            const refreshBaseURL = (() => {
              const envUrl = import.meta.env.VITE_API_BASE_URL;
              if (!envUrl) return '/api/v1';
              if (envUrl.startsWith('/')) return envUrl;
              // In development, force HTTP for localhost connections
              if (import.meta.env.DEV && envUrl.includes('localhost')) {
                return envUrl.replace(/^https:\/\//i, 'http://');
              }
              return envUrl;
            })();
            const response = await axios.post<
              | { data: LoginResponse; error?: never }
              | { data?: never; error: { code: string; message: string } }
            >(
              `${refreshBaseURL}/bff/auth/refresh`,
              {}, // Empty body - backend will read refresh token from httpOnly cookie
              {
                headers: {
                  'Content-Type': 'application/json',
                },
                withCredentials: true, // Required to send httpOnly cookies
                timeout: 10000, // 10 second timeout for refresh
              }
            );

            // Check for error response
            if (response.data.error) {
              throw new Error(response.data.error.message || 'Token refresh failed');
            }

            if (!response.data.data) {
              throw new Error(
                'Invalid response format from authentication service: missing data field'
              );
            }

            const { accessToken } = response.data.data;
            // Store access token in memory (industry best practice)
            tokenStorage.setAccessToken(accessToken);
            // Refresh token is automatically stored in httpOnly cookie by backend
            // No client-side storage needed (industry best practice)

            logger.info('Token refreshed successfully');
            return accessToken;
          } catch (refreshError) {
            // Refresh failed - token expired or invalid
            logger.warn('Token refresh failed', {
              error: refreshError instanceof Error ? refreshError.message : String(refreshError),
            });

            // Clear all auth data
            tokenStorage.clearAll();
            localStorage.removeItem('userContext');

            // Don't use window.location.href as it causes a hard redirect
            // Instead, let the ProtectedRoute handle the redirect via React Router
            // This prevents issues with navigation and state management
            // The error will propagate and ProtectedRoute will redirect to /login
            throw refreshError;
          } finally {
            // Clear the refresh promise so future requests can retry
            delete apiClient._refreshPromise;
          }
        })();
      }

      try {
        // Wait for the refresh to complete (or fail)
        const accessToken = await apiClient._refreshPromise;

        // Retry original request with new token
        if (originalRequest.headers) {
          originalRequest.headers.Authorization = `Bearer ${accessToken}`;
        }
        // Reset retry flag for the retry attempt
        originalRequest._retry = false;
        return apiClient(originalRequest);
      } catch (refreshError) {
        // Refresh failed - already handled in refresh promise
        return Promise.reject(refreshError);
      }
    }

    return Promise.reject(error);
  }
);

export default apiClient;
