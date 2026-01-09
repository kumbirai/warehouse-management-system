import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { CreateStockMovementRequest, stockMovementService } from '../services/stockMovementService';
import { logger } from '../../../utils/logger';

export interface UseCreateStockMovementResult {
  createStockMovement: (request: CreateStockMovementRequest, tenantId: string) => Promise<void>;
  isLoading: boolean;
  error: Error | null;
}

export const useCreateStockMovement = (): UseCreateStockMovementResult => {
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<Error | null>(null);
  const navigate = useNavigate();

  const createStockMovement = async (request: CreateStockMovementRequest, tenantId: string) => {
    setIsLoading(true);
    setError(null);

    try {
      const response = await stockMovementService.createStockMovement(request, tenantId);

      if (response.error) {
        throw new Error(response.error.message || 'Failed to create stock movement');
      }

      if (!response.data) {
        throw new Error('Invalid response from server');
      }

      logger.info('Stock movement created successfully', {
        movementId: response.data.stockMovementId,
      });
      navigate(`/stock-movements/${response.data.stockMovementId}`);
    } catch (err) {
      const error = err instanceof Error ? err : new Error('Failed to create stock movement');
      logger.error('Error creating stock movement:', error);
      setError(error);
      throw error;
    } finally {
      setIsLoading(false);
    }
  };

  return { createStockMovement, isLoading, error };
};
