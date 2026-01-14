import { useQuery } from '@tanstack/react-query';
import { stockManagementService } from '../../stock-management/services/stockManagementService';
import {
  GetStockItemsByClassificationApiResponse,
  GetStockItemsByClassificationResponse,
} from '../../stock-management/types/stockManagement';

/**
 * Hook to fetch stock items at a specific location.
 */
export const useStockItemsByLocation = (
  locationId: string | null | undefined,
  tenantId: string | null | undefined
) => {
  return useQuery<GetStockItemsByClassificationResponse, Error>({
    queryKey: ['stockItemsByLocation', locationId, tenantId],
    queryFn: async () => {
      if (!locationId || !tenantId) {
        throw new Error('Location ID and Tenant ID are required');
      }
      const response: GetStockItemsByClassificationApiResponse =
        await stockManagementService.getStockItemsByLocation(locationId, tenantId);
      if (response.error) {
        throw new Error(response.error.message || 'Failed to load stock items');
      }
      if (!response.data) {
        throw new Error('Invalid response from server');
      }
      return response.data;
    },
    enabled: !!locationId && !!tenantId,
    staleTime: 2 * 60 * 1000, // 2 minutes
  });
};
