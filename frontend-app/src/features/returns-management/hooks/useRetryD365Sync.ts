import { useMutation, useQueryClient } from '@tanstack/react-query';
import { reconciliationService } from '../services/reconciliationService';
import { useAuth } from '../../../hooks/useAuth';
import { useToast } from '../../../hooks/useToast';

export const useRetryD365Sync = () => {
  const { user } = useAuth();
  const queryClient = useQueryClient();
  const { success, error: showError } = useToast();

  return useMutation({
    mutationFn: async (returnId: string) => {
      if (!user?.tenantId) {
        throw new Error('Tenant ID is required');
      }
      const response = await reconciliationService.retryD365Sync(returnId, user.tenantId);
      if (response.error) {
        throw new Error(response.error.message);
      }
      return { returnId };
    },
    onSuccess: data => {
      queryClient.invalidateQueries({ queryKey: ['reconciliation-records'] });
      queryClient.invalidateQueries({ queryKey: ['returns'] });
      success(`D365 sync retry initiated for return ${data.returnId.substring(0, 8)}`);
    },
    onError: error => {
      const errorMessage = error instanceof Error ? error.message : 'Failed to retry D365 sync';
      showError(errorMessage);
    },
  });
};
