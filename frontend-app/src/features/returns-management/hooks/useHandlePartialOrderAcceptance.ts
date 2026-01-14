import { useMutation, useQueryClient } from '@tanstack/react-query';
import { partialOrderAcceptanceService } from '../services/partialOrderAcceptanceService';
import { HandlePartialOrderAcceptanceRequest } from '../types/returns';
import { useAuth } from '../../../hooks/useAuth';
import { useToast } from '../../../hooks/useToast';

export const useHandlePartialOrderAcceptance = () => {
  const { user } = useAuth();
  const queryClient = useQueryClient();
  const { success, error: showError } = useToast();

  return useMutation({
    mutationFn: async (request: HandlePartialOrderAcceptanceRequest) => {
      if (!user?.tenantId) {
        throw new Error('Tenant ID is required');
      }
      const response = await partialOrderAcceptanceService.handlePartialOrderAcceptance(
        request,
        user.tenantId
      );
      if (response.error) {
        throw new Error(response.error.message);
      }
      return response.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['returns'] });
      success('Partial order acceptance processed successfully');
    },
    onError: error => {
      const errorMessage =
        error instanceof Error ? error.message : 'Failed to process partial order acceptance';
      showError(errorMessage);
    },
  });
};
