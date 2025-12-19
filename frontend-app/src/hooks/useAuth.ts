import {useEffect, useRef} from 'react';
import {useDispatch, useSelector} from 'react-redux';
import {useNavigate} from 'react-router-dom';
import axios from 'axios';
import {authService, UserContext} from '../services/authService';
import {correlationIdService} from '../services/correlationIdService';
import {clearUser, setLoading, setUser} from '../store/authSlice';
import {RootState} from '../store';
import {logger} from '../utils/logger';
import {
    LOCATION_MANAGER,
    OPERATOR,
    PICKER,
    RECONCILIATION_CLERK,
    RECONCILIATION_MANAGER,
    RETURNS_CLERK,
    RETURNS_MANAGER,
    STOCK_CLERK,
    STOCK_MANAGER,
    SYSTEM_ADMIN,
    TENANT_ADMIN,
    USER,
    VIEWER,
    WAREHOUSE_MANAGER,
} from '../constants/roles';

/**
 * Global initialization flag to prevent multiple initializations across all component instances.
 * This ensures auth is only initialized once per app session, not per component mount.
 */
let globalAuthInitialized = false;

/**
 * Custom hook for authentication.
 * Provides authentication state and methods.
 *
 * Production-grade features:
 * - Prevents race conditions with module-level initialization tracking
 * - Handles cleanup for async operations to prevent memory leaks
 * - Proper error handling with structured logging
 * - Token validation with background refresh (only on app startup, not on every mount)
 */
