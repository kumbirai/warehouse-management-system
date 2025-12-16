import { useState } from 'react';
import { stockManagementService } from '../services/stockManagementService';
import {
  UploadConsignmentCsvRequest,
  UploadConsignmentCsvResponse,
} from '../types/stockManagement';
import { useAuth } from '../../../hooks/useAuth';
import { logger } from '../../../utils/logger';

export interface UseUploadConsignmentCsvResult {
  uploadCsv: (request: UploadConsignmentCsvRequest) => Promise<UploadConsignmentCsvResponse>;
  isLoading: boolean;
  error: Error | null;
}

export const useUploadConsignmentCsv = (): UseUploadConsignmentCsvResult => {
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<Error | null>(null);
  const { user } = useAuth();

  const uploadCsv = async (
    request: UploadConsignmentCsvRequest
  ): Promise<UploadConsignmentCsvResponse> => {
    setIsLoading(true);
    setError(null);

    try {
      if (!user?.tenantId) {
        throw new Error('Tenant ID is required');
      }

      const response = await stockManagementService.uploadConsignmentCsv(request, user.tenantId);

      if (response.error) {
        throw new Error(response.error.message || 'Failed to upload CSV');
      }

      if (!response.data) {
        throw new Error('Invalid response from server');
      }

      logger.info('CSV uploaded successfully', {
        totalRows: response.data.totalRows,
        createdCount: response.data.createdCount,
        errorCount: response.data.errorCount,
      });

      return response.data;
    } catch (err) {
      const error = err instanceof Error ? err : new Error('Failed to upload CSV');
      logger.error('Error uploading CSV:', error);
      setError(error);
      throw error;
    } finally {
      setIsLoading(false);
    }
  };

  return { uploadCsv, isLoading, error };
};
