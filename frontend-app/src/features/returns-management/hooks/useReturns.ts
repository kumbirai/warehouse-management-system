import { useQuery } from '@tanstack/react-query';
import { returnService } from '../services/returnService';
import { ReturnListFilters } from '../types/returns';
import { useAuth } from '../../../hooks/useAuth';

export const useReturns = (filters: ReturnListFilters) => {
  const { user } = useAuth();

  return useQuery({
    queryKey: ['returns', filters, user?.tenantId],
    queryFn: async () => {
      if (!user?.tenantId) {
        throw new Error('Tenant ID is required');
      }
      const response = await returnService.listReturns({
        ...filters,
        tenantId: user.tenantId,
      });
      if (response.error) {
        throw new Error(response.error.message);
      }
      return response.data;
    },
    enabled: !!user?.tenantId,
  });
};
