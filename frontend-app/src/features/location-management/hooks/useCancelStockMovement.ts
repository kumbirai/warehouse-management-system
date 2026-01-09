import { useState } from 'react';
import { CancelStockMovementRequest, stockMovementService } from '../services/stockMovementService';
import { logger } from '../../../utils/logger';

export interface UseCancelStockMovementResult {
  cancelStockMovement: (
    movementId: string,
    request: CancelStockMovementRequest,
    tenantId: string
  ) => Promise<void>;
  isLoading: boolean;
  error: Error | null;
}

export const useCancelStockMovement = (): UseCancelStockMovementResult => {
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<Error | null>(null);

  const cancelStockMovement = async (
    movementId: string,
    request: CancelStockMovementRequest,
    tenantId: string
  ) => {
    setIsLoading(true);
    setError(null);

    try {
      const response = await stockMovementService.cancelStockMovement(
        movementId,
        request,
        tenantId
      );

      if (response.error) {
        throw new Error(response.error.message || 'Failed to cancel stock movement');
      }

      if (!response.data) {
        throw new Error('Invalid response from server');
      }

      logger.info('Stock movement cancelled successfully', { movementId });
    } catch (err) {
      const error = err instanceof Error ? err : new Error('Failed to cancel stock movement');
      logger.error('Error cancelling stock movement:', error);
      setError(error);
      throw error;
    } finally {
      setIsLoading(false);
    }
  };

  return { cancelStockMovement, isLoading, error };
};
