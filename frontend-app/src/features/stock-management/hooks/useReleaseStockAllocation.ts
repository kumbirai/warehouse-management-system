import { useState } from 'react';
import { stockManagementService } from '../services/stockManagementService';
import { logger } from '../../../utils/logger';

export interface UseReleaseStockAllocationResult {
  releaseStockAllocation: (allocationId: string, tenantId: string) => Promise<void>;
  isLoading: boolean;
  error: Error | null;
}

export const useReleaseStockAllocation = (): UseReleaseStockAllocationResult => {
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<Error | null>(null);

  const releaseStockAllocation = async (allocationId: string, tenantId: string) => {
    setIsLoading(true);
    setError(null);

    try {
      const response = await stockManagementService.releaseStockAllocation(allocationId, tenantId);

      if (response.error) {
        throw new Error(response.error.message || 'Failed to release stock allocation');
      }

      if (!response.data) {
        throw new Error('Invalid response from server');
      }

      logger.info('Stock allocation released successfully', { allocationId });
    } catch (err) {
      const error = err instanceof Error ? err : new Error('Failed to release stock allocation');
      logger.error('Error releasing stock allocation:', error);
      setError(error);
      throw error;
    } finally {
      setIsLoading(false);
    }
  };

  return { releaseStockAllocation, isLoading, error };
};

