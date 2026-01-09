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
 * Circuit breaker state for tracking persistent failures.
 * Prevents infinite retry loops by temporarily disabling retries after repeated failures.
 */
interface CircuitBreakerState {
  failures: number;
  lastFailureTime: number;
  isOpen: boolean;
}

// Circuit breaker configuration
const CIRCUIT_BREAKER_CONFIG = {
  failureThreshold: 5, // Open circuit after 5 consecutive failures
  resetTimeout: 30000, // Reset after 30 seconds
};

// Track circuit breaker state per URL pattern
const circuitBreakers = new Map<string, CircuitBreakerState>();

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
    const originalRequest = error.config as InternalAxiosRequestConfig & {
      _retry?: boolean;
      _retryCount?: number;
    };

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
      const retryAfter = error.response.headers['retry-after'];
      const waitTime = retryAfter ? parseInt(retryAfter, 10) * 1000 : 2000;

      if (originalRequest && !originalRequest._retry) {
        originalRequest._retry = true;
        logger.warn('Rate limited (429), waiting before retry', {
          url: originalRequest.url,
          waitTime,
        });
        await new Promise(resolve => setTimeout(resolve, waitTime));
        return apiClient(originalRequest);
      }
      // If already retried, reject to prevent infinite loops
      logger.error('Rate limit retry already attempted, rejecting', {
        url: originalRequest?.url,
      });
      return Promise.reject(error);
    }

    // Handle 5xx server errors - retry with exponential backoff and circuit breaker
    if (error.response?.status && error.response.status >= 500 && error.response.status < 600) {
      if (!originalRequest) {
        logger.error('No original request config for 5xx error, cannot retry', {
          status: error.response.status,
        });
        return Promise.reject(error);
      }

      // Get or initialize retry count
      const retryCount = (originalRequest._retryCount as number) || 0;
      const maxRetries = 3;

      // Check circuit breaker
      const urlKey = originalRequest.url || 'unknown';
      const circuitBreaker = circuitBreakers.get(urlKey) || {
        failures: 0,
        lastFailureTime: 0,
        isOpen: false,
      };

      // Reset circuit breaker if enough time has passed
      const now = Date.now();
      if (
        circuitBreaker.isOpen &&
        now - circuitBreaker.lastFailureTime > CIRCUIT_BREAKER_CONFIG.resetTimeout
      ) {
        logger.info('Circuit breaker reset, allowing retries again', { url: urlKey });
        circuitBreaker.isOpen = false;
        circuitBreaker.failures = 0;
      }

      // If circuit is open, reject immediately
      if (circuitBreaker.isOpen) {
        logger.warn('Circuit breaker is open, rejecting request without retry', {
          url: urlKey,
          status: error.response.status,
        });
        return Promise.reject(error);
      }

      // Increment failure count
      circuitBreaker.failures++;
      circuitBreaker.lastFailureTime = now;

      // Open circuit if threshold exceeded
      if (circuitBreaker.failures >= CIRCUIT_BREAKER_CONFIG.failureThreshold) {
        circuitBreaker.isOpen = true;
        logger.error('Circuit breaker opened due to persistent failures', {
          url: urlKey,
          failures: circuitBreaker.failures,
          threshold: CIRCUIT_BREAKER_CONFIG.failureThreshold,
        });
        circuitBreakers.set(urlKey, circuitBreaker);
        return Promise.reject(error);
      }

      // Retry if under max retries
      if (retryCount < maxRetries) {
        originalRequest._retryCount = retryCount + 1;

        // Exponential backoff with jitter to prevent thundering herd
        const baseDelay = Math.pow(2, retryCount) * 1000; // 1s, 2s, 4s
        const jitter = Math.random() * 1000; // Random jitter up to 1s
        const backoffDelay = baseDelay + jitter;

        logger.warn(
          `Retrying request after ${Math.round(backoffDelay)}ms (attempt ${retryCount + 1}/${maxRetries})`,
          {
            url: originalRequest.url,
            status: error.response.status,
            circuitBreakerFailures: circuitBreaker.failures,
          }
        );

        // Store updated circuit breaker state
        circuitBreakers.set(urlKey, circuitBreaker);

        await new Promise(resolve => setTimeout(resolve, backoffDelay));
        return apiClient(originalRequest);
      }

      // Max retries reached
      logger.error('Max retries reached for 5xx error', {
        url: originalRequest.url,
        status: error.response.status,
        retryCount,
        circuitBreakerFailures: circuitBreaker.failures,
      });

      // Update circuit breaker
      circuitBreakers.set(urlKey, circuitBreaker);
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

    // For successful responses or non-retryable errors, reset circuit breaker for this URL
    if (originalRequest?.url) {
      const urlKey = originalRequest.url;
      const circuitBreaker = circuitBreakers.get(urlKey);
      if (circuitBreaker && circuitBreaker.failures > 0) {
        // Reset failure count on success (but keep circuit breaker state)
        circuitBreaker.failures = 0;
        circuitBreakers.set(urlKey, circuitBreaker);
      }
    }

    return Promise.reject(error);
  }
);

export default apiClient;
