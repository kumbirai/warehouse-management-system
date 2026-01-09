import { useMutation, useQueryClient } from '@tanstack/react-query';
import { notificationService } from '../services/notificationService';
import { useAuth } from '../../../hooks/useAuth';
import { logger } from '../../../utils/logger';

export const useMarkAsRead = () => {
  const { user } = useAuth();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (notificationId: string) => {
      if (!user?.tenantId) {
        throw new Error('Tenant ID is required');
      }
      return await notificationService.markAsRead(notificationId, user.tenantId);
    },
    onSuccess: (data, notificationId) => {
      // Invalidate notifications list to refresh badge count
      queryClient.invalidateQueries({ queryKey: ['notifications'] });
      // Update the specific notification in cache
      queryClient.setQueryData(['notification', notificationId, user?.tenantId], data);
      logger.info('Notification marked as read', { notificationId });
    },
    onError: error => {
      logger.error('Failed to mark notification as read:', error);
    },
  });
};
