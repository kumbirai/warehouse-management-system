import {useQuery} from '@tanstack/react-query';
import {useEffect} from 'react';
import {useAuth} from '../../../hooks/useAuth';
import {stockManagementService} from '../services/stockManagementService';
import {GetStockItemsByClassificationApiResponse, StockClassification} from '../types/stockManagement';
import {logger} from '../../../utils/logger';

/**
 * Hook: useStockItemsByClassification
 * <p>
 * Fetches stock items filtered by classification, or all stock items if classification is null.
 */
export const useStockItemsByClassification = (classification: StockClassification | null) => {
  const { user } = useAuth();
  const tenantId = user?.tenantId;

  const queryResult = useQuery<GetStockItemsByClassificationApiResponse>({
    queryKey: ['stockItemsByClassification', classification, tenantId],
    queryFn: () => {
      if (!tenantId) {
        logger.warn('Cannot fetch stock items: tenantId missing', {
          classification,
          tenantId,
        });
        throw new Error('Tenant ID is required');
      }
      if (classification === null) {
        logger.debug('Fetching all stock items', {
          tenantId,
        });
        return stockManagementService.getAllStockItems(tenantId);
      }
      logger.debug('Fetching stock items by classification', {
        classification,
        tenantId,
      });
      return stockManagementService.getStockItemsByClassification(classification, tenantId);
    },
    enabled: !!tenantId,
  });

  // Handle success logging
  useEffect(() => {
    if (queryResult.data && queryResult.isSuccess) {
      logger.info('Stock items fetched successfully', {
        classification,
        tenantId,
        count: queryResult.data?.data?.stockItems?.length || 0,
      });
    }
  }, [queryResult.data, queryResult.isSuccess, classification, tenantId]);

  // Handle error logging
  useEffect(() => {
    if (queryResult.error) {
      logger.error('Error fetching stock items by classification', {
        classification,
        tenantId,
        error: queryResult.error instanceof Error ? queryResult.error.message : String(queryResult.error),
      });
    }
  }, [queryResult.error, classification, tenantId]);

  return queryResult;
};
