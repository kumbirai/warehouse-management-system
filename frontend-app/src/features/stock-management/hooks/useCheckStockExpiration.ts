import { useCallback, useState } from 'react';
import { useAuth } from '../../../hooks/useAuth';
import { stockManagementService } from '../services/stockManagementService';
import { StockClassification, StockExpirationCheckResponse } from '../types/stockManagement';
import { logger } from '../../../utils/logger';

interface UseCheckStockExpirationResult {
  checkExpiration: (
    productCode: string,
    locationId: string
  ) => Promise<StockExpirationCheckResponse | null>;
  isLoading: boolean;
  error: Error | null;
}

export const useCheckStockExpiration = (): UseCheckStockExpirationResult => {
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<Error | null>(null);
  const { user } = useAuth();

  const checkExpiration = useCallback(
    async (
      productCode: string,
      locationId: string
    ): Promise<StockExpirationCheckResponse | null> => {
      if (!user?.tenantId) {
        setError(new Error('Tenant ID is required'));
        return null;
      }

      setIsLoading(true);
      setError(null);

      try {
        const response = await stockManagementService.checkStockExpiration(
          productCode,
          locationId,
          user.tenantId
        );

        if (response.error) {
          throw new Error(response.error.message || 'Failed to check stock expiration');
        }

        if (!response.data) {
          throw new Error('Invalid response from server');
        }

        // Cast string classification to enum type
        return {
          ...response.data,
          classification: response.data.classification as StockClassification,
        };
      } catch (err) {
        const error = err instanceof Error ? err : new Error('Failed to check stock expiration');
        logger.error('Error checking stock expiration:', error);
        setError(error);
        return null;
      } finally {
        setIsLoading(false);
      }
    },
    [user?.tenantId]
  );

  return {
    checkExpiration,
    isLoading,
    error,
  };
};
