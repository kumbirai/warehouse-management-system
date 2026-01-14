import { useQuery } from '@tanstack/react-query';
import { returnService } from '../services/returnService';
import { useAuth } from '../../../hooks/useAuth';

export const useReturn = (returnId: string) => {
  const { user } = useAuth();

  return useQuery({
    queryKey: ['return', returnId, user?.tenantId],
    queryFn: async () => {
      if (!user?.tenantId) {
        throw new Error('Tenant ID is required');
      }
      const response = await returnService.getReturn(returnId, user.tenantId);
      if (response.error) {
        throw new Error(response.error.message);
      }
      return response.data;
    },
    enabled: !!returnId && !!user?.tenantId,
  });
};
