import { useState, useCallback } from 'react';
import { productService } from '../services/productService';
import { logger } from '../../../utils/logger';

export interface UseCheckProductCodeUniquenessResult {
  checkUniqueness: (productCode: string, tenantId: string) => Promise<boolean>;
  isLoading: boolean;
  error: Error | null;
}

export const useCheckProductCodeUniqueness = (): UseCheckProductCodeUniquenessResult => {
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<Error | null>(null);

  const checkUniqueness = useCallback(async (productCode: string, tenantId: string): Promise<boolean> => {
    if (!productCode || productCode.trim().length === 0) {
      return true; // Empty is considered "unique" (will be validated by form)
    }

    setIsLoading(true);
    setError(null);

    try {
      const response = await productService.checkProductCodeUniqueness(productCode, tenantId);
      
      if (response.success && response.data) {
        return response.data.isUnique;
      } else {
        throw new Error(response.error?.message || 'Failed to check product code uniqueness');
      }
    } catch (err) {
      const error = err instanceof Error ? err : new Error('Failed to check product code uniqueness');
      logger.error('Error checking product code uniqueness:', error);
      setError(error);
      // Return false on error to be safe (assume not unique)
      return false;
    } finally {
      setIsLoading(false);
    }
  }, []);

  return { checkUniqueness, isLoading, error };
};

