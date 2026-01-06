import { useQuery } from '@tanstack/react-query';
import { stockManagementService } from '../services/stockManagementService';
import { useAuth } from '../../../hooks/useAuth';

export const useStockAdjustment = (adjustmentId: string | null) => {
  const { user } = useAuth();

  return useQuery({
    queryKey: ['stockAdjustment', adjustmentId, user?.tenantId],
    queryFn: async () => {
      if (!adjustmentId || !user?.tenantId) {
        throw new Error('Adjustment ID and tenant ID are required');
      }
      const response = await stockManagementService.getStockAdjustment(adjustmentId, user.tenantId);
      return response;
    },
    enabled: !!adjustmentId && !!user?.tenantId,
  });
};
