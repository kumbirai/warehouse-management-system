import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { stockManagementService } from '../services/stockManagementService';
import { CreateConsignmentRequest } from '../types/stockManagement';
import { useAuth } from '../../../hooks/useAuth';
import { logger } from '../../../utils/logger';

export interface UseCreateConsignmentResult {
  createConsignment: (request: CreateConsignmentRequest) => Promise<void>;
  isLoading: boolean;
  error: Error | null;
}

export const useCreateConsignment = (): UseCreateConsignmentResult => {
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<Error | null>(null);
  const navigate = useNavigate();
  const { user } = useAuth();

  const createConsignment = async (request: CreateConsignmentRequest) => {
    setIsLoading(true);
    setError(null);

    try {
      if (!user?.tenantId) {
        throw new Error('Tenant ID is required');
      }

      const response = await stockManagementService.createConsignment(request, user.tenantId);

      if (response.error) {
        throw new Error(response.error.message || 'Failed to create consignment');
      }

      if (!response.data) {
        throw new Error('Invalid response from server');
      }

      logger.info('Consignment created successfully', {
        consignmentId: response.data.consignmentId,
      });
      navigate(`/stock-management/consignments/${response.data.consignmentId}`);
    } catch (err) {
      const error = err instanceof Error ? err : new Error('Failed to create consignment');
      logger.error('Error creating consignment:', error);
      setError(error);
      throw error;
    } finally {
      setIsLoading(false);
    }
  };

  return { createConsignment, isLoading, error };
};
