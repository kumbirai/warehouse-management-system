import {useMutation, useQueryClient} from '@tanstack/react-query';
import {useAuth} from '../../../hooks/useAuth';
import {stockManagementService} from '../services/stockManagementService';
import {AssignLocationToStockRequest} from '../types/stockManagement';

/**
 * Hook: useAssignLocationToStock
 * <p>
 * Assigns a location to a stock item.
 */
export const useAssignLocationToStock = () => {
  const { user } = useAuth();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      stockItemId,
      request,
    }: {
      stockItemId: string;
      request: AssignLocationToStockRequest;
    }) => {
      const tenantId = user?.tenantId;
      if (!tenantId) {
        throw new Error('Tenant ID is required');
      }
      return stockManagementService.assignLocationToStock(stockItemId, request, tenantId);
    },
    onSuccess: (_, variables) => {
      // Invalidate stock item queries to refetch updated data
      queryClient.invalidateQueries({ queryKey: ['stockItem', variables.stockItemId] });
      queryClient.invalidateQueries({ queryKey: ['stockItemsByClassification'] });
    },
  });
};
