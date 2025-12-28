import { useQuery } from '@tanstack/react-query';
import { stockManagementService } from '../services/stockManagementService';
import { useAuth } from '../../../hooks/useAuth';

export const useStockAllocation = (allocationId: string | null) => {
  const { user } = useAuth();

  return useQuery({
    queryKey: ['stockAllocation', allocationId, user?.tenantId],
    queryFn: async () => {
      if (!allocationId || !user?.tenantId) {
        throw new Error('Allocation ID and tenant ID are required');
      }
      const response = await stockManagementService.getStockAllocation(allocationId, user.tenantId);
      return response;
    },
    enabled: !!allocationId && !!user?.tenantId,
  });
};

