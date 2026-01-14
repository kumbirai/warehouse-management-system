import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useAuth } from '../../../hooks/useAuth';
import { stockManagementService } from '../services/stockManagementService';

/**
 * Hook: useConfirmConsignment
 * <p>
 * Confirms a consignment receipt, triggering stock item creation.
 */
export const useConfirmConsignment = () => {
  const { user } = useAuth();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (consignmentId: string) => {
      const tenantId = user?.tenantId;
      if (!tenantId) {
        throw new Error('Tenant ID is required');
      }
      return stockManagementService.confirmConsignment(consignmentId, tenantId);
    },
    onSuccess: (_, consignmentId) => {
      // Invalidate consignment queries to refetch updated data
      queryClient.invalidateQueries({ queryKey: ['consignment', consignmentId] });
      queryClient.invalidateQueries({ queryKey: ['consignments'] });
      // Invalidate stock items as they are created after confirmation
      queryClient.invalidateQueries({ queryKey: ['stockItemsByClassification'] });
    },
  });
};
