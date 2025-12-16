import { useEffect, useState } from 'react';
import { stockManagementService } from '../services/stockManagementService';
import { Consignment } from '../types/stockManagement';
import { useAuth } from '../../../hooks/useAuth';
import { logger } from '../../../utils/logger';

export interface UseConsignmentResult {
  consignment: Consignment | null;
  isLoading: boolean;
  error: Error | null;
  refetch: () => Promise<void>;
}

export const useConsignment = (consignmentId: string | undefined): UseConsignmentResult => {
  const [consignment, setConsignment] = useState<Consignment | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<Error | null>(null);
  const { user } = useAuth();

  const fetchConsignment = async () => {
    if (!consignmentId || !user?.tenantId) {
      return;
    }

    setIsLoading(true);
    setError(null);

    try {
      const response = await stockManagementService.getConsignment(consignmentId, user.tenantId);

      if (response.error) {
        throw new Error(response.error.message || 'Failed to fetch consignment');
      }

      if (!response.data) {
        throw new Error('Invalid response from server');
      }

      setConsignment(response.data);
    } catch (err) {
      const error = err instanceof Error ? err : new Error('Failed to fetch consignment');
      logger.error('Error fetching consignment:', error);
      setError(error);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchConsignment();
  }, [consignmentId, user?.tenantId]);

  return { consignment, isLoading, error, refetch: fetchConsignment };
};