export const useAuth = () => {
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const { user, isAuthenticated, isLoading } = useSelector((state: RootState) => state.auth);
  const initializationRef = useRef<boolean>(false);
  const abortControllerRef = useRef<AbortController | null>(null);

  useEffect(() => {
    // Prevent multiple initializations in this component instance
    if (initializationRef.current) {
      return;
    }

    // Check if tokens were cleared by apiClient (e.g., after failed token refresh)
    // If we have user in state but no token, clear the user state
    if (user && isAuthenticated && !authService.isAuthenticated()) {
      logger.debug('Token cleared but user state exists - clearing user state');
      globalAuthInitialized = false;
      dispatch(clearUser());
      return;
    }

    // If user is already authenticated in Redux state, mark as initialized and skip verification
    // This prevents unnecessary API calls on every component mount
    // Only verify token if we don't have a user in state yet
    if (user && isAuthenticated && authService.isAuthenticated()) {
      initializationRef.current = true;
      // Mark as globally initialized without verification
      // Token verification will happen naturally through API calls if needed
      // This prevents logout on navigation when user is already authenticated
      globalAuthInitialized = true;
      // Skip verification - user is already authenticated
      // Don't make unnecessary API calls that could cause logout on transient errors
      return;
    }

    // Also check if we have a token in localStorage - if so, mark as initialized
    // This prevents re-initialization attempts when navigating between routes
    if (authService.isAuthenticated() && globalAuthInitialized) {
      initializationRef.current = true;
      // Already initialized, skip
      return;
    }

    // Initialize auth state from localStorage (only if not already initialized globally)
    if (!globalAuthInitialized) {
      initializationRef.current = true;
      globalAuthInitialized = true;

      const initializeAuth = async () => {
        // Create abort controller for cleanup
        abortControllerRef.current = new AbortController();
        const signal = abortControllerRef.current.signal;

        dispatch(setLoading(true));

        try {
          if (authService.isAuthenticated()) {
            // Use stored context first to avoid unnecessary API calls
            let storedContext: UserContext | null = null;
            try {
              storedContext = authService.getStoredUserContext();
              if (storedContext) {
                dispatch(setUser(storedContext));
              }
            } catch (error) {
              // Handle any unexpected errors from getStoredUserContext gracefully
              logger.warn('Error retrieving stored user context, will fetch from API', {
                error: error instanceof Error ? error.message : String(error),
              });
              // Continue to fetch from API
            }

            // Then fetch fresh context in background
            try {
              const userContext = await authService.getCurrentUser();

              // Check if component is still mounted
              if (signal.aborted) {
                return;
              }

              dispatch(setUser(userContext));
              authService.storeUserContext(userContext);
            } catch (error) {
              // Check if component is still mounted
              if (signal.aborted) {
                return;
              }

              // Check if it's a real authentication error (401 HTTP status)
              const isHttp401Error = axios.isAxiosError(error) && error.response?.status === 401;

              // Also check for error objects in response body (some backends return 200 with error)
              const isErrorObjectAuthError =
                axios.isAxiosError(error) && error.response?.data?.error?.code === 'UNAUTHORIZED';

              if (isHttp401Error || isErrorObjectAuthError) {
                // Real authentication error - token is invalid/expired
                logger.warn('Authentication failed, clearing tokens', {
                  errorCode: isHttp401Error ? 'HTTP_401' : 'UNAUTHORIZED_ERROR',
                });
                globalAuthInitialized = false;
                authService.logout();
                dispatch(clearUser());
                // ProtectedRoute will redirect to login when isAuthenticated becomes false
              } else {
                // Other error (network, 500, etc.) - keep stored context if available
                logger.warn('Failed to fetch user context (non-auth error)', {
                  error: error instanceof Error ? error.message : String(error),
                });
                if (!storedContext) {
                  // No stored context and can't fetch - clear auth state
                  globalAuthInitialized = false;
                  authService.logout();
                  dispatch(clearUser());
                }
                // If we have stored context, keep it (might be offline or temporary server issue)
                // This allows the app to work with cached data
              }
            }
          } else {
            // No token - check for stored user context (shouldn't happen, but handle gracefully)
            try {
              const storedContext = authService.getStoredUserContext();
              if (storedContext) {
                // Stored context without token - clear it
                authService.logout();
                dispatch(clearUser());
              } else {
                dispatch(clearUser());
              }
            } catch (error) {
              // Handle any unexpected errors from getStoredUserContext gracefully
              logger.warn('Error retrieving stored user context during cleanup', {
                error: error instanceof Error ? error.message : String(error),
              });
              // Clear auth state anyway since we don't have a token
              authService.logout();
              dispatch(clearUser());
            }
          }
        } catch (error) {
          // Unexpected error during initialization
          logger.error('Auth initialization error', error);
          globalAuthInitialized = false;
          authService.logout();
          dispatch(clearUser());
        } finally {
          if (!signal.aborted) {
            dispatch(setLoading(false));
          }
        }
      };

      initializeAuth();
    }

    // Cleanup function
    return () => {
      if (abortControllerRef.current) {
        abortControllerRef.current.abort();
      }
      // Don't reset globalAuthInitialized here - it should persist across component unmounts
      // Only reset it on logout or when auth fails
      initializationRef.current = false;
    };
    // Intentionally excluding user and isAuthenticated from dependencies to prevent infinite loops.
    // The effect should only run once on mount, and we track initialization state with useRef.
    // Adding user/isAuthenticated would cause re-initialization on every state change.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [dispatch]);

  const login = async (username: string, password: string) => {
    try {
      logger.debug('Starting login process', { username });
      const response = await authService.login({ username, password });

      logger.debug('Login response received, storing tokens and user context', {
        hasAccessToken: !!response.accessToken,
        hasUserContext: !!response.userContext,
        userId: response.userContext?.userId,
        username: response.userContext?.username,
        roles: response.userContext?.roles,
      });

      // Refresh token is now in httpOnly cookie (set by backend)
      // Only access token needs to be stored (in memory)
      authService.storeTokens(
        response.accessToken,
        response.refreshToken || '' // May be null (stored in cookie by backend)
      );
      authService.storeUserContext(response.userContext);

      logger.debug('Tokens and user context stored, updating Redux state');

      // Reset global flag on successful login to ensure fresh state
      globalAuthInitialized = true;
      dispatch(setUser(response.userContext));

      logger.info('Login completed successfully, Redux state updated', {
        userId: response.userContext.userId,
        username: response.userContext.username,
      });

      return response;
    } catch (error) {
      // Use logger.error with error as second parameter for proper error handling
      logger.error('Login failed in useAuth hook', error instanceof Error ? error : undefined, {
        errorMessage: error instanceof Error ? error.message : String(error),
        errorStack: error instanceof Error ? error.stack : undefined,
        errorName: error instanceof Error ? error.name : undefined,
      });
      throw error;
    }
  };

  const logout = async () => {
    // Reset global flag on logout
    globalAuthInitialized = false;

    // Clear correlation ID for new session
    correlationIdService.clearCorrelationId();

    // Call logout service (clears tokens and calls backend to clear cookie)
    await authService.logout();

    // Clear Redux state
    dispatch(clearUser());

    // Navigate to login
    navigate('/login');
  };

  const hasRole = (role: string): boolean => {
    return user?.roles?.includes(role) ?? false;
  };

  const hasAnyRole = (roles: string[]): boolean => {
    return roles.some(role => hasRole(role));
  };

  // System-Level Roles
  const isSystemAdmin = (): boolean => {
    return hasRole(SYSTEM_ADMIN);
  };

  // Tenant-Level Roles
  const isTenantAdmin = (): boolean => {
    return hasRole(TENANT_ADMIN);
  };

  const isWarehouseManager = (): boolean => {
    return hasRole(WAREHOUSE_MANAGER);
  };

  const isStockManager = (): boolean => {
    return hasRole(STOCK_MANAGER);
  };

  const isLocationManager = (): boolean => {
    return hasRole(LOCATION_MANAGER);
  };

  const isReconciliationManager = (): boolean => {
    return hasRole(RECONCILIATION_MANAGER);
  };

  const isReturnsManager = (): boolean => {
    return hasRole(RETURNS_MANAGER);
  };

  // Operational Roles
  const isOperator = (): boolean => {
    return hasRole(OPERATOR);
  };

  const isPicker = (): boolean => {
    return hasRole(PICKER);
  };

  const isStockClerk = (): boolean => {
    return hasRole(STOCK_CLERK);
  };

  const isReconciliationClerk = (): boolean => {
    return hasRole(RECONCILIATION_CLERK);
  };

  const isReturnsClerk = (): boolean => {
    return hasRole(RETURNS_CLERK);
  };

  const isViewer = (): boolean => {
    return hasRole(VIEWER);
  };

  const isUser = (): boolean => {
    return hasRole(USER);
  };

  // Convenience methods for role groups
  const isAdmin = (): boolean => {
    return isSystemAdmin() || isTenantAdmin();
  };

  const isManager = (): boolean => {
    return (
      isWarehouseManager() ||
      isStockManager() ||
      isLocationManager() ||
      isReconciliationManager() ||
      isReturnsManager()
    );
  };

  const isClerk = (): boolean => {
    return isStockClerk() || isReconciliationClerk() || isReturnsClerk();
  };

  return {
    user,
    isAuthenticated,
    isLoading,
    login,
    logout,
    hasRole,
    hasAnyRole,
    // System-Level
    isSystemAdmin,
    // Tenant-Level
    isTenantAdmin,
    isWarehouseManager,
    isStockManager,
    isLocationManager,
    isReconciliationManager,
    isReturnsManager,
    // Operational
    isOperator,
    isPicker,
    isStockClerk,
    isReconciliationClerk,
    isReturnsClerk,
    isViewer,
    isUser,
    // Convenience
    isAdmin,
    isManager,
    isClerk,
  };
};
