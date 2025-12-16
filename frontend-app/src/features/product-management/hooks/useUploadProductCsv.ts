import { useState } from 'react';
import { productService } from '../services/productService';
import { UploadProductCsvResponse } from '../types/product';
import { logger } from '../../../utils/logger';

export interface UseUploadProductCsvResult {
  uploadCsv: (file: File, tenantId: string) => Promise<UploadProductCsvResponse>;
  isLoading: boolean;
  error: Error | null;
}

export const useUploadProductCsv = (): UseUploadProductCsvResult => {
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<Error | null>(null);

  const uploadCsv = async (file: File, tenantId: string): Promise<UploadProductCsvResponse> => {
    setIsLoading(true);
    setError(null);

    try {
      // Validate file size (10MB limit)
      const maxSize = 10 * 1024 * 1024; // 10MB
      if (file.size > maxSize) {
        throw new Error('File size exceeds 10MB limit');
      }

      // Validate file type
      if (!file.name.endsWith('.csv')) {
        throw new Error('File must be a CSV file');
      }

      const response = await productService.uploadProductCsv(file, tenantId);
      
      if (response.success && response.data) {
        logger.info('CSV uploaded successfully', {
          totalRows: response.data.totalRows,
          createdCount: response.data.createdCount,
          updatedCount: response.data.updatedCount,
          errorCount: response.data.errorCount,
        });
        return response.data;
      } else {
        throw new Error(response.error?.message || 'Failed to upload CSV');
      }
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

