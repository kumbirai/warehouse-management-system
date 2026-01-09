import { useQuery } from '@tanstack/react-query';
import { notificationService } from '../services/notificationService';
import { Notification, NotificationListFilters } from '../types/notification';
import { useAuth } from '../../../hooks/useAuth';

export interface UseNotificationsResult {
  data?: {
    data?: Notification[];
    totalElements?: number;
    totalPages?: number;
  };
  isLoading: boolean;
  error: Error | null;
}

export const useNotifications = (filters: NotificationListFilters = {}): UseNotificationsResult => {
  const { user } = useAuth();

  const result = useQuery({
    queryKey: ['notifications', filters, user?.tenantId],
    queryFn: async () => {
      if (!user?.tenantId) {
        throw new Error('Tenant ID is required');
      }
      const response = await notificationService.listNotifications({
        ...filters,
        tenantId: user.tenantId,
        recipientUserId: filters.recipientUserId || user.userId,
      });

      // Backend returns array directly, we need to paginate on frontend
      const allNotifications = response.data || [];
      const page = filters.page || 0;
      const size = filters.size || 20;
      const startIndex = page * size;
      const endIndex = startIndex + size;
      const paginatedNotifications = allNotifications.slice(startIndex, endIndex);
      const totalElements = allNotifications.length;
      const totalPages = Math.ceil(totalElements / size);

      return {
        ...response,
        data: paginatedNotifications,
        totalElements,
        totalPages,
      };
    },
    enabled: !!user?.tenantId,
    staleTime: 30000, // 30 seconds
  });

  return {
    data: result.data,
    isLoading: result.isLoading,
    error: result.error as Error | null,
  };
};
