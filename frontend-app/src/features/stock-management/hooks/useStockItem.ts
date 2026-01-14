import { useQuery } from '@tanstack/react-query';
import { useAuth } from '../../../hooks/useAuth';
import { stockManagementService } from '../services/stockManagementService';

/**
 * Hook: useStockItem
 * <p>
 * Fetches a stock item by ID.
 */
export const useStockItem = (stockItemId: string | null) => {
  const { user } = useAuth();
  const tenantId = user?.tenantId;

  return useQuery({
    queryKey: ['stockItem', stockItemId, tenantId],
    queryFn: () => {
      if (!stockItemId || !tenantId) {
        throw new Error('Stock item ID and tenant ID are required');
      }
      return stockManagementService.getStockItem(stockItemId, tenantId);
    },
    enabled: !!stockItemId && !!tenantId,
  });
};
