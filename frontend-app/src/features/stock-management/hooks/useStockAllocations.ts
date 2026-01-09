import { useQuery } from '@tanstack/react-query';
import { stockManagementService } from '../services/stockManagementService';
import { useAuth } from '../../../hooks/useAuth';
import { StockAllocationFilters } from '../types/stockManagement';

export const useStockAllocations = (filters: StockAllocationFilters = {}) => {
  const { user } = useAuth();

  return useQuery({
    queryKey: ['stockAllocations', filters, user?.tenantId],
    queryFn: async () => {
      if (!user?.tenantId) {
        throw new Error('Tenant ID is required');
      }
      const response = await stockManagementService.listStockAllocations(filters, user.tenantId);
      return response;
    },
    enabled: !!user?.tenantId,
  });
};
