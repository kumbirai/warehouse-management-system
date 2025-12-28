import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { stockManagementService } from '../services/stockManagementService';
import { CreateStockAllocationRequest } from '../types/stockManagement';
import { logger } from '../../../utils/logger';

export interface UseAllocateStockResult {
  allocateStock: (request: CreateStockAllocationRequest, tenantId: string) => Promise<void>;
  isLoading: boolean;
  error: Error | null;
}

export const useAllocateStock = (): UseAllocateStockResult => {
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<Error | null>(null);
  const navigate = useNavigate();

  const allocateStock = async (request: CreateStockAllocationRequest, tenantId: string) => {
    setIsLoading(true);
    setError(null);

    try {
      const response = await stockManagementService.createStockAllocation(request, tenantId);

      if (response.error) {
        throw new Error(response.error.message || 'Failed to allocate stock');
      }

      if (!response.data) {
        throw new Error('Invalid response from server');
      }

      logger.info('Stock allocated successfully', { allocationId: response.data.allocationId });
      navigate(`/stock-allocations/${response.data.allocationId}`);
    } catch (err) {
      const error = err instanceof Error ? err : new Error('Failed to allocate stock');
      logger.error('Error allocating stock:', error);
      setError(error);
      throw error;
    } finally {
      setIsLoading(false);
    }
  };

  return { allocateStock, isLoading, error };
};

