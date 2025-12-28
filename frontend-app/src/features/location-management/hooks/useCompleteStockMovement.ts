import { useState } from 'react';
import { stockMovementService } from '../services/stockMovementService';
import { logger } from '../../../utils/logger';

export interface UseCompleteStockMovementResult {
  completeStockMovement: (movementId: string, tenantId: string) => Promise<void>;
  isLoading: boolean;
  error: Error | null;
}

export const useCompleteStockMovement = (): UseCompleteStockMovementResult => {
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<Error | null>(null);

  const completeStockMovement = async (movementId: string, tenantId: string) => {
    setIsLoading(true);
    setError(null);

    try {
      const response = await stockMovementService.completeStockMovement(movementId, tenantId);

      if (response.error) {
        throw new Error(response.error.message || 'Failed to complete stock movement');
      }

      if (!response.data) {
        throw new Error('Invalid response from server');
      }

      logger.info('Stock movement completed successfully', { movementId });
    } catch (err) {
      const error = err instanceof Error ? err : new Error('Failed to complete stock movement');
      logger.error('Error completing stock movement:', error);
      setError(error);
      throw error;
    } finally {
      setIsLoading(false);
    }
  };

  return { completeStockMovement, isLoading, error };
};

