import { useState } from 'react';
import { stockManagementService } from '../services/stockManagementService';
import { ValidateConsignmentRequest, ValidateConsignmentResponse } from '../types/stockManagement';
import { useAuth } from '../../../hooks/useAuth';
import { logger } from '../../../utils/logger';

export interface UseValidateConsignmentResult {
  validateConsignment: (
    request: ValidateConsignmentRequest
  ) => Promise<ValidateConsignmentResponse>;
  isLoading: boolean;
  error: Error | null;
}

export const useValidateConsignment = (): UseValidateConsignmentResult => {
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<Error | null>(null);
  const { user } = useAuth();

  const validateConsignment = async (
    request: ValidateConsignmentRequest
  ): Promise<ValidateConsignmentResponse> => {
    setIsLoading(true);
    setError(null);

    try {
      if (!user?.tenantId) {
        throw new Error('Tenant ID is required');
      }

      const response = await stockManagementService.validateConsignment(request, user.tenantId);

      if (response.error) {
        throw new Error(response.error.message || 'Failed to validate consignment');
      }

      if (!response.data) {
        throw new Error('Invalid response from server');
      }

      logger.info('Consignment validated successfully', {
        consignmentId: response.data.consignmentId,
      });
      return response.data;
    } catch (err) {
      const error = err instanceof Error ? err : new Error('Failed to validate consignment');
      logger.error('Error validating consignment:', error);
      setError(error);
      throw error;
    } finally {
      setIsLoading(false);
    }
  };

  return { validateConsignment, isLoading, error };
};
