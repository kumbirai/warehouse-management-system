import {useQuery} from '@tanstack/react-query';
import {useAuth} from '../../../hooks/useAuth';
import {stockManagementService} from '../services/stockManagementService';
import {StockClassification} from '../types/stockManagement';
import {logger} from '../../../utils/logger';

/**
 * Hook: useStockItemsByClassification
 * <p>
 * Fetches stock items filtered by classification.
 */
export const useStockItemsByClassification = (classification: StockClassification | null) => {
  const { user } = useAuth();
  const tenantId = user?.tenantId;

  return useQuery({
    queryKey: ['stockItemsByClassification', classification, tenantId],
    queryFn: () => {
      if (!classification || !tenantId) {
        logger.warn('Cannot fetch stock items: classification or tenantId missing', {
          classification,
          tenantId,
        });
        throw new Error('Classification and tenant ID are required');
      }
      logger.debug('Fetching stock items by classification', {
        classification,
        tenantId,
      });
      return stockManagementService.getStockItemsByClassification(classification, tenantId);
    },
    enabled: !!classification && !!tenantId,
    onSuccess: (data) => {
      logger.info('Stock items fetched successfully', {
        classification,
        tenantId,
        count: data?.data?.stockItems?.length || 0,
      });
    },
    onError: (error) => {
      logger.error('Error fetching stock items by classification', {
        classification,
        tenantId,
        error: error instanceof Error ? error.message : String(error),
      });
    },
  });
};
