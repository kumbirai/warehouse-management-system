import { useState } from 'react';
import { pickingApiService } from '../services/pickingApiService';
import { UploadPickingListCsvRequest, UploadPickingListCsvResponse } from '../types/pickingTypes';
import { useAuth } from '../../../hooks/useAuth';
import { logger } from '../../../utils/logger';

export interface UseUploadPickingListCsvResult {
  uploadCsv: (request: UploadPickingListCsvRequest) => Promise<UploadPickingListCsvResponse>;
  isLoading: boolean;
  error: Error | null;
}

export const useUploadPickingListCsv = (): UseUploadPickingListCsvResult => {
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<Error | null>(null);
  const { user } = useAuth();

  const uploadCsv = async (
    request: UploadPickingListCsvRequest
  ): Promise<UploadPickingListCsvResponse> => {
    setIsLoading(true);
    setError(null);

    try {
      if (!user?.tenantId) {
        throw new Error('Tenant ID is required');
      }

      const response = await pickingApiService.uploadPickingListCsv(request, user.tenantId);

      if (response.error) {
        throw new Error(response.error.message || 'Failed to upload CSV');
      }

      if (!response.data) {
        throw new Error('Invalid response from server');
      }

      logger.info('CSV uploaded successfully', {
        totalRows: response.data.totalRows,
        successfulRows: response.data.successfulRows,
        errorRows: response.data.errorRows,
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
