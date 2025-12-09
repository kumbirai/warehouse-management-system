import {useEffect, useRef} from 'react';
import {useDispatch, useSelector} from 'react-redux';
import {useNavigate} from 'react-router-dom';
import axios from 'axios';
import {authService} from '../services/authService';
import {correlationIdService} from '../services/correlationIdService';
import {clearUser, setLoading, setUser} from '../store/authSlice';
import {RootState} from '../store';
import {logger} from '../utils/logger';

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
    const {user, isAuthenticated, isLoading} = useSelector((state: RootState) => state.auth);
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
                        const storedContext = authService.getStoredUserContext();
                        if (storedContext) {
                            dispatch(setUser(storedContext));
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
                            const isErrorObjectAuthError = axios.isAxiosError(error) &&
                                error.response?.data?.error?.code === 'UNAUTHORIZED';

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
                        const storedContext = authService.getStoredUserContext();
                        if (storedContext) {
                            // Stored context without token - clear it
                            authService.logout();
                            dispatch(clearUser());
                        } else {
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
        const response = await authService.login({username, password});
        // Refresh token is now in httpOnly cookie (set by backend)
        // Only access token needs to be stored (in memory)
        authService.storeTokens(
            response.accessToken,
            response.refreshToken || '' // May be null (stored in cookie by backend)
        );
        authService.storeUserContext(response.userContext);
        // Reset global flag on successful login to ensure fresh state
        globalAuthInitialized = true;
        dispatch(setUser(response.userContext));
        return response;
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

    const isSystemAdmin = (): boolean => {
        return hasRole('SYSTEM_ADMIN');
    };

    const isUser = (): boolean => {
        return hasRole('USER');
    };

    const isTenantAdmin = (): boolean => {
        return hasRole('TENANT_ADMIN');
    };

    return {
        user,
        isAuthenticated,
        isLoading,
        login,
        logout,
        hasRole,
        isSystemAdmin,
        isUser,
        isTenantAdmin,
    };
};

