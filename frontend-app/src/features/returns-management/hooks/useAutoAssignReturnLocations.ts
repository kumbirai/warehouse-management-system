import { useMutation, useQueryClient } from '@tanstack/react-query';
import {
  returnLocationService,
  AssignReturnLocationsRequest,
} from '../services/returnLocationService';
import { useAuth } from '../../../hooks/useAuth';
import { useToast } from '../../../hooks/useToast';

export const useAutoAssignReturnLocations = () => {
  const { user } = useAuth();
  const queryClient = useQueryClient();
  const { success, error: showError } = useToast();

  return useMutation({
    mutationFn: async (request: AssignReturnLocationsRequest) => {
      if (!user?.tenantId) {
        throw new Error('Tenant ID is required');
      }
      const response = await returnLocationService.assignReturnLocations(request, user.tenantId);
      if (response.error) {
        throw new Error(response.error.message);
      }
      return response.data;
    },
    onSuccess: data => {
      queryClient.invalidateQueries({ queryKey: ['returns'] });
      queryClient.invalidateQueries({ queryKey: ['return-locations'] });
      const assignmentCount = Object.keys(data.assignments).length;
      success(`Locations assigned successfully. ${assignmentCount} items assigned to locations.`);
    },
    onError: error => {
      const errorMessage =
        error instanceof Error ? error.message : 'Failed to assign return locations';
      showError(errorMessage);
    },
  });
};
