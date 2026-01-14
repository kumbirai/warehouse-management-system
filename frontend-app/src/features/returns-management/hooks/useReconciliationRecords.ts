import { useQuery } from '@tanstack/react-query';
import { reconciliationService } from '../services/reconciliationService';
import { ReconciliationListFilters } from '../types/reconciliation';
import { useAuth } from '../../../hooks/useAuth';

export const useReconciliationRecords = (filters: ReconciliationListFilters) => {
  const { user } = useAuth();

  return useQuery({
    queryKey: ['reconciliation-records', filters, user?.tenantId],
    queryFn: async () => {
      if (!user?.tenantId) {
        throw new Error('Tenant ID is required');
      }
      const response = await reconciliationService.listReconciliationRecords({
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
