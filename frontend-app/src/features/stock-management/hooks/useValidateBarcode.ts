import { useState } from 'react';
import { productService } from '../../product-management/services/productService';
import { useAuth } from '../../../hooks/useAuth';
import { logger } from '../../../utils/logger';

export interface BarcodeValidationResult {
  valid: boolean;
  productInfo?: {
    productId: string;
    productCode: string;
    description: string;
    primaryBarcode: string;
  };
  barcodeFormat: string;
}

export interface UseValidateBarcodeResult {
  validateBarcode: (barcode: string) => Promise<BarcodeValidationResult>;
  isLoading: boolean;
  error: Error | null;
}

export const useValidateBarcode = (): UseValidateBarcodeResult => {
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<Error | null>(null);
  const { user } = useAuth();

  const validateBarcode = async (barcode: string): Promise<BarcodeValidationResult> => {
    setIsLoading(true);
    setError(null);

    try {
      if (!user?.tenantId) {
        throw new Error('Tenant ID is required');
      }

      if (!barcode || barcode.trim().length === 0) {
        throw new Error('Barcode is required');
      }

      const response = await productService.validateBarcode(barcode.trim(), user.tenantId);

      if (response.error) {
        throw new Error(response.error.message || 'Failed to validate barcode');
      }

      if (!response.data) {
        throw new Error('Invalid response from server');
      }

      return response.data;
    } catch (err) {
      const error = err instanceof Error ? err : new Error('Failed to validate barcode');
      logger.error('Error validating barcode:', error);
      setError(error);
      throw error;
    } finally {
      setIsLoading(false);
    }
  };

  return { validateBarcode, isLoading, error };
};
