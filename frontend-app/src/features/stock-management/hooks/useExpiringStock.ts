import { useCallback, useEffect, useState } from 'react';
import { useAuth } from '../../../hooks/useAuth';
import { stockManagementService } from '../services/stockManagementService';
import {
  ExpiringStockFilters,
  ExpiringStockItem,
  StockClassification,
} from '../types/stockManagement';
import { logger } from '../../../utils/logger';

interface UseExpiringStockResult {
  expiringStock: ExpiringStockItem[];
  isLoading: boolean;
  error: Error | null;
  refetch: () => void;
}

export const useExpiringStock = (filters: ExpiringStockFilters = {}): UseExpiringStockResult => {
  const [expiringStock, setExpiringStock] = useState<ExpiringStockItem[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<Error | null>(null);
  const { user } = useAuth();

  const fetchExpiringStock = useCallback(async () => {
    if (!user?.tenantId) {
      setError(new Error('Tenant ID is required'));
      return;
    }

    setIsLoading(true);
    setError(null);

    try {
      const response = await stockManagementService.getExpiringStock(filters, user.tenantId);

      if (response.error) {
        throw new Error(response.error.message || 'Failed to fetch expiring stock');
      }

      if (!response.data) {
        throw new Error('Invalid response from server');
      }

      // Cast string classification values to enum types
      setExpiringStock(
        response.data.map(item => ({
          ...item,
          classification: item.classification as StockClassification,
        }))
      );
    } catch (err) {
      const error = err instanceof Error ? err : new Error('Failed to fetch expiring stock');
      logger.error('Error fetching expiring stock:', error);
      setError(error);
    } finally {
      setIsLoading(false);
    }
  }, [filters, user?.tenantId]);

  useEffect(() => {
    if (user?.tenantId) {
      fetchExpiringStock();
    }
  }, [user?.tenantId, fetchExpiringStock]);

  return {
    expiringStock,
    isLoading,
    error,
    refetch: fetchExpiringStock,
  };
};
