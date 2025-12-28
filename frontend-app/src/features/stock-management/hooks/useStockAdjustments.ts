import { useQuery } from '@tanstack/react-query';
import { stockManagementService } from '../services/stockManagementService';
import { useAuth } from '../../../hooks/useAuth';
import { StockAdjustmentFilters } from '../types/stockManagement';

export const useStockAdjustments = (filters: StockAdjustmentFilters = {}) => {
  const { user } = useAuth();

  return useQuery({
    queryKey: ['stockAdjustments', filters, user?.tenantId],
    queryFn: async () => {
      if (!user?.tenantId) {
        throw new Error('Tenant ID is required');
      }
      const response = await stockManagementService.listStockAdjustments(filters, user.tenantId);
      return response;
    },
    enabled: !!user?.tenantId,
  });
};

