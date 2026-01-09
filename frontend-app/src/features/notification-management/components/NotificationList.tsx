import { Box, Typography } from '@mui/material';
import { Notification } from '../types/notification';
import { NotificationItem } from './NotificationItem';

interface NotificationListProps {
  notifications: Notification[];
  emptyMessage?: string;
}

export const NotificationList = ({
  notifications,
  emptyMessage = 'No notifications found',
}: NotificationListProps) => {
  if (notifications.length === 0) {
    return (
      <Box sx={{ textAlign: 'center', py: 4 }}>
        <Typography variant="body1" color="text.secondary">
          {emptyMessage}
        </Typography>
      </Box>
    );
  }

  return (
    <Box>
      {notifications.map(notification => (
        <NotificationItem key={notification.notificationId} notification={notification} />
      ))}
    </Box>
  );
};
