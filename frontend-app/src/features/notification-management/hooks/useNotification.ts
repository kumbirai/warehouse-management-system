import { useQuery } from '@tanstack/react-query';
import { notificationService } from '../services/notificationService';
import { useAuth } from '../../../hooks/useAuth';

export const useNotification = (notificationId: string | null) => {
  const { user } = useAuth();

  return useQuery({
    queryKey: ['notification', notificationId, user?.tenantId],
    queryFn: async () => {
      if (!notificationId || !user?.tenantId) {
        throw new Error('Notification ID and Tenant ID are required');
      }
      return await notificationService.getNotification(notificationId, user.tenantId);
    },
    enabled: !!notificationId && !!user?.tenantId,
  });
};
