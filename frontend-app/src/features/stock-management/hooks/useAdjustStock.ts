import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { stockManagementService } from '../services/stockManagementService';
import { CreateStockAdjustmentRequest } from '../types/stockManagement';
import { logger } from '../../../utils/logger';

export interface UseAdjustStockResult {
  adjustStock: (request: CreateStockAdjustmentRequest, tenantId: string) => Promise<void>;
  isLoading: boolean;
  error: Error | null;
}

export const useAdjustStock = (): UseAdjustStockResult => {
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<Error | null>(null);
  const navigate = useNavigate();

  const adjustStock = async (request: CreateStockAdjustmentRequest, tenantId: string) => {
    setIsLoading(true);
    setError(null);

    try {
      const response = await stockManagementService.createStockAdjustment(request, tenantId);

      if (response.error) {
        throw new Error(response.error.message || 'Failed to adjust stock');
      }

      if (!response.data) {
        throw new Error('Invalid response from server');
      }

      logger.info('Stock adjusted successfully', { adjustmentId: response.data.adjustmentId });
      navigate(`/stock-adjustments/${response.data.adjustmentId}`);
    } catch (err) {
      const error = err instanceof Error ? err : new Error('Failed to adjust stock');
      logger.error('Error adjusting stock:', error);
      setError(error);
      throw error;
    } finally {
      setIsLoading(false);
    }
  };

  return { adjustStock, isLoading, error };
};
