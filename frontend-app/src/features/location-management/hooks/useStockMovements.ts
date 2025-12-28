import { useCallback, useEffect, useRef, useState } from 'react';
import { stockMovementService, StockMovement, StockMovementListFilters } from '../services/stockMovementService';
import { logger } from '../../../utils/logger';

export interface UseStockMovementsResult {
  movements: StockMovement[];
  totalCount: number;
  isLoading: boolean;
  error: Error | null;
  refetch: () => Promise<void>;
}

export const useStockMovements = (filters: StockMovementListFilters, tenantId: string): UseStockMovementsResult => {
  const [movements, setMovements] = useState<StockMovement[]>([]);
  const [totalCount, setTotalCount] = useState(0);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<Error | null>(null);
  const abortControllerRef = useRef<AbortController | null>(null);
  const isFetchingRef = useRef(false);

  const fetchMovements = useCallback(async () => {
    if (isFetchingRef.current) {
      return;
    }

    if (!tenantId) {
      setIsLoading(false);
      setMovements([]);
      return;
    }

    if (abortControllerRef.current) {
      abortControllerRef.current.abort();
    }

    abortControllerRef.current = new AbortController();
    isFetchingRef.current = true;
    setIsLoading(true);
    setError(null);

    try {
      const response = await stockMovementService.listStockMovements(filters, tenantId);

      if (response.error) {
        throw new Error(response.error.message || 'Failed to fetch stock movements');
      }

      if (!response.data) {
        throw new Error('Invalid response from server');
      }

      setMovements(response.data.movements || []);
      setTotalCount(response.data.totalCount || 0);
    } catch (err) {
      if (err instanceof Error && err.name === 'AbortError') {
        return;
      }

      const error = err instanceof Error ? err : new Error('Failed to fetch stock movements');
      logger.error('Error fetching stock movements:', error);
      setError(error);
      setMovements([]);
    } finally {
      isFetchingRef.current = false;
      setIsLoading(false);
    }
  }, [filters, tenantId]);

  useEffect(() => {
    fetchMovements();

    return () => {
      if (abortControllerRef.current) {
        abortControllerRef.current.abort();
      }
    };
  }, [fetchMovements]);

  return { movements, totalCount, isLoading, error, refetch: fetchMovements };
};

