import { Badge, IconButton } from '@mui/material';
import { Notifications as NotificationsIcon } from '@mui/icons-material';
import { Link as RouterLink } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { notificationService } from '../services/notificationService';
import { Routes } from '../../../utils/navigationUtils';
import { useAuth } from '../../../hooks/useAuth';

export const NotificationBadge = () => {
  const { user } = useAuth();

  const { data: notificationsResponse } = useQuery({
    queryKey: ['notifications', 'unread-count', user?.tenantId, user?.userId],
    queryFn: async () => {
      if (!user?.tenantId || !user?.userId) {
        return { data: [] };
      }
      return await notificationService.listNotifications({
        tenantId: user.tenantId,
        recipientUserId: user.userId,
        status: 'DELIVERED',
      });
    },
    enabled: !!user?.tenantId && !!user?.userId,
    staleTime: 30000, // 30 seconds
    refetchInterval: 60000, // Refetch every minute for real-time updates
  });

  const unreadNotifications = notificationsResponse?.data || [];
  const unreadCount = unreadNotifications.filter((n) => n.status === 'DELIVERED').length;

  return (
    <IconButton
      color="inherit"
      component={RouterLink}
      to={Routes.notifications}
      aria-label={`${unreadCount} unread notifications`}
    >
      <Badge badgeContent={unreadCount > 0 ? unreadCount : undefined} color="error">
        <NotificationsIcon />
      </Badge>
    </IconButton>
  );
};
