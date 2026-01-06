import { useQuery } from '@tanstack/react-query';
import { stockManagementService } from '../services/stockManagementService';
import { StockLevelFilters } from '../types/stockManagement';
import { useAuth } from '../../../hooks/useAuth';

export const useStockLevels = (filters: StockLevelFilters = {}) => {
  const { user } = useAuth();

  return useQuery({
    queryKey: ['stock-levels', filters, user?.tenantId],
    queryFn: async () => {
      if (!user?.tenantId) {
        throw new Error('Tenant ID is required');
      }
      if (!filters.productId) {
        throw new Error('Product ID is required');
      }
      return await stockManagementService.getStockLevels(filters, user.tenantId);
    },
    enabled: !!user?.tenantId && !!filters.productId,
    staleTime: 30000, // 30 seconds
  });
};
