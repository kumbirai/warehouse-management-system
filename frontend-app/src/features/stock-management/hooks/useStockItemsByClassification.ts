import {useQuery} from '@tanstack/react-query';
import {useAuth} from '../../../hooks/useAuth';
import {stockManagementService} from '../services/stockManagementService';
import {StockClassification} from '../types/stockManagement';

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
        throw new Error('Classification and tenant ID are required');
      }
      return stockManagementService.getStockItemsByClassification(classification, tenantId);
    },
    enabled: !!classification && !!tenantId,
  });
};
