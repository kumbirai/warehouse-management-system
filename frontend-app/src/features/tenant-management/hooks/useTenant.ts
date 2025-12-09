import {useCallback, useEffect, useState} from 'react';
import axios from 'axios';
import {tenantService} from '../services/tenantService';
import {Tenant} from '../types/tenant';

export const useTenant = (tenantId?: string) => {
    const [tenant, setTenant] = useState<Tenant | null>(null);
    const [isLoading, setIsLoading] = useState<boolean>(false);
    const [error, setError] = useState<string | null>(null);

    const fetchTenant = useCallback(async () => {
        if (!tenantId) {
            return;
        }
        setIsLoading(true);
        setError(null);
        try {
            const response = await tenantService.getTenant(tenantId);
            if (response.error) {
                // Check if it's an authentication/authorization error
                const isAuthError = response.error.code === 'UNAUTHORIZED' ||
                    response.error.code === 'ACCESS_DENIED' ||
                    response.error.message?.toLowerCase().includes('authentication required') ||
                    response.error.message?.toLowerCase().includes('access denied');

                if (isAuthError) {
                    // Auth error - don't display, API client will handle redirect
                    setError(null);
                    return;
                }
                throw new Error(response.error.message);
            }
            setTenant(response.data ?? null);
        } catch (err) {
            // Don't display 401/403 errors - they should redirect to login via API client interceptor
            const isHttpAuthError = axios.isAxiosError(err) && (
                err.response?.status === 401 ||
                err.response?.status === 403 ||
                err.response?.data?.error?.code === 'UNAUTHORIZED' ||
                err.response?.data?.error?.code === 'ACCESS_DENIED' ||
                err.response?.data?.error?.message?.toLowerCase().includes('authentication required') ||
                err.response?.data?.error?.message?.toLowerCase().includes('access denied')
            );
            if (isHttpAuthError) {
                // Auth error - API client will handle redirect, just clear error state
                setError(null);
                return;
            }

            // For other errors, display the error message
            const message = err instanceof Error ? err.message : 'Failed to load tenant';
            setError(message);
        } finally {
            setIsLoading(false);
        }
    }, [tenantId]);

    useEffect(() => {
        fetchTenant();
    }, [fetchTenant]);

    return {tenant, isLoading, error, refetch: fetchTenant};
};

