import { useEffect, useState } from 'react';
import { stockMovementService, StockMovement } from '../services/stockMovementService';
import { logger } from '../../../utils/logger';

export interface UseStockMovementResult {
  movement: StockMovement | null;
  isLoading: boolean;
  error: Error | null;
  refetch: () => Promise<void>;
}

export const useStockMovement = (movementId: string, tenantId: string): UseStockMovementResult => {
  const [movement, setMovement] = useState<StockMovement | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<Error | null>(null);

  const fetchMovement = async () => {
    if (!movementId || !tenantId) {
      setIsLoading(false);
      return;
    }

    setIsLoading(true);
    setError(null);

    try {
      const response = await stockMovementService.getStockMovement(movementId, tenantId);

      if (response.error) {
        throw new Error(response.error.message || 'Failed to fetch stock movement');
      }

      if (!response.data) {
        throw new Error('Invalid response from server');
      }

      setMovement(response.data);
    } catch (err) {
      const error = err instanceof Error ? err : new Error('Failed to fetch stock movement');
      logger.error('Error fetching stock movement:', error);
      setError(error);
      setMovement(null);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchMovement();
  }, [movementId, tenantId]);

  return { movement, isLoading, error, refetch: fetchMovement };
};

