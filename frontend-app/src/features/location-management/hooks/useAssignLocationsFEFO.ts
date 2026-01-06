import { useMutation, useQueryClient } from '@tanstack/react-query';
import { locationService } from '../services/locationService';
import { AssignLocationsFEFORequest } from '../types/location';
import { useAuth } from '../../../hooks/useAuth';
import { logger } from '../../../utils/logger';

export const useAssignLocationsFEFO = () => {
  const { user } = useAuth();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (request: AssignLocationsFEFORequest) => {
      if (!user?.tenantId) {
        throw new Error('Tenant ID is required');
      }
      return await locationService.assignLocationsFEFO(request, user.tenantId);
    },
    onSuccess: () => {
      // Invalidate stock items and locations to refresh data
      queryClient.invalidateQueries({ queryKey: ['stock-items'] });
      queryClient.invalidateQueries({ queryKey: ['locations'] });
      logger.info('FEFO location assignment completed successfully');
    },
    onError: (error) => {
      logger.error('Failed to assign locations via FEFO:', error);
    },
  });
};
