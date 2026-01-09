import { useCallback, useEffect, useState } from 'react';
import { locationService } from '../services/locationService';
import { Location } from '../types/location';
import { logger } from '../../../utils/logger';

export interface UseWarehouseResult {
  warehouse: Location | null;
  isLoading: boolean;
  error: Error | null;
  refetch: () => Promise<void>;
}

export const useWarehouse = (warehouseId: string, tenantId: string): UseWarehouseResult => {
  const [warehouse, setWarehouse] = useState<Location | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<Error | null>(null);

  const fetchWarehouse = useCallback(async () => {
    if (!warehouseId || !tenantId) {
      return;
    }

    setIsLoading(true);
    setError(null);

    try {
      const response = await locationService.getWarehouse(warehouseId, tenantId);

      if (response.error) {
        throw new Error(response.error.message || 'Failed to fetch warehouse');
      }

      if (!response.data) {
        throw new Error('Invalid response from server');
      }

      setWarehouse(response.data);
    } catch (err) {
      const error = err instanceof Error ? err : new Error('Failed to fetch warehouse');
      logger.error('Error fetching warehouse:', error);
      setError(error);
    } finally {
      setIsLoading(false);
    }
  }, [warehouseId, tenantId]);

  useEffect(() => {
    fetchWarehouse();
  }, [fetchWarehouse]);

  return { warehouse, isLoading, error, refetch: fetchWarehouse };
};
