import { useCallback, useEffect, useState } from 'react';
import { useAuth } from '../../../hooks/useAuth';
import { stockManagementService } from '../services/stockManagementService';
import {
  RestockPriority,
  RestockRequest,
  RestockRequestFilters,
  RestockRequestStatus,
} from '../types/stockManagement';
import { logger } from '../../../utils/logger';

interface UseRestockRequestsResult {
  restockRequests: RestockRequest[];
  totalCount: number;
  isLoading: boolean;
  error: Error | null;
  refetch: () => void;
}

export const useRestockRequests = (
  filters: RestockRequestFilters = {}
): UseRestockRequestsResult => {
  const [restockRequests, setRestockRequests] = useState<RestockRequest[]>([]);
  const [totalCount, setTotalCount] = useState(0);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<Error | null>(null);
  const { user } = useAuth();

  const fetchRestockRequests = useCallback(async () => {
    if (!user?.tenantId) {
      setError(new Error('Tenant ID is required'));
      return;
    }

    setIsLoading(true);
    setError(null);

    try {
      const response = await stockManagementService.listRestockRequests(filters, user.tenantId);

      if (response.error) {
        throw new Error(response.error.message || 'Failed to fetch restock requests');
      }

      if (!response.data) {
        throw new Error('Invalid response from server');
      }

      // Cast string priority and status values to enum types
      setRestockRequests(
        response.data.requests.map(request => ({
          ...request,
          priority: request.priority as RestockPriority,
          status: request.status as RestockRequestStatus,
        }))
      );
      setTotalCount(response.data.totalCount);
    } catch (err) {
      const error = err instanceof Error ? err : new Error('Failed to fetch restock requests');
      logger.error('Error fetching restock requests:', error);
      setError(error);
    } finally {
      setIsLoading(false);
    }
  }, [filters, user?.tenantId]);

  useEffect(() => {
    if (user?.tenantId) {
      fetchRestockRequests();
    }
  }, [user?.tenantId, fetchRestockRequests]);

  return {
    restockRequests,
    totalCount,
    isLoading,
    error,
    refetch: fetchRestockRequests,
  };
};
