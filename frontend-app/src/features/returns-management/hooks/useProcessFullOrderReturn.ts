import { useMutation, useQueryClient } from '@tanstack/react-query';
import { fullOrderReturnService } from '../services/fullOrderReturnService';
import { ProcessFullOrderReturnRequest } from '../types/returns';
import { useAuth } from '../../../hooks/useAuth';
import { useToast } from '../../../hooks/useToast';

export const useProcessFullOrderReturn = () => {
  const { user } = useAuth();
  const queryClient = useQueryClient();
  const { success, error: showError } = useToast();

  return useMutation({
    mutationFn: async (request: ProcessFullOrderReturnRequest) => {
      if (!user?.tenantId) {
        throw new Error('Tenant ID is required');
      }
      const response = await fullOrderReturnService.processFullOrderReturn(request, user.tenantId);
      if (response.error) {
        throw new Error(response.error.message);
      }
      return response.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['returns'] });
      success('Full order return processed successfully');
    },
    onError: error => {
      const errorMessage =
        error instanceof Error ? error.message : 'Failed to process full order return';
      showError(errorMessage);
    },
  });
};
